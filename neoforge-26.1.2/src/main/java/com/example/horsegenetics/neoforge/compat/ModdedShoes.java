package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.common.gear.HorseShoes;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.item.HorseShoeItem;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>A horse shoe for every ingot and gem another mod added.</b>
 *
 * <p>The registered half; {@link GeneratedShoes} writes the files. It follows
 * {@link ModdedArmour} on purpose and shares its list: one shoe per
 * <b>material name</b> out of {@link ModdedMaterials#armourMetals()}, so six
 * mods' steels are one Steel Horse Shoe for the reason given there.
 *
 * <p>Where it differs: a shoe is this mod's item for vanilla's metals too
 * ({@code ModItems.HORSE_SHOES}), where horse armour is vanilla's. So a mod
 * that ships its own copper or gold ingot must not get a second copper or gold
 * shoe - {@link HorseShoes#ships} is that rule.
 */
public final class ModdedShoes {

    /** One entry per <i>shoe</i>, in {@link ModdedMaterials#armourMetals()} order. */
    public record Shoe(ModdedMaterials.Metal metal, DeferredItem<HorseShoeItem> item) {

        public String id() {
            return HorseShoes.idFor(metal.material());
        }
    }

    private static final List<Shoe> SHOES = new ArrayList<>();

    static {
        Set<String> registered = new HashSet<>();
        for (ModdedMaterials.Metal metal : ModdedMaterials.armourMetals()) {
            String material = metal.material();
            if (HorseShoes.ships(material)) {
                // Their ingot of a metal we already shoe in. Not a warning: it is
                // the ordinary case for a mod that re-registers a vanilla metal.
                continue;
            }
            String id = HorseShoes.idFor(material);
            // THIS LOOP MUST NOT THROW - see ModdedArmour, which learned it on a
            // 319-jar pack. The collapse in armourMetals() makes a clash
            // unreachable; this makes it not matter if that ever changes.
            if (!registered.add(id)) {
                HorseGenetics.LOGGER.warn("compat: two metals both want the id {} - {} gets no shoe. "
                        + "This should be impossible; please report it", id, metal.itemId());
                continue;
            }
            int rgb = metal.colour();
            // Where its armour stands (ModdedArmour.protectionFor): pure, because
            // nothing about their item can be asked this early.
            HorseShoes.Metal tier = HorseShoes.tierOf(metal.gem());
            DeferredItem<HorseShoeItem> item = ModItems.ITEMS.registerItem(id,
                    properties -> new HorseShoeItem(rgb, tier, properties.stacksTo(1)
                            .enchantable(com.example.horsegenetics.neoforge.item.ShoeEnchantments.ENCHANTABILITY)));
            ModItems.TAB_ITEMS.add(item);
            SHOES.add(new Shoe(metal, item));
        }
        // A shoe whose only metal belongs to a mod that has since been removed
        // may still be on a hoof in a saved world. The alias makes it load as an
        // iron shoe instead of naming an id that does not exist - the same
        // answer, by the same list, as ModdedArmour's. Not verified against a
        // real save.
        for (String armourId : ModdedMaterials.droppedArmours()) {
            String suffix = "_horse_armor";
            if (!armourId.endsWith(suffix)) {
                continue;
            }
            String material = armourId.substring(0, armourId.length() - suffix.length());
            String id = HorseShoes.idFor(material);
            if (!registered.contains(id) && !HorseShoes.ships(material)) {
                ModItems.ITEMS.addAlias(
                        Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, id),
                        Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, HorseShoes.Metal.IRON.id()));
            }
        }
    }

    private ModdedShoes() {
    }

    public static List<Shoe> shoes() {
        return List.copyOf(SHOES);
    }

    /** Touching this class registers all of it - same trick as {@code ModdedArmour.init}. */
    public static void init() {
        // Intentionally empty.
    }
}
