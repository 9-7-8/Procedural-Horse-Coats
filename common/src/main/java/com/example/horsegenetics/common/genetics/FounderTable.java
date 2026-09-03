package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>How common each of a gene's allele combinations is in the founder
 * population</b> - the weighted table a wild horse's pair is drawn from.
 *
 * <p>This replaces the "1 in N per allele" frequency each gene used to hand-roll
 * into its own {@code randomPair}. Two reasons that had to go:
 * <ul>
 *   <li><b>It has no meaning past two alleles.</b> "1 in 45 per allele" says
 *       nothing about a locus with thirty of them.</li>
 *   <li><b>It hid the number the author actually cares about.</b> A per-allele
 *       chance only implies a homozygote rate; declaring the distribution
 *       <i>per combination</i> lets an author set the rare-homozygote rate
 *       directly, and there is no second question about whether a draw is per
 *       copy or per horse.</li>
 * </ul>
 *
 * <p><b>Weights are percentages and should sum to 100.</b> If they do not they
 * are normalised proportionally and a warning is logged - a wrong total is an
 * authoring slip, not a reason to refuse to spawn horses. The table is
 * <b>sparse</b>: any combination not listed has weight zero and simply never
 * turns up in the wild, which is what lets a thirty-allele locus declare the
 * dozen combinations that actually occur instead of all 465.
 *
 * <p><b>One {@link Rng#nextFloat()} per gene per founder</b>, drawn in
 * {@link Genes#codeOrder()} - so a founder's genotype is reproducible from the
 * RNG stream. Founders only: breeding never consults this table.
 *
 * <p>The same shape serves the chaos carrot's per-gene distribution
 * ({@code wiki/roadmap.html} §14.1) when that lands.
 */
public final class FounderTable {

    private static final Logger LOG = System.getLogger("horsegenetics.genetics");

    /**
     * How far the declared percentages may miss 100 before it is worth a
     * warning - a ten-thousandth of a percent, so a table written to six
     * decimal places is not scolded for floating-point drift.
     */
    private static final double TOTAL_EPSILON = 1e-4;

    private final List<AllelePair> pairs;
    private final double[] shares;      // normalised, sums to 1
    private final double[] cumulative;  // running sum of shares, last element 1

    private FounderTable(List<AllelePair> pairs, double[] shares, double[] cumulative) {
        this.pairs = pairs;
        this.shares = shares;
        this.cumulative = cumulative;
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The whole population carries one combination - the shape almost every
     * "this gene is not in the wild at all" case wants.
     */
    public static FounderTable always(Allele a, Allele b) {
        return builder().weight(a, b, 100.0).build();
    }

    /**
     * The three-combination table a two-allele gene gets when {@code variant}
     * is spread through the population at frequency {@code p} and pairs up at
     * random: {@code p²} homozygous variant, {@code 2p(1-p)} heterozygous,
     * {@code (1-p)²} homozygous {@code baseline}.
     *
     * <p>A convenience, not a second model - it just computes the three numbers
     * a two-allele author would otherwise work out by hand, and it is what the
     * old per-allele "1 in N" frequencies meant. A gene wanting a homozygote
     * rate that <i>isn't</i> {@code p²} (a lethal, a founder effect) states its
     * three weights directly instead, which is the whole point of declaring the
     * table per combination.
     *
     * @param p the variant's population frequency, in {@code [0, 1]} - the old
     *          "1 in N per allele" is {@code 1.0 / N}
     */
    public static FounderTable hardyWeinberg(Allele variant, Allele baseline, double p) {
        if (p < 0.0 || p > 1.0) {
            throw new IllegalArgumentException("allele frequency must be in [0, 1], got " + p);
        }
        double q = 1.0 - p;
        return builder()
                .weight(variant, variant, 100.0 * p * p)
                .weight(variant, baseline, 100.0 * 2.0 * p * q)
                .weight(baseline, baseline, 100.0 * q * q)
                .build();
    }

    /** Fluent construction. Weights are percentages; see {@link FounderTable}. */
    public static final class Builder {

        private final Map<AllelePair, Double> weights = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * {@code percent} of founders carry this combination. Order of the two
         * alleles does not matter ({@link AllelePair} canonicalizes), and
         * declaring the same combination twice adds the weights.
         */
        public Builder weight(Allele a, Allele b, double percent) {
            if (percent < 0.0) {
                throw new IllegalArgumentException("founder weight must be >= 0, got " + percent);
            }
            if (percent > 0.0) {
                weights.merge(new AllelePair(a, b), percent, Double::sum);
            }
            return this;
        }

        /** Both copies the same allele. */
        public Builder weight(Allele both, double percent) {
            return weight(both, both, percent);
        }

        public FounderTable build() {
            if (weights.isEmpty()) {
                throw new IllegalArgumentException("a founder table needs at least one combination with weight > 0");
            }
            double total = 0.0;
            for (double w : weights.values()) {
                total += w;
            }
            if (Math.abs(total - 100.0) > TOTAL_EPSILON) {
                LOG.log(Logger.Level.WARNING,
                        "founder weights for {0} sum to {1}, not 100 - normalising proportionally",
                        weights.keySet().iterator().next().geneKey(), total);
            }

            List<AllelePair> pairs = new ArrayList<>(weights.keySet());
            double[] shares = new double[pairs.size()];
            double[] cumulative = new double[pairs.size()];
            double running = 0.0;
            for (int i = 0; i < pairs.size(); i++) {
                shares[i] = weights.get(pairs.get(i)) / total;
                running += shares[i];
                cumulative[i] = running;
            }
            // Guard the last bucket against float drift so a draw of 0.999... lands.
            cumulative[cumulative.length - 1] = 1.0;
            return new FounderTable(List.copyOf(pairs), shares, cumulative);
        }
    }

    // ------------------------------------------------------------------
    // Use
    // ------------------------------------------------------------------

    /** One founder's combination. Consumes exactly one {@link Rng#nextFloat()}. */
    public AllelePair draw(Rng rng) {
        float roll = rng.nextFloat();
        for (int i = 0; i < cumulative.length; i++) {
            if (roll < cumulative[i]) {
                return pairs.get(i);
            }
        }
        return pairs.get(pairs.size() - 1);
    }

    /** Every combination that can turn up in the wild, in declaration order. */
    public List<AllelePair> pairs() {
        return pairs;
    }

    /**
     * The normalised share of founders carrying {@code pair}, in {@code [0, 1]}
     * - {@code 0} for a combination the table does not list. For the wiki and
     * the gene dictionary.
     */
    public double share(AllelePair pair) {
        int i = pairs.indexOf(pair);
        return i < 0 ? 0.0 : shares[i];
    }
}
