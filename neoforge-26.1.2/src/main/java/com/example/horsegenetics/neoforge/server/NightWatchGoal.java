package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * <b>The watching half of the night behaviour.</b> After dark, a horse
 * homozygous on {@code horsegenetics.magic_night_watch} stops being a horse and
 * starts being interested in you.
 *
 * <p>Five modes, and the goal is one class rather than five because they differ
 * only in <i>where the horse wants to stand</i> and <i>whether it needs to see
 * you</i>. Everything else - find the nearest player, face them, hold - is
 * shared, and five classes would have been the same forty lines five times.
 *
 * <table>
 *   <tr><th>mode</th><th>where it stands</th><th>needs line of sight</th></tr>
 *   <tr><td>{@code stare}</td><td>wherever it already is</td><td>no - through walls</td></tr>
 *   <tr><td>{@code approach}</td><td>within {@code radius}</td><td>no</td></tr>
 *   <tr><td>{@code line_of_sight}</td><td>wherever it already is</td><td><b>yes</b></td></tr>
 *   <tr><td>{@code unseen}</td><td>outside the player's view arc</td><td>no</td></tr>
 *   <tr><td>{@code behind}</td><td>right behind them</td><td>no</td></tr>
 * </table>
 *
 * <p><b>Not verified in-game.</b> Written against 26.1.2 sources.
 *
 * <h2>It yields to the temper locus</h2>
 * {@link NightBehaviourHandler} clears this goal's ability while
 * {@code magic_night_temper} has something to act on, so {@link #canUse()}
 * returns false and the horse gets on with hunting or fleeing. A horse that
 * both stalks you and runs from you is not two behaviours, it is a bug.
 *
 * <h2>Standing still is the whole point</h2>
 * The goal takes {@link Flag#MOVE} and {@link Flag#LOOK} even in the modes that
 * never path anywhere, and that is deliberate rather than sloppy: holding MOVE
 * is what stops the wander goal from taking over and drifting the horse off
 * mid-stare. A watcher that wandered would just be a horse.
 */
public final class NightWatchGoal extends Goal {

    /** How far the horse will look for somebody to watch, whatever its mode's own radius says. */
    private static final double SEARCH = 48.0;

    /** Walking pace. A watcher that sprinted would read as an attack. */
    private static final double SPEED = 1.0;

    /** Re-path no more often than this, in ticks. */
    private static final int REPATH_INTERVAL = 10;

    /**
     * How far outside the player's facing a horse must get to count as
     * "unseen", in degrees either side. A player's field of view is about 70
     * across, so 80 is just past the edge of it.
     */
    private static final double UNSEEN_ARC = 80.0;

    private final Horse horse;
    private Player target;
    private int repathCooldown;

    public NightWatchGoal(Horse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** The active watch ability, or {@code null} - the handler owns this. */
    private GeneAbility.NightWatch watch() {
        return NightBehaviourHandler.activeWatch(horse);
    }

    @Override
    public boolean canUse() {
        GeneAbility.NightWatch w = watch();
        if (w == null || horse.isVehicle() || horse.isLeashed()) {
            return false;   // a ridden or tied horse is the rider's problem
        }
        target = horse.level().getNearestPlayer(horse, SEARCH);
        if (target == null || !target.isAlive()) {
            return false;
        }
        // THE ONE THAT GETS BEHIND YOU STOPS THE MOMENT YOU TURN ROUND.
        //
        // Owner, 2026-09-13: "watch behind has the horse stare at me even if I'm
        // looking at it. If I'm looking at it, it should behave like a normal
        // horse." That is the right rule and it is better than what was written:
        // the whole appeal of this variant is what happens where you are NOT
        // looking, and a horse that keeps creeping into your back while you
        // stare straight at it is not eerie, it is broken-looking.
        //
        // 'unseen' already had this instinct half-built - it repositions when
        // seen - but 'behind' moved to your back unconditionally. Turning to
        // face it now ends the goal outright, so it drops the stare and the
        // walk together and goes back to being a horse.
        if ("behind".equals(w.mode()) && inView()) {
            return false;
        }
        // 'unseen' keeps running while it is seen - unlike 'behind' - because
        // slipping OUT of the arc is the whole of that variant. It just does it
        // without staring at you on the way (see tick).
        // Only this mode is stopped by a closed door; the rest see through it.
        return !"line_of_sight".equals(w.mode()) || horse.hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        target = null;
        horse.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        GeneAbility.NightWatch w = watch();
        if (w == null || target == null) {
            return;
        }
        // THE STARE IS NOT THE CONSTANT ANY MORE.
        //
        // It used to be: every mode looked at the player every tick, and only
        // the movement differed. Two of them read badly for it. Owner,
        // 2026-09-13: "watch unseen is indeed staring at me", and of the other,
        // "if I'm looking at it, it should behave like a normal horse".
        //
        // She is right, and the reason is what the two modes are ABOUT. The
        // fixed and sighted watchers are meant to hold your eye - being stared
        // at is the whole of them. 'unseen' and 'behind' are the opposite: they
        // are about what happens where you are NOT looking, and a horse that
        // keeps its eyes locked on you while you stare straight back is not
        // eerie, it just looks broken. So those two drop the stare the moment
        // they are in your arc, and pick it up again when you turn away.
        if (!avoidsBeingSeen(w) || !inView()) {
            horse.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        Vec3 want = destination(w);
        if (want == null) {
            horse.getNavigation().stop();
            return;
        }
        if (repathCooldown > 0) {
            repathCooldown--;
            return;
        }
        repathCooldown = REPATH_INTERVAL;
        if (horse.position().distanceToSqr(want) > 1.5 * 1.5) {
            horse.getNavigation().moveTo(want.x, want.y, want.z, SPEED);
        } else {
            horse.getNavigation().stop();
        }
    }

    /** Where this mode wants the horse to be, or {@code null} for "stay put". */
    private Vec3 destination(GeneAbility.NightWatch w) {
        return switch (w.mode()) {
            // Two modes never move at all: one stares from wherever it is, the
            // other only cares whether it can see you.
            case "stare", "line_of_sight" -> null;

            // Close to the mode's radius and then stop. Only walks when it is
            // outside; once inside it holds, so it does not creep.
            case "approach" -> horse.distanceTo(target) > w.radius()
                    ? target.position()
                    : null;

            // Directly behind the player. The look vector points forward, so
            // behind is minus it - approached the long way round, never through.
            case "behind" -> goRound(target, target.position()
                    .subtract(flat(target.getLookAngle()).scale(w.radius())));

            // Out of the player's view arc. If the horse is already outside it,
            // it stays; otherwise it walks round to the player's back.
            case "unseen" -> inView() ? goRound(target, behindOffset(target)) : null;

            default -> null;
        };
    }

    /** A point behind the player, used by {@code unseen} to leave the arc. */
    private Vec3 behindOffset(Player player) {
        Vec3 back = flat(player.getLookAngle()).scale(-Math.max(3.0, horse.getBbWidth() * 2));
        return player.position().add(back);
    }

    /** How wide to swing when the direct line would cross the player. */
    private static final double SWING_WIDE = 3.5;

    /**
     * <b>Walk AROUND the player, never through them.</b>
     *
     * <p>Owner, 2026-09-13: <i>"if I turn to look at it, it tries to get behind
     * me by WALKING THROUGH ME, which looks too unnatural. It's meant to behave
     * more sneaky."</i> Exactly so - and the cause is that the destination was
     * handed straight to the navigator, which quite reasonably drew the
     * shortest line, and the shortest line from in front of you to behind you
     * goes through you.
     *
     * <p>So: if the horse is on the far side of the player from where it wants
     * to be, it is given a waypoint <b>out to whichever side it is already
     * nearest</b> instead. It arcs out, the geometry is recomputed on the next
     * re-path, and it comes round the back. The horse never chooses the shorter
     * way past your face, which is what "sneaky" means in movement terms.
     */
    private Vec3 goRound(Player player, Vec3 goal) {
        Vec3 toHorse = flat(horse.position().subtract(player.position()));
        Vec3 toGoal = flat(goal.subtract(player.position()));
        if (toHorse.lengthSqr() < 1.0e-4 || toGoal.lengthSqr() < 1.0e-4) {
            return goal;
        }
        if (toHorse.dot(toGoal) >= 0) {
            return goal;   // already on the right side; the straight line is fine
        }
        Vec3 right = new Vec3(-toGoal.z, 0, toGoal.x);
        double side = right.dot(toHorse) < 0 ? -1.0 : 1.0;
        return player.position().add(right.normalize().scale(side * SWING_WIDE));
    }

    /** Flattened and normalised, because all of this is a question about yaw. */
    private static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : f.normalize();
    }

    /**
     * Modes whose whole identity is being unobserved, and which therefore go
     * quiet when they are observed.
     */
    private static boolean avoidsBeingSeen(GeneAbility.NightWatch w) {
        return "behind".equals(w.mode()) || "unseen".equals(w.mode());
    }

    /**
     * Is the horse inside the player's view arc?
     *
     * <p>Deliberately a <b>yaw</b> test and not a raycast. "Can the player see
     * it" for this gene means "is it in front of them", because the unsettling
     * part is turning round and finding it there - not whether a fencepost
     * happened to be in the way.
     */
    private boolean inView() {
        Vec3 toHorse = horse.position().subtract(target.position());
        if (toHorse.lengthSqr() < 1.0e-4) {
            return true;
        }
        Vec3 look = target.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
        Vec3 flatTo = new Vec3(toHorse.x, 0, toHorse.z).normalize();
        double cos = flatLook.dot(flatTo);
        return cos > Math.cos(Math.toRadians(UNSEEN_ARC));
    }
}
