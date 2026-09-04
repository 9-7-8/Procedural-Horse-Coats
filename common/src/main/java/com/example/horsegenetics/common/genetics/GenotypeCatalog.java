package com.example.horsegenetics.common.genetics;

import java.util.AbstractList;
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
 *       on the wild type, while {@code KIT}'s eight alleles contribute eight
 *       entries out of thirty-six combinations.</li>
 *   <li>An expression that {@link Expression#masks() masks} <b>hides everything
 *       else</b>: while it shows, no other gene is visible, so the catalogue
 *       keeps exactly one entry for it - that combination with every other gene
 *       at its wild type. That's why dominant white, lethal white and the test
 *       overlay get one pen each instead of a third of the corridor.</li>
 * </ul>
 *
 * <h2>Nothing is materialised</h2>
 * The catalogue is <b>computed on demand</b>: {@link #size()} is arithmetic and
 * {@link #get(int)} builds one {@link Genotype} from an odometer reading. It
 * used to be an eagerly-built {@code List}, which was fine at a few thousand
 * entries and is not fine now - every gene multiplies the count, and the white
 * -pattern loci alone put it in the millions. A list that size is hundreds of
 * megabytes of {@code Genotype} nobody ever reads more than a few hundred of.
 *
 * <h2>Ordering</h2>
 * A mixed-radix odometer over the <b>non-masking</b> {@link #distinctPairsOf}
 * whose <b>first</b> gene in {@link Genes#codeOrder()} is the fastest-varying
 * digit - so walking the catalogue exhausts one gene before touching the next:
 * {@code eeaa, EEaa, eeAA, EEAA, eeaa ChCh, ...} The one entry each masking
 * expression owns comes <b>after</b> all of them, in gene order, because a
 * masked horse has no position in a product it takes no part in.
 */
public final class GenotypeCatalog {

    /**
     * The odometer's digits, rebuilt whenever a gene is registered. Cheap to
     * compute (it is one pass over the genes) and it holds only the per-gene
     * pair lists, never the product.
     */
    private static volatile Layout layout;

    private GenotypeCatalog() {
    }

    /** Called by {@link Genes} when the registry changes. */
    static void invalidate() {
        layout = null;
    }

    /**
     * Every unordered {@link AllelePair} of {@code gene} a horse can actually
     * carry - all {@code n(n+1)/2} of them for {@code n} alleles, minus any the
     * gene rules out with {@link Gene#canOccur} (the sex locus has no
     * {@code Y/Y}; {@code KIT} has no homozygote of a nonviable {@code W}) -
     * walking {@link Gene#alleles()} backwards, so the last-declared allele (by
     * convention the population's baseline) comes first.
     */
    public static List<AllelePair> allPairsOf(Gene gene) {
        List<Allele> alleles = gene.alleles();
        List<AllelePair> pairs = new ArrayList<>();
        for (int i = alleles.size() - 1; i >= 0; i--) {
            for (int j = alleles.size() - 1; j >= i; j--) {
                AllelePair pair = new AllelePair(alleles.get(i), alleles.get(j));
                if (gene.canOccur(pair)) {
                    pairs.add(pair);
                }
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
        Layout l = layoutOrBuild();
        long total = l.plainCombinations + l.masked.size();
        return (int) Math.min(total, Integer.MAX_VALUE);
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

    /** The genotype at {@code index} in {@code [0, size())}. Built on the spot. */
    public static Genotype get(int index) {
        Layout l = layoutOrBuild();
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("index " + index + " outside catalogue of " + size());
        }
        if (index >= l.plainCombinations) {
            return l.masked.get((int) (index - l.plainCombinations));
        }
        List<AllelePair> pairs = new ArrayList<>(l.plain.size());
        long remaining = index;
        for (List<AllelePair> gene : l.plain) {
            pairs.add(gene.get((int) (remaining % gene.size())));
            remaining /= gene.size();
        }
        return Genotype.of(pairs);
    }

    /**
     * The whole catalogue, in order, as a <b>lazy view</b> - iterating it is
     * fine, holding on to it costs nothing, and calling something like
     * {@code toList()} on a multi-million-entry catalogue is the caller's
     * problem to avoid.
     */
    public static List<Genotype> entries() {
        int n = size();
        return new AbstractList<>() {
            @Override public Genotype get(int index) {
                return GenotypeCatalog.get(index);
            }

            @Override public int size() {
                return n;
            }
        };
    }

    // ------------------------------------------------------------------

    /**
     * The odometer digits: one list of non-masking pairs per gene, plus the
     * flat list of one-entry-each masked genotypes.
     */
    private record Layout(List<List<AllelePair>> plain, long plainCombinations, List<Genotype> masked) {
    }

    private static Layout layoutOrBuild() {
        Layout l = layout;
        if (l == null) {
            synchronized (GenotypeCatalog.class) {
                l = layout;
                if (l == null) {
                    l = buildLayout();
                    layout = l;
                }
            }
        }
        return l;
    }

    private static Layout buildLayout() {
        List<Gene> order = Genes.codeOrder();
        Map<Gene, List<AllelePair>> distinct = new LinkedHashMap<>();
        for (Gene gene : order) {
            distinct.put(gene, distinctPairsOf(gene));
        }

        List<List<AllelePair>> plain = new ArrayList<>(order.size());
        long combinations = 1L;
        for (Gene gene : order) {
            List<AllelePair> unmasked = new ArrayList<>();
            for (AllelePair pair : distinct.get(gene)) {
                if (!gene.expressionOf(pair).masks()) {
                    unmasked.add(pair);
                }
            }
            plain.add(List.copyOf(unmasked));
            combinations *= unmasked.size();
        }

        // One entry per masking expression: that combination, everything else wild.
        List<Genotype> masked = new ArrayList<>();
        for (Gene masker : order) {
            for (AllelePair pair : distinct.get(masker)) {
                if (!masker.expressionOf(pair).masks()) {
                    continue;
                }
                List<AllelePair> pairs = new ArrayList<>(order.size());
                for (Gene gene : order) {
                    pairs.add(gene == masker ? pair : wildTypePairOf(gene, distinct.get(gene)));
                }
                masked.add(Genotype.of(pairs));
            }
        }

        return new Layout(List.copyOf(plain), combinations, List.copyOf(masked));
    }

    /**
     * The representative of {@code gene}'s wild-type group. Every gene in the
     * model has one; the fallback exists so a gene that somehow declares no
     * silent combination still produces a valid genotype instead of a null.
     */
    private static AllelePair wildTypePairOf(Gene gene, List<AllelePair> distinct) {
        for (AllelePair pair : distinct) {
            if (gene.expressionOf(pair).wildType()) {
                return pair;
            }
        }
        return distinct.get(0);
    }
}
