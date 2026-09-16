package com.example.horsegenetics.neoforge.data.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * Loot function: <b>dye the item being produced a random colour</b> - what lets
 * the horseman stock "a red saddle" without sixteen listings and without a
 * sixteen-item registry of our own.
 *
 * <pre>
 * { "function": "horsegenetics:set_random_dye" }
 * </pre>
 *
 * <p>Same shape and same reasoning as {@link SetRandomBreedFunction}: a villager
 * trade's {@code given_item_modifiers} run when the <b>offer is generated</b>,
 * so the colour is rolled once per restock and then sits in the horseman's
 * window at a fixed price, rather than re-rolling under the player's cursor.
 *
 * <h2>Why this needs no other mod, and deliberately so</h2>
 *
 * <p>This was scoped as a compatibility patch for <b>Saturated Saddles</b>, on
 * the assumption that a coloured saddle was one of its items. It is not: that
 * datapack adds no items at all. It adds {@code minecraft:crafting_dye} recipes
 * whose result is the <b>vanilla</b> {@code minecraft:saddle} carrying the
 * <b>vanilla</b> {@code minecraft:dyed_color} component - the same component
 * leather armour has always used. There is no {@code saturated_saddles}
 * namespace to depend on.
 *
 * <p>So there is nothing to detect and nothing to gate. A dyed saddle is a
 * vanilla item in a vanilla state, and this function produces one whether or not
 * the player has that datapack installed. The same is true of
 * {@code minecraft:leather_horse_armor}, which vanilla has always let you dye.
 *
 * <h2>Do not use this on a saddle</h2>
 *
 * <p><b>Vanilla does not tint the saddle</b>, and this is settled, not guessed:
 * {@code EquipmentAssetProvider} builds the leather asset with
 * {@code Layer.leatherDyeable(...)} but builds the saddle asset with the
 * single-argument {@code new EquipmentClientInfo.Layer(...)}, whose dyeable
 * field is {@code Optional.empty()}. So {@code dyed_color} on a saddle is set,
 * correct, and <i>invisible</i> - the tinted saddle model is supplied by
 * Saturated Saddles' <i>resource pack</i>, not by its datapack and not by
 * vanilla.
 *
 * <p>A dyed-saddle trade was written and then withdrawn for exactly that reason:
 * it sold a fourteen-emerald saddle indistinguishable from the eight-emerald one
 * unless the player happened to own that resource pack. If a coloured saddle is
 * ever wanted, the mod has to ship the dyeable equipment asset itself.
 *
 * <p><b>Leather horse armour is tinted by vanilla</b> and is the right target -
 * it is the two-layer leather pattern, a tinted base plus an untinted overlay.
 */
public class SetRandomDyeFunction extends LootItemConditionalFunction {

    public static final MapCodec<SetRandomDyeFunction> MAP_CODEC = com.mojang.serialization.codecs
            .RecordCodecBuilder.mapCodec(i -> commonFields(i).apply(i, SetRandomDyeFunction::new));

    protected SetRandomDyeFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        RandomSource random = context.getRandom();
        // The sixteen dyes rather than a free RGB roll: the point is an item that
        // looks like one somebody dyed, and a random 24-bit colour mostly lands on
        // muddy shades no dye can make.
        DyeColor colour = DyeColor.values()[random.nextInt(DyeColor.values().length)];
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(colour.getTextureDiffuseColor()));
        return stack;
    }

    @Override
    public MapCodec<? extends LootItemConditionalFunction> codec() {
        return MAP_CODEC;
    }
}
