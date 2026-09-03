package com.example.horsegenetics.common.genetics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every <b>visually distinct</b> genotype the registered {@link Genes} can
 * produce, in a stable order. This is what the horse dimension's gallery walks:
 * one pen per entry.
 *
 * <p>Nothing here is hard-coded. The catalogue falls out of
 * {@link Genes#codeOrder()}, each {@link Gene#alleles()} list and each
 * {@link Gene#expressionOf}, so registering a gene (or an allele) widens it on
 * its own.
 *
 * <h2>What counts as distinct</h2>
 * The full product of every allele pair would be mostly duplicates, so two
 * reductions are applied - both of them read straight off the gene's
 * {@link Expression} table, with no dominance metadata in the middle:
 * <ul>
 *   <li><b>Pairs that land on the same {@link Expression} collapse</b> to one
 *       representative. That is what the old "drop the heterozygote unless the
 *       gene is incomplete dominant" rule was approximating, except it is now
 *       exact and works for any number of alleles: extension contributes
 *       {@code ee} and {@code EE} because {@code Ee} and {@code EE} both land
 *       on the wild type, while sabino contributes all three because all three
 *       land somewhere different.</li>
 *   <li>An expression that {@link Expression#masks() masks} <b>hides everything
 *       else</b>: while it shows, no other gene is visible, so the catalogue
 *       keeps exactly one entry for it - that combination with every other gene
 *       at its wild type. That's why there is one white pen and one test pen
 *       instead of hundreds.</li>
 * </ul>
 *
 * <h2>Ordering</h2>
 * A mixed-radix odometer over {@link #distinctPairsOf} whose <b>first</b> gene
 * in {@link Genes#codeOrder()} is the fastest-varying digit, with the masked
 * duplicates filtered out - so walking the catalogue exhausts one gene before
 * touching the next: {@code eeaa, EEaa, eeAA, EEAA, [white], ...} Within one
 * gene the pairs run in {@link #allPairsOf} order.
 */
public final class GenotypeCatalog {

    /**
     * Built on first use and thrown away whenever a gene is registered - the
     * catalogue is a product over {@link Genes#codeOrder()}, so a gene loaded at
     * startup widens it. A few thousand entries, so rebuilding is cheap; the
     * alternative (eager, at class load) silently missed every data-driven gene.
     */
    private static volatile List<Genotype> entries;

    private GenotypeCatalog() {
    }

    /** Called by {@link Genes} when the registry changes. */
    static void invalidate() {
        entries = null;
    }

    private static List<Genotype> entriesOrBuild() {
        List<Genotype> e = entries;
        if (e == null) {
            synchronized (GenotypeCatalog.class) {
                e = entries;
                if (e == null) {
                    e = build();
                    entries = e;
                }
            }
        }
        return e;
    }

    /**
     * Every unordered {@link AllelePair} of {@code gene} - all
     * {@code n(n+1)/2} of them for {@code n} alleles - walking
     * {@link Gene#alleles()} backwards, so the last-declared allele (by
     * convention the population's baseline) comes first.
     */
    public static List<AllelePair> allPairsOf(Gene gene) {
        List<Allele> alleles = gene.alleles();
        List<AllelePair> pairs = new ArrayList<>();
        for (int i = alleles.size() - 1; i >= 0; i--) {
            for (int j = alleles.size() - 1; j >= i; j--) {
                pairs.add(new AllelePair(alleles.get(i), alleles.get(j)));
            }
        }
        return List.copyOf(pairs);
    }

    /**
     * The pairs of {@code gene} that are worth their own pen: one per distinct
     * {@link Expression}. Pairs landing on the same expression look the same, so
     * only one is kept - the <b>homozygous</b> one where the group has one
     * (a pen labelled {@code EE} reads better than one labelled {@code Ee}),
     * otherwise the first in {@link #allPairsOf} order.
     *
     * <p><b>Every wild type is one group.</b> A gene may declare several
     * ({@code "wild"} and {@code "carrier"} say something different in the gene
     * dictionary), but "changes nothing" is one look, and the gallery is about
     * looks - so a carrier does not get its own pen indistinguishable from the
     * plain one.
     */
    public static List<AllelePair> distinctPairsOf(Gene gene) {
        Map<String, AllelePair> byExpression = new LinkedHashMap<>();
        for (AllelePair pair : allPairsOf(gene)) {
            Expression e = gene.expressionOf(pair);
            String group = e.wildType() ? "" : e.id();
            AllelePair kept = byExpression.get(group);
            if (kept == null || (!kept.homozygous() && pair.homozygous())) {
                byExpression.put(group, pair);
            }
        }
        return List.copyOf(byExpression.values());
    }

    /**
     * How many visually distinct genotypes exist. Epigenetics are <b>not</b>
     * counted - two horses with the same genotype and different epigenetic
     * seeds are one entry.
     */
    public static int size() {
        return entriesOrBuild().size();
    }

    /**
     * How many genotypes exist <i>before</i> the same-expression reduction - the raw
     * product of every gene's {@link #allPairsOf} count. Every one of these is
     * a distinct heritable genotype; {@link #size()} is how many of them are
     * distinct to <i>look</i> at. Epigenetics aren't counted in either.
     */
    public static long totalGenotypes() {
        long total = 1L;
        for (Gene gene : Genes.codeOrder()) {
            total *= allPairsOf(gene).size();
        }
        return total;
    }

    /** The genotype at {@code index} in {@code [0, size())}. */
    public static Genotype get(int index) {
        return entriesOrBuild().get(index);
    }

    /** The whole catalogue, in order. */
    public static List<Genotype> entries() {
        return entriesOrBuild();
    }

    // ------------------------------------------------------------------

    private static List<Genotype> build() {
        List<Gene> order = Genes.codeOrder();
        List<List<AllelePair>> options = new ArrayList<>();
        long combinations = 1L;
        for (Gene gene : order) {
            List<AllelePair> pairs = distinctPairsOf(gene);
            options.add(pairs);
            combinations *= pairs.size();
        }

        List<Genotype> out = new ArrayList<>();
        for (long combination = 0; combination < combinations; combination++) {
            List<AllelePair> pairs = new ArrayList<>(order.size());
            long remaining = combination;
            for (List<AllelePair> gene : options) {
                pairs.add(gene.get((int) (remaining % gene.size())));
                remaining /= gene.size();
            }
            Genotype genotype = Genotype.of(pairs);
            if (isCanonical(genotype)) {
                out.add(genotype);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Is this the catalogue's chosen representative of how it looks? Only
     * masking matters here (the same-expression reduction already happened in
     * {@link #distinctPairsOf}): once a gene lands on a
     * {@link Expression#masks() masking} expression every other gene is
     * invisible, so the one entry kept is the one where every other gene sits
     * at a wild type.
     */
    private static boolean isCanonical(Genotype genotype) {
        Gene masker = null;
        for (Gene gene : Genes.codeOrder()) {
            if (gene.expressionOf(genotype.pair(gene)).masks()) {
                masker = gene;
                break;
            }
        }
        if (masker == null) {
            return true;
        }
        for (Gene gene : Genes.codeOrder()) {
            if (gene != masker && !gene.expressionOf(genotype.pair(gene)).wildType()) {
                return false;
            }
        }
        return true;
    }
}
