package com.example.horsegenetics.neoforge.server;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Right-click interactions on horses:
 *
 * <ul>
 *   <li>a <b>renamed name tag</b> sets the horse's registered first / last
 *       name (split on the first space) and is consumed;</li>
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

        if (stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME)) {
            if (!client) {
                handleNameTag(event, horse, player, stack);
            }
            return;
        }

        if (!horse.level().dimension().equals(DebugPenManager.DEBUG_LEVEL)) {
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

    private static void handleNameTag(PlayerInteractEvent.EntityInteract event, Horse horse, Player player, ItemStack stack) {
        if (!HorseRecords.hasRealRecord(horse)) {
            return; // let onHorseJoin assign the initial name first
        }
        String text = stack.getHoverName().getString().strip();
        String first;
        String last;
        int space = text.indexOf(' ');
        if (space < 0) {
            first = text;
            last = "";
        } else {
            first = text.substring(0, space).strip();
            last = text.substring(space + 1).strip();
        }
        if (first.isEmpty() && last.isEmpty()) {
            return; // first and last cannot both be blank
        }

        HorseRecords.rename(horse, first, last);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        consume(event, InteractionResult.SUCCESS);
    }

    private static void consume(PlayerInteractEvent.EntityInteract event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    private HorseInteractionHandler() {
    }
}
