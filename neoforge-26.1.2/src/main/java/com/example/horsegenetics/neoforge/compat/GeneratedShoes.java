package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * The generated half of "a horse shoe for every ingot another mod added".
 *
 * <p>The registered half is {@link ModdedShoes}. This writes each shoe's item
 * definition, its name, its place in the four hoof-slot tags and its trade.
 *
 * <h2>What it does not write</h2>
 * <b>No texture and no model.</b> Every shoe, shipped or generated, is the one
 * shipped greyscale {@code item/horse_shoe} tinted by
 * {@code client/MetalTintSource} - the trick {@link GeneratedArmour} uses for
 * its icon, and for the same reasons. The shoe on the hoof is not a texture at
 * all: it is boxes in the item's colour ({@code common/gear/HorseShoes}).
 *
 * <p><b>No recipe.</b> Tack is made at the tack bench (#226), which does not
 * exist yet; until it does a shoe is bought from the metalsmith (owner,
 * 2026-10-12). The bench will read the metal out of the same scan.
 *
 * <h2>The tags are added to, the trades are not</h2>
 * A tag merges across packs, so the four {@code gear/shoe_*} files written here
 * list only the generated shoes and say {@code replace: false}. A trade set
 * does not merge - see {@link MetalsmithTrades}.
 */
final class GeneratedShoes {

    private static final String NS = HorseGenetics.MOD_ID;

    /** The four hoof slots' tags - {@code HorseTackSlot.SHOE_*}, lower-cased. */
    static final List<String> SLOT_TAGS =
            List.of("shoe_front_left", "shoe_front_right", "shoe_hind_left", "shoe_hind_right");

    private GeneratedShoes() {
    }

    static void emit(Map<String, byte[]> files, JsonObject lang, MetalsmithTrades trades) {
        List<ModdedShoes.Shoe> shoes = ModdedShoes.shoes();
        if (shoes.isEmpty()) {
            return;
        }
        JsonArray tagged = new JsonArray();
        for (ModdedShoes.Shoe shoe : shoes) {
            ModdedMaterials.Metal metal = shoe.metal();
            String id = shoe.id();

            // ---- the item definition: the shared drawing, tinted ------------
            JsonObject tint = new JsonObject();
            tint.addProperty("type", NS + ":metal_tint");
            JsonArray tints = new JsonArray();
            tints.add(tint);
            JsonObject itemModel = new JsonObject();
            itemModel.addProperty("type", "minecraft:model");
            itemModel.addProperty("model", NS + ":item/horse_shoe");
            itemModel.add("tints", tints);
            JsonObject item = new JsonObject();
            item.add("model", itemModel);
            GeneratedGates.put(files, "assets/" + NS + "/items/" + id + ".json", item);

            lang.addProperty("item." + NS + "." + id, GeneratedArmour.label(metal) + " Horse Shoe");
            tagged.add(NS + ":" + id);

            // ---- and the metalsmith stocks it -------------------------------
            // Only where the pack can also get the metal: the rule the armour
            // beside it follows (issue #14), so the two are sold together or
            // not at all.
            boolean obtainable = false;
            for (ModdedMaterials.Metal source : ModdedMaterials.metalsFor(metal.armourId())) {
                if (ModdedMaterials.itemExists(source.itemId())) {
                    obtainable = true;
                    break;
                }
            }
            if (!obtainable) {
                HorseGenetics.LOGGER.warn("compat: no registered item is in the c: tag that named {} - "
                        + "{} is not sold", metal.itemId(), id);
                continue;
            }
            // The tier its armour is sold at, at a quarter of the armour's
            // price or so: four shoes cost about what the barding does.
            int tier = GeneratedArmour.tierFor(metal);
            trades.sell(files, tier, id, priceFor(tier), 8, tier * 2);
        }

        for (String slot : SLOT_TAGS) {
            JsonObject tag = new JsonObject();
            tag.addProperty("replace", false);
            tag.add("values", tagged.deepCopy());
            GeneratedGates.put(files, "data/" + NS + "/tags/item/gear/" + slot + ".json", tag);
        }
    }

    /** The shipped shoes run 2-10 emeralds across the five tiers; these sit with them. */
    private static int priceFor(int tier) {
        return switch (tier) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 7;
            default -> 10;
        };
    }
}
