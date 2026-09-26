package com.example.horsegenetics.common.genetics;

import java.util.Locale;

/**
 * How rare a gene is - the one axis the whole gameplay economy sorts on
 * (roadmap wiki &sect;19): research-paper loot weighting, how long the Equine
 * Research Shelf takes to copy a paper, villager stock and pools, random jars.
 *
 * <p>Six tiers, in ascending rarity. <b>A tier is not a price.</b> It used to
 * map to a rarity item the gene-carrot recipe charged - iron through nether
 * star - and that is retired (owner's call; see {@code KnownGeneSpliceRecipe}):
 * the carrot costs a paper, a golden carrot and a hair whatever the locus is,
 * and the tier prices the <i>paper</i> instead, by making it rarer to find and
 * slower to copy. Rarity therefore says how hard a gene is to <b>learn</b>, and
 * never how hard it is to spend once learnt.
 *
 * <p>A gene that declares nothing is {@link #DEFAULT} (settled, &sect;21).
 */
public enum GeneRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    MYTHIC;

    /** The tier a gene falls into when it declares no rarity. */
    public static final GeneRarity DEFAULT = UNCOMMON;

    /**
     * A relative loot / stock weight - commoner genes turn up more often. Steep
     * enough that a mythic gene is a real find, shallow enough that every gene
     * is reachable.
     */
    public int lootWeight() {
        return switch (this) {
            case COMMON -> 60;
            case UNCOMMON -> 30;
            case RARE -> 15;
            case EPIC -> 7;
            case LEGENDARY -> 3;
            case MYTHIC -> 1;
        };
    }

    /** Case-insensitive; an unknown or blank string falls back to {@link #DEFAULT}. */
    public static GeneRarity fromString(String s) {
        if (s == null || s.isBlank()) {
            return DEFAULT;
        }
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}
