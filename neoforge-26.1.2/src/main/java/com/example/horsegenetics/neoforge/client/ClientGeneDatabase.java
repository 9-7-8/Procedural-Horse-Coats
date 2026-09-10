package com.example.horsegenetics.neoforge.client;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client mirror of the player's {@code GeneDatabaseData} - only what the Horse
 * Browser's gene-database tab needs. Fed by {@code GeneDatabaseSyncPayload},
 * cleared on logout. Never trusted for crafting: the gene-carrot recipe gate
 * is re-checked on the server.
 */
public final class ClientGeneDatabase {

    private static volatile Map<String, List<String>> seenByGene = Map.of();
    private static volatile Set<String> carrotUnlocked = Set.of();

    /**
     * Every allele this player has laid eyes on, as {@code geneKey|token} -
     * what the Alleles tab greys out from. Separate from {@link #seenByGene} on
     * purpose: discovering a gene is a gameplay gate, collecting an allele is a
     * record, and a baseline allele belongs in the second and not the first.
     */
    private static volatile Set<String> collected = Set.of();
    private static volatile Set<String> breeds = Set.of();

    public static void accept(Map<String, List<String>> seen, List<String> unlocked,
                              List<String> collectedAlleles, List<String> knownBreeds) {
        seenByGene = Map.copyOf(seen);
        carrotUnlocked = Set.copyOf(unlocked);
        collected = Set.copyOf(collectedAlleles);
        breeds = Set.copyOf(knownBreeds);
    }

    /** Has this player ever seen this allele on a horse? */
    public static boolean hasAllele(String geneKey, String token) {
        return collected.contains(geneKey + "|" + token);
    }

    /** Has this player owned a horse of {@code breedId}? */
    public static boolean hasBreed(String breedId) {
        return breeds.contains(breedId);
    }

    public static int breedCount() {
        return breeds.size();
    }

    public static int collectedCount() {
        return collected.size();
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
        collected = Set.of();
        breeds = Set.of();
    }

    private ClientGeneDatabase() {
    }
}
