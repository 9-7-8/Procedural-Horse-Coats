package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.entity.Cowboy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
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
 * <h2>Doors</h2>
 * The rider is the only one of the pair with hands, so this opens what the
 * horse walks into ({@link CowboyDoors}), and shuts the barn behind them once
 * everybody is in. Both leaves, always - see that class for why one is useless.
 *
 * <h2>Night</h2>
 * Home at dusk, doors shut. A monster on the way home does not change the
 * destination, only the pace - {@link CowboyRoutine} hands back a
 * {@link CowboyRoutine.Plan#urgent() urgent} shelter plan and the ride is made
 * at {@link #FLEE_SPEED}. He only runs blind when home is no use to him, and
 * does not settle again until the thing chasing him is well out of range.
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

    /** Roughly how often an idling pair picks a new spot to amble to. */
    private static final int IDLE_STROLL_INTERVAL = 200;

    /** How far an idle amble goes. */
    private static final int IDLE_STROLL_RANGE = 12;

    /** How far ahead of the barn he starts opening up. */
    private static final int DOOR_OPEN_RANGE = 16;

    /** Ticks between passes over the barn's doors. Cheap, but not free. */
    private static final int DOOR_SCAN_INTERVAL = 10;

    /**
     * How long he holds the doors for stragglers once he is home himself.
     *
     * <p>There has to be a deadline, because "everybody is in" is a condition
     * that can simply never come true: a horse pinned behind a fence a villager
     * built, one that pathed onto the far side of a hill, one standing in a
     * doorway it cannot fit through. Without this, one such horse holds all four
     * doors open until dawn and the barn is a shed with the lights on. Half a
     * minute is long enough for a string that is merely strung out, and short
     * enough that a string that is stuck does not cost the rest of them the
     * night.
     */
    private static final int DOOR_HOLD_TICKS = 600;

    private final AbstractHorse horse;
    private int repathCooldown;
    private int strollCooldown;
    private int doorScanCooldown;

    /** The barn is standing open for the night run-in and we have not shut it yet. */
    private boolean barnStandingOpen;

    /** We shut the barn behind us, so we owe it an opening at first light. */
    private boolean barnShutForNight;

    /** Ticks he has been standing at home with the doors still open. */
    private int doorHeldTicks;

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
        repathCooldown = 0;
        strollCooldown = 0;
        doorScanCooldown = 0;
        doorHeldTicks = 0;
        wasUrgent = false;
        stuckTicks = 0;
        lastDistance = Double.MAX_VALUE;
    }

    @Override
    public void stop() {
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
        if (doorScanCooldown > 0) {
            doorScanCooldown--;
        }

        CowboyRoutine.Plan plan = CowboyRoutine.plan(
                cowboy, level, horse.blockPosition(), wasUrgent);
        wasUrgent = plan.urgent();
        announceDutyChange(cowboy, level, plan);
        announceIfStuck(cowboy, level, plan);
        switch (plan.duty()) {
            case FLEE -> flee(level, plan);
            case SHELTER -> shelter(cowboy, level, plan);
            case PATROL -> patrol(cowboy, level, plan);
        }
    }

    // ------------------------------------------------------------------

    private void flee(ServerLevel level, CowboyRoutine.Plan plan) {
        // Running means the barn is no use tonight; leave it however it stands.
        barnStandingOpen = false;
        doorHeldTicks = 0;
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

    private void shelter(Cowboy cowboy, ServerLevel level, CowboyRoutine.Plan plan) {
        BlockPos barn = cowboy.home().orElse(horse.blockPosition());

        // Open up on the approach so the whole herd can pour in behind him,
        // rather than piling against a shut door while he squeezes through.
        //
        // `!barnShutForNight` is load-bearing and was missing. Shutting up sets
        // barnStandingOpen back to false, and this branch only ever asked whether
        // the doors were standing open - so the very next tick, still on the
        // SHELTER duty and still well within DOOR_OPEN_RANGE, it opened them
        // again. The barn spent the night flapping: he read as never having gone
        // in, the doors read as never having shut, and because an open doorway is
        // not a wall he never counted as sheltered either, which fed straight back
        // into the monster check. Once shut, they stay shut until patrol() opens
        // them at first light.
        if (!barnStandingOpen && !barnShutForNight
                && horse.blockPosition().closerThan(barn, DOOR_OPEN_RANGE)) {
            CowboyDoors.setBarnDoors(level, barn, true, cowboy);
            barnStandingOpen = true;
            barnShutForNight = false;
        }

        if (plan.goal().isPresent()) {
            doorHeldTicks = 0;
            // Same destination either way; a threat behind him only changes the pace.
            if (plan.urgent()) {
                moveTo(plan.goal().get(), FLEE_SPEED, FLEE_REPATH_INTERVAL);
            } else {
                moveTo(plan.goal().get(), TRAVEL_SPEED, REPATH_INTERVAL);
            }
            return;
        }

        horse.getNavigation().stop();
        if (!barnStandingOpen) {
            return;
        }
        // He is in. Shut up once everyone who is coming is in - or once he has
        // waited DOOR_HOLD_TICKS for one who never will.
        doorHeldTicks += ticksSinceLastCall();
        if (herdIsHome(cowboy, level, barn) || doorHeldTicks >= DOOR_HOLD_TICKS) {
            CowboyDoors.setBarnDoors(level, barn, false, cowboy);
            barnStandingOpen = false;
            barnShutForNight = true;
            doorHeldTicks = 0;
        }
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

    /**
     * How many game ticks one call of {@link #tick()} stands for. One, now that
     * {@link #requiresUpdateEveryTick()} is true - kept as a named thing rather
     * than a bare {@code 1} because it was {@code 2} and the difference silently
     * doubled every duration in this class.
     */
    private static int ticksSinceLastCall() {
        return 1;
    }

    private void patrol(Cowboy cowboy, ServerLevel level, CowboyRoutine.Plan plan) {
        BlockPos barn = cowboy.home().orElse(horse.blockPosition());
        // Morning: whatever we shut last night, we open. Tracked as a flag
        // rather than re-read off the world, so the common case - a cowboy out
        // on his rounds all day - costs nothing at all.
        if (barnShutForNight) {
            CowboyDoors.setBarnDoors(level, barn, true, cowboy);
            barnShutForNight = false;
        }
        barnStandingOpen = false;
        doorHeldTicks = 0;

        // And a standing guarantee that he can never be sealed in: while he is
        // inside the barn in daylight, its doors get opened. This is what saves
        // him from a player who shuts them on him, or from a night that ended
        // with the flag lost to a world reload - neither of which the flag
        // above can see.
        if (doorScanCooldown == 0 && CowboyRoutine.insideBarn(barn, horse.blockPosition())) {
            doorScanCooldown = DOOR_SCAN_INTERVAL;
            CowboyDoors.setBarnDoors(level, barn, true, cowboy);
        }

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

    /**
     * Everybody who <i>can</i> be home is home. Only loaded, living herd members
     * count: a horse that wandered out of render distance, or one a wolf ate,
     * must not hold the doors open till dawn.
     */
    private boolean herdIsHome(Cowboy cowboy, ServerLevel level, BlockPos barn) {
        return cowboy.liveHerd(level).stream()
                .allMatch(member -> CowboyRoutine.insideBarn(barn, member.blockPosition()));
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
        if (!horse.getNavigation().moveTo(x, y, z, speed)) {
            MoveControl control = horse.getMoveControl();
            control.setWantedPosition(x, y, z, speed);
        }
    }
}
