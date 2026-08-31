package com.example.horsegenetics.neoforge.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
}
