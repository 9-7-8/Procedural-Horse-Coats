package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import java.util.EnumSet;

/**
 * The single behaviour goal that reads a horse's <b>bond</b>
 * ({@link HorseCareAttachment#behaviourTier()}) and, depending on the tier,
 * makes it look at, wander toward, or follow its owner (roadmap wiki &sect;13).
 *
 * <ul>
 *   <li><b>tier 1</b> (bond 31-60) - turns its head to face the owner within
 *       {@link #LOOK_RANGE} blocks; no movement.</li>
 *   <li><b>tier 2</b> (61-80) - paths toward the owner, but only if the
 *       navigator can find a route ("on a clear path"); gives up otherwise.</li>
 *   <li><b>tier 3</b> (81-100) - follows at a walk, re-pathing persistently,
 *       stopping close. Fences and walls are respected because this is real
 *       pathfinding, not a teleport.</li>
 * </ul>
 *
 * <p>One goal, added once per horse in {@link HorseCareHandler}; adding and
 * removing goals as the number crosses a threshold would be fiddler and harder
 * to test, so instead {@link #canUse()} returns false below tier 1.
 */
public final class BondFollowGoal extends Goal {

    private static final double LOOK_RANGE = 10.0;
    private static final double TIER3_STOP = 3.0;
    private static final double TIER2_STOP = 4.0;
    /** Don't chase across the world - past this the owner is "gone", wait for them. */
    private static final double MAX_FOLLOW = 32.0;

    private final AbstractHorse horse;
    private LivingEntity owner;
    private int recalcCooldown;

    public BondFollowGoal(AbstractHorse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private int tier() {
        HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get());
        return care == null ? 0 : care.behaviourTier();
    }

    private boolean ownerUsable(LivingEntity o) {
        return o != null && o.isAlive() && !o.isSpectator() && o.level() == horse.level();
    }

    @Override
    public boolean canUse() {
        if (!horse.isTamed() || horse.isLeashed() || horse.isVehicle()) {
            return false;
        }
        int tier = tier();
        if (tier < 1) {
            return false;
        }
        LivingEntity o = horse.getOwner();
        if (!ownerUsable(o)) {
            return false;
        }
        double distSq = horse.distanceToSqr(o);
        if (tier == 1) {
            return distSq <= LOOK_RANGE * LOOK_RANGE;
        }
        double stop = tier == 3 ? TIER3_STOP : TIER2_STOP;
        if (distSq < (stop + 2.0) * (stop + 2.0) || distSq > MAX_FOLLOW * MAX_FOLLOW) {
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
        int tier = tier();
        if (tier < 1) {
            return false;
        }
        LivingEntity o = tier == 1 ? horse.getOwner() : this.owner;
        if (!ownerUsable(o)) {
            return false;
        }
        double distSq = horse.distanceToSqr(o);
        if (tier == 1) {
            return distSq <= (LOOK_RANGE + 2.0) * (LOOK_RANGE + 2.0);
        }
        double stop = tier == 3 ? TIER3_STOP : TIER2_STOP;
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

        int tier = tier();
        if (tier <= 1 || horse.isVehicle()) {
            return;
        }
        if (--this.recalcCooldown > 0) {
            return;
        }
        this.recalcCooldown = adjustedTickDelay(10);

        double stop = tier == 3 ? TIER3_STOP : TIER2_STOP;
        if (horse.distanceToSqr(target) > stop * stop) {
            double speed = tier == 3 ? 1.0 : 0.9;
            // tier 2 only moves if a route exists; moveTo returning false = no
            // clear path, so it just stands (roadmap: "wanders toward ... on a
            // clear path"). tier 3 re-issues every cycle and so is persistent.
            horse.getNavigation().moveTo(target, speed);
        } else {
            horse.getNavigation().stop();
        }
    }
}
