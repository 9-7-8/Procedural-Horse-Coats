package com.example.horsegenetics.common.breed;

/**
 * How often a breed turns up among wild herds, from the breed sheet's
 * "Commonness" column. The {@link #weight} is the relative pull each breed has
 * when a herd is rolled for a biome it belongs to - a doubling per step.
 */
public enum Commonness {
    EXTREMELY_COMMON(48.0),
    VERY_COMMON(24.0),
    COMMON(12.0),
    MODERATE(6.0),
    UNCOMMON(3.0),
    RARE(1.5),
    VERY_RARE(0.75);

    public final double weight;

    Commonness(double weight) {
        this.weight = weight;
    }

    /**
     * The tier whose {@link #weight} is closest to {@code spawnWeight} - the
     * inverse of the builder's {@code commonness(...)} call, for the things that
     * want to sort breeds by rarity rather than draw them.
     *
     * <p>{@link Breed} stores only the weight, because that is the only form
     * the herd roll ever needs. Anything that wants the <i>tier</i> back - the
     * cowboy pricing his horses, a UI grouping a breed list - asks here rather
     * than each inventing its own thresholds.
     */
    public static Commonness forWeight(double spawnWeight) {
        Commonness best = MODERATE;
        double bestGap = Double.MAX_VALUE;
        for (Commonness c : values()) {
            double gap = Math.abs(c.weight - spawnWeight);
            if (gap < bestGap) {
                bestGap = gap;
                best = c;
            }
        }
        return best;
    }
}
