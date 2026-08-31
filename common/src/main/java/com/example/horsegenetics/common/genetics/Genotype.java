package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A horse's full genotype: one {@link AllelePair} per registered {@link Gene}
 * (see {@link Genes}). Built from {@link Allele} objects; still round-trips
 * through a compact <b>code string</b> (two symbols per gene, in
 * {@link Genes#codeOrder()}) for persistence, sync and the pedigree record.
 *
 * <p>Shorter legacy codes still parse - a missing trailing locus is read as
 * that gene's wild-type. So {@code "EeAawwtt"} (pre-champagne) becomes
 * {@code "EeAawwttcc"}, {@code "EeAaWw"} (pre-test) adds {@code "ttcc"}, and
 * {@code "EeAa"} (pre-white) adds {@code "wwttcc"}.
 *
 * <p>Pure data + logic - no Minecraft.
 */
public final class Genotype {

    private final Map<String, AllelePair> byGene;

    private Genotype(Map<String, AllelePair> byGene) {
        this.byGene = Collections.unmodifiableMap(byGene);
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /** From explicit pairs; any gene not supplied is filled with its wild-type. */
    public static Genotype of(List<AllelePair> pairs) {
        Map<String, AllelePair> supplied = new LinkedHashMap<>();
        for (AllelePair p : pairs) {
            supplied.put(p.geneKey(), p);
        }
        Map<String, AllelePair> full = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            AllelePair p = supplied.get(g.key());
            full.put(g.key(), p != null ? p : new AllelePair(g.wildType(), g.wildType()));
        }
        return new Genotype(full);
    }

    public static Genotype of(AllelePair... pairs) {
        return of(List.of(pairs));
    }

    /** Parse a canonical or shorter-legacy code. */
    public static Genotype parse(String code) {
        Objects.requireNonNull(code, "code");
        String c = padLegacy(code);
        int full = Genes.codeLength();
        if (c.length() != full) {
            throw new IllegalArgumentException("genotype code must be " + full
                    + " chars (2 per gene: " + symbolHint() + "), or a shorter legacy code; got: " + code);
        }
        Map<String, AllelePair> m = new LinkedHashMap<>();
        int i = 0;
        for (Gene g : Genes.codeOrder()) {
            Allele a1 = g.fromSymbol(c.charAt(i++));
            Allele a2 = g.fromSymbol(c.charAt(i++));
            m.put(g.key(), new AllelePair(a1, a2));
        }
        return new Genotype(m);
    }

    private static String padLegacy(String code) {
        int full = Genes.codeLength();
        // Only the historical code lengths are treated as "legacy" and padded:
        // 4 (E/A), 6 (+W), 8 (+T). Anything else must be full-length or it's an error.
        if (code.length() >= full || code.length() < 4 || code.length() % 2 != 0) {
            return code;
        }
        StringBuilder sb = new StringBuilder(code);
        List<Gene> order = Genes.codeOrder();
        for (int gi = code.length() / 2; sb.length() < full && gi < order.size(); gi++) {
            char wt = order.get(gi).wildType().symbol();
            sb.append(wt).append(wt);
        }
        return sb.toString();
    }

    private static String symbolHint() {
        StringBuilder sb = new StringBuilder();
        for (Gene g : Genes.codeOrder()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            for (Allele a : g.alleles()) {
                sb.append(a.symbol());
            }
        }
        return sb.toString();
    }

    /** Serialize to the canonical code. */
    public String toCode() {
        StringBuilder sb = new StringBuilder();
        for (Gene g : Genes.codeOrder()) {
            AllelePair p = byGene.get(g.key());
            sb.append(p.first().symbol()).append(p.second().symbol());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Random population / Mendelian breeding
    // ------------------------------------------------------------------

    /** Wild founder genotype - each gene rolls its own pair (see the gene classes for draw counts / order). */
    public static Genotype random(Rng rng) {
        Map<String, AllelePair> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            m.put(g.key(), g.randomPair(rng));
        }
        return new Genotype(m);
    }

    /**
     * Mendelian: for each gene the child takes one allele from each parent,
     * drawn 50/50 within that parent's pair. Two {@link Rng#nextBoolean()}
     * draws per gene, genes in {@link Genes#codeOrder()}.
     */
    public Genotype breedWith(Genotype other, Rng rng) {
        Map<String, AllelePair> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            AllelePair mine = pair(g);
            AllelePair theirs = other.pair(g);
            Allele c1 = rng.nextBoolean() ? mine.first() : mine.second();
            Allele c2 = rng.nextBoolean() ? theirs.first() : theirs.second();
            m.put(g.key(), new AllelePair(c1, c2));
        }
        return new Genotype(m);
    }

    // ------------------------------------------------------------------
    // Access
    // ------------------------------------------------------------------

    public AllelePair pair(Gene gene) {
        return byGene.get(gene.key());
    }

    public AllelePair pair(String geneKey) {
        return byGene.get(geneKey);
    }

    public Collection<AllelePair> pairs() {
        return byGene.values();
    }

    public boolean has(Allele allele) {
        AllelePair p = byGene.get(allele.geneKey());
        return p != null && p.has(allele);
    }

    // ------------------------------------------------------------------
    // Determinism / coat generation hooks
    // ------------------------------------------------------------------

    /** No gene needs per-horse randomness - the coat is one of a fixed set. */
    public boolean isDeterministic() {
        return !hasVisibleNonDeterministic();
    }

    /** Some visible gene is non-deterministic - the coat texture must be generated per horse. */
    public boolean hasVisibleNonDeterministic() {
        for (Gene g : Genes.codeOrder()) {
            AllelePair p = pair(g);
            if (g.isVisible(p, this) && !g.isDeterministic(p, this)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Coarse phenotype label (foal *_baby textures, family-tree fallback, UI)
    // ------------------------------------------------------------------

    public boolean isWhite() {
        return Genes.WHITE.isWhite(pair(Genes.WHITE));
    }

    public boolean hasBlackPigment() {
        return Genes.EXTENSION.producesBlack(pair(Genes.EXTENSION));
    }

    public boolean isAgouti() {
        return Genes.AGOUTI.isBay(pair(Genes.AGOUTI));
    }

    public boolean isSeal() {
        return Genes.AGOUTI.isSeal(pair(Genes.AGOUTI));
    }

    public boolean isChampagne() {
        return Genes.CHAMPAGNE.isChampagne(pair(Genes.CHAMPAGNE));
    }

    public boolean hasTest() {
        return Genes.TEST.isTest(pair(Genes.TEST));
    }

    public CoatPhenotype phenotype() {
        if (isWhite()) {
            return CoatPhenotype.WHITE;
        }
        if (!hasBlackPigment()) {
            return CoatPhenotype.CHESTNUT;
        }
        return (isAgouti() || isSeal()) ? CoatPhenotype.BAY : CoatPhenotype.BLACK;
    }

    // ------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        return o instanceof Genotype g && g.byGene.equals(byGene);
    }

    @Override
    public int hashCode() {
        return byGene.hashCode();
    }

    @Override
    public String toString() {
        return "Genotype[" + toCode() + " -> " + phenotype()
                + (isChampagne() ? " +champagne" : "")
                + (isSeal() ? " +seal" : "")
                + (hasTest() ? " +test" : "") + "]";
    }
}
