package com.example.horsegenetics.neoforge.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * <b>A shifted lycanthrope's grudge.</b> Strike one and it follows and hits back
 * until sunrise, or until it loses sight of you.
 *
 * <p>One goal rather than the usual {@code MeleeAttackGoal} + target goal pair,
 * for one reason: <b>the forms have no attack</b>. A chicken has no
 * {@code ATTACK_DAMAGE} attribute at all, so {@code Mob.doHurtTarget} - which is
 * what {@code MeleeAttackGoal} calls - has nothing to read. The alternative was
 * to add the attribute to all thirty-seven vanilla mob types through
 * {@code EntityAttributeModificationEvent}, which would hand every sheep, cod
 * and wandering trader in the world an attack stat they never had, for the sake
 * of the one in ten thousand that is secretly a horse. Dealing a flat
 * {@value #DAMAGE} by hand touches nothing that is not already ours.
 *
 * <p>The two forget conditions are the requirement, verbatim: <b>sunrise</b> and
 * <b>eyesight</b>. Sunrise is checked here as well as in
 * {@link LycanthropyHandler} even though dawn reverts the animal anyway - the
 * revert happens on a {@value LycanthropyHandler#SUN_CHECK_INTERVAL}-tick
 * stagger, and a grudge that outlived the sun by two seconds is still a grudge
 * that outlived the sun. Eyesight uses the same
 * {@value #FORGET_TICKS}-tick memory as the wild horse herd's
 * ({@code HorseAggroHandler.WildHorseForgetTargetGoal}), so "break line of sight
 * and it calms down" means the same thing everywhere in the mod.
 *
 * <p><b>Not play-tested.</b> Written against 26.1.2 sources.
 */
public final class LycanTemperGoal extends Goal {

    /** Ticks without line of sight before the grudge lapses - three seconds, as for a wild horse. */
    static final int FORGET_TICKS = 60;

    /** Damage one strike deals. Flat, because most of the forms have no attack attribute to scale. */
    static final float DAMAGE = 3.0F;

    /** Ticks between strikes. */
    private static final int ATTACK_COOLDOWN = 20;

    /** How fast it closes. A little above a walk - it is angry, not a predator. */
    private static final double CHASE_SPEED = 1.2;

    /** Re-path this often rather than every tick; the target is moving. */
    private static final int REPATH_INTERVAL = 10;

    private final Mob animal;
    private int noLineOfSightTicks;
    private int attackCooldown;
    private int repathIn;

    public LycanTemperGoal(Mob animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = animal.getTarget();
        return target != null && target.isAlive() && !target.isRemoved();
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
        this.noLineOfSightTicks = 0;
        this.attackCooldown = 0;
        this.repathIn = 0;
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = animal.getTarget();
        if (target == null) {
            return;
        }
        if (!animal.level().isDarkOutside()) {
            forget();
            return;
        }
        if (animal.getSensing().hasLineOfSight(target)) {
            this.noLineOfSightTicks = 0;
        } else if (++this.noLineOfSightTicks >= FORGET_TICKS) {
            forget();
            return;
        }

        animal.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--this.repathIn <= 0) {
            this.repathIn = REPATH_INTERVAL;
            animal.getNavigation().moveTo(target, CHASE_SPEED);
        }

        if (--this.attackCooldown > 0) {
            return;
        }
        // The reach a vanilla melee goal would use for a mob this size, so a
        // strider's bite starts from further out than a tadpole's.
        double reach = animal.getBbWidth() * animal.getBbWidth() * 2.0 + target.getBbWidth();
        if (animal.distanceToSqr(target) <= reach && animal.level() instanceof ServerLevel level) {
            this.attackCooldown = ATTACK_COOLDOWN;
            animal.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            target.hurtServer(level, level.damageSources().mobAttack(animal), DAMAGE);
        }
    }

    private void forget() {
        animal.setTarget(null);
        animal.setLastHurtByMob(null);
        animal.getNavigation().stop();
    }
}
