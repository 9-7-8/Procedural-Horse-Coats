package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.menu.EquestrianBenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/**
 * <b>The Tack Dyeing Bench.</b> A saddler's bench: it dyes a saddle's seat, its
 * bridle and its metal hardware independently, and will later cut a banner into
 * a saddle pad.
 *
 * <h2>No block entity, deliberately</h2>
 * It holds nothing between uses and computes its result the moment its slots
 * change, so it is shaped like vanilla's loom rather than like the
 * {@link EquineResearchShelfBlock research shelf}, which needs one only because
 * it <i>ticks</i>. {@link ContainerLevelAccess} is the entire connection to the
 * world. That saves a block entity, a ticker, save/load and a
 * {@code ContainerData} sync, none of which would have had anything to carry.
 *
 * <p>The inputs are handed back when the screen closes - see
 * {@link EquestrianBenchMenu#removed}. A bench that eats a saddle because
 * somebody pressed Escape is a bench nobody uses twice.
 */
public class EquestrianBenchBlock extends Block {

    private static final Component TITLE =
            Component.translatable("block.horsegenetics.equestrian_bench");

    public EquestrianBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new EquestrianBenchMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                    TITLE));
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Wood and leather, and it takes a beating - a workbench rather than
     * furniture. Flammable like the rest of this mod's posts and shelves.
     */
    static Properties benchProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }
}
