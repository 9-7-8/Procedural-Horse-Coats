package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.common.cart.CartKind;
import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * <b>A cart in every wood another mod adds</b> - the item model, the item
 * definition, the recipe and the lang key for each, written into the generated
 * pack at run time.
 *
 * <h2>The same contract lives twice</h2>
 * {@code tools/bake-carts.mjs} does this for vanilla's twelve at author time
 * and commits the result; this does it for everyone else's woods at run time.
 * That is deliberately the same split the double gates use, and it carries the
 * same warning: <b>change one, change the other.</b> A drift shows up as a
 * modded cart that cannot be crafted while every vanilla one can, or as a
 * purple item in the creative tab, and neither logs a thing.
 *
 * <h2>What is not generated</h2>
 * <b>No texture, ever</b>, on either side of that split. The cart a player
 * actually looks at in the world is stitched from the wood's own block sprites
 * by {@code CartModelManagerMixin} at resource-reload time, so a modded cart is
 * correctly coloured without anyone drawing anything.
 *
 * <p>The <i>inventory icon</i> is the exception, and a known gap: there are 72
 * painted icons in this jar, one per vanilla wood per cart, and nothing to
 * paint a modded one from. A modded wood's carts therefore wear the oak icon in
 * the inventory and look right the moment they are placed. Tinting the oak icon
 * per wood is the obvious fix and needs an average plank colour on
 * {@link ModdedMaterials.Wood}, which it does not carry yet - see
 * {@code MetalTintSource} for the pattern that would do it.
 *
 * <h2>Recipes</h2>
 * A modded cart's recipe spends that mod's own planks, and the wagon and reaper
 * additionally want its stripped log and its slab. Those are guessed from
 * vanilla's naming convention, so every one is checked with
 * {@link ModdedMaterials#itemExists} first and the recipe is skipped - loudly -
 * rather than written against an item that is not there. An uncraftable cart is
 * a disappointment; a recipe naming a missing item is a data-pack error on
 * every world load.
 */
final class GeneratedCarts {

    private static final String NS = HorseGenetics.MOD_ID;
    private static final String WHEEL = NS + ":cart_wheel";

    /** The icon a modded wood's carts borrow. See the class note. */
    private static final String FALLBACK_WOOD = "oak";

    private GeneratedCarts() {
    }

    static void emit(final Map<String, byte[]> files, final JsonObject lang) {
        for (final ModdedMaterials.Wood wood : ModdedMaterials.woods()) {
            final String prefix = wood.namespace() + "_" + wood.name();
            for (final CartKind kind : CartKind.values()) {
                final String id = prefix + "_" + kind.id();

                // Model + item definition, both pointing at the fallback icon.
                final JsonObject textures = new JsonObject();
                textures.addProperty("layer0", NS + ":item/" + FALLBACK_WOOD + "_" + kind.id());
                final JsonObject model = new JsonObject();
                model.addProperty("parent", "minecraft:item/generated");
                model.add("textures", textures);
                GeneratedGates.put(files, "assets/" + NS + "/models/item/" + id + ".json", model);

                final JsonObject inner = new JsonObject();
                inner.addProperty("type", "minecraft:model");
                inner.addProperty("model", NS + ":item/" + id);
                final JsonObject itemDef = new JsonObject();
                itemDef.add("model", inner);
                GeneratedGates.put(files, "assets/" + NS + "/items/" + id + ".json", itemDef);

                lang.addProperty("item." + NS + "." + id, label(wood.name()) + " " + label(kind.id()));

                final JsonObject recipe = recipe(wood, kind, id);
                if (recipe != null) {
                    GeneratedGates.put(files, "data/" + NS + "/recipe/" + id + ".json", recipe);
                }
            }
        }
    }

    /**
     * The shaped recipe for one cart in one modded wood, or null when an
     * ingredient this mod would have to name does not exist.
     *
     * <p>Mirrors {@code bake-carts.mjs}'s {@code recipe()} exactly. The patterns
     * are upstream UsefulCarts', unchanged.
     */
    private static JsonObject recipe(final ModdedMaterials.Wood wood, final CartKind kind, final String id) {
        final String planks = wood.namespace() + ":" + wood.name() + "_planks";
        final String slab = wood.namespace() + ":" + wood.name() + "_slab";
        final String stripped = wood.namespace() + ":stripped_" + wood.name() + "_log";

        if (!ModdedMaterials.itemExists(planks)) {
            HorseGenetics.LOGGER.warn("compat: {} has no planks item ({}) - {} will be uncraftable",
                    wood.name(), planks, id);
            return null;
        }

        final JsonObject key = new JsonObject();
        final JsonArray pattern = new JsonArray();
        key.addProperty("p", planks);
        key.addProperty("w", WHEEL);
        switch (kind) {
            case SUPPLY_CART -> {
                key.addProperty("c", "minecraft:chest");
                pattern.add("pcp");
                pattern.add("pcp");
                pattern.add("wpw");
            }
            case PLOW -> {
                key.addProperty("s", "minecraft:stick");
                pattern.add("sss");
                pattern.add("psp");
                pattern.add("wpw");
            }
            case SEED_DRILL -> {
                key.addProperty("c", "minecraft:chest");
                key.addProperty("h", "minecraft:hopper");
                pattern.add("pcp");
                pattern.add("php");
                pattern.add("wpw");
            }
            case REAPER -> {
                if (!ModdedMaterials.itemExists(slab)) {
                    HorseGenetics.LOGGER.warn("compat: no slab {} - {} will be uncraftable", slab, id);
                    return null;
                }
                key.addProperty("i", "minecraft:iron_ingot");
                key.addProperty("l", slab);
                key.addProperty("s", "minecraft:stick");
                // "sl " and not "sl" - every row of a shaped recipe must be the
                // same width, and a short one throws out the whole recipe file
                // at datapack load. See the note in tools/bake-carts.mjs, which
                // writes this same shape for vanilla's woods.
                pattern.add("sl ");
                pattern.add("spp");
                pattern.add("iww");
            }
            case ANIMAL_CART -> {
                pattern.add("ppp");
                pattern.add("ppp");
                pattern.add("wpw");
            }
            case WAGON -> {
                if (!ModdedMaterials.itemExists(stripped)) {
                    HorseGenetics.LOGGER.warn("compat: no stripped log {} - {} will be uncraftable", stripped, id);
                    return null;
                }
                key.addProperty("l", stripped);
                pattern.add("lll");
                pattern.add("wpw");
                pattern.add("wpw");
            }
        }

        final JsonObject result = new JsonObject();
        result.addProperty("count", 1);
        result.addProperty("id", NS + ":" + id);

        final JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shaped");
        recipe.addProperty("category", "misc");
        recipe.add("key", key);
        recipe.add("pattern", pattern);
        recipe.add("result", result);
        return recipe;
    }

    /** {@code dark_oak} to {@code Dark Oak}. */
    private static String label(final String name) {
        final StringBuilder out = new StringBuilder();
        for (final String word : name.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.length() == 0 ? name : out.toString();
    }
}
