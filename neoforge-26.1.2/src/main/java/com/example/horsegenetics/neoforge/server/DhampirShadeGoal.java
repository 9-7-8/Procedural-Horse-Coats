package com.example.horsegenetics.neoforge.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.EnumSet;

/**
 * <b>Get out of the sun.</b> While a dhampir is caught in open daylight it
 * looks for the nearest square that is either under cover or in water, and runs
 * for it - jumping anything in the way it can clear.
 *
 * <p>Runs at the top goal priority. A burning horse has nothing more urgent to
 * be doing, and a dhampir that stood still grazing while the sky killed it would
 * read as a bug rather than as a vampire.
 *
 * <h4>Fences</h4>
 * There is no pathfinding-with-jumps in vanilla horse AI, so this does the
 * simplest honest thing: while it is running for shelter and its horizontal
 * movement is blocked, it presses jump. A horse's jump strength is a real stat
 * with a real range, so a strong one clears a fence and a Falabella does not -
 * which is the correct answer to "up to and including jumping fences if they
 * can". It is an approximation of intent, not a pathfinder, and it is flagged
 * as such in {@code wiki/verification.html}.
 */
public final class DhampirShadeGoal extends Goal {

    /** How far to look for cover. Beyond this it simply runs, and re-searches. */
    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_HEIGHT = 4;
    /** Faster than a walk - it is on fire. */
    private static final double SPEED = 1.6;
    /** Re-path no more often than this, in ticks. */
    private static final int REPATH_INTERVAL = 20;

    private final Horse horse;
    private BlockPos shelter;
    private int repathCooldown;

    public DhampirShadeGoal(Horse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (horse.isVehicle() || horse.isLeashed()) {
            return false;   // a ridden or tied horse is the rider's problem
        }
        if (!DhampirHandler.inSunlight(horse)) {
            return false;
        }
        shelter = findShelter();
        return shelter != null;
    }

    @Override
    public boolean canContinueToUse() {
        return shelter != null && !horse.isVehicle() && DhampirHandler.inSunlight(horse);
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void stop() {
        shelter = null;
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (shelter == null) {
            return;
        }
        if (--repathCooldown <= 0) {
            repathCooldown = REPATH_INTERVAL;
            // Re-look as it moves: a horse that runs past a doorway should take
            // the doorway rather than keep going to the tree it first saw.
            BlockPos nearer = findShelter();
            if (nearer != null) {
                shelter = nearer;
            }
            horse.getNavigation().moveTo(shelter.getX() + 0.5, shelter.getY(), shelter.getZ() + 0.5, SPEED);
        }
        horse.getLookControl().setLookAt(shelter.getX() + 0.5, shelter.getY() + 1.0, shelter.getZ() + 0.5);
        if (horse.horizontalCollision && horse.onGround()) {
            horse.getJumpControl().jump();   // a fence, a wall, a step - try it
        }
    }

    /**
     * The nearest position that is out of the sky or in water. Searched as a
     * widening box rather than by picking the single best square: the goal is
     * "get under something", and the first thing found by increasing distance is
     * the right answer to that.
     */
    private BlockPos findShelter() {
        if (!(horse.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos from = horse.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dy = -SEARCH_HEIGHT; dy <= SEARCH_HEIGHT; dy++) {
                    cursor.set(from.getX() + dx, from.getY() + dy, from.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    boolean sheltered = !level.canSeeSky(cursor);
                    boolean wet = level.getFluidState(cursor).isSource();
                    if (!sheltered && !wet) {
                        continue;
                    }
                    if (!wet && !level.getBlockState(cursor).isAir()) {
                        continue;   // shade you can actually stand in
                    }
                    double d = from.distSqr(cursor);
                    if (d < bestDist) {
                        bestDist = d;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }
}
