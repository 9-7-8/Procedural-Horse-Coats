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
 * <h2>Where the horse lands</h2>
 * A waystone's arrival spot is one block in front of the stone, chosen for a
 * player standing up: two blocks of air and no more. A horse is wider than a
 * player and this mod's horses can be <i>taller</i> - a Shire at the top of the
 * size loci is a different animal from a Falabella - so a spot that fits the
 * rider can bury the horse in the wall of whatever the stone is built against,
 * and a buried horse suffocates.
 *
 * <p>Owner, 2026-09-29: <i>"find the nearest open 3x3x3 of pure air blocks to
 * put the player on the horse in, even if that's up in the air, to prevent the
 * horse suffocating in a wall."</i> {@link Hook#onTeleportEntity} does exactly
 * that on {@code WaystoneTeleportEntityEvent.Pre}, which carries
 * {@code setTargetPosition}.
 *
 * <p>Three things about it are deliberate:
 * <ul>
 *   <li><b>Only when a horse is in the batch.</b> An ordinary waystone hop is
 *       left exactly where Waystones put it. Relocating every arrival on the
 *       server would be a much larger claim than this class is making.</li>
 *   <li><b>Nothing moves if the spot is already clear.</b> The search starts by
 *       testing the arrival Waystones chose, so the usual case - a stone in the
 *       open - costs a couple of dozen block reads and changes nothing.</li>
 *   <li><b>Air, not "not solid".</b> The owner said pure air and that is what
 *       is tested. Water, a carpet, tall grass and a snow layer are all things
 *       a horse can be put down in and none of them are what was asked for,
 *       and none of them are unambiguously safe for an animal that cannot
 *       swim upward out of a one-block hole.</li>
 * </ul>
 * Rider and horse are both moved, by the same function of the same input, so
 * they arrive in the same place and Waystones' own remount at the far end still
 * works.
 *
 * <p>{@link #nearestClearance} deliberately sits <b>outside</b> {@link Hook},
 * unlike everything else this fix needed. It names no Waystones type - a
 * {@code BlockGetter} and a {@code BlockPos} - and the rule about Hook is that
 * nothing in it may be loaded without Waystones present. Keeping the one piece
 * with real logic in it out here means it can be read, and tested, by anything.
 *
 * <p><b>Not verified in-game.</b> Nothing here has been seen running: it needs a
 * client with Waystones, Balm and Shogi installed, which the dev instance is not.
 * See {@code wiki/compatibility.html}.
 */
public final class WaystonesCompat {

    private static final String WAYSTONES = "waystones";

    /**
     * How far from the waystone's own arrival spot the search is willing to
     * look, in blocks, on every axis. Eight is far enough to get out of the
     * building a stone is usually inside and close enough that the arrival is
     * still recognisably at the waystone - past that a player would rather be
     * told the spot is bad than be put somewhere they have to walk back from.
     */
    static final int SEARCH_RADIUS = 8;

    /** The clear space a horse is given: 3 wide, 3 tall, 3 deep. Owner's number. */
    static final int CLEARANCE = 3;

    /**
     * <b>The nearest block a horse can stand on with air all round it.</b>
     * {@code from} itself is tested first and returned unchanged when it is
     * already clear, so the ordinary arrival costs 27 block reads and moves
     * nobody.
     *
     * <p>Searched by <b>shell</b> outwards rather than by scanning the whole
     * box, so the common case - the spot one block over - stops almost at
     * once instead of reading {@code (2r+1)^3 * 27} states. Two shells are
     * finished before returning, because a Chebyshev shell is not a sphere:
     * a corner of shell {@code r} is {@code r}&#8730;3 away and can be
     * further off than a face of shell {@code r+1}. Within the shells
     * examined the true Euclidean nearest is what comes back.
     *
     * <p>No ground requirement, on purpose - <i>"even if that's up in the
     * air"</i>. A horse that falls two blocks is a horse; a horse in a wall
     * is a corpse.
     *
     * <p><b>The build-height check is not a tidiness check.</b> Below the
     * world {@code getBlockState} answers {@code void_air}, which
     * {@code isAir()} says yes to - so a waystone near bedrock would
     * otherwise find a perfectly "clear" spot eight blocks under the floor
     * of the world and drop the horse out of it.
     *
     * <p>Public only so {@code gametest/ModGameTests} can measure it against an
     * exhaustive scan of the same box. Nothing else calls it.
     *
     * @return the block the rider's feet go in, or {@code null} if nothing
     *         within {@link #SEARCH_RADIUS} is clear
     */
    public static net.minecraft.core.BlockPos nearestClearance(
            net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos from) {
        net.minecraft.core.BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        int stopAfter = Integer.MAX_VALUE;
        for (int r = 0; r <= SEARCH_RADIUS && r <= stopAfter; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        // The shell only: everything nearer was done on an
                        // earlier pass.
                        if (Math.abs(dx) != r && Math.abs(dy) != r && Math.abs(dz) != r) {
                            continue;
                        }
                        long distance = (long) dx * dx + (long) dy * dy + (long) dz * dz;
                        if (distance >= bestDistance) {
                            continue;
                        }
                        net.minecraft.core.BlockPos candidate = from.offset(dx, dy, dz);
                        if (isClear(level, candidate)) {
                            best = candidate;
                            bestDistance = distance;
                        }
                    }
                }
            }
            if (best != null && stopAfter == Integer.MAX_VALUE) {
                stopAfter = r + 1;
            }
        }
        return best;
    }

    /**
     * Is the {@link #CLEARANCE} cube standing on {@code feet} entirely air?
     * The cube is centred on the feet block horizontally and rises from it,
     * so what is being asked is "can something 3 wide and 3 tall stand
     * here", which is the shape of the problem.
     */
    private static boolean isClear(net.minecraft.world.level.BlockGetter level,
                                   net.minecraft.core.BlockPos feet) {
        if (level.isOutsideBuildHeight(feet.getY())
                || level.isOutsideBuildHeight(feet.getY() + CLEARANCE - 1)) {
            return false;
        }
        int half = CLEARANCE / 2;
        net.minecraft.core.BlockPos.MutableBlockPos cursor =
                new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int y = 0; y < CLEARANCE; y++) {
            for (int x = -half; x <= half; x++) {
                for (int z = -half; z <= half; z++) {
                    cursor.set(feet.getX() + x, feet.getY() + y, feet.getZ() + z);
                    if (!level.getBlockState(cursor).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

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
            net.blay09.mods.waystones.api.event.WaystoneTeleportEntityEvent.Pre.EVENT
                    .register(Hook::onTeleportEntity);
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

        /**
         * <b>Do not put the horse inside the wall.</b> Fired once per entity in
         * the batch; the rider and the horse each go through it and each get
         * the same answer, because the answer is a pure function of the target
         * position Waystones handed in.
         *
         * <p>Deliberately <b>not</b> cancelling or overriding the result on
         * failure: if nothing within {@link WaystonesCompat#SEARCH_RADIUS} is clear the
         * teleport goes ahead at Waystones' own spot, which is what would have
         * happened without this mod. A waystone that cannot be arrived at is a
         * worse bug than a horse in a tight spot, and this class is not
         * entitled to decide somebody's trip does not happen.
         */
        private static void onTeleportEntity(
                net.blay09.mods.waystones.api.event.WaystoneTeleportEntityEvent.Pre event) {
            if (!batchHasHorse(event.getContext())) {
                return;
            }
            net.minecraft.server.level.ServerLevel level = event.getTargetLevel();
            net.minecraft.world.phys.Vec3 target = event.getTargetPosition();
            net.minecraft.core.BlockPos clear = nearestClearance(
                    level, net.minecraft.core.BlockPos.containing(target));
            if (clear == null) {
                HorseGenetics.LOGGER.debug("Waystone arrival at {} has no {}x{}x{} of air within "
                                + "{} blocks; the horse is going there anyway.",
                        target, CLEARANCE, CLEARANCE, CLEARANCE, SEARCH_RADIUS);
                return;
            }
            // Centre of the block horizontally, and standing on its floor. The
            // x/z are re-centred even when the block did not move, because
            // Waystones' own spot can be off-centre and half a block of a
            // 3-wide space is not worth giving away.
            event.setTargetPosition(new net.minecraft.world.phys.Vec3(
                    clear.getX() + 0.5, clear.getY(), clear.getZ() + 0.5));
        }

        /**
         * Is there a horse in this batch at all? Three places one can be: the
         * entity being moved, the vehicle {@link #onPrepare} put in, and a horse
         * on a lead, which Waystones collects by itself and which has exactly
         * the same problem on arrival.
         */
        private static boolean batchHasHorse(
                net.blay09.mods.waystones.api.WaystoneTeleportContext context) {
            if (context.getEntity() instanceof net.minecraft.world.entity.animal.equine.AbstractHorse
                    || context.getEntity().getVehicle()
                            instanceof net.minecraft.world.entity.animal.equine.AbstractHorse) {
                return true;
            }
            for (net.minecraft.world.entity.Entity extra : context.getAdditionalEntities()) {
                if (extra instanceof net.minecraft.world.entity.animal.equine.AbstractHorse) {
                    return true;
                }
            }
            for (net.minecraft.world.entity.Mob led : context.getLeashedEntities()) {
                if (led instanceof net.minecraft.world.entity.animal.equine.AbstractHorse) {
                    return true;
                }
            }
            return false;
        }

        private Hook() {
        }
    }

    private WaystonesCompat() {
    }
}
