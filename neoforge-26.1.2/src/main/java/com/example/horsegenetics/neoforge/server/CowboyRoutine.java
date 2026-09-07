package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * {@link #DANGER_RADIUS}, he gets away from it, and that outranks the patrol.
 * He is {@link #sheltered} when he is <b>in the building with the doors
 * shut</b> and nothing hostile in there with him, and while he is sheltered a
 * zombie on the other side of the wall is not his problem.
 *
 * <p><b>Getting away is not always running away.</b> After dark the barn is the
 * best cover there is, so a threat makes him ride <i>for it</i>, hard - the
 * {@link Duty#SHELTER} plan simply carries the {@link Plan#threat} and the mount
 * goal moves at its bolting speed. Only when home is no use - no home, too far
 * to reach, or something already inside it - does he run blind.
 *
 * <p>That distinction is the fix for a bug that read as two: an unconditional
 * "monster near, therefore {@link Duty#FLEE}" is <i>always</i> true on an open
 * plain at night, so the ride home never got a look in. He never went to his
 * barn, and what running he did was a dash away from one zombie straight
 * towards the next. Both symptoms, one cause.
 *
 * <p>By <b>day</b> there is nothing to shut himself into - the barn stands open
 * from dawn - so a daytime threat is plain {@link Duty#FLEE}: he keeps his
 * distance and gets on with it when the thing has gone.
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

    /**
     * How near a monster has to be, when he is not sheltered, to set him moving.
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
     * forever reads as "he barely flees". Leaving needs
     * {@link #DANGER_RADIUS}; settling needs this.
     */
    public static final int SAFE_RADIUS = 28;

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
     * <p>That one asks "is this horse home enough to shut the doors on?" and
     * wants to be generous. This one asks "is this man behind a wall?" - and it
     * only has to contain a cowboy standing anywhere inside his own barn, which
     * since he started parking at {@link #FAR_END} means most of the long axis.
     *
     * <p>Being generous here used to be dangerous, because "inside a box around
     * home" was the whole of the test and a man parked <i>outside</i> his barn
     * passed it. It is not any more: {@link #sheltered} also requires the doors
     * to be shut, and they are only ever shut at night with him inside. The box
     * is the loose half of an and.
     */
    private static final int SHELTER_RADIUS = 6;

    /**
     * How far up the barn he parks from its centre, toward the far end.
     *
     * <p>He goes <b>nearly all the way to the back</b>. The doorway is the scarce
     * thing in this building - eleven horses have to come through a two-block gap
     * - and a man sitting on a horse in the middle of the floor is one horse's
     * worth of the room they need. The interior is about eleven blocks along its
     * long axis, so four from the middle is a block short of the far wall.
     *
     * <p>Four and not five, and that block of margin is load-bearing: everything
     * else about the night is measured from the barn's <i>centre</i> with
     * {@link #INSIDE_RADIUS}, and a stall parked exactly on that radius is a
     * cowboy who is at home for one test and not at home for the next.
     */
    private static final int FAR_END = 4;

    /** How far in front of the barn's doors villagers are moved at dusk. */
    private static final int FRONT_STANDOFF = 6;

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
     * @param threat the monster he is getting away from, or {@code null}. Set on
     *              {@link Duty#FLEE} always, and on {@link Duty#SHELTER} when the
     *              ride home <i>is</i> the escape - which is what tells the mount
     *              goal to make it at a gallop rather than a walk.
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
        boolean dark = level.isDarkOutside();

        // Something hostile, and no walls: the first question asked, whatever the
        // hour. What he does about it is the second question, and it has two
        // different answers - see the class comment.
        Monster threat = sheltered(level, barn, from)
                ? null
                : nearestMonster(level, from, wasFleeing ? SAFE_RADIUS : DANGER_RADIUS);

        if (dark) {
            // Home is the answer to the night, threat or no threat - and a threat
            // only makes him ride for it harder. He runs blind only when home is
            // no use to him.
            if (threat != null && !barnIsRefuge(level, cowboy, barn, from)) {
                return new Plan(Duty.FLEE, Optional.empty(), threat);
            }
            BlockPos stall = farEndOf(level, barn);
            return new Plan(Duty.SHELTER,
                    from.closerThan(stall, ARRIVED) ? Optional.empty() : Optional.of(stall),
                    threat);
        }

        // Daylight: the barn stands open, so there is nothing to shut himself
        // into and keeping his distance is the whole of the plan.
        if (threat != null) {
            return new Plan(Duty.FLEE, Optional.empty(), threat);
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

    /**
     * The back of the barn - where he parks, so the doorway and the floor in
     * front of it are left for the string.
     *
     * <p>Found by <b>bearing</b> and not by local coordinates, because the jigsaw
     * rotates the building any of four ways and nothing at runtime knows which.
     * The barn's road-facing end is the end its connector is on, and that
     * connector attaches to a village street - so the direction from the village
     * bell to the barn is the direction from its door end to its far end, and
     * {@link #FAR_END} blocks along it is the back wall.
     *
     * <p>No bell, no bearing: he parks in the middle, which is where he used to
     * park anyway.
     */
    public static BlockPos farEndOf(ServerLevel level, BlockPos barn) {
        Direction axis = longAxis(level, barn);
        return axis == null ? barn : barn.relative(axis, FAR_END);
    }

    /** Open ground in front of the barn's doors - where villagers in the way get put. */
    public static BlockPos frontOf(ServerLevel level, BlockPos barn) {
        Direction axis = longAxis(level, barn);
        return axis == null ? barn : barn.relative(axis.getOpposite(), FRONT_STANDOFF);
    }

    /**
     * Which way the barn's long axis runs: from its door end toward its back
     * wall, as one of the four cardinals.
     *
     * <p><b>Snapped to a cardinal, and that is the whole point.</b> The bearing
     * from the bell to the barn is a real angle, and using it raw walked the
     * cowboy diagonally: five blocks along the length <i>and</i> two across the
     * width, into a corner six blocks from the middle of a building whose middle
     * is the only thing the rest of the night measures from. He parked there, the
     * "is he home?" test said no, the door timer reset every tick and the barn
     * stood open all night with him wedged against the far wall. The building is
     * axis-aligned however the jigsaw turned it, so the answer has to be too.
     */
    private static @Nullable Direction longAxis(ServerLevel level, BlockPos barn) {
        Optional<BlockPos> centre = villageCentre(level, barn);
        if (centre.isEmpty()) {
            return null;
        }
        int dx = barn.getX() - centre.get().getX();
        int dz = barn.getZ() - centre.get().getZ();
        if (dx == 0 && dz == 0) {
            return null;
        }
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /**
     * Is riding home a way out of this, or just a way to somewhere else?
     *
     * <p>Three things have to hold: he has a barn at all, it is near enough to
     * be worth crossing open ground for, and there is nothing hostile in it
     * already. The third is the one that matters - galloping home into a barn a
     * zombie is standing in is the worst of both plans.
     */
    private static boolean barnIsRefuge(ServerLevel level, Cowboy cowboy, BlockPos barn, BlockPos from) {
        return cowboy.home().isPresent()
                && from.closerThan(barn, RETURN_RADIUS)
                && level.getEntitiesOfClass(Monster.class, barnBox(barn)).isEmpty();
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

    /** The box that counts as "in the building" - for anything that has to look inside it. */
    public static AABB barnBox(BlockPos barn) {
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
