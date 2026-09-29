package com.example.horsegenetics.neoforge.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import java.util.EnumSet;

/**
 * <b>Walk to whoever blew the whistle.</b> The moving half of {@link WhistleCalls};
 * every decision about whether there is a call, and about how one ends, is made
 * there.
 *
 * <p>This goal is deliberately almost empty. It asks the registry "is this horse
 * called, and by whom", points the navigator at them, and looks at them. It does
 * not decide to stop, does not time itself out and does not teleport anything -
 * because a goal that never gets to start cannot do any of those, and the whole
 * point of the sweep owning them is to cover that case.
 *
 * <h2>Priority 1, which is not what the plan said</h2>
 *
 * <p>The treatment for this feature reasoned "below survival, above everything
 * social" and concluded priority <b>2</b>. Reading the generated
 * {@code wiki/behaviour-hierarchy.html} rather than that line shows why 2 is
 * wrong: priority 2 is vanilla's {@code BreedGoal}, which claims MOVE and LOOK,
 * and <b>an equal priority never interrupts</b> - {@code WrappedGoal.canBeReplacedBy}
 * requires a strictly lower number. A horse in love mode would refuse the
 * whistle outright, which is the exact failure the treatment was warning about,
 * just behind a different goal than it named. There is no integer between 1 and
 * 2, so the choice is to sit at 1 and give up what arbitration was doing.
 *
 * <p>What arbitration was doing at 2 is yielding to panic and sun-shade, which
 * are also at 1. That is paid for by hand: {@link WhistleCalls#inDanger} is
 * checked here <i>and</i> in the sweep, and it is deliberately coarser than
 * {@code PanicGoal}'s own trigger so it errs toward letting the horse look after
 * itself. Escape and the inspect hold are at 0 and still interrupt this the
 * ordinary way, so the two genuinely non-negotiable goals need no help.
 *
 * <p><b>A new row in the behaviour hierarchy</b> - after touching this, re-run
 * {@code node wiki/tools/bake-behaviour-hierarchy.mjs}.
 */
public final class WhistleCallGoal extends Goal {

    /** See the class note: 1, not the 2 the plan assumed. */
    public static final int PRIORITY = 1;

    /** A shade above a walk. A called horse should look like it is coming. */
    private static final double SPEED = 1.15;

    private static final int REPATH_EVERY = 10;

    private final AbstractHorse horse;
    private ServerPlayer caller;
    private int recalcCooldown;

    public WhistleCallGoal(AbstractHorse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        ServerPlayer called = WhistleCalls.calledBy(horse);
        if (called == null || WhistleCalls.inDanger(horse)) {
            return false;
        }
        this.caller = called;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // The sweep clears the marker on every ending, so "still marked" is the
        // whole condition. Asking anything else here would be a second opinion
        // about when a call is over, and the two would drift.
        return canUse();
    }

    @Override
    public void start() {
        this.recalcCooldown = 0;
    }

    @Override
    public void stop() {
        this.caller = null;
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        ServerPlayer target = this.caller;
        if (target == null) {
            return;
        }
        horse.getLookControl().setLookAt(target, 10.0F, (float) horse.getMaxHeadXRot());
        if (--this.recalcCooldown > 0) {
            return;
        }
        this.recalcCooldown = adjustedTickDelay(REPATH_EVERY);
        // The route was proved walkable at blow time, so re-issuing is
        // persistence rather than asking for an impossible path ten times a
        // second - the same reasoning as BondFollowGoal's tick.
        horse.getNavigation().moveTo(target, SPEED);
    }
}
