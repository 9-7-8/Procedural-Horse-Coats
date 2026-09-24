package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.menu.HorseStasisBankMenu;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * <b>The Horse Stasis Bank.</b> Where the horses you are not using live.
 *
 * <h2>What it is for</h2>
 * A {@link com.example.horsegenetics.neoforge.item.StasisChamberItem stasis
 * chamber} already holds a horse for nothing - the point of the whole feature is
 * that a shelved horse is not an entity and costs the server no tick time. What
 * it does not do is keep two hundred of them in any order. This is the cabinet:
 * a chest that takes chambers and nothing else, so a stud farm is a block you can
 * walk up to rather than a double chest of identical bottles.
 *
 * <p><b>Stage two of the build order, and no more than that.</b> Right now the
 * bank stores and nothing else; the tiers above Basic still buy nothing, because
 * everything they buy is the Browse tab and what it can do - see the Roadmap tab
 * on {@code wiki/horse-stasis.html}. It is listed here rather than hidden because
 * "chambers can be put somewhere sensible" is worth having on its own, and
 * because every later stage is a second tab on this menu.
 *
 * <h2>Breaking it</h2>
 * Every chamber drops, plus the block - {@link HorseStasisBankBlockEntity#preRemoveSideEffects}.
 * Nothing is lost by moving a bank, which matters a great deal more here than it
 * does for a shelf of papers.
 */
public class HorseStasisBankBlock extends BaseEntityBlock {

    public static final MapCodec<HorseStasisBankBlock> CODEC =
            simpleCodec(HorseStasisBankBlock::new);

    public HorseStasisBankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HorseStasisBankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof HorseStasisBankBlockEntity bank) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new HorseStasisBankMenu(id, inv, bank),
                    Component.translatable("block.horsegenetics.horse_stasis_bank")));
        }
        return InteractionResult.CONSUME;
    }

    // No getTicker: nothing in the bank advances with nobody looking yet. Passive
    // healing (stage four) is the thing that will need one, and it will need a
    // server-only ticker exactly like the research shelf's.

    /**
     * Chest-strength wood, iron-banded, and <b>deliberately not flammable</b>.
     *
     * <p>Vanilla's chest takes {@code ignitedByLava()} and this does not, which is
     * a divergence on purpose: a chest that burns down loses its diamonds, and a
     * bank that burns down loses fifty-four pedigreed horses to a lightning strike
     * and drops them into the fire that did it. Mod blocks are not in
     * {@code FireBlock}'s flammability table unless they register themselves, so
     * omitting the one call is the whole of it.
     */
    static Block.Properties bankProperties() {
        return Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(SoundType.WOOD);
    }
}
