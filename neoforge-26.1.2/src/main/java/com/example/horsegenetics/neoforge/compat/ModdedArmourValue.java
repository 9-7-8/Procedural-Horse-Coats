package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.common.gear.ArmourCurve;
import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>A modded horse armour protects in proportion to its mod's own chestplate</b>
 * (#296), instead of {@link ModdedArmour#protectionFor}'s flat guess (iron's
 * value for an ingot, diamond's for a gem).
 *
 * <p><b>Why an event and not the item.</b> {@link ModdedArmour}'s class note
 * says it in full: another item's armour value is a data component, and none is
 * bound until the first datapack load, long after our item has had to say what
 * it is worth. So the item still registers with the guess, and
 * {@link ItemAttributeModifierEvent} - fired every time a stack's modifiers are
 * read, which is how a horse picks up its body armour and how a tooltip prints
 * "+N Armor" - swaps the guess for the chestplate's value once it can be read.
 * A chestplate that cannot be found, or reads as no armour, leaves the guess.
 *
 * <p><b>The value.</b> The chestplate's own armour (what its stack's modifiers
 * add in the chest slot, so another mod's event-made armour counts too) run
 * through vanilla's chestplate-to-horse curve, {@link ArmourCurve}, with the
 * rungs read off {@link ArmorMaterials} here rather than copied, and
 * netherite's horse value as the ceiling.
 *
 * <p><b>Which chestplate.</b> {@code <ns>:<material>_chestplate}, any namespace
 * but ours - the same spelling {@code MaterialScan.collectChestplates} reads out
 * of the jars - and, where two mods both make one, the first by id, which is the
 * one {@code MaterialScan} took the colour from.
 *
 * <p>Worked out once per armour and kept until a datapack reload
 * ({@link TagsUpdatedEvent}). A horse already wearing the armour keeps the old
 * value until it is taken off and put back: vanilla re-reads modifiers only
 * when the equipped stack changes. UNVERIFIED: that the client has its
 * components bound by the time it draws a tooltip - a read that throws is
 * caught and not kept, so the worst case is the guess on the tooltip only.
 */
@EventBusSubscriber
public final class ModdedArmourValue {

    /** Vanilla's own body-armour modifier id ({@code ArmorMaterial.createAttributes}), replaced in place. */
    private static final Identifier BODY_ARMOR_ID = Identifier.withDefaultNamespace("armor." + ArmorType.BODY.getName());

    /** No reading: keep the registered guess. */
    private static final int NO_READING = -1;

    private static final Map<Item, Integer> VALUES = new ConcurrentHashMap<>();
    private static volatile Map<Item, ModdedMaterials.Metal> metalsByItem;

    private ModdedArmourValue() {
    }

    @SubscribeEvent
    static void onAttributes(ItemAttributeModifierEvent event) {
        Item item = event.getItemStack().getItem();
        ModdedMaterials.Metal metal = metalsByItem().get(item);
        if (metal == null) {
            return;
        }
        int body;
        try {
            body = VALUES.computeIfAbsent(item, ignored -> valueFor(metal));
        } catch (RuntimeException notBoundYet) {
            // Components not bound yet: keep the guess this once. computeIfAbsent
            // keeps nothing when its function throws, so the next read tries again.
            return;
        }
        if (body == NO_READING) {
            return;
        }
        event.replaceModifier(Attributes.ARMOR,
                new AttributeModifier(BODY_ARMOR_ID, body, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.BODY);
    }

    @SubscribeEvent
    static void onReload(TagsUpdatedEvent event) {
        VALUES.clear();
    }

    private static Map<Item, ModdedMaterials.Metal> metalsByItem() {
        Map<Item, ModdedMaterials.Metal> map = metalsByItem;
        if (map == null) {
            map = new HashMap<>();
            for (ModdedArmour.Armour armour : ModdedArmour.armours()) {
                map.put(armour.item().get(), armour.metal());
            }
            metalsByItem = map;
        }
        return map;
    }

    /**
     * The horse armour value this metal's chestplate earns, or {@link #NO_READING}.
     * A read that throws (components not bound yet) throws on through, so
     * {@link #onAttributes} keeps nothing and asks again next time, rather than
     * pinning the guess until a reload.
     */
    private static int valueFor(ModdedMaterials.Metal metal) {
        Item chestplate = chestplateOf(metal.material());
        if (chestplate == null) {
            return NO_READING;
        }
        double chest = chestArmour(new ItemStack(chestplate));
        if (!(chest > 0)) {
            return NO_READING;
        }
        int body = bodyFor(chest);
        HorseGenetics.LOGGER.info("compat: {} follows {} (chest {}) - {} on a horse",
                metal.armourId(), BuiltInRegistries.ITEM.getKey(chestplate), chest, body);
        return body;
    }

    /** What a chestplate worth {@code chest} gives on a horse, on vanilla's plain rungs. */
    public static int bodyFor(double chest) {
        return ArmourCurve.bodyFor(chest, List.of(
                rung(ArmorMaterials.LEATHER), rung(ArmorMaterials.COPPER),
                rung(ArmorMaterials.IRON), rung(ArmorMaterials.DIAMOND)),
                ArmorMaterials.NETHERITE.defense().getOrDefault(ArmorType.BODY, 19));
    }

    private static ArmourCurve.Rung rung(ArmorMaterial material) {
        return new ArmourCurve.Rung(material.defense().getOrDefault(ArmorType.CHESTPLATE, 0),
                material.defense().getOrDefault(ArmorType.BODY, 0));
    }

    /** The armour a stack adds in the chest slot, by plain addition. */
    public static double chestArmour(ItemStack stack) {
        double[] sum = {0};
        stack.getAttributeModifiers().forEach(EquipmentSlot.CHEST, (attribute, modifier) -> {
            if (attribute.value() == Attributes.ARMOR.value() && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                sum[0] += modifier.amount();
            }
        });
        return sum[0];
    }

    /** {@code <ns>:<material>_chestplate} in any namespace but ours, the first by id. */
    private static Item chestplateOf(String material) {
        String path = material + "_chestplate";
        TreeMap<String, Item> found = new TreeMap<>();
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (id.getPath().equals(path) && !id.getNamespace().equals(HorseGenetics.MOD_ID)) {
                found.put(id.toString(), BuiltInRegistries.ITEM.getValue(id));
            }
        }
        return found.isEmpty() ? null : found.firstEntry().getValue();
    }
}
