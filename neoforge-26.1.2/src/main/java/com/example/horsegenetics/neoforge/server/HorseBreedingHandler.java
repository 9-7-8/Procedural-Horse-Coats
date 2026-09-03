package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.GeneticCodeCombiner;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.HorseStats;
import com.example.horsegenetics.common.horse.ParentStats;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;
import com.example.horsegenetics.common.name.HorseNames;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Real breeding. On {@link BabyEntitySpawnEvent} (fired from
 * {@code Animal#spawnChildFromBreeding} before the foal is added to the
 * world):
 *
 * <ul>
 *   <li>same-sex pairings are cancelled - breeding needs a mare and a stallion;</li>
 *   <li>the foal's <b>genome</b> is {@link GeneticCodeCombiner#combine}d from
 *       the parents - Mendelian alleles, each one carrying the priority and
 *       epigenetic seed of the exact parent copy it came from, so a foal that
 *       inherits its dam's {@code A} inherits her bay point heights too;</li>
 *   <li>its name takes the first name of one parent and the last name of the
 *       other ({@link HorseNames#breedNth});</li>
 *   <li>its generation is {@code 1 + max(dam, sire)};</li>
 *   <li>its {@code speed} / {@code health} are rolled from the parents by
 *       {@link HorseStats#rollFoalStat} and pushed onto the foal's attributes;</li>
 *   <li>it is credited to the breeding player ({@code bredBy});</li>
 *   <li>if the dam is tamed, the foal is auto-tamed to the dam's owner.</li>
 * </ul>
 *
 * <p>The foal-building step is {@link #applyBredFoal} - split out so the
 * <b>stallion seed jar</b> ({@code server/StallionSeedJarHandler}) can reuse it
 * with a synthetic sire record built from the jar's stored genome, instead of a
 * live stallion entity. The foal's coat attachment is written there rather than
 * at join time - the inherited epigenetics can only be read while both genomes
 * are in hand. {@link HorseGeneticsEventHandler} founds one from scratch for
 * everything that arrives without one (wild spawns, {@code /summon}, gallery
 * horses).
 */
@EventBusSubscriber
public final class HorseBreedingHandler {

    @SubscribeEvent
    static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof Horse parentA)) return;
        if (!(event.getParentB() instanceof Horse parentB)) return;
        if (!(event.getChild() instanceof Horse child)) return;

        Rng rng = HorseRecords.rng(child);

        HorseRecord recordA = ensureParentRecord(parentA);
        HorseRecord recordB = ensureParentRecord(parentB);

        if (recordA.sex() == recordB.sex()) {
            event.setCanceled(true);
            return;
        }

        boolean aIsDam = recordA.sex() == Sex.FEMALE;
        HorseRecord damRecord = aIsDam ? recordA : recordB;
        HorseRecord sireRecord = aIsDam ? recordB : recordA;
        Horse damHorse = aIsDam ? parentA : parentB;
        Horse sireHorse = aIsDam ? parentB : parentA;

        applyBredFoal(child, damHorse,
                genomeOf(damHorse, damRecord, rng), damRecord,
                genomeOf(sireHorse, sireRecord, rng), sireRecord,
                event.getCausedByPlayer(), rng);
    }

    /**
     * Populate a just-created foal entity from its two parents' genomes and
     * records: combined genome, code, generation, varied name, rolled stats,
     * {@code bredBy}, dam-owner taming, the {@link HorseRecord}, and the coat
     * attachment. Everything except adding the child to the world.
     *
     * <p>Shared by natural breeding and the stallion seed jar - the jar path
     * passes a synthetic {@code sireRecord} built from its stored genome and a
     * {@code null} live sire.
     *
     * @param child    the foal entity (already spawned for natural breeding;
     *                 created-but-not-yet-added for the seed-jar path)
     * @param damHorse the live dam - only its tame state / owner is read here
     * @param breeder  the player who caused the breeding, or {@code null}
     */
    static void applyBredFoal(Horse child, Horse damHorse,
                              Genome damGenome, HorseRecord damRecord,
                              Genome sireGenome, HorseRecord sireRecord,
                              @Nullable Player breeder, Rng rng) {
        Genome childGenome = GeneticCodeCombiner.combine(damGenome, sireGenome, rng);
        String childCode = childGenome.genotypeCode();
        int childGeneration = 1 + Math.max(damRecord.generation(), sireRecord.generation());
        int priorFoals = HorseRecords.offspringCount(child, damRecord.id(), sireRecord.id());
        NameParts childName = HorseNames.breedNth(
                new NameParts(damRecord.firstName(), damRecord.lastName()),
                new NameParts(sireRecord.firstName(), sireRecord.lastName()),
                priorFoals, HorseRecords.names(), rng);

        double childSpeed = HorseStats.rollFoalStat(damRecord.speed(), sireRecord.speed(), rng);
        double childHealth = HorseStats.rollFoalStat(damRecord.health(), sireRecord.health(), rng);
        ParentStats parentStats = ParentStats.of(
                damRecord.speed(), sireRecord.speed(), damRecord.health(), sireRecord.health());

        HorseRecord childRecord = HorseRecord.bred(
                child.getUUID(),
                HorseRecords.randomSex(rng),
                childName.first(),
                childName.last(),
                childGenome,
                damRecord.id(),
                sireRecord.id(),
                childGeneration)
                .withStats(childSpeed, childHealth)
                .withParentStats(parentStats);

        if (breeder != null) {
            childRecord = childRecord.withBredBy(breeder.getGameProfile().name());
        }

        // Foals born to a tamed dam are tamed to the dam's owner.
        if (damHorse.isTamed()) {
            child.setTamed(true);
            LivingEntity owner = damHorse.getOwner();
            if (owner != null) {
                child.setOwner(owner);
            }
            if (owner instanceof Player ownerPlayer) {
                childRecord = childRecord.withTamedBy(ownerPlayer.getGameProfile().name());
            }
        }

        HorseRecords.apply(child, childRecord);
        HorseRecords.applyStatsToEntity(child, childRecord, true);
    }

    /**
     * The parent's full genome, straight off its record. A parent whose record
     * predates the epigenome field founds one now, so the foal still inherits
     * real allele copies rather than nothing.
     */
    static Genome genomeOf(Horse parent, HorseRecord record, Rng rng) {
        if (record.hasGenome()) {
            return record.genome();
        }
        Genome genome = Genome.of(record.genotype(), rng);
        HorseRecords.apply(parent, record.withGenome(genome));
        return genome;
    }

    /** Return the parent's record, creating a founder and/or backfilling stats if needed. */
    static HorseRecord ensureParentRecord(Horse parent) {
        if (!HorseRecords.hasRealRecord(parent)) {
            HorseRecord founder = HorseRecords.newFounder(parent, HorseRecords.rng(parent));
            HorseRecords.apply(parent, founder);
            return founder;
        }
        HorseRecord record = HorseRecords.of(parent);
        if (!record.hasStats()) {
            record = record.withStats(HorseRecords.entitySpeed(parent), HorseRecords.entityHealth(parent));
            HorseRecords.apply(parent, record);
        }
        return record;
    }

    private HorseBreedingHandler() {
    }
}
