package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Server &rarr; client: <b>every horse standing in the horse realm</b>, for the
 * browser's <i>Horse realm</i> tab. Built by {@code server/RealmRoster}.
 *
 * <p>It carries {@link HorseRosterPayload.Entry} rather than a shape of its own,
 * and reuses that payload's hand-written entry codec, because it is the same
 * table with a different question behind it: one renderer, one row type, and a
 * column added to the stable is a column here without a second codec to keep in
 * step. The two are separate payloads only so that arriving in the realm cannot
 * overwrite the client's idea of what is in the player's stable.
 */
public record RealmRosterPayload(List<HorseRosterPayload.Entry> entries) implements CustomPacketPayload {

    public static final Type<RealmRosterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "realm_roster"));

    public static final StreamCodec<ByteBuf, RealmRosterPayload> STREAM_CODEC = StreamCodec.composite(
            HorseRosterPayload.ENTRY_STREAM_CODEC.apply(
                    ByteBufCodecs.list(HorseRosterPayload.MAX_ENTRIES)),
            RealmRosterPayload::entries,
            RealmRosterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
