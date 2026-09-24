package com.example.horsegenetics.neoforge;

import com.example.horsegenetics.common.name.NamingPolicy;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-only settings for this mod: how the Family Tree screen handles a chart
 * taller than the window, whether a horse's nameplate carries a sex symbol, and
 * how hard this machine works to give every horse its own coat.
 *
 * <p>What makes a setting belong here rather than in
 * {@link ServerConfig} is that <b>nothing about the world changes</b> - two
 * players on one server can disagree about any of these and still be looking
 * at the same horses.
 *
 * <h2>The two naming settings are the exception, and how</h2>
 * {@link #NAMING_INHERITED_HALF} and {@link #NAMING_PARENT_SOURCE} <i>do</i>
 * change the world: they decide what a foal is called, and a name is stored.
 * They sit here anyway because the thing being configured is a <b>player's</b>
 * preference for their <b>own</b> horses, and a server config has no way to
 * hold one answer per player. So the client does not apply them - it
 * <i>suggests</i> them, sending both to the server on login
 * ({@code NamingPolicyPayload}), which files them under that player's UUID in
 * {@code HorseNamingData} and is the only thing that ever names a foal. Two
 * players on one server therefore get their own naming policy, and neither can
 * name the other's horses.
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
     * <b>How close a horse must be before it gets a coat of its own.</b> In
     * blocks. Further out it wears a shared stand-in until you approach; a coat
     * already made is kept at any range. See {@code GeneticCoatTextureFactory}.
     */
    public static final ModConfigSpec.IntValue COAT_DETAIL_DISTANCE;

    /**
     * <b>Milliseconds of coat baking allowed per 50 ms.</b> The first bake in a
     * window always runs, so zero means "one at a time". Added after walking
     * toward a herd froze the owner's machine outright (2026-09-13).
     */
    public static final ModConfigSpec.IntValue COAT_BAKE_BUDGET_MS;

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

    /**
     * <b>Which half of its name a foal inherits from a parent</b>; the other
     * half is rolled from the word tables. Suggested to the server on login -
     * see the class note.
     */
    public static final ModConfigSpec.EnumValue<NamingPolicy.InheritedHalf> NAMING_INHERITED_HALF;

    /**
     * <b>Which parent that inherited half comes from.</b> Suggested to the
     * server on login - see the class note.
     */
    public static final ModConfigSpec.EnumValue<NamingPolicy.ParentSource> NAMING_PARENT_SOURCE;

    private static final int DEFAULT_COAT_DETAIL_DISTANCE = 32;
    private static final int DEFAULT_COAT_BAKE_BUDGET_MS = 4;

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
        COAT_DETAIL_DISTANCE = builder
                .comment("How close, in blocks, a horse must be before this machine makes its",
                        "own coat texture. Further away it wears a plain stand-in coat until you",
                        "come closer; a coat that has already been made stays at any distance.",
                        "Lower it if walking toward a big herd stutters.")
                .defineInRange("coats.detailDistance", DEFAULT_COAT_DETAIL_DISTANCE, 8, 256);
        COAT_BAKE_BUDGET_MS = builder
                .comment("Milliseconds per 50 ms that may be spent making new coat textures.",
                        "Coats that do not fit wait a frame or two and wear the stand-in",
                        "meanwhile. The first coat in each 50 ms always runs, so 0 means",
                        "\"one at a time\". Lower it on a slow machine, raise it on a fast one.")
                .defineInRange("coats.bakeBudgetMs", DEFAULT_COAT_BAKE_BUDGET_MS, 0, 50);
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
        NAMING_INHERITED_HALF = builder
                .comment("Which half of its name a foal born to YOUR horses keeps from a",
                        "parent. The other half is rolled from the name tables, which is what",
                        "keeps two foals of the same pair from sharing a name.",
                        "  LAST  - the foal keeps the parent's last name, e.g. a filly out of",
                        "          \"Bright Meadow\" is \"Swift Meadow\". A surname line. (default)",
                        "  FIRST - the foal keeps the parent's first name and rolls the last.",
                        "Sent to the server when you join; it is stored against your name and",
                        "applies only to horses you own, so other players on the server are",
                        "unaffected and keep their own setting.")
                .defineEnum("naming.inheritedHalf", NamingPolicy.InheritedHalf.LAST);
        NAMING_PARENT_SOURCE = builder
                .comment("Which parent that inherited half comes from.",
                        "  BY_SEX - a filly takes it from her dam, a colt from his sire.",
                        "           (default)",
                        "  DAM    - always the dam, whatever the foal is.",
                        "  SIRE   - always the sire, whatever the foal is.",
                        "Sent to the server when you join; see naming.inheritedHalf.")
                .defineEnum("naming.parentSource", NamingPolicy.ParentSource.BY_SEX);
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

    /** Safe read, in blocks. Read every frame for every horse, so it must never throw. */
    public static int coatDetailDistance() {
        try {
            return COAT_DETAIL_DISTANCE.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_COAT_DETAIL_DISTANCE;
        }
    }

    /** Safe read, in milliseconds. */
    public static int coatBakeBudgetMs() {
        try {
            return COAT_BAKE_BUDGET_MS.get();
        } catch (IllegalStateException notLoaded) {
            return DEFAULT_COAT_BAKE_BUDGET_MS;
        }
    }

    /**
     * This client's suggested naming policy, to send to the server on login.
     * Safe read - the mod's default if the config isn't loaded yet.
     */
    public static NamingPolicy namingPolicy() {
        try {
            return new NamingPolicy(NAMING_INHERITED_HALF.get(), NAMING_PARENT_SOURCE.get());
        } catch (IllegalStateException notLoaded) {
            return NamingPolicy.DEFAULT;
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
