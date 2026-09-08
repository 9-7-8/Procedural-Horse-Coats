package com.example.horsegenetics.common.genetics.epi;

import com.example.horsegenetics.common.Rng;

/**
 * <b>Genetic drift</b> - the tiny nudge every epigenetic value takes as it is
 * passed from parent to foal.
 *
 * <p>Before this, inheritance was verbatim: a lineage's numbers were frozen the
 * day its founder was caught, and no amount of breeding could produce a horse
 * outside what the wild population already contained. Drift is what makes a
 * breeding project a project - a line really does slowly become its own thing,
 * and once in a great while something startling is born.
 *
 * <h2>The curve, and why it is shaped like this</h2>
 * Every scalar moves at <b>every</b> breeding. That sounds drastic until you
 * look at the magnitude: it is drawn from a two-sided exponential whose scale is
 * {@value #SCALE} of the value's design span, so
 *
 * <table>
 *   <tr><th>of every scalar passed on</th><th>moves by less than</th></tr>
 *   <tr><td>50%</td><td>0.10% of its range</td></tr>
 *   <tr><td>90%</td><td>0.35%</td></tr>
 *   <tr><td>99%</td><td>0.70%</td></tr>
 *   <tr><td>all but 1 in 10 000</td><td>1.4%</td></tr>
 *   <tr><td>all but 1 in 1 000 000</td><td>2.1%</td></tr>
 * </table>
 *
 * <p>So the overwhelming majority of foals are, to the eye and to the stat
 * readout, <b>exactly their parents</b> - "breeds true" here means
 * imperceptible, not bit-identical - while the tail stays open. A horse carries
 * on the order of a few hundred of these numbers, so roughly one foal in thirty
 * has one value that moved a visible amount, and that is the whole point.
 *
 * <p>The exponential is the shape the owner asked for in as many words: the
 * larger the drift, the exponentially less likely it is. It is generated as
 * {@code -ln(u)}, whose tail is technically unbounded but is in practice capped
 * near {@code 16.6 * scale} by the resolution of {@link Rng#nextFloat()} - the
 * same bounded-tail trade {@link Rng#nextGaussian()} makes deliberately, and for
 * the same reason: a real infinite tail eventually produces a horse the size of
 * a chunk.
 *
 * <h2>Drift does not respect the design range</h2>
 * A drifted value is held only inside {@link EpiValue#clampLo()}..
 * {@link EpiValue#clampHi()}, the hard safety bound - never inside the design
 * range founders are rolled in. That is deliberate and is the owner's call: the
 * design range is where wild horses start, not a ceiling on what a breeder can
 * reach. A line bred for three hundred generations is allowed to be somewhere no
 * wild horse has ever been.
 *
 * <h2>Seeds and categories do not nudge</h2>
 * There is no "slightly different" noise seed or "slightly different" body site
 * - nudging either replaces the thing rather than adjusting it. Both instead get
 * a {@value #REPLACE_CHANCE} chance per breeding of being re-rolled outright, so
 * a lineage keeps its exact splash shape and its particle site essentially
 * forever, and a foal that does change is a real event rather than a boundary
 * accident.
 *
 * <h2>Draw order</h2>
 * Per value in schema order: a scalar takes two draws per leg (magnitude, then
 * sign); a seed or category takes one, plus one more only when it fires.
 */
public final class EpiDrift {

    /**
     * The exponential's scale, as a fraction of a value's design span. Tuned so
     * half of all drifts are under a tenth of a percent - see the table above.
     */
    public static final double SCALE = 0.0015;

    /** Per-breeding chance that a noise seed or a category is re-rolled whole. */
    public static final double REPLACE_CHANCE = 0.001;

    private EpiDrift() {
    }

    /**
     * One inherited copy's numbers, nudged. Applied to <b>each parent's
     * contributed gamete copy independently</b>, so a foal's two copies drift
     * apart from each other as well as from their parents.
     *
     * <p>Applies to every copy whether or not the gene expresses, or is even
     * visible - drift rides on the allele, like everything else here, so a
     * recessive that surfaces ten generations later surfaces having drifted.
     */
    public static EpiValues drift(EpiValues values, Rng rng) {
        EpiSchema schema = values.schema();
        if (schema.isEmpty()) {
            return values;
        }
        double[] scalars = values.rawScalars().clone();
        long[] seeds = values.rawSeeds().clone();
        boolean moved = false;

        for (int i = 0; i < schema.size(); i++) {
            EpiValue v = schema.get(i);
            switch (v.kind()) {
                case SEED -> {
                    if (rng.nextFloat() < REPLACE_CHANCE) {
                        seeds[i] = rng.nextLong();
                        moved = true;
                    }
                }
                case CATEGORY -> {
                    if (rng.nextFloat() < REPLACE_CHANCE) {
                        scalars[schema.scalarOffset(i)] = rng.nextInt((int) v.max());
                        moved = true;
                    }
                }
                case SCALAR -> {
                    for (int leg = 0; leg < v.arity(); leg++) {
                        int at = schema.scalarOffset(i) + leg;
                        double next = v.clamp(scalars[at] + step(v.designSpan(), rng));
                        if (next != scalars[at]) {
                            scalars[at] = next;
                            moved = true;
                        }
                    }
                }
            }
        }
        return moved ? EpiValues.of(schema, scalars, seeds) : values;
    }

    /**
     * One signed exponential step. {@code 1 - nextFloat()} lands in
     * {@code (0, 1]} so the logarithm is always defined and the step is always
     * finite.
     */
    private static double step(double span, Rng rng) {
        double u = 1.0 - rng.nextFloat();
        double magnitude = SCALE * span * -Math.log(u);
        return rng.nextBoolean() ? magnitude : -magnitude;
    }
}
