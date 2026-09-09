package com.example.horsegenetics.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side settings. Both of them are the same kind of setting: the genetics
 * are always built and always inherited, and what a world may switch off is
 * only whether a gene is allowed to <b>touch the animal standing in front of
 * you</b> - its hearts ({@code health.mode}) or its body size
 * ({@code body.size}).
 *
 * <p>{@link ClientConfig} is the wrong side for both. Whether a foal dies has
 * to be the same answer for everyone on a server, and it has to be the same
 * answer the breeding handler gives when it decides not to make one; body size
 * moves the <b>hitbox</b>, so a client that disagreed with the server about a
 * horse's size would be aiming at a horse that is not there.
 *
 * <h2>health.mode - three positions</h2>
 * <ul>
 *   <li><b>{@code full}</b> (the default) - the disorders reduce a horse's max
 *       health, lethal foals are born and then die, and an embryonic lethal
 *       pairing produces no foal.</li>
 *   <li><b>{@code no_deaths}</b> - the disorders still reduce max health and
 *       still show in the info panel, but nothing dies: a lethal foal lives as a
 *       very frail horse, and an embryonic-lethal pairing produces a foal like
 *       any other.</li>
 *   <li><b>{@code off}</b> - the disorders have no effect on the horse at
 *       all.</li>
 * </ul>
 *
 * <h2>body.size</h2>
 * <b>{@code true} by default.</b> The size loci resolve a body scale, and
 * writing it to {@code Attributes.SCALE} is what makes them visible - vanilla
 * scales the model <i>and</i> the hitbox from it. That second half is the catch:
 * a saddle, a lead, an arrow and a fence gap all meet a Falabella somewhere
 * other than where they meet a Percheron, and a player who would rather have
 * every horse fit the way vanilla horses fit can turn the size write off here.
 * Every horse then renders and collides at scale 1.0 while still carrying,
 * showing and inheriting exactly the size alleles it always did - the info
 * panel and the paper both keep reporting what the genotype says, because that
 * has not changed.
 *
 * <h2>What neither setting can change</h2>
 * <b>All the health genetics are built and inherited regardless.</b> The genes
 * are registered in every world, they occupy the same slots in the genotype
 * code, they are drawn from the same founder tables and they pass to foals the
 * same way. If the setting could change any of that, two players on different
 * settings would be breeding different animals, and a horse traded between them
 * would change genotype on the way. All they govern is whether what a horse
 * <i>carries</i> is allowed to affect the horse standing in front of you. The
 * same is true of {@code body.size}: it gates one attribute write, not a gene.
 */
public final class ServerConfig {

    /** How much of the disease layer a world plays with. */
    public enum HealthMode {
        /** Reduced hearts, dead foals, refused pairings. The default. */
        FULL,
        /** Reduced hearts, and nothing dies. */
        NO_DEATHS,
        /** The disorders do not affect the horse at all. */
        OFF;

        /** Do the disorders change a horse's body (hearts, size, conditions shown)? */
        public boolean affectsBody() {
            return this != OFF;
        }

        /** Does a lethal genotype actually kill? */
        public boolean deathsEnabled() {
            return this == FULL;
        }
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<HealthMode> HEALTH_MODE;

    public static final ModConfigSpec.BooleanValue BODY_SIZE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        HEALTH_MODE = builder
                .comment("How much of the health genetics this world plays with.",
                        "  FULL      - fewer hearts, lethal foals die shortly after birth,",
                        "              and an embryonic-lethal pairing produces no foal. (default)",
                        "  NO_DEATHS - fewer hearts and the conditions are still reported,",
                        "              but nothing dies and no pairing is refused.",
                        "  OFF       - the disorders have no effect on the horse at all.",
                        "The genes themselves are always registered and always inherited,",
                        "whichever of these is chosen - this only governs the consequences.")
                .defineEnum("health.mode", HealthMode.FULL);
        BODY_SIZE = builder
                .comment("Whether the size loci actually resize the horse. (default: true)",
                        "  true  - a Falabella is genuinely small and a Percheron genuinely",
                        "          large: vanilla scales the model AND the hitbox from it.",
                        "  false - every horse is rendered and collides at scale 1.0, so tack",
                        "          and hitboxes sit exactly where vanilla puts them.",
                        "The size genes are registered, inherited and reported either way -",
                        "this only governs whether the resolved scale reaches the entity.",
                        "SERVER-SIDE: it moves hitboxes, so the server's answer is the one",
                        "that counts and every client on it follows.",
                        "Like health.mode, a change reaches horses already in the world when",
                        "they next load - each horse re-resolves its body once per level load.")
                .define("body.size", true);
        SPEC = builder.build();
    }

    /** Safe read - falls back to the default if the config is not loaded yet. */
    public static HealthMode healthMode() {
        try {
            return HEALTH_MODE.get();
        } catch (IllegalStateException notLoaded) {
            return HealthMode.FULL;
        }
    }

    /** Shorthand for the flag {@code HorseTraits.resolve} takes. */
    public static boolean healthGeneticsActive() {
        return healthMode().affectsBody();
    }

    /**
     * <b>May the resolved body scale reach {@code Attributes.SCALE}?</b> False
     * means every horse is the vanilla size - see the {@code body.size} section
     * above for why a world would want that.
     */
    public static boolean bodySizeActive() {
        try {
            return BODY_SIZE.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** Shorthand: may a lethal genotype actually kill a foal, or refuse a pairing? */
    public static boolean lethalsActive() {
        return healthMode().deathsEnabled();
    }

    private ServerConfig() {
    }
}
