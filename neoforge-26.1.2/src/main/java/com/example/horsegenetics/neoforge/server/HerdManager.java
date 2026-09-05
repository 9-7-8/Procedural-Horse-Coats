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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Builds a wild horse's founder record and slots it into a <b>natural herd</b>,
 * one tick after it spawns (deferred from
 * {@code HorseGeneticsEventHandler.onHorseJoin} via {@code server.execute}).
 *
 * <h2>Herds by connected clump, with a deterministic lead</h2>
 * {@code Horse.finalizeSpawn} clobbers any custom pack {@code SpawnGroupData},
 * so the herd is formed here instead. A single 32-block look is <b>not enough</b>:
 * {@code NaturalSpawner} walks each pack member a cumulative +/-5 blocks from the
 * last, so a pack of four routinely spans more than {@link #HERD_RADIUS}
 * end-to-end, and the deferred tasks do not run in spatial order (least of all
 * for a pack streamed back off disk). The previous "first task to run founds,
 * everyone within 32 joins it" rule therefore let a spread pack <b>found two
 * herds of two breeds</b> whenever a far member's task ran before it could see
 * the near member that had already founded.
 *
 * <p>So a joining horse now walks the <b>whole connected clump</b> (a flood-fill
 * over untamed pack-mates, each within {@code HERD_RADIUS} of the next), starting
 * from itself:
 * <ul>
 *   <li>If any clump member is <b>already in a wild herd</b>, it <b>joins</b>
 *       that herd - same breed, band and lead.</li>
 *   <li>Else it elects the clump's <b>lowest-UUID member</b> as the lead and
 *       derives the breed and band from a {@link RandomSource} <b>seeded by that
 *       lead's UUID</b> and the lead's biome. Every member of the clump computes
 *       the same lead, so they all land on the same herd of the same breed no
 *       matter whose task runs first - the elected lead included, when its own
 *       task finally runs.</li>
 *   <li>A clump of one - a genuinely solitary horse - is a lone <b>Unknown</b>.</li>
 * </ul>
 */
public final class HerdManager {

    /** How far to look for herd-mates / pack-mates, per flood-fill step. */
    public static final double HERD_RADIUS = 32.0;

    /** Safety bound on a single flood-fill (a clump is a handful of packs at most). */
    private static final int MAX_CLUMP = 64;

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

        Horse herdedMate = wildSpawn ? herdedMemberOfClump(horse, level) : null;
        if (herdedMate != null) {
            // JOIN a herd that some clump-mate has already formed.
            HorseCareAttachment mate = herdedMate.getData(ModAttachments.HORSE_CARE.get());
            breed = Breeds.get(mate.herdBreed().orElse(""));
            band = parseBand(mate.herdBand().orElse(BandType.TRADITIONAL.name()));
            lead = mate.herd().orElse(herdedMate.getUUID());
            sex = lead.equals(horse.getUUID()) ? leadSex(horse, rng) : joinerSex(horse, band, rng);
        } else if (wildSpawn) {
            List<Horse> clump = freshClump(horse, level);
            if (clump.size() > 1) {
                // FOUND: elect the clump's lowest-UUID member as the lead and
                // derive everything from its UUID, so every member agrees.
                Horse leader = clump.stream()
                        .min(Comparator.comparing(Horse::getUUID))
                        .orElse(horse);
                lead = leader.getUUID();
                RandomSource seeded = seededFor(lead);
                breed = pickHerdBreed(level.getBiome(leader.blockPosition()), seeded);
                band = seeded.nextInt(10) < 7 ? BandType.TRADITIONAL : BandType.BACHELOR;
                sex = lead.equals(horse.getUUID()) ? leadSex(horse, rng) : joinerSex(horse, band, rng);
            } else {
                // genuinely alone -> a lone Unknown
                breed = Breeds.UNKNOWN;
                band = BandType.TRADITIONAL;
                lead = null;
                sex = coin(rng);
            }
        } else {
            // not a natural spawn (/summon, an imported horse) -> a lone Unknown
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

    /**
     * Walk the connected clump of fresh, untamed, still-unprocessed pack-mates
     * (each within {@link #HERD_RADIUS} of the next), starting from {@code origin}
     * and including it. Transitive, so a pack that spans more than one radius
     * end-to-end is still one clump.
     */
    private static List<Horse> freshClump(Horse origin, ServerLevel level) {
        List<Horse> found = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        ArrayDeque<Horse> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin.getUUID());
        found.add(origin);
        while (!queue.isEmpty() && found.size() < MAX_CLUMP) {
            Horse h = queue.poll();
            for (Horse n : level.getEntitiesOfClass(Horse.class, h.getBoundingBox().inflate(HERD_RADIUS),
                    o -> !seen.contains(o.getUUID()) && o.isAlive() && !o.isTamed()
                            && o.getPersistentData().getBooleanOr(BreedSpawnHandler.WILD_SPAWN_KEY, false)
                            && !HorseRecords.hasRealRecord(o))) {
                seen.add(n.getUUID());
                found.add(n);
                queue.add(n);
            }
        }
        return found;
    }

    /**
     * The first horse in {@code origin}'s connected clump that is already in a
     * wild herd, or {@code null}. The traversal passes <i>through</i> fresh
     * pack-mates (so a herd formed at the far end of a spread pack is still
     * found) as well as reaching horses that are themselves already herded.
     */
    private static Horse herdedMemberOfClump(Horse origin, ServerLevel level) {
        Set<UUID> seen = new HashSet<>();
        ArrayDeque<Horse> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin.getUUID());
        int visited = 0;
        while (!queue.isEmpty() && visited++ < MAX_CLUMP) {
            Horse h = queue.poll();
            for (Horse n : level.getEntitiesOfClass(Horse.class, h.getBoundingBox().inflate(HERD_RADIUS),
                    o -> !seen.contains(o.getUUID()) && o != origin && o.isAlive() && !o.isTamed())) {
                HorseCareAttachment c = n.getData(ModAttachments.HORSE_CARE.get());
                if (c.inWildHerd()) {
                    return n;
                }
                boolean freshMate = n.getPersistentData().getBooleanOr(BreedSpawnHandler.WILD_SPAWN_KEY, false)
                        && !HorseRecords.hasRealRecord(n);
                if (freshMate) {
                    seen.add(n.getUUID());
                    queue.add(n);
                }
            }
        }
        return null;
    }

    /** A stable per-lead randomness: every clump member derives the same herd from this. */
    private static RandomSource seededFor(UUID id) {
        return RandomSource.create(id.getMostSignificantBits() ^ id.getLeastSignificantBits());
    }

    private static Sex leadSex(Horse horse, Rng rng) {
        return horse.isBaby() ? coin(rng) : Sex.MALE; // the stallion / bachelor head
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
