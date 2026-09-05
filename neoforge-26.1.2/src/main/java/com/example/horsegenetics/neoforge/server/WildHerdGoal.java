package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Keeps a wild horse near its <b>herd lead</b> ({@link HorseCareAttachment#herd},
 * which for a natural herd is the lead horse's UUID). This is what makes a
 * freshly-spawned pack clump together and wander as a unit instead of drifting
 * apart the moment vanilla's stroll goal picks a random point.
 *
 * <ul>
 *   <li>The lead itself ({@code herd == its own UUID}) never runs this - it
 *       free-wanders and everyone else trails it.</li>
 *   <li>If the lead is gone (dead, or unloaded for a while), the first member
 *       to notice <b>promotes itself</b> to lead so the herd keeps moving.</li>
 *   <li>Real pathfinding, throttled to a re-path every ~15 ticks; a wall or a
 *       fence stops a follower just like it stops {@link BondFollowGoal}.</li>
 * </ul>
 */
public final class WildHerdGoal extends Goal {

    private static final double CATCH_UP_RANGE = 8.0;   // start moving past this
    private static final double STOP_RANGE = 5.0;       // close enough
    private static final double MAX_RANGE = 40.0;       // lead is "gone" past this
    private static final int LOST_LEAD_GRACE = 200;     // ~10s of no lead -> promote self

    private final AbstractHorse horse;
    private AbstractHorse lead;
    private int recalcCooldown;
    private int leadMissingTicks;

    public WildHerdGoal(AbstractHorse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private HorseCareAttachment care() {
        return horse.getData(ModAttachments.HORSE_CARE.get());
    }

    private AbstractHorse resolveLead() {
        HorseCareAttachment c = care();
        if (c == null || c.herd().isEmpty()) {
            return null;
        }
        UUID leadId = c.herd().get();
        if (leadId.equals(horse.getUUID())) {
            return null; // this horse IS the lead
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity e = level.getEntity(leadId);
        return (e instanceof AbstractHorse h && h.isAlive()) ? h : null;
    }

    @Override
    public boolean canUse() {
        if (horse.isTamed() || horse.isLeashed() || horse.isVehicle()) {
            return false;
        }
        HorseCareAttachment c = care();
        if (c == null || !c.inWildHerd()) {
            return false;
        }
        AbstractHorse l = resolveLead();
        if (l == null) {
            maybePromoteSelf(c);
            return false;
        }
        this.leadMissingTicks = 0;
        double d2 = horse.distanceToSqr(l);
        if (d2 < CATCH_UP_RANGE * CATCH_UP_RANGE || d2 > MAX_RANGE * MAX_RANGE) {
            return false;
        }
        this.lead = l;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (horse.isTamed() || horse.isLeashed() || horse.isVehicle() || this.lead == null || !this.lead.isAlive()) {
            return false;
        }
        double d2 = horse.distanceToSqr(this.lead);
        return d2 > STOP_RANGE * STOP_RANGE && d2 < (MAX_RANGE + 6.0) * (MAX_RANGE + 6.0);
    }

    private void maybePromoteSelf(HorseCareAttachment c) {
        if (c.herd().isPresent() && c.herd().get().equals(horse.getUUID())) {
            return;
        }
        if (++this.leadMissingTicks >= LOST_LEAD_GRACE) {
            this.leadMissingTicks = 0;
            horse.setData(ModAttachments.HORSE_CARE.get(), c.withWildHerd(horse.getUUID(),
                    c.herdBreed().orElse("unknown"), c.herdBand().orElse("TRADITIONAL")));
        }
    }

    @Override
    public void start() {
        this.recalcCooldown = 0;
    }

    @Override
    public void stop() {
        this.lead = null;
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.lead == null) {
            return;
        }
        horse.getLookControl().setLookAt(this.lead, 10.0F, (float) horse.getMaxHeadXRot());
        if (--this.recalcCooldown > 0) {
            return;
        }
        this.recalcCooldown = adjustedTickDelay(15);
        if (horse.distanceToSqr(this.lead) > STOP_RANGE * STOP_RANGE) {
            horse.getNavigation().moveTo(this.lead, 1.0);
        } else {
            horse.getNavigation().stop();
        }
    }
}
