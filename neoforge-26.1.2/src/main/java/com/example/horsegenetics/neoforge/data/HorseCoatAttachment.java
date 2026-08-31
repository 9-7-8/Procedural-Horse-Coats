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
 * phenotype, whether the coat is deterministic, the pixels - is derived.
 *
 * <p>Dev only: no legacy-format handling. {@link #UNASSIGNED} is the attachment
 * default; the spawn handler replaces it immediately.
 */
public record HorseCoatAttachment(String genotypeCode, long epigeneticSeed) {

    /** Placeholder value until the spawn handler assigns a real coat. */
    public static final HorseCoatAttachment UNASSIGNED = new HorseCoatAttachment("<unassigned>", 0L);

    public static final MapCodec<HorseCoatAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("genotype").forGetter(HorseCoatAttachment::genotypeCode),
            Codec.LONG.fieldOf("epigenetic_seed").forGetter(HorseCoatAttachment::epigeneticSeed)
    ).apply(instance, HorseCoatAttachment::new));

    public static final Codec<HorseCoatAttachment> CODEC = MAP_CODEC.codec();

    public static HorseCoatAttachment from(CoatData coatData) {
        return new HorseCoatAttachment(coatData.genotype().toCode(), coatData.epigeneticSeed());
    }

    public boolean isUnassigned() {
        return UNASSIGNED.genotypeCode().equals(genotypeCode);
    }

    public Genotype genotype() {
        return Genotype.parse(genotypeCode);
    }

    public CoatData coatData() {
        return new CoatData(genotype(), epigeneticSeed);
    }
}
