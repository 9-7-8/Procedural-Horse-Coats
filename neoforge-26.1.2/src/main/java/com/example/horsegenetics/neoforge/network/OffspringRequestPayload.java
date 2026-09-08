package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Client -&gt; server: "who came from this horse?". The other half of
 * {@link FamilyTreeRequestPayload}, which walks the same pedigree upward.
 *
 * <p><b>Sent only on a Refresh press</b>, never on opening the tab and never on
 * a timer. That is the owner's call and it is the right one: the answer is a
 * pass over the whole ancestry table per generation and each descendant comes
 * back as a full record, so it is a question worth asking deliberately. The tab
 * shows what it was last told, and says when that was asked.
 */
public record OffspringRequestPayload(UUID rootId) implements CustomPacketPayload {

    public static final Type<OffspringRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "offspring_request"));

    public static final StreamCodec<ByteBuf, OffspringRequestPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, OffspringRequestPayload::rootId,
            OffspringRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
