package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/**
 * Server &rarr; client: <b>one horse joined or left the field</b>, for everybody
 * standing in the horse realm.
 *
 * <p>{@link RealmRosterPayload} is the whole field, which is thousands of rows
 * and several seconds of packets. Sending that again because a single foal was
 * born would be absurd, and re-sending it on a timer would mean the tab spent
 * most of its life re-filling - so the full roster is sent <b>once, on
 * arrival</b>, and everything after that is one of these. (Owner, 2026-09-26:
 * "maintain the record server-wide... so that they don't have to reload it
 * every time.")
 *
 * <p>Both halves in one payload because both halves happen together: a horse
 * claimed out of the field is a removal, a horse turned out into it is an
 * addition, and a horse led from one to the other in a tick is both. Either list
 * may be empty; a payload with both empty is never sent.
 *
 * <p>A delta naming a horse the client has never heard of is simply added, and
 * one removing a horse it does not hold is ignored. That is not defensive
 * padding - it is what makes the stream self-correcting for a player who
 * arrived mid-change.
 */
public record RealmRosterDeltaPayload(List<HorseRosterPayload.Entry> added,
                                      List<UUID> removed) implements CustomPacketPayload {

    /**
     * Rows per delta. A delta is one or two horses in practice; the cap is here
     * because a stream codec needs one, not because anything sends that many.
     */
    private static final int MAX = 64;

    public static final Type<RealmRosterDeltaPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "realm_roster_delta"));

    public static final StreamCodec<ByteBuf, RealmRosterDeltaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    HorseRosterPayload.ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list(MAX)),
                    RealmRosterDeltaPayload::added,
                    UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)),
                    RealmRosterDeltaPayload::removed,
                    RealmRosterDeltaPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
