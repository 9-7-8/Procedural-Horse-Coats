package com.example.horsegenetics.neoforge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Subclass of vanilla's End-portal block entity so we can reuse
 * {@code AbstractEndPortalRenderer}'s plumbing. We render every face that isn't
 * shared with another portal block, so a single block reads from any angle
 * inside a vertical hay frame while a multi-block portal only shows its outer
 * shell (no seams / z-fighting between neighbours).
 */
public class HayPortalBlockEntity extends TheEndPortalBlockEntity {

    public HayPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HAY_PORTAL.get(), pos, state);
    }

    @Override
    public boolean shouldRenderFace(Direction direction) {
        return getLevel() == null
                || !(getLevel().getBlockState(getBlockPos().relative(direction)).getBlock() instanceof HayPortalBlock);
    }
}
