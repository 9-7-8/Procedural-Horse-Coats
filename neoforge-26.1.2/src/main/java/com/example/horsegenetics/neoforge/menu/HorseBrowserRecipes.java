package com.example.horsegenetics.neoforge.menu;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.neoforge.data.GeneDatabaseData;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.server.recipe.CarrotCombineRecipe;
import com.example.horsegenetics.neoforge.server.recipe.KnownGeneSpliceRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;

/**
 * The <b>only</b> things the Horse Browser's Crafting tab can make - this mod's
 * own recipes, and nothing else. There is no vanilla recipe lookup: the grid
 * is a private crafting surface for the gene-paper &rarr; splice-carrot chain.
 *
 * <ol>
 *   <li><b>Gene paper</b> - a single book in the grid plus a gene picked in the
 *       list becomes a {@code research_paper} for that gene, if the player has
 *       discovered it (creative sees every gene). This is the same paper the
 *       "Write research paper" button makes, minus the button.</li>
 *   <li><b>Known Gene Splice carrot</b> - the paper-parameterised
 *       {@link KnownGeneSpliceRecipe}.</li>
 *   <li><b>Combining carrots</b> - {@link CarrotCombineRecipe}.</li>
 * </ol>
 *
 * <p>Both custom recipes are stateless {@code CustomRecipe}s, so a single shared
 * instance of each is enough.
 */
public final class HorseBrowserRecipes {

    private static final KnownGeneSpliceRecipe KNOWN_GENE_SPLICE = new KnownGeneSpliceRecipe();
    private static final CarrotCombineRecipe CARROT_COMBINE = new CarrotCombineRecipe();

    private HorseBrowserRecipes() {
    }

    /** The output for the current grid, or {@link ItemStack#EMPTY}. Server-authoritative. */
    public static ItemStack resultFor(CraftingInput input, Player player, String selectedGeneKey) {
        ItemStack paper = genePaper(input, player, selectedGeneKey);
        if (!paper.isEmpty()) {
            return paper;
        }
        Level level = player.level();
        if (KNOWN_GENE_SPLICE.matches(input, level)) {
            return KNOWN_GENE_SPLICE.assemble(input);
        }
        if (CARROT_COMBINE.matches(input, level)) {
            return CARROT_COMBINE.assemble(input);
        }
        return ItemStack.EMPTY;
    }

    /** Consume the inputs a successful {@link #resultFor} used - one of each, shapeless-style. */
    public static void consume(CraftingContainer craft, Player player, String selectedGeneKey) {
        CraftingInput input = craft.asCraftInput();
        int bookSlot = genePaperBookSlot(input, player, selectedGeneKey);
        if (bookSlot >= 0) {
            craft.removeItem(bookSlot, 1);
            return;
        }
        for (int i = 0; i < craft.getContainerSize(); i++) {
            if (!craft.getItem(i).isEmpty()) {
                craft.removeItem(i, 1);
            }
        }
    }

    // ------------------------------------------------------------------
    // Gene paper: exactly one book + a discovered, carrot-eligible gene
    // ------------------------------------------------------------------

    private static ItemStack genePaper(CraftingInput input, Player player, String selectedGeneKey) {
        Gene gene = eligibleGene(input, player, selectedGeneKey);
        if (gene == null) {
            return ItemStack.EMPTY;
        }
        ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
        paper.set(ModDataComponents.RESEARCH_GENE.get(), gene.key());
        return paper;
    }

    private static int genePaperBookSlot(CraftingInput input, Player player, String selectedGeneKey) {
        if (eligibleGene(input, player, selectedGeneKey) == null) {
            return -1;
        }
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).is(Items.BOOK)) {
                return i;
            }
        }
        return -1;
    }

    /** The gene a book in this grid would document, or {@code null} if the grid isn't "just a book". */
    private static Gene eligibleGene(CraftingInput input, Player player, String selectedGeneKey) {
        if (selectedGeneKey == null || selectedGeneKey.isEmpty()) {
            return null;
        }
        int books = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (!s.is(Items.BOOK)) {
                return null; // anything else in the grid disqualifies the paper craft
            }
            books += s.getCount();
        }
        if (books != 1) {
            return null;
        }
        Gene gene = Genes.byKeyOrNull(selectedGeneKey);
        if (gene == null || !gene.hasGeneCarrot()) {
            return null;
        }
        return discovered(player, gene) ? gene : null;
    }

    private static boolean discovered(Player player, Gene gene) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        return player.level() instanceof ServerLevel level
                && GeneDatabaseData.get(level.getServer()).knows(player.getUUID(), gene.key());
    }
}
