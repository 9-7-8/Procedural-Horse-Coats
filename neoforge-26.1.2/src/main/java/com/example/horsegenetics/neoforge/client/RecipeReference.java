package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>The mod's own recipes, for reading rather than for making.</b> Loaded from
 * {@code assets/horsegenetics/recipe_reference.json}, which
 * {@code tools/bake-recipe-reference.mjs} generates from the real recipe files
 * in {@code data/}.
 *
 * <h2>Why a baked file and not the recipe manager</h2>
 * The browser needs ingredients to draw, and the client cannot dependably get
 * them: {@code data/} is datapack territory, and the client-side recipe API in
 * 26.1.2 hands out display objects rather than the recipes themselves. A
 * generated summary read straight off the classpath needs neither, and cannot
 * drift from the recipes it was generated from - only go stale, which is what
 * the regenerate table in CLAUDE.md is for.
 *
 * <p><b>The trade is honest and worth writing down:</b> this describes the
 * recipes <i>this jar ships</i>, not the recipes the server is running. A
 * datapack that changes one of them will be right in the crafting table and
 * wrong here. Nothing in the mod does that today.
 *
 * <h2>Ingredients are resolved late</h2>
 * Item ids become {@link ItemStack}s on first use, not at load: the registry is
 * not necessarily populated when this class is first touched. A tag
 * ({@code #minecraft:fences}) resolves to its first member, or to nothing if the
 * tag is unknown - {@code getTagOrEmpty} never throws.
 */
public final class RecipeReference {

    /** One recipe, as much of it as a reference needs. */
    public record Entry(String id, Kind kind, List<String> grid, String result, int count) {

        /** Nine cells, row-major; {@code null} where the cell is empty. */
        public List<ItemStack> gridStacks() {
            List<ItemStack> out = new ArrayList<>(9);
            for (String cell : grid) {
                out.add(cell == null ? ItemStack.EMPTY : stackOf(cell));
            }
            while (out.size() < 9) {
                out.add(ItemStack.EMPTY);
            }
            return out;
        }

        public ItemStack resultStack() {
            ItemStack stack = stackOf(result);
            stack.setCount(count);
            return stack;
        }
    }

    public enum Kind {
        /** Ingredients anywhere in the grid. */
        SHAPELESS,
        /** Ingredients in the arrangement given. */
        SHAPED,
        /** A {@code CustomRecipe} whose inputs are computed in Java. */
        CUSTOM
    }

    private static final String RESOURCE = "/assets/horsegenetics/recipe_reference.json";

    private static List<Entry> entries;

    private RecipeReference() {
    }

    /** Every recipe in the file, in id order. Empty if the file is missing or bad. */
    public static synchronized List<Entry> all() {
        if (entries == null) {
            entries = load();
        }
        return entries;
    }

    /** The one entry with this id, or {@code null}. */
    public static Entry byId(String id) {
        for (Entry e : all()) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        return null;
    }

    private static List<Entry> load() {
        try (InputStream in = RecipeReference.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                HorseGenetics.LOGGER.warn("[recipes] {} is missing - the Recipes tab will be empty. "
                        + "Run tools/bake-recipe-reference.mjs.", RESOURCE);
                return List.of();
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Entry> out = new ArrayList<>();
            for (JsonElement el : root.getAsJsonArray("recipes")) {
                JsonObject o = el.getAsJsonObject();
                String id = o.get("id").getAsString();
                String kindName = o.get("kind").getAsString();
                switch (kindName) {
                    case "shapeless" -> out.add(new Entry(id, Kind.SHAPELESS,
                            padded(strings(o.getAsJsonArray("in"))),
                            o.get("out").getAsString(), o.get("count").getAsInt()));
                    case "shaped" -> out.add(new Entry(id, Kind.SHAPED,
                            padded(strings(o.getAsJsonArray("grid"))),
                            o.get("out").getAsString(), o.get("count").getAsInt()));
                    case "custom" -> out.add(new Entry(id, Kind.CUSTOM, List.of(), "", 0));
                    default -> HorseGenetics.LOGGER.warn("[recipes] {}: unknown kind {}", id, kindName);
                }
            }
            return List.copyOf(out);
        } catch (IOException | RuntimeException e) {
            // A broken reference must not cost the player the browser.
            HorseGenetics.LOGGER.warn("[recipes] could not read {}", RESOURCE, e);
            return List.of();
        }
    }

    private static List<String> strings(JsonArray array) {
        List<String> out = new ArrayList<>(array.size());
        for (JsonElement el : array) {
            out.add(el.isJsonNull() ? null : el.getAsString());
        }
        return out;
    }

    /** Nine cells, so a caller never has to think about how many there were. */
    private static List<String> padded(List<String> cells) {
        List<String> out = new ArrayList<>(cells);
        while (out.size() < 9) {
            out.add(null);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * An item id or a {@code #tag} as a stack. An unknown id gives
     * {@link ItemStack#EMPTY} rather than throwing - the reference is drawn, and
     * a blank cell is a better failure than a crashed screen.
     */
    static ItemStack stackOf(String id) {
        if (id == null || id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (id.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(id.substring(1));
            if (tagId == null) {
                return ItemStack.EMPTY;
            }
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(
                    TagKey.create(Registries.ITEM, tagId))) {
                return new ItemStack(holder);
            }
            return ItemStack.EMPTY;
        }
        Identifier itemId = Identifier.tryParse(id);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
