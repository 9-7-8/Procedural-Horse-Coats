package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.ResearchPaperItem;
import com.example.horsegenetics.neoforge.server.recipe.RarityItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The canonical 3x3 layout of a gene's <b>Known Gene Splice carrot</b> recipe,
 * shown as ghosts in the Horse Browser's Crafting grid when a gene is selected -
 * exactly what {@link com.example.horsegenetics.neoforge.server.recipe.KnownGeneSpliceRecipe}
 * requires, in grid-slot order.
 *
 * <p>The default recipe is four items and nothing else: horse hair, this
 * gene's research paper, a golden carrot, and the rarity ingot (gold at the
 * default tier).
 *
 * <p>Client-safe: it only names vanilla items and this mod's own.
 */
public final class SpliceRecipeDisplay {

    private SpliceRecipeDisplay() {
    }

    /** Nine stacks in grid order (row-major); trailing slots are {@link ItemStack#EMPTY}. */
    public static List<ItemStack> forGene(Gene gene) {
        List<ItemStack> out = new ArrayList<>(9);
        out.add(new ItemStack(ModItems.HORSE_HAIR.get()));
        out.add(researchPaper(gene));
        out.add(new ItemStack(Items.GOLDEN_CARROT));
        out.add(new ItemStack(RarityItems.forRarity(gene.rarity())));
        while (out.size() < 9) {
            out.add(ItemStack.EMPTY);
        }
        return out;
    }

    /**
     * A {@code research_paper} stack for {@code gene} - an <b>example</b> one,
     * since a real paper names a pair and this display has only a gene. It shows
     * the pair {@link ResearchTopic#defaultFor} picks, which is the gene's own
     * {@code geneCarrotHomozygous()} convention; the recipe itself takes any pair.
     */
    public static ItemStack researchPaper(Gene gene) {
        return ResearchPaperItem.of(ResearchTopic.defaultFor(gene));
    }
}
