package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
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
 * Everything a <b>dhampir</b> does that is behaviour rather than genetics: it
 * burns in daylight, it runs for shade or water, and it hunts to heal.
 *
 * <p>The gene itself ({@code common.genetics.genes.DhampirGene}) owns the coat,
 * the eyes, the stat multipliers and the fact that the animal cannot be fed.
 * This is the translator, and it is a hand-written handler rather than a set of
 * {@code effects} verbs on purpose: each of the three would be a verb with
 * exactly one user, and the {@code effects} vocabulary is meant to be things a
 * <i>data-driven</i> gene would reach for. The herd, aggro and lethal-foal
 * behaviours are all hand-written for the same reason.
 *
 * <h4>Burning</h4>
 * Not vanilla's ignite-in-daylight: a horse set alight burns down far too fast,
 * and the point of the animal is that it survives the run to the treeline. It
 * takes {@link #SUN_DAMAGE} every {@link #SUN_INTERVAL} ticks while the sky is
 * clear above it and the sun is up, which on triple health is a long, losing
 * argument with the weather rather than an execution.
 *
 * <p><b>Not play-tested.</b> Written against 26.1.2 sources; see
 * {@code wiki/verification.html}.
 */
@EventBusSubscriber
public final class DhampirHandler {

    private DhampirHandler() {
    }

    /** Half a heart, every two seconds, while it is caught in the open. */
    private static final float SUN_DAMAGE = 1.0F;
    private static final int SUN_INTERVAL = 40;

    private static final int SHADE_GOAL_PRIORITY = 1;   // above everything - it is on fire
    private static final int HUNT_GOAL_PRIORITY = 3;

    // ------------------------------------------------------------------
    // Wiring
    // ------------------------------------------------------------------

    @SubscribeEvent
    static void addGoals(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Horse horse)) {
            return;
        }
        if (!isDhampir(horse)) {
            return;
        }
        for (WrappedGoal w : horse.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof DhampirShadeGoal) {
                return;     // already wired
            }
        }
        horse.goalSelector.addGoal(SHADE_GOAL_PRIORITY, new DhampirShadeGoal(horse));
        horse.goalSelector.addGoal(HUNT_GOAL_PRIORITY, new DhampirHuntGoal(horse));
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
        if (!inSunlight(horse) || !isDhampir(horse)) {
            return;
        }
        horse.hurtServer(level, level.damageSources().onFire(), SUN_DAMAGE);
        level.sendParticles(ParticleTypes.SMOKE,
                horse.getX(), horse.getY() + horse.getBbHeight() * 0.7, horse.getZ(),
                5, 0.3, 0.3, 0.3, 0.01);
    }

    // ------------------------------------------------------------------
    // Shared predicates - the goals use these too
    // ------------------------------------------------------------------

    /**
     * Is this horse the full animal? Read off the genome every time rather than
     * cached: it is one parse on a 40-tick stagger, and a cache would have to be
     * invalidated by every path that rewrites a horse's record.
     */
    public static boolean isDhampir(Horse horse) {
        if (!HorseRecords.hasRealRecord(horse)) {
            return false;
        }
        try {
            Genotype gt = Genotype.parse(HorseRecords.of(horse).geneticCode());
            return Genes.DHAMPIR.isDhampir(gt.pair(Genes.DHAMPIR));
        } catch (RuntimeException bad) {
            return false;
        }
    }

    /**
     * Daylight on open sky, and not underwater. Water is shelter here - a
     * dhampir standing in a pond is safe, which is what makes "run to water"
     * a real answer and not just flavour.
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
