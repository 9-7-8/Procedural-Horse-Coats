package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Data attachments are NOT auto-synced to the client. Since the client needs
 * this to render the horse at all, we push it explicitly whenever a horse's
 * coat is assigned or a player starts tracking the horse. The client stores
 * incoming payloads in ClientCoatCache, keyed by entity id.
 */
public record CoatSyncPayload(int entityId, CoatPhenotype phenotype, float legBlackHeight)
        implements CustomPacketPayload {

    public static final Type<CoatSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "coat_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoatSyncPayload> STREAM_CODEC = StreamCodec.composite(
            StreamCodec.of((buf, v) -> buf.writeVarInt(v), buf -> buf.readVarInt()), CoatSyncPayload::entityId,
            StreamCodec.of((buf, v) -> buf.writeEnum(v), buf -> buf.readEnum(CoatPhenotype.class)), CoatSyncPayload::phenotype,
            StreamCodec.of((buf, v) -> buf.writeFloat(v), buf -> buf.readFloat()), CoatSyncPayload::legBlackHeight,
            CoatSyncPayload::new
    );

    public static CoatSyncPayload of(int entityId, CoatData coatData) {
        return new CoatSyncPayload(entityId, coatData.phenotype(), coatData.legBlackHeight());
    }

    public CoatData coatData() {
        return CoatData.fromRaw(phenotype, legBlackHeight);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
