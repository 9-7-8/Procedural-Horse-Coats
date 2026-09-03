package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One {@link AlleleEpigenetics} per <b>allele copy</b> a horse carries - the
 * epigenetic half of a {@link Genome}, sitting alongside the {@link Genotype}
 * that says which alleles those copies are.
 *
 * <p><b>Alignment is the invariant:</b> for every gene, {@link Copies#first()}
 * belongs to {@code genotype.pair(gene).first()} and {@link Copies#second()} to
 * {@code .second()}. {@link AllelePair} canonicalizes itself dominant-first, so
 * anything that builds the two together has to align them the same way - which
 * is exactly why {@link Genome} owns both and does the breeding.
 *
 * <p>Round-trips through a <b>gene-keyed code string</b> shaped like the
 * genotype code: one {@code <geneKey>=<copy>/<copy>} segment per gene in
 * {@link Genes#codeOrder()} joined by {@code -}, each copy written
 * {@code <priority>:<seed in unsigned hex>}. Parsing is <b>tolerant</b> the
 * same way {@link Genotype#parse} is: a registered gene with no segment gets a
 * deterministic placeholder (derived from its key, so two horses agree on it),
 * and a segment naming an unregistered gene is dropped. Safe because an absent
 * gene reads wild type - invisible, excluded from
 * {@link #visibleFingerprint} - so a placeholder never changes the texture key.
 * There is <b>no</b> legacy positional-format handling.
 */
public final class Epigenome {

    private static final String GENE_SEP = "-";
    private static final String COPY_SEP = "/";
    private static final String FIELD_SEP = ":";
    private static final String NAME_SEP = "=";

    /** The two allele copies at one gene, aligned to that gene's {@link AllelePair}. */
    public record Copies(AlleleEpigenetics first, AlleleEpigenetics second) {

        public Copies swapped() {
            return new Copies(second, first);
        }
    }

    private final Map<String, Copies> byGene;

    private Epigenome(Map<String, Copies> byGene) {
        this.byGene = Collections.unmodifiableMap(byGene);
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /**
     * Fresh epigenetics for a founder / wild horse: an independent random
     * priority + seed on every allele copy, deconflicted so no gene carries the
     * same priority twice.
     */
    public static Epigenome random(Rng rng) {
        Map<String, Copies> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            AlleleEpigenetics a = AlleleEpigenetics.random(rng);
            AlleleEpigenetics b = AlleleEpigenetics.deconflict(a, AlleleEpigenetics.random(rng), rng);
            m.put(g.key(), new Copies(a, b));
        }
        return new Epigenome(m);
    }

    /**
     * The same, replayed from a single {@code long} - used where a horse has no
     * stored epigenome but a stable stand-in is wanted (the family tree draws
     * ancestors this way, from their record UUID).
     */
    public static Epigenome fromSeed(long seed) {
        return random(new SeededRng(seed));
    }

    /** From explicit per-gene copies; every registered gene must be supplied. */
    public static Epigenome of(Map<String, Copies> byGene) {
        Map<String, Copies> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            Copies c = byGene.get(g.key());
            if (c == null) {
                throw new IllegalArgumentException("no epigenetics supplied for " + g.key());
            }
            m.put(g.key(), c);
        }
        return new Epigenome(m);
    }

    public static Epigenome parse(String code) {
        Objects.requireNonNull(code, "code");
        Map<String, Copies> supplied = new LinkedHashMap<>();
        if (!code.isEmpty()) {
            for (String segment : code.split(GENE_SEP, -1)) {
                int eq = segment.indexOf(NAME_SEP);
                if (eq < 0) {
                    throw new IllegalArgumentException(
                            "epigenome segment needs '<gene>=<copy>/<copy>', got: " + segment);
                }
                Gene g = Genes.byKeyOrNull(segment.substring(0, eq));
                if (g == null) {
                    continue; // a gene no longer registered - drop the segment
                }
                String[] copies = segment.substring(eq + 1).split(COPY_SEP, -1);
                if (copies.length != 2) {
                    throw new IllegalArgumentException("segment for " + g.key()
                            + " needs two '/'-separated copies, got: " + segment);
                }
                supplied.put(g.key(), new Copies(parseCopy(copies[0]), parseCopy(copies[1])));
            }
        }
        Map<String, Copies> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            Copies c = supplied.get(g.key());
            m.put(g.key(), c != null ? c : placeholder(g.key()));
        }
        return new Epigenome(m);
    }

    /**
     * Deterministic stand-in epigenetics for a gene the stored code predates -
     * seeded off the gene key so every horse agrees, and never visible (the
     * gene reads wild type), so it can't fork a texture cache entry.
     */
    private static Copies placeholder(String geneKey) {
        Rng rng = new SeededRng(geneKey.hashCode());
        AlleleEpigenetics a = AlleleEpigenetics.random(rng);
        AlleleEpigenetics b = AlleleEpigenetics.deconflict(a, AlleleEpigenetics.random(rng), rng);
        return new Copies(a, b);
    }

    private static AlleleEpigenetics parseCopy(String s) {
        int sep = s.indexOf(FIELD_SEP);
        if (sep < 0) {
            throw new IllegalArgumentException("epigenetics copy needs '<priority>:<seed>', got: " + s);
        }
        return new AlleleEpigenetics(
                Integer.parseInt(s.substring(0, sep)),
                Long.parseUnsignedLong(s.substring(sep + 1), 16));
    }

    public String toCode() {
        StringBuilder sb = new StringBuilder();
        for (Gene g : Genes.codeOrder()) {
            if (sb.length() > 0) {
                sb.append(GENE_SEP);
            }
            Copies c = byGene.get(g.key());
            sb.append(g.key()).append(NAME_SEP);
            appendCopy(sb, c.first());
            sb.append(COPY_SEP);
            appendCopy(sb, c.second());
        }
        return sb.toString();
    }

    private static void appendCopy(StringBuilder sb, AlleleEpigenetics e) {
        sb.append(e.priority()).append(FIELD_SEP).append(Long.toUnsignedString(e.epigeneticSeed(), 16));
    }

    // ------------------------------------------------------------------
    // Access
    // ------------------------------------------------------------------

    public Copies copies(Gene gene) {
        return byGene.get(gene.key());
    }

    public Copies copies(String geneKey) {
        return byGene.get(geneKey);
    }

    /**
     * Which copy's epigenetics this horse actually shows at {@code gene}:
     * <ul>
     *   <li><b>heterozygous</b> - the dominant copy, i.e. {@code pair.first()}
     *       (an {@link AllelePair} is canonicalized dominant-first), because
     *       that's the allele doing the visible work;</li>
     *   <li><b>homozygous</b> - both copies express, so the tie is broken by
     *       {@link AlleleEpigenetics#priority()}: <b>higher wins</b>.</li>
     * </ul>
     */
    public AlleleEpigenetics expressed(Gene gene, Genotype genotype) {
        Copies c = copies(gene);
        AllelePair pair = genotype.pair(gene);
        if (!pair.homozygous()) {
            return c.first();
        }
        return c.first().priority() >= c.second().priority() ? c.first() : c.second();
    }

    /** The seed the coat pipeline feeds {@code gene}'s per-horse randomness. */
    public long expressedSeed(Gene gene, Genotype genotype) {
        return expressed(gene, genotype).epigeneticSeed();
    }

    /**
     * A 64-bit digest of just the epigenetics that <i>can change this horse's
     * pixels</i> - the expressed seed of every gene that is both visible and
     * non-deterministic under {@code genotype}. Two horses agreeing here render
     * the same coat, so this (not the whole epigenome) is what
     * {@code CoatData.textureKey()} keys on.
     */
    public long visibleFingerprint(Genotype genotype) {
        long h = 0xcbf29ce484222325L;
        for (Gene g : Genes.codeOrder()) {
            AllelePair pair = genotype.pair(g);
            if (!g.isVisible(pair, genotype) || g.isDeterministic(pair, genotype)) {
                continue;
            }
            h = (h ^ g.key().hashCode()) * 0x100000001b3L;
            h = (h ^ expressedSeed(g, genotype)) * 0x100000001b3L;
        }
        return h;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Epigenome e && e.byGene.equals(byGene);
    }

    @Override
    public int hashCode() {
        return byGene.hashCode();
    }

    @Override
    public String toString() {
        return "Epigenome[" + toCode() + "]";
    }
}
