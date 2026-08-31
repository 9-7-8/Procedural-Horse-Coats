package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.GeneticCodeCombiner;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.HorseStats;
import com.example.horsegenetics.common.horse.ParentStats;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;
import com.example.horsegenetics.common.name.HorseNames;
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
 *   <li>the foal's genetic code is {@link GeneticCodeCombiner#combine}d from
 *       the parents;</li>
 *   <li>its name takes the first name of one parent and the last name of the
 *       other ({@link HorseNames#breed});</li>
 *   <li>its generation is {@code 1 + max(dam, sire)};</li>
 *   <li>its {@code speed} / {@code health} are rolled from the parents by
 *       {@link HorseStats#rollFoalStat} and pushed onto the foal's attributes;</li>
 *   <li>it's credited to the breeding player ({@code bredBy});</li>
 *   <li>if the dam is tamed, the foal is auto-tamed to the dam's owner.</li>
 * </ul>
 *
 * The coat is derived from the code when the foal joins the level
 * ({@link HorseGeneticsEventHandler}).
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

        String childCode = GeneticCodeCombiner.combine(damRecord.geneticCode(), sireRecord.geneticCode(), rng);
        int childGeneration = 1 + Math.max(damRecord.generation(), sireRecord.generation());
        NameParts childName = HorseNames.breed(
                new NameParts(damRecord.firstName(), damRecord.lastName()),
                new NameParts(sireRecord.firstName(), sireRecord.lastName()),
                rng);

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
