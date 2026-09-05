package com.example.horsegenetics.neoforge.client;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client mirror of the player's {@code GeneDatabaseData} - only what the Horse
 * Browser's gene-database tab needs. Fed by {@code GeneDatabaseSyncPayload},
 * cleared on logout. Never trusted for crafting: the magic-carrot recipe gate
 * is re-checked on the server.
 */
public final class ClientGeneDatabase {

    private static volatile Map<String, List<String>> seenByGene = Map.of();
    private static volatile Set<String> carrotUnlocked = Set.of();

    public static void accept(Map<String, List<String>> seen, List<String> unlocked) {
        seenByGene = Map.copyOf(seen);
        carrotUnlocked = Set.copyOf(unlocked);
    }

    public static boolean knows(String geneKey) {
        return seenByGene.containsKey(geneKey);
    }

    public static boolean carrotUnlocked(String geneKey) {
        return carrotUnlocked.contains(geneKey);
    }

    public static List<String> seenTokens(String geneKey) {
        return seenByGene.getOrDefault(geneKey, List.of());
    }

    public static int knownCount() {
        return seenByGene.size();
    }

    public static void clear() {
        seenByGene = Map.of();
        carrotUnlocked = Set.of();
    }

    private ClientGeneDatabase() {
    }
}
