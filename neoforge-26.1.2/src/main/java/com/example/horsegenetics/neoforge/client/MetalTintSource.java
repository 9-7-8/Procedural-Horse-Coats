package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.compat.ModdedArmour;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Colours a generated horse armour's <b>inventory icon</b> with the metal it is
 * made of.
 *
 * <h2>Why the icon needs its own answer</h2>
 * A generated armour is one shipped greyscale plate tinted per material, and the
 * tint reaches the <i>worn</i> armour through the equipment asset's
 * {@code dyeable.color_when_undyed}. That mechanism does not reach an item in a
 * slot at all - equipment assets describe layers on an entity. So the icon needs
 * the same colour delivered by the other route, which is an
 * {@link ItemTintSource} named by the generated item definition.
 *
 * <p>Two routes to one number, then. They cannot drift, because both are written
 * from {@code ModdedMaterials.Metal.colour()} in the same pass - one into the
 * equipment JSON, one read back here off the item's own id.
 *
 * <h2>Why it looks the colour up by id</h2>
 * The alternative is a component on every stack, which would be a byte of
 * network traffic and a migration risk for a number that is constant per item
 * and already known on both sides. The id is the key the generated pack used, so
 * asking the same question of the same table gives the same answer.
 *
 * <p>Registered in {@code ClientSetup} under {@code horsegenetics:metal_tint},
 * which is the id the generated {@code items/<metal>_horse_armor.json} names.
 * A plain white fallback rather than a guess: a grey plate is a readable icon,
 * and an armour tinted the wrong colour is a bug that looks like a decision.
 */
public final class MetalTintSource implements ItemTintSource {

    public static final MapCodec<MetalTintSource> MAP_CODEC =
            MapCodec.unit(new MetalTintSource());

    /** Item id -> colour. Built once; the generated set cannot change in a run. */
    private static Map<String, Integer> colours;

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (colours == null) {
            Map<String, Integer> built = new HashMap<>();
            for (ModdedArmour.Armour armour : ModdedArmour.armours()) {
                built.put(armour.metal().armourId(), armour.metal().colour());
            }
            colours = built;
        }
        Integer colour = colours.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        return ARGB.opaque(colour != null ? colour : 0xFFFFFF);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
