package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.common.genetics.GeneRarity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The one place the <b>tier &rarr; rarity-item</b> mapping lives (roadmap wiki
 * &sect;14.2): iron / gold / diamond / emerald / netherite ingot / nether star.
 * Kept on the recipe side, off the gene, so retuning the economy is a one-line
 * change and a third-party gene cannot invent its own currency.
 */
public final class RarityItems {

    public static Item forRarity(GeneRarity rarity) {
        return switch (rarity) {
            case COMMON -> Items.IRON_INGOT;
            case UNCOMMON -> Items.GOLD_INGOT;
            case RARE -> Items.DIAMOND;
            case EPIC -> Items.EMERALD;
            case LEGENDARY -> Items.NETHERITE_INGOT;
            case MYTHIC -> Items.NETHER_STAR;
        };
    }

    private RarityItems() {
    }
}
