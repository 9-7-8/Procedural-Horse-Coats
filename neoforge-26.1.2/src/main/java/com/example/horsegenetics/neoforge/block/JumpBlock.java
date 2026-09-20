package com.example.horsegenetics.neoforge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
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
 * @see Jumps for the per-wood registration
 */
public class JumpBlock extends HorizontalDirectionalBlock {

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

        public Style next() {
            return values()[(this.ordinal() + 1) % values().length];
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
     * <p><b>Changed in place by right-clicking</b> rather than by having an
     * item per style. Three styles times twelve woods would be thirty-six
     * items for a difference the player can see at a glance, and cycling in
     * place means a built course can be restyled without being rebuilt.
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
     */
    private static boolean connects(BlockState state, BlockState neighbour) {
        return neighbour.getBlock() instanceof JumpBlock
                && neighbour.getValue(FACING).getAxis() == state.getValue(FACING).getAxis();
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

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction leftSide = facing.getCounterClockWise();
        return withPost(state
                .setValue(LEFT, connects(state, level.getBlockState(pos.relative(leftSide))))
                .setValue(RIGHT, connects(state, level.getBlockState(pos.relative(leftSide.getOpposite())))),
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
     * <b>Changing a jump costs something.</b> A stick restyles it; a plank
     * repaints it in that plank's wood.
     *
     * <p>Owner's call, and the reason is that a course is a thing you build
     * rather than a thing you fiddle with: a free right-click makes style and
     * wood into a toggle, and a material cost makes them a decision. Both are
     * deliberately cheap - one item - because the cost is meant to be a brake,
     * not a tax. Neither is consumed in creative.
     *
     * <p><b>Restyle is a stick</b>, because a style is an arrangement of poles
     * and a stick is the closest vanilla has to one. <b>Repaint is a plank</b>
     * of the wood you want, which is also how you say <i>which</i> wood - there
     * is no menu and no cycle order to learn, you simply hold the wood.
     *
     * <p>Repainting swaps to the sibling block of that wood and carries every
     * property across with {@code withPropertiesOf}, so facing, style and the
     * connection flags all survive. That is the whole reason the twelve woods
     * are separate blocks and not data - and it is the part that will change
     * when the standards and rails become independently coloured, because two
     * woods on one jump cannot be a block each.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        boolean restyle = stack.is(Items.STICK);
        JumpBlock repaint = restyle ? null : Jumps.byPlank(stack.getItem());
        if (!restyle && repaint == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (restyle) {
            level.setBlockAndUpdate(pos, state.setValue(STYLE, state.getValue(STYLE).next()));
        } else if (repaint == state.getBlock()) {
            // Already that wood. Refusing costs the player nothing and is
            // better than silently eating a plank for no change.
            return InteractionResult.PASS;
        } else {
            level.setBlockAndUpdate(pos, repaint.withPropertiesOf(state));
        }
        stack.consume(1, player);
        level.playSound(null, pos, SoundType.WOOD.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
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
