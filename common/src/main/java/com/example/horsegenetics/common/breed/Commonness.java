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
}
