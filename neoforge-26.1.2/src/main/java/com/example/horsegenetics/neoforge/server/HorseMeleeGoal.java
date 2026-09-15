package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * <b>A horse that swings twice as often as the thing it is fighting, and rears
 * when it does.</b>
 *
 * <p>Owner, 2026-09-13: <i>"gladiator died to a zombie because their attack
 * frequency was too low. Make horses attack at least as frequently as a zombie
 * does"</i> - and, later the same day, after a guardian lost a fight by one
 * second: <i>"Can you make horses attack faster/more frequently?"</i>
 *
 * <h2>The first version never changed the cadence at all</h2>
 * It overrode {@code getAttackInterval()} to twelve ticks and was documented as
 * having fixed the problem. It had not, and the bytecode says why:
 * {@code MeleeAttackGoal.checkAndPerformAttack} calls {@code resetAttackCooldown()},
 * and <b>{@code resetAttackCooldown} writes a hard-coded {@code 20}</b> into a
 * private field. {@code getAttackInterval()} is not on that path. So every horse
 * went on swinging at a zombie's exact rate, and the log shows what that looks
 * like: a tamed guardian dead at 15:09:59 and its zombie dead at 15:10:00. An
 * even trade, lost by a hair - which is what the unchanged interval predicts.
 *
 * <p>The fix is to own the cooldown. {@link #resetAttackCooldown} sets
 * {@link #cooldown}, {@link #isTimeToAttack} reads it, and {@link #tick} counts it
 * down before the attack check, the same order vanilla uses for its own field.
 * That works because vanilla's {@code canPerformAttack} asks
 * {@code isTimeToAttack()} virtually rather than reading the field (checked in
 * bytecode, not assumed), and it leaves the private field inert: it is set to
 * zero on {@code start} and nothing ever raises it again.
 *
 * <h2>Why twice a zombie's rate</h2>
 * A zombie walks straight at what it is hitting; a horse is a pathfinding animal
 * with a wide body that spends part of every second repositioning, so an even
 * interval lands fewer hits for the horse than for the zombie. {@value #ATTACK_INTERVAL}
 * ticks is two swings a second against the zombie's one, which is headroom
 * rather than parity - the thing the first attempt was meant to give and never
 * did.
 *
 * <h2>The rear is not decoration</h2>
 * <a href="known-gaps.html#gap-213">Gap 213</a> is the other half of the same
 * report and it is the half nobody can fix with a number: <i>"it kind of stands
 * there and then a beat later deals damage"</i>. A horse has <b>no attack
 * animation</b> - it is not a mob vanilla ever intended to melee - so the
 * wind-up is invisible and the damage appears to come from a standing animal.
 * {@link AbstractHorse#standIfPossible} is the animation the horse already has,
 * it reads as a strike, and it is what a real horse does to kick.
 *
 * <p>It rears on a swing <b>only if it is not already rearing</b>. At twelve
 * ticks that never mattered, because the swings were really twenty apart; at
 * ten, re-triggering it on every hit would keep the horse up on its hind legs
 * more or less permanently.
 *
 * <p><b>A rear used to stop the fight.</b> Vanilla counts a standing horse as
 * immobile, and an immobile mob runs no goals at all, so every rear froze this
 * goal and its cooldown for twenty ticks and the ten-tick swing never happened
 * (gap 222, the yard's KICK GLADIATOR pen: 59 ticks between blows).
 * {@code mixin.HorseRearFightMixin} lets a rearing horse with a live target keep
 * running its AI; the animation is unchanged.
 */
public final class HorseMeleeGoal extends MeleeAttackGoal {

    /** Ticks between swings: ten, against the twenty of a zombie and of vanilla's goal. */
    private static final int ATTACK_INTERVAL = 10;

    private final Horse horse;

    /** Ticks until this horse may swing again. Ours, not vanilla's private field - see the class note. */
    private int cooldown;

    public HorseMeleeGoal(Horse horse, double speed) {
        super(horse, speed, true);
        this.horse = horse;
    }

    @Override
    public void start() {
        super.start();
        cooldown = 0;
    }

    @Override
    public void tick() {
        // Down BEFORE super.tick(), which is where the attack check happens -
        // the order vanilla uses for its own field.
        cooldown = Math.max(cooldown - 1, 0);
        super.tick();
    }

    @Override
    protected void resetAttackCooldown() {
        cooldown = adjustedTickDelay(ATTACK_INTERVAL);
    }

    @Override
    protected boolean isTimeToAttack() {
        return cooldown <= 0;
    }

    @Override
    protected int getTicksUntilNextAttack() {
        return cooldown;
    }

    @Override
    protected int getAttackInterval() {
        return adjustedTickDelay(ATTACK_INTERVAL);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        // canPerformAttack already includes isTimeToAttack - asked before the
        // super call, which resets the cooldown and would make it read false.
        boolean willSwing = canPerformAttack(target);
        super.checkAndPerformAttack(target);
        if (willSwing && !horse.isStanding()) {
            // AFTER the hit, not before. The rear is a readout of what just
            // happened rather than a wind-up: a wind-up that plays and then
            // misses (the target stepped out of reach in the same tick) reads
            // as the horse attacking thin air, which is the complaint this is
            // meant to answer rather than a new spelling of it.
            horse.standIfPossible();
        }
    }
}
