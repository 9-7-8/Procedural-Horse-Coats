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
 * magic-carrot recipe is unlocked. Server-global {@link SavedData} keyed by
 * player {@link UUID}, so it lives in {@code <save>/data/} and gates crafting;
 * a client mirror ({@code client/ClientGeneDatabase}) drives the browser tab
 * only.
 *
 * <p>It hides nothing (settled): the info panel, pen signs and genotype code
 * keep showing every gene a horse carries. The only thing discovery gates is
 * the magic-carrot recipe.
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

    private record PlayerBook(UUID player, Map<String, Entry> byGene) {
        static final Codec<PlayerBook> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(PlayerBook::player),
                Codec.unboundedMap(Codec.STRING, Entry.CODEC).fieldOf("genes").forGetter(PlayerBook::byGene)
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

    private GeneDatabaseData() {
    }

    private GeneDatabaseData(List<PlayerBook> books) {
        for (PlayerBook b : books) {
            byPlayer.put(b.player(), new LinkedHashMap<>(b.byGene()));
        }
    }

    public static GeneDatabaseData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<PlayerBook> snapshot() {
        List<PlayerBook> out = new ArrayList<>();
        byPlayer.forEach((id, m) -> out.add(new PlayerBook(id, Map.copyOf(m))));
        return out;
    }

    // ------------------------------------------------------------------

    /** Read a paper: discover the gene, unlock its carrot recipe. True if anything changed. */
    public boolean read(ServerPlayer player, Gene gene) {
        boolean changed = discoverInternal(player.getUUID(), gene, allTokens(gene), player.level().getGameTime());
        Map<String, Entry> book = byPlayer.get(player.getUUID());
        Entry e = book.get(gene.key());
        if (!e.carrotUnlocked()) {
            book.put(gene.key(), new Entry(e.seenTokens(), true, e.discoveredAt()));
            changed = true;
        }
        if (changed) {
            setDirty();
            sync(player);
        }
        return changed;
    }

    /** Discover a gene from meeting a horse that carries a non-baseline allele at it. */
    public void discover(ServerPlayer player, Gene gene, List<String> seenTokens) {
        if (discoverInternal(player.getUUID(), gene, seenTokens, player.level().getGameTime())) {
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

    private static List<String> allTokens(Gene gene) {
        List<String> out = new ArrayList<>();
        gene.alleles().forEach(a -> out.add(a.token()));
        return out;
    }

    public void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, GeneDatabaseSyncPayload.of(bookOf(player.getUUID())));
    }
}
