package com.example.horsegenetics.neoforge.data.loot;

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
 *   "max_rarity": "uncommon" }   // optional, default epic
 * </pre>
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
        /** A {@code research_paper}: writes {@code research_gene}. */
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
                    RARITY_CODEC.optionalFieldOf("max_rarity", GeneRarity.EPIC).forGetter(f -> f.maxRarity)
            )).apply(i, SetRandomGeneFunction::new));

    private final Which target;
    private final GeneRarity minRarity;
    private final GeneRarity maxRarity;

    protected SetRandomGeneFunction(List<LootItemCondition> conditions,
                                    Which target, GeneRarity minRarity, GeneRarity maxRarity) {
        super(conditions);
        this.target = target;
        this.minRarity = minRarity;
        this.maxRarity = maxRarity;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        Gene gene = GenePool.draw(context.getRandom(), minRarity, maxRarity);
        if (gene == null) {
            return ItemStack.EMPTY;
        }
        switch (target) {
            case PAPER -> stack.set(ModDataComponents.RESEARCH_GENE.get(), gene.key());
            case CARROT -> stack.set(ModDataComponents.CARROT_EFFECTS.get(), List.of(
                    com.example.horsegenetics.common.genetics.CarrotEffect
                            .defaultSpliceFor(gene).id()));
        }
        return stack;
    }

    @Override
    public MapCodec<? extends LootItemConditionalFunction> codec() {
        return MAP_CODEC;
    }
}
