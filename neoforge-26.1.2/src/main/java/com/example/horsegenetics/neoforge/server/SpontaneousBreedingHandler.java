package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

/**
 * <b>Horses that breed without being asked.</b> Two of them, both expressing
 * {@code SpontaneousBreedingGene}, within a few blocks of one another, and every
 * so often there is a foal.
 *
 * <h2>This is the most dangerous gene in the batch, and the caps come first</h2>
 * Uncapped automatic animal breeding is the classic way to kill a server, and
 * unlike every other behaviour gene it runs <b>while the player is asleep and
 * not watching</b>. So three limits are load-bearing rather than tuning, and
 * none of them may be relaxed without thinking about a pasture left running
 * overnight:
 *
 * <ul>
 *   <li>{@link #LOCAL_CAP} - if there are already this many horses nearby, the
 *       pair simply do not. This is the one that actually bounds the population,
 *       because it is a limit on the <i>outcome</i> rather than on the rate.</li>
 *   <li>{@link #CHECK_TICKS} - the pair are only even considered occasionally,
 *       and the scan is what costs anything here.</li>
 *   <li>Vanilla's own breeding cooldown, which is left entirely alone: this
 *       handler puts a horse <i>in love</i> and lets the ordinary breeding path
 *       do the rest, so age, cooldown, pedigree and stat inheritance all still
 *       run. It must never become a second breeding implementation.</li>
 * </ul>
 *
 * <h2>It takes two</h2>
 * Both horses must express. One expressing horse that bred with anything would
 * spread itself through a herd in a few generations and stop being rare;
 * requiring a pair makes the trait self-limiting genetically as well as
 * mechanically - the population can only grow where somebody has already
 * gathered two.
 */
@EventBusSubscriber
public final class SpontaneousBreedingHandler {

    private SpontaneousBreedingHandler() {
    }

    /** How far apart the pair may be. Small: they have to actually be together. */
    private static final double RANGE = 8.0;

    /**
     * Horses within {@link #RANGE} above which nothing happens at all.
     *
     * <p>The population limit, and the only one of the three that bounds the
     * outcome rather than the rate. A pasture that has reached it stays there
     * for ever rather than growing slowly.
     */
    private static final int LOCAL_CAP = 6;

    /** Ticks between considering it. Long - this is a scan, and it need never be prompt. */
    private static final int CHECK_TICKS = 600;

    @SubscribeEvent
    static void onHorseTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Horse horse) || horse.level().isClientSide()) {
            return;
        }
        // Offset off the horse's own tick count so a field of them does not all
        // scan on the same tick - the discipline every radius effect here follows.
        if ((horse.tickCount + horse.getId()) % CHECK_TICKS != 0) {
            return;
        }
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (horse.isBaby() || horse.isVehicle() || horse.isInLove() || !horse.canFallInLove()) {
            return;
        }
        if (!expresses(horse)) {
            return;
        }

        AABB box = horse.getBoundingBox().inflate(RANGE);
        List<Horse> nearby = level.getEntitiesOfClass(Horse.class, box, Horse::isAlive);
        if (nearby.size() > LOCAL_CAP) {
            return;     // the pasture is full; this is the cap that matters
        }

        for (Horse other : nearby) {
            if (other == horse || other.isBaby() || other.isInLove() || !other.canFallInLove()) {
                continue;
            }
            if (!expresses(other)) {
                continue;
            }
            // In love, not bred: the ordinary breeding path takes it from here,
            // so cooldowns, pedigree and stat inheritance all still apply.
            horse.setInLove(null);
            other.setInLove(null);
            return;
        }
    }

    /** Does this horse carry two copies? Both parents must, which is the whole gene. */
    private static boolean expresses(Horse horse) {
        HorseRecord record = HorseRecords.of(horse);
        if (!record.hasName()) {
            return false;
        }
        try {
            Genotype genotype = Genotype.parse(record.geneticCode());
            return Genes.SPONTANEOUS_BREEDING.expresses(
                    genotype.pair(Genes.SPONTANEOUS_BREEDING));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
