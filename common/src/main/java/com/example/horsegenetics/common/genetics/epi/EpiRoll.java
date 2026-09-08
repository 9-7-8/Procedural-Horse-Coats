package com.example.horsegenetics.common.genetics.epi;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.HairPattern;

/**
 * Rolls a founder's {@link EpiValues} for one gene on one allele copy, honouring
 * each value's declared {@link EpiValue.Dist}.
 *
 * <p>This is the <b>only</b> place a number is invented. Everywhere else -
 * breeding, loading a record, reading a code string - values are carried
 * forward, which is what "epigenetics are tied to the allele" means. A founder
 * is a wild horse, a bred-in-the-designer horse, a debug-pen horse, or a copy a
 * splice carrot substituted; nothing else rolls.
 *
 * <h2>Distributions are declared, not flattened</h2>
 * A great deal of tuning went into the shapes these genes draw - ednrb's cover
 * is a power curve so most splash horses carry a little white and a few carry a
 * lot, and the magical stat deltas are Gaussian so a single copy is usually
 * subtle. Rolling everything uniformly would have been simpler and would have
 * quietly changed the character of the wild population on every one of those
 * genes. So the shape rides on the {@link EpiValue} and is honoured here.
 *
 * <h2>Draw order</h2>
 * Values in schema order; per value, a seed takes one {@link Rng#nextLong()}, a
 * category one {@link Rng#nextInt}, and a scalar one draw per leg - a
 * {@code nextFloat()} for uniform and power, or a {@link Rng#nextGaussian()}
 * (which is {@value Rng#GAUSSIAN_SAMPLES} floats) for a Gaussian. This matters
 * far less than it used to: values are stored by name, so re-ordering a schema
 * shifts which founders get which numbers but does not misread an existing
 * horse.
 */
public final class EpiRoll {

    private EpiRoll() {
    }

    /** A fresh set of numbers for one allele copy at one gene. */
    public static EpiValues founder(EpiSchema schema, Rng rng) {
        if (schema.isEmpty()) {
            return EpiValues.EMPTY;
        }
        double[] scalars = new double[schema.scalarWidth()];
        long[] seeds = new long[schema.size()];
        // The one cross-value dependency: the three channels of a colour come
        // out of a single hue/saturation/value draw made at component 0. See
        // EpiValue.Dist.Bright for why a founder's colour cannot just be three
        // independent numbers.
        int[] colour = new int[3];
        for (int i = 0; i < schema.size(); i++) {
            EpiValue v = schema.get(i);
            switch (v.kind()) {
                case SEED -> seeds[i] = rng.nextLong();
                case CATEGORY -> scalars[schema.scalarOffset(i)] = rng.nextInt((int) v.max());
                case SCALAR -> {
                    if (v.dist() instanceof EpiValue.Dist.Bright b) {
                        if (b.component() == 0) {
                            rollColour(b, rng, colour);
                        }
                        scalars[schema.scalarOffset(i)] = colour[b.component()];
                    } else {
                        for (int leg = 0; leg < v.arity(); leg++) {
                            scalars[schema.scalarOffset(i) + leg] = v.clamp(draw(v, rng));
                        }
                    }
                }
            }
        }
        return EpiValues.of(schema, scalars, seeds);
    }

    /** One scalar, from its own distribution. */
    private static double draw(EpiValue v, Rng rng) {
        if (v.dist() instanceof EpiValue.Dist.Gaussian g) {
            return Math.max(g.floor(), g.mean() + rng.nextGaussian() * g.sigma());
        }
        if (v.dist() instanceof EpiValue.Dist.Power p) {
            return v.min() + v.designSpan() * Math.pow(rng.nextFloat(), p.gamma());
        }
        return v.min() + v.designSpan() * rng.nextFloat();
    }

    /**
     * Hue over the whole circle, saturation and value held up inside the band
     * the gene asked for, converted once into {@code out} as {@code r, g, b}.
     */
    private static void rollColour(EpiValue.Dist.Bright b, Rng rng, int[] out) {
        double hue = rng.nextFloat();
        double sat = b.satMin() + rng.nextFloat() * (b.satMax() - b.satMin());
        double val = b.valMin() + rng.nextFloat() * (b.valMax() - b.valMin());
        int rgb = HairPattern.hsvToRgb(hue, sat, val);
        out[0] = (rgb >> 16) & 0xFF;
        out[1] = (rgb >> 8) & 0xFF;
        out[2] = rgb & 0xFF;
    }
}
