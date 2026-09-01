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
import com.example.horsegenetics.neoforge.data.HorseCoatAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;

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
 *       other ({@link HorseNames#breed});</li>
 *   <li>its generation is {@code 1 + max(dam, sire)};</li>
 *   <li>its {@code speed} / {@code health} are rolled from the parents by
 *       {@link HorseStats#rollFoalStat} and pushed onto the foal's attributes;</li>
 *   <li>it's credited to the breeding player ({@code bredBy});</li>
 *   <li>if the dam is tamed, the foal is auto-tamed to the dam's owner.</li>
 * </ul>
 *
 * The foal's coat attachment is written here rather than at join time - the
 * inherited epigenetics can only be read while both parents are in hand.
 * {@link HorseGeneticsEventHandler} founds one from scratch for everything that
 * arrives without one (wild spawns, {@code /summon}, gallery horses).
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

        Genome childGenome = GeneticCodeCombiner.combine(
                genomeOf(damHorse, damRecord, rng), genomeOf(sireHorse, sireRecord, rng), rng);
        String childCode = childGenome.genotypeCode();
        int childGeneration = 1 + Math.max(damRecord.generation(), sireRecord.generation());
        int priorFoals = HorseRecords.offspringCount(parentA, damRecord.id(), sireRecord.id());
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
                childCode,
                damRecord.id(),
                sireRecord.id(),
                childGeneration)
                .withStats(childSpeed, childHealth)
                .withParentStats(parentStats);

        Player breeder = event.getCausedByPlayer();
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
        child.setData(ModAttachments.HORSE_COAT.get(), HorseCoatAttachment.from(childGenome));
    }

    /**
     * The parent's full genome: its stored genotype + epigenome. A parent that
     * predates its coat attachment (or whose attachment has drifted from its
     * record) founds one now, so the foal still inherits real allele copies
     * rather than nothing.
     */
    private static Genome genomeOf(Horse parent, HorseRecord record, Rng rng) {
        HorseCoatAttachment coat = parent.getData(ModAttachments.HORSE_COAT.get());
        if (coat != null && !coat.isUnassigned() && coat.genotypeCode().equals(record.geneticCode())) {
            return coat.genome();
        }
        Genome genome = Genome.of(Genotype.parse(record.geneticCode()), rng);
        parent.setData(ModAttachments.HORSE_COAT.get(), HorseCoatAttachment.from(genome));
        return genome;
    }

    /** Return the parent's record, creating a founder and/or backfilling stats if needed. */
    private static HorseRecord ensureParentRecord(Horse parent) {
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
