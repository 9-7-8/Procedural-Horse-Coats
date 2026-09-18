package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The generated half of "a horse armour for every ingot another mod added".
 *
 * <p>The registered half is {@link ModdedArmour}; this writes the equipment
 * asset, the item model, the recipe, the language keys and the metalsmith's
 * trades.
 *
 * <h2>One texture, tinted - not a PNG per metal</h2>
 * The obvious build is to recolour vanilla's iron armour once per ingot and
 * write the bytes out. This does not do that, and the reason is that the mod
 * already has a better mechanism for exactly this shape of problem.
 *
 * <p>An equipment asset layer may declare {@code dyeable}, and vanilla then
 * multiplies a colour over the layer's texture. So every generated armour points
 * at <b>one</b> shipped greyscale plate -
 * {@code textures/entity/equipment/horse_body/plate.png}, a normalised tint mask
 * baked out of vanilla's iron by {@code tools/tack/bake-plate-armor-textures.ps1}
 * - and differs from its neighbours only by the {@code color_when_undyed} in its
 * own little JSON file.
 *
 * <p>What that buys: no image code at runtime, nothing to go wrong decoding or
 * encoding a PNG, a generated pack that is a few hundred bytes per metal instead
 * of a few kilobytes, and - the one that actually decided it - <b>identical
 * behaviour on a dedicated server</b>, which has no business generating
 * textures and may not even have vanilla's client assets to read.
 *
 * <h2>The trades are rewritten, not appended to</h2>
 * {@code minecraft:trade_set} is a datapack <i>registry</i>, so two packs
 * declaring the same tier do not merge - the higher one wins outright. The
 * generated tier file therefore has to carry the shipped trades as well as the
 * new ones, and it gets them by <b>reading the shipped file off our own
 * classpath</b> rather than by keeping a second copy of the list here. Edit the
 * metalsmith's tiers and this follows; it cannot go stale.
 */
final class GeneratedArmour {

    private static final String NS = HorseGenetics.MOD_ID;
    private static final String METALSMITH = "equestrian_metalsmith";

    /** The shipped plate, and the item icon that goes with it. */
    static final String PLATE_TEXTURE = NS + ":plate";

    private GeneratedArmour() {
    }

    static void emit(Map<String, byte[]> files, JsonObject lang) {
        List<ModdedMaterials.Metal> metals = ModdedMaterials.metals();
        if (metals.isEmpty()) {
            return;
        }
        // Which tier each armour is sold at, so the trade files below can be
        // grouped: the cheap end of the ladder early, the expensive end late.
        JsonObject[] tierTrades = new JsonObject[6];

        for (ModdedMaterials.Metal metal : metals) {
            String id = metal.armourId();
            int colour = metal.colour();

            // ---- the equipment asset: one dyeable layer ---------------------
            JsonObject layer = new JsonObject();
            layer.addProperty("texture", PLATE_TEXTURE);
            JsonObject dyeable = new JsonObject();
            // Signed ARGB, opaque - the same shape vanilla's own leather asset
            // uses, and what SaddleTint's constants become on the way in.
            dyeable.addProperty("color_when_undyed", 0xFF000000 | colour);
            layer.add("dyeable", dyeable);
            JsonArray body = new JsonArray();
            body.add(layer);
            JsonObject layers = new JsonObject();
            layers.add("horse_body", body);
            JsonObject asset = new JsonObject();
            asset.add("layers", layers);
            GeneratedGates.put(files, "assets/" + NS + "/equipment/" + id + ".json", asset);

            // ---- the icon, tinted the same way ------------------------------
            // The model is the plain greyscale plate; the colour arrives through
            // client/MetalTintSource, which reads it back off the item id. Two
            // routes to one colour, because an equipment asset cannot tint an
            // inventory slot and an item tint cannot reach a worn layer.
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", NS + ":item/plate_horse_armor");
            model.add("textures", textures);
            GeneratedGates.put(files, "assets/" + NS + "/models/item/" + id + ".json", model);

            JsonObject tint = new JsonObject();
            tint.addProperty("type", NS + ":metal_tint");
            JsonArray tints = new JsonArray();
            tints.add(tint);
            JsonObject itemModel = new JsonObject();
            itemModel.addProperty("type", "minecraft:model");
            itemModel.addProperty("model", NS + ":item/" + id);
            itemModel.add("tints", tints);
            JsonObject item = new JsonObject();
            item.add("model", itemModel);
            GeneratedGates.put(files, "assets/" + NS + "/items/" + id + ".json", item);

            lang.addProperty("item." + NS + "." + id, label(metal) + " Horse Armor");

            // ---- craftable, because loot-only is not a ladder ---------------
            // Vanilla's horse armours are chest loot and this mod is not going
            // to add recipes for those (that is BHAR's job, and a collision with
            // it would make one of the two uncraftable - see
            // tools/check-recipes.mjs). These are OUR items, in our namespace,
            // so no third-party recipe can claim the same output; the hair cloth
            // keeps the house rule that every recipe carries a modded input.
            if (ModdedMaterials.itemExists(metal.itemId())) {
                GeneratedGates.put(files, "data/" + NS + "/recipe/" + id + ".json", recipe(metal, id));
            } else {
                // A common tag naming an item nobody registered - an optional
                // dependency that is not installed, most likely. The armour is
                // still sold by the metalsmith; it just cannot be forged.
                HorseGenetics.LOGGER.warn("compat: {} is in a c: tag but is not a registered item - "
                        + "{} is uncraftable", metal.itemId(), id);
            }

            // ---- and the metalsmith stocks it -------------------------------
            int tier = tierFor(metal);
            JsonObject trade = new JsonObject();
            JsonObject wants = new JsonObject();
            wants.addProperty("id", "minecraft:emerald");
            wants.addProperty("count", priceFor(tier));
            JsonObject gives = new JsonObject();
            gives.addProperty("id", NS + ":" + id);
            trade.add("wants", wants);
            trade.add("gives", gives);
            trade.addProperty("max_uses", tier >= 4 ? 2 : 4);
            trade.addProperty("xp", tier * 5);
            GeneratedGates.put(files,
                    "data/" + NS + "/villager_trade/" + METALSMITH + "/level_" + tier + "/sell_" + id + ".json",
                    trade);
            if (tierTrades[tier] == null) {
                tierTrades[tier] = new JsonObject();
                tierTrades[tier].add("ids", new JsonArray());
            }
            tierTrades[tier].getAsJsonArray("ids")
                    .add(NS + ":" + METALSMITH + "/level_" + tier + "/sell_" + id);
        }

        for (int tier = 1; tier <= 5; tier++) {
            if (tierTrades[tier] != null) {
                emitTradeSet(files, tier, tierTrades[tier].getAsJsonArray("ids"));
            }
        }
    }

    /**
     * The tier file, shipped trades plus generated ones.
     *
     * <p>Read off our own classpath rather than restated here - see the class
     * note. If the shipped file cannot be read the generated trades are written
     * on their own, which loses the vanilla armours from that tier but keeps the
     * modded ones; the alternative is a tier that is empty for everybody.
     */
    private static void emitTradeSet(Map<String, byte[]> files, int tier, JsonArray added) {
        String path = "/data/" + NS + "/trade_set/" + METALSMITH + "/level_" + tier + ".json";
        JsonObject tierFile = null;
        try (InputStream in = GeneratedArmour.class.getResourceAsStream(path)) {
            if (in != null) {
                JsonElement parsed = JsonParser.parseString(
                        new String(in.readAllBytes(), StandardCharsets.UTF_8));
                if (parsed.isJsonObject()) {
                    tierFile = parsed.getAsJsonObject();
                }
            }
        } catch (Exception unreadable) {
            HorseGenetics.LOGGER.warn("compat: could not read the shipped {} - "
                    + "its modded armours will be the only trades in that tier", path, unreadable);
        }
        if (tierFile == null) {
            tierFile = new JsonObject();
            tierFile.addProperty("amount", 3.0);
            tierFile.add("trades", new JsonArray());
            tierFile.addProperty("random_sequence", NS + ":trade_set/" + METALSMITH + "/level_" + tier);
        }
        JsonArray trades = tierFile.getAsJsonArray("trades");
        for (JsonElement id : added) {
            trades.add(id);
        }
        GeneratedGates.put(files,
                "data/" + NS + "/trade_set/" + METALSMITH + "/level_" + tier + ".json", tierFile);
    }

    private static JsonObject recipe(ModdedMaterials.Metal metal, String id) {
        JsonArray ingredients = new JsonArray();
        for (int i = 0; i < 6; i++) {
            ingredients.add(metal.itemId());
        }
        ingredients.add(NS + ":hair_cloth");

        JsonObject result = new JsonObject();
        result.addProperty("id", NS + ":" + id);

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "equipment");
        recipe.add("ingredients", ingredients);
        recipe.add("result", result);
        return recipe;
    }

    /**
     * Which of the metalsmith's five tiers stocks it.
     *
     * <p>Follows the protection {@link ModdedArmour} worked out rather than
     * inventing a second opinion: a stronger armour is a later trade, so the
     * shop's ladder and the armour's ladder are the same ladder. A player who
     * has to level him up for the good stuff should find that the good stuff is
     * good.
     */
    private static int tierFor(ModdedMaterials.Metal metal) {
        int defense = ModdedArmour.protectionFor(metal);
        if (defense <= 3) {
            return 1;
        }
        if (defense <= 5) {
            return 2;
        }
        if (defense <= 7) {
            return 3;
        }
        return defense <= 10 ? 4 : 5;
    }

    /** The shipped armours run 10-36 emeralds across the five tiers; these sit with them. */
    private static int priceFor(int tier) {
        return switch (tier) {
            case 1 -> 9;
            case 2 -> 13;
            case 3 -> 19;
            case 4 -> 27;
            default -> 38;
        };
    }

    /** "rose_gold" becomes "Rose Gold". See GeneratedGates.label for why the mod is not named. */
    private static String label(ModdedMaterials.Metal metal) {
        StringBuilder out = new StringBuilder();
        for (String word : metal.material().split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }
}
