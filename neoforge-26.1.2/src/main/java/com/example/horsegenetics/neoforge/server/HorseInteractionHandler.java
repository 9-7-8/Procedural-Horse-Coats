package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
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
 *       untamed horse, a <b>clock</b> instantly ages a foal to an adult.</li>
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
        // anywhere in a dev build (FMLEnvironment.isProduction() is false under
        // runClient) - they make breeding tests bearable. In a real build they
        // are dimension-only.
        if (!horse.level().dimension().equals(DebugPenManager.DEBUG_LEVEL)
                && FMLEnvironment.isProduction()) {
            return;
        }

        // Cancel on BOTH sides so the client doesn't predict a mount (right-clicking
        // a tamed horse with an item vanilla treats as "ride"). The state change
        // itself only runs server-side.
        if (stack.is(Items.STICK) && !horse.isTamed()) {
            if (!client) {
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


    private static void consume(PlayerInteractEvent.EntityInteract event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    private HorseInteractionHandler() {
    }
}
