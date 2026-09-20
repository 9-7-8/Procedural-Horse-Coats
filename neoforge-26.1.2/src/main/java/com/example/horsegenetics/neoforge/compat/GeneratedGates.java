package com.example.horsegenetics.neoforge.compat;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The generated half of "a double gate for every wood another mod added".
 *
 * <p>The registered half is in {@code block/DoubleGates}; this writes the files
 * that make one visible, placeable, craftable and breakable.
 *
 * <h2>There are no textures here, and that is the whole trick</h2>
 * A double gate has never had art of its own. The eight shapes are shipped
 * template models - {@code horsegenetics:block/template_double_fence_gate*} -
 * and a wood's eight models are each one line: a parent and a texture id. So a
 * modded wood needs no generated PNG at all; it needs a pointer at the plank
 * texture <i>that mod already drew</i>, exactly as the shipped twelve point at
 * vanilla's.
 *
 * <h2>This file and bake-double-gates.mjs must agree</h2>
 * {@code neoforge-26.1.2/tools/bake-double-gates.mjs} writes the same nine kinds
 * of file for vanilla's twelve, at author time, and this writes them for modded
 * woods at run time. <b>They are the same contract in two languages and they can
 * drift.</b> If you change the shape of a blockstate, a model, the item
 * definition, the loot table or the recipe in one, change it in the other - the
 * symptom of missing is a modded gate that is a purple chequerboard while every
 * vanilla one is fine, with nothing logged anywhere.
 */
final class GeneratedGates {

    /** Must match {@code DoubleGates.STYLE}. */
    static final String STYLE = "double_fence_gate";
    private static final String NS = HorseGenetics.MOD_ID;

    private static final Map<String, Integer> ROTATION = Map.of(
            "south", 0, "west", 90, "north", 180, "east", 270);

    private GeneratedGates() {
    }

    static void emit(Map<String, byte[]> files, JsonObject lang) {
        List<ModdedMaterials.Wood> woods = ModdedMaterials.woods();
        if (woods.isEmpty()) {
            return;
        }
        JsonArray tagValues = new JsonArray();

        for (ModdedMaterials.Wood wood : woods) {
            String id = wood.doubleGateId();
            tagValues.add(NS + ":" + id);
            lang.addProperty("block." + NS + "." + id, label(wood) + " Double Fence Gate");

            // ---- eight models, one line of difference each ------------------
            for (String half : new String[] {"left", "right"}) {
                for (boolean open : new boolean[] {false, true}) {
                    for (boolean inWall : new boolean[] {false, true}) {
                        String suffix = "_" + half + (inWall ? "_wall" : "") + (open ? "_open" : "");
                        JsonObject model = new JsonObject();
                        model.addProperty("parent", NS + ":block/template_" + STYLE + suffix);
                        JsonObject textures = new JsonObject();
                        textures.addProperty("texture", wood.plankTexture());
                        model.add("textures", textures);
                        put(files, "assets/" + NS + "/models/block/" + id + suffix + ".json", model);
                    }
                }
            }

            // ---- 4 facings x in_wall x open x half = 32 variants -------------
            JsonObject variants = new JsonObject();
            for (Map.Entry<String, Integer> facing : ROTATION.entrySet()) {
                for (boolean inWall : new boolean[] {false, true}) {
                    for (boolean open : new boolean[] {false, true}) {
                        for (String half : new String[] {"left", "right"}) {
                            String suffix = "_" + half + (inWall ? "_wall" : "") + (open ? "_open" : "");
                            JsonObject variant = new JsonObject();
                            variant.addProperty("model", NS + ":block/" + id + suffix);
                            variant.addProperty("uvlock", true);
                            if (facing.getValue() != 0) {
                                variant.addProperty("y", facing.getValue());
                            }
                            variants.add("facing=" + facing.getKey() + ",half=" + half
                                    + ",in_wall=" + inWall + ",open=" + open, variant);
                        }
                    }
                }
            }
            JsonObject blockstate = new JsonObject();
            blockstate.add("variants", variants);
            put(files, "assets/" + NS + "/blockstates/" + id + ".json", blockstate);

            // ---- the item icon is the LEFT half -----------------------------
            // There is no bare `<id>` model to point at: a double gate is two
            // blocks and neither is "the" block. Pointing an item definition at
            // a model nobody wrote is this layer's signature silent failure -
            // nothing logs, and the slot is a purple chequerboard. The left half
            // is the one the template gives vanilla's gate gui transform to.
            JsonObject itemModel = new JsonObject();
            itemModel.addProperty("type", "minecraft:model");
            itemModel.addProperty("model", NS + ":block/" + id + "_left");
            JsonObject item = new JsonObject();
            item.add("model", itemModel);
            put(files, "assets/" + NS + "/items/" + id + ".json", item);

            put(files, "data/" + NS + "/loot_table/blocks/" + id + ".json", blockLoot(id));

            // THE DOUBLE GATE IS THE HOUSE RULE'S ONE EXCEPTION, here as well as
            // in bake-double-gates.mjs - two of that mod's own gates and nothing
            // else. It carried our braided rope until the rope was removed for
            // being a twelve-hair tax on a building block. The ingredient is
            // still that mod's, so the recipe stays unreachable until the player
            // has the wood without anything having to check that they do; what
            // it no longer carries is an ingredient of OURS. See the long note
            // in bake-double-gates.mjs for why the collision risk is accepted.
            if (ModdedMaterials.itemExists(wood.gateId())) {
                put(files, "data/" + NS + "/recipe/" + id + ".json", recipe(wood, id));
            } else {
                // Declared in their assets, not in their registry - see
                // ModdedMaterials.itemExists. The gate still exists and still
                // draws; it simply cannot be crafted from something that is not
                // there, and a recipe saying otherwise would only fail to parse.
                HorseGenetics.LOGGER.warn("compat: {} declares {} but never registers it - "
                        + "the double gate is uncraftable", wood.namespace(), wood.gateId());
            }
        }

        // ---- the two vanilla block tags ------------------------------------
        // Additive: tag files MERGE across datapacks unless one sets "replace",
        // so this contributes the modded gates beside the shipped twelve rather
        // than replacing them. That is also why this may not simply copy the
        // shipped file and add to it - two packs both listing the vanilla twelve
        // is harmless, but a generated file that went stale would then be
        // asserting things about woods that are no longer installed.
        JsonObject tag = new JsonObject();
        tag.add("values", tagValues);
        put(files, "data/minecraft/tags/block/fence_gates.json", tag);
        // mineable/axe is shared with the showjumping rails, and this map has
        // one entry per path - a plain put here would drop whichever family
        // ran first. See mergeBlockTag.
        mergeBlockTag(files, "data/minecraft/tags/block/mineable/axe.json", tagValues);
    }

    /**
     * Add {@code values} to a block tag already in {@code files}, rather than
     * replacing it.
     *
     * <p><b>More than one generator contributes to {@code mineable/axe}</b> -
     * the double gates and the jumps today - and a datapack has exactly one
     * file per tag, so the last plain {@code put} would win and the other
     * family would quietly lose its tool. That failure is invisible: nothing
     * logs, nothing is a chequerboard, the blocks simply mine slowly and drop
     * nothing without an axe, which reads as a balance choice rather than a
     * bug. Every generator touching a shared tag must come through here.
     *
     * <p>Order-independent on purpose, so it does not matter which generator
     * {@code GeneratedPack.build} happens to call first.
     */
    static void mergeBlockTag(Map<String, byte[]> files, String path, JsonArray values) {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        byte[] prior = files.get(path);
        if (prior != null) {
            JsonObject existing = com.google.gson.JsonParser
                    .parseString(new String(prior, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if (existing.has("values")) {
                for (com.google.gson.JsonElement e : existing.getAsJsonArray("values")) {
                    seen.add(e.getAsString());
                }
            }
        }
        for (com.google.gson.JsonElement e : values) {
            seen.add(e.getAsString());
        }
        JsonArray merged = new JsonArray();
        for (String value : seen) {
            merged.add(value);
        }
        JsonObject tag = new JsonObject();
        tag.add("values", merged);
        put(files, path, tag);
    }

    /**
     * <b>One item out, and the {@code half=left} condition is the whole of it.</b>
     * A pair is two blocks, so breaking one runs two removals - the half that was
     * struck, and the orphan its partner becomes, which
     * {@code DoubleFenceGateBlock.updateShape} turns to air. That second removal
     * is <em>not</em> free of drops: {@code Block.updateOrDestroy} calls
     * {@code destroyBlock(pos, (flags & 32) == 0)} and an ordinary neighbour
     * update carries no {@code UPDATE_SUPPRESS_DROPS}, so an unconditional table
     * pays out twice and the gate duplicates on every break. Vanilla's doors and
     * beds fix this here rather than in Java - see
     * {@code data/minecraft/loot_table/blocks/oak_door.json}, conditioned on
     * {@code half=lower}. Only the loot-carrying half may pay, whichever half was
     * struck. <b>The author-time twin is {@code tools/bake-double-gates.mjs}.</b>
     */
    private static JsonObject blockLoot(String id) {
        JsonObject half = new JsonObject();
        half.addProperty("half", "left");
        JsonObject onlyLeft = new JsonObject();
        onlyLeft.addProperty("condition", "minecraft:block_state_property");
        onlyLeft.addProperty("block", NS + ":" + id);
        onlyLeft.add("properties", half);
        JsonArray entryConditions = new JsonArray();
        entryConditions.add(onlyLeft);

        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", NS + ":" + id);
        entry.add("conditions", entryConditions);
        JsonArray entries = new JsonArray();
        entries.add(entry);

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

    private static JsonObject recipe(ModdedMaterials.Wood wood, String id) {
        JsonArray ingredients = new JsonArray();
        ingredients.add(wood.gateId());
        ingredients.add(wood.gateId());

        JsonObject result = new JsonObject();
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
     * <p>The mod's name is deliberately <b>not</b> in it, even though two mods
     * may both add a maple and both gates will then read "Maple Double Fence
     * Gate". Their <i>ids</i> differ - the namespace is the first segment of
     * ours - so nothing is ambiguous to the game, only to the eye, and an item
     * called "Biomes You'll Go Maple Double Fence Gate" is worse to read for the
     * far commoner case where there is only one maple.
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

    static void put(Map<String, byte[]> files, String path, JsonObject json) {
        files.put(path, (json.toString() + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
