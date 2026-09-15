package com.example.horsegenetics.common.genetics.genes;

/**
 * <b>Magic heat tolerance</b> ({@code horsegenetics.magic_heat}) - better or worse in a hot biome.
 *
 * <p>Hot is the translator's {@code hot_biome} flag. The owner's call (2026-09-15) is Minecraft's hot biomes, a base
 * temperature of 1.0 and up (every desert, badlands and savanna, and the Nether), plus the jungles and the mangrove
 * swamp. See {@link AbstractClimateGene} for the shape, and {@link MagicColdGene} for its twin.
 */
public final class MagicHeatGene extends AbstractClimateGene {

    public static final String KEY = "horsegenetics.magic_heat";
    public static final int PRIORITY = 183;

    public MagicHeatGene() {
        super(KEY, PRIORITY, "Magic heat tolerance", "hot_biome", "a hot biome",
                "Htol", "Heat tolerant", "Hsen", "Heat sensitive");
    }
}
