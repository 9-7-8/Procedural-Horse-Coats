package com.example.horsegenetics.neoforge.data.loot;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Commonness;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Locale;

/**
 * Loot function: <b>stamp a randomly drawn breed onto the item being
 * produced</b> - what turns a blank {@code breed_spawn_egg} into a Fjord's.
 *
 * <pre>
 * { "function": "horsegenetics:set_random_breed",
 *   "commonest": "common",     // optional, default extremely_common
 *   "rarest": "very_rare" }    // optional, default very_rare
 * </pre>
 *
 * <p>Same shape and same reasoning as {@code SetRandomGeneFunction}: a villager
 * trade's {@code given_item_modifiers} run when the <b>offer is generated</b>,
 * so "a random rare breed egg" is rolled once per restock and then sits in the
 * horseman's window at a fixed price, rather than re-rolling under the player's
 * cursor.
 *
 * <p>An empty window returns an empty stack, which {@code VillagerTrade.getOffer}
 * reads as "no offer" and drops - so a tier window with no breeds in it costs
 * the villager a trade slot rather than putting a blank egg on sale. That is the
 * behaviour that matters when a player has switched most breeds off.
 */
public class SetRandomBreedFunction extends LootItemConditionalFunction {

    private static final Codec<Commonness> TIER_CODEC = Codec.STRING.xmap(
            s -> Commonness.valueOf(s.toUpperCase(Locale.ROOT)),
            c -> c.name().toLowerCase(Locale.ROOT));

    public static final MapCodec<SetRandomBreedFunction> MAP_CODEC = com.mojang.serialization.codecs
            .RecordCodecBuilder.mapCodec(i -> commonFields(i).and(i.group(
                    TIER_CODEC.optionalFieldOf("commonest", Commonness.EXTREMELY_COMMON)
                            .forGetter(f -> f.commonest),
                    TIER_CODEC.optionalFieldOf("rarest", Commonness.VERY_RARE)
                            .forGetter(f -> f.rarest)
            )).apply(i, SetRandomBreedFunction::new));

    private final Commonness commonest;
    private final Commonness rarest;

    protected SetRandomBreedFunction(List<LootItemCondition> conditions,
                                     Commonness commonest, Commonness rarest) {
        super(conditions);
        this.commonest = commonest;
        this.rarest = rarest;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        Breed breed = BreedPool.draw(context.getRandom(), commonest, rarest);
        if (breed == null) {
            return ItemStack.EMPTY;
        }
        stack.set(ModDataComponents.BREED_ID.get(), breed.id());
        return stack;
    }

    @Override
    public MapCodec<? extends LootItemConditionalFunction> codec() {
        return MAP_CODEC;
    }
}
