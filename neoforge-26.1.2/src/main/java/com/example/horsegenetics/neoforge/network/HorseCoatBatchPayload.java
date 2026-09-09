package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.GenomeCodeCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/**
 * Server -&gt; client: the epigenomes behind a {@link HorseCoatRequestPayload}.
 *
 * <p><b>Only the epigenome.</b> The client already holds every genotype it could
 * possibly be asking about - they came with the roster - so sending the genotype
 * again would be sending the smaller half of the answer twice. The browser pairs
 * the two and builds a {@code CoatData}.
 *
 * <p>An id the server could not answer for is simply absent from the reply
 * rather than carrying an empty string: the client's "already asked" bookkeeping
 * is keyed on the request, not the reply, so a missing horse stays missing
 * instead of being asked for again every frame.
 */
public record HorseCoatBatchPayload(List<Entry> entries) implements CustomPacketPayload {

    /** Matches {@link HorseCoatRequestPayload#MAX_IDS} - one reply per request. */
    public static final int MAX_ENTRIES = HorseCoatRequestPayload.MAX_IDS;

    public record Entry(UUID id, String epigenomeCode) {
    }

    public static final Type<HorseCoatBatchPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_coat_batch"));

    private static final StreamCodec<ByteBuf, Entry> ENTRY_STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Entry::id,
            GenomeCodeCodecs.EPIGENOME_CODE, Entry::epigenomeCode,
            Entry::new
    );

    public static final StreamCodec<ByteBuf, HorseCoatBatchPayload> STREAM_CODEC = StreamCodec.composite(
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ENTRIES)), HorseCoatBatchPayload::entries,
            HorseCoatBatchPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
