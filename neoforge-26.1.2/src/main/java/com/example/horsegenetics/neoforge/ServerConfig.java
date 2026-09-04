package com.example.horsegenetics.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side settings - the first of them, and the reason this class exists at
 * all: how much of the <b>health genetics</b> a world actually plays with.
 *
 * <p>{@link ClientConfig} was the wrong side for this. Whether a foal dies has
 * to be the same answer for everyone on a server, and it has to be the same
 * answer the breeding handler gives when it decides not to make one - both of
 * those are server decisions.
 *
 * <h2>Three positions</h2>
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
 * <h2>What the setting cannot change</h2>
 * <b>All the health genetics are built and inherited regardless.</b> The genes
 * are registered in every world, they occupy the same slots in the genotype
 * code, they are drawn from the same founder tables and they pass to foals the
 * same way. If the setting could change any of that, two players on different
 * settings would be breeding different animals, and a horse traded between them
 * would change genotype on the way. All it governs is whether what a horse
 * <i>carries</i> is allowed to affect the horse standing in front of you.
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

    /** Shorthand: may a lethal genotype actually kill a foal, or refuse a pairing? */
    public static boolean lethalsActive() {
        return healthMode().deathsEnabled();
    }

    private ServerConfig() {
    }
}
