package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.GenomeSample;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.Sex;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
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
 * <p>Carrot / gamete-bias effects are not modelled yet; when they are, the
 * effect list joins this record and {@code GenomeSample.breedInto} grows a bias
 * argument.
 */
public record StoredGenome(String genotypeCode, String epigenomeCode,
                           UUID sourceId, String sourceName) {

    public static final Codec<StoredGenome> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("genotype").forGetter(StoredGenome::genotypeCode),
            Codec.STRING.fieldOf("epigenome").forGetter(StoredGenome::epigenomeCode),
            UUIDUtil.CODEC.fieldOf("source_id").forGetter(StoredGenome::sourceId),
            Codec.STRING.optionalFieldOf("source_name", "").forGetter(StoredGenome::sourceName)
    ).apply(i, StoredGenome::new));

    public static final StreamCodec<ByteBuf, StoredGenome> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StoredGenome::genotypeCode,
            ByteBufCodecs.STRING_UTF8, StoredGenome::epigenomeCode,
            UUIDUtil.STREAM_CODEC, StoredGenome::sourceId,
            ByteBufCodecs.STRING_UTF8, StoredGenome::sourceName,
            StoredGenome::new);

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
