package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.common.gear.Saddles;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.item.HorseSaddleItem;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>A saddle for every material another mod makes a chestplate of</b> (#75).
 *
 * <p>The registered half; {@link GeneratedSaddles} writes the files. It follows
 * {@link ModdedShoes}, but over {@link ModdedMaterials#chestplateMetals()}: the
 * saddle and the horse armour come in the chestplate's materials (owner,
 * 2026-11-07), so a pack's tin with no tin chestplate gets a tin shoe and
 * neither of the other two. {@link Saddles#ships} keeps a mod's own copper or
 * gold from registering a second one.
 */
public final class ModdedSaddles {

    /** One entry per <i>saddle</i>, in {@link ModdedMaterials#chestplateMetals()} order. */
    public record Saddle(ModdedMaterials.Metal metal, DeferredItem<HorseSaddleItem> item) {

        public String id() {
            return Saddles.idFor(metal.material());
        }
    }

    private static final List<Saddle> SADDLES = new ArrayList<>();

    static {
        Set<String> registered = new HashSet<>();
        for (ModdedMaterials.Metal metal : ModdedMaterials.chestplateMetals()) {
            String material = metal.material();
            if (Saddles.ships(material)) {
                continue;   // their ingot of a material we already saddle in
            }
            String id = Saddles.idFor(material);
            // THIS LOOP MUST NOT THROW - see ModdedArmour.
            if (!registered.add(id)) {
                HorseGenetics.LOGGER.warn("compat: two metals both want the id {} - {} gets no saddle. "
                        + "This should be impossible; please report it", id, metal.itemId());
                continue;
            }
            int rgb = metal.colour();
            Saddles.Tier tier = Saddles.tierOf(metal.gem());
            DeferredItem<HorseSaddleItem> item = ModItems.ITEMS.registerItem(id,
                    properties -> new HorseSaddleItem(rgb, tier, HorseSaddleItem.properties(properties)));
            ModItems.TAB_ITEMS.add(item);
            SADDLES.add(new Saddle(metal, item));
        }
        // A saddle whose material has since gone - its mod removed, or its
        // chestplate - may still be on a horse in a saved world. It loads as an
        // iron saddle, the same answer by the same list as the armour's. Not
        // verified against a real save.
        for (String armourId : ModdedMaterials.droppedArmours()) {
            String suffix = "_horse_armor";
            if (!armourId.endsWith(suffix)) {
                continue;
            }
            String material = armourId.substring(0, armourId.length() - suffix.length());
            String id = Saddles.idFor(material);
            if (!registered.contains(id) && !Saddles.ships(material)) {
                ModItems.ITEMS.addAlias(
                        Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, id),
                        Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, Saddles.Tier.IRON.id()));
            }
        }
    }

    private ModdedSaddles() {
    }

    public static List<Saddle> saddles() {
        return List.copyOf(SADDLES);
    }

    /** Touching this class registers all of it - same trick as {@code ModdedArmour.init}. */
    public static void init() {
        // Intentionally empty.
    }
}
