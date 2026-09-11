package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.horse.TransferDeed;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Serialization for the Layer-1 {@link TransferDeed} - what a signed transfer
 * paper carries in its {@code horsegenetics:horse_deed} component. In the
 * integration layer for the same reason {@link HorseRecordCodecs} is: the
 * domain type stays free of DataFixerUpper.
 *
 * <p>This one really does have to cross the wire, unlike most components -
 * the paper's tooltip names the horse, its breed and its breeder, and the
 * paper itself is drawn as a model of that horse in that horse's coat, all of
 * it on the client.
 */
public final class TransferDeedCodecs {

    public static final Codec<TransferDeed> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("horse_id").forGetter(TransferDeed::horseId),
            Codec.STRING.fieldOf("horse_name").forGetter(TransferDeed::horseName),
            Codec.STRING.optionalFieldOf("breed").forGetter(TransferDeed::breed),
            Codec.STRING.optionalFieldOf("bred_by").forGetter(TransferDeed::bredBy),
            Codec.STRING.optionalFieldOf("issued_by", "").forGetter(TransferDeed::issuedBy),
            GenomeCodeCodecs.STORED_GENOTYPE.fieldOf("genetic_code").forGetter(TransferDeed::geneticCode),
            Codec.STRING.fieldOf("epigenome_code").forGetter(TransferDeed::epigenomeCode)
    ).apply(i, TransferDeed::new));

    public static final StreamCodec<ByteBuf, TransferDeed> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private TransferDeedCodecs() {
    }
}
