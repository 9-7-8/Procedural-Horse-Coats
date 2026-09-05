package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
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
import com.example.horsegenetics.neoforge.item.ModItems;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Chest-loot injection of {@code research_paper}s (roadmap wiki &sect;16.2). With
 * probability {@code chance} it adds one paper for a gene drawn weighted by
 * {@link Gene#rarity()} - so a common gene's paper turns up far more often than
 * a mythic one - to whatever loot the datapack JSON's conditions match (a small
 * set of chest tables).
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
        ItemStack paper = new ItemStack(ModItems.RESEARCH_PAPER.get());
        paper.set(ModDataComponents.RESEARCH_GENE.get(), gene.key());
        loot.add(paper);
        return loot;
    }

    private static Gene weightedGene(RandomSource rng) {
        List<Gene> pool = new ArrayList<>();
        int total = 0;
        for (Gene g : Genes.codeOrder()) {
            if (g.hasMagicCarrot()) {
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
