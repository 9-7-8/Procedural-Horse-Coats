package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * <b>A horse that has picked a fight does not bolt out of it.</b>
 *
 * <p>Owner, 2026-09-13, on the guardian: <i>"the guardian makes a half-hearted
 * attempt, is terrible at combat, runs off, then gets chased by the zombie and
 * gets killed by it."</i> Every clause of that is one bug, and it is not the
 * gene.
 *
 * <h2>Priority 1 beats priority 3</h2>
 * {@code AbstractHorse} registers vanilla's {@link PanicGoal} at <b>priority
 * 1</b>, and {@link HorseAggroHandler} adds the melee goal at
 * {@code MELEE_GOAL_PRIORITY}, which is 3. Both hold {@code Flag.MOVE}, and a
 * lower number wins - so the moment anything lands a hit, panic takes the
 * movement flag and the horse runs. The melee goal is then starved for as long
 * as the panic lasts, which is exactly the window in which a fight is
 * happening.
 *
 * <p>That produces the described sequence precisely: a swing or two before the
 * first hit lands ("a half-hearted attempt"), then flight, then a zombie
 * following a fleeing animal that will not turn round. <b>It was never about
 * attack speed or damage</b>, which is why making the swing faster did not
 * save the gladiator either.
 *
 * <h2>Panic is still right for every other horse</h2>
 * So this does not remove it, it qualifies it: <b>panic unless you have a
 * target.</b> A horse with no aggression gene behaves exactly as vanilla - it
 * flees fire, it flees what hits it - because it never has a target to hold it
 * in place. A guardian or a gladiator that has chosen something to attack
 * stands and fights, and goes back to fleeing the instant the target is gone.
 *
 * <p>Fire is the deliberate exception, and it matters for
 * {@link DhampirHandler dhampir}: a burning horse panics whatever it was doing,
 * because running is the correct response to being on fire and a dhampir that
 * stood in the sun trading blows would burn to death doing it.
 */
public final class HorsePanicGoal extends PanicGoal {

    private final Horse horse;

    public HorsePanicGoal(Horse horse, double speed) {
        super(horse, speed);
        this.horse = horse;
    }

    @Override
    public boolean canUse() {
        return !fighting() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !fighting() && super.canContinueToUse();
    }

    /**
     * Holding a live target - and not on fire. The fire clause is checked
     * first on purpose: burning outranks fighting, for a dhampir above all.
     */
    private boolean fighting() {
        if (horse.isOnFire() || horse.isFreezing()) {
            return false;
        }
        return horse.getTarget() != null && horse.getTarget().isAlive();
    }
}
