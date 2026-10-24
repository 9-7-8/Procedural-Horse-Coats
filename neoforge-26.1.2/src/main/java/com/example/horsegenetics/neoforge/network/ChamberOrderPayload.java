package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Client -&gt; server: the order this player wants {@code /horsebank} to use their
 * empty chambers in ({@code horsebank.chamber_order}, a client setting - issue #272).
 * Sent on join, like {@link NamingPolicyPayload}, and filed under the sender's UUID
 * by {@code server/HorseBankCommand}; the server cleans it, so a forged list can only
 * name chamber kinds that exist.
 */
public record ChamberOrderPayload(List<String> order) implements CustomPacketPayload {

    public static final Type<ChamberOrderPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "chamber_order"));

    public static final StreamCodec<ByteBuf, ChamberOrderPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16).apply(ByteBufCodecs.list(8)), ChamberOrderPayload::order,
            ChamberOrderPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
