package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
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
 * resolved that way.
 *
 * <h2>How strong it is</h2>
 * A gem sits where diamond sits and an ingot sits where iron does, and that is
 * the whole of it. Stated plainly rather than dressed up: <b>where an ore
 * generates is not readable from a jar</b> - there is no depth to measure and no
 * declaration to read - so this is a guess about balance from the one signal
 * there is, and it will sometimes be wrong.
 *
 * <h2>Why it is not cleverer than that - and cannot be</h2>
 * It used to be. {@code protectionFor} asked their own chestplate what it was
 * worth first, and gave this armour the horse-body value of whichever vanilla
 * material came closest; the ladder was only the fallback. It was a better
 * answer and it does not work, for a reason worth writing down because it will
 * come up again for any mod that wants to read another mod's item:
 *
 * <p><b>An item's armour value, its rarity, and everything else about it is a
 * data component, and default components are bound exactly once - during the
 * first datapack load, long after every moment at which this mod can still say
 * what its own items are worth.</b> {@code Item.components()} delegates to
 * {@code Holder.Reference.components()}, which throws
 * {@code NullPointerException: Components not bound yet} until then.
 * {@link ModifyDefaultComponentsEvent} is not an exception to that - it is the
 * mechanism by which components are built, so nothing is bound while it is
 * being dispatched. Nor is deferring the read into the {@code Initializer}
 * lambda it hands out: {@code DataComponentInitializers.build} runs every
 * initializer into builders and only then binds them all, so during that pass
 * no item is bound either. Both were tried; see {@code wiki/api-notes.html}.
 *
 * <p>The consequence is that the ladder is the only thing askable at a moment
 * we can still answer, so it is now asked directly, at registration, and
 * {@link #protectionFor} is a pure function of the metal. That is also what
 * makes it safe for {@code GeneratedArmour} to call while deciding which of the
 * metalsmith's tiers stocks the thing, which happens at pack-finder time -
 * another moment components are not yet bound, and the second place the old
 * version would have crashed had the first not got there first.
 *
 * <h2>The rungs are read, not written down</h2>
 * Every vanilla rung comes out of {@link ArmorMaterials} at runtime. Writing
 * "iron horse 5" into this file would be a copy of somebody else's balance table
 * that goes quietly wrong the first time they retune it - and vanilla has
 * retuned exactly these numbers before.
 */
public final class ModdedArmour {

    /** One entry per <i>armour</i>, in {@link ModdedMaterials#armourMetals()} order. */
    public record Armour(ModdedMaterials.Metal metal, DeferredItem<Item> item) {
    }

    private static final List<Armour> ARMOURS = new ArrayList<>();

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
                    properties -> new Item(properties.horseArmor(
                            material(metal, protectionFor(metal)))));
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
     * How much armour this metal's horse armour gives: diamond's body value for
     * a gem, iron's for an ingot.
     *
     * <p><b>A pure function of the metal, and it has to be.</b> It is called from
     * a static initialiser in the mod constructor, from
     * {@link GeneratedArmour}'s tier decision at pack-finder time, and from the
     * item registration in between - three moments with three different amounts
     * of game in existence, and the only reason it is safe at all three is that
     * it touches neither the registries nor any item's components. The class
     * note says what it cost to learn that. Do not put a registry lookup back in
     * here.
     */
    public static int protectionFor(ModdedMaterials.Metal metal) {
        ArmorMaterial base = metal.gem() ? ArmorMaterials.DIAMOND : ArmorMaterials.IRON;
        return base.defense().getOrDefault(ArmorType.BODY, 5);
    }
}
