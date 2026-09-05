package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.EnumSet;
import java.util.List;

/**
 * <b>Wild horses aggro like wolves.</b> Hit one and its whole herd turns on the
 * attacker - a horse will path to and kick anything it is angry at. Break line
 * of sight for {@value WildHorseForgetTargetGoal#FORGET_TICKS} ticks and it
 * calms back down to neutral.
 *
 * <ul>
 *   <li><b>{@link #addAttackDamage}</b> (mod bus) - gives {@code EntityType.HORSE}
 *       an {@code ATTACK_DAMAGE} attribute (vanilla horses have none), so a kick
 *       actually hurts.</li>
 *   <li><b>{@link #addAggroGoals}</b> - every horse gets a {@link MeleeAttackGoal}
 *       and a {@link WildHorseForgetTargetGoal}; both self-gate on
 *       {@code !isTamed()}.</li>
 *   <li><b>{@link #onHorseHurt}</b> - on damage from a living attacker, the
 *       victim and every herd-mate within {@value #HERD_ALERT_RADIUS} blocks
 *       target the attacker.</li>
 * </ul>
 *
 * <p>Tamed horses are never affected. Debug-dimension horses take no damage
 * ({@code HorseGeneticsEventHandler.noHorseDamageInDebugDimension}), so they
 * never aggro either.
 */
@EventBusSubscriber
public final class HorseAggroHandler {

    private static final double HERD_ALERT_RADIUS = 24.0;
    private static final int MELEE_GOAL_PRIORITY = 3;
    private static final int TARGET_GOAL_PRIORITY = 1;

    private HorseAggroHandler() {
    }

    /** Mod bus: {@code EntityAttributeModificationEvent} is an {@code IModBusEvent}. */
    @SubscribeEvent
    static void addAttackDamage(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.HORSE, Attributes.ATTACK_DAMAGE)) {
            event.add(EntityType.HORSE, Attributes.ATTACK_DAMAGE, 4.0);
        }
    }

    @SubscribeEvent
    static void addAggroGoals(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Horse horse)) {
            return;
        }
        for (WrappedGoal w : horse.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof MeleeAttackGoal) {
                return; // already wired
            }
        }
        horse.goalSelector.addGoal(MELEE_GOAL_PRIORITY, new MeleeAttackGoal(horse, 1.4, true));
        horse.targetSelector.addGoal(TARGET_GOAL_PRIORITY, new WildHorseForgetTargetGoal(horse));
    }

    @SubscribeEvent
    static void onHorseHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Horse victim) || victim.isTamed() || victim.level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker == victim || (attacker instanceof Horse h && sameHerd(h, victim))) {
            return;
        }

        aggro(victim, attacker);
        HorseCareAttachment care = victim.getData(ModAttachments.HORSE_CARE.get());
        if (!care.inWildHerd()) {
            return;
        }
        List<Horse> herd = victim.level().getEntitiesOfClass(Horse.class,
                victim.getBoundingBox().inflate(HERD_ALERT_RADIUS),
                h -> h != victim && !h.isTamed() && sameHerd(h, victim));
        for (Horse mate : herd) {
            aggro(mate, attacker);
        }
    }

    private static void aggro(Horse horse, LivingEntity target) {
        // A creative / spectator player is still targeted (the horse rears and
        // kicks) but takes no damage - which is the right feedback in testing.
        horse.setTarget(target);
        horse.setLastHurtByMob(target);
    }

    private static boolean sameHerd(Horse a, Horse b) {
        HorseCareAttachment ca = a.getData(ModAttachments.HORSE_CARE.get());
        HorseCareAttachment cb = b.getData(ModAttachments.HORSE_CARE.get());
        return ca.inWildHerd() && cb.inWildHerd() && ca.herd().equals(cb.herd());
    }

    /**
     * Clears a wild horse's target once it has had no line of sight to it for
     * {@value #FORGET_TICKS} ticks - "break eyesight and they go back to
     * neutral". Also clears a dead / gone target immediately.
     */
    public static final class WildHorseForgetTargetGoal extends Goal {

        static final int FORGET_TICKS = 60; // 3 seconds

        private final Horse horse;
        private int noLosTicks;

        public WildHorseForgetTargetGoal(Horse horse) {
            this.horse = horse;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return !horse.isTamed() && horse.getTarget() != null;
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
            this.noLosTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = horse.getTarget();
            if (target == null || !target.isAlive() || target.isRemoved()) {
                horse.setTarget(null);
                return;
            }
            if (horse.getSensing().hasLineOfSight(target)) {
                this.noLosTicks = 0;
            } else if (++this.noLosTicks >= FORGET_TICKS) {
                horse.setTarget(null);
                horse.setLastHurtByMob(null);
            }
        }
    }
}
