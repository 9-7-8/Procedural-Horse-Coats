package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.name.HorseNameGenerator;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;
import com.example.horsegenetics.neoforge.NeoRng;
import com.example.horsegenetics.neoforge.data.HorseAncestryData;
import com.example.horsegenetics.neoforge.data.ModAttachments;
import com.example.horsegenetics.neoforge.network.HorseRecordSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * The Layer-2 boundary. Its only job is translation: build / read
 * {@link HorseRecord}s and move them between the domain layer and Minecraft's
 * own systems (the {@link ModAttachments#HORSE_RECORD} attachment, the
 * {@link HorseAncestryData} SavedData, and the entity's visible name). No
 * genetics, naming, or lookup logic lives here.
 */
public final class HorseRecords {

    private static final HorseNameGenerator NAMES = HorseNameGenerator.fromResources();

    private HorseRecords() {
    }

    /** The attachment default has no name at all; any assigned record does. */
    public static boolean hasRealRecord(Horse horse) {
        return of(horse).hasName();
    }

    public static HorseRecord of(Horse horse) {
        return horse.getData(ModAttachments.HORSE_RECORD.get());
    }

    /** Store {@code record} on the entity, in the ancestry DB, sync it, and set the visible name. */
    public static void apply(Horse horse, HorseRecord record) {
        horse.setData(ModAttachments.HORSE_RECORD.get(), record);
        horse.setCustomName(Component.literal(record.displayName()));
        horse.setCustomNameVisible(true);
        if (horse.level() instanceof ServerLevel level) {
            HorseAncestryData.get(level.getServer()).record(record);
            PacketDistributor.sendToPlayersTrackingEntity(horse,
                    new HorseRecordSyncPayload(horse.getId(), record));
        }
    }

    public static HorseRecord newFounder(Horse horse, Rng rng) {
        return newFounder(horse, rng, randomSex(rng));
    }

    /** Founder record with a forced sex (the horse dimension wants one mare + one stallion). */
    public static HorseRecord newFounder(Horse horse, Rng rng, Sex sex) {
        return newFounder(horse, rng, sex, Genotype.random(rng).toCode());
    }

    /**
     * Founder record with a forced sex <b>and</b> a forced genotype - the horse
     * dimension stocks each pen with a specific entry from
     * {@link com.example.horsegenetics.common.genetics.GenotypeCatalog}, so it
     * must not be re-rolled.
     */
    public static HorseRecord newFounder(Horse horse, Rng rng, Sex sex, String geneticCode) {
        NameParts name = NAMES.generateParts(rng);
        return HorseRecord.founder(horse.getUUID(), sex, name.first(), name.last(), geneticCode)
                .withStats(entitySpeed(horse), entityHealth(horse));
    }

    // --- speed / health stats ---

    public static double entitySpeed(Horse horse) {
        return horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    public static double entityHealth(Horse horse) {
        return horse.getAttributeValue(Attributes.MAX_HEALTH);
    }

    /**
     * Push a record's rolled {@code speed} / {@code health} onto the live
     * entity's attributes. No-op for values still at the {@code 0.0} sentinel.
     * {@code fullHeal} = set current HP to the new max (true for a newborn
     * foal, false on a reload so an injured horse isn't healed for free).
     */
    public static void applyStatsToEntity(Horse horse, HorseRecord record, boolean fullHeal) {
        if (record.speed() > 0.0) {
            AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(record.speed());
            }
        }
        if (record.health() > 0.0) {
            AttributeInstance health = horse.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(record.health());
                if (fullHeal) {
                    horse.setHealth((float) record.health());
                } else if (horse.getHealth() > record.health()) {
                    horse.setHealth((float) record.health());
                }
            }
        }
    }

    /**
     * If this horse's record predates the stat fields (old save, or a founder
     * whose stats never got copied), fill them from the live entity now.
     */
    public static void backfillStatsIfMissing(Horse horse) {
        HorseRecord record = of(horse);
        if (!record.hasStats()) {
            apply(horse, record.withStats(entitySpeed(horse), entityHealth(horse)));
        }
    }

    public static NameParts newNameParts(Rng rng) {
        return NAMES.generateParts(rng);
    }

    /** The shared name generator (for breeding, which needs random word draws). */
    public static HorseNameGenerator names() {
        return NAMES;
    }

    /**
     * How many foals {@code damId} x {@code sireId} have already produced,
     * from the server-global ancestry DB. Drives foal-name variation
     * ({@link com.example.horsegenetics.common.name.HorseNames#breedNth}).
     */
    public static int offspringCount(Horse contextHorse, java.util.UUID damId, java.util.UUID sireId) {
        if (contextHorse.level() instanceof ServerLevel level && level.getServer() != null) {
            return HorseAncestryData.get(level.getServer()).offspringCount(damId, sireId);
        }
        return 0;
    }

    /** Set the registered first/last name (name-tag hook). Callers guard "not both blank". */
    public static void rename(Horse horse, String firstName, String lastName) {
        apply(horse, of(horse).withNames(firstName, lastName));
    }

    /** Set or clear the free-form barn name (blank -> clear). */
    public static void setBarnName(Horse horse, String barnName) {
        String s = barnName == null ? "" : barnName.strip();
        apply(horse, of(horse).withBarnName(s.isEmpty() ? Optional.empty() : Optional.of(s)));
    }

    /** Record who tamed this horse, once, if not already set. */
    public static void setTamedBy(Horse horse, String username) {
        HorseRecord record = of(horse);
        if (record.tamedBy().isEmpty()) {
            apply(horse, record.withTamedBy(username));
        }
    }

    public static Sex randomSex(Rng rng) {
        return rng.nextBoolean() ? Sex.MALE : Sex.FEMALE;
    }

    public static Rng rng(Horse horse) {
        return new NeoRng(horse.getRandom());
    }
}
