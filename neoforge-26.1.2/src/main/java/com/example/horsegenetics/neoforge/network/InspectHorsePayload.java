package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -&gt; server: "I am reading this horse's information screen - hold it
 * still." The AI runs on the server and the screen is a client-side thing, so
 * the client has to say so; {@code server/HorseInspectHold} is the other end.
 *
 * <p><b>Sent repeatedly</b>, once a second while the screen is open, and once
 * with {@code watching == false} when it closes. The hold expires on its own if
 * the heartbeat stops, which is what makes a disconnect, a crash or a
 * {@code /kill} on the client leave a horse walking again rather than frozen
 * for the rest of the world's life.
 */
public record InspectHorsePayload(int entityId, boolean watching) implements CustomPacketPayload {

    public static final Type<InspectHorsePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "inspect_horse"));

    public static final StreamCodec<ByteBuf, InspectHorsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, InspectHorsePayload::entityId,
            ByteBufCodecs.BOOL, InspectHorsePayload::watching,
            InspectHorsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
