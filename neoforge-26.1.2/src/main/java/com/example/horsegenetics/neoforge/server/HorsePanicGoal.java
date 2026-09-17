package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.progress.ProgressTask;
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
 * {@link SunSensitivityHandler sun sensitivity}: a burning horse panics whatever
 * it was doing, because running is the correct response to being on fire - unless
 * the fire is the sky, below.
 */
public final class HorsePanicGoal extends PanicGoal {

    private final Horse horse;

    public HorsePanicGoal(Horse horse, double speed) {
        super(horse, speed);
        this.horse = horse;
    }

    @Override
    public void start() {
        super.start();
        // The moment it bolts, rather than every tick of the flight.
        HorseProgress.completeForWatcher(horse, ProgressTask.WILD_BOLT);
    }

    @Override
    public boolean canUse() {
        return !holdGround() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !holdGround() && super.canContinueToUse();
    }

    /**
     * <b>Two reasons a horse should stand its ground instead of bolting.</b>
     *
     * <p><b>It is fighting.</b> A guardian or gladiator that has chosen a
     * target holds position; anything without an aggression gene never has one,
     * so it flees exactly as vanilla does.
     *
     * <p><b>Or it is a sun-sensitive horse on fire</b> - and this one is the
     * opposite of what it looks like. Panic is normally the right answer to
     * burning, and the first version of this class said so in as many words. It
     * is wrong for precisely one animal: a sun-sensitive horse (the dhampir, when
     * this was written) is not on fire because of a fire, it is on fire because
     * of <em>the sky</em>, and there is nothing to run away from. It has a goal
     * for this - {@link SunShadeGoal} finds a roof and
     * walks to it - and that goal sits at <b>priority 1</b>, the same priority
     * vanilla registers {@code PanicGoal} at. Same priority, same
     * {@code Flag.MOVE}, and vanilla's is added first in {@code registerGoals},
     * so panic took the flag and the shade goal never ran.
     *
     * <p>Which is the whole of <i>"the dhampir still died and couldn't escape
     * the sun"</i>: it was not failing to find shade, it was being prevented
     * from walking to shade it had already found, by a goal that was trying to
     * help. The movement anybody watching saw was the panic, not the journey.
     */
    private boolean holdGround() {
        if (horse.isOnFire() || horse.isFreezing()) {
            return sunSensitive();
        }
        return horse.getTarget() != null && horse.getTarget().isAlive();
    }

    /**
     * Resolved once and kept. {@code isSensitive} parses the horse's genotype and
     * this is asked every tick by two methods, on every horse in the world -
     * the same reasoning as {@code FoodTemptGoal}'s cached lookup. Lazy rather
     * than done in the constructor because goals are attached on entity join
     * and the record is filled on the founding tick, so there is often nothing
     * to read yet.
     */
    private Boolean sunSensitive;

    private boolean sunSensitive() {
        if (sunSensitive == null) {
            if (!HorseRecords.hasRealRecord(horse)) {
                return false;   // ask again next tick
            }
            sunSensitive = SunSensitivityHandler.isSensitive(horse);
        }
        return sunSensitive;
    }
}
