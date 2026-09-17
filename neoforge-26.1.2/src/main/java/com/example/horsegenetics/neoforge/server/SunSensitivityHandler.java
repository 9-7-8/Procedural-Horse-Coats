package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.progress.ProgressTask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Everything <b>sun sensitivity</b> does: the horse burns in daylight and runs
 * for shade or water.
 *
 * <p>The gene ({@code common.genetics.genes.SunSensitivityGene}) owns only the
 * alleles; this is the translator, hand-written rather than a set of
 * {@code effects} verbs for the reason the dhampir gene gave before it was split
 * - each would be a verb with exactly one user.
 *
 * <h4>Burning</h4>
 * Not vanilla's ignite-in-daylight: a horse set alight burns down far too fast,
 * and the point of the animal is that it survives the run to the treeline. It
 * takes {@link #SUN_DAMAGE} every {@link #SUN_INTERVAL} ticks while the sky is
 * clear above it and the sun is up.
 *
 * <h4>The goal goes on every horse</h4>
 * It used to be added only to a horse already known to be a dhampir at the
 * moment it joined the level - and a wild founder's record is written a tick
 * <i>after</i> it joins, so a horse that spawned wild never got the goal until
 * the chunk reloaded. {@link SunShadeGoal} now sits on every horse and asks the
 * genome once, lazily, which is the {@code HorsePanicGoal} pattern.
 *
 * <p>Shade-seeking was confirmed in-game on the dhampir (2026-09-13); the split
 * into its own locus is <b>not play-tested</b> - see {@code wiki/verification.html}.
 */
@EventBusSubscriber
public final class SunSensitivityHandler {

    private SunSensitivityHandler() {
    }

    /** Half a heart, every two seconds, while it is caught in the open. */
    private static final float SUN_DAMAGE = 1.0F;
    private static final int SUN_INTERVAL = 40;

    /** Above everything - it is on fire. See {@code HorsePanicGoal} for why panic yields to it. */
    public static final int SHADE_GOAL_PRIORITY = 1;

    @SubscribeEvent
    static void addGoals(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Horse horse)) {
            return;
        }
        for (WrappedGoal w : horse.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof SunShadeGoal) {
                return;     // already wired
            }
        }
        horse.goalSelector.addGoal(SHADE_GOAL_PRIORITY, new SunShadeGoal(horse));
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || !horse.isAlive()) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        // Staggered by entity id like the care tick, so a pen of them does not
        // all burn on the same frame.
        if ((horse.tickCount + horse.getId()) % SUN_INTERVAL != 0) {
            return;
        }
        // Sunlight first: it is a few block reads, and the genome parse only
        // happens for a horse actually standing in the open by day.
        if (!inSunlight(horse) || !isSensitive(horse)) {
            return;
        }
        horse.hurtServer(level, level.damageSources().onFire(), SUN_DAMAGE);
        level.sendParticles(ParticleTypes.SMOKE,
                horse.getX(), horse.getY() + horse.getBbHeight() * 0.7, horse.getZ(),
                5, 0.3, 0.3, 0.3, 0.01);
        // Only on a burn, which is already one tick in SUN_INTERVAL.
        HorseProgress.completeForWatcher(horse, ProgressTask.SUN_SENSITIVE_SEEN);
    }

    /**
     * Does daylight burn this horse? Read off the genome every time; callers that
     * ask every tick cache it themselves.
     */
    public static boolean isSensitive(Horse horse) {
        if (!HorseRecords.hasRealRecord(horse)) {
            return false;
        }
        try {
            Genotype gt = Genotype.parse(HorseRecords.of(horse).geneticCode());
            return Genes.SUN_SENSITIVITY.isSensitive(gt.pair(Genes.SUN_SENSITIVITY));
        } catch (RuntimeException bad) {
            return false;
        }
    }

    /**
     * Daylight on open sky, and not underwater. Water is shelter here - a horse
     * standing in a pond is safe, which is what makes "run to water" a real
     * answer and not just flavour.
     */
    public static boolean inSunlight(Horse horse) {
        if (!(horse.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!level.isBrightOutside() || level.isRainingAt(horse.blockPosition())) {
            return false;
        }
        if (horse.isInWater()) {
            return false;
        }
        BlockPos head = BlockPos.containing(horse.getX(), horse.getEyeY(), horse.getZ());
        return level.canSeeSky(head);
    }
}
