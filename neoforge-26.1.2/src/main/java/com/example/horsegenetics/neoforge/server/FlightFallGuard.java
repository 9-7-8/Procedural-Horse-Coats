package com.example.horsegenetics.neoforge.server;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * <b>Flying down is not falling.</b> Cancels fall damage on a horse that is actually flying, and on whoever is riding
 * it.
 *
 * <h2>Why this is an event cancel and not a fall-distance reset</h2>
 * It was a reset twice, and it was wrong twice, for the same reason both times: <b>the damage lands before the reset
 * does</b>.
 * <ul>
 *   <li>First it reset {@code fallDistance} in the travel mixin, <i>after</i> {@code travelFlying}. But the damage is
 *       applied inside {@code Entity.move}, one call earlier - the ordering trap {@code gene-bird-boned.html} already
 *       documents.</li>
 *   <li>Then it reset on the server each tick. But a ridden horse's fall damage on the server does not come from the
 *       entity tick at all: {@code handleMoveVehicle} calls {@code doCheckFallDamage} the moment the movement packet
 *       arrives, which is outside the tick entirely. A post-tick handler is always too late.</li>
 * </ul>
 * There is no ordering left to get right. Cancelling the damage is the only version that cannot be beaten to it -
 * and it is what {@code bird_boned} does, for exactly this reason.
 *
 * <h2>This is not fall immunity</h2>
 * It holds only while {@link HorseFlight#active} - a horse in true flight with the toggle on, or a glider that is
 * airborne. Cut the flight and the horse falls like any other animal without {@code bird_boned}, which is the
 * owner's design call: a glider that stalls at height dies unless that gene has been bred in too.
 */
@EventBusSubscriber
public final class FlightFallGuard {

    private FlightFallGuard() {
    }

    /**
     * Runs early, so nothing downstream has already acted on a blow that is about to be cancelled.
     *
     * <p>The rider is covered as well as the horse because {@code Entity.propagateFallToPassengers} hands a mount's
     * fall damage down to whoever is on it - so protecting only the horse would drop the rider's share on them
     * anyway, which is the bug the fireproof gene had.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onFallDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_FALL)) {
            return;
        }
        Entity hurt = event.getEntity();
        if (hurt instanceof Horse horse) {
            if (HorseFlight.active(horse)) {
                event.setCanceled(true);
                horse.resetFallDistance();
            }
            return;
        }
        // The rider: their fall damage is the horse's, handed down.
        if (hurt.getVehicle() instanceof Horse mount && HorseFlight.active(mount)) {
            event.setCanceled(true);
            hurt.resetFallDistance();
            mount.resetFallDistance();
        }
    }
}
