package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server &rarr; client: every gene an open {@code ResearchShelfMenu}'s shelf is
 * holding, so the Store tab has something to draw. Sent when the screen opens
 * and again after every change.
 *
 * <p>Gene keys, not items: a filed paper is completely described by the gene it
 * names, and the shelf holds one of each. Capped at the registry's own size in
 * practice; the declared cap is generous and explicit rather than a default -
 * see {@code GenomeCodeCodecs} for why a default-length cap is a bug waiting.
 */
public record ShelfSyncPayload(List<String> geneKeys) implements CustomPacketPayload {

    /** Far more than the gene registry will ever hold, and a real number. */
    private static final int MAX_GENES = 8192;

    public static final Type<ShelfSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "shelf_sync"));

    public static final StreamCodec<ByteBuf, ShelfSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(MAX_GENES)),
            ShelfSyncPayload::geneKeys,
            ShelfSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
