package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.menu.ResearchShelfMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * <b>The Equine Research Shelf.</b> A bookshelf that holds research papers -
 * one of each gene, in a chest's worth of slots - and will copy any of them
 * onto a blank book for as long as the original stays in it.
 *
 * <h2>What it is for</h2>
 * It makes gene knowledge <b>physical</b>. A paper you found is a paper you can
 * lose; a paper you have filed is a gene your shelf can produce forever, and a
 * shelf you have filled is a collection somebody could walk into and see. Taking
 * the original back out ends the copying, which is what stops it being a
 * one-time unlock and makes it a thing you keep.
 *
 * <p>The other half of the loop is {@code server/GeneBookFromHorse}: a book on a
 * horse gives you a paper for one gene <i>that horse</i> carries, at random.
 * That is how papers enter the world; this is how they stop being scarce once
 * they have.
 *
 * <h2>Breaking it</h2>
 * Every filed paper drops, plus the block. Nothing is lost by moving a shelf,
 * which matters when the alternative is a player never daring to.
 */
public class EquineResearchShelfBlock extends BaseEntityBlock {

    public static final MapCodec<EquineResearchShelfBlock> CODEC =
            simpleCodec(EquineResearchShelfBlock::new);

    public EquineResearchShelfBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // a normal block, unlike the portal
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EquineResearchShelfBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof EquineResearchShelfBlockEntity shelf) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ResearchShelfMenu(id, inv, shelf),
                    Component.translatable("block.horsegenetics.equine_research_shelf")));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.RESEARCH_SHELF.get(),
                        EquineResearchShelfBlockEntity::tick);
    }

    // Breaking it drops the papers from EquineResearchShelfBlockEntity's
    // preRemoveSideEffects, where vanilla's chests do it - see there.

    // No enchantment-power override: 26.1.2 has no getEnchantPowerBonus hook on
    // Block, and enchanting power is a block tag in this version. Adding this
    // block to that tag is a one-line datapack change if it ever wants to feed
    // an enchanting table; it deliberately does not today, because a shelf full
    // of research is not a shelf full of books.

    static Block.Properties shelfProperties() {
        return Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.BOOKSHELF);
    }
}
