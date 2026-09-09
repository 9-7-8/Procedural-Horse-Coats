package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.common.coat.CoatData;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.GenomeCodeCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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

    // The two code fields are NOT on a default-length string codec, and must
    // never go back to one: writeUtf(String) caps at 32 767 characters and an
    // epigenome code passed that in 0.3.1, which kicked every client on world
    // entry. See GenomeCodeCodecs.
    public static final StreamCodec<RegistryFriendlyByteBuf, CoatSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CoatSyncPayload::entityId,
            GenomeCodeCodecs.GENOTYPE_CODE, CoatSyncPayload::genotypeCode,
            GenomeCodeCodecs.EPIGENOME_CODE, CoatSyncPayload::epigenomeCode,
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
