package com.example.horsegenetics.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side settings. Two of the three are the same kind of setting: the
 * genetics are always built and always inherited, and what a world may switch
 * off is only whether a gene is allowed to <b>touch the animal standing in
 * front of you</b> - its hearts ({@code health.mode}) or its body size
 * ({@code body.size}). The third ({@code debug.announce}) governs nothing about
 * a horse at all, only whether the mod narrates itself.
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
 * <h2>debug.announce</h2>
 * <b>On in a dev run, off in a normal install.</b> This mod says what it is
 * doing - a cowboy founding, a villager taking an equestrian job, a stable being
 * filled - in chat and in the log, because most of it happens where nobody is
 * looking. That is a development tool and it shipped switched on: the owner's
 * friends played 0.3.2 and got <code>[Cowboy]</code> and <code>[Equestrian]</code>
 * lines in their chat, which is noise to a player and looks like a bug.
 *
 * <p>It is a setting rather than a bare {@code isProduction()} check because
 * turning it back on is exactly what you want from someone who is reporting a
 * bug, and because a gate you cannot read is how a whole session once went by
 * unable to tell "the gate is shut" from "the code never ran". See
 * {@code server/DebugAnnounce}, which logs which answer it got, once, at
 * startup.
 *
 * <h2>debug.tools</h2>
 * <b>On in a dev run, off in a normal install.</b> The test kit
 * ({@code /testkit}, {@code /bond}), the breeding report in chat, the debug
 * pen and stall-overlay packets, and the stick/clock breeding shortcuts
 * outside the horse dimension. All of them were {@code isProduction()}-gated,
 * which meant they existed only under {@code runClient} and vanished from
 * every jar anybody could actually play.
 *
 * <p>That stopped working when testing moved onto a real server: the owner
 * plays a <i>release</i> build against a dedicated server over a tailnet, so a
 * tool that only exists in a dev run is a tool they no longer have. This is
 * the same call {@code debug.announce} made one release earlier, for the same
 * reason - a gate you cannot open is indistinguishable from a feature that
 * does not work, and the person who most needs these is the one holding a jar
 * rather than a checkout.
 *
 * <p>It stays <b>off by default in a normal install</b>: the commands are
 * additionally {@code LEVEL_GAMEMASTERS}, so a player on someone else's server
 * cannot reach them even when a server owner turns this on.
 *
 * <h2>What none of them can change</h2>
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

    /**
     * <b>Instant jump</b> - press to leap at full height, then wait out a
     * cooldown, instead of holding the key to charge.
     *
     * <p>SERVER-SIDE, by the same rule as {@link #BODY_SIZE}: it moves the
     * entity, so the server's answer is the one that counts. NeoForge syncs a
     * server config to every client on it, which is what lets the rider's own
     * client and its jump bar read the same three values.
     */
    public static final ModConfigSpec.BooleanValue INSTANT_JUMP;

    /** How long a horse cannot jump for after jumping, in ticks. */
    public static final ModConfigSpec.IntValue JUMP_COOLDOWN_TICKS;

    /** Extra forward throw on a jump, as a multiple of vanilla's. */
    public static final ModConfigSpec.DoubleValue JUMP_FORWARD_BOOST;

    public static final ModConfigSpec.DoubleValue GESTATION_DAYS;

    /**
     * <b>Does an owner hear about their horse being hurt?</b> See
     * {@code server/HorseHurtNoticeHandler} for who is told and how often.
     */
    public static final ModConfigSpec.BooleanValue DAMAGE_NOTICES;

    /**
     * <b>Does the whole server hear when an owned horse dies?</b> See
     * {@code server/HorseDeathNoticeHandler} for what the line says.
     */
    public static final ModConfigSpec.BooleanValue DEATH_NOTICES;

    /**
     * <b>The reproductive day while {@code debug.tools} is on</b> - one real
     * minute instead of twenty. Owner's call, 2026-09-13: a heat, a pregnancy
     * and half a cycle each become a minute, which is long enough to walk
     * between horses and short enough to watch a whole pregnancy.
     */
    public static final long DEBUG_REPRO_DAY_TICKS = 1_200L;

    public static final ModConfigSpec.BooleanValue DEBUG_ANNOUNCE;

    public static final ModConfigSpec.BooleanValue DEBUG_TOOLS;

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
        INSTANT_JUMP = builder
                .comment("Whether pressing jump launches the horse at once, at full height. (default: true)",
                        "Vanilla charges: you hold the key, a meter fills, and the horse leaps on",
                        "RELEASE at whatever the meter reached. That makes how high a horse jumps",
                        "partly a fact about the rider's thumb, so a good horse ridden badly clears",
                        "less than a mediocre one ridden well - which quietly works against the",
                        "whole point of breeding for jump.",
                        "With this on, the press IS the jump, always at that horse's maximum, and",
                        "the meter becomes a cooldown draining back to ready. What a horse clears",
                        "is then a fact about the horse.",
                        "Off restores vanilla's charge exactly, cooldown and all.")
                .define("ride.instant_jump", true);
        JUMP_COOLDOWN_TICKS = builder
                .comment("How long after a jump before the horse can jump again, in ticks (20 = 1 second). (default: 20)",
                        "This is what the jump meter shows while it drains. Ignored when",
                        "ride.instant_jump is off.",
                        "Vanilla's camel - which is where this whole mechanic is borrowed from,",
                        "dash and all - uses 55. Twenty is a horse, not a camel.")
                .defineInRange("ride.jump_cooldown_ticks", 20, 0, 200);
        JUMP_FORWARD_BOOST = builder
                .comment("Extra forward throw on a jump, as a multiple of vanilla's. (default: 1.0)",
                        "0 is vanilla: a jump adds a fixed forward nudge, and only while the rider",
                        "holds forward. 1.0 doubles that nudge, 2.0 triples it.",
                        "This is pure feel. Vanilla's number is small enough that a jump reads as a",
                        "hop rather than as a horse going over something.",
                        "It changes distance, never height - what a horse can clear is untouched,",
                        "so the metres on the horse screen stay honest.")
                .defineInRange("ride.jump_forward_boost", 1.0, 0.0, 5.0);
        GESTATION_DAYS = builder
                .comment("How long a pregnancy lasts, in Minecraft days (one day = 20 minutes). (default: 1)",
                        "Only the mod's own breeding makes a pregnancy - seed jars, breeding carrots and",
                        "a stallion left with a mare in heat. Plain golden carrots still give a foal at once.",
                        "Every other stage is scaled from this by its real-world ratio to a 340-day",
                        "pregnancy and never shorter than one day, so at 1 a mare is in heat for a day",
                        "and out of it for a day; at 340 she keeps a real 21-day cycle.",
                        "Game time, not the day counter: sleeping and /time set move nothing.")
                .defineInRange("fertility.gestation_days",
                        com.example.horsegenetics.common.repro.ReproTiming.DEFAULT_GESTATION_DAYS, 1.0, 340.0);
        DAMAGE_NOTICES = builder
                .comment("Whether a player is told in chat when one of their own horses is hurt. (default: true)",
                        "The line names the horse, what hurt it, and what to do about that -",
                        "shade for a horse burning in the sun, a wall for one being blown up.",
                        "Only the owner is told, only while they are in the same world as the",
                        "horse, never when they are the one dealing the damage, and at most",
                        "once every few seconds per horse, so a pen on fire is not a wall of text.",
                        "Turn it off on a server where players keep hundreds of horses.")
                .define("notices.owned_horse_damage", true);
        DEATH_NOTICES = builder
                .comment("Whether everybody on the server is told when an owned horse dies. (default: true)",
                        "The line names the horse, whose it was and what killed it, once, and the",
                        "owner's own copy is red so they can find it in a busy chat log.",
                        "A wild horse dying says nothing, and neither does anything at all while",
                        "the showDeathMessages game rule is off.",
                        "Turn it off on a server where horses die often enough to be noise.")
                .define("notices.owned_horse_death", true);
        DEBUG_ANNOUNCE = builder
                .comment("Whether this mod prints its own diagnostics to chat and the log.",
                        "  A cowboy founding, a villager taking an equestrian job, a stable",
                        "  being filled - the [Cowboy] / [Equestrian] / [Stables] lines.",
                        "Defaults to ON in a development run and OFF in a normal install,",
                        "which is what the line below actually reports, so this file says",
                        "what this build decided rather than what it usually decides.",
                        "Turn it on in a normal install when you are chasing a bug and want",
                        "something to paste into a report.")
                .define("debug.announce", !production());
        DEBUG_TOOLS = builder
                .comment("Whether this server's testing tools exist at all.",
                        "  /testkit and /bond, the breeding report in chat, the debug-pen",
                        "  and stall-overlay packets, and the stick/clock breeding",
                        "  shortcuts outside the horse dimension.",
                        "Defaults to ON in a development run and OFF in a normal install.",
                        "Turn it on when you are testing a release build on a real server -",
                        "that is what it is for. The commands still require gamemaster",
                        "permission, so this does not hand them to ordinary players.")
                .define("debug.tools", !production());
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
    /** Safe read - falls back to the default if the config isn't loaded yet. */
    public static boolean instantJump() {
        try {
            return INSTANT_JUMP.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** Safe read - falls back to the default if the config isn't loaded yet. */
    public static int jumpCooldownTicks() {
        try {
            return JUMP_COOLDOWN_TICKS.get();
        } catch (IllegalStateException notLoaded) {
            return 20;
        }
    }

    /** Safe read - falls back to the default if the config isn't loaded yet. */
    public static double jumpForwardBoost() {
        try {
            return JUMP_FORWARD_BOOST.get();
        } catch (IllegalStateException notLoaded) {
            return 1.0;
        }
    }

    public static boolean bodySizeActive() {
        try {
            return BODY_SIZE.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code fertility.gestation_days}, safely. */
    public static double gestationDays() {
        try {
            return GESTATION_DAYS.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.repro.ReproTiming.DEFAULT_GESTATION_DAYS;
        }
    }

    /**
     * <b>Every reproductive stage length, for this world, now.</b> The gestation
     * setting on the normal day, or on {@link #DEBUG_REPRO_DAY_TICKS} while the
     * testing tools are on.
     */
    public static com.example.horsegenetics.common.repro.ReproTiming reproTiming() {
        return com.example.horsegenetics.common.repro.ReproTiming.of(gestationDays(),
                debugTools() ? DEBUG_REPRO_DAY_TICKS : com.example.horsegenetics.common.repro.ReproTiming.DAY_TICKS);
    }

    /** {@code notices.owned_horse_damage}, safely. */
    public static boolean damageNotices() {
        try {
            return DAMAGE_NOTICES.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code notices.owned_horse_death}, safely. */
    public static boolean deathNotices() {
        try {
            return DEATH_NOTICES.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** Shorthand: may a lethal genotype actually kill a foal, or refuse a pairing? */
    public static boolean lethalsActive() {
        return healthMode().deathsEnabled();
    }

    /**
     * <b>May this mod say what it is doing, in chat and in the log?</b> See
     * {@code server/DebugAnnounce} for what those lines are and why they exist.
     */
    public static boolean debugAnnounce() {
        try {
            return DEBUG_ANNOUNCE.get();
        } catch (IllegalStateException notLoaded) {
            // Before the config file is read - the mod constructor, mostly.
            // Match the default rather than guessing true, or a normal install
            // gets the lines it is about to be told it does not want.
            return !production();
        }
    }

    /**
     * <b>Do this server's testing tools exist?</b> See the {@code debug.tools}
     * section above. Off by default in a normal install; the commands behind it
     * are gamemaster-only regardless.
     */
    public static boolean debugTools() {
        try {
            return DEBUG_TOOLS.get();
        } catch (IllegalStateException notLoaded) {
            // Command registration can run before the server config is read.
            // Match the default rather than guessing.
            return !production();
        }
    }

    /**
     * {@code FMLEnvironment.isProduction()}, but never fatal: it throws when no
     * loader is active (a unit test, a tool), and a config default is not worth
     * a crash. Unknown counts as production, so the quiet answer is the one a
     * strange environment gets.
     */
    private static boolean production() {
        try {
            return net.neoforged.fml.loading.FMLEnvironment.isProduction();
        } catch (Throwable notLoaded) {
            return true;
        }
    }

    private ServerConfig() {
    }
}
