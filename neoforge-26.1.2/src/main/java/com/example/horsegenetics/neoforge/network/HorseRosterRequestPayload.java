package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -&gt; server: "which horses are mine?". Carries nothing - the answer is
 * entirely about who asked. The server replies with a
 * {@link HorseRosterPayload}.
 *
 * <p>Sent when the horse browser's My horses or Breeding preview tab is opened,
 * and again on their Refresh button, rather than pushed on a timer: the roster
 * is only looked at deliberately, and a horse tamed thirty seconds ago is worth
 * one click.
 */
public record HorseRosterRequestPayload() implements CustomPacketPayload {

    public static final HorseRosterRequestPayload INSTANCE = new HorseRosterRequestPayload();

    public static final Type<HorseRosterRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "horse_roster_request"));

    public static final StreamCodec<ByteBuf, HorseRosterRequestPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
