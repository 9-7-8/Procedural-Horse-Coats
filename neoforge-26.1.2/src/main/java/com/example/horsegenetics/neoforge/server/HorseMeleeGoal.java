package com.example.horsegenetics.neoforge.server;

import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;

/**
 * <b>A horse that swings as often as the thing it is fighting, and rears when
 * it does.</b>
 *
 * <p>Owner, 2026-09-13: <i>"gladiator died to a zombie because their attack
 * frequency was too low. Make horses attack at least as frequently as a zombie
 * does."</i>
 *
 * <h2>The cadence, and why parity was not enough</h2>
 * A plain {@link MeleeAttackGoal} swings every {@code adjustedTickDelay(20)}
 * ticks, and vanilla's {@code ZombieAttackGoal} is the same goal with the same
 * number - so a horse and a zombie were already trading blows on the same beat,
 * at {@link com.example.horsegenetics.common.genetics.genes.MagicFighterGene#BASELINE_DAMAGE}
 * against a zombie's three. On paper that is a fair fight.
 *
 * <p>It is not one in practice, and the reason is everything either side of the
 * swing. <b>A zombie walks straight at what it is hitting; a horse is a
 * pathfinding animal with a wide body that spends part of every second
 * repositioning</b> - so an even interval produces an uneven number of landed
 * hits, and the horse loses ground it never gets back. {@value #ATTACK_INTERVAL}
 * ticks is the answer to "at least as frequently", with enough headroom that
 * the repositioning is paid for rather than merely matched.
 *
 * <h2>The rear is not decoration</h2>
 * <a href="known-gaps.html#gap-213">Gap 213</a> is the other half of the same
 * report and it is the half nobody can fix with a number: <i>"it kind of stands
 * there and then a beat later deals damage"</i>. A horse has <b>no attack
 * animation</b> - it is not a mob vanilla ever intended to melee - so the
 * wind-up is invisible and the damage appears to come from a standing animal.
 * Swinging faster without fixing that makes it look <em>worse</em>, because the
 * unexplained hits arrive more often.
 *
 * <p>{@link AbstractHorse#standIfPossible} is the animation the horse already
 * has, it reads as a strike, and it is what a real horse does to kick. So every
 * swing rears. Nothing else about the attack changes.
 */
public final class HorseMeleeGoal extends MeleeAttackGoal {

    /**
     * Ticks between swings. Vanilla's melee goal - and therefore a zombie - uses
     * twenty; this is comfortably under it, which is what "at least as
     * frequently as a zombie" asks for once the horse's repositioning is
     * accounted for.
     */
    private static final int ATTACK_INTERVAL = 12;

    private final Horse horse;

    public HorseMeleeGoal(Horse horse, double speed) {
        super(horse, speed, true);
        this.horse = horse;
    }

    @Override
    protected int getAttackInterval() {
        return adjustedTickDelay(ATTACK_INTERVAL);
    }

    @Override
    protected void checkAndPerformAttack(net.minecraft.world.entity.LivingEntity target) {
        boolean willSwing = isTimeToAttack() && canPerformAttack(target);
        super.checkAndPerformAttack(target);
        if (willSwing) {
            // AFTER the hit, not before. The rear is a readout of what just
            // happened rather than a wind-up: a wind-up that plays and then
            // misses (the target stepped out of reach in the same tick) reads
            // as the horse attacking thin air, which is the complaint this is
            // meant to answer rather than a new spelling of it.
            horse.standIfPossible();
        }
    }
}
