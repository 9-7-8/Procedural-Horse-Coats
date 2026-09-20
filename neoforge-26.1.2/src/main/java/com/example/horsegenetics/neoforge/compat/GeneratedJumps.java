package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The generated half of "a showjumping rail for every wood another mod added".
 *
 * <p>The registered half is in {@code block/Jumps}; this writes the files that
 * make one visible, placeable, craftable and breakable.
 *
 * <h2>There are no textures here, and that is the whole trick</h2>
 * A jump has never had art of its own. Every shape is a shipped template
 * model - {@code horsegenetics:block/template_jump_<style><connection>} - and a
 * wood's models are each one line: a parent and a texture id. So a modded wood needs
 * no generated PNG at all; it needs a pointer at the plank texture <i>that mod
 * already drew</i>, exactly as the shipped twelve point at vanilla's.
 *
 * <h2>This file and bake-jumps.mjs must agree</h2>
 * {@code neoforge-26.1.2/tools/bake-jumps.mjs} writes the same kinds of file
 * for vanilla's twelve, at author time, and this writes them for modded woods
 * at run time. <b>They are the same contract in two languages and they can
 * drift.</b> If you change the shape of a blockstate, a model, the item
 * definition, the loot table or the recipe in one, change it in the other - the
 * symptom of missing is a modded jump that is a purple chequerboard while every
 * vanilla one is fine, with nothing logged anywhere.
 *
 * @see GeneratedGates which this is deliberately a copy of, and whose
 *      {@code put} and {@code mergeBlockTag} it reuses
 */
final class GeneratedJumps {

    /** Must match {@code Jumps.STYLE}. */
    static final String STYLE = "jump";
    private static final String NS = HorseGenetics.MOD_ID;

    /** Must match {@code RECIPE_YIELD} in bake-jumps.mjs. */
    private static final int RECIPE_YIELD = 4;

    /** Authored for facing=south; blockstate y rotation maps clockwise. */
    private static final Map<String, Integer> ROTATION = Map.of(
            "south", 0, "west", 90, "north", 180, "east", 270);

    /** Every connection suffix, in the order the models are written. */
    private static final String[] CONNECTIONS = {"", "_l", "_r", "_lr", "_lr_post"};

    /** Must match {@code JumpBlock.Style}, in the same order. */
    private static final String[] STYLES = {"vertical", "oxer", "crossrails"};

    /**
     * The item id suffix and display label for each style, in {@link #STYLES}
     * order. The vertical is the bare id and takes its name from the block;
     * the other two need names of their own. {@code STYLE_ITEM} in
     * bake-jumps.mjs is the twin.
     */
    private static final String[] STYLE_SUFFIX = {"", "_oxer", "_crossrails"};
    private static final String[] STYLE_LABEL = {null, "Oxer", "Crossrails"};

    /** Must match {@code RECIPE_FENCES} in bake-jumps.mjs. */
    private static final int RECIPE_FENCES = 3;

    /**
     * The model suffix for a set of flags.
     *
     * <p><b>One method, called from both the model loop and the blockstate
     * loop</b>, because writing it out twice is how this shipped broken the
     * first time: composing {@code (left ? "_l" : "") + (right ? "_r" : "")}
     * gives {@code "_l_r"}, the models are named {@code "_lr"}, and the
     * mismatch is invisible until somebody places three jumps in a row and the
     * middle one is a purple cube. Only the both-connected case differs, so a
     * row of two looks perfect and the bug hides. The author-time twin is
     * {@code suffixFor} in bake-jumps.mjs.
     *
     * <p>{@code post} is honoured only mid-run, matching
     * {@code JumpBlock.withPost}, which never sets it otherwise. The blockstate
     * still has to name every combination, so the end-of-run states map to the
     * postless model rather than to one that would then have to exist.
     */
    private static String suffix(boolean left, boolean right, boolean post) {
        if (left && right) {
            return post ? "_lr_post" : "_lr";
        }
        if (left) {
            return "_l";
        }
        return right ? "_r" : "";
    }

    private GeneratedJumps() {
    }

    static void emit(Map<String, byte[]> files, JsonObject lang) {
        List<ModdedMaterials.Wood> woods = ModdedMaterials.woods();
        if (woods.isEmpty()) {
            return;
        }
        JsonArray tagValues = new JsonArray();

        for (ModdedMaterials.Wood wood : woods) {
            String id = wood.jumpId();
            tagValues.add(NS + ":" + id);
            lang.addProperty("block." + NS + "." + id, label(wood) + " Jump");

            // ---- one model per style per connection state --------------------
            // The suffix names the CONNECTIONS, not the standards: "_l" means a
            // jump continues the rail to the left, so the left standard is the
            // one that is gone, "_lr" is a bare rail mid-run, and "_lr_post"
            // is that rail carrying an intermediate upright.
            for (String style : STYLES) {
                for (String suffix : CONNECTIONS) {
                    JsonObject model = new JsonObject();
                    model.addProperty("parent",
                            NS + ":block/template_" + STYLE + "_" + style + suffix);
                    JsonObject textures = new JsonObject();
                    textures.addProperty("texture", wood.plankTexture());
                    model.add("textures", textures);
                    put(files, "assets/" + NS + "/models/block/" + id + "_" + style + suffix
                            + ".json", model);
                }
            }

            // ---- 3 styles x 4 facings x left x right x post = 96 variants -----
            JsonObject variants = new JsonObject();
            for (String style : STYLES) {
                for (Map.Entry<String, Integer> facing : ROTATION.entrySet()) {
                    for (boolean left : new boolean[] {false, true}) {
                        for (boolean right : new boolean[] {false, true}) {
                            for (boolean post : new boolean[] {false, true}) {
                                JsonObject variant = new JsonObject();
                                variant.addProperty("model", NS + ":block/" + id + "_" + style
                                        + suffix(left, right, post));
                                // NOT uvlocked on the crossrails: uvlock re-projects
                                // a face's UVs against the block axes after the
                                // blockstate's y rotation, and on an element that is
                                // ITSELF rotated 45 degrees the two fight and the
                                // grain shears. bake-jumps.mjs does the same.
                                variant.addProperty("uvlock", !"crossrails".equals(style));
                                if (facing.getValue() != 0) {
                                    variant.addProperty("y", facing.getValue());
                                }
                                variants.add("facing=" + facing.getKey() + ",left=" + left
                                        + ",post=" + post + ",right=" + right
                                        + ",style=" + style, variant);
                            }
                        }
                    }
                }
            }
            JsonObject blockstate = new JsonObject();
            blockstate.add("variants", variants);
            put(files, "assets/" + NS + "/blockstates/" + id + ".json", blockstate);

            // ---- one item per style ------------------------------------------
            // Each points at that style's both-standards model, the only one of
            // the five given a gui transform in the shipped template.
            for (int i = 0; i < STYLES.length; i++) {
                JsonObject itemModel = new JsonObject();
                itemModel.addProperty("type", "minecraft:model");
                itemModel.addProperty("model", NS + ":block/" + id + "_" + STYLES[i]);
                JsonObject item = new JsonObject();
                item.add("model", itemModel);
                put(files, "assets/" + NS + "/items/" + id + STYLE_SUFFIX[i] + ".json", item);
                if (STYLE_LABEL[i] != null) {
                    lang.addProperty("item." + NS + "." + id + STYLE_SUFFIX[i],
                            label(wood) + " " + STYLE_LABEL[i]);
                }
            }

            put(files, "data/" + NS + "/loot_table/blocks/" + id + ".json", blockLoot(id));

            // THE RECIPE NEEDS THAT MOD'S FENCE, AND Wood.fenceId IS COMPOSED
            // RATHER THAN SCANNED - the material scan looks for fence GATES,
            // because that is what the double gate needed. So this is the one
            // place the guess gets checked, and a recipe naming an item that is
            // not registered does not fail quietly: it fails to parse and takes
            // the rest of the generated pack with it.
            if (ModdedMaterials.itemExists(wood.fenceId())) {
                put(files, "data/" + NS + "/recipe/" + id + ".json", recipe(wood, id));
            } else {
                HorseGenetics.LOGGER.warn("compat: {} ships a fence gate but no {} - "
                        + "the jump is uncraftable", wood.namespace(), wood.fenceId());
            }
        }

        // ---- the vanilla block tag -----------------------------------------
        // Additive twice over: tag files MERGE across datapacks, so this
        // contributes the modded jumps beside the shipped twelve rather than
        // replacing them; and mergeBlockTag keeps it from stamping on the
        // double gates, which are in the same tag and the same file map.
        GeneratedGates.mergeBlockTag(files, "data/minecraft/tags/block/mineable/axe.json", tagValues);
    }

    /**
     * An ordinary single-drop table.
     *
     * <p>Unlike {@link GeneratedGates#blockLoot} there is no {@code half}
     * condition, and there must not be one: a jump is <b>one</b> block, so
     * there is no partner to orphan and nothing that could pay out twice. The
     * conditions here select the <i>style</i>, not a half.
     */
    private static JsonObject blockLoot(String id) {
        // ONE DROP, BUT THE RIGHT STYLE'S ITEM. A pool with rolls:1 picks among
        // the entries whose conditions pass, and exactly one style condition
        // can pass, so this is a switch rather than a lottery. Without it,
        // breaking an oxer hands back a vertical and the style is quietly lost.
        JsonArray entries = new JsonArray();
        for (int i = 0; i < STYLES.length; i++) {
            JsonObject styleProperty = new JsonObject();
            styleProperty.addProperty("style", STYLES[i]);
            JsonObject isStyle = new JsonObject();
            isStyle.addProperty("condition", "minecraft:block_state_property");
            isStyle.addProperty("block", NS + ":" + id);
            isStyle.add("properties", styleProperty);
            JsonArray entryConditions = new JsonArray();
            entryConditions.add(isStyle);

            JsonObject entry = new JsonObject();
            entry.addProperty("type", "minecraft:item");
            entry.addProperty("name", NS + ":" + id + STYLE_SUFFIX[i]);
            entry.add("conditions", entryConditions);
            entries.add(entry);
        }

        JsonObject survives = new JsonObject();
        survives.addProperty("condition", "minecraft:survives_explosion");
        JsonArray conditions = new JsonArray();
        conditions.add(survives);

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        pool.add("entries", entries);
        pool.add("conditions", conditions);
        JsonArray pools = new JsonArray();
        pools.add(pool);

        JsonObject loot = new JsonObject();
        loot.addProperty("type", "minecraft:block");
        loot.add("pools", pools);
        return loot;
    }

    /**
     * Three of that mod's fences and one raw horse hair, yielding four.
     *
     * <p>The hair is what keeps this inside the house rule
     * ({@code wiki/items.html#rules}) rather than making the jumps a second
     * documented exception beside the double gates - the gates' argument was
     * that nothing in vanilla consumes a fence <i>gate</i>, and that does not
     * carry over to plain fences. The yield is what keeps the hair from being a
     * tax: four jumps a hair, so a twelve-fence course costs three.
     */
    private static JsonObject recipe(ModdedMaterials.Wood wood, String id) {
        JsonArray ingredients = new JsonArray();
        for (int i = 0; i < RECIPE_FENCES; i++) {
            ingredients.add(wood.fenceId());
        }
        ingredients.add(NS + ":horse_hair");

        JsonObject result = new JsonObject();
        result.addProperty("count", RECIPE_YIELD);
        result.addProperty("id", NS + ":" + id);

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "building");
        recipe.add("ingredients", ingredients);
        recipe.add("result", result);
        return recipe;
    }

    /**
     * "blue_oak" becomes "Blue Oak".
     *
     * <p>The mod's name is deliberately <b>not</b> in it, for the reason spelled
     * out on {@link GeneratedGates#label}: two mods both adding a maple is rarer
     * than one mod adding a maple, and "Biomes You'll Go Maple Jump" is worse to
     * read for the common case. The ids differ regardless.
     */
    private static String label(ModdedMaterials.Wood wood) {
        StringBuilder out = new StringBuilder();
        for (String word : wood.name().split("_")) {
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

    private static void put(Map<String, byte[]> files, String path, JsonObject json) {
        GeneratedGates.put(files, path, json);
    }
}
