package com.example.horsegenetics.neoforge.server;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * <b>Flying down is not falling.</b> Cancels fall damage on any horse that carries the flying gene, and on whoever is
 * riding it.
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
 * <h2>It IS fall immunity, for a horse that flies at all</h2>
 * The test is {@link HorseFlight.Flight#flies()} - <b>does this horse carry flight</b> - and not
 * {@link HorseFlight#active}, which is "is it flying this instant".
 *
 * <p>It was {@code active} until 2026-09-25, and the owner's report is what changed it:
 * <i>"using f to land a flying horse can still deal damage"</i>. That is inherent to gating on
 * {@code active}. Pressing F clears the toggle, so the horse is <b>no longer flying</b> the moment
 * it starts dropping - the guard switches off at the top of the fall and the ground arrives with
 * nothing protecting it. The same hole is in gliding: {@code GLIDE} is only active while
 * {@code getControllingPassenger() != null}, so dismounting in mid-air, or a glider that stalls,
 * falls unprotected. Every one of those reads as the flight gene failing rather than as a rule.
 *
 * <p>So the rule is now the simple one the owner asked for: <b>a horse with the flying gene takes
 * no fall damage, ever, and neither does anyone riding it.</b> No toggle state, no airborne test,
 * nothing to get the ordering of. That does overlap {@code bird_boned} for a flier - a flier no
 * longer needs it - which is a deliberate widening and not an oversight; bird-boned still does its
 * own job for every horse that cannot fly.
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
            // flies(), not active() - see the class note. A horse that carries
            // flight is immune whether or not it is using it, because the moment
            // it stops using it is exactly when it is falling.
            if (HorseFlight.of(horse).flies()) {
                event.setCanceled(true);
                horse.resetFallDistance();
            }
            return;
        }
        // The rider: their fall damage is the horse's, handed down.
        if (hurt.getVehicle() instanceof Horse mount && HorseFlight.of(mount).flies()) {
            event.setCanceled(true);
            hurt.resetFallDistance();
            mount.resetFallDistance();
        }
    }
}
