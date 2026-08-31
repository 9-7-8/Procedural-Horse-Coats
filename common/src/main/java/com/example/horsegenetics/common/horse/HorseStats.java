package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.Rng;

/**
 * How a foal's numeric stats (movement speed, max health) come from its
 * parents. <b>Not genetic yet</b> - a placeholder until these get folded into
 * the Mendelian model. Each stat is drawn uniformly from a wide band:
 * <b>75% of the lower parent value</b> to <b>150% of the higher</b>. There is
 * deliberately <b>no upper cap</b> - a determined breeder can push a line to
 * absurd numbers over generations. {@link HorseRecord} rounds the stored
 * values up (health to a whole number, speed to 3 decimals).
 */
public final class HorseStats {

    /** Uniform roll in {@code [0.75 * min(a, b), 1.5 * max(a, b)]}. */
    public static double rollFoalStat(double parentA, double parentB, Rng rng) {
        double lo = 0.75 * Math.min(parentA, parentB);
        double hi = 1.5 * Math.max(parentA, parentB);
        return lo + rng.nextFloat() * (hi - lo);
    }

    private HorseStats() {
    }
}
