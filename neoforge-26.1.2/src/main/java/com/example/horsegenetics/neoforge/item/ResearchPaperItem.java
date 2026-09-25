package com.example.horsegenetics.neoforge.item;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A <b>research paper</b>: one gene and <b>one allele pair</b>
 * ({@link ResearchTopic}), on a {@code research_gene} component. It is the
 * ingredient the paper-parameterised {@code KnownGeneSpliceRecipe} matches on,
 * and the pair it names is the pair the carrot grants.
 *
 * <h2>A pair, not a gene</h2>
 * A paper used to name a whole locus, and the carrot built from it took whatever
 * pair convention had picked - the first-declared allele, het or hom per
 * {@code Gene.geneCarrotHomozygous()}. That is the wrong unit twice over: a
 * breeder is asking "what does {@code A/a} do here", and a forty-allele locus
 * could only ever be spliced to allele number one. Now the paper says which two,
 * so {@code Particle: Sflm/Gflm} is a thing a player can hold.
 *
 * <p>Three sources: chest loot ({@code AddResearchPaperModifier}), the
 * equestrian supplier, and writing one off a live horse with a book
 * ({@code server/GeneBookFromHorse}) - which is the only route that can hand you
 * a compound pair, because it copies down what the animal actually carries.
 */
public class ResearchPaperItem extends Item {

    @SuppressWarnings("deprecation")
    public ResearchPaperItem(Properties properties) {
        super(properties);
    }

    /** The pair a stack documents, or {@code null} if the component is missing. */
    public static ResearchTopic topicOf(ItemStack stack) {
        return stack.get(ModDataComponents.RESEARCH_TOPIC.get());
    }

    /** The gene a stack documents, or {@code null} if the component is missing / unknown. */
    public static Gene geneOf(ItemStack stack) {
        ResearchTopic topic = topicOf(stack);
        return topic == null ? null : topic.gene();
    }

    /** Write {@code topic} onto a fresh paper - the one place a paper is built. */
    public static ItemStack of(ResearchTopic topic) {
        ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
        paper.set(ModDataComponents.RESEARCH_TOPIC.get(), topic);
        return paper;
    }

    // A research paper is not something you read, and right-clicking one does
    // nothing at all.
    //
    // It used to add its gene to your database and unlock that gene's carrot,
    // which made a paper a shortcut past the horses: find one in a chest, read
    // it, and you knew a gene you had never met. The database (GeneDatabaseData)
    // is now earned only by keeping a living example - tamed, bred, or owned any
    // other way - so a paper is a *component* rather than a lesson. It goes in a
    // shelf to be copied, and it goes in a gene carrot to be spent.
    //
    // Deliberately no "you cannot read this" message: an item that says nothing
    // when clicked reads as an ingredient, and one that refuses reads as broken.

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        ResearchTopic topic = topicOf(stack);
        Gene gene = topic == null ? null : topic.gene();
        if (gene == null) {
            adder.accept(Component.literal("Blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        // The pair leads, because it is what makes two papers for one gene
        // different items. The rarity beside it is the gene's, and prices the
        // carrot; the zygosity is what a breeder reads it as.
        adder.accept(Component.literal(topic.label() + "  ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(gene.rarity().name().toLowerCase())
                        .withStyle(ChatFormatting.GRAY)));
        adder.accept(Component.literal(topic.zygosity()).withStyle(ChatFormatting.DARK_GRAY));

        // What this pair actually does, not what the locus is about in general -
        // Expression is the per-combination sentence the wiki and the info panel
        // already use, and it is the whole reason a pair is the better unit.
        AllelePair pair = topic.pair();
        Expression expression = pair == null ? null : gene.expressionOf(pair);
        String blurb = expression == null ? gene.description() : expression.name();
        if (blurb != null && !blurb.isBlank()) {
            adder.accept(Component.literal(blurb).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
