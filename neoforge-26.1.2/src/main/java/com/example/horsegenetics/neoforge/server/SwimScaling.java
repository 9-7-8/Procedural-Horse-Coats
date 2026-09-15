package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * <b>Magic swim speed, as a nudge on the horse's own swimming</b> (owner, 2026-09-15, gap 249).
 *
 * <p>The gene used to multiply {@code water_movement_efficiency}, whose base is 0, so it did nothing. It now scales the
 * push the horse makes through the water each tick - not its whole velocity, which would compound tick on tick into a
 * horse that accelerates for ever.
 *
 * <p>Vanilla swimming is, per tick, {@code v = drag * (v_before + push)}. So this tick's push is
 * {@code v / drag - v_before}, and adding {@code (factor - 1) * drag * push} to the stored velocity makes the steady
 * swimming speed exactly {@code factor} times what it would have been, in either direction. It is horizontal only, so
 * surfacing and diving are untouched. It runs only in water, not lava, and <b>only on the side that moves the horse</b>
 * ({@code canSimulateMovement}): the server for a loose horse, the rider's own client for a ridden one, which the server
 * does not simulate. Both sides call this - {@code GeneAbilityHandler} on the server, {@code ClientSwimHandler} on the
 * client.
 *
 * <p>UNVERIFIED: {@link #WATER_DRAG} is vanilla's default {@code getWaterSlowDown()} of 0.8. If horses in 26.1.2 use
 * another value, the scaling is approximate rather than exact; the yard's SWIM SPEED pen measures it.
 */
public final class SwimScaling {

    private SwimScaling() {
    }

    static final double WATER_DRAG = 0.8;
    /** Vanilla's in-water drag while sprinting (a ridden horse at a gallop). */
    static final double SPRINT_DRAG = 0.9;
    /** A backstop, in blocks per tick: whatever the estimate gets wrong, no swimmer leaves at a sprint's pace and more. */
    private static final double MAX_SPEED = 1.0;
    private static final double MIN_FACTOR = 0.05;

    /**
     * Last tick's (already scaled) velocity per horse. Two maps, because single-player runs the client and the
     * integrated server in one process and a WeakHashMap is not safe to share across their threads.
     */
    private static final Map<Horse, Vec3> SERVER_LAST = new WeakHashMap<>();
    private static final Map<Horse, Vec3> CLIENT_LAST = new WeakHashMap<>();

    /** Call once per tick, after the horse has moved. */
    public static void apply(Horse horse, double factor) {
        Map<Horse, Vec3> last = horse.level().isClientSide() ? CLIENT_LAST : SERVER_LAST;
        if (!horse.canSimulateMovement() || !horse.isInWater() || horse.isInLava() || factor == 1.0) {
            last.remove(horse);
            return;
        }
        Vec3 v = horse.getDeltaMovement();
        Vec3 before = last.get(horse);
        if (before != null) {
            // The drag vanilla actually used this tick. With the wrong one, part of the drag is left inside the "push",
            // and scaling that turns drag into thrust: at 0.8 assumed and 0.9 real, an Otter at factor 2 would never
            // stop speeding up.
            double drag = horse.isSprinting() ? SPRINT_DRAG : WATER_DRAG;
            double f = Math.max(MIN_FACTOR, factor);
            double pushX = v.x / drag - before.x;
            double pushZ = v.z / drag - before.z;
            double nx = v.x + (f - 1.0) * drag * pushX;
            double nz = v.z + (f - 1.0) * drag * pushZ;
            double h = Math.sqrt(nx * nx + nz * nz);
            if (h > MAX_SPEED) {
                nx *= MAX_SPEED / h;
                nz *= MAX_SPEED / h;
            }
            v = new Vec3(nx, v.y, nz);
            horse.setDeltaMovement(v);
        }
        last.put(horse, v);
    }
}
