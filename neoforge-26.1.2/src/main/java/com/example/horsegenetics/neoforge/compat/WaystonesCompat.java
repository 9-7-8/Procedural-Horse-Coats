package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.neoforged.fml.ModList;

/**
 * <b>A horse you are riding comes with you through a waystone.</b>
 *
 * <h2>What was wrong, and why no setting fixes it</h2>
 * Waystones has two config rules for bringing animals along - {@code
 * transportLeashed} and {@code transportPets} - and <b>neither can ever move a
 * horse</b>:
 *
 * <ul>
 *   <li>{@code transportPets} scans for {@code net.minecraft.world.entity.TamableAnimal}.
 *       A vanilla horse is not one: {@code AbstractHorse extends Animal implements
 *       PlayerRideableJumping, HasCustomInventoryScreen, OwnableEntity}. Wolves,
 *       cats and parrots are {@code TamableAnimal}; horses, donkeys, mules,
 *       llamas and camels are not. So turning that rule on does nothing
 *       whatsoever for a stable, which is the trap - the setting exists, reads
 *       as if it covers this, and cannot.</li>
 *   <li>{@code transportLeashed} scans for {@code Mob}, so a horse <b>on a
 *       lead</b> does already travel. That half works and this class leaves it
 *       alone.</li>
 * </ul>
 *
 * <p>The gap is the <b>ridden</b> horse. Waystones assembles its teleport batch
 * by walking <i>down</i> from the traveller - {@code getPassengers()} - and never
 * up to {@code getVehicle()}, so the horse a player is sitting on is not in the
 * batch and is left standing at the departure stone while its rider vanishes.
 *
 * <h2>How it is fixed</h2>
 * {@code WaystoneTeleportEvent.Prepare} hands out the {@code
 * WaystoneTeleportContext}, which has {@code addAdditionalEntity} for exactly
 * this. Put the vehicle in the batch and Waystones does the rest: {@code
 * EntityTeleportBatch} already restores riding relationships at the far end with
 * {@code getVehicle} / {@code startRiding}, so the player arrives still mounted
 * rather than in a heap beside the horse.
 *
 * <h2>Why touching this mod's classes is allowed</h2>
 * {@link HorsePoweredCompat} refuses to reach into another mod, and the reason it
 * gives is guessing: a patch written against names nobody promised fails silently
 * the first time they rename something. Waystones publishes {@code
 * net.blay09.mods.waystones.api} and versions it, which is the same deliberate
 * exception {@link JadeHorsePlugin} already makes for Jade. It is
 * <b>compile-only</b>: the jars are never shipped, and {@link Hook} is a separate
 * class so that with Waystones absent nothing in the {@code net.blay09} packages
 * is ever loaded or verified - this class's own guard runs first and the inner one
 * is never touched.
 *
 * <p><b>Not verified in-game.</b> Nothing here has been seen running: it needs a
 * client with Waystones, Balm and Shogi installed, which the dev instance is not.
 * See {@code wiki/compatibility.html}.
 */
public final class WaystonesCompat {

    private static final String WAYSTONES = "waystones";

    /**
     * Register the hook if Waystones is present. Called from the mod
     * constructor; safe to call when it is absent, which is the point of the
     * split.
     */
    public static void init() {
        if (!ModList.get().isLoaded(WAYSTONES)) {
            return;
        }
        try {
            Hook.register();
            HorseGenetics.LOGGER.info("Waystones found - a ridden horse will travel with its rider.");
        } catch (LinkageError | RuntimeException wrongVersion) {
            // A Waystones that moved or renamed the API rather than one that is
            // missing. Loud, because the failure it would otherwise produce is a
            // horse silently left behind, and nobody would connect that to a mod
            // update - exactly the failure mode HorsePoweredCompat's class note
            // is about.
            HorseGenetics.LOGGER.warn("Waystones is installed but its teleport API did not match "
                    + "what this build was compiled against, so a ridden horse will NOT travel "
                    + "with its rider. A horse on a lead still will.", wrongVersion);
        }
    }

    /**
     * Everything that names a Waystones type, kept apart from the guard above so
     * the guard can run without loading any of it.
     */
    private static final class Hook {

        static void register() {
            net.blay09.mods.waystones.api.event.WaystoneTeleportEvent.Prepare.EVENT
                    .register(Hook::onPrepare);
        }

        private static void onPrepare(
                net.blay09.mods.waystones.api.event.WaystoneTeleportEvent.Prepare event) {
            net.blay09.mods.waystones.api.WaystoneTeleportContext context = event.getContext();
            net.minecraft.world.entity.Entity vehicle = context.getEntity().getVehicle();
            // Any AbstractHorse, so donkeys, mules and this mod's own horses all
            // count - and only the direct vehicle, because that is the one the
            // player is actually on. Nothing else nearby is collected: gathering
            // up loose horses is what transportPets is for, and turning that on
            // by proxy is not this class's call to make.
            if (vehicle instanceof net.minecraft.world.entity.animal.equine.AbstractHorse) {
                context.addAdditionalEntity(vehicle);
            }
        }

        private Hook() {
        }
    }

    private WaystonesCompat() {
    }
}
