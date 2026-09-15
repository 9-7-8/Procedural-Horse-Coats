package com.example.horsegenetics.common.genetics.genes;

/**
 * <b>Magic cold tolerance</b> ({@code horsegenetics.magic_cold}) - better or worse in a cold biome.
 *
 * <p>Cold is the translator's {@code cold_biome} flag. The owner's rule (2026-09-15): anything with snow is cold, so it
 * is wherever snow can fall at the horse's own position, a high peak in a temperate biome included. See
 * {@link AbstractClimateGene} for the shape, and {@link MagicHeatGene} for its twin.
 */
public final class MagicColdGene extends AbstractClimateGene {

    public static final String KEY = "horsegenetics.magic_cold";
    public static final int PRIORITY = 184;

    public MagicColdGene() {
        super(KEY, PRIORITY, "Magic cold tolerance", "cold_biome", "a cold biome",
                "Ctol", "Cold tolerant", "Csen", "Cold sensitive");
    }
}
