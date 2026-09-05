package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.BandType;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;
import com.example.horsegenetics.neoforge.data.HorseCareAttachment;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.network.CoatSyncPayload;
import com.example.horsegenetics.neoforge.network.HorseCareSyncPayload;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Builds a wild horse's founder record and slots it into a <b>natural herd</b>,
 * one tick after it spawns (deferred from
 * {@code HorseGeneticsEventHandler.onHorseJoin} via {@code server.execute}).
 *
 * <h2>Herds by proximity</h2>
 * {@code Horse.finalizeSpawn} clobbers any custom pack {@code SpawnGroupData},
 * so the herd is formed here instead: when a wild horse is processed it looks
 * at the untamed wild horses around it.
 * <ul>
 *   <li>If one of them is <b>already in a wild herd</b>, it <b>joins</b> that
 *       herd &mdash; same breed, same band, same lead.</li>
 *   <li>Else if one of them is <b>also a fresh natural spawn</b> (a pack-mate
 *       still waiting its turn), this horse <b>founds</b> a herd and becomes its
 *       lead stallion; the pack-mates join it as they are processed.</li>
 *   <li>Else it is genuinely alone &mdash; a lone <b>Unknown</b>, no herd.</li>
 * </ul>
 * So <b>every clump of wild horses is one herd of one breed</b>, and only a
 * truly solitary horse is Unknown.
 */
public final class HerdManager {

    /** How far to look for herd-mates / pack-mates. */
    public static final double HERD_RADIUS = 32.0;

    private HerdManager() {
    }

    /** Deferred: give a freshly-spawned wild horse its genome, record, traits and herd. */
    public static void assignFounder(Horse horse, Rng rng) {
        if (!(horse.level() instanceof ServerLevel level)) {
            return;
        }
        if (HorseRecords.hasRealRecord(horse)) {
            return; // already founded by another path
        }

        boolean wildSpawn = horse.getPersistentData().getBooleanOr(BreedSpawnHandler.WILD_SPAWN_KEY, false);

        Breed breed;
        BandType band;
        Sex sex;
        UUID lead;

        Horse herdMate = wildSpawn ? nearestHerdedWildHorse(horse, level) : null;
        if (herdMate != null) {
            // JOIN an existing herd
            HorseCareAttachment mate = herdMate.getData(ModAttachments.HORSE_CARE.get());
            breed = Breeds.get(mate.herdBreed().orElse(""));
            band = parseBand(mate.herdBand().orElse(BandType.TRADITIONAL.name()));
            lead = mate.herd().orElse(herdMate.getUUID());
            sex = joinerSex(horse, band, rng);
        } else if (wildSpawn && hasPendingPackmate(horse, level)) {
            // FOUND a herd - this horse is the lead
            breed = pickHerdBreed(level.getBiome(horse.blockPosition()), horse.getRandom());
            band = horse.getRandom().nextInt(10) < 7 ? BandType.TRADITIONAL : BandType.BACHELOR;
            lead = horse.getUUID();
            sex = horse.isBaby() ? coin(rng) : Sex.MALE; // the stallion / bachelor head
        } else {
            // genuinely alone (or not a natural spawn) -> a lone Unknown
            breed = Breeds.UNKNOWN;
            band = BandType.TRADITIONAL;
            lead = null;
            sex = coin(rng);
        }

        Genome genome = BreedFounder.roll(breed, rng, sex);
        String token = breed == Breeds.UNKNOWN
                ? BreedLineage.UNKNOWN.toToken()
                : BreedLineage.pure(breed.id()).toToken();
        NameParts name = HorseRecords.newNameParts(rng);
        HorseRecord record = HorseRecord.founder(horse.getUUID(), name.first(), name.last(), genome, token);

        horse.getPersistentData().remove(BreedSpawnHandler.WILD_SPAWN_KEY);
        HorseRecords.apply(horse, record);
        HorseRecords.applyTraitsToEntity(horse, record, true);
        PacketDistributor.sendToPlayersTrackingEntity(horse,
                CoatSyncPayload.of(horse.getId(), new CoatData(record.genome())));

        if (lead != null) {
            HorseCareAttachment care = horse.getData(ModAttachments.HORSE_CARE.get())
                    .withWildHerd(lead, breed.id(), band.name());
            horse.setData(ModAttachments.HORSE_CARE.get(), care);
            PacketDistributor.sendToPlayersTrackingEntity(horse,
                    new HorseCareSyncPayload(horse.getId(), care.bond(), care.inHerd()));
        }
    }

    // ------------------------------------------------------------------

    private static Horse nearestHerdedWildHorse(Horse horse, ServerLevel level) {
        return level.getEntitiesOfClass(Horse.class, horse.getBoundingBox().inflate(HERD_RADIUS),
                        h -> h != horse && h.isAlive() && !h.isTamed()
                                && h.getData(ModAttachments.HORSE_CARE.get()).inWildHerd())
                .stream()
                .min(Comparator.comparingDouble(horse::distanceToSqr))
                .orElse(null);
    }

    /** Another fresh natural spawn nearby that hasn't been processed yet - i.e. a pack-mate. */
    private static boolean hasPendingPackmate(Horse horse, ServerLevel level) {
        return !level.getEntitiesOfClass(Horse.class, horse.getBoundingBox().inflate(HERD_RADIUS),
                h -> h != horse && h.isAlive() && !h.isTamed()
                        && h.getPersistentData().getBooleanOr(BreedSpawnHandler.WILD_SPAWN_KEY, false)
                        && !HorseRecords.hasRealRecord(h)).isEmpty();
    }

    private static Sex joinerSex(Horse horse, BandType band, Rng rng) {
        if (horse.isBaby()) {
            return coin(rng);
        }
        return band == BandType.BACHELOR ? Sex.MALE : Sex.FEMALE;
    }

    private static Sex coin(Rng rng) {
        return rng.nextBoolean() ? Sex.MALE : Sex.FEMALE;
    }

    private static BandType parseBand(String s) {
        try {
            return BandType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return BandType.TRADITIONAL;
        }
    }

    /**
     * A herd's breed: weighted by the biome's breeds only. Unknown only when the
     * biome has no assigned breed at all.
     */
    public static Breed pickHerdBreed(Holder<Biome> biome, RandomSource rng) {
        String biomeId = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
        List<Breed> candidates = Breeds.forBiome(biomeId);
        if (candidates.isEmpty()) {
            return Breeds.UNKNOWN;
        }
        double total = 0.0;
        for (Breed b : candidates) {
            total += b.spawnWeight();
        }
        double roll = rng.nextDouble() * total;
        for (Breed b : candidates) {
            roll -= b.spawnWeight();
            if (roll < 0.0) {
                return b;
            }
        }
        return candidates.get(candidates.size() - 1);
    }
}
