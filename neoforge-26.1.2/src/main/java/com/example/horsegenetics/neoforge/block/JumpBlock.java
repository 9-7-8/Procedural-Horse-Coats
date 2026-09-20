package com.example.horsegenetics.neoforge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>A showjumping rail</b> - one rail spanning the block, and a standard at
 * each end of the run.
 *
 * <h2>Height is stacking, and that is the whole design</h2>
 * There is no height property. A jump is one block tall; you make a bigger one
 * by putting another on top, and each block added asks for one more block of
 * clearance. That turns a stack into a <em>calibrated</em> test: a horse that
 * clears three is saying something specific about
 * {@code Attributes.JUMP_STRENGTH}, and therefore about its genes.
 *
 * <p><b>The bottom rung is 1.5 blocks, not 1.0</b>, because the collision box
 * is half a block taller than the model - see {@link #getCollisionShape}.
 * Without that a lone jump sits under a horse's 1.0 step height and is simply
 * walked across, which is what shipped first and came straight back from play.
 *
 * <p>That matters more than it looks. Nothing else in the mod makes jump
 * <i>visible</i> - the debug yard's stat pens measure it by printing the
 * attribute - and {@code common/trait/HorseUnits.jumpMetres} converts strength
 * to metres through a cubic its own class doc admits is <b>unverified against
 * this mod's horses</b>. A stack of these is the instrument that can finally
 * check it. See {@code wiki/item-jumps.html}.
 *
 * <h2>The standards are drawn only at the ends</h2>
 * {@link #LEFT} and {@link #RIGHT} are fence-style connection flags: true means
 * another jump continues the rail on that side, and the standard on that side
 * is therefore <i>not</i> drawn. A row of jumps is one continuous rail with a
 * post at each extremity, which is what a real fence line looks like.
 *
 * <p>This is the same problem {@link DoubleFenceGateBlock} exists to solve, and
 * the same answer. A jump that always drew both standards would grow a pair of
 * posts between every adjacent pair - the double-post look the double gate was
 * built to avoid. Getting it wrong is not a crash; it is a course that looks
 * subtly cluttered and nobody can say why.
 *
 * <h2>Handedness follows the gates</h2>
 * "Left" is {@code facing.getCounterClockWise()}, exactly as
 * {@code DoubleGates.partnerDirection} defines it. Keep the two the same: two
 * neighbouring block families in one mod disagreeing about which way is left is
 * a trap for whoever reads them next, and it costs nothing to match.
 *
 * <p><b>Neighbours connect on the rail AXIS, not on exact facing.</b> A jump
 * facing north and one facing south are the same physical object - the model is
 * symmetric about its rail - so refusing to connect them would produce a row
 * that is visually identical to a connected one but posted in the middle,
 * depending on which way the player happened to be walking when they placed
 * each block. Axis is the honest test.
 *
 * <h2>One block, and the woods are data</h2>
 * There were twelve of these, one per wood, until the standards and the rails
 * had to be able to differ. That pair as blockstate properties is 12 x 12 x
 * everything else - 13,824 states, every one allocated at registry bootstrap on
 * the server as well as the client - so the woods live on
 * {@link JumpBlockEntity} and {@code client/JumpModel} reads them per position.
 *
 * @see Jumps for the three style items this one block is placed by
 * @see com.example.horsegenetics.neoforge.menu.JumpMenu for the screen that edits one
 */
public class JumpBlock extends HorizontalDirectionalBlock
        implements net.minecraft.world.level.block.EntityBlock {

    public static final MapCodec<JumpBlock> CODEC = simpleCodec(JumpBlock::new);

    /**
     * True when another jump continues the rail on the counter-clockwise side,
     * so this block draws no standard there.
     */
    public static final BooleanProperty LEFT = BooleanProperty.create("left");

    /** The clockwise side. See {@link #LEFT}. */
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    /**
     * True when this block draws an <b>intermediate upright</b> - the T where a
     * post meets the rail partway along a run.
     *
     * <p>A long fence line that is one unbroken rail between two distant
     * standards does not look like a fence; real ones are posted at intervals.
     * So a run grows a post every {@link #POST_SPACING} blocks. Owner's ask,
     * and it is purely cosmetic - the post adds no collision the rail does not
     * already have.
     *
     * <p><b>It is a function of world position, not of the run.</b> The post
     * lands where the coordinate along the rail axis divides by three, rather
     * than being counted from the end of the run, and that is deliberate: two
     * parallel fence lines then post in the same places instead of drifting
     * against each other, and extending a run from either end does not shuffle
     * every post along it. The cost is that where a run <i>starts</i> does not
     * affect where its posts fall, which is the right trade for a thing you
     * build by eye.
     */
    public static final BooleanProperty POST = BooleanProperty.create("post");

    /** One intermediate upright every this many blocks along a run. */
    private static final int POST_SPACING = 3;

    /**
     * <b>What kind of fence this is.</b> Cosmetic only - see {@link #STYLE}.
     *
     * <p>Every style is <b>one block deep at most</b>. The owner was explicit:
     * real courses have oxers with ground poles leading up to them, and we are
     * not building those, because a jump that occupies two blocks stops being a
     * thing you can put in a row and start being a structure.
     */
    public enum Style implements StringRepresentable {
        /** One rail. The original, and the default. */
        VERTICAL("vertical"),
        /** Two rails with a spread, front and back, inside the one block. */
        OXER("oxer"),
        /** Two poles crossed in an X, lowest in the middle. */
        CROSSRAILS("crossrails");

        private final String name;

        Style(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    /**
     * <b>Cosmetic, deliberately.</b> Every style is the same obstacle - same
     * clearance, same collision - and differs only in what it looks like.
     *
     * <p>That is a design choice and not a shortcut. The height ladder is the
     * mod's one honest reading of a horse's jump genetics
     * ({@code wiki/item-jumps.html#instrument}), and it stays readable only
     * while a stack of three means the same thing whatever it is built from. A
     * style that jumped differently would make the instrument depend on
     * decoration.
     *
     * <p><b>A blockstate property, not data on the block entity</b>, unlike the
     * two woods. It changes the geometry and therefore the model and the
     * collision box, and there are three of it - so it costs three states where
     * the woods would have cost a hundred and forty-four. That is the whole
     * rule: what the model picks a <i>shape</i> by stays a property; what it
     * picks a <i>texture</i> by became data.
     *
     * <p>Changed in place, from the jump's screen, and free. A built course can
     * be restyled without being rebuilt.
     */
    public static final EnumProperty<Style> STYLE = EnumProperty.create("style", Style.class);

    // --- geometry -----------------------------------------------------------
    //
    // Model space, 0-16. The rail spans the full width so adjacent jumps meet
    // with no seam; the standards inset to 3 so a lone jump reads as a pole
    // between two posts rather than a solid slab.
    //
    // The standards run the FULL height, y 0-16, on purpose: stacked, they form
    // one continuous upright, which is what a real standard is - and so does the
    // intermediate post, for the same reason.

    /** Where the drawn rail stops. */
    private static final int RAIL_TOP = 15;

    /** Where the drawn standards stop - the full block, so a stack is continuous. */
    private static final int STANDARD_TOP = 16;

    /**
     * Where the <i>collision</i> boxes stop - half a block above this one.
     *
     * <p><b>Vanilla's own fence number.</b> A fence is drawn one block tall and
     * collides one and a half, which is the only reason a player or a horse
     * cannot step over one; without it a 16-high barrier sits under a horse's
     * 1.0 step height and gets walked across. This block shipped without it and
     * the owner's verdict in play was immediate: the jumps &ldquo;look great,
     * expand great&rdquo;, but a horse walked over them and it
     * &ldquo;doesn't feel immersive&rdquo;. Make it behave like a fence.
     */
    private static final int COLLISION_TOP = 24;

    /**
     * One horizontal bar of a style, in the authored facing=south frame where
     * the rail runs along x.
     *
     * <p>Only y and z vary between styles - every bar spans the full width, so
     * that adjacent jumps meet with no seam whatever they are built as.
     */
    private record Bar(int yMin, int yMax, int zMin, int zMax) {
    }

    /** The bars a style DRAWS. */
    private static Bar[] drawnBars(Style style) {
        return switch (style) {
            case VERTICAL -> new Bar[] {new Bar(11, RAIL_TOP, 6, 10)};
            // Front and back inside the ONE block - the owner's hard rule is
            // that no style here is more than a block deep.
            case OXER -> new Bar[] {new Bar(11, RAIL_TOP, 2, 6), new Bar(11, RAIL_TOP, 10, 14)};
            // The X is drawn by the model with rotated elements, which a
            // VoxelShape cannot express; this is only its bounding bar.
            case CROSSRAILS -> new Bar[] {new Bar(2, 14, 6, 10)};
        };
    }

    /**
     * The single box a style COLLIDES with, which is <b>not</b> its drawn shape.
     *
     * <p>An oxer's two rails collide as the solid slab between them and the
     * crossrails' X collides as the block it occupies, because a horse that
     * clips a gap between two poles and passes through is worse than a horse
     * stopped by a fence that looks slightly emptier than it is. It also keeps
     * the promise {@link #STYLE} makes: every style is the same obstacle.
     */
    private static Bar collisionBar(Style style) {
        return switch (style) {
            case VERTICAL -> new Bar(11, COLLISION_TOP, 6, 10);
            case OXER -> new Bar(11, COLLISION_TOP, 2, 14);
            case CROSSRAILS -> new Bar(2, COLLISION_TOP, 6, 10);
        };
    }

    /** How deep the standards must be to enclose the style, as [zMin, zMax]. */
    private static int[] standardDepth(Style style) {
        return style == Style.OXER ? new int[] {1, 15} : new int[] {5, 11};
    }

    /**
     * Turn a bar into a box for the given facing.
     *
     * <p>A jump facing north or south (axis Z) is a barrier running east-west,
     * so the bar's z range is the block's z. Facing east or west swaps them.
     */
    private static VoxelShape box(Bar bar, Direction facing) {
        return facing.getAxis() == Direction.Axis.Z
                ? Block.box(0, bar.yMin(), bar.zMin(), 16, bar.yMax(), bar.zMax())
                : Block.box(bar.zMin(), bar.yMin(), 0, bar.zMax(), bar.yMax(), 16);
    }

    /**
     * Every shape this block can have, built once.
     *
     * <p>Three styles times four facings times three booleans is ninety-six
     * states and half that many distinct shapes - a jump facing north and one
     * facing south occupy the same boxes. Caching on the combination used
     * rather than
     * recomputing per collision test keeps {@link #getShape} free, which
     * matters: a horse galloping a course is asking this question every tick.
     */
    private static final Map<ShapeKey, VoxelShape> SHAPES = buildShapes(false);

    /**
     * The same shapes again, raised to {@link #COLLISION_TOP}.
     *
     * <p>This is the fence trick, and it is the whole reason a jump is an
     * obstacle rather than a step - see the class note.
     */
    private static final Map<ShapeKey, VoxelShape> COLLISION_SHAPES = buildShapes(true);

    private record ShapeKey(Style style, Direction facing, boolean left, boolean right,
                            boolean post) {
    }

    public JumpBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(STYLE, Style.VERTICAL)
                .setValue(LEFT, false)
                .setValue(RIGHT, false)
                .setValue(POST, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /**
     * Every jump carries one, and it holds nothing but the two woods.
     *
     * <p>Data only - no ticker. A course is hundreds of these, and this block
     * deliberately does not implement {@code getTicker}.
     */
    @Override
    public @Nullable net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
            BlockPos pos, BlockState state) {
        return new JumpBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STYLE, LEFT, RIGHT, POST);
    }

    // --- shape --------------------------------------------------------------

    /**
     * @param railTop     where the rail box stops
     * @param standardTop where the two upright boxes stop. <b>Not the same as
     *                    {@code railTop} for the drawn shape</b> - the rail
     *                    stops at 15 and the standards run the full 16, which
     *                    is what makes a stack read as one continuous upright.
     *                    Collapsing these two into one number silently lops a
     *                    pixel off every post.
     */
    private static Map<ShapeKey, VoxelShape> buildShapes(boolean collision) {
        int standardTop = collision ? COLLISION_TOP : STANDARD_TOP;
        Map<ShapeKey, VoxelShape> shapes = new HashMap<>();
        for (Style style : Style.values()) {
            int[] depth = standardDepth(style);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                VoxelShape bars = Shapes.empty();
                if (collision) {
                    bars = box(collisionBar(style), facing);
                } else {
                    for (Bar bar : drawnBars(style)) {
                        bars = Shapes.or(bars, box(bar, facing));
                    }
                }
                Direction leftSide = facing.getCounterClockWise();
                for (boolean left : new boolean[] {false, true}) {
                    for (boolean right : new boolean[] {false, true}) {
                        for (boolean post : new boolean[] {false, true}) {
                            VoxelShape shape = bars;
                            if (!left) {
                                shape = Shapes.or(shape, standard(leftSide, standardTop, depth));
                            }
                            if (!right) {
                                shape = Shapes.or(shape,
                                        standard(leftSide.getOpposite(), standardTop, depth));
                            }
                            if (post) {
                                shape = Shapes.or(shape, post(standardTop));
                            }
                            shapes.put(new ShapeKey(style, facing, left, right, post), shape);
                        }
                    }
                }
            }
        }
        return Map.copyOf(shapes);
    }

    /**
     * The intermediate upright, dead centre of the block.
     *
     * <p>Centred on both axes, so it is the same box whichever way the rail
     * runs and needs no per-facing case.
     */
    private static VoxelShape post(int top) {
        return Block.box(6, 0, 6, 10, top, 10);
    }

    /**
     * The upright post hard against the given side of the block.
     *
     * <p>The post starts at y=0 rather than at the rail, so a stack reads as
     * one continuous upright and so there is no gap at the foot of a fence
     * line for something to walk through.
     */
    private static VoxelShape standard(Direction side, int top, int[] depth) {
        int lo = depth[0];
        int hi = depth[1];
        return switch (side) {
            case EAST -> Block.box(13, 0, lo, 16, top, hi);
            case WEST -> Block.box(0, 0, lo, 3, top, hi);
            case SOUTH -> Block.box(lo, 0, 13, hi, top, 16);
            case NORTH -> Block.box(lo, 0, 0, hi, top, 3);
            default -> Shapes.empty();
        };
    }

    // UNVERIFIED in 26.1.2: no other block in this module defines a shape, so
    // this override's signature is not corroborated anywhere in the repo. The
    // compiler is the check - if it moved, it moved here first.
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPES.get(key(state));
    }

    /**
     * <b>Half a block taller than the jump looks</b>, exactly as a vanilla
     * fence is.
     *
     * <p>This is the difference between a jump and a decoration. The drawn rail
     * tops out below a horse's 1.0 step height, so with collision following the
     * model a horse simply <em>walks over</em> a single jump - which is what
     * shipped first, and what the owner sent back: they &ldquo;look great,
     * expand great&rdquo;, but being stepped across &ldquo;doesn't feel
     * immersive&rdquo;. Raising only the collision keeps the fence looking
     * right and makes it behave like one.
     *
     * <p>The invisible half-block above the rail is the same bargain vanilla
     * strikes with every fence and wall, and players already read it as normal.
     * <b>It applies to the player too</b>: you cannot jump a jump on foot any
     * more than you can jump a fence, so a course is walked around rather than
     * through. That is the vanilla behaviour and it is intended.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return COLLISION_SHAPES.get(key(state));
    }

    private static ShapeKey key(BlockState state) {
        return new ShapeKey(state.getValue(STYLE), state.getValue(FACING),
                state.getValue(LEFT), state.getValue(RIGHT), state.getValue(POST));
    }

    // --- connection ---------------------------------------------------------

    /**
     * Does the block on {@code side} continue this jump's rail?
     *
     * <p>Axis rather than exact facing - see the class note.
     *
     * <p><b>And the same style.</b> Two different styles standing side by side
     * are not one fence: an oxer's rails are front and back where a vertical's
     * is down the middle, and its standards are deeper to hold them. Dropping
     * the standard between them leaves an oxer's two rails running into thin
     * air beside a vertical's one, with nothing holding either up. Owner, on
     * seeing it: &ldquo;jumps shouldn't connect to jumps of a different type,
     * it looks wrong&rdquo;.
     *
     * <p>A run of one style therefore posts at <i>its</i> ends, and a course
     * built of alternating styles reads as the separate obstacles it is.
     */
    private static boolean connects(BlockState state, BlockState neighbour) {
        return neighbour.getBlock() instanceof JumpBlock
                && neighbour.getValue(FACING).getAxis() == state.getValue(FACING).getAxis()
                && neighbour.getValue(STYLE) == state.getValue(STYLE);
    }

    /**
     * Is this position one of the ones that carries an intermediate upright?
     *
     * <p>{@code floorMod} rather than {@code %}: coordinates go negative, and
     * Java's remainder does too, so a plain {@code % 3 == 0} posts correctly
     * east of the origin and irregularly west of it. That is the kind of bug
     * that is invisible in a test world built near spawn.
     */
    private static boolean postsHere(BlockPos pos, Direction facing) {
        int along = facing.getAxis() == Direction.Axis.Z ? pos.getX() : pos.getZ();
        return Math.floorMod(along, POST_SPACING) == 0;
    }

    /**
     * An intermediate upright only belongs <b>mid-run</b> - where the rail
     * carries on in both directions. At the end of a run there is already a
     * standard, and a post one block in from it reads as a mistake rather than
     * as a fence.
     */
    private static BlockState withPost(BlockState state, BlockPos pos) {
        boolean midRun = state.getValue(LEFT) && state.getValue(RIGHT);
        return state.setValue(POST, midRun && postsHere(pos, state.getValue(FACING)));
    }

    /**
     * <b>A jump placed on top of a jump takes its facing.</b>
     *
     * <p>Without this, stacking is a lottery: facing comes from whichever way
     * the player happened to be looking, so the second block of a two-high
     * fence can come out crosswise to the first. Owner, building one: "we do
     * need to be able to cleanly stack verticals". Inheriting from below makes
     * a stack line up however you walk around it.
     *
     * <p><b>Facing follows the stack; style follows the hand.</b> The style is
     * deliberately <i>not</i> inherited - {@code JumpItem} applies it after
     * this runs, so a player holding an oxer gets an oxer even on top of a
     * vertical. Only what would otherwise be arbitrary is taken from below.
     *
     * <p>Only the block directly below is consulted, not the sides. A jump
     * beside a jump is usually the start of the same fence line, but not
     * always, and overriding the player's facing there would make a row
     * impossible to turn a corner with.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader below = context.getLevel();
        BlockState under = below.getBlockState(context.getClickedPos().below());
        Direction facing = under.getBlock() instanceof JumpBlock
                ? under.getValue(FACING)
                : context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return connected(state, context.getLevel(), context.getClickedPos());
    }

    /**
     * <b>Work out this state's two connection flags and its post</b> from what
     * is actually beside it.
     *
     * <p>Public and static because <b>the style has to be set before this runs
     * and is not known here</b>. {@link #connects} tests style as well as axis,
     * so a state computed as a vertical and then restyled to an oxer carries
     * connections that belong to a block that never existed - an oxer sharing
     * a vertical's standards, which is exactly the look this was changed to
     * stop. The two callers that set a style therefore call this afterwards:
     * {@code JumpItem.getPlacementState} when one is placed, and
     * {@code JumpMenu.clickMenuButton} when one is restyled in its screen.
     *
     * <p>The <i>neighbours</i> fix themselves - {@code setBlockAndUpdate} and
     * placement both run {@link #updateShape} on them - but a block never
     * receives its own update, so this is the half that has to be asked for.
     */
    public static BlockState connected(BlockState state, LevelReader level, BlockPos pos) {
        Direction leftSide = state.getValue(FACING).getCounterClockWise();
        return withPost(state
                .setValue(LEFT, connects(state, level.getBlockState(pos.relative(leftSide))))
                .setValue(RIGHT,
                        connects(state, level.getBlockState(pos.relative(leftSide.getOpposite())))),
                pos);
    }

    /**
     * Recompute the two flags when a neighbour along the rail changes.
     *
     * <p>Only the two rail-axis sides are consulted; a block placed above or
     * below a jump changes nothing about where its standards go, and answering
     * those updates would only be a chance to get it wrong.
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        Direction leftSide = state.getValue(FACING).getCounterClockWise();
        if (directionToNeighbour == leftSide) {
            return withPost(state.setValue(LEFT, connects(state, neighbourState)), pos);
        }
        if (directionToNeighbour == leftSide.getOpposite()) {
            return withPost(state.setValue(RIGHT, connects(state, neighbourState)), pos);
        }
        return state;
    }


    /**
     * <b>Right-click opens the jump's screen</b> - two plank slots and three
     * style buttons.
     *
     * <p>This replaced two right-click interactions (owner, 2026-09-20): a
     * stick cycled the style and a plank repainted the whole block, each
     * spending its item. Both belonged to a jump that was <i>twelve blocks</i>,
     * one per wood, where a repaint was a swap to a sibling block carrying its
     * properties across. Two woods on one jump cannot be a block each, so the
     * woods became block-entity data - and once they are data, a plank held in
     * the hand can no longer say <i>which half</i> of the jump it is for.
     *
     * <p><b>It opens with an item in hand too</b>, which is not incidental: the
     * thing you are usually holding when you want this window is a plank. Every
     * item but one falls through to here - see {@link #useItemOn} for the one,
     * and for why the exception is not optional.
     *
     * @see com.example.horsegenetics.neoforge.menu.JumpMenu
     */
    /**
     * <b>A jump held against a jump stacks it. Everything else opens the
     * screen.</b>
     *
     * <p>This override exists for one case and it is the important one.
     * {@code ServerPlayerGameMode.useItemOn} calls
     * {@link #useWithoutItem} whenever the block's {@code useItemOn} comes back
     * {@code TRY_WITH_EMPTY_HAND} - <b>the hand does not have to be empty</b>,
     * despite the name; only a sneaking player with something in their hands
     * suppresses it. So the inherited default would open this block's screen
     * when a player right-clicked a jump while holding a jump, and <em>height
     * is stacking</em>: putting one on top of another is the single most common
     * thing anybody does to this block, and it would have needed a shift-click
     * forever.
     *
     * <p>{@code PASS} rather than {@code TRY_WITH_EMPTY_HAND} is what makes the
     * difference: {@code PASS} is not a {@code TryEmptyHandInteraction}, so the
     * dispatch skips the screen and falls through to the item's own
     * {@code useOn}, which places the block.
     *
     * <p>Everything else - a plank, a pickaxe, an empty hand - goes to the
     * screen, and a player who wants to place some <i>other</i> block against a
     * jump sneaks, exactly as they already do against a chest.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.getItem() instanceof com.example.horsegenetics.neoforge.item.JumpItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, who) -> new com.example.horsegenetics.neoforge.menu.JumpMenu(
                            id, inventory, net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos)),
                    state.getBlock().getName()));
        }
        return InteractionResult.CONSUME;
    }

    /**
     * <b>Carry the item's two woods into the block that was just placed.</b>
     *
     * <p>The stack's components are the only place they are: a jump broken out
     * of a course keeps them through {@code copy_components} in the loot table,
     * and a crafted one gets them from its recipe's result. Without this the
     * round trip loses them and every jump placed is oak, which reads as "the
     * screen did not save" rather than as a placement bug.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof JumpBlockEntity jump) {
            jump.setMaterials(JumpMaterials.fromComponents(stack));
        }
    }

    /**
     * Pick-block gives back <b>this style, in these woods</b>.
     *
     * <p>The default would hand over whichever item the block's
     * {@code asItem()} resolves to - the vertical - stripped of its woods, so
     * middle-clicking a birch-railed oxer while building a course would put a
     * plain oak vertical in the hand. That is the kind of thing that is only
     * ever noticed three jumps later.
     */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state,
                                          boolean includeData) {
        ItemStack stack = new ItemStack(Jumps.item(state.getValue(STYLE)));
        if (level.getBlockEntity(pos) instanceof JumpBlockEntity jump) {
            jump.materials().writeTo(stack);
        }
        return stack;
    }

    // --- rotation and mirroring ---------------------------------------------
    //
    // Structure blocks and /clone rotate blockstates rather than re-placing
    // them, so a jump in a generated stable would otherwise come out facing its
    // original way with its standards on the wrong sides.

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /**
     * A mirror flips the rail end-for-end, so the two connection flags swap
     * with it. Rotating the facing alone would leave a row's posts at the wrong
     * extremities.
     */
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState turned = state.rotate(mirror.getRotation(state.getValue(FACING)));
        // POST rides along unchanged: a mirror swaps the ends of the run but
        // does not move the block, and the post is a property of where it is.
        return turned
                .setValue(LEFT, state.getValue(RIGHT))
                .setValue(RIGHT, state.getValue(LEFT));
    }
}
