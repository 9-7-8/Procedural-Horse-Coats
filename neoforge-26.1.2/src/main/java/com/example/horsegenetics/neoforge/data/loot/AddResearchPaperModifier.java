package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.ResearchTopic;
import com.example.horsegenetics.neoforge.item.ResearchPaperItem;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Chest-loot injection of {@code research_paper}s (roadmap wiki &sect;16.2). With
 * probability {@code chance} it adds one paper for a gene drawn weighted by
 * {@link Gene#rarity()} - so a common gene's paper turns up far more often than
 * a mythic one - to whatever loot the datapack JSON's conditions match. The
 * <b>pair</b> on that paper is then drawn flat out of
 * {@link ResearchTopic#lootPool}.
 *
 * <p>That is now <b>every</b> chest table in every namespace, at thirty per cent,
 * where it was eighteen across eight named vanilla chests. The owner calls these
 * "gene books", after what the research shelf copies them onto, and asked for
 * them to be much more common. There are far more papers to find than there are
 * genes - two per variant allele, not one per locus - so a rate that suits one
 * dungeon's worth of exploring still leaves most of the pool unseen.
 */
public class AddResearchPaperModifier extends LootModifier {

    public static final MapCodec<AddResearchPaperModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(
                    com.mojang.serialization.Codec.FLOAT.optionalFieldOf("chance", 0.15F)
                            .forGetter(m -> m.chance))
                    .apply(inst, AddResearchPaperModifier::new));

    private final float chance;

    public AddResearchPaperModifier(LootItemCondition[] conditions, int priority, float chance) {
        super(conditions, priority);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        RandomSource rng = context.getRandom();
        if (rng.nextFloat() >= chance) {
            return loot;
        }
        Gene gene = weightedGene(rng);
        if (gene == null) {
            return loot;
        }
        // The gene is drawn weighted by rarity; the pair inside it is drawn flat
        // out of the carrier / true-breeding pool. A chest hands over "Cream:
        // Cr/n" or "Cream: Cr/Cr", never a compound pair - see
        // ResearchTopic.lootPool for why that line is where it is.
        List<ResearchTopic> pool = ResearchTopic.lootPool(gene);
        if (pool.isEmpty()) {
            return loot;
        }
        loot.add(ResearchPaperItem.of(pool.get(rng.nextInt(pool.size()))));
        return loot;
    }

    private static Gene weightedGene(RandomSource rng) {
        List<Gene> pool = new ArrayList<>();
        int total = 0;
        for (Gene g : Genes.codeOrder()) {
            if (g.hasGeneCarrot()) {
                pool.add(g);
                total += g.rarity().lootWeight();
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        int roll = rng.nextInt(total);
        for (Gene g : pool) {
            roll -= g.rarity().lootWeight();
            if (roll < 0) {
                return g;
            }
        }
        return pool.get(pool.size() - 1);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
