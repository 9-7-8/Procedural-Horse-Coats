package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>A horse armour for every ingot and gem another mod added.</b>
 *
 * <p>One registered item per <b>material name</b>, wearing the shipped greyscale
 * plate tinted to that metal's own colour - see {@link GeneratedArmour} for the
 * files, and {@link ModdedMaterials} for where the metals and the colours come
 * from.
 *
 * <p>Per material name and <b>not</b> per metal, which is the distinction that
 * cost a release: six mods can all add a steel, and an armour named after the
 * material collides with itself six times over.
 * {@link ModdedMaterials#armourMetals()} is where that is resolved and why it is
 * resolved that way. The consequence here is that
 * {@link #fromTheirChestplate} reads the chestplate of whichever of the six
 * mods won - it is one mod's opinion of what steel is worth, standing in for
 * all of them, which is a smaller error than it sounds like given they are all
 * describing the same metal.
 *
 * <h2>How strong it is, in three tries</h2>
 * The owner's call was "the mod's own material tier where we can read it, and a
 * ladder where we cannot", so {@link #protectionFor} asks three questions in
 * that order:
 * <ol>
 *   <li><b>What does their own chestplate give?</b> Then give this armour the
 *       horse-body value of whichever <i>vanilla</i> material has the closest
 *       chestplate. That is the honest translation of "their tier": it says
 *       nothing about horse armour, which their mod almost certainly has no
 *       opinion about, and everything about where they put the metal.</li>
 *   <li><b>Otherwise, a ladder.</b> A gem sits where diamond sits, an ingot
 *       where iron does, nudged one rung by the item's {@code Rarity}. This is
 *       stated plainly rather than dressed up: <b>where an ore generates is not
 *       readable from a jar</b> - there is no depth to measure and no
 *       declaration to read - so this is a guess about balance from the two
 *       weakest signals there are, and it will sometimes be wrong.</li>
 *   <li><b>Otherwise iron</b>, which is the middle of the ladder and the thing
 *       a player is least surprised by.</li>
 * </ol>
 *
 * <h2>Why the numbers are written after registration and not during</h2>
 * Step 1 reads another mod's chestplate, and no other mod's items exist while
 * ours are being registered. So the item is registered with iron's numbers and
 * {@link ModifyDefaultComponentsEvent} - which fires once every mod's items
 * exist - replaces them. That event is the only moment both halves of the
 * question are answerable at once.
 *
 * <h2>The rungs are read, not written down</h2>
 * Both halves of every vanilla rung come out of {@link ArmorMaterials} at
 * runtime. Writing "iron chest 6, iron horse 5" into this file would be a copy
 * of somebody else's balance table that goes quietly wrong the first time they
 * retune it - and vanilla has retuned exactly these numbers before.
 */
@EventBusSubscriber(modid = HorseGenetics.MOD_ID)
public final class ModdedArmour {

    /** One entry per <i>armour</i>, in {@link ModdedMaterials#armourMetals()} order. */
    public record Armour(ModdedMaterials.Metal metal, DeferredItem<Item> item) {
    }

    private static final List<Armour> ARMOURS = new ArrayList<>();
    private static final Map<String, Integer> PROTECTION = new HashMap<>();

    /** Vanilla's ladder, in the order a player would rank it. */
    private static final ArmorMaterial[] RUNGS = {
            ArmorMaterials.LEATHER,
            ArmorMaterials.COPPER,
            ArmorMaterials.IRON,
            ArmorMaterials.GOLD,
            ArmorMaterials.DIAMOND,
            ArmorMaterials.NETHERITE,
    };

    static {
        // armourMetals(), not metals(): one armour per material name, however
        // many mods brought that material. See that method for why, and for why
        // every one of their ingots still forges it.
        Set<String> registered = new HashSet<>();
        for (ModdedMaterials.Metal metal : ModdedMaterials.armourMetals()) {
            String id = metal.armourId();
            // Belt and braces. The collapse above is what makes this unreachable,
            // and this is what makes it not matter if a later change to the
            // naming rule reintroduces a clash: THIS LOOP MUST NOT THROW. It runs
            // in a static initialiser reached from the mod constructor, on data
            // read out of other people's jars, so anything it throws is an
            // ExceptionInInitializerError that takes the whole mod - and with it
            // the player's server - down at load. v0.5.012 did exactly that on a
            // 319-jar pack, on `redstone_alloy_horse_armor`. A lost armour is a
            // log line; a lost mod is an evening.
            if (!registered.add(id)) {
                HorseGenetics.LOGGER.warn("compat: two metals both want the id {} - {} gets no armour. "
                        + "This should be impossible; please report it", id, metal.itemId());
                continue;
            }
            DeferredItem<Item> item = ModItems.ITEMS.registerItem(id,
                    properties -> new Item(properties.horseArmor(provisional(metal))));
            // Into the mod's own creative tab as well as the registry. Without
            // this they exist but cannot be got at in creative at all - only
            // crafted or bought - which reads as the feature not working.
            ModItems.TAB_ITEMS.add(item);
            ARMOURS.add(new Armour(metal, item));
        }
    }

    private ModdedArmour() {
    }

    public static List<Armour> armours() {
        return List.copyOf(ARMOURS);
    }

    /** Touching this class registers all of it - same trick as {@code DoubleGates.init}. */
    public static void init() {
        // Intentionally empty.
    }

    /**
     * The material the item is <i>registered</i> with. Everything on it is final
     * except the defence, which {@link #rewriteProtection} replaces once the
     * rest of the game exists.
     */
    private static ArmorMaterial provisional(ModdedMaterials.Metal metal) {
        return material(metal, ArmorMaterials.IRON.defense().getOrDefault(ArmorType.BODY, 5));
    }

    private static ArmorMaterial material(ModdedMaterials.Metal metal, int bodyDefense) {
        ArmorMaterial like = ArmorMaterials.IRON;
        return new ArmorMaterial(
                like.durability(),
                Map.of(ArmorType.BODY, bodyDefense),
                like.enchantmentValue(),
                SoundEvents.ARMOR_EQUIP_IRON,
                like.toughness(),
                like.knockbackResistance(),
                // Their own common tag, which is where we found the metal in the
                // first place - so an anvil repairs it with the ingot it is made
                // of, without us naming their item anywhere.
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(
                        "c", (metal.gem() ? "gems/" : "ingots/") + metal.material())),
                assetKey(metal));
    }

    private static ResourceKey<EquipmentAsset> assetKey(ModdedMaterials.Metal metal) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID,
                Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, metal.armourId()));
    }

    // ------------------------------------------------------------------
    // Protection
    // ------------------------------------------------------------------

    /**
     * How much armour this metal's horse armour gives. Cached: the registry
     * lookups behind it are not free and the answer cannot change within a run.
     *
     * <p>Safe to call before the item registry is populated - it simply falls
     * through to the ladder - which is what lets {@link GeneratedArmour} use it
     * to decide which of the metalsmith's tiers stocks the thing.
     */
    public static int protectionFor(ModdedMaterials.Metal metal) {
        return PROTECTION.computeIfAbsent(metal.itemId(), key -> {
            Integer theirs = fromTheirChestplate(metal);
            return theirs != null ? theirs : fromLadder(metal);
        });
    }

    /**
     * Their chestplate's armour value, translated to the horse-body value of the
     * vanilla material it most resembles.
     *
     * <p>The chestplate is found <b>by name</b> - {@code <their ns>:<material>_chestplate} -
     * and that is a convention rather than a contract. Nothing makes a mod name
     * its armour after its ingot; almost every mod does. When one does not, this
     * returns null and the ladder answers instead, which is the right failure:
     * a wrong guess here would be invisible, and no guess is merely approximate.
     */
    private static Integer fromTheirChestplate(ModdedMaterials.Metal metal) {
        String namespace = metal.itemId().substring(0, metal.itemId().indexOf(':'));
        Item chestplate = BuiltInRegistries.ITEM.getValue(
                Identifier.fromNamespaceAndPath(namespace, metal.material() + "_chestplate"));
        if (chestplate == null || chestplate == Items.AIR) {
            return null;
        }
        double theirs = armourValue(chestplate);
        if (theirs <= 0) {
            return null;
        }
        ArmorMaterial nearest = null;
        double best = Double.MAX_VALUE;
        for (ArmorMaterial rung : RUNGS) {
            double distance = Math.abs(rung.defense().getOrDefault(ArmorType.CHESTPLATE, 0) - theirs);
            if (distance < best) {
                best = distance;
                nearest = rung;
            }
        }
        return nearest == null ? null : nearest.defense().getOrDefault(ArmorType.BODY, 5);
    }

    /** The ARMOR this item adds when worn, read off its own default components. */
    private static double armourValue(Item item) {
        var modifiers = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0;
        }
        double total = 0;
        for (var entry : modifiers.modifiers()) {
            if (entry.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
                    && entry.slot().test(EquipmentSlot.CHEST)) {
                total += entry.modifier().amount();
            }
        }
        return total;
    }

    /**
     * The guess. See the class note for why this is weaker than it looks: an
     * ore's depth is not in a jar, so the only signals are which of the two
     * common tags the mod filed it under and what {@code Rarity} it gave the
     * item.
     */
    private static int fromLadder(ModdedMaterials.Metal metal) {
        ArmorMaterial base = metal.gem() ? ArmorMaterials.DIAMOND : ArmorMaterials.IRON;
        int defense = base.defense().getOrDefault(ArmorType.BODY, 5);

        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(metal.itemId()));
        if (item != null && item != Items.AIR) {
            var rarity = item.components().get(DataComponents.RARITY);
            if (rarity != null) {
                defense += switch (rarity) {
                    case COMMON -> 0;
                    case UNCOMMON -> 1;
                    case RARE -> 2;
                    case EPIC -> 3;
                };
            }
        }
        // Never above netherite's: a modded ingot may be anything at all, and
        // "better than the best thing in the game" is not a claim this mod is in
        // a position to make on another mod's behalf.
        return Math.min(defense, ArmorMaterials.NETHERITE.defense().getOrDefault(ArmorType.BODY, 12));
    }

    /**
     * Replace the provisional numbers now that every mod's items exist. This is
     * the only moment {@link #fromTheirChestplate} can answer.
     */
    @SubscribeEvent
    static void rewriteProtection(ModifyDefaultComponentsEvent event) {
        for (Armour armour : ARMOURS) {
            // Cleared first so the cache cannot be holding a ladder answer from
            // a pack-generation call that ran before the registry was up.
            PROTECTION.remove(armour.metal().itemId());
            int defense = protectionFor(armour.metal());
            // The three-argument Initializer, not the Consumer overload beside
            // it: that one is @Deprecated(forRemoval) in 26.1.2 itself.
            event.modify(armour.item().get(), (components, context, item) -> components.set(
                    DataComponents.ATTRIBUTE_MODIFIERS,
                    material(armour.metal(), defense).createAttributes(ArmorType.BODY)));
        }
        if (!ARMOURS.isEmpty()) {
            HorseGenetics.LOGGER.info("compat: set armour values for {} modded horse armours", ARMOURS.size());
        }
    }
}
