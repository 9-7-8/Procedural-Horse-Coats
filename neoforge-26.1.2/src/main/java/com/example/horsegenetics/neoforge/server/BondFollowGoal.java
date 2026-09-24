package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/**
 * The behaviour goals that read a horse's <b>bond</b>
 * ({@link HorseCareAttachment#behaviourTier()}) and, depending on the tier, make
 * it look at, wander toward, or follow its owner (roadmap wiki &sect;13).
 *
 * <ul>
 *   <li><b>tier 1</b> (bond 31-60) - turns its head to face the owner within
 *       {@link #LOOK_RANGE} blocks; no movement. That is {@link Watch}, a
 *       separate goal claiming {@link Flag#LOOK} only - see below.</li>
 *   <li><b>tier 2</b> (61-80) - paths toward the owner, but only if the
 *       navigator can find a route ("on a clear path"); gives up otherwise.</li>
 *   <li><b>tier 3</b> (81-100) - follows at a walk, re-pathing persistently,
 *       stopping close. Fences and walls are respected because this is real
 *       pathfinding, not a teleport.</li>
 * </ul>
 *
 * <p><b>Every tier needs a walkable route to the owner, not a sight line.</b>
 * A horse shut in a pen whose owner is outside the fence acts like any other
 * horse: {@link ReachCheck} asks the navigator for a real path and the goal is
 * off when there is none. Without it a penned horse spent all day staring
 * through the rails at its owner - and, because the staring tier held
 * {@link Flag#MOVE}, out-ranked every herd goal below it, so a bonded stallion
 * never went to a mare in heat.
 *
 * <p>The two goals are added once per horse in {@link HorseCareHandler}; adding
 * and removing them as the number crosses a threshold would be fiddlier and
 * harder to test, so instead {@link #canUse()} returns false below the tier.
 */
public final class BondFollowGoal extends Goal {

    private static final double LOOK_RANGE = 10.0;
    private static final double TIER3_STOP = 3.0;
    private static final double TIER2_STOP = 4.0;
    /** Don't chase across the world - past this the owner is "gone", wait for them. */
    private static final double MAX_FOLLOW = 32.0;

    private final AbstractHorse horse;
    private final ReachCheck reach = new ReachCheck();
    private LivingEntity owner;
    private int recalcCooldown;

    public BondFollowGoal(AbstractHorse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private static int tierOf(AbstractHorse horse) {
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        return care == null ? 0 : care.behaviourTier();
    }

    private static boolean ownerUsable(AbstractHorse horse, LivingEntity o) {
        return o != null && o.isAlive() && !o.isSpectator() && o.level() == horse.level();
    }

    @Override
    public boolean canUse() {
        if (!horse.isTamed() || horse.isLeashed() || horse.isVehicle()) {
            return false;
        }
        int tier = tierOf(horse);
        if (tier < 2) {
            return false;
        }
        LivingEntity o = horse.getOwner();
        if (!ownerUsable(horse, o)) {
            return false;
        }
        double distSq = horse.distanceToSqr(o);
        double stop = tier == 3 ? TIER3_STOP : TIER2_STOP;
        if (distSq < (stop + 2.0) * (stop + 2.0) || distSq > MAX_FOLLOW * MAX_FOLLOW) {
            return false;
        }
        if (!reach.test(horse, o)) {
            return false;
        }
        this.owner = o;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!horse.isTamed() || horse.isLeashed() || horse.isVehicle()) {
            return false;
        }
        if (tierOf(horse) < 2) {
            return false;
        }
        LivingEntity o = this.owner;
        if (!ownerUsable(horse, o) || !reach.test(horse, o)) {
            return false;
        }
        double distSq = horse.distanceToSqr(o);
        double stop = tierOf(horse) == 3 ? TIER3_STOP : TIER2_STOP;
        return distSq > stop * stop && distSq < (MAX_FOLLOW + 4.0) * (MAX_FOLLOW + 4.0);
    }

    @Override
    public void start() {
        this.recalcCooldown = 0;
        if (this.owner == null) {
            this.owner = horse.getOwner();
        }
    }

    @Override
    public void stop() {
        this.owner = null;
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.owner != null ? this.owner : horse.getOwner();
        if (target == null) {
            return;
        }
        horse.getLookControl().setLookAt(target, 10.0F, (float) horse.getMaxHeadXRot());

        if (horse.isVehicle() || --this.recalcCooldown > 0) {
            return;
        }
        this.recalcCooldown = adjustedTickDelay(10);

        double stop = tierOf(horse) == 3 ? TIER3_STOP : TIER2_STOP;
        if (horse.distanceToSqr(target) > stop * stop) {
            double speed = tierOf(horse) == 3 ? 1.0 : 0.9;
            // The route was proved walkable in canUse/canContinueToUse, so tier 3
            // re-issuing every cycle is persistence rather than the old spin of
            // asking the navigator for an impossible path ten times a second.
            horse.getNavigation().moveTo(target, speed);
        } else {
            horse.getNavigation().stop();
        }
    }

    /**
     * <b>Tier 1: head-turning, and nothing else.</b> Split out of the follow goal
     * because of what its flags mean to the goal selector - a goal holding
     * {@link Flag#MOVE} blocks every goal of a higher priority <em>number</em>
     * from moving, whether or not it moves itself. Sharing the follow goal's
     * priority 4, a horse doing nothing but looking at its owner silenced
     * {@link HerdGoals}' sparring, dam-and-foal, displacing, heat and grooming
     * goals (priorities 3-7) for as long as its owner stood in the paddock.
     * Claiming {@link Flag#LOOK} alone, and sitting below the herd goals, it now
     * fills the gaps between them instead of replacing them.
     */
    public static final class Watch extends Goal {

        private final AbstractHorse horse;
        private final ReachCheck reach = new ReachCheck();
        private LivingEntity owner;

        public Watch(AbstractHorse horse) {
            this.horse = horse;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!horse.isTamed() || horse.isLeashed() || horse.isVehicle() || tierOf(horse) < 1) {
                return false;
            }
            LivingEntity o = horse.getOwner();
            if (!ownerUsable(horse, o) || horse.distanceToSqr(o) > LOOK_RANGE * LOOK_RANGE
                    || !reach.test(horse, o)) {
                return false;
            }
            this.owner = o;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (!horse.isTamed() || horse.isLeashed() || horse.isVehicle() || tierOf(horse) < 1) {
                return false;
            }
            LivingEntity o = this.owner;
            return ownerUsable(horse, o)
                    && horse.distanceToSqr(o) <= (LOOK_RANGE + 2.0) * (LOOK_RANGE + 2.0)
                    && reach.test(horse, o);
        }

        @Override
        public void stop() {
            this.owner = null;
        }

        @Override
        public void tick() {
            if (this.owner != null) {
                horse.getLookControl().setLookAt(this.owner, 10.0F, (float) horse.getMaxHeadXRot());
            }
        }
    }

    /**
     * "Could this horse actually walk there?" - a real
     * {@link net.minecraft.world.entity.ai.navigation.PathNavigation#createPath}
     * call, which is far too expensive for a <code>canUse</code> that runs every
     * tick for every horse, so the answer is cached for {@link #EVERY} ticks.
     * A path that exists but stops short ({@code !canReach()}) is a fence: it
     * counts as no path.
     */
    static final class ReachCheck {

        /** Two seconds - a fence does not move, and a gate opening is not urgent. */
        private static final int EVERY = 40;

        private int cooldown;
        private boolean reachable;

        boolean test(AbstractHorse horse, LivingEntity target) {
            if (--this.cooldown > 0) {
                return this.reachable;
            }
            this.cooldown = EVERY;
            Path path = horse.getNavigation().createPath(target, 0);
            this.reachable = path != null && path.canReach();
            return this.reachable;
        }
    }
}
