package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.example.horsegenetics.neoforge.block.JumpWoods;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The generated half of "a showjumping rail can be made of every wood another
 * mod added".
 *
 * <h2>There is no block here any more, and that is the point</h2>
 * This used to register-and-generate a whole jump <i>block</i> per modded wood,
 * beside the shipped twelve. It does not, because there are no per-wood blocks
 * left: a jump's two woods are {@code JumpBlockEntity} data, so a new wood needs
 * no block, no item, no blockstate and no loot table. What it needs is
 * <b>models</b> - the parts {@code client/JumpModel} composes - a <b>recipe</b>
 * that stamps its name onto the crafted item, a <b>name</b>, and a <b>case</b>
 * in each of the three item definitions so its icon is right.
 *
 * <h2>There are no textures here, and that is the older trick</h2>
 * A jump has never had art of its own. Every shape is a shipped template model -
 * {@code horsegenetics:block/template_jump_<style>...} - and a wood's models are
 * each one line: a parent and a texture id. So a modded wood needs no generated
 * PNG at all; it needs a pointer at the plank texture <i>that mod already
 * drew</i>, exactly as the shipped twelve point at vanilla's.
 *
 * <h2>This file and bake-jumps.mjs must agree</h2>
 * {@code neoforge-26.1.2/tools/bake-jumps.mjs} writes the same kinds of file
 * for vanilla's twelve, at author time, and this writes them for modded woods
 * at run time. <b>They are the same contract in two languages and they can
 * drift.</b> If you change the shape of a model, the item definition or the
 * recipe in one, change it in the other - the symptom of missing is a modded
 * jump that is a purple chequerboard while every vanilla one is fine, with
 * nothing logged anywhere.
 *
 * <p>The item definitions are the one place this <b>replaces</b> a shipped file
 * rather than adding beside it: the generated pack sits at
 * {@code Pack.Position.TOP}, and the three {@code items/jump*.json} it writes
 * carry vanilla's twelve cases <i>and</i> the modded ones. A select case list
 * cannot be merged across packs, so it has to be rewritten whole.
 *
 * @see GeneratedGates which this is deliberately a copy of, and whose
 *      {@code put} it reuses
 */
final class GeneratedJumps {

    /** Must match {@code Jumps.BASE} and {@code STYLE} in bake-jumps.mjs. */
    static final String STYLE = "jump";
    private static final String NS = HorseGenetics.MOD_ID;

    /** Must match {@code RECIPE_YIELD} in bake-jumps.mjs. */
    private static final int RECIPE_YIELD = 4;

    /** Every connection suffix, in the order the models are written. */
    private static final String[] CONNECTIONS = {"", "_l", "_r", "_lr", "_lr_post"};

    /** Must match {@code JumpBlock.Style}, in the same order. */
    private static final String[] STYLES = {"vertical", "oxer", "crossrails"};

    /**
     * The item id suffix for each style, in {@link #STYLES} order. The vertical
     * is the bare {@code jump}. {@code STYLE_ITEM} in bake-jumps.mjs is the
     * twin.
     */
    private static final String[] STYLE_SUFFIX = {"", "_oxer", "_crossrails"};

    /** Must match {@code RECIPE_FENCES} in bake-jumps.mjs. */
    private static final int RECIPE_FENCES = 3;

    private GeneratedJumps() {
    }

    static void emit(Map<String, byte[]> files, JsonObject lang) {
        List<ModdedMaterials.Wood> woods = ModdedMaterials.woods();
        if (woods.isEmpty()) {
            return;
        }

        for (ModdedMaterials.Wood wood : woods) {
            // The wood KEY, which is what a jump stores and what every model id
            // is composed from. jumpId() is that key plus "_jump", and the model
            // names follow the shipped ones exactly: <key>_jump_<style>_rails.
            String id = wood.jumpId();

            // What the screen and every item name call this wood.
            // JumpWoods.label reads this key, and title-cases the wood key
            // itself if it is missing - so a wood whose mod ships no lang is
            // still legible, just less prettily.
            lang.addProperty(NS + ".wood." + wood.namespace() + "_" + wood.name(), label(wood));

            for (String style : STYLES) {
                // The icon model: a whole jump of one wood, both standards.
                put(files, id + "_" + style, "template_" + STYLE + "_" + style, wood);
                // The two halves the block's model composes. The suffix names
                // the CONNECTIONS, not the standards: "_l" means a jump
                // continues the rail to the left, so the left standard is the
                // one that is gone, "_lr" is a bare rail mid-run, and
                // "_lr_post" is that rail carrying an intermediate upright.
                put(files, id + "_" + style + "_rails",
                        "template_jump_" + style + "_rails", wood);
                for (String suffix : CONNECTIONS) {
                    put(files, id + "_" + style + "_standards" + suffix,
                            "template_jump_" + style + "_standards" + suffix, wood);
                }
            }

            // THE RECIPE NEEDS THAT MOD'S FENCE, AND Wood.fenceId IS COMPOSED
            // RATHER THAN SCANNED - the material scan looks for fence GATES,
            // because that is what the double gate needed. So this is the one
            // place the guess gets checked, and a recipe naming an item that is
            // not registered does not fail quietly: it fails to parse and takes
            // the rest of the generated pack with it.
            if (ModdedMaterials.itemExists(wood.fenceId())) {
                GeneratedGates.put(files, "data/" + NS + "/recipe/" + STYLE + "_"
                        + wood.namespace() + "_" + wood.name() + ".json", recipe(wood));
            } else {
                HorseGenetics.LOGGER.warn("compat: {} ships a fence gate but no {} - "
                        + "jumps cannot be crafted in that wood", wood.namespace(), wood.fenceId());
            }
        }

        // ---- the three item definitions, rewritten whole --------------------
        // JumpWoods.keys() is vanilla's twelve and then every modded wood, and
        // is the same list the model bakes parts for - so an icon case exists
        // for exactly the woods a jump can actually be.
        for (int i = 0; i < STYLES.length; i++) {
            GeneratedGates.put(files,
                    "assets/" + NS + "/items/" + STYLE + STYLE_SUFFIX[i] + ".json",
                    itemDefinition(STYLES[i]));
        }
    }

    /** One model file: a parent and a plank texture, which is all a wood needs. */
    private static void put(Map<String, byte[]> files, String name, String template,
                            ModdedMaterials.Wood wood) {
        JsonObject textures = new JsonObject();
        textures.addProperty("texture", wood.plankTexture());
        JsonObject model = new JsonObject();
        model.addProperty("parent", NS + ":block/" + template);
        model.add("textures", textures);
        GeneratedGates.put(files, "assets/" + NS + "/models/block/" + name + ".json", model);
    }

    /**
     * One style's item definition: a {@code minecraft:select} on the rails
     * component, with a case per wood.
     *
     * <p>Selecting on the <b>rails</b> alone rather than on a pair-valued
     * component is what keeps this at one case per wood instead of one per
     * pair; the icon is therefore always right about the rails and says nothing
     * about the standards, which is the correct trade at sixteen pixels. The
     * fallback is oak, so a jump carrying no components at all - a {@code /give}
     * - draws as one rather than as a missing model.
     */
    private static JsonObject itemDefinition(String style) {
        JsonArray cases = new JsonArray();
        for (String wood : JumpWoods.keys()) {
            JsonObject one = new JsonObject();
            one.addProperty("when", wood);
            one.add("model", styleModel(wood, style));
            cases.add(one);
        }

        JsonObject select = new JsonObject();
        select.addProperty("type", "minecraft:select");
        select.addProperty("property", "minecraft:component");
        select.addProperty("component", NS + ":jump_rails");
        select.add("cases", cases);
        select.add("fallback", styleModel("oak", style));

        JsonObject definition = new JsonObject();
        definition.add("model", select);
        return definition;
    }

    private static JsonObject styleModel(String wood, String style) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", NS + ":block/" + wood + "_" + STYLE + "_" + style);
        return model;
    }

    /**
     * Three of that mod's fences and one raw horse hair, yielding four - of the
     * <b>one</b> jump item, stamped with this wood.
     *
     * <p>The wood is not in the result's id any more, it is in its
     * {@code components}. That is the whole reason twelve-plus recipes can make
     * one item and it still comes out fir.
     *
     * <p>The hair is what keeps this inside the house rule
     * ({@code wiki/items.html#rules}) rather than making the jumps a second
     * documented exception beside the double gates - the gates' argument was
     * that nothing in vanilla consumes a fence <i>gate</i>, and that does not
     * carry over to plain fences. The yield is what keeps the hair from being a
     * tax: four jumps a hair, so a twelve-fence course costs three.
     */
    private static JsonObject recipe(ModdedMaterials.Wood wood) {
        JsonArray ingredients = new JsonArray();
        for (int i = 0; i < RECIPE_FENCES; i++) {
            ingredients.add(wood.fenceId());
        }
        ingredients.add(NS + ":horse_hair");

        String key = wood.namespace() + "_" + wood.name();
        JsonObject components = new JsonObject();
        components.addProperty(NS + ":jump_rails", key);
        components.addProperty(NS + ":jump_standards", key);

        JsonObject result = new JsonObject();
        result.addProperty("count", RECIPE_YIELD);
        result.addProperty("id", NS + ":" + STYLE);
        result.add("components", components);

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
}
