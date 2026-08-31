package com.example.horsegenetics.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-only settings for this mod. Currently just how the Family Tree screen
 * handles a chart that's taller than the window.
 */
public final class ClientConfig {

    public static final ModConfigSpec SPEC;

    /**
     * {@code false} (default): shrink the whole chart - boxes, text, models -
     * until it fits, no scroll bar. {@code true}: keep everything full size and
     * scroll (wheel + a right-edge scroll bar).
     */
    public static final ModConfigSpec.BooleanValue FAMILY_TREE_SCROLLBAR;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        FAMILY_TREE_SCROLLBAR = builder
                .comment("Family Tree screen: false = shrink the chart to fit the window (default);",
                        "true = keep it full size and add a scroll bar.")
                .define("familyTree.scrollBar", false);
        SPEC = builder.build();
    }

    /** Safe read - falls back to the default if the config isn't loaded yet. */
    public static boolean familyTreeScrollBar() {
        try {
            return FAMILY_TREE_SCROLLBAR.get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    private ClientConfig() {
    }
}
