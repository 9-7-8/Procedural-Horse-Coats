package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Knob;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Value;

/**
 * One horse's draw of a {@link GeneSpec}'s {@link Knob}s - the bridge between
 * "the spec says this number varies" and an actual number to paint with.
 *
 * <p>Every knob is drawn <b>once</b>, in declaration order, from the epigenetic
 * seed of the allele copy that expresses. That is the whole determinism
 * contract in one sentence: the same copy always draws the same numbers, so a
 * horse's coat rebuilds identically next session and a foal that inherits the
 * copy inherits the look. Nothing downstream may consult the {@link Rng} again.
 *
 * <p>Draw order, so a spec's numbers stay stable when it is edited: for each
 * knob, one {@code nextLong()} if it is a seed, otherwise one
 * {@code nextFloat()} for the horse's base value, plus one more per leg if it is
 * {@code perLeg}. Adding a knob in the middle of the list <b>does</b> reshuffle
 * every knob after it - which is fine (a horse changes look when its gene is
 * re-tuned) but worth knowing while iterating in the creator.
 */
public final class SpecValues {

    /** The four legs, in {@code CoatRegions.LEGS} order. */
    public static final int LEG_COUNT = 4;

    private final double[][] ranges;
    private final long[] seeds;
    private final GeneSpec spec;
    private final int dose;

    private SpecValues(GeneSpec spec, double[][] ranges, long[] seeds, int dose) {
        this.spec = spec;
        this.ranges = ranges;
        this.seeds = seeds;
        this.dose = dose;
    }

    /** Draw every knob. {@code dose} is how many variant copies the horse carries. */
    public static SpecValues draw(GeneSpec spec, Rng rng, int dose) {
        int n = spec.knobs().size();
        double[][] ranges = new double[n][];
        long[] seeds = new long[n];
        for (int i = 0; i < n; i++) {
            Knob knob = spec.knobs().get(i);
            if (knob.seed()) {
                seeds[i] = rng.nextLong();
                continue;
            }
            double base = knob.min() + rng.nextFloat() * (knob.max() - knob.min());
            if (!knob.perLeg()) {
                ranges[i] = new double[]{base};
                continue;
            }
            double[] perLeg = new double[LEG_COUNT];
            for (int leg = 0; leg < LEG_COUNT; leg++) {
                perLeg[leg] = base * (1.0 - knob.spread() + rng.nextFloat() * knob.spread() * 2.0);
            }
            ranges[i] = perLeg;
        }
        return new SpecValues(spec, ranges, seeds, dose);
    }

    /** How many copies of the variant allele the horse carries: 0, 1 or 2. */
    public int dose() {
        return dose;
    }

    /**
     * Resolve a value for a texel on leg {@code legIndex} ({@code -1} anywhere
     * that is not a leg - a {@code perLeg} knob then reads its first leg, which
     * only matters if an author points one at the body).
     */
    public double get(Value value, int legIndex) {
        if (value instanceof Value.Const c) {
            return c.v();
        }
        if (value instanceof Value.PerDose p) {
            return switch (Math.min(2, Math.max(0, dose))) {
                case 0 -> p.zero();
                case 1 -> p.one();
                default -> p.two();
            };
        }
        int i = ((Value.FromKnob) value).index();
        double[] drawn = ranges[i];
        if (drawn == null) {
            throw new IllegalStateException("knob '" + spec.knobs().get(i).name()
                    + "' is a seed and cannot be used as a number");
        }
        int leg = legIndex < 0 ? 0 : Math.min(legIndex, drawn.length - 1);
        return drawn[leg];
    }

    public double get(Value value) {
        return get(value, -1);
    }

    /**
     * The {@code long} behind a noise field's {@code seed} parameter. An author
     * who leaves it out gets {@code fallback} - a value derived from the gene
     * key and the layer, so the pattern is stable and every gene's is different.
     */
    public long seed(Value value, long fallback) {
        if (value instanceof Value.FromKnob k && spec.knobs().get(k.index()).seed()) {
            return seeds[k.index()];
        }
        return fallback;
    }
}
