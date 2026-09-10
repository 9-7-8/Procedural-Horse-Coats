package com.example.horsegenetics.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-only settings for this mod: how the Family Tree screen handles a chart
 * taller than the window, and whether a horse's nameplate carries a sex symbol.
 *
 * <p>What makes a setting belong here rather than in
 * {@link ServerConfig} is that <b>nothing about the world changes</b> - two
 * players on one server can disagree about either of these and still be looking
 * at the same horses.
 */
public final class ClientConfig {

    public static final ModConfigSpec SPEC;

    /**
     * {@code false} (default): shrink the whole chart - boxes, text, models -
     * until it fits, no scroll bar. {@code true}: keep everything full size and
     * scroll (wheel + a right-edge scroll bar).
     */
    public static final ModConfigSpec.BooleanValue FAMILY_TREE_SCROLLBAR;

    /**
     * <b>Show a sex symbol after a horse's name.</b> Display only - see
     * {@code client/HorseNameplateSex} for why it is not part of the name and
     * why this side is the right side for it.
     */
    public static final ModConfigSpec.BooleanValue NAMEPLATE_SEX_SYMBOL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        FAMILY_TREE_SCROLLBAR = builder
                .comment("Family Tree screen: false = shrink the chart to fit the window (default);",
                        "true = keep it full size and add a scroll bar.")
                .define("familyTree.scrollBar", false);
        NAMEPLATE_SEX_SYMBOL = builder
                .comment("Add a sex symbol after a horse's name above its head:",
                        "  true  - a mare reads \"Bramble Fell ♀\" in pink and a stallion",
                        "          \"Copper Vale ♂\" in blue. (default)",
                        "  false - just the name.",
                        "This is drawn, not stored: the horse's actual name is unchanged, so",
                        "the symbol never reaches a transfer paper, the browser or a rename",
                        "box - and two players on one server may disagree about it.")
                .define("nameplate.sexSymbol", true);
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

    /** Safe read - falls back to the default if the config isn't loaded yet. */
    public static boolean nameplateSexSymbol() {
        try {
            return NAMEPLATE_SEX_SYMBOL.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    private ClientConfig() {
    }
}
