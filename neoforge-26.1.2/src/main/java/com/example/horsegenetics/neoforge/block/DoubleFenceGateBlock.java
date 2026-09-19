package com.example.horsegenetics.neoforge.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * <b>A fence gate two blocks wide.</b> One item places two blocks, they open and
 * shut together, and breaking either takes both.
 *
 * <h2>Why it extends {@link FenceGateBlock} rather than copying it</h2>
 * Not for the inherited behaviour - most of that is overridden here - but because
 * {@code StallDetector.isDoorway} asks {@code instanceof FenceGateBlock}, by block
 * identity, in every state. A stall shut with one of these is therefore a closed
 * room to the stall finder <b>for free</b>, exactly as a vanilla gate is, and
 * nothing in {@code server/StallDetector} had to learn about this class. A gate
 * that did not extend it would have measured as a hole in the wall, and the
 * floor fill would have walked out through it into the yard - the bug that
 * class's header is mostly about.
 *
 * <h2>The pair, and which way it lies</h2>
 * The two halves are one {@link Half} each, and a half finds its partner by
 * turning from {@code FACING}:
 * <ul>
 *   <li>{@link Half#LEFT} - partner at {@code FACING.getCounterClockWise()}</li>
 *   <li>{@link Half#RIGHT} - partner at {@code FACING.getClockWise()}</li>
 * </ul>
 * which makes {@code LEFT} the half whose hinge post stands at low local x with
 * its leaf reaching toward its partner. That is the same handedness the models
 * are drawn in, and the blockstate's y-rotations are vanilla's, so the two agree
 * at every facing.
 *
 * <p><b>Opening may flip {@code FACING} through 180&deg;, and that swaps which
 * side the partner is on.</b> Vanilla turns a gate to swing away from whoever
 * opened it; the same turn applied here would leave each half hunting for its
 * partner in the wrong direction and both would delete themselves on the next
 * neighbour update. So a flip swaps the halves' {@link Half} as well, which
 * points each one back at the same physical block it was already paired with.
 * That is the whole reason {@link #useWithoutItem} is written out rather than
 * delegated.
 *
 * <h2>Removal is {@link #updateShape}, not a break hook</h2>
 * A half whose partner is gone returns air, the way {@code BedBlock} does it.
 * Doing it there rather than in {@code playerWillDestroy} means the pair also
 * survives being taken apart by things that are not a player breaking a block -
 * an explosion, a piston, {@code /setblock}, another mod.
 *
 * <h2>One item in, one item out - and where that is actually enforced</h2>
 * <b>In the loot table, not here.</b> This was written as though returning air
 * from {@code updateShape} dropped nothing, and it does not:
 * {@code Block.updateOrDestroy} calls
 * {@code destroyBlock(pos, (flags & 32) == 0)}, and an ordinary neighbour update
 * carries no {@code UPDATE_SUPPRESS_DROPS}. So the struck half paid out and the
 * orphaned partner paid out again - <b>breaking a double gate gave you two.</b>
 * Owner, in play: "breaking a double fence drops two".
 *
 * <p>Vanilla's doors and beds have the same two-block problem and solve it in
 * the data: every gate's table is conditioned on {@code half=left}, so whichever
 * half is struck, exactly one of the two removals pays. Compare
 * {@code data/minecraft/loot_table/blocks/oak_door.json}. Ours is written by
 * {@code tools/bake-double-gates.mjs} for vanilla's woods and
 * {@code compat/GeneratedGates} for everyone else's; <b>both</b> carry the
 * condition, and a gate that duplicates again means one of them lost it.
 *
 * <p>{@link #playerWillDestroy} below is the other end of the same rule: a break
 * that is not supposed to drop anything at all.
 */
public class DoubleFenceGateBlock extends FenceGateBlock {

    /** Which half of the pair this block is. See the class note on handedness. */
    public enum Half implements StringRepresentable {
        LEFT("left"),
        RIGHT("right");

        private final String name;

        Half(String name) {
            this.name = name;
        }

        public Half opposite() {
            return this == LEFT ? RIGHT : LEFT;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);

    /**
     * <b>The flag both halves are written with, and the bug it exists to stop.</b>
     *
     * <p>A pair cannot be moved in one {@code setBlock} - it is two blocks - and
     * vanilla's gate flag 10 does not carry {@code UPDATE_KNOWN_SHAPE}, so shape
     * updates run <em>between</em> the two writes. That gap is harmless while only
     * {@code OPEN} changes, because the halves still differ. It is not harmless
     * when opening also flips {@code FACING}: the flip swaps each half's
     * {@link Half}, so for the instant between the two writes both blocks read as
     * the <em>same</em> half, each looks for a partner that is no longer opposite
     * it, and {@link #updateShape} deletes one. The owner found it by opening a
     * gate quickly from the wrong side - "half broken, other half invisible".
     *
     * <p>{@code UPDATE_KNOWN_SHAPE} suppresses that cascade, so the pair is never
     * observed mid-write. Nothing is lost by it: neither write changes what the
     * gate is adjacent to, so there is no {@code IN_WALL} to recompute.
     */
    private static final int PAIR_WRITE =
            Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE | Block.UPDATE_KNOWN_SHAPE;

    /**
     * Typed to the <b>parent</b>, which is not a slip. {@code FenceGateBlock}
     * declares {@code codec()} as returning {@code MapCodec<FenceGateBlock>}, and
     * a {@code MapCodec} is invariant, so {@code MapCodec<DoubleFenceGateBlock>}
     * cannot override it however much a double gate is a fence gate. The factory
     * below still builds one of these; only the declared type is widened.
     *
     * <p>Inheriting vanilla's codec instead would have compiled and been wrong:
     * one of these would serialise back out as a plain fence gate.
     */
    public static final MapCodec<FenceGateBlock> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    WoodType.CODEC.fieldOf("wood_type")
                            .forGetter(b -> ((DoubleFenceGateBlock) b).woodType),
                    propertiesCodec()
            ).apply(i, DoubleFenceGateBlock::new));

    private final WoodType woodType;

    public DoubleFenceGateBlock(WoodType woodType, Properties properties) {
        super(woodType, properties);
        this.woodType = woodType;
        this.registerDefaultState(this.defaultBlockState().setValue(HALF, Half.LEFT));
    }

    @Override
    public MapCodec<FenceGateBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    /** The direction this half's partner lies in. */
    public static Direction partnerDirection(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(HALF) == Half.LEFT ? facing.getCounterClockWise() : facing.getClockWise();
    }

    /**
     * Placed as the {@link Half#LEFT} half, or <b>refused</b> if the block the
     * other half would occupy is not free. Returning null here is what makes the
     * item bounce back rather than leaving a lone half standing.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = super.getStateForPlacement(context);
        if (base == null) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // EITHER SIDE WILL DO, and trying only one is what made this feel broken.
        // The block always lands where you clicked; the only question is which
        // side its partner takes. The first version always took the
        // counter-clockwise side and refused outright when that cell was
        // occupied - so placing along a wall, or next to anything, silently did
        // nothing about half the time depending which way you happened to face.
        // Owner: "clicking and nothing happens, then you have to click again on
        // the other block, is not an intuitive gameplay design."
        for (Half half : new Half[] {Half.LEFT, Half.RIGHT}) {
            BlockState candidate = base.setValue(HALF, half);
            BlockPos otherPos = pos.relative(partnerDirection(candidate));
            if (level.getBlockState(otherPos).canBeReplaced(context)
                    && level.getWorldBorder().isWithinBounds(otherPos)) {
                return candidate;
            }
        }
        // Both sides blocked: there is genuinely nowhere for a two-wide gate, so
        // refuse rather than leave half of one standing. Still silent, which is
        // gap 270 - but it is now the rare case rather than the ordinary one.
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide()) {
            BlockPos otherPos = pos.relative(partnerDirection(state));
            // The OPPOSITE of whatever was placed, not a hardcoded RIGHT: the
            // placed half is now whichever one had room beside it, so assuming
            // it was always LEFT would hand both blocks the same half and each
            // would immediately decide the other was not its partner.
            level.setBlock(otherPos, state.setValue(HALF, state.getValue(HALF).opposite()), 3);
            // RE-ASSERT THIS HALF, now that the partner exists.
            // BlockItem.placeBlock writes the first half with flag 11, and 11 does
            // not carry UPDATE_KNOWN_SHAPE - so neighbour shape updates run while
            // the partner cell is still air, updateShape below sees an orphan and
            // returns air, and the half removes itself. setPlacedBy only runs after
            // that. The server ends up correct because this method then writes both;
            // the client had already been told the block was gone and was not told
            // otherwise, so the first half of a brand-new pair rendered as nothing
            // until something else forced a chunk update. Owner saw exactly that in
            // play: "if you only place the first one, it's invisible".
            level.setBlock(pos, state, 3);
            level.updateNeighborsAt(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    /**
     * A half with no partner is not a gate. Delegates everything else to
     * {@link FenceGateBlock}, which is what still computes {@code IN_WALL}.
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == partnerDirection(state)) {
            boolean paired = neighbourState.is(this) && neighbourState.getValue(HALF) != state.getValue(HALF);
            if (!paired) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour,
                neighbourPos, neighbourState, random);
    }

    /**
     * <b>A break that drops nothing must drop nothing from either half.</b>
     * The {@code half=left} loot condition makes {@link Half#LEFT} the half that
     * pays, so a creative player breaking the {@link Half#RIGHT} half got the
     * item anyway: their own block is suppressed by creative, but the LEFT
     * partner is removed by {@link #updateShape} through
     * {@code Block.updateOrDestroy}, which knows nothing about who swung. That is
     * the duplication bug's mirror image - a free gate out of creative mode.
     *
     * <p>So the paying half is taken out first, with flag 35
     * ({@code UPDATE_SUPPRESS_DROPS} set), leaving {@code updateShape} nothing to
     * pay for. {@code DoorBlock} does exactly this via
     * {@code DoublePlantBlock.preventDropFromBottomPart}; this is that method
     * with the pair's own geometry. Breaking the LEFT half needs no such help -
     * the RIGHT orphan's loot condition already fails.
     *
     * <p>The tool clause matters for the same reason vanilla carries it: a break
     * that would not have dropped the block must not drop the partner either.
     * A fence gate needs no tool, so today it is only ever creative that gets
     * here - it is kept so this stays correct if the block ever gains one.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && state.getValue(HALF) == Half.RIGHT
                && (player.preventsBlockDrops() || !player.hasCorrectToolForDrops(state))) {
            BlockPos otherPos = pos.relative(partnerDirection(state));
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(HALF) == Half.LEFT) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
                // The break particles and sound the suppressed removal would
                // otherwise have made - without this the partner half simply
                // blinks out, which reads as a glitch rather than a break.
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Both halves swing together, and the sound plays once.
     *
     * <p>Written out rather than calling super because super moves one block. The
     * {@code FACING} flip and the {@link Half} swap have to land on both halves in
     * the same tick or the pair loses track of itself - see the class note.
     *
     * <p>Both writes use vanilla's flag 10, which updates clients without running
     * neighbour shape updates. That matters here beyond matching vanilla: a shape
     * update between the two writes would see one half flipped and the other not,
     * and {@link #updateShape} would read that as an orphan and delete it.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        boolean opening = !state.getValue(OPEN);
        Direction facing = state.getValue(FACING);
        boolean flip = opening && facing == player.getDirection().getOpposite();
        Direction newFacing = flip ? player.getDirection() : facing;

        BlockPos otherPos = pos.relative(partnerDirection(state));
        BlockState otherState = level.getBlockState(otherPos);

        Half half = state.getValue(HALF);
        level.setBlock(pos, state.setValue(OPEN, opening)
                .setValue(FACING, newFacing)
                .setValue(HALF, flip ? half.opposite() : half), PAIR_WRITE);

        if (otherState.is(this)) {
            Half otherHalf = otherState.getValue(HALF);
            level.setBlock(otherPos, otherState.setValue(OPEN, opening)
                    .setValue(FACING, newFacing)
                    .setValue(HALF, flip ? otherHalf.opposite() : otherHalf), PAIR_WRITE);
        }

        level.playSound(player, pos, opening ? this.openSound : this.closeSound, SoundSource.BLOCKS,
                1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(player, opening ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.SUCCESS;
    }

    /**
     * <b>Redstone moves the pair, not a half.</b> A signal reaching either block
     * opens both, so a lever on one end of a two-wide gate does what it looks like
     * it should. Vanilla's own version asks only about its own position, which on
     * a pair would swing one leaf and leave the other shut.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        BlockPos otherPos = pos.relative(partnerDirection(state));
        BlockState otherState = level.getBlockState(otherPos);
        boolean hasPower = level.hasNeighborSignal(pos)
                || (otherState.is(this) && level.hasNeighborSignal(otherPos));

        if (state.getValue(POWERED) != hasPower) {
            boolean wasOpen = state.getValue(OPEN);
            level.setBlock(pos, state.setValue(POWERED, hasPower).setValue(OPEN, hasPower), 2);
            if (otherState.is(this)) {
                level.setBlock(otherPos, otherState.setValue(POWERED, hasPower).setValue(OPEN, hasPower), 2);
            }
            if (wasOpen != hasPower) {
                level.playSound(null, pos, hasPower ? this.openSound : this.closeSound, SoundSource.BLOCKS,
                        1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
                level.gameEvent(null, hasPower ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }
        }
    }
}
