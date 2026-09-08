package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * <b>The epigenetic numbers a breed pins on its founders</b>, per gene, per
 * named value - the general form of the thing {@link BreedStatCurve} does for
 * the four body-stat axes.
 *
 * <p>A gene's alleles carry literal named numbers ({@code Gene.epiSchema()}):
 * how much white ednrb's splash covers, how dark shade renders, how wide a
 * dorsal stripe is. Those are exactly the differences that make one registry's
 * horses recognisable at a glance, and until now a breed could not say anything
 * about them - it could only pick allele pairs, which is the difference between
 * "Friesians are black" (a pair) and "Friesians are <i>deeply</i> black" (a
 * number).
 *
 * <p>So a breed may name a closed band for any value of any gene:
 * <pre>
 *   "bands": { "horsegenetics.shade": { "depth": [0.80, 1.00] } }
 * </pre>
 * and {@link BreedFounder} writes a point inside it onto <b>both allele
 * copies</b> of that founder. From there it is ordinary epigenetics: it
 * inherits, it drifts, and nothing pulls it back. A breed shapes the horses it
 * starts with and then lets go - the same contract {@code BreedStatTargets}
 * documents, for the same reason.
 *
 * <h2>What it will not touch</h2>
 * <ul>
 *   <li><b>The four body-stat genes.</b> Those are owned by the breed's
 *       {@code stats} block, which knows about scores, hands, and which allele
 *       is the pushing one. A band naming one of them is dropped with a warning
 *       rather than silently fighting it.</li>
 *   <li><b>Seeds and categories.</b> A {@link EpiValue.Kind#SEED} is the long
 *       behind a noise field and a {@link EpiValue.Kind#CATEGORY} is an index
 *       into a list - neither has a "slightly bigger", so a band over one means
 *       nothing. Only {@link EpiValue.Kind#SCALAR}s are banded.</li>
 *   <li><b>A value the gene does not declare.</b> Dropped with a warning, the
 *       same way an unknown gene is - a breed written against someone else's
 *       gene pack must still load on an install that does not have it.</li>
 * </ul>
 *
 * <p>A value with {@link EpiValue#arity()} above one (a per-leg number) has the
 * band applied to <b>every</b> leg, each drawn separately, so a breed can ask
 * for "some white on each foot" without asking for four identical socks.
 */
public final class BreedBands {

    /** A breed that pins no numbers - the common case. */
    public static final BreedBands NONE = new BreedBands(Map.of());

    /** One closed range, and the point of it a founder is given. */
    public record Band(double lo, double hi) {

        public Band {
            if (hi < lo) {
                double t = lo;
                lo = hi;
                hi = t;
            }
        }

        /** {@code u} clamped to {@code [0,1]}; {@code lerp(0)==lo}, {@code lerp(1)==hi}. */
        public double lerp(double u) {
            double c = u < 0.0 ? 0.0 : (u > 1.0 ? 1.0 : u);
            return lo + (hi - lo) * c;
        }
    }

    private final Map<String, Map<String, Band>> byGene;

    private BreedBands(Map<String, Map<String, Band>> byGene) {
        Map<String, Map<String, Band>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Band>> e : byGene.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())));
        }
        this.byGene = Collections.unmodifiableMap(copy);
    }

    public boolean isEmpty() {
        return byGene.isEmpty();
    }

    /** The gene keys this pins something on, in declaration order. */
    public Set<String> genes() {
        return byGene.keySet();
    }

    /** The value name to band map for one gene, or an empty map. */
    public Map<String, Band> forGene(String geneKey) {
        Map<String, Band> m = byGene.get(geneKey);
        return m == null ? Map.of() : m;
    }

    public Map<String, Band> forGene(Gene gene) {
        return forGene(gene.key());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, Map<String, Band>> bands = new LinkedHashMap<>();

        public Builder band(String geneKey, String valueName, double lo, double hi) {
            bands.computeIfAbsent(geneKey, k -> new LinkedHashMap<>())
                    .put(valueName, new Band(lo, hi));
            return this;
        }

        public BreedBands build() {
            return bands.isEmpty() ? NONE : new BreedBands(bands);
        }
    }
}
