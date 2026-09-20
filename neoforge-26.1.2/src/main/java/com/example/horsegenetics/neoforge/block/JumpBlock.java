package com.example.horsegenetics.neoforge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
 * by putting another on top. That falls out of collision rather than being
 * arranged: the drawn rail tops out at y=15/16, <b>below a horse's 1.0 step
 * height</b>, so a single jump on the ground is walked over - it is a ground
 * pole, or a cavaletti. Two high must be cleared. Every further block adds
 * almost exactly one block of required clearance, which turns a stack into a
 * <em>calibrated</em> test: a horse that clears four is saying something
 * specific about {@code Attributes.JUMP_STRENGTH}, and therefore about its
 * genes.
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

    // --- geometry -----------------------------------------------------------
    //
    // Model space, 0-16. The rail spans the full width so adjacent jumps meet
    // with no seam; the standards inset to 3 so a lone jump reads as a pole
    // between two posts rather than a solid slab.
    //
    // The standards run the FULL height, y 0-16, on purpose: stacked, they form
    // one continuous upright, which is what a real standard is. A lone jump
    // therefore has two stubby posts, which is correct for a ground pole.

    /** Rail running east-west, for a jump facing north or south. */
    private static final VoxelShape RAIL_X = Block.box(0, 11, 6, 16, 15, 10);

    /** Rail running north-south, for a jump facing east or west. */
    private static final VoxelShape RAIL_Z = Block.box(6, 11, 0, 10, 15, 16);

    /**
     * Every shape this block can have, built once.
     *
     * <p>Four facings times two connection flags each is sixteen states and
     * only eight distinct shapes - a jump facing north and one facing south
     * occupy the same boxes. Caching on the pair actually used rather than
     * recomputing per collision test keeps {@link #getShape} free, which
     * matters: a horse galloping a course is asking this question every tick.
     */
    private static final Map<ShapeKey, VoxelShape> SHAPES = buildShapes();

    private record ShapeKey(Direction facing, boolean left, boolean right) {
    }

    public JumpBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(LEFT, false)
                .setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT);
    }

    // --- shape --------------------------------------------------------------

    private static Map<ShapeKey, VoxelShape> buildShapes() {
        Map<ShapeKey, VoxelShape> shapes = new HashMap<>();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean left : new boolean[] {false, true}) {
                for (boolean right : new boolean[] {false, true}) {
                    // The rail lies across the facing: a jump facing north or
                    // south (axis Z) is a barrier running east-west.
                    VoxelShape shape = facing.getAxis() == Direction.Axis.Z ? RAIL_X : RAIL_Z;
                    Direction leftSide = facing.getCounterClockWise();
                    if (!left) {
                        shape = Shapes.or(shape, standard(leftSide));
                    }
                    if (!right) {
                        shape = Shapes.or(shape, standard(leftSide.getOpposite()));
                    }
                    shapes.put(new ShapeKey(facing, left, right), shape);
                }
            }
        }
        return Map.copyOf(shapes);
    }

    /** The upright post hard against the given side of the block. */
    private static VoxelShape standard(Direction side) {
        return switch (side) {
            case EAST -> Block.box(13, 0, 5, 16, 16, 11);
            case WEST -> Block.box(0, 0, 5, 3, 16, 11);
            case SOUTH -> Block.box(5, 0, 13, 11, 16, 16);
            case NORTH -> Block.box(5, 0, 0, 11, 16, 3);
            default -> Shapes.empty();
        };
    }

    // UNVERIFIED in 26.1.2: no other block in this module defines a shape, so
    // this override's signature is not corroborated anywhere in the repo. The
    // compiler is the check - if it moved, it moved here first.
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPES.get(new ShapeKey(
                state.getValue(FACING), state.getValue(LEFT), state.getValue(RIGHT)));
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

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction leftSide = facing.getCounterClockWise();
        return state
                .setValue(LEFT, connects(state, level.getBlockState(pos.relative(leftSide))))
                .setValue(RIGHT, connects(state, level.getBlockState(pos.relative(leftSide.getOpposite()))));
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
