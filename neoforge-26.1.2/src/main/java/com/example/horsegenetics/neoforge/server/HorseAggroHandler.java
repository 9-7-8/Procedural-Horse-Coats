package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.CowboyBrand;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.common.genetics.genes.MagicFighterGene;
import com.example.horsegenetics.common.progress.ProgressTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import java.util.ArrayList;
import net.minecraft.world.entity.ai.goal.PanicGoal;
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
 *       and a {@link WildHorseForgetTargetGoal}. Only the forget goal gates on
 *       {@code !isTamed()}: the melee goal runs for anything with a target, which
 *       is how a tamed guardian or a tamed dam fights at all.</li>
 *   <li><b>{@link #onHorseHurt}</b> - on damage from a living attacker, the
 *       victim and every herd-mate within {@value #HERD_ALERT_RADIUS} blocks
 *       target the attacker. A <b>cowboy's string</b> counts as a herd here,
 *       though it is held together by a brand rather than by a lead mare.</li>
 *   <li><b>{@link #onCowboyHurt}</b> - and hitting the <i>man</i> rallies them
 *       too, which is what makes killing him for his horses cost something.</li>
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

    /** Vanilla's own horse panic speed, kept so nothing else about fleeing changes. */
    private static final double PANIC_SPEED = 1.2;

    private HorseAggroHandler() {
    }

    /** Mod bus: {@code EntityAttributeModificationEvent} is an {@code IModBusEvent}. */
    @SubscribeEvent
    static void addAttackDamage(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.HORSE, Attributes.ATTACK_DAMAGE)) {
            // The baseline lives on the FIGHTER GENE, not here. That locus is
            // what a player reads to find out what a horse hits for, and its
            // wild type has to mean the same number as a horse that has never
            // been near it - so this registration takes the gene's value rather
            // than repeating one.
            event.add(EntityType.HORSE, Attributes.ATTACK_DAMAGE,
                    MagicFighterGene.BASELINE_DAMAGE);
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
        horse.goalSelector.addGoal(MELEE_GOAL_PRIORITY, new HorseMeleeGoal(horse, 1.4));
        horse.targetSelector.addGoal(TARGET_GOAL_PRIORITY, new WildHorseForgetTargetGoal(horse));

        // AND SWAP VANILLA'S PANIC FOR ONE THAT KNOWS ABOUT FIGHTING. Horses
        // register PanicGoal at priority ONE; the melee goal above is at three;
        // both hold Flag.MOVE, and the lower number wins. So the first hit a
        // fighting horse took handed movement to panic, the melee goal starved
        // for the whole flight, and the animal ran away from the fight its gene
        // had just started - "the guardian makes a half-hearted attempt, is
        // terrible at combat, runs off, then gets chased by the zombie and gets
        // killed by it" (owner, 2026-09-13). Nothing about attack speed or
        // damage could have fixed that, which is why making the swing faster
        // did not save the gladiator either.
        List<WrappedGoal> panics = new ArrayList<>();
        for (WrappedGoal w : horse.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof PanicGoal && !(w.getGoal() instanceof HorsePanicGoal)) {
                panics.add(w);
            }
        }
        for (WrappedGoal w : panics) {
            horse.goalSelector.removeGoal(w.getGoal());
            horse.goalSelector.addGoal(w.getPriority(), new HorsePanicGoal(horse, PANIC_SPEED));
        }
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
        // A takeover fight is between two stallions; the band does not pile in.
        if (BandLife.inFight(victim)) {
            return;
        }

        aggro(victim, attacker);
        // A cowboy's string rallies the same way a wild band does, and for the
        // same reason - it is a herd, it just has a man at the head of it
        // instead of a mare. It is a different field, though: a branded horse
        // carries the cowboy's id and is not `inWildHerd`, so the gate below
        // would have turned six horses standing together into six strangers.
        HorseCareAttachment care = victim.getData(ModAttachments.HORSE_CARE.get());
        if (!care.inWildHerd() && !branded(victim).isBranded()) {
            return;
        }
        List<Horse> herd = victim.level().getEntitiesOfClass(Horse.class,
                victim.getBoundingBox().inflate(HERD_ALERT_RADIUS),
                h -> h != victim && !h.isTamed() && sameHerd(h, victim) && YardPens.together(victim, h));
        for (Horse mate : herd) {
            aggro(mate, attacker);
        }
    }

    /**
     * <b>Rob the man and his string turns on you</b> (owner, 2026-09-23).
     * Killing a cowboy clears the brand off his herd and leaves them there to be
     * tamed ({@code CowboyHandler.onCowboyDied}), which is the mod's one way to
     * get one of his horses without emeralds - and until now it cost nothing but
     * the swings, because he flees on a {@code PanicGoal} and six horses stood
     * and watched. They are the price of the theft: every branded horse within
     * {@value #HERD_ALERT_RADIUS} blocks of him takes the attacker as its target,
     * and forgets it the ordinary way once it has lost sight of them for
     * {@value WildHorseForgetTargetGoal#FORGET_TICKS} ticks.
     *
     * <p>Not restricted to players, though in practice only a player can get
     * here: {@code CowboySafetyHandler} makes an {@link Enemy} incapable of
     * targeting him at all.
     *
     * <p>{@code liveHerd} covers loaded horses only, which is the right set -
     * an unloaded one could not have seen it happen.
     */
    @SubscribeEvent
    static void onCowboyHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Cowboy cowboy)
                || !(cowboy.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == cowboy) {
            return;
        }
        double radiusSqr = HERD_ALERT_RADIUS * HERD_ALERT_RADIUS;
        for (Horse horse : cowboy.liveHerd(level)) {
            if (!horse.isTamed() && horse.distanceToSqr(cowboy) <= radiusSqr) {
                aggro(horse, attacker);
            }
        }
    }

    /** How much harder a horse's kick lands on a monster than on anything else. */
    static final float HOSTILE_KICK_MULTIPLIER = 2.0F;

    /**
     * <b>A horse hits a monster harder than the monster hits back</b> (owner,
     * 2026-09-14: "It should be greater than a zombie when it's attacking hostile mobs
     * (not players or passive mobs)"). An ordinary horse's kick is
     * {@code MagicFighterGene.BASELINE_DAMAGE}, 3 - exactly a zombie's hit on Normal - and
     * a guardian lost that trade twice, because a horse also spends part of every second
     * repositioning. Doubled against anything that is an {@link Enemy}, a plain horse
     * kicks for 6 and a gladiator keeps its lead over it; a player, a cow or another
     * horse still takes the gene's own number.
     *
     * <p>Only the melee kick (a {@code mob_attack} from the horse itself), so a gene that
     * deals damage in the horse's name - cleansing light, say - is not doubled with it.
     */
    @SubscribeEvent
    static void onHorseKicksMonster(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Enemy) || event.getEntity() instanceof Player) {
            return;
        }
        if (event.getSource().getDirectEntity() instanceof Horse
                && event.getSource().is(DamageTypes.MOB_ATTACK)) {
            event.setAmount(event.getAmount() * HOSTILE_KICK_MULTIPLIER);
        }
    }

    /**
     * <b>A kick that landed.</b> Here rather than in {@link HorseMeleeGoal},
     * which knows it swung but not whether the blow reached anybody - a creative
     * player is targeted and kicked at and takes nothing, and a box that ticks
     * for that is telling the player they have been kicked when they have not.
     */
    @SubscribeEvent
    static void onPlayerKicked(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player hit)) {
            return;
        }
        if (event.getSource().getDirectEntity() instanceof Horse kicker && !kicker.isTamed()
                && event.getSource().is(DamageTypes.MOB_ATTACK)) {
            HorseProgress.complete(hit, ProgressTask.WILD_KICK);
        }
    }

    /**
     * A tamed horse taking damage, credited to its owner. What the task is
     * really about is what does <i>not</i> happen next - vanilla's trickle of
     * healing is off for horses - so the hurt itself is the whole trigger.
     */
    @SubscribeEvent
    static void onTamedHorseHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Horse horse) || !horse.isTamed()) {
            return;
        }
        // getOwner() is null for an owner who is not loaded here, and
        // HorseProgress ignores anything that is not a ServerPlayer.
        if (horse.getOwner() instanceof Player owner) {
            HorseProgress.complete(owner, ProgressTask.HORSE_INJURED);
        }
    }

    private static void aggro(Horse horse, LivingEntity target) {
        // PASSIFICATION VETOES EVERY ROUTE TO A TARGET, this one included. A
        // horse that has taken your offering does not turn on you because you
        // hit it, and does not join its herd-mates when somebody else does.
        // "Will not select the player as a target for any reason" only means
        // anything if each reason asks.
        if (Passification.suppresses(horse, target)) {
            return;
        }
        // A creative / spectator player is still targeted (the horse rears and
        // kicks) but takes no damage - which is the right feedback in testing.
        horse.setTarget(target);
        horse.setLastHurtByMob(target);
    }

    private static CowboyBrand branded(Horse horse) {
        CowboyBrand brand = horse.getData(ModAttachments.COWBOY_BRAND.get());
        return brand == null ? CowboyBrand.NONE : brand;
    }

    /**
     * Two horses in one string, or two in one wild band. The brand is asked
     * first and answers on its own: a branded horse's {@code care.herd} holds
     * the <i>cowboy's</i> id rather than a lead horse's and its
     * {@code inWildHerd} is false, so the wild-band test below says no to a pair
     * that plainly belongs together.
     */
    private static boolean sameHerd(Horse a, Horse b) {
        CowboyBrand ba = branded(a);
        CowboyBrand bb = branded(b);
        if (ba.isBranded() || bb.isBranded()) {
            return ba.cowboy().equals(bb.cowboy());
        }
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
