package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The persisted, server-side coat record for one horse: its genotype code and
 * its <b>epigenome code</b> - the priority + epigenetic seed carried by each of
 * its allele copies, which drives every non-deterministic coat gene's per-horse
 * randomness. Both are set once (rolled for a founder, inherited for a foal)
 * and never re-rolled. Everything else - phenotype, whether the coat is
 * deterministic, the pixels - is derived.
 *
 * <p>Dev only: no legacy-format handling. {@link #UNASSIGNED} is the attachment
 * default; the spawn handler replaces it.
 */
public record HorseCoatAttachment(String genotypeCode, String epigenomeCode) {

    /** Placeholder value until the spawn / breeding handler assigns a real coat. */
    public static final HorseCoatAttachment UNASSIGNED = new HorseCoatAttachment("<unassigned>", "");

    public static final MapCodec<HorseCoatAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("genotype").forGetter(HorseCoatAttachment::genotypeCode),
            Codec.STRING.fieldOf("epigenome").forGetter(HorseCoatAttachment::epigenomeCode)
    ).apply(instance, HorseCoatAttachment::new));

    public static final Codec<HorseCoatAttachment> CODEC = MAP_CODEC.codec();

    public static HorseCoatAttachment from(CoatData coatData) {
        return from(coatData.genome());
    }

    public static HorseCoatAttachment from(Genome genome) {
        return new HorseCoatAttachment(genome.genotypeCode(), genome.epigenomeCode());
    }

    public boolean isUnassigned() {
        return UNASSIGNED.genotypeCode().equals(genotypeCode) || epigenomeCode.isEmpty();
    }

    public Genotype genotype() {
        return Genotype.parse(genotypeCode);
    }

    public Epigenome epigenome() {
        return Epigenome.parse(epigenomeCode);
    }

    public Genome genome() {
        return Genome.parse(genotypeCode, epigenomeCode);
    }

    public CoatData coatData() {
        return new CoatData(genome());
    }
}
