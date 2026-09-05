package com.example.horsegenetics.common.genetics;

import java.util.Locale;

/**
 * How rare a gene is - the one axis the whole gameplay economy sorts on
 * (roadmap wiki &sect;19): the gene-carrot recipe cost, research-paper loot
 * weighting, villager stock and pools, random jars.
 *
 * <p>Six tiers, in ascending rarity. The <b>tier &rarr; rarity-item</b> mapping
 * (iron / gold / diamond / emerald / netherite ingot / nether star) lives once
 * on the recipe side ({@code server/recipe/RarityItems}), never on the gene, so
 * retuning the economy is a one-line change and a third-party gene cannot
 * invent its own currency.
 *
 * <p>A gene that declares nothing is {@link #DEFAULT} - the <b>gold-ingot
 * tier</b> (settled, &sect;21).
 */
public enum GeneRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    MYTHIC;

    /** The tier a gene falls into when it declares no rarity - the gold-ingot tier. */
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
