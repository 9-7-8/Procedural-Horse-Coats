package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Right-click interactions on horses:
 *
 * <ul>
 *   <li><b>any name tag</b> (not just an anvil-renamed one) opens the rename
 *       window ({@code client/HorseRenameScreen}); confirming sets the horse's
 *       registered first / last name and consumes one tag
 *       (see {@code RenameHorsePayload});</li>
 *   <li>in the debug-pen dimension only: a <b>stick</b> instantly tames an
 *       untamed horse, a <b>clock</b> instantly ages a foal to an adult;</li>
 *   <li><b>shift-right-click on a tamed foal</b> opens its inventory screen -
 *       see {@link #onFoalInventory}.</li>
 * </ul>
 *
 * Barn-name edits come from the inventory screen (see
 * {@code SetBarnNamePayload}); "tamed by" tracking is in
 * {@link HorseOwnerTrackingHandler}; the paper inspector is in
 * {@link HorsePaperInspectHandler}.
 */
@EventBusSubscriber
public final class HorseInteractionHandler {

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Horse horse)) return;

        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        boolean client = event.getLevel().isClientSide();

        // Any name tag - enchanted / anvil-renamed or not - opens the rename
        // window; vanilla's "set the entity's custom name" is suppressed. The
        // tag is consumed only when the player confirms (see RenameHorsePayload).
        if (stack.is(Items.NAME_TAG)) {
            if (HorseRecords.hasRealRecord(horse)) {
                if (!client && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                            new com.example.horsegenetics.neoforge.network.OpenHorseRenamePayload(horse.getId()));
                }
                consume(event, InteractionResult.SUCCESS);
            }
            return;
        }

        // The stick / clock shortcuts work in the horse dimension always, and
        // anywhere that debug.tools is on - they make breeding tests bearable.
        // That is a dev run by default, and a release server the owner has
        // switched it on for. Otherwise they are dimension-only.
        if (!horse.level().dimension().equals(DebugPenManager.DEBUG_LEVEL)
                && !ServerConfig.debugTools()) {
            return;
        }

        // Cancel on BOTH sides so the client doesn't predict a mount (right-clicking
        // a tamed horse with an item vanilla treats as "ride"). The state change
        // itself only runs server-side.
        if (stack.is(Items.STICK) && !horse.isTamed()) {
            // Fire the tame EVENT first, as CrouchFeedGoal and vanilla's
            // RunAroundLikeCrazyGoal do - tameWithName alone skips it, so a
            // stick-tamed horse never reached GeneDiscoveryHandler.onTame: no
            // Breeds row, no genes discovered, no "tame a mare" tick. Owner
            // report 2026-09-11: a stick-tamed Morgan and Friesian stayed "???".
            if (!client && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(horse, player)) {
                horse.tameWithName(player);
            }
            consume(event, InteractionResult.SUCCESS);
        } else if (stack.is(Items.CLOCK) && horse.isBaby()) {
            if (!client) {
                horse.setAge(0); // 0 = adult
                if (player.getVehicle() == horse) {
                    player.stopRiding(); // belt-and-braces if a mount slipped through
                }
            }
            consume(event, InteractionResult.SUCCESS);
        }
    }


    /**
     * <b>Shift-right-click opens a foal's inventory too.</b> Vanilla's
     * {@code AbstractHorse.mobInteract} bails out on {@code isBaby()} before it
     * reaches the "tamed and sneaking, so open the inventory" branch, so a foal
     * has no inventory screen at all - and with it goes this mod's <b>i</b>
     * button, which is the only way to read a foal's genes in-game. That is the
     * wrong trade: a foal cannot be saddled, but the screen is where its
     * genetics live.
     *
     * <p>Nothing has to be done about the saddle: the slot's own
     * {@code isActive()} asks {@code canUseSlot(SADDLE)}, which is already false
     * for a baby, so it simply does not appear. This opens the same menu vanilla
     * would and lets it decide.
     *
     * <p><b>Lowest priority, deliberately.</b> Every other horse interaction in
     * this mod - the name tag, the carrots, the transfer paper, the shears -
     * cancels the event when it claims a click, and a cancelled event is not
     * delivered here. So this only ever sees a click nothing else wanted, and
     * adding an interaction later needs no change here.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onFoalInventory(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractHorse horse)) return;
        Player player = event.getEntity();
        if (!horse.isBaby() || !horse.isTamed() || horse.isVehicle()
                || !player.isSecondaryUseActive()) {
            return;
        }
        if (!event.getLevel().isClientSide()) {
            horse.openCustomInventoryScreen(player);
        }
        // Cancelled on both sides: uncancelled, the client would go on to
        // predict vanilla's baby path (a feed, or nothing) and flicker.
        consume(event, InteractionResult.SUCCESS);
    }

    private static void consume(PlayerInteractEvent.EntityInteract event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    private HorseInteractionHandler() {
    }
}
