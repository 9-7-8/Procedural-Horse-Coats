package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.item.ResearchPaperItem;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The <b>one parameterised gene-carrot recipe</b> (roadmap wiki &sect;14.2):
 * golden carrot + a {@code research_paper} + a hair item + the rarity item for
 * that gene's tier + at least one flavour ingredient &rarr; a
 * {@code known_gene_splice_carrot} carrying that gene's
 * {@code known:<gene>:het|hom} effect.
 *
 * <p>One recipe rather than N generated per-gene recipes, so a drop-in gene
 * file gets its carrot the moment it registers - no datapack. The recipe reads
 * the gene off the paper at craft time.
 */
public class KnownGeneSpliceRecipe extends CustomRecipe {

    public static final MapCodec<KnownGeneSpliceRecipe> MAP_CODEC = MapCodec.unit(KnownGeneSpliceRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, KnownGeneSpliceRecipe> STREAM_CODEC =
            StreamCodec.unit(new KnownGeneSpliceRecipe());
    public static final RecipeSerializer<KnownGeneSpliceRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static boolean isHair(ItemStack s) {
        return s.is(ModItems.HORSE_HAIR.get()) || s.is(ModItems.HAIR_CLOTH.get());
    }

    /** The gene the paper in this grid documents, if the grid is otherwise a valid gene-carrot craft. */
    private static Gene resolve(CraftingInput input) {
        Gene gene = null;
        int gold = 0;
        int paper = 0;
        int hair = 0;
        int filled = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            filled++;
            if (s.is(Items.GOLDEN_CARROT)) {
                gold++;
            } else if (s.getItem() instanceof ResearchPaperItem) {
                paper++;
                gene = ResearchPaperItem.geneOf(s);
            } else if (isHair(s)) {
                hair++;
            }
        }
        if (gold != 1 || paper != 1 || hair != 1 || gene == null || !gene.hasGeneCarrot()) {
            return null;
        }
        // The default recipe is exactly four items: golden carrot + this gene's
        // paper + a hair item + the gene's rarity ingot. Nothing else in the grid.
        int rarity = 0;
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).is(RarityItems.forRarity(gene.rarity()))) {
                rarity++;
            }
        }
        if (rarity != 1 || filled != 4) {
            return null;
        }
        return gene;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return resolve(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Gene gene = resolve(input);
        if (gene == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = new ItemStack(ModItems.KNOWN_GENE_SPLICE_CARROT.get());
        // Built through CarrotEffect rather than by concatenating a token, so
        // the shape of that token lives in exactly one place. It used to be
        // spelled out here and in SetRandomGeneFunction, and when the token
        // gained the allele names both spellings compiled and silently stopped
        // parsing.
        out.set(ModDataComponents.CARROT_EFFECTS.get(),
                List.of(CarrotEffect.defaultSpliceFor(gene).id()));
        return out;
    }

    @Override
    public RecipeSerializer<KnownGeneSpliceRecipe> getSerializer() {
        return SERIALIZER;
    }
}
