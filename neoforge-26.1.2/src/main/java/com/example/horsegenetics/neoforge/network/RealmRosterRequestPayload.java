package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client &rarr; server: "what is standing in the horse realm?". Carries nothing;
 * the answer is the same for everyone who asks. The server replies with a
 * {@link RealmRosterPayload}.
 *
 * <p>Sent when the browser's <i>Horse realm</i> tab is opened and on its Refresh
 * button, exactly as {@link HorseRosterRequestPayload} is - and for a stronger
 * version of the same reason. That roster is a stable, which changes when the
 * player changes it; this is a field of loose horses that are walking about, so
 * it is a snapshot by nature and the tab says so rather than pretending to be
 * live.
 */
public record RealmRosterRequestPayload() implements CustomPacketPayload {

    public static final RealmRosterRequestPayload INSTANCE = new RealmRosterRequestPayload();

    public static final Type<RealmRosterRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "realm_roster_request"));

    public static final StreamCodec<ByteBuf, RealmRosterRequestPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
