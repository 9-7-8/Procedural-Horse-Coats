package com.example.horsegenetics.neoforge.server.recipe;

import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.ResearchTopic;
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
 * golden carrot + a {@code research_paper} + a hair item &rarr; a
 * {@code known_gene_splice_carrot} carrying the {@code known:<gene>:<a>:<b>}
 * effect for <b>the pair that paper names</b>.
 *
 * <p>One recipe rather than N generated per-gene recipes, so a drop-in gene
 * file gets its carrot the moment it registers - no datapack. The recipe reads
 * the pair off the paper at craft time, which is the point of a paper being a
 * pair: two papers for one locus craft two different carrots, and a wide locus
 * is reachable at every allele rather than only its first.
 *
 * <h2>The rarity tier used to be charged here, and no longer is</h2>
 * A fourth slot took the ingot for the gene's {@code GeneRarity} - iron up to a
 * nether star. Owner's call: it priced the carrot out of reach at the top of
 * the ladder, where a mythic locus wanted a nether star <i>per carrot</i> and
 * the natural use is two of them, one per parent. The tier still prices the
 * paper - it weights what drops in a chest and how long the Equine Research
 * Shelf takes to copy one - so a rare gene is still work; the work is finding
 * the paper, not farming a boss for every carrot cut from it.
 */
public class KnownGeneSpliceRecipe extends CustomRecipe {

    /**
     * One instance, shared by both codecs. See
     * {@link CarrotCombineRecipe#INSTANCE} for why a second one is a login kick
     * on every dedicated server rather than a recipe that does not work.
     */
    public static final KnownGeneSpliceRecipe INSTANCE = new KnownGeneSpliceRecipe();

    public static final MapCodec<KnownGeneSpliceRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, KnownGeneSpliceRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<KnownGeneSpliceRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /**
     * Hair cloth used to be accepted here as well, as a way of paying the same
     * cost in one slot. The cloth no longer exists, so hair is the only answer.
     */
    private static boolean isHair(ItemStack s) {
        return s.is(ModItems.HORSE_HAIR.get());
    }

    /** The pair the paper in this grid documents, if the grid is otherwise a valid gene-carrot craft. */
    private static ResearchTopic resolve(CraftingInput input) {
        ResearchTopic topic = null;
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
                topic = ResearchPaperItem.topicOf(s);
            } else if (isHair(s)) {
                hair++;
            }
        }
        // isResolved() is the load-bearing half: a paper naming a gene this build
        // retired, or a pair the locus says cannot occur, would craft a carrot
        // that does nothing when fed. Refusing to resolve is visible; an inert
        // carrot is not.
        Gene gene = topic == null ? null : topic.gene();
        if (gold != 1 || paper != 1 || hair != 1
                || gene == null || !gene.hasGeneCarrot() || !topic.isResolved()) {
            return null;
        }
        // The recipe is exactly three items: golden carrot + this gene's paper
        // + a hair item. Nothing else in the grid.
        if (filled != 3) {
            return null;
        }
        return topic;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return resolve(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ResearchTopic topic = resolve(input);
        CarrotEffect splice = topic == null ? null : topic.splice();
        if (splice == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = new ItemStack(ModItems.KNOWN_GENE_SPLICE_CARROT.get());
        // Built through CarrotEffect rather than by concatenating a token, so
        // the shape of that token lives in exactly one place. It used to be
        // spelled out here and in SetRandomGeneFunction, and when the token
        // gained the allele names both spellings compiled and silently stopped
        // parsing.
        out.set(ModDataComponents.CARROT_EFFECTS.get(), List.of(splice.id()));
        return out;
    }

    @Override
    public RecipeSerializer<KnownGeneSpliceRecipe> getSerializer() {
        return SERIALIZER;
    }
}
