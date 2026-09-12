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

    /**
     * <b>Has this player been shown the Getting Started tab?</b> Set the first
     * time they leave it, so the browser opens on it once and never again.
     */
    public static final ModConfigSpec.BooleanValue TUTORIAL_SEEN;

    /**
     * <b>Do this client's debug tools exist?</b> The F6 pen generator and F7
     * stall overlay keybinds, the "Spawn Test Horse World" title-screen button
     * and the cleanup that removes those worlds again, and the per-coat texture
     * dump in the log.
     *
     * <p>The server half of the same idea is {@code ServerConfig.debug.tools},
     * and the two are deliberately separate rather than one setting: a client
     * config cannot be read by a dedicated server, and the title-screen button
     * is a singleplayer thing that no server has an opinion about. Turning this
     * on does not give you the server-side tools, and vice versa.
     */
    public static final ModConfigSpec.BooleanValue DEBUG_TOOLS;

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
        TUTORIAL_SEEN = builder
                .comment("Whether the Horse Browser has already opened on its Getting Started",
                        "tab. It does that once, and sets this the first time you leave the tab.",
                        "Set it back to false to be shown the introduction again.")
                .define("tutorial.seen", false);
        DEBUG_TOOLS = builder
                .comment("Whether this client's debug tools exist at all.",
                        "  The F6 debug-pen and F7 stall-overlay keys, the \"Spawn Test",
                        "  Horse World\" button on the title screen (and the cleanup that",
                        "  deletes those worlds again), and a per-coat dump in the log.",
                        "Defaults to ON in a development run and OFF in a normal install.",
                        "Turn it on when you are testing a release build - that is what it",
                        "is for. The two keys also need the server to allow them:",
                        "see debug.tools in server.toml.")
                .define("debug.tools", !production());
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

    /** Has the introduction already been shown? Defaults to "no" for a fresh install. */
    public static boolean tutorialSeen() {
        try {
            return TUTORIAL_SEEN.get();
        } catch (IllegalStateException notLoaded) {
            return false;
        }
    }

    /** Remember that it has. Written to disk, so it survives a restart. */
    public static void markTutorialSeen() {
        try {
            if (!TUTORIAL_SEEN.get()) {
                TUTORIAL_SEEN.set(true);
                TUTORIAL_SEEN.save();
            }
        } catch (IllegalStateException notLoaded) {
            // Config not up yet - it will simply be shown once more.
        }
    }

    private ClientConfig() {
    }

    /**
     * <b>Do this client's debug tools exist?</b> See the field above. Off by
     * default in a normal install.
     */
    public static boolean debugTools() {
        try {
            return DEBUG_TOOLS.get();
        } catch (IllegalStateException notLoaded) {
            // Keybind and title-screen registration both run early.
            return !production();
        }
    }

    /**
     * {@code FMLEnvironment.isProduction()}, but never fatal - it throws when
     * no loader is active. Unknown counts as production, so a strange
     * environment gets the quiet answer. Mirrors {@code ServerConfig}.
     */
    private static boolean production() {
        try {
            return net.neoforged.fml.loading.FMLEnvironment.isProduction();
        } catch (Throwable notLoaded) {
            return true;
        }
    }
}
