package com.example.horsegenetics.common.breed;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>The climate rule for breeds</b> - which breeds must carry the two climate
 * loci, and at what strength, read off where the breed spawns.
 *
 * <p>Two halves, both the owner's (2026-10-01):
 * <ul>
 *   <li>a breed that spawns in any Nether biome is heat tolerant, fixed -
 *       {@code Htol/Htol} at weight 100, the way the Rimehart is fixed for cold;</li>
 *   <li>a breed that spawns in a cold biome carries cold tolerance at least at the
 *       tier its share of cold biomes asks for - {@link #coldTarget}.</li>
 * </ul>
 * A breed already above its tier is left alone: the rule only raises. Heat for
 * hot overworld biomes is <i>not</i> part of it - those breeds keep what their
 * lore set.
 *
 * <p>Nothing at run time reads this. It is the single place the biome lists and
 * the ladder live, so that {@code BreedClimateRuleTest} can hold every shipped
 * breed to them and a new vanilla biome is added once. The breed files carry
 * the pools themselves; this only says what they must be at least.
 *
 * <p>A biome id from another mod is not classified - {@link #isClassified} -
 * and counts as neither cold nor Nether. Every shipped breed lists vanilla ids
 * only, so nothing is lost by that today.
 */
public final class BreedClimate {

    public static final String HEAT = "horsegenetics.magic_heat";
    public static final String COLD = "horsegenetics.magic_cold";

    /** Vanilla's five Nether biomes. Spawning in any one makes a breed a Nether breed. */
    public static final Set<String> NETHER_BIOMES = new HashSet<>(Arrays.asList(
            "minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:warped_forest",
            "minecraft:soul_sand_valley", "minecraft:basalt_deltas"));

    /**
     * Every vanilla biome whose base temperature is below 0.15 - the game's own
     * "snow falls here at sea level" line. Read out of the 26.1.2 sources
     * ({@code OverworldBiomes}, 2026-10-01), not from memory: snowy plains and
     * ice spikes 0.0, snowy taiga -0.5, snowy beach 0.05, snowy slopes -0.3,
     * grove -0.2, frozen and jagged peaks -0.7, frozen river and frozen ocean
     * 0.0. Deep frozen ocean is 0.5 and is not on it; neither are the biomes that
     * only snow by altitude (windswept hills 0.2, taiga 0.25), since a breed's
     * spawn list names a biome, not a mountain.
     */
    public static final Set<String> COLD_BIOMES = new HashSet<>(Arrays.asList(
            "minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga",
            "minecraft:snowy_beach", "minecraft:snowy_slopes", "minecraft:grove",
            "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:frozen_river",
            "minecraft:frozen_ocean"));

    /**
     * The cold ladder, as the expected number of {@code Ctol} copies a founder
     * carries - which is how two differently shaped pools are compared.
     */
    public enum Tier {
        NONE(0.0),
        /** {@code Ctol/n 15, n/n 85}. */
        LIGHT(0.15),
        /** {@code Ctol/Ctol 5, Ctol/n 30, n/n 65}. */
        PARTIAL(0.40),
        /** {@code Ctol/Ctol 60, Ctol/n 35, n/n 5}. */
        NEAR_FIXED(1.55);

        public final double copies;

        Tier(double copies) {
            this.copies = copies;
        }
    }

    private BreedClimate() {
    }

    /** True for a breed that spawns in any Nether biome. */
    public static boolean isNether(List<String> biomes) {
        for (String id : biomes) {
            if (NETHER_BIOMES.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the rule can say anything about this id - vanilla's only. */
    public static boolean isClassified(String biomeId) {
        return biomeId.startsWith("minecraft:");
    }

    /** The share of a breed's distinct biomes that are cold, 0 for none listed. */
    public static double coldShare(List<String> biomes) {
        Set<String> distinct = new LinkedHashSet<>(biomes);
        if (distinct.isEmpty()) {
            return 0;
        }
        int cold = 0;
        for (String id : distinct) {
            if (COLD_BIOMES.contains(id)) {
                cold++;
            }
        }
        return (double) cold / distinct.size();
    }

    /** The lowest cold tier a breed spawning in these biomes may carry. */
    public static Tier coldTarget(List<String> biomes) {
        double share = coldShare(biomes);
        if (share >= 0.50) {
            return Tier.NEAR_FIXED;
        }
        if (share >= 0.25) {
            return Tier.PARTIAL;
        }
        return share > 0 ? Tier.LIGHT : Tier.NONE;
    }

    /** Expected copies of {@code allele} per founder drawn from {@code pool}; 0 for none. */
    public static double expectedCopies(List<Breed.Combo> pool, String allele) {
        if (pool == null || pool.isEmpty()) {
            return 0;
        }
        double copies = 0;
        double total = 0;
        for (Breed.Combo c : pool) {
            int n = (allele.equals(c.a()) ? 1 : 0) + (allele.equals(c.b()) ? 1 : 0);
            copies += n * c.weight();
            total += c.weight();
        }
        return total == 0 ? 0 : copies / total;
    }
}
