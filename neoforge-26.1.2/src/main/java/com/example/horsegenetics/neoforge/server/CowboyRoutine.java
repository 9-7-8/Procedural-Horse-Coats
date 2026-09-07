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
 * <h2>The day: a leash, not a route</h2>
 * He works the outskirts, which is where his barn is. Left alone he would drift
 * outward forever, so there are two pulls inward at different strengths:
 * past {@link #WANDER_RADIUS} he heads toward the <b>village centre</b> - not
 * into it, he pulls up {@link #CENTRE_STANDOFF} short of the bell, so he reads
 * as a man riding the ring road rather than one parked in the square - and past
 * {@link #RETURN_RADIUS} he gives up on the patrol and rides straight home.
 * That second rule is what brings him back after a night spent running: however
 * far the monsters chased him, sunrise puts him on the road to his post.
 *
 * <p>The centre is the village's {@code minecraft:meeting} POI - its bell. That
 * is the one part of a generated village reliably at its middle and reliably
 * findable at runtime, which beats trying to recover the structure's bounding
 * box after the fact.
 *
 * <h2>Running, at any hour</h2>
 * If he is not {@link #sheltered} and something hostile is within
 * {@link #DANGER_RADIUS}, he runs - and that outranks both the patrol and the
 * ride home. He is {@link #sheltered} when he is <b>in the building with the
 * doors shut</b> and nothing hostile in there with him, and while he is
 * sheltered a zombie on the other side of the wall is not his problem.
 *
 * <p><b>Not a night rule</b>, though it used to be, and being one was half the
 * bug: a zombie that walked out of a cave mouth at noon, a skeleton in the shade
 * of the barn, a husk in the open - all of them stood next to a man who ignored
 * them, because the check sat inside an {@code isDarkOutside()} branch. What
 * makes him run is a monster near him and no walls around him, and neither of
 * those has anything to do with the clock.
 *
 * <p>The other half of the bug was {@link #sheltered} itself, which used to be
 * "inside a box around home". He <b>parks</b> at home - {@code ARRIVED} is three
 * blocks and his idle amble is measured from the barn - so for most of every day
 * he was inside that box with the doors standing wide open, counted as safe, and
 * ignored anything that walked up to him. Two things fixed it: shelter now needs
 * the doors <i>shut</i> ({@link CowboyDoors#barnIsShut}), because an open doorway
 * is not a wall; and it uses {@link #SHELTER_RADIUS} rather than the looser
 * {@link #INSIDE_RADIUS} the herd check wants, because "near the barn" and "in
 * the barn" are not the same claim and only one of them stops a zombie.
 *
 * <h2>The night: shelter</h2>
 * At dusk everything goes to the barn and the doors shut behind them. Nothing
 * else about the night is special - if something is after him out there, the
 * rule above already has him running, and it had him running an hour before
 * sunset too.
 */
public final class CowboyRoutine {

    /** How far from the barn he will wander before he starts drifting back. */
    public static final int WANDER_RADIUS = 40;

    /** Past this he abandons the patrol and rides home - the morning-after rule. */
    public static final int RETURN_RADIUS = 64;

    /** He closes on the village centre only until he is this near the bell. */
    public static final int CENTRE_STANDOFF = 22;

    /** How near a monster has to be, when he is not sheltered, to set him running. */
    public static final int DANGER_RADIUS = 12;

    /** How far to look for the village bell from the barn. */
    private static final int BELL_SEARCH = 96;

    /**
     * Half-span of "in the building", around the cowboy's home block.
     *
     * <p>An approximation, and knowingly so: the barn is 15x7 on the ground and
     * the jigsaw can rotate it any of four ways, so the only shape that is
     * rotation-independent is a square. 5 covers the whole 11x5 interior along
     * the short axis with room to spare and most of it along the long one,
     * which is the right way round to be wrong - a horse standing in a far
     * corner reads as "not quite home yet" and gets nudged in, where a box big
     * enough to contain the whole interior would also contain several blocks of
     * open ground outside the walls, and count a zombie out there as a breach.
     */
    private static final int INSIDE_RADIUS = 5;

    /**
     * Half-span of "in the building" for the <b>shelter</b> test, which is a
     * stricter question than the one {@link #INSIDE_RADIUS} answers.
     *
     * <p>That one asks "is this horse home enough to shut the doors on?", and
     * wants to be generous - a horse in a far corner should not hold the barn
     * open. This one asks "is this man behind a wall?", and being generous there
     * is how he ends up ignoring a zombie stood next to him. The barn's interior
     * is only about two blocks either side of centre along its short axis, and
     * he parks within {@link #ARRIVED} of home, so three is the tightest box that
     * still reliably contains a parked cowboy.
     */
    private static final int SHELTER_RADIUS = 3;

    /** How far above and below home "in the building" reaches. */
    private static final int INSIDE_HEIGHT = 3;

    /** Close enough to a destination to count as arrived. */
    public static final double ARRIVED = 3.0;

    private static final Predicate<Holder<PoiType>> MEETING = holder -> holder.is(PoiTypes.MEETING);

    private CowboyRoutine() {
    }

    /** What he is doing. The mount goal picks its speed and its target from this. */
    public enum Duty {
        /** Heading for the barn, or already in it. */
        SHELTER,
        /** Working his patch by day. */
        PATROL,
        /** Out in the open with something after him. */
        FLEE
    }

    /**
     * @param duty  what he is doing
     * @param goal  where to go, or empty when he is already where he wants to be
     *              (for {@link Duty#FLEE} this is always empty - the mount goal
     *              picks a direction away from {@link #threat})
     * @param threat the monster to run from, only ever set for {@link Duty#FLEE}
     */
    public record Plan(Duty duty, Optional<BlockPos> goal, @Nullable Monster threat) {
        static Plan going(Duty duty, BlockPos target) {
            return new Plan(duty, Optional.of(target), null);
        }

        static Plan arrived(Duty duty) {
            return new Plan(duty, Optional.empty(), null);
        }
    }

    /**
     * @param from where he actually is - his mount's position while he is
     *             riding, which is a block or two off his own
     */
    public static Plan plan(Cowboy cowboy, ServerLevel level, BlockPos from) {
        BlockPos barn = cowboy.home().orElse(from);

        // Out in the open with something hostile near him: first question asked,
        // whatever the hour. See the class comment.
        if (!sheltered(level, barn, from)) {
            Monster threat = nearestMonster(level, from, DANGER_RADIUS);
            if (threat != null) {
                return new Plan(Duty.FLEE, Optional.empty(), threat);
            }
        }

        if (level.isDarkOutside()) {
            return from.closerThan(barn, ARRIVED)
                    ? Plan.arrived(Duty.SHELTER)
                    : Plan.going(Duty.SHELTER, barn);
        }

        // Daylight. Strayed a long way - a night of running, most likely - so
        // the patrol waits and he rides for his post.
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

    /** In the barn, doors shut, nothing hostile in there with him. */
    public static boolean sheltered(ServerLevel level, BlockPos barn, BlockPos from) {
        if (!within(barn, from, SHELTER_RADIUS)) {
            return false;
        }
        if (!CowboyDoors.barnIsShut(level, barn)) {
            return false; // an open doorway is not a wall - see the class comment
        }
        return level.getEntitiesOfClass(Monster.class, barnBox(barn)).isEmpty();
    }

    /** Home enough to count toward "the whole string is in". */
    public static boolean insideBarn(BlockPos barn, BlockPos pos) {
        return within(barn, pos, INSIDE_RADIUS);
    }

    private static boolean within(BlockPos barn, BlockPos pos, int radius) {
        return Math.abs(pos.getX() - barn.getX()) <= radius
                && Math.abs(pos.getZ() - barn.getZ()) <= radius
                && Math.abs(pos.getY() - barn.getY()) <= INSIDE_HEIGHT;
    }

    private static AABB barnBox(BlockPos barn) {
        return new AABB(barn).inflate(INSIDE_RADIUS, INSIDE_HEIGHT, INSIDE_RADIUS);
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
