package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * <b>The cowboy's horse does the walking.</b>
 *
 * <p>A rider that is not a player cannot steer a horse - vanilla only hands
 * control to a saddled mount's player passenger - so a mounted mob is dead
 * weight and the animal keeps running its own stroll goal. Rather than fight
 * that from the rider's side, this puts the itinerary on the horse: while a
 * {@link Cowboy} is aboard, this goal holds {@link Flag#MOVE} continuously,
 * which starves every other movement goal the horse has, and rides to whatever
 * {@link CowboyRoutine} says the pair should be doing.
 *
 * <p>It keeps running even when there is nowhere to go, and simply stops the
 * navigation - that is the difference between "the cowboy is standing at his
 * barn" and "the horse has decided to wander off with him on it".
 *
 * <h2>Daytime only</h2>
 * There is no night here any more. {@link CowboyHandler} takes him off the horse
 * at dusk and {@link CowboyRemountGoal} walks him back to it at dawn, so this
 * goal simply stops running when he stops being a passenger. The barn, its
 * doors and the whole business of getting a string of horses through them are
 * gone with it - see {@link CowboyRoutine} for why.
 */
public final class CowboyMountGoal extends Goal {

    private static final double TRAVEL_SPEED = 1.1;
    private static final double AMBLE_SPEED = 0.7;

    /** Riding for his life. Deliberately faster than anything else he does. */
    private static final double FLEE_SPEED = 1.9;

    /** Re-path this often while travelling - cheaper than every tick, still responsive. */
    private static final int REPATH_INTERVAL = 40;

    /** Re-path far more often while fleeing; the thing chasing him is moving too. */
    private static final int FLEE_REPATH_INTERVAL = 10;

    /** How far the straight-line fallback aims when no sampled position pathed. */
    private static final double FLEE_DISTANCE = 16.0;

    /** Ticks of having somewhere to be and not getting nearer before the dev build complains. */
    private static final int STUCK_TICKS = 100;

    /** Ticks between full state reports to the log. One a second. */
    private static final int REPORT_INTERVAL = 20;

    /** Roughly how often an idling pair picks a new spot to amble to. */
    private static final int IDLE_STROLL_INTERVAL = 200;

    /** How far an idle amble goes. */
    private static final int IDLE_STROLL_RANGE = 12;

    private final AbstractHorse horse;
    private int repathCooldown;
    private int strollCooldown;

    /** What he was doing last tick, for the dev-build announcement below. */
    private CowboyRoutine.@Nullable Duty lastDuty;

    /**
     * Was the last plan an urgent one? Fed back into
     * {@link CowboyRoutine#plan} so a monster he has already started getting
     * away from has to get properly clear before he settles - see
     * {@link CowboyRoutine#SAFE_RADIUS}. Held here because the routine is a pure
     * function and this is the one thing it needs remembered for it.
     */
    private boolean wasUrgent;

    /** How long he has had somewhere to be without getting closer to it. */
    private int stuckTicks;

    /** How close he was to that somewhere last time it was measured. */
    private double lastDistance = Double.MAX_VALUE;

    /** Countdown to the next full state report. */
    private int reportCooldown;

    public CowboyMountGoal(AbstractHorse horse) {
        this.horse = horse;
        // MOVE only. Holding JUMP as well would starve the horse's own FloatGoal,
        // which is the goal that keeps it - and the man on it - from drowning.
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private Cowboy rider() {
        for (Entity passenger : horse.getPassengers()) {
            if (passenger instanceof Cowboy cowboy && cowboy.isAlive()) {
                return cowboy;
            }
        }
        return null;
    }

    @Override
    public boolean canUse() {
        return rider() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return rider() != null;
    }

    /** Is this goal still in the horse's goal set at all? */
    private boolean isRegisteredOn(AbstractHorse subject) {
        for (WrappedGoal wrapped : subject.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() == this) {
                return true;
            }
        }
        return false;
    }

    /**
     * Everything the goal selector will let us know about the horse, in one
     * line. The single most useful diagnostic there is for "why is it not going
     * where I told it": if the mount is wandering, some other goal is
     * <b>running</b>, and this says which.
     */
    private String horseState() {
        StringBuilder running = new StringBuilder();
        for (WrappedGoal wrapped : horse.goalSelector.getAvailableGoals()) {
            if (wrapped.isRunning()) {
                if (running.length() > 0) {
                    running.append(", ");
                }
                running.append(wrapped.getGoal().getClass().getSimpleName())
                        .append('#').append(wrapped.getPriority());
            }
        }
        return String.format(
                "pos=%d,%d,%d speed=%.3f nav=%s immobile=%s standing=%s eating=%s "
                        + "vehicle=%s tamed=%s running=[%s]",
                horse.blockPosition().getX(), horse.blockPosition().getY(), horse.blockPosition().getZ(),
                horse.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED),
                horse.getNavigation().isDone() ? "idle" : "pathing",
                horse.isImmobile(), horse.isStanding(), horse.isEating(),
                horse.isVehicle(), horse.isTamed(),
                running.length() == 0 ? "none" : running.toString());
    }

    /**
     * Every tick, not every other one.
     *
     * <p>A goal that returns {@code false} here is run by
     * {@code GoalSelector.tickRunningGoals(false)} on odd ticks, which is to say
     * not at all - so every interval in this class was quietly doubled, and a
     * horse whose path had just failed stood still for twice as long as the
     * constant said. It is one navigation call every few ticks either way; the
     * saving was never real and the halved responsiveness was.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        DebugAnnounce.log("Cowboy", "mount goal START on horse " + horse.getUUID() + " - " + horseState());
        repathCooldown = 0;
        strollCooldown = 0;
        wasUrgent = false;
        stuckTicks = 0;
        lastDistance = Double.MAX_VALUE;
        reportCooldown = 0;
    }

    @Override
    public void stop() {
        // Both halves of GoalSelector's stop condition, spelled out. If the rider
        // is non-null the goal wanted to keep going, and the selector stopped it
        // anyway - which can only be a disabled control flag, and says so here
        // rather than leaving it to be inferred from a log three days later.
        DebugAnnounce.log("Cowboy", "mount goal STOP on horse " + horse.getUUID()
                + " canContinueToUse=" + canContinueToUse()
                + " rider=" + rider()
                + " stillRegistered=" + isRegisteredOn(horse)
                + " - " + horseState());
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        Cowboy cowboy = rider();
        if (cowboy == null || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (cowboy.isTrading()) {
            horse.getNavigation().stop();   // stand still while a customer is at the window
            return;
        }

        if (repathCooldown > 0) {
            repathCooldown--;
        }
        if (strollCooldown > 0) {
            strollCooldown--;
        }

        CowboyRoutine.Plan plan = CowboyRoutine.plan(
                cowboy, level, horse.blockPosition(), wasUrgent);
        wasUrgent = plan.urgent();
        announceDutyChange(cowboy, level, plan);
        announceIfStuck(cowboy, level, plan);
        report(cowboy, level, plan);
        switch (plan.duty()) {
            case FLEE -> flee(level, plan);
            case PATROL -> patrol(cowboy, level, plan);
        }
    }

    // ------------------------------------------------------------------

    private void flee(ServerLevel level, CowboyRoutine.Plan plan) {
        if (repathCooldown > 0 && !horse.getNavigation().isDone()) {
            return;
        }
        if (!(horse instanceof PathfinderMob mob) || plan.threat() == null) {
            return;
        }
        Vec3 threat = plan.threat().position();
        Vec3 away = DefaultRandomPos.getPosAway(mob, 20, 8, threat);
        if (away == null) {
            // Nowhere pathable in the arc it sampled. Rather than stand still for
            // another interval - which is most of what "he barely flees" looked
            // like - aim straight down the line away from the thing and let the
            // navigator get as far along it as it can.
            away = straightAwayFrom(threat);
        }
        repathCooldown = FLEE_REPATH_INTERVAL;
        drive(away.x, away.y, away.z, FLEE_SPEED);
    }

    /** A point {@link #FLEE_DISTANCE} blocks directly away from a threat. */
    private Vec3 straightAwayFrom(Vec3 threat) {
        Vec3 heading = horse.position().subtract(threat);
        if (heading.horizontalDistanceSqr() < 1.0E-4) {
            heading = new Vec3(1.0, 0.0, 0.0); // standing on top of it; any way will do
        }
        return horse.position().add(heading.normalize().scale(FLEE_DISTANCE));
    }

    /**
     * One line a second to the log, saying what he is trying to do and what the
     * horse under him is actually doing about it.
     *
     * <p>Log only, not chat - it is one line a second per cowboy, which is a
     * paste-into-a-report volume rather than a read-while-you-play one. The
     * short version goes to chat via {@link #announceDutyChange} and
     * {@link #announceIfStuck}.
     */
    private void report(Cowboy cowboy, ServerLevel level, CowboyRoutine.Plan plan) {
        if (!DebugAnnounce.enabled() || --reportCooldown > 0) {
            return;
        }
        reportCooldown = REPORT_INTERVAL;
        BlockPos barn = cowboy.home().orElse(horse.blockPosition());
        DebugAnnounce.log("Cowboy", String.format(
                "%s duty=%s goal=%s urgent=%s dark=%s "
                        + "barn=%d,%d,%d dist=%.1f trading=%s | %s",
                cowboy.cowboyName(), plan.duty(),
                plan.goal().map(g -> g.getX() + "," + g.getY() + "," + g.getZ()).orElse("-"),
                plan.urgent(), level.isDarkOutside(),
                barn.getX(), barn.getY(), barn.getZ(),
                Math.sqrt(horse.blockPosition().distSqr(barn)),
                cowboy.isTrading(), horseState()));
    }

    /**
     * In a dev build, say why he is not getting anywhere.
     *
     * <p>Here because "he does not go to his barn" has now survived three fixes
     * that were each arrived at by reading the code, and the thing none of them
     * could tell me is which of the several ways a horse can refuse to move is
     * the one actually happening. So when he has somewhere to be and has not got
     * closer to it for {@link #STUCK_TICKS}, this prints the state that decides
     * it: how far off he is, whether the navigator thinks it has a path, and
     * whether the horse is in one of the two states that make
     * {@code AbstractHorse.isImmobile()} true. One line names the culprit.
     *
     * <p>Delete it with {@code wiki/known-gaps.html} gap 59.
     */
    private void announceIfStuck(Cowboy cowboy, ServerLevel level, CowboyRoutine.Plan plan) {
        if (!DebugAnnounce.enabled() || plan.goal().isEmpty()) {
            stuckTicks = 0;
            return;
        }
        double distance = Math.sqrt(horse.blockPosition().distSqr(plan.goal().get()));
        if (distance < lastDistance - 0.5) {
            lastDistance = distance;
            stuckTicks = 0;
            return;
        }
        if (++stuckTicks < STUCK_TICKS) {
            return;
        }
        stuckTicks = 0;
        lastDistance = distance;
        DebugAnnounce.say(level, "Cowboy", String.format(
                "%s stuck on %s: %.1f blocks off, nav %s, immobile=%s standing=%s eating=%s",
                cowboy.cowboyName(), plan.duty(), distance,
                horse.getNavigation().isDone() ? "idle" : "pathing",
                horse.isImmobile(), horse.isStanding(), horse.isEating()),
                ChatFormatting.RED);
    }

    /**
     * In a dev build, say every time he changes what he is doing.
     *
     * <p>Here because "he does not flee from zombies" was reported twice and could
     * not be told apart, from outside, from "this goal is not running at all" -
     * and the two want completely different fixes. One line per change answers
     * that in the first minute of a playthrough: a stream of
     * <code>PATROL</code>/<code>SHELTER</code> means the goal is alive and the
     * routine's rules are what is wrong, and total silence means the goal never
     * starts and nothing in {@link CowboyRoutine} matters yet.
     *
     * <p>Costs nothing in a real build - see {@link DebugAnnounce}. Delete it once
     * the answer is known.
     */
    private void announceDutyChange(Cowboy cowboy, ServerLevel level, CowboyRoutine.Plan plan) {
        if (!DebugAnnounce.enabled() || plan.duty() == lastDuty) {
            return;
        }
        lastDuty = plan.duty();
        DebugAnnounce.say(level, "Cowboy", cowboy.cowboyName() + ": " + plan.duty(),
                plan.duty() == CowboyRoutine.Duty.FLEE ? ChatFormatting.RED : ChatFormatting.GRAY);
    }

    private void patrol(Cowboy cowboy, ServerLevel level, CowboyRoutine.Plan plan) {
        BlockPos barn = cowboy.home().orElse(horse.blockPosition());
        if (plan.goal().isPresent()) {
            moveTo(plan.goal().get(), TRAVEL_SPEED, REPATH_INTERVAL);
            return;
        }

        // Arrived. Amble about now and then so the pair does not stand like a
        // statue, but never far enough to leave the patch.
        if (strollCooldown == 0 && horse.getNavigation().isDone()) {
            strollCooldown = IDLE_STROLL_INTERVAL + horse.getRandom().nextInt(IDLE_STROLL_INTERVAL);
            BlockPos spot = barn.offset(
                    horse.getRandom().nextInt(IDLE_STROLL_RANGE * 2 + 1) - IDLE_STROLL_RANGE,
                    0,
                    horse.getRandom().nextInt(IDLE_STROLL_RANGE * 2 + 1) - IDLE_STROLL_RANGE);
            drive(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, AMBLE_SPEED);
        }
    }

    private void moveTo(BlockPos target, double speed, int interval) {
        if (repathCooldown > 0 && !horse.getNavigation().isDone()) {
            return;
        }
        repathCooldown = interval;
        drive(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
    }

    /**
     * Point the horse at somewhere and make sure it actually goes.
     *
     * <p><b>Navigation first, move control second.</b> A path is what you want -
     * it goes round things - but {@code moveTo} returns false whenever it cannot
     * build one, and a rider on a horse standing in a barn doorway or against a
     * fence corner hits that often. Falling through to the move control walks the
     * horse straight at the target instead, which is worse pathing and much
     * better than not moving at all. Nothing else on this horse is steering, so
     * there is no one to argue with.
     *
     * <p>It also clears the <b>rearing</b> state first.
     * {@code AbstractHorse.isImmobile()} is true while a horse is standing, and
     * standing is set by {@code RandomStandGoal}, which carries <i>no</i>
     * {@link Flag} at all - so holding {@link Flag#MOVE} does not starve it and
     * it can freeze the mount for twenty ticks at a time, on its own schedule,
     * for ever. A horse that is trying to get home does not rear.
     */
    private void drive(double x, double y, double z, double speed) {
        if (horse.isStanding()) {
            horse.clearStanding();
        }
        boolean pathed = horse.getNavigation().moveTo(x, y, z, speed);
        if (!pathed) {
            MoveControl control = horse.getMoveControl();
            control.setWantedPosition(x, y, z, speed);
        }
        DebugAnnounce.log("Cowboy", String.format(
                "drive to %.1f,%.1f,%.1f at %.2f -> %s",
                x, y, z, speed, pathed ? "pathed" : "no path, using move control"));
    }
}
