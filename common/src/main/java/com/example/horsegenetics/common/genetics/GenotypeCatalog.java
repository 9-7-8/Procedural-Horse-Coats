package com.example.horsegenetics.common.genetics;

import java.util.ArrayList;
import java.util.List;

/**
 * Every <b>visually distinct</b> genotype the registered {@link Genes} can
 * produce, in a stable order. This is what the horse dimension's gallery walks:
 * one pen per entry.
 *
 * <p>Nothing here is hard-coded. The catalogue falls out of
 * {@link Genes#codeOrder()}, each {@link Gene#alleles()} list and each
 * {@link Gene#dominance()}, so registering a gene (or an allele) widens it on
 * its own.
 *
 * <h2>What counts as distinct</h2>
 * The full product of every allele pair would be mostly duplicates, so two
 * reductions from {@link DominancePattern} are applied:
 * <ul>
 *   <li><b>Heterozygotes are dropped</b> unless the gene is
 *       {@link DominancePattern#INCOMPLETE_DOMINANT} - on a dominant or
 *       recessive gene the heterozygote is a copy of one of the homozygotes.
 *       So extension contributes {@code ee} and {@code EE}, but cream
 *       contributes {@code NN}, {@code CrN} <i>and</i> {@code CrCr}.</li>
 *   <li>A {@link DominancePattern#COMPLETE_DOMINANT} gene <b>masks everything
 *       else</b>: while it shows, no other gene is visible, so the catalogue
 *       keeps exactly one entry for it - the variant homozygote with every
 *       other gene at its wild type. That's why there is one white pen and one
 *       test pen instead of hundreds.</li>
 * </ul>
 *
 * <h2>Ordering</h2>
 * A mixed-radix odometer over {@link #distinctPairsOf} whose <b>first</b> gene
 * in {@link Genes#codeOrder()} is the fastest-varying digit, with the masked
 * duplicates filtered out - so walking the catalogue exhausts one gene before
 * touching the next: {@code eeaa, EEaa, eeAA, EEAA, [white], ...} Within one
 * gene the pairs run <b>least dominant first</b>.
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
     * Every unordered {@link AllelePair} of {@code gene}, least dominant first.
     * {@link Gene#alleles()} is most-dominant-first, so this walks it backwards.
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
     * The pairs of {@code gene} that are worth their own pen: all of them when
     * the heterozygote has a look of its own
     * ({@link DominancePattern#heterozygoteIsDistinct()}), otherwise the
     * homozygotes only.
     */
    public static List<AllelePair> distinctPairsOf(Gene gene) {
        if (gene.dominance().heterozygoteIsDistinct()) {
            return allPairsOf(gene);
        }
        List<AllelePair> homozygous = new ArrayList<>();
        for (AllelePair pair : allPairsOf(gene)) {
            if (pair.homozygous()) {
                homozygous.add(pair);
            }
        }
        return List.copyOf(homozygous);
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
     * How many genotypes exist <i>before</i> the dominance reduction - the raw
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
     * masking matters here (the heterozygote reduction already happened in
     * {@link #distinctPairsOf}): once a {@code COMPLETE_DOMINANT} gene shows,
     * every other gene is invisible, so the one entry kept is the one where
     * every other gene sits at its wild type.
     */
    private static boolean isCanonical(Genotype genotype) {
        Gene masker = null;
        for (Gene gene : Genes.codeOrder()) {
            if (gene.dominance().masksOtherGenes() && showsVariant(gene, genotype.pair(gene))) {
                masker = gene;
                break;
            }
        }
        if (masker == null) {
            return true;
        }
        for (Gene gene : Genes.codeOrder()) {
            if (gene != masker && !isWildType(gene, genotype.pair(gene))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Does {@code pair} express the variant? Only asked of dominant genes
     * (including {@code COMPLETE_DOMINANT}), where carrying one variant allele
     * is enough - so this is just "not homozygous wild type".
     */
    private static boolean showsVariant(Gene gene, AllelePair pair) {
        return !isWildType(gene, pair);
    }

    private static boolean isWildType(Gene gene, AllelePair pair) {
        return pair.homozygous() && pair.first().equals(gene.wildType());
    }
}
