package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.GenomeSample;
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
 * bookkeeping an item carrying one needs: the donor's {@link Sex}, its entity
 * {@link UUID} (so a foal bred from the sample still has a real pedigree edge),
 * a display name for the tooltip, and its speed / health (not genetic, but part
 * of "him" - the foal-stat roll needs a value for the sire side).
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
public record StoredGenome(String genotypeCode, String epigenomeCode, Sex sex,
                           UUID sourceId, String sourceName, double speed, double health) {

    private static final Codec<Sex> SEX_CODEC = Codec.STRING.xmap(Sex::valueOf, Sex::name);

    public static final Codec<StoredGenome> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("genotype").forGetter(StoredGenome::genotypeCode),
            Codec.STRING.fieldOf("epigenome").forGetter(StoredGenome::epigenomeCode),
            SEX_CODEC.fieldOf("sex").forGetter(StoredGenome::sex),
            UUIDUtil.CODEC.fieldOf("source_id").forGetter(StoredGenome::sourceId),
            Codec.STRING.optionalFieldOf("source_name", "").forGetter(StoredGenome::sourceName),
            Codec.DOUBLE.optionalFieldOf("speed", 0.0).forGetter(StoredGenome::speed),
            Codec.DOUBLE.optionalFieldOf("health", 0.0).forGetter(StoredGenome::health)
    ).apply(i, StoredGenome::new));

    public static final StreamCodec<ByteBuf, StoredGenome> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StoredGenome::genotypeCode,
            ByteBufCodecs.STRING_UTF8, StoredGenome::epigenomeCode,
            ByteBufCodecs.BOOL, sg -> sg.sex == Sex.FEMALE,
            UUIDUtil.STREAM_CODEC, StoredGenome::sourceId,
            ByteBufCodecs.STRING_UTF8, StoredGenome::sourceName,
            ByteBufCodecs.DOUBLE, StoredGenome::speed,
            ByteBufCodecs.DOUBLE, StoredGenome::health,
            (genotype, epigenome, female, id, name, speed, health) ->
                    new StoredGenome(genotype, epigenome, female ? Sex.FEMALE : Sex.MALE, id, name, speed, health));

    public GenomeSample sample() {
        return new GenomeSample(genotypeCode, epigenomeCode);
    }
}
