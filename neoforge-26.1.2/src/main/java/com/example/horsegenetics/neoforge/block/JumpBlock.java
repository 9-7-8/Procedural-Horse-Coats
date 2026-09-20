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
 * @see Jumps for the one item this block is placed by, and the placement hint
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
            // VoxelShape cannot express; this is only its bounding bar. It
            // spans the FULL block now that the arms are sized by their
            // projection rather than by their own length - an outline still
            // stopping at 14 would sit visibly inside the poles it is meant to
            // be tracing.
            case CROSSRAILS -> new Bar[] {new Bar(0, 16, 6, 10)};
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
                            int size) {
    }

    public JumpBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(STYLE, Style.VERTICAL)
                .setValue(LEFT, false)
                .setValue(RIGHT, false));
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
        builder.add(FACING, STYLE, LEFT, RIGHT);
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
        Map<ShapeKey, VoxelShape> shapes = new HashMap<>();
        for (int size = 0; size < JumpMaterials.SIZES.length; size++) {
            float scale = JumpMaterials.SIZES[size];
            // THE HALF-BLOCK OF INVISIBLE FENCE DOES NOT SCALE. Everything drawn
            // shrinks with the jump; the bargain that stops a horse stepping
            // over it is a constant, so a jump is always `size + 0.5` blocks to
            // clear. That is what makes the bottom of the ladder work without a
            // rule of its own - a 0.1 pole clears at 0.6, under a horse's 1.0
            // step height, so it is walked over exactly as a ground pole should
            // be.
            int standardTop = collision
                    ? Math.round(STANDARD_TOP * scale) + (COLLISION_TOP - STANDARD_TOP)
                    : Math.round(STANDARD_TOP * scale);
            for (Style style : Style.values()) {
                int[] depth = standardDepth(style);
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    VoxelShape bars = Shapes.empty();
                    if (collision) {
                        bars = box(scaled(collisionBar(style), scale, true), facing);
                    } else {
                        for (Bar bar : drawnBars(style)) {
                            bars = Shapes.or(bars, box(scaled(bar, scale, false), facing));
                        }
                    }
                    Direction leftSide = facing.getCounterClockWise();
                    for (boolean left : new boolean[] {false, true}) {
                        for (boolean right : new boolean[] {false, true}) {
                            VoxelShape shape = bars;
                            if (!left) {
                                shape = Shapes.or(shape, standard(leftSide, standardTop, depth));
                            }
                            if (!right) {
                                shape = Shapes.or(shape,
                                        standard(leftSide.getOpposite(), standardTop, depth));
                            }
                            shapes.put(new ShapeKey(style, facing, left, right, size), shape);
                        }
                    }
                }
            }
        }
        return Map.copyOf(shapes);
    }

    /**
     * One bar at a jump's height.
     *
     * <p>Scaling is about the FLOOR, which is how the model scales too: a jump
     * grows upwards out of the ground rather than about its own middle.
     *
     * @param keepTop for a collision bar, whose top is the constant half-block
     *                above the drawn height and must not be scaled twice
     */
    private static Bar scaled(Bar bar, float scale, boolean keepTop) {
        int top = keepTop
                ? Math.round((bar.yMax() - (COLLISION_TOP - STANDARD_TOP)) * scale)
                        + (COLLISION_TOP - STANDARD_TOP)
                : Math.round(bar.yMax() * scale);
        return new Bar(Math.round(bar.yMin() * scale), top, bar.zMin(), bar.zMax());
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
        return SHAPES.get(key(state, level, pos));
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
        return COLLISION_SHAPES.get(key(state, level, pos));
    }

    /**
     * <b>The shape depends on the block ENTITY</b>, because the height does.
     *
     * <p>That is a thing to be careful with - a shape is asked for far more
     * often than a model is baked, and this adds a block-entity lookup to every
     * collision test. It is the pattern vanilla's shulker box uses for exactly
     * the same reason, and the alternative was a ten-value blockstate property
     * multiplying every state in the file by ten.
     *
     * <p>No block entity - which happens while a chunk is still loading, and in
     * the odd placement check - means the ordinary one-block jump.
     */
    private static ShapeKey key(BlockState state, BlockGetter level, BlockPos pos) {
        int size = level.getBlockEntity(pos) instanceof JumpBlockEntity jump
                ? JumpMaterials.clampSize(jump.materials().size())
                : JumpMaterials.DEFAULT_SIZE;
        return new ShapeKey(state.getValue(STYLE), state.getValue(FACING),
                state.getValue(LEFT), state.getValue(RIGHT), size);
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

    // --- the crossed pair -----------------------------------------------
    //
    // A crossrail is TWO POLES crossing once, and a wide one is two LONG poles
    // crossing once across the whole obstacle - not one X per block. So a
    // crossrail has to know which slice of a shared pair it is drawing, and
    // that is what these two work out.

    /** How many blocks a crossed pair may span. */
    public static final int CROSS_MAX_SPAN = 3;

    /** How many distinct slices that makes: 1 + 2 + 3. */
    public static final int CROSS_SEGMENTS = 6;

    /**
     * How far a run is followed when working out where its X's fall.
     *
     * <p>A bound rather than no bound because this runs at <i>mesh</i> time,
     * once per crossrail: an unbounded walk down a thousand-block fence would
     * be a thousand lookups per block in it. Runs longer than this simply get
     * cut into X's from the far end of the scan instead of from the true start,
     * which is a cosmetic difference nobody building a course will ever reach.
     */
    private static final int CROSS_MAX_RUN = 32;

    /**
     * <b>Which slice of a crossed pair this block draws</b>, packed as
     * {@code span * 4 + index}.
     *
     * <h2>Grouped from the START OF THE RUN, not from world position</h2>
     * This counted in triples off the world coordinate first - the same rule
     * {@link #postsHere} uses for the intermediate post - and the rule is wrong
     * here for a reason worth keeping. A three-block run only lands inside one
     * such triple when it happens to begin on a multiple of three, so <b>two
     * runs in three came out as a two-wide X and a lone one</b>. The owner built
     * one and got exactly that: "it now correctly spans 2, but it should span up
     * to 3."
     *
     * <p>So the run is found first and cut into groups of three from its own
     * start. A run of three is always one three-wide X; a run of five is a
     * three and a two; a run of seven is a three, a three and a lone one.
     *
     * <p><b>The cost is that extending a run from its left end re-cuts every X
     * along it</b>, where the position-based rule never moved an existing one.
     * That is the trade, and it is the right way round: a course is built once
     * and looked at afterwards, so being able to <i>say</i> how wide a crossrail
     * will be beats never re-drawing one.
     *
     * <p>Called from {@code JumpModel.collectParts}, which runs on chunk-meshing
     * worker threads against a region snapshot. It reads nothing but block
     * states, which is safe there.
     */
    public static int segment(BlockGetter level, BlockPos pos, BlockState state) {
        // "Left" is the counter-clockwise side and lies at HIGH x in the
        // authored frame, so the run is walked from the LOW-x end - which is
        // the side index 0 belongs to, and the side its upright goes on.
        Direction left = state.getValue(FACING).getCounterClockWise();

        int back = reach(level, pos, left.getOpposite(), state);
        int forward = reach(level, pos, left, state);

        int offset = back;                       // our place in the whole run
        int length = back + 1 + forward;
        int groupStart = offset - offset % CROSS_MAX_SPAN;
        int span = Math.min(CROSS_MAX_SPAN, length - groupStart);
        return span * 4 + (offset - groupStart);
    }

    /**
     * How many jumps continue this run in one direction, up to
     * {@link #CROSS_MAX_RUN}.
     *
     * <p>{@link #connects} is the test, so a run is broken by exactly what
     * breaks a rail: a different style, a different axis, anything that is not
     * a jump.
     */
    private static int reach(BlockGetter level, BlockPos pos, Direction direction,
                             BlockState state) {
        int found = 0;
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (found < CROSS_MAX_RUN) {
            cursor.move(direction);
            if (!connects(state, level.getBlockState(cursor))) {
                break;
            }
            found++;
        }
        return found;
    }

    /** How many blocks wide the X this block belongs to is. */
    public static int crossSpan(int segment) {
        return segment >> 2;
    }

    /** Where in that X this block sits, counted from its left end. */
    public static int crossIndex(int segment) {
        return segment & 3;
    }

    /**
     * <b>Does this crossrail draw the upright on its left-hand end?</b>
     *
     * <p>True at the start of every <i>group</i>, not only at the start of the
     * run - which is how a run longer than three grows a post where two X's
     * meet. Owner: "after 3 wide it needs to make the T block between them."
     *
     * <p>Only the LEFT edge of a group is posted, never the right, so the
     * boundary between two X's carries exactly <b>one</b> upright rather than
     * two back-to-back ones straddling the same face. The far right end of the
     * whole run still gets its standard the ordinary way, from
     * {@link #RIGHT} - see {@code JumpModel.collectParts}.
     */
    public static boolean startsGroup(int segment) {
        return crossIndex(segment) == 0;
    }

    /**
     * <b>Set a jump's height, and every jump joined to it.</b>
     *
     * <p>A fence has a height; each block of it does not. Owner's call, and it
     * is also the only version that cannot go wrong by accident - you can build
     * a stepped line deliberately by breaking the run, and you cannot build one
     * by forgetting to click eleven times.
     *
     * <p>Bounded by {@link #CROSS_MAX_RUN} each way, like every other walk here.
     */
    public static void setRunSize(Level level, BlockPos pos, BlockState state, int size) {
        Direction left = state.getValue(FACING).getCounterClockWise();
        applySize(level, pos, size);
        for (Direction direction : new Direction[] {left, left.getOpposite()}) {
            BlockPos.MutableBlockPos cursor = pos.mutable();
            for (int step = 0; step < CROSS_MAX_RUN; step++) {
                cursor.move(direction);
                if (!connects(state, level.getBlockState(cursor))) {
                    break;
                }
                applySize(level, cursor.immutable(), size);
            }
        }
    }

    private static void applySize(Level level, BlockPos pos, int size) {
        if (level.getBlockEntity(pos) instanceof JumpBlockEntity jump
                && jump.materials().size() != size) {
            jump.setMaterials(jump.materials().withSize(size));
            // The SHAPE changed, not just the look, so the whole block has to be
            // re-broadcast rather than only the model data refreshed.
            level.setBlock(pos, level.getBlockState(pos), Block.UPDATE_ALL);
        }
    }

    /**
     * The six slices in one order, which the model's array and the baker's
     * {@code crossSuffix} both follow: spans in order, positions within each.
     */
    public static int segmentIndex(int span, int index) {
        return span * (span - 1) / 2 + index;
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
     * <p>Only the block directly below is consulted for <b>facing</b>, not the
     * sides. A jump beside a jump is usually the start of the same fence line,
     * but not always, and overriding the player's facing there would make a row
     * impossible to turn a corner with.
     *
     * <p><b>Style is inherited too, and from the sides as well.</b> It used to
     * come from the item - there was an item per style - and there is one item
     * now, so a newly placed jump has to get its style from somewhere or every
     * block of an oxer course would be placed as a vertical and restyled by
     * hand. Below first, then either neighbour along the rail axis: extending a
     * run continues that run's style, and stacking continues the stack's.
     *
     * <p>Which means <b>the first jump of a course is the only one you have to
     * dress</b>, and the rest of the line follows it. A course of mixed styles
     * is still built by putting a block down and pressing a button, exactly as
     * before - the inheritance only decides what it starts as.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState under = level.getBlockState(pos.below());
        Direction facing = under.getBlock() instanceof JumpBlock
                ? under.getValue(FACING)
                : context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(STYLE, inheritedStyle(level, pos, facing));
        return connected(state, level, pos);
    }

    /**
     * The style a jump placed here should start as: the one below it, else one
     * beside it along the rail, else the default.
     *
     * <p>The two sides are consulted in a fixed order rather than by which is
     * nearer the player, so that placing the same block twice in the same gap
     * gives the same answer. Between two runs of different styles somebody has
     * to lose, and a rule you can state beats one that depends on where you
     * were standing.
     */
    private static JumpBlock.Style inheritedStyle(LevelReader level, BlockPos pos, Direction facing) {
        BlockState under = level.getBlockState(pos.below());
        if (under.getBlock() instanceof JumpBlock) {
            return under.getValue(STYLE);
        }
        Direction leftSide = facing.getCounterClockWise();
        for (Direction side : new Direction[] {leftSide, leftSide.getOpposite()}) {
            BlockState beside = level.getBlockState(pos.relative(side));
            if (beside.getBlock() instanceof JumpBlock
                    && beside.getValue(FACING).getAxis() == facing.getAxis()) {
                return beside.getValue(STYLE);
            }
        }
        return Style.VERTICAL;
    }

    /**
     * <b>Work out this state's two connection flags and its post</b> from what
     * is actually beside it.
     *
     * <p>Public and static because <b>the style has to be settled before this
     * runs</b>. {@link #connects} tests style as well as axis, so computing the
     * flags for one style and then changing it leaves connections belonging to
     * a block that never existed - an oxer sharing a vertical's standards,
     * which is exactly the look this was added to stop. So
     * {@link #getStateForPlacement} sets the inherited style first and calls
     * this last, and {@code JumpMenu.clickMenuButton} calls it again whenever a
     * placed jump is restyled in its screen.
     *
     * <p>The <i>neighbours</i> fix themselves - {@code setBlockAndUpdate} and
     * placement both run {@link #updateShape} on them - but a block never
     * receives its own update, so this is the half that has to be asked for.
     */
    public static BlockState connected(BlockState state, LevelReader level, BlockPos pos) {
        Direction leftSide = state.getValue(FACING).getCounterClockWise();
        return state
                .setValue(LEFT, connects(state, level.getBlockState(pos.relative(leftSide))))
                .setValue(RIGHT,
                        connects(state, level.getBlockState(pos.relative(leftSide.getOpposite()))));
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
            return state.setValue(LEFT, connects(state, neighbourState));
        }
        if (directionToNeighbour == leftSide.getOpposite()) {
            return state.setValue(RIGHT, connects(state, neighbourState));
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
     * <p><b>It opens with anything in hand, including another jump.</b> There
     * was an exception for that case - a jump held against a jump placed rather
     * than opening, on the argument that stacking is the commonest thing
     * anybody does to this block - and the owner reversed it on seeing it:
     * "right clicking on a jump with a jump in hand should open the jump
     * customization menu and not place another block". Sneaking places, which
     * is what a player already does to put a block against a chest, and it
     * means the window is reachable no matter what is in your hand.
     *
     * <p>Mechanically this is simply <i>not</i> overriding {@code useItemOn}:
     * {@code ServerPlayerGameMode.useItemOn} calls this whenever a block's
     * {@code useItemOn} comes back {@code TRY_WITH_EMPTY_HAND}, and <b>the hand
     * does not have to be empty</b> despite the name - only a sneaking player
     * with something in their hands suppresses it.
     *
     * @see com.example.horsegenetics.neoforge.menu.JumpMenu
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // They have found the screen, so the placement hint has done its
            // job and stops for good.
            Jumps.learned(serverPlayer);
            // The position goes in the opening packet: the client menu reads
            // everything it draws straight off this block rather than being
            // sent a copy. See JumpMenu, and ModMenus.JUMP for the factory.
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, who) -> new com.example.horsegenetics.neoforge.menu.JumpMenu(
                            id, inventory,
                            net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos),
                            pos),
                    state.getBlock().getName()),
                    buffer -> buffer.writeBlockPos(pos));
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
        // ...and tell them where everything about this block lives, because
        // there is one jump item and nothing about it says "right-click me".
        // Jumps.hint stops on its own once they have opened one.
        if (!level.isClientSide() && placer instanceof Player player) {
            Jumps.hint(player);
        }
    }

    /**
     * Pick-block gives back <b>a jump in these woods</b>.
     *
     * <p>The default would hand over the item stripped of its woods, so
     * middle-clicking a birch-railed jump while building a course would put a
     * plain oak one in the hand. That is the kind of thing that is only ever
     * noticed three jumps later.
     *
     * <p>The <i>style</i> is not carried, and cannot be: there is one item and
     * style is not on it. Pick-block on an oxer gives a jump, which places as
     * an oxer anyway whenever it lands beside or on top of one - see
     * {@link #getStateForPlacement}.
     */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state,
                                          boolean includeData) {
        ItemStack stack = new ItemStack(Jumps.item());
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
        return turned
                .setValue(LEFT, state.getValue(RIGHT))
                .setValue(RIGHT, state.getValue(LEFT));
    }
}
