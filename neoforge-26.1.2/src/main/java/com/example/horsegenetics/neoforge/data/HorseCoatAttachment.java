package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Genotype;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The persisted, server-side coat record for one horse: its genotype code and
 * its <b>epigenetic seed</b> (rolled once at birth; drives every
 * non-deterministic coat gene's per-horse randomness). Everything else -
 * phenotype, whether the coat is deterministic, the actual pixels - is derived.
 *
 * <p>Thin wrapper over common's {@link CoatData} - all it adds is the NBT
 * codec. Old saves that still carry {@code phenotype} / {@code leg_black_height}
 * fields load fine (unknown fields are ignored); a missing {@code epigenetic_seed}
 * reads as {@code 0}, which the join handler treats as "roll one now".
 */
public record HorseCoatAttachment(String genotypeCode, long epigeneticSeed) {

    public static final MapCodec<HorseCoatAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("genotype").forGetter(HorseCoatAttachment::genotypeCode),
            Codec.LONG.optionalFieldOf("epigenetic_seed", 0L).forGetter(HorseCoatAttachment::epigeneticSeed)
    ).apply(instance, HorseCoatAttachment::new));

    public static final Codec<HorseCoatAttachment> CODEC = MAP_CODEC.codec();

    public static HorseCoatAttachment from(CoatData coatData) {
        return new HorseCoatAttachment(coatData.genotype().toCode(), coatData.epigeneticSeed());
    }

    public static HorseCoatAttachment from(Genotype genotype, long epigeneticSeed) {
        return new HorseCoatAttachment(genotype.toCode(), epigeneticSeed);
    }

    public Genotype genotype() {
        return Genotype.parse(genotypeCode);
    }

    public CoatData coatData() {
        return new CoatData(genotype(), epigeneticSeed);
    }

    /** True when a seed still needs rolling (legacy save, or the unassigned default). */
    public boolean needsEpigeneticSeed() {
        return epigeneticSeed == 0L;
    }
}
