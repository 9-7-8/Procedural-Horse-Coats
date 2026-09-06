package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.server.recipe.RarityItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The canonical 3x3 layout of a gene's <b>Known Gene Splice carrot</b> recipe,
 * for the Horse Browser's &ldquo;View splice recipe&rdquo; button - what
 * {@link com.example.horsegenetics.neoforge.server.recipe.KnownGeneSpliceRecipe}
 * needs, one representative stack per ingredient, in grid-slot order.
 *
 * <p>Client-safe: it only names vanilla items and this mod's own, so the screen
 * can build the same list the server fills the grid from.
 */
public final class SpliceRecipeDisplay {

    private SpliceRecipeDisplay() {
    }

    /** Nine stacks in grid order (row-major); trailing slots are {@link ItemStack#EMPTY}. */
    public static List<ItemStack> forGene(Gene gene) {
        List<ItemStack> out = new ArrayList<>(9);
        out.add(new ItemStack(Items.GOLDEN_CARROT));
        out.add(researchPaper(gene));
        out.add(new ItemStack(ModItems.HORSE_HAIR.get()));
        out.add(new ItemStack(RarityItems.forRarity(gene.rarity())));
        out.add(new ItemStack(Items.SUGAR)); // one representative "flavour"
        while (out.size() < 9) {
            out.add(ItemStack.EMPTY);
        }
        return out;
    }

    /** A {@code research_paper} stack tagged for {@code gene}. */
    public static ItemStack researchPaper(Gene gene) {
        ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
        paper.set(ModDataComponents.RESEARCH_GENE.get(), gene.key());
        return paper;
    }
}
