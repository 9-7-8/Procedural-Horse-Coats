package com.example.horsegenetics.neoforge.block;

import com.example.horsegenetics.neoforge.compat.ModdedMaterials;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <b>Every wood a jump can be made of</b>, and the plank that stands for each.
 *
 * <p>Vanilla's twelve in vanilla's order, then every wood another mod brought,
 * keyed the way {@code ModdedMaterials.Wood.jumpId} keys them. The list is the
 * same on both sides, because it is read out of the installed jars rather than
 * off a registry - see {@code compat/ModdedMaterials} for why that matters.
 *
 * <h2>This is on the server too, and it has to be</h2>
 * It lived in {@code client/} while the only thing that read it was
 * {@link com.example.horsegenetics.neoforge.client.JumpModel}. The jump's screen
 * moved it: {@code JumpMenu} runs on the server and has to turn the plank a
 * player just put in a slot into a wood key, and turn a wood key back into the
 * plank it hands them. Same list, both sides, one class.
 *
 * <h2>The two maps are built lazily, and that is not an optimisation</h2>
 * {@link #plankOf} and {@link #keyOfPlank} walk the ITEM registry, which does
 * not exist yet while {@link Jumps} is registering in a static initialiser.
 * Building them on first use is the same dodge {@code Jumps.byPlank} used
 * before it, and for the same reason.
 */
public final class JumpWoods {

    /** Vanilla's twelve, in vanilla's order. Must match {@code bake-jumps.mjs}. */
    private static final String[] VANILLA = {
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "pale_oak", "mangrove", "cherry", "bamboo", "crimson", "warped",
    };

    private static volatile @Nullable List<String> keys;
    private static volatile @Nullable Map<String, Item> planks;
    private static volatile @Nullable Map<Item, String> byPlank;

    /**
     * Every wood key, vanilla first.
     *
     * <p>A modded wood's key is {@code <namespace>_<name>}, matching the model
     * files {@code compat/GeneratedJumps} writes for it - so a wood whose mod is
     * not installed simply is not in the list, and a jump still carrying it
     * falls back to the default rather than drawing nothing.
     */
    public static List<String> keys() {
        List<String> cached = keys;
        if (cached == null) {
            List<String> built = new ArrayList<>(VANILLA.length + 8);
            for (String wood : VANILLA) {
                built.add(wood);
            }
            for (ModdedMaterials.Wood wood : ModdedMaterials.woods()) {
                built.add(wood.namespace() + "_" + wood.name());
            }
            cached = List.copyOf(built);
            keys = cached;
        }
        return cached;
    }

    /**
     * <b>The plank that stands for a wood</b>, or null if that wood's mod is
     * gone.
     *
     * <p>Vanilla's are {@code minecraft:<wood>_planks}. A modded wood's plank
     * item id is derived from the plank TEXTURE the material scan recorded -
     * {@code "ns:block/fir_planks"} names the item {@code "ns:fir_planks"} -
     * which is composed rather than scanned, exactly as {@code Wood.fenceId} is.
     * So <b>every caller has to tolerate a null</b>: an unusually-named modded
     * wood has a jump you can place but no plank to swap it with.
     */
    public static @Nullable Item plankOf(String wood) {
        return plankMap().get(wood);
    }

    /**
     * <b>The wood a plank stands for</b>, or null if this item is not a plank
     * of any wood a jump can be made of.
     *
     * <p>This is the whole input validation of the jump's screen: a slot takes
     * a stack if and only if this answers.
     */
    public static @Nullable String keyOfPlank(Item plank) {
        Map<Item, String> cached = byPlank;
        if (cached == null) {
            Map<Item, String> built = new HashMap<>();
            for (Map.Entry<String, Item> entry : plankMap().entrySet()) {
                // putIfAbsent, not put: two woods sharing a plank item would
                // otherwise have the LAST one win, and which is last depends on
                // mod load order. First wins is at least the same everywhere.
                built.putIfAbsent(entry.getValue(), entry.getKey());
            }
            cached = Map.copyOf(built);
            byPlank = cached;
        }
        return cached.get(plank);
    }

    /**
     * What to call a wood in the jump's screen and in an item's name -
     * "Oak", "Pale Oak", "Fir".
     *
     * <p><b>Not the plank's own name</b>, which is "Oak Planks" and gives
     * "Oak Planks Jump". Vanilla's twelve get a lang key each, written by
     * {@code bake-jumps.mjs}; a modded wood gets its key title-cased, which is
     * right far more often than it is wrong and never blank.
     *
     * <p><b>{@code translatableWithFallback}, not a lookup.</b> This is called
     * on the server - an item's name is resolved where the item is - and a
     * dedicated server holds no lang file of ours at all, so asking whether the
     * key resolves would title-case every wood for everybody. Handing the client
     * both and letting it choose is the only version that is right in both
     * places.
     */
    public static Component label(String wood) {
        return Component.translatableWithFallback(
                "horsegenetics.wood." + wood, titleCase(wood));
    }

    /** {@code "pale_oak"} -> {@code "Pale Oak"}. */
    private static String titleCase(String wood) {
        StringBuilder out = new StringBuilder(wood.length());
        for (String part : wood.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    private static Map<String, Item> plankMap() {
        Map<String, Item> cached = planks;
        if (cached == null) {
            Map<String, Item> built = new LinkedHashMap<>();
            for (String wood : VANILLA) {
                put(built, wood, "minecraft:" + wood + "_planks");
            }
            for (ModdedMaterials.Wood wood : ModdedMaterials.woods()) {
                put(built, wood.namespace() + "_" + wood.name(),
                        wood.plankTexture().replace("block/", ""));
            }
            cached = Map.copyOf(built);
            planks = cached;
        }
        return cached;
    }

    /** A composed id that names nothing comes back as AIR, and is skipped. */
    private static void put(Map<String, Item> into, String wood, String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item != null && item != Items.AIR) {
            into.put(wood, item);
        }
    }

    private JumpWoods() {
    }
}
