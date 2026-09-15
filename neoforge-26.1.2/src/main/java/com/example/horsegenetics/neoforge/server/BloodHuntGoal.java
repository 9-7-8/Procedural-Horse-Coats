package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.care.Hunger;
import com.example.horsegenetics.common.genetics.Diet;
import com.example.horsegenetics.neoforge.data.HorseCooldownsAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * <b>The only way a blood-diet horse heals.</b> Below full health it goes
 * looking for something living and takes one bite out of it: half a heart of
 * damage dealt, three hearts healed, and then it leaves that animal alone for a
 * day. Written for the dhampir, and moved to the diet locus's {@code Dbld}
 * allele when that gene was split.
 *
 * <p>It is deliberately <b>not</b> a predator. One bite per animal per day
 * against a cow with five hearts means the cow lives, walks off, and is bitten
 * again tomorrow - so a blood-drinker kept beside a pen of livestock sustains
 * itself without emptying the pen, and one kept alone starves slowly.
 *
 * <h4>What it will bite, and in what order</h4>
 * Owner, 2026-09-13: <i>"They can feed on any mob in the game, including
 * players, but they'll only bite players if there's literally nothing else
 * around. Same with tamed cats and dogs: those are the lowest priority."</i>
 * <ol>
 *   <li><b>Any living mob</b> - passive or hostile - nearest first;</li>
 *   <li>then, only when there is no such mob in reach, <b>a player</b> (never
 *       one in creative or spectator), its owner included;</li>
 *   <li>then, lowest of all, <b>a tamed animal</b> - a cat, a dog, a parrot.</li>
 * </ol>
 * Never <b>another horse</b> (owner's call - it keeps a herd of them from eating
 * each other) and never anything <b>undead</b>, which has no living blood to take.
 *
 * <h4>Where the day is counted</h4>
 * On the <b>prey</b>, not on the horse: {@link HorseCooldownsAttachment} under
 * {@link #BITTEN_KEY}. Storing it on the horse would mean a growing map of every
 * animal it has ever met; storing it on the animal is one number that goes away
 * with the animal. It also makes the rule "one bite per animal per day" rather
 * than "per pair", so two blood-drinkers cannot double-drain the same cow.
 *
 * <p>Sits on every horse, like {@link SunShadeGoal}, and asks the diet once.
 * <b>Not play-tested</b> in this form - biting a player is new.
 */
public final class BloodHuntGoal extends Goal {

    /** The cooldown key stamped on the <i>prey</i>. Kept from the dhampir, so an old stamp still counts. */
    public static final String BITTEN_KEY = "dhampir-bitten";

    private static final double SEARCH_RADIUS = 16.0;
    private static final double BITE_RANGE_SQ = 4.0;    // 2 blocks
    private static final double SPEED = 1.2;
    private static final float DAMAGE_DEALT = 1.0F;     // half a heart
    private static final float HEALED = 6.0F;           // three hearts
    private static final int REPATH_INTERVAL = 20;
    /** Give up on one animal after this long rather than trailing it forever. */
    private static final int MAX_PURSUIT = 300;
    /** Re-check the surroundings no more often than this while idle and hurt. */
    private static final int SEARCH_INTERVAL = 20;

    private final Horse horse;
    private LivingEntity prey;
    private int repathCooldown;
    private int pursuitTicks;
    private int searchCooldown;

    /** Resolved once - see {@link SunShadeGoal#sensitive}. */
    private Boolean drinksBlood;
    private Boolean sunSensitive;

    public BloodHuntGoal(Horse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean drinksBlood() {
        if (drinksBlood == null) {
            if (!HorseRecords.hasRealRecord(horse)) {
                return false;
            }
            drinksBlood = HorseDietHandler.dietOf(horse).diet() == Diet.BLOOD;
            sunSensitive = SunSensitivityHandler.isSensitive(horse);
        }
        return drinksBlood;
    }

    @Override
    public boolean canUse() {
        if (!drinksBlood()) {
            return false;
        }
        if (horse.isVehicle() || horse.isLeashed() || horse.getHealth() >= horse.getMaxHealth()) {
            return false;
        }
        if (sunSensitive && SunSensitivityHandler.inSunlight(horse)) {
            return false;   // shelter first; the shade goal owns it while it burns
        }
        // The entity scan is the expensive part - once a second is plenty for
        // an animal that is only mildly hungry.
        if (--searchCooldown > 0) {
            return false;
        }
        searchCooldown = SEARCH_INTERVAL;
        prey = findPrey();
        traceSearch();
        return prey != null;
    }

    /** Game tick of this horse's last search trace; one a minute is plenty to read a day by. */
    private long lastSearchTrace = Long.MIN_VALUE;
    /** Whether this pursuit has already said its path was refused. */
    private boolean pathRefusalTraced;

    /**
     * <b>Why a hurt blood-drinker is or is not hunting</b> (gap 250). In the yard's 08:47 run BLOOD ONLY went a whole
     * day beside a cow without a bite, while an identical horse in the next row bit at once; the next run it bit at
     * tick 111. Nothing said which half failed - the search or the walk to the prey. At most once a minute per horse:
     * what the search found, or how many living things were in range and why each was ruled out.
     */
    private void traceSearch() {
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (now - lastSearchTrace < 1_200L) {
            return;
        }
        lastSearchTrace = now;
        if (prey != null) {
            ActionTrace.log("blood", ActionTrace.describeShort(horse) + " hunting " + ActionTrace.describeShort(prey)
                    + String.format(" (%.1f blocks, tier %d), health %.1f/%.1f", Math.sqrt(horse.distanceToSqr(prey)),
                    tier(prey), horse.getHealth(), horse.getMaxHealth()));
            return;
        }
        int inRange = 0;
        int invalid = 0;
        int bittenToday = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, horse.getBoundingBox().inflate(SEARCH_RADIUS),
                e -> e != horse)) {
            inRange++;
            if (!isValidPrey(e)) {
                invalid++;
            } else if (!readyToBite(e, now)) {
                bittenToday++;
            }
        }
        ActionTrace.log("blood", ActionTrace.describeShort(horse) + " found nothing to bite: " + inRange
                + " living in range, " + invalid + " not prey (horse, undead, creative), " + bittenToday
                + String.format(" already bitten today; health %.1f/%.1f", horse.getHealth(), horse.getMaxHealth()));
    }

    @Override
    public boolean canContinueToUse() {
        return prey != null && prey.isAlive() && pursuitTicks < MAX_PURSUIT
                && horse.getHealth() < horse.getMaxHealth()
                && horse.distanceToSqr(prey) < SEARCH_RADIUS * SEARCH_RADIUS * 4;
    }

    @Override
    public void start() {
        repathCooldown = 0;
        pursuitTicks = 0;
        pathRefusalTraced = false;
    }

    @Override
    public void stop() {
        prey = null;
        horse.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (prey == null) {
            return;
        }
        pursuitTicks++;
        horse.getLookControl().setLookAt(prey, 30.0F, 30.0F);
        if (--repathCooldown <= 0) {
            repathCooldown = REPATH_INTERVAL;
            if (!horse.getNavigation().moveTo(prey, SPEED) && !pathRefusalTraced) {
                pathRefusalTraced = true;       // once per pursuit: the other half of gap 250's question
                ActionTrace.log("blood", ActionTrace.describeShort(horse) + " has NO PATH to "
                        + ActionTrace.describeShort(prey) + String.format(" (%.1f blocks)", Math.sqrt(horse.distanceToSqr(prey))));
            }
        }
        if (horse.distanceToSqr(prey) <= BITE_RANGE_SQ) {
            bite();
        }
    }

    private void bite() {
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        prey.hurtServer(level, level.damageSources().mobAttack(horse), DAMAGE_DEALT);
        // THE BITE IS ITS MEAL (hunger, owner 2026-09-14: "blood diet just does the small ping
        // of damage"). It feeds first, then heals out of what it just ate, the way every heal
        // is paid for - so a starving blood-drinker's first bite still heals it.
        double hunger = Hunger.eat(horse.getData(ModAttachments.HUNGER.get()), Hunger.Food.BITE);
        double healed = Hunger.affordable(hunger, Math.min(HEALED, horse.getMaxHealth() - horse.getHealth()));
        horse.heal((float) healed);
        horse.setData(ModAttachments.HUNGER.get(), Hunger.afterHealing(hunger, healed));
        stamp(prey, level.getGameTime());
        ActionTrace.log("blood", ActionTrace.describeShort(horse) + " bit " + ActionTrace.describeShort(prey)
                + " (tier " + tier(prey) + ") at tick " + level.getGameTime() + String.format(
                " - healed %.1f, now %.1f/%.1f, hunger %.0f", healed, horse.getHealth(), horse.getMaxHealth(),
                horse.getData(ModAttachments.HUNGER.get())));

        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.8F, 0.6F);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                prey.getX(), prey.getY() + prey.getBbHeight() * 0.6, prey.getZ(),
                6, 0.2, 0.2, 0.2, 0.0);

        prey = null;    // one bite, then look elsewhere
        horse.getNavigation().stop();
    }

    /**
     * The best thing worth biting that has not been bitten today: the lowest
     * {@link #tier}, and the nearest within it - so a blood-drinker works its way
     * round a pen rather than fixating, and reaches for a player only when the
     * pen is empty.
     */
    private LivingEntity findPrey() {
        if (!(horse.level() instanceof ServerLevel level)) {
            return null;
        }
        long now = level.getGameTime();
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                horse.getBoundingBox().inflate(SEARCH_RADIUS),
                e -> isValidPrey(e) && readyToBite(e, now));
        return nearby.stream()
                .min(Comparator.<LivingEntity>comparingInt(BloodHuntGoal::tier)
                        .thenComparingDouble(horse::distanceToSqr))
                .orElse(null);
    }

    /** 0 an ordinary mob, 1 a player, 2 a tamed animal - see the class note. */
    static int tier(LivingEntity entity) {
        if (entity instanceof Player) {
            return 1;
        }
        if (entity instanceof TamableAnimal pet && pet.isTame()) {
            return 2;
        }
        return 0;
    }

    /** Living, not undead, not a horse; a mob, or a player who can be hurt. */
    private boolean isValidPrey(LivingEntity entity) {
        if (!entity.isAlive() || entity == horse || entity instanceof AbstractHorse) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        if (!(entity instanceof Mob)) {
            return false;
        }
        // EntityType has no is(TagKey) overload in 26.1.2 - go through its
        // registry holder, which is the general way to ask.
        return !entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
    }

    private static boolean readyToBite(LivingEntity prey, long now) {
        HorseCooldownsAttachment stamps = prey.getData(ModAttachments.HORSE_COOLDOWNS.get());
        return stamps == null || stamps.ready(BITTEN_KEY, now);
    }

    private static void stamp(LivingEntity prey, long now) {
        HorseCooldownsAttachment stamps = prey.getData(ModAttachments.HORSE_COOLDOWNS.get());
        prey.setData(ModAttachments.HORSE_COOLDOWNS.get(),
                (stamps == null ? HorseCooldownsAttachment.DEFAULT : stamps).stamp(BITTEN_KEY, now));
    }
}
