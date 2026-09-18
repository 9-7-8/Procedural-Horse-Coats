package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.ModBlocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Ticks the three <b>Farming and haulage</b> checklist boxes that are about
 * growing things rather than pulling them.
 *
 * <p>All three hang off block events rather than off the crop class, because a
 * {@link CropBlock} has no natural "a player did this" moment - it is ticked by
 * the world, and the loot table is what runs when it breaks. Two small
 * listeners are cheaper than threading a player through either.
 *
 * <p>Deliberately server-side and null-guarded: {@code BlockEvent} fires on
 * both sides, and {@code HorseProgress.complete} no-ops for anything that is
 * not a {@code ServerPlayer}, but doing the state comparison twice per broken
 * wheat block for nothing is worth one early return.
 */
@EventBusSubscriber(modid = HorseGenetics.MOD_ID)
public final class GoldenCarrotHandler {

    private GoldenCarrotHandler() {
    }

    @SubscribeEvent
    public static void onPlace(final BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof Player player)
                || !event.getPlacedBlock().is(ModBlocks.GOLDEN_CARROT_CROP.get())) {
            return;
        }
        HorseProgress.complete(player, ProgressTask.PLANT_GOLDEN_CARROT);
    }

    /**
     * Harvesting a mature golden carrot crop.
     *
     * <p>{@code BlockDropsEvent} rather than a break event: 26.1.2 has no
     * {@code BlockEvent.BreakEvent}, and this one is the better hook anyway -
     * it fires once, server-side only (it is handed a {@code ServerLevel}),
     * after the loot has been decided, so it cannot credit a break that was
     * cancelled.
     *
     * <p>It wants a <b>mature</b> crop. Breaking a seedling is not a harvest,
     * and crediting it would let a player tick the box by trampling one.
     */
    @SubscribeEvent
    public static void onDrops(final BlockDropsEvent event) {
        final BlockState state = event.getState();
        if (!state.is(ModBlocks.GOLDEN_CARROT_CROP.get())
                || !(state.getBlock() instanceof CropBlock crop)
                || !crop.isMaxAge(state)
                || !(event.getBreaker() instanceof Player player)) {
            return;
        }
        HorseProgress.complete(player, ProgressTask.HARVEST_GOLDEN_CARROT);
    }

    /**
     * Baling hay - the actual point of growing wheat for horses.
     *
     * <p>A hay bale is the largest single meal in the game and the thing a
     * hungry horse walks across a paddock for, so the checklist asks for the
     * bale rather than for the wheat. There is no craft event worth hooking, so
     * this is the moment the bale is <b>placed</b>: which is also the moment it
     * becomes food, since a horse eats it out of the world and not out of your
     * hand.
     */
    @SubscribeEvent
    public static void onPlaceHay(final BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof Player player)
                || !event.getPlacedBlock().is(Blocks.HAY_BLOCK)) {
            return;
        }
        HorseProgress.complete(player, ProgressTask.BALE_HAY);
    }
}
