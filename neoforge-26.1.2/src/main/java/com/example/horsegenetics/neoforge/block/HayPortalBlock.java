package com.example.horsegenetics.neoforge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A hay-bale portal block. Cosmetic only - no collision, indestructible,
 * emits light. It has a block entity purely so it can borrow the vanilla
 * <b>End portal</b> renderer ({@link com.example.horsegenetics.neoforge.client.HayPortalRenderer}) -
 * that's the "same texture / visual effects as the ender portal, for now"
 * ask. The block itself renders nothing ({@link RenderShape#INVISIBLE}).
 *
 * <p>All portal <i>behaviour</i> (dwell timers, teleport, linking, teardown,
 * frame lighting) lives in
 * {@link com.example.horsegenetics.neoforge.server.HorsePortalManager} and
 * {@link com.example.horsegenetics.neoforge.server.PortalEventHandler}.
 * {@link #AXIS} is bookkeeping for those - the visual is axis-agnostic.
 */
public class HayPortalBlock extends BaseEntityBlock {

    public static final MapCodec<HayPortalBlock> CODEC = simpleCodec(HayPortalBlock::new);

    /** Horizontal axis the portal blocks are lined up along (matches vanilla nether portal). */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public HayPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HayPortalBlockEntity(pos, state);
    }

    /**
     * <b>Break the frame and the whole sheet goes.</b> Without this a portal
     * block was only ever removed deliberately, so knocking one hay bale out of
     * a lit frame left the interior hanging in the air - reported exactly that
     * way, and it looks like the portal is still usable when it is not.
     *
     * <p>The mechanism is vanilla's, from {@code NetherPortalBlock}: a portal
     * block that finds itself no longer enclosed returns <b>air</b> from
     * {@code updateShape} rather than removing anything itself. Turning to air
     * is a block change, which updates <i>its</i> neighbours, so one broken bale
     * collapses the entire sheet in a cascade and no code here has to know how
     * big the portal was or find the rest of it.
     *
     * <p>What it cannot borrow is vanilla's test: {@code PortalShape} is written
     * around obsidian and a nether portal's proportions. {@link #enclosed} is
     * the same idea for a hay frame, and deliberately the same rule
     * {@code HorsePortalManager} used to light it - walk out in the four
     * in-plane directions, and every ray has to reach a hay bale having crossed
     * nothing but portal.
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        if (!neighbourState.is(this) && !enclosed(level, pos, state.getValue(AXIS))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour,
                neighbourPos, neighbourState, random);
    }

    /**
     * Is this cell still inside a hay frame? Walks out along the portal's own
     * horizontal axis and vertically; each of the four rays must reach a hay
     * bale after crossing only portal blocks. Bounded by {@link #MAX_REACH} so a
     * malformed state cannot walk a chunk border.
     */
    private static boolean enclosed(LevelReader level, BlockPos pos, Direction.Axis axis) {
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() != axis && dir.getAxis() != Direction.Axis.Y) {
                continue; // out of the portal's plane - the frame does not run that way
            }
            if (!reachesFrame(level, pos, dir)) {
                return false;
            }
        }
        return true;
    }

    /** Portal blocks, then a hay bale. Anything else, or nothing, means broken. */
    private static boolean reachesFrame(LevelReader level, BlockPos from, Direction dir) {
        BlockPos.MutableBlockPos probe = from.mutable();
        for (int i = 0; i < MAX_REACH; i++) {
            probe.move(dir);
            BlockState state = level.getBlockState(probe);
            if (state.is(Blocks.HAY_BLOCK)) {
                return true;
            }
            if (!state.is(ModBlocks.HAY_PORTAL.get())) {
                return false;
            }
        }
        return false;
    }

    /**
     * Wider than any frame {@code HorsePortalManager} will light, and small
     * enough that this stays a handful of block reads on a neighbour update.
     */
    private static final int MAX_REACH = 24;
}
