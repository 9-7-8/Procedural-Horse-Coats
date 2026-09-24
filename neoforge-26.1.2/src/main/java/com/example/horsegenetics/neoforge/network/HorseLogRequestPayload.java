package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -&gt; server: "what has been happening to my horses?". Carries nothing -
 * the answer is entirely about who asked, exactly as
 * {@link HorseRosterRequestPayload}. The server replies with a
 * {@link HorseLogPayload}.
 *
 * <p>Sent when the browser's Log tab is opened and on its Refresh button. The
 * server also pushes the log unasked when it writes a row for an online player,
 * so this is the cold-start question rather than the only way the client hears
 * anything.
 */
public record HorseLogRequestPayload() implements CustomPacketPayload {

    public static final HorseLogRequestPayload INSTANCE = new HorseLogRequestPayload();

    public static final Type<HorseLogRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_log_request"));

    public static final StreamCodec<ByteBuf, HorseLogRequestPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
