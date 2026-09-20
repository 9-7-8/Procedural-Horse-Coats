package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.compat.ModdedMaterials;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Every wood a jump can be made of</b>, as the keys {@link JumpModel} composes
 * model ids from.
 *
 * <p>Vanilla's twelve in vanilla's order, then every wood another mod brought,
 * keyed the way {@code ModdedMaterials.Wood.jumpId} keys them. The list is the
 * same on both sides, because it is read out of the installed jars rather than
 * off a registry - see {@code compat/ModdedMaterials} for why that matters.
 *
 * <p><b>Held as a list, not recomputed</b>: it is asked for once per blockstate
 * variant at bake time, which is ninety-six times per jump block, and the
 * modded half of it walks other mods' jars.
 */
public final class JumpWoods {

    /** Vanilla's twelve, in vanilla's order. Must match {@code bake-jumps.mjs}. */
    private static final String[] VANILLA = {
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "pale_oak", "mangrove", "cherry", "bamboo", "crimson", "warped",
    };

    private static volatile List<String> keys;

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

    private JumpWoods() {
    }
}
