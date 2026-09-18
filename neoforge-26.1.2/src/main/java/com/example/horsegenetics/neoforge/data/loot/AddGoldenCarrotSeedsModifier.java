package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.neoforge.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * <b>The second way to get golden carrot seeds</b> - the first being the
 * equestrian supplier's top rank, at thirty-eight emeralds apiece.
 *
 * <p>With probability {@code chance} it drops one to three seeds into whatever
 * chest tables the datapack JSON's conditions match. The seeds are not
 * craftable and nothing else produces them, so these two sources are the whole
 * supply and the chance is what decides how long a player waits for a crop they
 * cannot buy their way into cheaply.
 *
 * <p>Seven per cent, across eight chest tables - rare enough that finding one is
 * an event, common enough that a player who explores at all will eventually
 * meet the crop without having to save up for a villager.
 */
public class AddGoldenCarrotSeedsModifier extends LootModifier {

    public static final MapCodec<AddGoldenCarrotSeedsModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(
                    com.mojang.serialization.Codec.FLOAT.optionalFieldOf("chance", 0.07F)
                            .forGetter(m -> m.chance))
                    .apply(inst, AddGoldenCarrotSeedsModifier::new));

    private final float chance;

    public AddGoldenCarrotSeedsModifier(LootItemCondition[] conditions, int priority, float chance) {
        super(conditions, priority);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (context.getRandom().nextFloat() >= this.chance) {
            return loot;
        }
        // One to three. A single seed is a slow start on a crop that doubles
        // per harvest; three is a field by the time it matters.
        int count = 1 + context.getRandom().nextInt(3);
        loot.add(new ItemStack(ModItems.GOLDEN_CARROT_SEEDS.get(), count));
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
