package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.CarrotEffect;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Locale;

/**
 * Loot function: <b>stamp a randomly drawn gene onto the item being produced</b>.
 * Two shapes, which is why {@link Which} exists rather than two near-identical
 * classes - the draw, the rarity window and the empty-pool handling are the
 * whole of the logic, and only the component written differs.
 *
 * <pre>
 * { "function": "horsegenetics:set_random_gene",
 *   "target": "carrot",          // or "paper"
 *   "min_rarity": "common",      // optional, default common
 *   "max_rarity": "uncommon",    // optional, default epic
 *   "homozygous": true }         // optional, default false
 * </pre>
 *
 * <h2>{@code homozygous}</h2>
 * A {@code KnownGeneSplice} names <i>both</i> alleles of the gamete rather than
 * carrying a boolean, so "the carrot that breeds true" is not a separate concept
 * - it is the pair {@code X/X} instead of {@code n/X}. On a <b>carrot</b>,
 * which of the two it gets is normally the gene's own call
 * ({@code Gene.geneCarrotHomozygous}) and this flag overrides it upward; that is
 * what the scientist's master tier sells. On a <b>paper</b> it narrows the pair
 * pool to the true-breeding half.
 *
 * <p><b>It cannot force a pair the gene forbids.</b> A locus whose homozygote is
 * lethal - or that otherwise fails {@code Gene.canOccur} - would make an inert
 * carrot, which is worse than not selling one, so such genes are skipped at the
 * draw rather than sold and silently doing nothing.
 *
 * <h2>Why a loot function and not a custom trade class</h2>
 * Villager trades are datapack registries in this version, and a trade's
 * {@code given_item_modifiers} run when the <b>offer is generated</b>. That is
 * exactly the semantics the roadmap argued for: "a random rare carrot" is
 * rolled once per restock and then sits in the villager's window at a fixed
 * price, rather than re-rolling under the player's cursor. So the whole
 * "custom listing that rolls when the offer is generated" requirement is met
 * by a stock vanilla mechanism plus this one function.
 *
 * <p>An empty pool returns an empty stack, which {@code VillagerTrade.getOffer}
 * reads as "no offer" and drops - so a rarity window with no genes in it costs
 * the villager a trade slot rather than putting a blank item on sale.
 */
public class SetRandomGeneFunction extends LootItemConditionalFunction {

    /** Which item this is filling in - they take different components. */
    public enum Which {
        /** A {@code known_gene_splice_carrot}: writes {@code carrot_effects}. */
        CARROT,
        /** A {@code research_paper}: writes {@code research_gene}, a gene and one pair. */
        PAPER;

        static final Codec<Which> CODEC = Codec.STRING.xmap(
                s -> valueOf(s.toUpperCase(Locale.ROOT)),
                w -> w.name().toLowerCase(Locale.ROOT));
    }

    private static final Codec<GeneRarity> RARITY_CODEC = Codec.STRING.xmap(
            GeneRarity::fromString, r -> r.name().toLowerCase(Locale.ROOT));

    public static final MapCodec<SetRandomGeneFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> commonFields(i).and(i.group(
                    Which.CODEC.fieldOf("target").forGetter(f -> f.target),
                    RARITY_CODEC.optionalFieldOf("min_rarity", GeneRarity.COMMON).forGetter(f -> f.minRarity),
                    RARITY_CODEC.optionalFieldOf("max_rarity", GeneRarity.EPIC).forGetter(f -> f.maxRarity),
                    Codec.BOOL.optionalFieldOf("homozygous", false).forGetter(f -> f.homozygous)
            )).apply(i, SetRandomGeneFunction::new));

    private final Which target;
    private final GeneRarity minRarity;
    private final GeneRarity maxRarity;
    private final boolean homozygous;

    protected SetRandomGeneFunction(List<LootItemCondition> conditions,
                                    Which target, GeneRarity minRarity, GeneRarity maxRarity,
                                    boolean homozygous) {
        super(conditions);
        this.target = target;
        this.minRarity = minRarity;
        this.maxRarity = maxRarity;
        this.homozygous = homozygous;
    }

    /**
     * Whether {@code gene} could hand over two copies of its variant allele.
     * Asked before the draw, not after - see {@link GenePool#draw}.
     */
    private static boolean canBreedTrue(Gene gene) {
        if (gene.alleles().isEmpty()) {
            return false;
        }
        Allele variant = gene.alleles().get(0);
        return gene.canOccur(new AllelePair(variant, variant));
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        Gene gene = GenePool.draw(context.getRandom(), minRarity, maxRarity,
                homozygous ? SetRandomGeneFunction::canBreedTrue : g -> true);
        if (gene == null) {
            return ItemStack.EMPTY;
        }
        switch (target) {
            case PAPER -> {
                // The same carrier / true-breeding pool a chest draws from, so a
                // bought paper and a found one are the same kind of thing;
                // `homozygous` narrows it to the true-breeding half.
                List<com.example.horsegenetics.common.genetics.ResearchTopic> pool = homozygous
                        ? com.example.horsegenetics.common.genetics.ResearchTopic.breedsTruePool(gene)
                        : com.example.horsegenetics.common.genetics.ResearchTopic.lootPool(gene);
                if (pool.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                stack.set(ModDataComponents.RESEARCH_TOPIC.get(),
                        pool.get(context.getRandom().nextInt(pool.size())));
            }
            case CARROT -> stack.set(ModDataComponents.CARROT_EFFECTS.get(),
                    List.of(spliceFor(gene).id()));
        }
        return stack;
    }

    /**
     * The pair the carrot names. {@code homozygous} overrides the gene's own
     * {@code geneCarrotHomozygous} call upward - it never makes a carrot
     * <i>less</i> certain than the gene would have.
     */
    private CarrotEffect spliceFor(Gene gene) {
        if (!homozygous) {
            return CarrotEffect.defaultSpliceFor(gene);
        }
        String variant = gene.alleles().get(0).token();
        return new CarrotEffect.KnownGeneSplice(gene.key(), variant, variant);
    }

    @Override
    public MapCodec<? extends LootItemConditionalFunction> codec() {
        return MAP_CODEC;
    }
}
