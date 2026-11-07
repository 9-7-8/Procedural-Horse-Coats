package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.common.gear.Saddles;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * The generated half of "a saddle for every material another mod makes a
 * chestplate of" (#75). The registered half is {@link ModdedSaddles}; this
 * writes each saddle's item definition, its name and its recipes.
 *
 * <p><b>No texture, no model, no equipment asset.</b> Every saddle, shipped or
 * generated, is the one shipped {@code item/saddle} icon with its fittings
 * tinted by {@code client/MetalTintSource}, and on the horse the one
 * {@code horsegenetics:saddle} asset with its fittings layer tinted by
 * {@code client/TackClientExtensions} - both from the item's own colour.
 *
 * <p><b>A recipe per ingot</b>, as {@link GeneratedArmour} writes them, so six
 * mods' steels all make the one Steel Saddle: a leather saddle and two of the
 * metal. Every saddle starts as leather (owner, 2026-11-07), and the leather
 * saddle is this mod's item, which keeps the house rule that a recipe carries
 * one of ours.
 *
 * <p><b>Not sold.</b> The leatherworker stocks the shipped saddles; a generated
 * trade set would have to rewrite his tiers the way {@link MetalsmithTrades}
 * rewrites the metalsmith's, for a saddle a player can craft from a leather one
 * anyway. The tack bench (#226) is where every saddle is made in the end.
 */
final class GeneratedSaddles {

    private static final String NS = HorseGenetics.MOD_ID;

    private GeneratedSaddles() {
    }

    static void emit(Map<String, byte[]> files, JsonObject lang) {
        List<ModdedSaddles.Saddle> saddles = ModdedSaddles.saddles();
        for (ModdedSaddles.Saddle saddle : saddles) {
            ModdedMaterials.Metal metal = saddle.metal();
            String id = saddle.id();

            // ---- the item definition: the shared icon, fittings tinted ----
            GeneratedGates.put(files, "assets/" + NS + "/items/" + id + ".json", itemDefinition());
            lang.addProperty("item." + NS + "." + id, GeneratedArmour.label(metal) + " Saddle");

            boolean craftable = false;
            for (ModdedMaterials.Metal source : ModdedMaterials.metalsFor(metal.armourId())) {
                if (!ModdedMaterials.itemExists(source.itemId())) {
                    continue;
                }
                GeneratedGates.put(files,
                        "data/" + NS + "/recipe/" + id + "_from_" + source.itemId().replace(':', '_') + ".json",
                        recipe(source.itemId(), id));
                craftable = true;
            }
            if (!craftable) {
                HorseGenetics.LOGGER.warn("compat: no registered item is in the c: tag that named {} - "
                        + "{} is uncraftable", metal.itemId(), id);
            }
        }
    }

    /**
     * The icon every saddle shares: layer 0 the leather, untinted; layer 1 the
     * fittings, in the item's own colour. Shipped saddles carry the same file,
     * written by hand.
     */
    static JsonObject itemDefinition() {
        JsonObject plain = new JsonObject();
        plain.addProperty("type", "minecraft:constant");
        plain.addProperty("value", -1);
        JsonObject metal = new JsonObject();
        metal.addProperty("type", NS + ":metal_tint");
        JsonArray tints = new JsonArray();
        tints.add(plain);
        tints.add(metal);
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", NS + ":item/saddle");
        model.add("tints", tints);
        JsonObject item = new JsonObject();
        item.add("model", model);
        return item;
    }

    /** A leather saddle and two of the metal: shapeless, the house rule. */
    static JsonObject recipe(String metalItem, String id) {
        JsonArray ingredients = new JsonArray();
        ingredients.add(NS + ":" + Saddles.Tier.LEATHER.id());
        ingredients.add(metalItem);
        ingredients.add(metalItem);
        JsonObject result = new JsonObject();
        result.addProperty("id", NS + ":" + id);
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "equipment");
        recipe.add("ingredients", ingredients);
        recipe.add("result", result);
        return recipe;
    }
}
