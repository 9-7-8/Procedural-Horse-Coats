package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.CoatGenerator;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.neoforge.ServerConfig;
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

    /**
     * A wild horse: everything rolled, <b>sex included</b> - it is a gene now,
     * so {@link Genotype#random} draws it along with the rest.
     */
    public static HorseRecord newFounder(Horse horse, Rng rng) {
        return newFounder(horse, rng, Genotype.random(rng));
    }

    /**
     * Founder record with a forced sex <b>and</b> a forced genotype - the horse
     * dimension stocks each pen with a rolled showcase genotype, and the custom
     * spawn egg with the one the player built, so neither may be re-rolled. The sex is written <i>into</i> the genotype
     * ({@link Genotype#withSex}) rather than beside it, because that is where a
     * horse's sex lives.
     *
     * <p>The <b>epigenome is rolled here</b>, in the same breath as the record,
     * because both now live on the record and a founder is exactly the horse
     * that is allowed to draw fresh ones. A foal must never come through this
     * path - it inherits its parents' allele copies verbatim
     * ({@code HorseBreedingHandler}).
     */
    public static HorseRecord newFounder(Horse horse, Rng rng, Sex sex, Genotype genotype) {
        return newFounder(horse, rng, genotype.withSex(sex));
    }

    /** Founder record with a forced genotype, whose sex locus is taken as-is. */
    public static HorseRecord newFounder(Horse horse, Rng rng, Genotype genotype) {
        return newFounder(horse, rng, CoatGenerator.generate(genotype, rng).genome());
    }

    /**
     * Founder record with a forced <b>genome</b> - epigenome included, so
     * nothing about the horse is rolled but its name.
     *
     * <p>The custom spawn egg is the caller. Its editor previews a live 3D
     * horse, and a preview that the spawn then re-rolls is not a preview; the
     * epigenome the player was looking at travels with the genotype and is
     * written straight in. Every other founder path rolls its own
     * ({@link CoatGenerator#generate}) and should keep doing so.
     */
    public static HorseRecord newFounder(Horse horse, Rng rng, Genome genome) {
        NameParts name = NAMES.generateParts(rng);
        return HorseRecord.founder(horse.getUUID(), name.first(), name.last(), genome);
    }

    // --- the body, resolved from the genotype ---------------------------

    /**
     * <b>What this horse's alleles say its body is</b> - speed, max health, jump
     * strength, body scale and the disorders it expresses.
     *
     * <p>Resolved, never stored. There used to be {@code speed} and
     * {@code health} fields on the record, filled from whatever the entity
     * happened to spawn with and then, for a foal, drawn uniformly out of a wide
     * band around its parents' numbers. That is gone: the numbers are a function
     * of the genotype, so re-deriving them costs a parse and buys the guarantee
     * that a horse's stats can never disagree with the alleles printed beside
     * them.
     *
     * <p>The server's {@code health.mode} setting is applied here, at the one
     * place the game asks - so a world with the disorders switched off still
     * breeds and inherits them identically, it just does not let them bite.
     */
    public static Traits traitsOf(Horse horse) {
        return traitsOf(of(horse));
    }

    public static Traits traitsOf(HorseRecord record) {
        // The epigenome matters here: the magical size locus says "big", and how
        // big is written on the allele copy the horse inherited.
        return HorseTraits.resolve(record.genotype(),
                record.hasGenome() ? record.epigenome() : null,
                ServerConfig.healthGeneticsActive());
    }

    /**
     * Push a genotype's resolved body onto the live entity's attributes:
     * movement speed, max health, jump strength and {@code SCALE}.
     *
     * <p>{@code SCALE} is what makes the size loci visible, and it is doing more
     * than the other three - vanilla scales the model <b>and</b> the hitbox from
     * it, so a pony is genuinely a smaller target and a dwarf genuinely a
     * shorter one, with no renderer work at all.
     *
     * @param fullHeal set current HP to the new max (a newborn foal). When
     *                 {@code false} - a reload, or a re-resolve after a config
     *                 change - current HP is only clamped <i>down</i>, so an
     *                 injured horse is never healed for free and a foal born
     *                 with four hearts does not quietly gain any.
     */
    public static void applyTraitsToEntity(Horse horse, Traits traits, boolean fullHeal) {
        setBase(horse, Attributes.MOVEMENT_SPEED, traits.speed());
        setBase(horse, Attributes.JUMP_STRENGTH, traits.jump());
        setBase(horse, Attributes.SCALE, traits.scale());

        AttributeInstance health = horse.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(traits.health());
            if (fullHeal || horse.getHealth() > traits.health()) {
                horse.setHealth((float) traits.health());
            }
        }
    }

    /** Convenience: resolve and apply in one step. */
    public static void applyTraitsToEntity(Horse horse, HorseRecord record, boolean fullHeal) {
        applyTraitsToEntity(horse, traitsOf(record), fullHeal);
    }

    private static void setBase(Horse horse, net.minecraft.core.Holder<
            net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = horse.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
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

    public static Rng rng(Horse horse) {
        return new NeoRng(horse.getRandom());
    }
}
