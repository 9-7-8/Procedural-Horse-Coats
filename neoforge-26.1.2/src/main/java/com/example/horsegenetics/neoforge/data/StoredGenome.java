package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.GenomeSample;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.Sex;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The payload of the {@code horsegenetics:stored_genome} data component - a
 * {@link GenomeSample} (genotype + epigenome code) plus the small amount of
 * bookkeeping an item carrying one needs: the donor's entity {@link UUID} (so a
 * foal bred from the sample still has a real pedigree edge) and a display name
 * for the tooltip.
 *
 * <p>It no longer stores the donor's speed and health. It used to have to,
 * because a foal's stats were rolled from two numbers and the sire side needed
 * one. Now they are resolved from the genotype - which this record already
 * carries - so storing them would be storing the same fact twice, in a form
 * that could go stale the moment a gene was re-tuned.
 *
 * <p>The donor's {@link Sex} is <b>not</b> a field: sex is a gene, so it is
 * already in {@code genotypeCode} and {@link #sex()} reads it back.
 *
 * <p>This is what the <b>stallion seed jar</b> holds. It deliberately stores
 * the <i>genotype</i>, never a drawn gamete: the Mendelian draw happens at
 * impregnation ({@link GenomeSample#breedInto}), so the seed-jar route is
 * deterministic the same way an in-world pairing is.
 *
 * <p>A jar filled from a carrot-armed stallion also carries his
 * {@link #carrotEffects}, folded into his side of the draw when it is used.
 */
public record StoredGenome(String genotypeCode, String epigenomeCode,
                           UUID sourceId, String sourceName, String breed, List<String> carrotEffects) {

    public StoredGenome {
        carrotEffects = carrotEffects == null ? List.of() : List.copyOf(carrotEffects);
    }

    /** No carrot effects - every stored genome but a jar filled from an armed stallion. */
    public StoredGenome(String genotypeCode, String epigenomeCode, UUID sourceId, String sourceName, String breed) {
        this(genotypeCode, epigenomeCode, sourceId, sourceName, breed, List.of());
    }

    public static final Codec<StoredGenome> CODEC = RecordCodecBuilder.create(i -> i.group(
            GenomeCodeCodecs.STORED_GENOTYPE.fieldOf("genotype").forGetter(StoredGenome::genotypeCode),
            Codec.STRING.fieldOf("epigenome").forGetter(StoredGenome::epigenomeCode),
            UUIDUtil.CODEC.fieldOf("source_id").forGetter(StoredGenome::sourceId),
            Codec.STRING.optionalFieldOf("source_name", "").forGetter(StoredGenome::sourceName),
            Codec.STRING.optionalFieldOf("breed", "").forGetter(StoredGenome::breed),
            Codec.STRING.listOf().optionalFieldOf("carrot_effects", List.of()).forGetter(StoredGenome::carrotEffects)
    ).apply(i, StoredGenome::new));

    // genotype/epigenome on GenomeCodeCodecs, not STRING_UTF8: this component
    // rides every inventory sync, so a code over 32 767 characters would kick
    // the holder rather than fail quietly. See GenomeCodeCodecs.
    public static final StreamCodec<ByteBuf, StoredGenome> STREAM_CODEC = StreamCodec.composite(
            GenomeCodeCodecs.GENOTYPE_CODE, StoredGenome::genotypeCode,
            GenomeCodeCodecs.EPIGENOME_CODE, StoredGenome::epigenomeCode,
            UUIDUtil.STREAM_CODEC, StoredGenome::sourceId,
            ByteBufCodecs.STRING_UTF8, StoredGenome::sourceName,
            ByteBufCodecs.STRING_UTF8, StoredGenome::breed,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), StoredGenome::carrotEffects,
            StoredGenome::new);

    /**
     * The breeding-carrot effects the stallion was armed with when the jar was
     * filled. They go into the jar with his genome and act on his side of the
     * draw when it is used (owner, 2026-09-13).
     */
    public List<com.example.horsegenetics.common.genetics.CarrotEffect> carrots() {
        return com.example.horsegenetics.common.genetics.CarrotEffect.parseList(carrotEffects);
    }

    /** The donor's sex, read off the stored genotype - a filled jar is always a stallion's. */
    public Sex sex() {
        return Genotype.sexOf(genotypeCode);
    }

    public GenomeSample sample() {
        return new GenomeSample(genotypeCode, epigenomeCode);
    }

    /** The donor's body, resolved from the stored genotype. */
    public com.example.horsegenetics.common.trait.Traits traits() {
        return com.example.horsegenetics.common.trait.HorseTraits.resolve(
                com.example.horsegenetics.common.genetics.Genotype.parse(genotypeCode));
    }
}
