package com.example.horsegenetics.neoforge.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.EnumSet;

/**
 * <b>Get out of the sun.</b> While a sun-sensitive horse is caught in open
 * daylight it looks for the nearest square that is either under cover or in
 * water, and runs for it - jumping anything in the way it can clear.
 *
 * <p>Written for the dhampir before it was split into loci; the history in the
 * comments below is that animal's, and every fix in it still applies.
 *
 * <p>Runs at the top goal priority. A burning horse has nothing more urgent to
 * be doing, and one that stood still grazing while the sky killed it would
 * read as a bug rather than as a vampire.
 *
 * <p>It sits on <b>every</b> horse and does nothing for one that is not
 * sun-sensitive - see {@link SunSensitivityHandler} for why.
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
public final class SunShadeGoal extends Goal {

    /** How far to look for cover. Beyond this it simply runs, and re-searches. */
    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_HEIGHT = 4;
    /** Faster than a walk - it is on fire. */
    private static final double SPEED = 1.6;
    /** Re-path no more often than this, in ticks. */
    private static final int REPATH_INTERVAL = 20;

    /**
     * <b>How far in from the edge of cover to aim.</b> Owner, 2026-09-13:
     * <i>"it should seek a block that's at least 2 in from each edge."</i>
     *
     * <p>One ring was not enough, and the reason is the horse's own size: it is
     * 1.4 blocks across, the navigator finishes a path within a tolerance
     * rather than on the exact square, and the animal shifts about where it
     * stands. One block of margin absorbs none of that. Two does.
     *
     * <p>Preferred rather than required - less margin still beats standing in
     * the open, and a lean-to that cannot offer two rings is still shelter.
     */
    private static final int PREFERRED_MARGIN = 2;

    private final Horse horse;
    private BlockPos shelter;
    private int repathCooldown;

    /**
     * Squares the navigator has refused this journey. Cleared on {@link #stop},
     * so a spot that was blocked by a cow once is reconsidered next time - the
     * memory is meant to stop a single journey deadlocking, not to write a
     * square off for the life of the horse.
     */
    private final java.util.Set<BlockPos> unreachable = new java.util.HashSet<>();

    public SunShadeGoal(Horse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    /**
     * Resolved once and kept - {@code canUse} runs every tick on every horse, and
     * a genome parse per tick per horse is the cost {@code FoodTemptGoal} already
     * learned to avoid. Lazy because the record is written a tick after join.
     */
    private Boolean sensitive;

    private boolean sensitive() {
        if (sensitive == null) {
            if (!HorseRecords.hasRealRecord(horse)) {
                return false;   // ask again next tick
            }
            sensitive = SunSensitivityHandler.isSensitive(horse);
        }
        return sensitive;
    }

    @Override
    public boolean canUse() {
        if (!sensitive()) {
            return false;
        }
        if (horse.isVehicle() || horse.isLeashed()) {
            return false;   // a ridden or tied horse is the rider's problem
        }
        if (!SunSensitivityHandler.inSunlight(horse)) {
            return false;
        }
        shelter = findShelter();
        return shelter != null;
    }

    /**
     * <b>Stay until the sun goes off it - not until it first reaches shade.</b>
     *
     * <p>This released as soon as {@code inSunlight} went false, and the trace
     * added on 2026-09-13 is what showed why that is fatal. The goal was
     * working perfectly: <i>heading for -10,163 (6.0 blocks, deep cover)</i>,
     * then <i>stopped at -11,163 - in shade, health 51/66</i>. Three times in a
     * row it found deep cover and got there.
     *
     * <p>And each time, the instant it was sheltered this goal let go of
     * {@code Flag.MOVE}, {@code WaterAvoidingRandomStrollGoal} picked the horse
     * up and walked it back into the sun. It burned on the way out and again on
     * the way back: <b>51, then 48, then 33</b>, losing ground every cycle
     * until it died. Four earlier fixes all aimed at <i>getting</i> it to
     * shade, and getting there was never the problem.
     *
     * <p>So it holds for as long as it is daylight, which keeps the movement
     * flag and keeps the stroll goal off it. A dhampir spends the day under
     * cover and hunts after dark, which is what the animal is meant to do.
     */
    @Override
    public boolean canContinueToUse() {
        if (shelter == null || horse.isVehicle() || horse.isLeashed()) {
            return false;
        }
        return horse.level().isBrightOutside();
    }

    /**
     * <b>Say what it decided, because guessing has now cost four attempts.</b>
     *
     * <p>This gene has been "fixed" four times - the search named blocks a
     * ground pathfinder cannot stand on; the shelter was built out of range;
     * panic held {@code Flag.MOVE} at the same priority and starved the goal;
     * the nearest cover was the roof's lip. Every one of those was real, and
     * every one was arrived at by <em>reading code and reasoning</em>, which is
     * exactly the method this project has spent the day proving unreliable.
     *
     * <p>One line per journey costs nothing and ends the argument: where it is
     * going, how far, whether that spot has cover on all sides, and whether the
     * path was accepted at all. If it dies again, the log will say which of
     * those four is still wrong instead of inviting a fifth guess.
     */
    @Override
    public void start() {
        repathCooldown = 0;
        if (shelter != null && horse.level() instanceof ServerLevel level) {
            boolean accepted = horse.getNavigation().moveTo(
                    shelter.getX() + 0.5, shelter.getY(), shelter.getZ() + 0.5, SPEED);
            ActionTrace.log("sun",ActionTrace.describeShort(horse) + " burning at "
                    + horse.blockPosition().toShortString() + ", heading for "
                    + shelter.toShortString() + " ("
                    + String.format("%.1f", Math.sqrt(horse.blockPosition().distSqr(shelter)))
                    + " blocks, margin " + coverMargin(level, shelter) + "/" + PREFERRED_MARGIN
                    + ", " + (accepted ? "path accepted" : "NO PATH") + ")");
        }
    }

    @Override
    public void stop() {
        if (shelter != null) {
            ActionTrace.log("sun",ActionTrace.describeShort(horse) + " stopped at "
                    + horse.blockPosition().toShortString() + " - "
                    + (SunSensitivityHandler.inSunlight(horse)
                            ? "STILL IN THE SUN, so it gave up rather than arrived"
                            : "in shade, health "
                                    + String.format("%.0f/%.0f", horse.getHealth(), horse.getMaxHealth())));
        }
        shelter = null;
        unreachable.clear();
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (shelter == null) {
            return;
        }
        // SHELTERED: stand still, and keep holding the movement flag. Doing
        // nothing here is the entire point - it is what stops the stroll goal
        // taking over and wandering back out into the daylight.
        if (!SunSensitivityHandler.inSunlight(horse)) {
            horse.getNavigation().stop();
            repathCooldown = REPATH_INTERVAL;
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
            if (!horse.getNavigation().moveTo(
                    shelter.getX() + 0.5, shelter.getY(), shelter.getZ() + 0.5, SPEED)) {
                // UNREACHABLE. Give up on THIS square and look again without it.
                //
                // The owner's description was "the dhampir standing still, and
                // just burning", and the trace had already said why on its very
                // first line - "heading for -10,163 (6.0 blocks, deep cover, NO
                // PATH)". The navigator refused, and the goal simply asked for
                // the same square again twenty ticks later, and again, holding
                // the movement flag the whole time so nothing else could move
                // the horse either. It stood in the sun and burned, which looks
                // exactly like a goal that is not running at all.
                //
                // Refusing the best square and taking the next one is always
                // better than standing on a refusal: shelter you can reach beats
                // shelter you cannot, whatever its margin.
                unreachable.add(shelter);
                shelter = findShelter();
                if (shelter != null) {
                    horse.getNavigation().moveTo(
                            shelter.getX() + 0.5, shelter.getY(), shelter.getZ() + 0.5, SPEED);
                }
            }
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
        boolean bestDeep = false;
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
                    if (!wet && !standable(level, cursor)) {
                        continue;
                    }
                    if (unreachable.contains(cursor)) {
                        continue;   // the navigator has already refused this one
                    }
                    double d = from.distSqr(cursor);
                    // DEEP COVER BEATS NEAR COVER, and that is the whole of
                    // "it's just not moving far enough under cover". The
                    // nearest sheltered block is always the roof's outer LIP -
                    // and a horse that arrives there is standing half in the
                    // sun, because its body is 1.4 blocks across and the
                    // navigator finishes a path within a tolerance rather than
                    // dead on the square. It kept burning, re-looked, found the
                    // same lip it was already standing on, and jittered there
                    // until it died.
                    //
                    // So anything with cover on every side outranks anything
                    // without, however much closer the lip is. Distance only
                    // decides between blocks of the same kind.
                    boolean deep = coverMargin(level, cursor) >= PREFERRED_MARGIN;
                    if (deep != bestDeep ? deep : d < bestDist) {
                        bestDeep = deep;
                        bestDist = d;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }

    /**
     * <b>Shade the horse can actually stand in</b> - which is a stricter test
     * than "air out of the sky", and the difference killed a dhampir.
     *
     * <p>The old test accepted any air block that could not see the sky, over a
     * {@value #SEARCH_HEIGHT}-block vertical span. Under a roof that is
     * <em>most of the column</em>: the air at head height and above is
     * sheltered, closer to the horse's own eye position than the floor is, and
     * therefore wins on distance every time. Then it is handed to a <b>ground
     * pathfinder</b>, which cannot stand in mid-air and either refuses the path
     * or walks somewhere else - so the horse kept burning a few blocks from a
     * roof it had correctly identified.
     *
     * <p>Owner, 2026-09-13: <i>"the dhampir ran to the shelter, but it's still
     * dying. We need a better way to detect 'sun' exposure, not just light
     * exposure."</i> The <em>detection</em> was never the problem -
     * {@link SunSensitivityHandler#inSunlight} has always used
     * {@code canSeeSky} on the eye block, which is sky exposure and not light at
     * all, and it correctly reads false under this roof. What failed was the
     * search: it found real shade and then named a square the animal could not
     * occupy.
     *
     * <p>So: air to stand in, air above it for the horse's head, and something
     * solid underneath. That is the same standard the navigator itself applies,
     * which is the point - a goal that picks destinations its own pathfinder
     * rejects is a goal that reports success and does nothing.
     */
    /**
     * <b>Cover on every side, not just overhead.</b> A block under the outer
     * edge of a roof is sheltered and still a bad place to send a horse: it
     * arrives approximately, it is wider than the block it stands on, and half
     * of it ends up back in the sun. One ring of margin is enough to make
     * "arrived" and "safe" the same thing.
     *
     * <p>Not required, only preferred - a lean-to one block deep is still
     * shelter, and a dhampir that refused it because it was not deep enough
     * would be worse off than one standing on the edge.
     */
    private static int coverMargin(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int ring = 1; ring <= PREFERRED_MARGIN; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    probe.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                    if (level.canSeeSky(probe)) {
                        return ring - 1;
                    }
                }
            }
        }
        return PREFERRED_MARGIN;
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
