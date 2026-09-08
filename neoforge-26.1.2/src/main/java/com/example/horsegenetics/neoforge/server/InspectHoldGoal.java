package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import java.util.EnumSet;

/**
 * The goal that keeps a horse still while a player reads its information screen
 * ({@link HorseInspectHold} decides who is being read about).
 *
 * <p>It is a <b>goal</b> rather than a per-tick position clamp on purpose:
 * claiming {@link Flag#MOVE}, {@link Flag#JUMP} and {@link Flag#LOOK} at the
 * highest priority means every other goal - stroll, panic, the bond follow, the
 * herd - is simply not running, so nothing is fighting the freeze and nothing
 * has to be undone when it lifts. It does not stop a horse being pushed, ridden
 * or shoved off a ledge, and it should not: the point is that the horse does not
 * <i>wander away</i> from a screen the player is reading, not that it becomes a
 * statue.
 */
public final class InspectHoldGoal extends Goal {

    /** Above everything else this mod or vanilla puts on a horse. */
    public static final int PRIORITY = 0;

    private final AbstractHorse horse;

    public InspectHoldGoal(AbstractHorse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // A ridden horse is being driven by its rider, not wandering, and taking
        // MOVE from them would be a control bug rather than a courtesy.
        return !horse.isVehicle() && HorseInspectHold.isHeld(horse);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        horse.getNavigation().stop();
        horse.setXxa(0.0F);
        horse.setZza(0.0F);
    }
}
