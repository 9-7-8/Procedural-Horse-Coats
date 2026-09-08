package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Knob;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Value;

import java.util.ArrayList;
import java.util.List;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

/**
 * One horse's draw of a {@link GeneSpec}'s {@link Knob}s - the bridge between
 * "the spec says this number varies" and an actual number to paint with.
 *
 * <p>Every knob is <b>stored</b> on the allele copy that expresses, and read
 * back by name. That is the whole determinism contract in one sentence: the
 * same copy always has the same numbers, so a horse's coat rebuilds identically
 * next session and a foal that inherits the copy inherits the look.
 *
 * <p>A knob's name is its identity, so <b>adding or re-ordering knobs no longer
 * reshuffles every knob after it</b> - which used to be the main hazard of
 * iterating on a gene in the creator. Renaming one still orphans it: it reads as
 * a fresh roll on the next parse.
 *
 * <p>{@link Knob} is the gene file format's own name for what
 * {@link EpiValue} is in Java, and {@link #schema} is the one-line translation
 * between them - which is why a data-driven gene needed almost no work to move
 * onto stored values.
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

    /**
     * This spec's knobs as an {@link EpiSchema} - what {@code SpecGene} returns
     * from {@code Gene.epiSchema()}, and the whole of the translation between
     * the gene file format and stored epigenetics.
     *
     * <p>A {@code perLeg} knob becomes a four-arity value, and its
     * {@code spread} is folded into the declared range rather than applied
     * around a hidden shared base. Each leg is then an independent stored number
     * a player can edit one at a time, which is the point of the exercise.
     */
    public static EpiSchema schema(GeneSpec spec) {
        List<EpiValue> values = new ArrayList<>(spec.knobs().size());
        for (Knob knob : spec.knobs()) {
            if (knob.seed()) {
                values.add(EpiValue.seed(knob.name()));
            } else if (knob.perLeg()) {
                values.add(EpiValue.perLeg(knob.name(),
                        knob.min() * (1.0 - knob.spread()), knob.max() * (1.0 + knob.spread())));
            } else {
                values.add(EpiValue.uniform(knob.name(), knob.min(), knob.max()));
            }
        }
        return EpiSchema.of(values);
    }

    /**
     * Read every knob off {@code epi}. {@code dose} is how many variant copies
     * the horse carries.
     */
    public static SpecValues read(GeneSpec spec, EpiValues epi, int dose) {
        int n = spec.knobs().size();
        double[][] ranges = new double[n][];
        long[] seeds = new long[n];
        for (int i = 0; i < n; i++) {
            Knob knob = spec.knobs().get(i);
            if (knob.seed()) {
                seeds[i] = epi.seed(knob.name());
                continue;
            }
            double[] drawn = new double[knob.perLeg() ? LEG_COUNT : 1];
            for (int leg = 0; leg < drawn.length; leg++) {
                drawn[leg] = epi.get(knob.name(), leg);
            }
            ranges[i] = drawn;
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
