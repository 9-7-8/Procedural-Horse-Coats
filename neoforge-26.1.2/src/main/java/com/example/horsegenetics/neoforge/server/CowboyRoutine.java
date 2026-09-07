package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * <b>What the cowboy is trying to do right now.</b> One answer, asked by both
 * goals - {@code CowboyMountGoal} steers his horse by it, {@code CowboyHerdGoal}
 * keeps the rest of the herd on whatever he settled on - so the man and his
 * animals cannot disagree about the plan.
 *
 * <h2>This is a daytime routine, and only a daytime routine</h2>
 * He is only mounted by day. At dusk {@link CowboyHandler} takes him off his
 * horse and he spends the night as an ordinary villager running ordinary
 * villager goals; at first light {@link CowboyRemountGoal} walks him back to it.
 * So there is no night duty here, no barn to shelter in, and no doors.
 *
 * <p><b>There used to be all three</b>, and removing them is the point of the
 * change that halved this file. The design was that he rode home at dusk, opened
 * four double doors, waited for eleven horses to file through a two-block gap
 * and shut up behind them. Every part of that worked in isolation and the whole
 * never did: a large horse cannot path through a two-block doorway at all, a
 * villager standing inside is a cork, the herd shoved each other back out of the
 * entrance, and each fix for one of those exposed the next. The owner's call,
 * after a day of it, was that a mounted man's bedtime is not worth a subsystem -
 * he gets ten times the health instead, and the night stops being a problem that
 * needs solving. See {@code wiki/known-gaps.html}.
 *
 * <h2>The day: a leash, not a route</h2>
 * He works the outskirts, which is where his barn is. Left alone he would drift
 * outward forever, so there are two pulls inward at different strengths: past
 * {@link #WANDER_RADIUS} he heads toward the <b>village centre</b> - not into
 * it, he pulls up {@link #CENTRE_STANDOFF} short of the bell, so he reads as a
 * man riding the ring road rather than one parked in the square - and past
 * {@link #RETURN_RADIUS} he gives up on the patrol and rides straight home.
 *
 * <p>The centre is the village's {@code minecraft:meeting} POI - its bell. That
 * is the one part of a generated village reliably at its middle and reliably
 * findable at runtime, which beats trying to recover the structure's bounding
 * box after the fact.
 *
 * <h2>Running</h2>
 * A monster within {@link #DANGER_RADIUS} and he gets away from it, and that
 * outranks the patrol. Not a night rule - a zombie that walks out of a cave
 * mouth at noon is the same problem - and a proximity rule rather than a damage
 * one: he should be leaving before anything has touched him.
 */
public final class CowboyRoutine {

    /** How far from the barn he will wander before he starts drifting back. */
    public static final int WANDER_RADIUS = 40;

    /** Past this he abandons the patrol and rides home. */
    public static final int RETURN_RADIUS = 64;

    /** He closes on the village centre only until he is this near the bell. */
    public static final int CENTRE_STANDOFF = 22;

    /**
     * How near a monster has to be to set him moving.
     *
     * <p>Deliberately further than a monster can reach him: he should be leaving
     * before anything has touched him, which is what a man on a horse would do
     * and what makes him read as wary rather than as slow to react.
     */
    public static final int DANGER_RADIUS = 16;

    /**
     * How far a monster has to get before he stops treating it as a threat.
     *
     * <p><b>Hysteresis, and the reason he used to twitch.</b> With one radius,
     * crossing it in either direction flips the plan - so he bolted twelve
     * blocks, stopped dead the instant he was clear, turned round, ambled back
     * towards the thing chasing him and bolted again. A short dash repeated
     * forever reads as "he barely flees". Leaving needs {@link #DANGER_RADIUS};
     * settling needs this.
     */
    public static final int SAFE_RADIUS = 28;

    /** How far to look for the village bell from the barn. */
    private static final int BELL_SEARCH = 96;

    /**
     * Half-span of the box that counts as "at the barn", around his home block.
     *
     * <p>An approximation, and knowingly so: the barn is 15x7 on the ground and
     * the jigsaw can rotate it any of four ways, so the only shape that is
     * rotation-independent is a square.
     */
    private static final int INSIDE_RADIUS = 5;

    /** How far above and below home "at the barn" reaches. */
    private static final int INSIDE_HEIGHT = 3;

    /** Close enough to a destination to count as arrived. */
    public static final double ARRIVED = 3.0;

    private static final Predicate<Holder<PoiType>> MEETING = holder -> holder.is(PoiTypes.MEETING);

    private CowboyRoutine() {
    }

    /** What he is doing. The mount goal picks its speed and its target from this. */
    public enum Duty {
        /** Working his patch. */
        PATROL,
        /** Something hostile is near him. */
        FLEE
    }

    /**
     * @param duty  what he is doing
     * @param goal  where to go, or empty when he is already where he wants to be
     *              (for {@link Duty#FLEE} this is always empty - the mount goal
     *              picks a direction away from {@link #threat})
     * @param threat the monster he is getting away from, or {@code null}
     */
    public record Plan(Duty duty, Optional<BlockPos> goal, @Nullable Monster threat) {
        static Plan going(Duty duty, BlockPos target) {
            return new Plan(duty, Optional.of(target), null);
        }

        static Plan arrived(Duty duty) {
            return new Plan(duty, Optional.empty(), null);
        }

        /** Is he moving because of something, rather than merely moving? */
        public boolean urgent() {
            return threat != null;
        }
    }

    /**
     * @param from where he actually is - his mount's position while he is
     *             riding, which is a block or two off his own
     * @param wasFleeing was the last plan a {@link Plan#urgent() urgent} one?
     *             Only the hysteresis reads it - see {@link #SAFE_RADIUS}. The
     *             mount goal remembers it, because the routine itself is
     *             deliberately stateless.
     */
    public static Plan plan(Cowboy cowboy, ServerLevel level, BlockPos from, boolean wasFleeing) {
        BlockPos barn = cowboy.home().orElse(from);

        Monster threat = nearestMonster(level, from, wasFleeing ? SAFE_RADIUS : DANGER_RADIUS);
        if (threat != null) {
            return new Plan(Duty.FLEE, Optional.empty(), threat);
        }

        // Strayed a long way, so the patrol waits and he rides for his post.
        if (!from.closerThan(barn, RETURN_RADIUS)) {
            return Plan.going(Duty.PATROL, barn);
        }
        if (from.closerThan(barn, WANDER_RADIUS)) {
            return Plan.arrived(Duty.PATROL); // inside his patch: free to wander
        }

        BlockPos centre = villageCentre(level, barn).orElse(barn);
        if (from.closerThan(centre, CENTRE_STANDOFF)) {
            return Plan.arrived(Duty.PATROL);
        }
        return Plan.going(Duty.PATROL, standoffPoint(from, centre));
    }

    /** Near enough to his home block to count as "at the barn". */
    public static boolean insideBarn(BlockPos barn, BlockPos pos) {
        return Math.abs(pos.getX() - barn.getX()) <= INSIDE_RADIUS
                && Math.abs(pos.getZ() - barn.getZ()) <= INSIDE_RADIUS
                && Math.abs(pos.getY() - barn.getY()) <= INSIDE_HEIGHT;
    }

    private static @Nullable Monster nearestMonster(ServerLevel level, BlockPos from, int radius) {
        Monster nearest = null;
        double best = Double.MAX_VALUE;
        for (Monster monster : level.getEntitiesOfClass(Monster.class, new AABB(from).inflate(radius))) {
            if (!monster.isAlive()) {
                continue;
            }
            double distance = monster.blockPosition().distSqr(from);
            if (distance < best) {
                best = distance;
                nearest = monster;
            }
        }
        return nearest;
    }

    /**
     * The village's bell, which is the origin of his whole map - the patrol
     * measures its standoff from it, and the founding code takes the bearing
     * from it to the barn as "which way is out of town".
     */
    public static Optional<BlockPos> villageCentre(ServerLevel level, BlockPos barn) {
        return level.getPoiManager().findClosest(MEETING, barn, BELL_SEARCH, PoiManager.Occupancy.ANY);
    }

    /**
     * A point on the line from {@code from} to {@code centre}, stopping
     * {@link #CENTRE_STANDOFF} blocks short of it. Riding to the bell and then
     * turning round reads as an errand; stopping at the edge of the square
     * reads as a man keeping the town in sight.
     */
    private static BlockPos standoffPoint(BlockPos from, BlockPos centre) {
        double dx = centre.getX() - from.getX();
        double dz = centre.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= CENTRE_STANDOFF) {
            return centre;
        }
        double travel = distance - CENTRE_STANDOFF;
        return from.offset(new Vec3i(
                Mth.floor(dx / distance * travel),
                0,
                Mth.floor(dz / distance * travel)));
    }
}
