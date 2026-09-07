package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.data.HorseCooldownsAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * <b>The only way a dhampir heals.</b> Below full health it goes looking for
 * something living and takes one bite out of it: half a heart of damage dealt,
 * three hearts healed, and then it leaves that animal alone for a day.
 *
 * <p>It is deliberately <b>not</b> a predator. One bite per animal per day
 * against a cow with five hearts means the cow lives, walks off, and is bitten
 * again tomorrow - so a dhampir kept beside a pen of livestock sustains itself
 * without emptying the pen, and one kept alone starves slowly. That balance is
 * the whole design: the triple health is paid for by needing a herd of
 * something else nearby.
 *
 * <h4>What it will bite</h4>
 * Anything living and <b>not undead</b> - so the passive animals, and creepers
 * and spiders, but never a zombie or a skeleton. Two exclusions on top:
 * <ul>
 *   <li><b>players</b>, because a horse that bit its owner to heal would be a
 *       hostile mob wearing a saddle;</li>
 *   <li><b>other horses</b>, which keeps a herd of dhampirs from eating each
 *       other and means a dhampir alone in a paddock of horses genuinely has
 *       nothing to live on.</li>
 * </ul>
 *
 * <h4>Where the day is counted</h4>
 * On the <b>prey</b>, not on the horse: {@link HorseCooldownsAttachment} under
 * {@link #BITTEN_KEY}. Storing it on the horse would mean a growing map of every
 * animal it has ever met; storing it on the animal is one number that goes away
 * with the animal. It also makes the rule "one bite per animal per day" rather
 * than "per pair", which is the reading that keeps two dhampirs from
 * double-draining the same cow.
 */
public final class DhampirHuntGoal extends Goal {

    /** The cooldown key stamped on the <i>prey</i>. */
    public static final String BITTEN_KEY = "dhampir-bitten";

    private static final double SEARCH_RADIUS = 16.0;
    private static final double BITE_RANGE_SQ = 4.0;    // 2 blocks
    private static final double SPEED = 1.2;
    private static final float DAMAGE_DEALT = 1.0F;     // half a heart
    private static final float HEALED = 6.0F;           // three hearts
    private static final int REPATH_INTERVAL = 20;
    /** Give up on one animal after this long rather than trailing it forever. */
    private static final int MAX_PURSUIT = 300;

    private final Horse horse;
    private LivingEntity prey;
    private int repathCooldown;
    private int pursuitTicks;

    public DhampirHuntGoal(Horse horse) {
        this.horse = horse;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (horse.isVehicle() || horse.isLeashed() || horse.getHealth() >= horse.getMaxHealth()) {
            return false;
        }
        if (DhampirHandler.inSunlight(horse)) {
            return false;   // shelter first; the shade goal owns it while it burns
        }
        prey = findPrey();
        return prey != null;
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
            horse.getNavigation().moveTo(prey, SPEED);
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
        horse.heal(HEALED);
        stamp(prey, level.getGameTime());

        level.playSound(null, horse.getX(), horse.getY(), horse.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.8F, 0.6F);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                prey.getX(), prey.getY() + prey.getBbHeight() * 0.6, prey.getZ(),
                6, 0.2, 0.2, 0.2, 0.0);

        prey = null;    // one bite, then look elsewhere
        horse.getNavigation().stop();
    }

    /**
     * The nearest thing worth biting that has not been bitten today. Sorted by
     * distance so a dhampir works its way round a pen rather than fixating.
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
                .min(Comparator.comparingDouble(horse::distanceToSqr))
                .orElse(null);
    }

    /** See the class note: living, not undead, not a player, not a horse. */
    private boolean isValidPrey(LivingEntity entity) {
        if (!entity.isAlive() || entity == horse) {
            return false;
        }
        if (entity instanceof Player || entity instanceof AbstractHorse) {
            return false;
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
