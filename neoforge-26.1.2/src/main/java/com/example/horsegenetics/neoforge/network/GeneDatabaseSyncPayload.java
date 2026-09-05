package com.example.horsegenetics.neoforge.network;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.data.GeneDatabaseData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The reader's whole {@link GeneDatabaseData} book, pushed on discovery / paper
 * read / login. {@code seenByGene} keys are the genes the player has met;
 * {@code carrotUnlocked} is the subset whose magic-carrot recipe is unlocked.
 * Client-side it only drives the browser tab - the recipe gate is always
 * re-checked on the server.
 */
public record GeneDatabaseSyncPayload(Map<String, List<String>> seenByGene,
                                      List<String> carrotUnlocked) implements CustomPacketPayload {

    public static final Type<GeneDatabaseSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "gene_database_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GeneDatabaseSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8,
                            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())),
                    GeneDatabaseSyncPayload::seenByGene,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    GeneDatabaseSyncPayload::carrotUnlocked,
                    GeneDatabaseSyncPayload::new);

    public static GeneDatabaseSyncPayload of(Map<String, GeneDatabaseData.Entry> book) {
        Map<String, List<String>> seen = new HashMap<>();
        List<String> unlocked = new ArrayList<>();
        book.forEach((key, entry) -> {
            seen.put(key, entry.seenTokens());
            if (entry.carrotUnlocked()) {
                unlocked.add(key);
            }
        });
        return new GeneDatabaseSyncPayload(seen, unlocked);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
