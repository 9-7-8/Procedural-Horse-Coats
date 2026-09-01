package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Data attachments are NOT auto-synced. The client needs the genotype + the
 * epigenome to (re)generate the coat texture, so we push both explicitly
 * whenever a horse's coat is assigned or a player starts tracking it. Stored
 * client-side in {@code ClientCoatCache}, keyed by entity id.
 */
public record CoatSyncPayload(int entityId, String genotypeCode, String epigenomeCode)
        implements CustomPacketPayload {

    public static final Type<CoatSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "coat_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoatSyncPayload> STREAM_CODEC = StreamCodec.composite(
            StreamCodec.of((buf, v) -> buf.writeVarInt(v), buf -> buf.readVarInt()), CoatSyncPayload::entityId,
            StreamCodec.of((buf, v) -> buf.writeUtf(v), buf -> buf.readUtf()), CoatSyncPayload::genotypeCode,
            StreamCodec.of((buf, v) -> buf.writeUtf(v), buf -> buf.readUtf()), CoatSyncPayload::epigenomeCode,
            CoatSyncPayload::new
    );

    public static CoatSyncPayload of(int entityId, CoatData coatData) {
        return new CoatSyncPayload(entityId, coatData.genome().genotypeCode(), coatData.genome().epigenomeCode());
    }

    public CoatData coatData() {
        return new CoatData(Genome.parse(genotypeCode, epigenomeCode));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
