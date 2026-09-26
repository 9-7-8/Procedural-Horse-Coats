package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Client &rarr; server: <b>claim this realm horse</b> - the <i>Bring home</i>
 * button on the browser's <i>Horse realm</i> tab. The server tames it to the
 * player, sends it to their holding pen and spends one holding pen ticket; see
 * {@code server/RealmClaim}.
 *
 * <p>By record UUID rather than entity id, like {@link HorseRecallPayload} and
 * unlike {@link MountHorsePayload}: the horse is in another dimension from the
 * player as often as not, so the client has no entity to point at - it is
 * clicking a row in a table.
 */
public record ClaimRealmHorsePayload(UUID horseId) implements CustomPacketPayload {

    public static final Type<ClaimRealmHorsePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "claim_realm_horse"));

    public static final StreamCodec<ByteBuf, ClaimRealmHorsePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ClaimRealmHorsePayload::horseId,
            ClaimRealmHorsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
