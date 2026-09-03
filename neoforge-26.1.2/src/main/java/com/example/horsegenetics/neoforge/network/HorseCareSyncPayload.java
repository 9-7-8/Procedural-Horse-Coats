package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Care/social state a client needs for the horse-inventory panel: the horse's
 * bond number and whether it belongs to a herd. Not auto-synced (it lives in a
 * data attachment), so {@code HorseCareHandler} pushes it on change and
 * {@code HorseGeneticsEventHandler} pushes it on start-tracking. Stored in
 * {@code ClientHorseCareCache}.
 */
public record HorseCareSyncPayload(int entityId, int bond, boolean inHerd) implements CustomPacketPayload {

    public static final Type<HorseCareSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "care_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HorseCareSyncPayload> STREAM_CODEC = StreamCodec.composite(
            StreamCodec.of((buf, v) -> buf.writeVarInt(v), buf -> buf.readVarInt()), HorseCareSyncPayload::entityId,
            StreamCodec.of((buf, v) -> buf.writeVarInt(v), buf -> buf.readVarInt()), HorseCareSyncPayload::bond,
            StreamCodec.of((buf, v) -> buf.writeBoolean(v), buf -> buf.readBoolean()), HorseCareSyncPayload::inHerd,
            HorseCareSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
