package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps a wild horse with its <b>band</b>.
 *
 * <h2>A family band follows its lead mare</h2>
 * Mares, not the stallion, decide where a band moves (see the science tab of
 * {@code wiki/horse-care.html}). So in a family band everyone trails the
 * <b>lead mare</b> ({@link HerdSocialHandler#leadMare}); the stallion trails her too,
 * but loosely, keeping to the edge - {@link HerdGoals.StallionGuard} is what he does
 * out there - and the lead mare herself wanders free. A bachelor band still
 * follows its lead.
 *
 * <h2>A band no longer splits when its lead dies</h2>
 * This goal used to promote the first member to notice a missing lead, and nobody
 * re-pointed at the new one, so every member became a herd of one. Following the
 * lead mare means a family band with no stallion simply carries on; everything else
 * about a missing lead is {@link HerdSocialHandler}'s, which can see the whole band.
 *
 * <p>Real pathfinding, re-pathed every ~15 ticks; a fence stops a follower just as it
 * stops {@link BondFollowGoal}.
 */
public final class WildHerdGoal extends Goal {

    private static final double CATCH_UP_RANGE = 8.0;
    private static final double STOP_RANGE = 5.0;
    /** The stallion keeps further back. */
    private static final double STALLION_CATCH_UP = 14.0;
    private static final double STALLION_STOP = 9.0;
    private static final double MAX_RANGE = 40.0;

    private final AbstractHorse horse;
    private AbstractHorse target;
    private double stopRange = STOP_RANGE;
    private int recalcCooldown;

    public WildHerdGoal(AbstractHorse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    /** Who this horse follows, or {@code null} for one that leads. */
    private AbstractHorse followTarget(ServerLevel level, HorseCareAttachment care) {
        UUID herd = care.herd().orElse(null);
        if (herd == null) {
            return null;
        }
        boolean iLead = herd.equals(horse.getUUID());
        if (!HerdSocialHandler.isBachelor(care)) {
            Optional<UUID> mare = horse instanceof Horse h
                    ? HerdSocialHandler.leadMare(level, herd, h) : Optional.empty();
            if (mare.isPresent() && !mare.get().equals(horse.getUUID())) {
                Entity e = level.getEntity(mare.get());
                if (e instanceof AbstractHorse m && m.isAlive()) {
                    return m;
                }
            }
            if (mare.isPresent() || iLead) {
                return null;    // the lead mare wanders free; a stallion with no mare loaded does too
            }
        }
        if (iLead) {
            return null;
        }
        Entity e = level.getEntity(herd);
        return e instanceof AbstractHorse l && l.isAlive() ? l : null;
    }

    @Override
    public boolean canUse() {
        if (horse.isTamed() || horse.isLeashed() || horse.isVehicle() || !(horse.level() instanceof ServerLevel level)) {
            return false;
        }
        HorseCareAttachment c = horse.getData(ModAttachments.HORSE_CARE.get());
        if (c == null || !c.inWildHerd()) {
            return false;
        }
        AbstractHorse t = followTarget(level, c);
        if (t == null) {
            return false;
        }
        boolean stallion = c.herd().map(horse.getUUID()::equals).orElse(false);
        double catchUp = stallion ? STALLION_CATCH_UP : CATCH_UP_RANGE;
        double d2 = horse.distanceToSqr(t);
        if (d2 < catchUp * catchUp || d2 > MAX_RANGE * MAX_RANGE) {
            return false;
        }
        this.target = t;
        this.stopRange = stallion ? STALLION_STOP : STOP_RANGE;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (horse.isTamed() || horse.isLeashed() || horse.isVehicle() || this.target == null || !this.target.isAlive()) {
            return false;
        }
        double d2 = horse.distanceToSqr(this.target);
        return d2 > stopRange * stopRange && d2 < (MAX_RANGE + 6.0) * (MAX_RANGE + 6.0);
    }

    @Override
    public void start() {
        this.recalcCooldown = 0;
    }

    @Override
    public void stop() {
        this.target = null;
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        horse.getLookControl().setLookAt(this.target, 10.0F, (float) horse.getMaxHeadXRot());
        if (--this.recalcCooldown > 0) {
            return;
        }
        this.recalcCooldown = adjustedTickDelay(15);
        if (horse.distanceToSqr(this.target) > stopRange * stopRange) {
            horse.getNavigation().moveTo(this.target, 1.0);
        } else {
            horse.getNavigation().stop();
        }
    }
}
