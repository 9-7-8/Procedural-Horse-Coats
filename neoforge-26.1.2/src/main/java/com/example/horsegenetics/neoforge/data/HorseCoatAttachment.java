package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Genotype;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The persisted, server-side record for one horse: its genotype code plus
 * the resolved CoatData (so we never re-roll the bay leg height on reload).
 *
 * This is intentionally a thin wrapper around common's types - all it adds
 * is the Codec that NeoForge's data attachment system needs for NBT
 * (de)serialization. If you backport to 1.12.2 later, this class does not
 * port; you'll write an NBTTagCompound reader/writer with the same two
 * fields instead. Everything it wraps (Genotype, CoatData) ports unchanged.
 */
public record HorseCoatAttachment(String genotypeCode, CoatPhenotype phenotype, float legBlackHeight) {

    /**
     * NeoForge 26.1.2's {@code AttachmentType.Builder#serialize} takes a
     * {@link MapCodec}, so that's the primary form; {@link #CODEC} is derived
     * from it for any caller that needs a plain {@link Codec}.
     */
    public static final MapCodec<HorseCoatAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("genotype").forGetter(HorseCoatAttachment::genotypeCode),
            Codec.STRING.xmap(CoatPhenotype::valueOf, Enum::name)
                    .fieldOf("phenotype").forGetter(HorseCoatAttachment::phenotype),
            Codec.FLOAT.fieldOf("leg_black_height").forGetter(HorseCoatAttachment::legBlackHeight)
    ).apply(instance, HorseCoatAttachment::new));

    public static final Codec<HorseCoatAttachment> CODEC = MAP_CODEC.codec();

    public static HorseCoatAttachment from(Genotype genotype, CoatData coatData) {
        return new HorseCoatAttachment(genotype.toCode(), coatData.phenotype(), coatData.legBlackHeight());
    }

    public Genotype genotype() {
        return Genotype.parse(genotypeCode);
    }

    public CoatData coatData() {
        return CoatData.fromRaw(phenotype, legBlackHeight);
    }
}
