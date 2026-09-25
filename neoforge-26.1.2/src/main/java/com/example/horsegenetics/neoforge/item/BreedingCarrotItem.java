package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.common.genetics.SpliceCategory;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * <b>A breeding carrot, which says what it will do.</b>
 *
 * <h2>Why this item exists</h2>
 * Every carrot in the family used to be a plain {@code Item}, and a
 * {@code known_gene_splice_carrot} therefore <b>looked identical whatever pair it
 * carried</b>. That was tolerable while a carrot's pair was a fixed convention
 * per gene; now that a paper names the pair, a player can hold two carrots for
 * the same locus that do different things, and an inventory that cannot tell them
 * apart is not a mechanic, it is a guessing game. The carrot has always carried
 * the right data and has never shown it.
 *
 * <p>The tooltip is built from {@link CarrotEffect} tokens, so a combination
 * carrot ({@code CarrotCombineRecipe}) lists every effect it merged and the four
 * base carrots each name themselves. Nothing here is per item: the same class
 * serves all eleven, and the token list is the only thing it reads.
 */
public class BreedingCarrotItem extends Item {

    @SuppressWarnings("deprecation")
    public BreedingCarrotItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        List<String> tokens = stack.get(ModDataComponents.CARROT_EFFECTS.get());
        if (tokens == null || tokens.isEmpty()) {
            // Deliberately silent rather than "does nothing": a carrot with no
            // effects is the creative-tab copy, and labelling it broken is worse
            // than labelling it nothing.
            return;
        }
        for (CarrotEffect effect : CarrotEffect.parseList(tokens)) {
            Component line = describe(effect);
            if (line != null) {
                adder.accept(line);
            }
        }
    }

    /** One line per effect, or {@code null} for a token this build cannot read. */
    private static Component describe(CarrotEffect effect) {
        if (effect instanceof CarrotEffect.KnownGeneSplice known) {
            ResearchTopic topic = new ResearchTopic(known.geneKey(), known.alleleA(), known.alleleB());
            Gene gene = topic.gene();
            if (gene == null) {
                // A carrot for a gene this build no longer has is inert (see
                // CarrotEffect.fold), and the tooltip says so rather than
                // pretending: an item that silently does nothing is the failure
                // this tooltip exists to prevent.
                return Component.literal(known.geneKey() + " (not in this build)")
                        .withStyle(ChatFormatting.DARK_RED);
            }
            return Component.literal(topic.label() + "  ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(topic.zygosity()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (effect instanceof CarrotEffect.GeneSplice splice) {
            SpliceCategory category = splice.category();
            String what = category == SpliceCategory.ANY
                    ? "one random gene"
                    : "one random " + category.id().replace('_', ' ') + " gene";
            return Component.literal(what).withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        if (effect instanceof CarrotEffect.EpigeneticSplice) {
            return Component.literal("re-rolls every inherited epigenome")
                    .withStyle(ChatFormatting.AQUA);
        }
        if (effect instanceof CarrotEffect.Stabilizer) {
            return Component.literal("passes on the dominant copy").withStyle(ChatFormatting.AQUA);
        }
        if (effect instanceof CarrotEffect.Magnifier) {
            return Component.literal("passes on the recessive copy").withStyle(ChatFormatting.AQUA);
        }
        return null;
    }
}
