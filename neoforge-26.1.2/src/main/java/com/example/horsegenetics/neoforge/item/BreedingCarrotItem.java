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
 *
 * <h2>HOW TO SPAWN ONE FOR A PARTICULAR GENE</h2>
 * Ten of the eleven carrots are plain items and {@code /give} takes them by id:
 *
 * <pre>
 * /give &#64;s horsegenetics:unknown_gene_splice_carrot
 * /give &#64;s horsegenetics:magical_gene_splice_carrot
 * /give &#64;s horsegenetics:stabilizer_carrot
 * </pre>
 *
 * <p>The eleventh - {@code known_gene_splice_carrot} - is <b>one item
 * parameterised by a component</b>, so its gene is not in its id. Spawning a
 * specific one by hand means writing the component yourself:
 *
 * <pre>
 * /give &#64;s horsegenetics:known_gene_splice_carrot[horsegenetics:carrot_effects=["known:horsegenetics.flying:Tf:Tf"]]
 * </pre>
 *
 * <p>The token is <b>{@code known:<geneKey>:<alleleA>:<alleleB>}</b>
 * ({@link CarrotEffect.KnownGeneSplice#id()}), and each part has to be exact:
 *
 * <ul>
 *   <li><b>{@code geneKey}</b> is {@link Gene#key()} in full, namespace
 *       included - {@code horsegenetics.flying}, {@code horsegenetics.kit}.</li>
 *   <li><b>the alleles</b> are the {@code token} strings from the gene's own
 *       file, case-sensitive: {@code Tf}, {@code El}, {@code n}. Read them off
 *       {@code common/src/main/resources/horsegenetics/genes/<gene>.json}, or
 *       off the gene's wiki page.</li>
 *   <li>the two may differ ({@code X:Y} is a compound pair), and
 *       {@code X:X} versus {@code X:n} is the whole homozygous / carrier
 *       distinction - a <b>homozygous carrot fed to each parent is the only way
 *       to guarantee a homozygous foal</b>.</li>
 * </ul>
 *
 * <p><b>Prefer {@code /testkit splice <gene> [<a> <b>]}</b>
 * ({@code server/DebugTestWorldHandler}). It tab-completes the gene and both
 * alleles off the live registry and refuses a pair the locus cannot have, where
 * a mistyped {@code /give} hands over a carrot that looks perfectly normal,
 * carries a token nothing parses, and does nothing at all - which you find out
 * one breeding later. {@code /testkit paper} is the same for the research paper,
 * whose {@code research_gene} component has the same problem. Both need
 * {@code debug.tools} on in the server config.
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
