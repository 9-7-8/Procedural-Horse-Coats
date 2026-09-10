package com.example.horsegenetics.neoforge.data;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.network.GeneDatabaseSyncPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The per-player <b>gene database</b> (roadmap wiki &sect;16.1) - which genes a
 * player has met, which allele variants they have seen, and whether each gene's
 * gene-carrot recipe is unlocked. Server-global {@link SavedData} keyed by
 * player {@link UUID}, so it lives in {@code <save>/data/} and gates crafting;
 * a client mirror ({@code client/ClientGeneDatabase}) drives the browser tab
 * only.
 *
 * <p>It hides nothing (settled): the info panel, pen signs and genotype code
 * keep showing every gene a horse carries. The only thing discovery gates is
 * the gene-carrot recipe.
 */
public final class GeneDatabaseData extends SavedData {

    /** One gene's entry for one player. */
    public record Entry(List<String> seenTokens, boolean carrotUnlocked, long discoveredAt) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().optionalFieldOf("seen", List.of()).forGetter(Entry::seenTokens),
                Codec.BOOL.optionalFieldOf("carrot", false).forGetter(Entry::carrotUnlocked),
                Codec.LONG.optionalFieldOf("at", 0L).forGetter(Entry::discoveredAt)
        ).apply(i, Entry::new));

        public Entry {
            seenTokens = List.copyOf(seenTokens);
        }
    }

    private record PlayerBook(UUID player, Map<String, Entry> byGene, List<String> collected,
                              List<String> breeds) {
        static final Codec<PlayerBook> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(PlayerBook::player),
                Codec.unboundedMap(Codec.STRING, Entry.CODEC).fieldOf("genes").forGetter(PlayerBook::byGene),
                Codec.STRING.listOf().optionalFieldOf("collected", List.of()).forGetter(PlayerBook::collected),
            Codec.STRING.listOf().optionalFieldOf("breeds", List.of()).forGetter(PlayerBook::breeds)
        ).apply(i, PlayerBook::new));
    }

    public static final Codec<GeneDatabaseData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.list(PlayerBook.CODEC).fieldOf("players").forGetter(GeneDatabaseData::snapshot)
    ).apply(i, GeneDatabaseData::new));

    public static final SavedDataType<GeneDatabaseData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "gene_database"),
            GeneDatabaseData::new,
            CODEC);

    private final Map<UUID, Map<String, Entry>> byPlayer = new LinkedHashMap<>();

    /**
     * <b>Every allele this player has actually laid eyes on</b>, as
     * {@code geneKey|token}. This is the collection, and it is deliberately
     * <i>not</i> the same thing as the gene database above.
     *
     * <p>Discovering a gene is a gameplay gate - it unlocks a recipe, and an
     * ordinary horse must not hand it to you. Collecting an allele is a record
     * of what you have seen, and a plain wild-type allele is exactly as much a
     * part of the set as a mythic one. Sharing one map would have meant one of
     * those two behaviours quietly becoming the other; a horse full of baseline
     * alleles would have unlocked every gene in the mod.
     */
    private final Map<UUID, Set<String>> collectedByPlayer = new LinkedHashMap<>();
    /**
     * Breeds each player has met, by id.
     *
     * <p>Same principle as the allele collection and for the same stated
     * reason: every reference tab in this browser fills in as you play, so a
     * new player is not handed forty-nine breeds they have never seen. Only
     * Getting Started and Recipes are complete from the first minute, because
     * those two are how you find out what to do at all. (Owner's call.)
     */
    private final Map<UUID, Set<String>> breedsByPlayer = new LinkedHashMap<>();

    private GeneDatabaseData() {
    }

    private GeneDatabaseData(List<PlayerBook> books) {
        for (PlayerBook b : books) {
            byPlayer.put(b.player(), new LinkedHashMap<>(b.byGene()));
            collectedByPlayer.put(b.player(), new LinkedHashSet<>(b.collected()));
            breedsByPlayer.put(b.player(), new LinkedHashSet<>(b.breeds()));
        }
    }

    public static GeneDatabaseData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<PlayerBook> snapshot() {
        List<PlayerBook> out = new ArrayList<>();
        Set<UUID> everyone = new LinkedHashSet<>(byPlayer.keySet());
        everyone.addAll(collectedByPlayer.keySet());
        everyone.addAll(breedsByPlayer.keySet());
        for (UUID id : everyone) {
            out.add(new PlayerBook(id,
                    Map.copyOf(byPlayer.getOrDefault(id, Map.of())),
                    List.copyOf(collectedByPlayer.getOrDefault(id, Set.of())),
                    List.copyOf(breedsByPlayer.getOrDefault(id, Set.of()))));
        }
        return out;
    }

    /** The id one allele is collected under. */
    public static String allele(String geneKey, String token) {
        return geneKey + "|" + token;
    }

    /**
     * <b>Record every allele on this horse.</b> Both copies at every locus,
     * baseline included - the point of a collection is that the common ones
     * count too, and a player who has never met a plain wild-type allele has
     * genuinely not met it.
     */
    public void collect(ServerPlayer player, com.example.horsegenetics.common.genetics.Genotype genotype) {
        collect(player, java.util.List.of(genotype));
    }

    /**
     * <b>The same, for a whole stable at once.</b>
     *
     * <p>One sync at the end rather than one per horse, which is the only
     * reason this overload exists: the sweep that fixed the empty Alleles tab
     * walks every horse a player owns, and a player with a breeding programme
     * owns a lot of them.
     */
    public void collect(ServerPlayer player,
                        Iterable<com.example.horsegenetics.common.genetics.Genotype> genotypes) {
        Set<String> mine = collectedByPlayer.computeIfAbsent(player.getUUID(), k -> new LinkedHashSet<>());
        boolean changed = false;
        for (com.example.horsegenetics.common.genetics.Genotype genotype : genotypes) {
            for (Gene gene : com.example.horsegenetics.common.genetics.Genes.codeOrder()) {
                var pair = genotype.pair(gene);
                changed |= mine.add(allele(gene.key(), pair.first().token()));
                changed |= mine.add(allele(gene.key(), pair.second().token()));
            }
        }
        if (changed) {
            setDirty();
            sync(player);
        }
    }

    /**
     * <b>Record the breeds of a batch of horses.</b> Ids only - a breed the mod
     * no longer ships stays in the set harmlessly and simply never matches a row.
     */
    public void discoverBreeds(ServerPlayer player, Iterable<String> breedIds) {
        Set<String> mine = breedsByPlayer.computeIfAbsent(player.getUUID(), k -> new LinkedHashSet<>());
        boolean changed = false;
        for (String id : breedIds) {
            if (id != null && !id.isBlank()) {
                changed |= mine.add(id);
            }
        }
        if (changed) {
            setDirty();
            sync(player);
        }
    }

    public Set<String> breedsBy(UUID player) {
        return breedsByPlayer.getOrDefault(player, Set.of());
    }

    public Set<String> collectedBy(UUID player) {
        return collectedByPlayer.getOrDefault(player, Set.of());
    }

    // ------------------------------------------------------------------

    /**
     * <b>Discover a gene by owning a horse that carries it.</b> Taming, breeding
     * or otherwise coming to own one is the only way in - and discovering it
     * unlocks its carrot recipe at the same moment, because there is no longer a
     * second step to take.
     *
     * <p>There used to be one: a research paper could be <i>read</i>, which
     * discovered the gene and unlocked the carrot. That made a paper a shortcut
     * past the animals - find one in a chest and you knew a gene you had never
     * met - so it is gone. A paper is a component now, not a lesson.
     */
    public void discover(ServerPlayer player, Gene gene, List<String> seenTokens) {
        UUID id = player.getUUID();
        boolean changed = discoverInternal(id, gene, seenTokens, player.level().getGameTime());
        Map<String, Entry> book = byPlayer.get(id);
        Entry e = book.get(gene.key());
        if (!e.carrotUnlocked()) {
            book.put(gene.key(), new Entry(e.seenTokens(), true, e.discoveredAt()));
            changed = true;
        }
        if (changed) {
            setDirty();
            sync(player);
        }
    }

    private boolean discoverInternal(UUID id, Gene gene, List<String> tokens, long now) {
        Map<String, Entry> book = byPlayer.computeIfAbsent(id, k -> new LinkedHashMap<>());
        Entry e = book.get(gene.key());
        if (e == null) {
            book.put(gene.key(), new Entry(List.copyOf(new LinkedHashSet<>(tokens)), false, now));
            return true;
        }
        Set<String> merged = new LinkedHashSet<>(e.seenTokens());
        if (merged.addAll(tokens)) {
            book.put(gene.key(), new Entry(List.copyOf(merged), e.carrotUnlocked(), e.discoveredAt()));
            return true;
        }
        return false;
    }

    public boolean isCarrotUnlocked(UUID player, String geneKey) {
        Map<String, Entry> book = byPlayer.get(player);
        Entry e = book == null ? null : book.get(geneKey);
        return e != null && e.carrotUnlocked();
    }

    public boolean knows(UUID player, String geneKey) {
        Map<String, Entry> book = byPlayer.get(player);
        return book != null && book.containsKey(geneKey);
    }

    public Map<String, Entry> bookOf(UUID player) {
        return byPlayer.getOrDefault(player, Map.of());
    }

    public void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, GeneDatabaseSyncPayload.of(
                bookOf(player.getUUID()), collectedBy(player.getUUID()),
                breedsBy(player.getUUID())));
    }
}
