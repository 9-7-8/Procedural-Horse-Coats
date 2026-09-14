package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.epi.EpiCodec;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

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
 * <h2>Only genes that vary are stored</h2>
 * A gene declares what it writes on an allele copy with
 * {@link Gene#epiSchema()}, and most declare nothing - their behaviour is a pure
 * function of their alleles. Those genes get <b>no segment at all</b>, which is
 * what keeps the code readable now that each stored gene carries a couple of
 * hundred characters of literal numbers rather than a sixteen-digit seed.
 *
 * <h2>A wild-type copy carries nothing</h2>
 * Owner, 2026-09-14: <i>"wildtype for most genes should not carry any epigene
 * data"</i>. A copy of a gene's {@link Gene#defaultAllele() wild-type allele} is
 * never the copy a painter or an ability reads - {@code WildTypeCopyTest} checks
 * that for every registered gene - so its numbers were dead weight: about half of
 * every horse's code, inherited and drifted for nothing. {@link #alignedTo} empties
 * those copies ({@link AlleleEpigenetics#NONE}), and gives an empty copy that has
 * become a variant (forced eyes, an editor changing an allele) deterministic numbers,
 * so nothing ever reads a blank. {@link Genome} aligns in its constructor, which every
 * horse passes through. A gene whose wild type really does read its copy opts out
 * with {@link Gene#wildTypeCarriesEpigenetics()}.
 *
 * <h2>The code string</h2>
 * One {@code <geneKey>=<copy>/<copy>} segment per varying gene in
 * {@link Genes#codeOrder()}, joined by {@code ;}. Each copy is
 * {@code p:<priority>} followed by that gene's named values - see
 * {@link EpiCodec} for the field grammar - or {@code -} for a copy that carries
 * nothing, and a gene whose two copies both carry nothing writes no segment at
 * all. Parsing is <b>tolerant</b> the same
 * way {@link Genotype#parse} is: a segment naming an unregistered gene is
 * dropped, a registered gene with no segment is rolled deterministically from
 * its key, and a value the text omits is rolled deterministically from the gene
 * key plus the value's name. There is <b>no</b> legacy seed-format handling.
 */
public final class Epigenome {

    private static final String NAME_SEP = "=";
    private static final String PRIORITY_FIELD = "p";

    /** What a gene that declares no epigenetics reads back as. */
    private static final Copies NONE = new Copies(
            new AlleleEpigenetics(1, EpiValues.EMPTY),
            new AlleleEpigenetics(2, EpiValues.EMPTY));

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

    /** Every gene that actually writes something on an allele copy. */
    private static boolean stores(Gene g) {
        return !g.epiSchema().isEmpty();
    }

    /**
     * The same question, publicly - {@link #with} only accepts a gene this
     * returns {@code true} for, and the gene editors need to ask before they
     * put one locus's numbers back after re-rolling the rest.
     */
    public static boolean carries(Gene gene) {
        return stores(gene);
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /**
     * Fresh epigenetics for a founder / wild horse: independent values on every
     * allele copy, rolled from each gene's declared distributions, and
     * deconflicted so no gene carries the same priority twice.
     */
    public static Epigenome random(Rng rng) {
        Map<String, Copies> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            if (!stores(g)) {
                continue;
            }
            m.put(g.key(), founderCopies(g.epiSchema(), rng));
        }
        return new Epigenome(m);
    }

    /**
     * The same, replayed from a single {@code long} - a <b>reproducible test and
     * tooling horse</b>, used by the golden-coat suite, the coat sample sheet,
     * the spec fixtures and the designer's preview.
     *
     * <p>It is <b>not</b> a stand-in for a horse whose epigenome was not stored.
     * It used to be used that way by the family tree, which drew a dead
     * ancestor's coat from a seed derived from its record UUID - a plausible
     * horse rather than the real one. Records carry their epigenome, so that
     * fallback is gone; if a record has no genome the honest answer is to draw
     * nothing.
     */
    public static Epigenome fromSeed(long seed) {
        Map<String, Copies> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            if (!stores(g)) {
                continue;
            }
            // Each gene draws from its OWN generator, namespaced by its key,
            // rather than from a shared stream read in registry order.
            //
            // This is what closes known-gaps gap 47. The shared-stream version
            // walked codeOrder() drawing as it went, so registering a locus at
            // priority 68 gave every gene above 68 a different draw and the
            // horse at seed 13 became a different horse. That is free in play -
            // there are no saves - but it is not free in tests: adding natural
            // zebra broke three assertions in WhitePatternGenesTest, all by
            // margins under 0.05, none of them a real regression, and the only
            // signal was a red build after an unrelated-looking change.
            //
            // Keying off the gene makes a seeded horse STABLE under gene
            // registration: adding, removing or re-prioritising a locus now
            // moves that locus and nothing else. The narrower shape of the same
            // gap survives on purpose - changing how many values one gene
            // declares still reshuffles that gene - but that is one gene, the
            // bake golden names it, and it is the change you were making.
            m.put(g.key(), copiesFor(g, seed));
        }
        return new Epigenome(m);
    }

    /**
     * One gene's seeded copies, from the seed and that gene's key <b>alone</b>.
     *
     * <p>Public because it is the whole of the rule {@link #fromSeed} follows,
     * and {@code EpigenomeTest} asserts that rule directly: recompute a gene on
     * its own and it must match the one inside a whole seeded horse. That is
     * what stops the shared-stream behaviour (known-gaps gap 47) coming back
     * unnoticed.
     */
    public static Copies copiesFor(Gene gene, long seed) {
        return founderCopies(gene.epiSchema(), new SeededRng(seed, gene.key()));
    }

    private static Copies founderCopies(EpiSchema schema, Rng rng) {
        AlleleEpigenetics a = AlleleEpigenetics.founder(schema, rng);
        AlleleEpigenetics b = AlleleEpigenetics.deconflict(a, AlleleEpigenetics.founder(schema, rng), rng);
        return new Copies(a, b);
    }

    /** From explicit per-gene copies; every gene that stores must be supplied. */
    public static Epigenome of(Map<String, Copies> byGene) {
        Map<String, Copies> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            if (!stores(g)) {
                continue;
            }
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
            for (String segment : split(code, EpiCodec.GENE_SEP)) {
                if (segment.isEmpty()) {
                    continue;
                }
                int eq = segment.indexOf(NAME_SEP);
                if (eq < 0) {
                    throw new IllegalArgumentException(
                            "epigenome segment needs '<gene>=<copy>/<copy>', got: " + segment);
                }
                Gene g = Genes.byKeyOrNull(segment.substring(0, eq));
                if (g == null || !stores(g)) {
                    continue; // a gene no longer registered, or one that stopped varying
                }
                String[] copies = split(segment.substring(eq + 1), EpiCodec.COPY_SEP);
                if (copies.length != 2) {
                    throw new IllegalArgumentException("segment for " + g.key()
                            + " needs two '/'-separated copies, got: " + segment);
                }
                supplied.put(g.key(), new Copies(
                        parseCopy(g, copies[0]), parseCopy(g, copies[1])));
            }
        }
        Map<String, Copies> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            if (!stores(g)) {
                continue;
            }
            Copies c = supplied.get(g.key());
            m.put(g.key(), c != null ? c : placeholder(g));
        }
        return new Epigenome(m);
    }

    /**
     * Deterministic stand-in epigenetics for a gene the stored code does not
     * mention - seeded off the gene key so every horse agrees on it, which is
     * what stops two clients rendering the same horse differently.
     */
    private static Copies placeholder(Gene g) {
        return founderCopies(g.epiSchema(), new SeededRng(g.key().hashCode()));
    }

    private static AlleleEpigenetics parseCopy(Gene g, String text) {
        if (EMPTY_COPY.equals(text.trim())) {
            return AlleleEpigenetics.NONE;
        }
        Map<String, String> fields = EpiCodec.fields(text);
        String p = fields.remove(PRIORITY_FIELD);
        int priority = p == null ? AlleleEpigenetics.MIN_PRIORITY : Integer.parseInt(p.trim());
        return new AlleleEpigenetics(priority, EpiCodec.read(g.epiSchema(), fields, g.key()));
    }

    /**
     * {@code String.split} with a literal char and no regex - the separators are
     * characters that mean something to a regex engine, and {@code common/} is
     * built to compile under TeaVM where a regex is a dependency worth avoiding.
     */
    private static String[] split(String s, char sep) {
        int count = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                count++;
            }
        }
        String[] out = new String[count];
        int at = 0;
        int n = 0;
        while (true) {
            int end = s.indexOf(sep, at);
            if (end < 0) {
                out[n] = s.substring(at);
                return out;
            }
            out[n++] = s.substring(at, end);
            at = end + 1;
        }
    }

    public String toCode() {
        StringBuilder sb = new StringBuilder(1024);
        for (Gene g : Genes.codeOrder()) {
            if (!stores(g)) {
                continue;
            }
            Copies c = byGene.get(g.key());
            if (c.first().isEmpty() && c.second().isEmpty()) {
                continue;   // two wild-type copies: nothing to write
            }
            if (sb.length() > 0) {
                sb.append(EpiCodec.GENE_SEP);
            }
            sb.append(g.key()).append(NAME_SEP);
            appendCopy(sb, c.first());
            sb.append(EpiCodec.COPY_SEP);
            appendCopy(sb, c.second());
        }
        return sb.toString();
    }

    /** How a copy that carries nothing is written. */
    private static final String EMPTY_COPY = "-";

    private static void appendCopy(StringBuilder sb, AlleleEpigenetics e) {
        if (e.isEmpty()) {
            sb.append(EMPTY_COPY);
            return;
        }
        sb.append(PRIORITY_FIELD).append(EpiCodec.NAME_SEP).append(e.priority());
        EpiCodec.write(sb, e.values()); // each field is written with its leading comma
    }

    // ------------------------------------------------------------------
    // Wild-type copies
    // ------------------------------------------------------------------

    /**
     * Is a copy of {@code allele} at {@code gene} one that carries nothing? True for
     * a gene's wild-type allele, unless the gene says its wild type reads its copy.
     */
    public static boolean silent(Gene gene, Allele allele) {
        return stores(gene) && !gene.wildTypeCarriesEpigenetics() && allele.equals(gene.defaultAllele());
    }

    /**
     * <b>This epigenome, fitted to the alleles it rides on</b> - see the class note.
     * A silent copy is emptied; a copy of any other allele that carries nothing is
     * given deterministic numbers (seeded from the gene key and the slot, the same
     * way {@link #parse} fills a gene the code omits), so every reader of a variant
     * copy finds a value. Returns {@code this} when nothing needs to change.
     */
    public Epigenome alignedTo(Genotype genotype) {
        Map<String, Copies> next = null;
        for (Gene g : Genes.codeOrder()) {
            if (!stores(g)) {
                continue;
            }
            Copies c = copies(g);
            AllelePair pair = genotype.pair(g);
            AlleleEpigenetics first = fit(g, pair.first(), c.first(), 0);
            AlleleEpigenetics second = fit(g, pair.second(), c.second(), 1);
            if (!first.isEmpty() && !second.isEmpty() && first.priority() == second.priority()) {
                second = second.bumped(true);   // a filled copy must not tie its partner
            }
            if (first != c.first() || second != c.second()) {
                if (next == null) {
                    next = new LinkedHashMap<>(byGene);
                }
                next.put(g.key(), new Copies(first, second));
            }
        }
        return next == null ? this : new Epigenome(next);
    }

    private static AlleleEpigenetics fit(Gene g, Allele allele, AlleleEpigenetics copy, int slot) {
        if (silent(g, allele)) {
            return copy.isEmpty() ? copy : AlleleEpigenetics.NONE;
        }
        if (!copy.isEmpty()) {
            return copy;
        }
        return AlleleEpigenetics.founder(g.epiSchema(), new SeededRng(g.key().hashCode() * 31L + slot));
    }

    // ------------------------------------------------------------------
    // Access
    // ------------------------------------------------------------------

    /**
     * This epigenome with one gene's copies replaced - how {@code BreedFounder}
     * stamps a breed's target onto a founder it has just rolled. Nothing else
     * should need it: after a horse exists its numbers only ever change by
     * inheritance.
     */
    public Epigenome with(String geneKey, Copies replacement) {
        Map<String, Copies> next = new LinkedHashMap<>(byGene);
        if (!next.containsKey(geneKey)) {
            throw new IllegalArgumentException(geneKey + " stores no epigenetics");
        }
        next.put(geneKey, replacement);
        return new Epigenome(next);
    }

    public Copies copies(Gene gene) {
        return copies(gene.key());
    }

    public Copies copies(String geneKey) {
        Copies c = byGene.get(geneKey);
        return c != null ? c : NONE;
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

    /** The literal numbers the coat pipeline paints {@code gene} with, for this horse. */
    public EpiValues expressedValues(Gene gene, Genotype genotype) {
        return expressed(gene, genotype).values();
    }

    /**
     * A 64-bit digest of just the epigenetics that <i>can change this horse's
     * pixels</i> - the expressed values of every gene that is both visible and
     * non-deterministic under {@code genotype}. Two horses agreeing here render
     * the same coat, so this (not the whole epigenome) is what
     * {@code CoatData.textureKey()} keys on.
     *
     * <p>It hashes the <b>values</b> now rather than a single seed. Getting this
     * wrong does not throw - it silently forks the texture cache, or worse
     * collides two different horses onto one texture - so it must fold in every
     * number a visible gene could paint with.
     */
    public long visibleFingerprint(Genotype genotype) {
        long h = 0xcbf29ce484222325L;
        for (Gene g : Genes.codeOrder()) {
            if (!stores(g)) {
                continue;
            }
            AllelePair pair = genotype.pair(g);
            if (!g.isVisible(pair, genotype) || g.isDeterministic(pair, genotype)) {
                continue;
            }
            h = (h ^ g.key().hashCode()) * 0x100000001b3L;
            h = expressedValues(g, genotype).hashInto(h);
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
