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
 * every horse fit near the way vanilla horses fit can turn the size write off
 * here. Every horse's scale is then compressed into 0.85-1.15
 * ({@code HorseTraits.compressScale}) rather than flattened to 1.0 (owner,
 * 2026-10-01), so a Shire still stands over a Falabella and every horse still
 * fits a two-block stall, while still carrying,
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
 * <h2>realm.breeding_rate_percent</h2>
 * <b>How fast reproduction runs in the horse realm, as a whole percent of
 * normal, while the horse is loaded.</b> 100 by default (owner, 2026-10-02; it
 * was 25 until then, and a server whose file already says 25 keeps it). At 25
 * every reproductive timer takes four times as long there: heat, the
 * once-a-heat retry, the stallion's day, and gestation. It is a <i>rate</i>, not
 * a chance - 25 does not mean a quarter of covers take, it means the calendar
 * runs at quarter speed.
 *
 * <p>0 stops it outright, existing pregnancies included. That is only possible
 * because the pacing is a clock rather than a scale factor - see
 * {@code server/HorseRealmRepro}. Scoped to that one dimension; the Overworld
 * and the debug corridor never read it.
 *
 * <h2>realm.pause_when_unloaded</h2>
 * <b>Whether a realm horse's breeding stands still while nobody is near enough
 * to keep it loaded.</b> On by default: it does not progress, and does not catch
 * up when somebody arrives. Off, unloaded time counts at the rate above, so at
 * 100 a realm foal can be born off-screen exactly as in the Overworld. The rule
 * is {@code common/realm/RealmClock}.
 *
 * <h2>What none of them can change</h2>
 * <b>All the health genetics are built and inherited regardless.</b> The genes
 * are registered in every world, they occupy the same slots in the genotype
 * code, they are drawn from the same founder tables and they pass to foals the
 * same way. If the setting could change any of that, two players on different
 * settings would be breeding different animals, and a horse traded between them
 * would change genotype on the way. All they govern is whether what a horse
 * <i>carries</i> is allowed to affect the horse standing in front of you. The
 * same is true of {@code body.size}: it shapes one attribute write, not a gene.
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
     * <b>How many horses may stand within a chunk of a mare before nobody covers
     * her.</b> Server-side and not negotiable from a client: whether a foal exists
     * has to be one answer for everyone, exactly as {@link #GESTATION_DAYS} is.
     * See {@code common/repro/NaturalCover.Crowd}, which carries it to the rule.
     */
    public static final ModConfigSpec.IntValue NEARBY_HORSE_CAP;

    /**
     * <b>How many covers a stallion makes in a day before his odds halve.</b>
     * Server-side for the same reason as {@link #NEARBY_HORSE_CAP}. See
     * {@code common/repro/StallionDay}, which carries it to every rule that asks.
     */
    public static final ModConfigSpec.IntValue FREE_COVERS_PER_DAY;

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
     * <b>Does an owner hear why one of their mares was not covered?</b> See
     * {@code server/NaturalBreedingHandler} for which refusals earn a line.
     */
    public static final ModConfigSpec.BooleanValue BREEDING_NOTICES;

    /**
     * <b>How badly hurt a horse has to be before it bucks its rider and runs.</b>
     * A fraction of its own maximum health; zero turns the behaviour off. See
     * {@code server/HorseEscapeGoal}.
     */
    public static final ModConfigSpec.DoubleValue ESCAPE_HEALTH_FRACTION;
    public static final ModConfigSpec.DoubleValue EMERGENCY_STASIS_FRACTION;

    /**
     * <b>Is a horse ever killed by a single blow?</b> Off, it is - vanilla's
     * rules, unchanged. See {@code server/HorseLastStandHandler}.
     */
    public static final ModConfigSpec.BooleanValue LAST_STAND;

    /** The health a horse saved from a killing blow is left standing on. */
    public static final ModConfigSpec.DoubleValue LAST_STAND_HEALTH;

    /** How long nothing can touch a horse that was just saved, in ticks. */
    public static final ModConfigSpec.IntValue LAST_STAND_IMMUNITY_TICKS;

    /**
     * How far back a saved horse must heal, as a fraction of its own maximum,
     * before it can be saved again.
     */
    public static final ModConfigSpec.DoubleValue LAST_STAND_REARM_FRACTION;

    /**
     * <b>Does a lead come back to you rather than falling on the ground?</b>
     * Covers both halves: the leads this mod causes to be dropped by teleporting
     * a horse, and the ones vanilla drops on its own - a snapped leash, a broken
     * knot. Off, every one of them falls where vanilla would put it. See
     * {@code server/HorseLeads}.
     */
    public static final ModConfigSpec.BooleanValue LEADS_RETURN;

    /**
     * <b>Does a rider mine at full speed?</b> On - the default - a player on a
     * horse that is standing on the ground is exempted from vanilla's
     * fifth-speed penalty for mining while off the ground. <b>On is not
     * vanilla</b>; off restores it. See {@code server/MountedMiningHandler}.
     */
    public static final ModConfigSpec.BooleanValue MOUNTED_MINING_PENALTY_REMOVED;

    /**
     * <b>Does right-clicking a horse with a piece of this mod's tack put it
     * on?</b> On - the default - and the seventeen gear slots equip by hand the
     * way vanilla's saddle and barding already do. Off, the Gear tab is the only
     * way. The saddle and the barding are vanilla's own behaviour either way and
     * this never touches them. See {@code server/TackEquipHandler}.
     */
    public static final ModConfigSpec.BooleanValue RIGHTCLICK_EQUIPS_TACK;

    /**
     * <b>How much bond a neglected horse loses per Minecraft day</b>; zero turns
     * the decay off. See {@code common.care.Bond}.
     */
    public static final ModConfigSpec.IntValue BOND_DECAY_PER_DAY;

    /** <b>The bond level that decay stops at</b>, and never falls below. */
    public static final ModConfigSpec.IntValue BOND_FLOOR;

    /**
     * <b>The reproductive day while {@code debug.tools} is on</b> - one real
     * minute instead of twenty. Owner's call, 2026-09-13: a heat, a pregnancy
     * and half a cycle each become a minute, which is long enough to walk
     * between horses and short enough to watch a whole pregnancy.
     */
    public static final long DEBUG_REPRO_DAY_TICKS = 1_200L;

    /**
     * <b>{@code realm.breeding_rate_percent}</b> - reproduction's speed in the
     * horse realm, 0 to 100, as a whole percent of normal. Read through
     * {@link #realmBreedingRatePercent()}, and applied by
     * {@code server/HorseRealmRepro}, never here.
     */
    public static final ModConfigSpec.IntValue REALM_BREEDING_RATE;

    /**
     * <b>{@code realm.pause_when_unloaded}</b> - an unloaded realm horse's
     * breeding stands still. Read through {@link #realmPace()}.
     */
    public static final ModConfigSpec.BooleanValue REALM_PAUSE_WHEN_UNLOADED;

    /**
     * <b>{@code realm.release_emeralds}</b> - what turning a horse out into the
     * realm pays. Read through {@link #realmReleaseEmeralds()}, and paid by
     * {@code server/HorseRelease}, never here.
     *
     * <p>It is a config value rather than a constant because it is the one
     * number in the mod that makes horses into income, and the loop it opens -
     * breed, tame, turn out, repeat - is one a settled paddock runs on its own.
     * Whether 2 a head is generous or nothing depends entirely on how fast that
     * paddock is, which is a question about somebody's world and not one this
     * mod can answer. 0 turns it off and gives back the old behaviour exactly.
     */
    public static final ModConfigSpec.IntValue REALM_RELEASE_EMERALDS;

    /**
     * <b>{@code ops.resurrect_grace_minutes}</b> - how long a dead horse is kept
     * resurrectable, counted <b>only while its owner is logged in</b>. Read
     * through {@link #resurrectGraceTicks()}, spent by
     * {@code server/HorseAfterlifeHandler} and honoured by
     * {@code server/HorseResurrectCommand}, never here.
     *
     * <p>It is a number of minutes rather than a flag because of what is being
     * kept. A resurrectable horse is a whole entity tag - every attachment, the
     * gear, the pedigree, the bond - and that is real bytes in the world save,
     * per dead horse, for ever, if nothing ever throws it away. That storage
     * question is now answered by size, {@link #RESURRECT_BUDGET_MB}; this
     * window is left as an operator's extra rule for a server that also wants
     * a time limit.
     *
     * <p><b>Why owner-online time and not wall clock.</b> The grace exists so a
     * player who loses a horse has a chance to notice and ask. Somebody who
     * logs off ten seconds before a creeper finds their mare has had no such
     * chance, and an hour of real time while they are asleep spends a window
     * they were never in the room for. Counting their own time online is the
     * only clock that measures the thing the window is for. It also means the
     * store cannot be aged out by a server simply being left running.
     *
     * <p><b>0 - the default since issue #15 - puts no time limit on it</b>, and
     * the store is held to {@link #RESURRECT_BUDGET_MB} instead. The count is
     * still kept, so turning a limit on later starts expiring the backlog
     * rather than grandfathering it.
     */
    public static final ModConfigSpec.IntValue RESURRECT_GRACE_MINUTES;

    /**
     * <b>{@code ops.resurrect_budget_mb}</b> - how big the afterlife store may
     * grow before it lets dead horses go, oldest death first and only as many
     * as it takes (owner, issue #15). Read through
     * {@link #resurrectBudgetBytes()}, enforced by
     * {@code server/HorseAfterlifeHandler}'s sweep through
     * {@code common/horse/AfterlifeBudget}.
     *
     * <p>A size rather than a clock because the cost of keeping a dead horse
     * is bytes, and a clock spends a horse long before the bytes add up to
     * anything. Measured as each horse's gzipped entity tag, which slightly
     * overstates the real file. Only the resurrectable snapshot is let go; the
     * pedigree's record of the horse stays for ever as it always has.
     * <b>0 is no cap.</b>
     */
    public static final ModConfigSpec.IntValue RESURRECT_BUDGET_MB;

    /**
     * <b>{@code behaviour.owner_only_riding}</b> - may a stranger get on your
     * horse? Off (the default is {@code true}, meaning the rule is on) a horse
     * refuses anybody but its owner, rears, and says so.
     *
     * <p>Owner, 2026-09-29: <i>"people should not be able to ride a horse they
     * don't own, it should buck them off with a warning."</i> Vanilla has no
     * such rule - a tamed horse is a saddle anybody can sit in - and on a
     * server that is the difference between a stable and a car park.
     *
     * <p><b>An untamed horse is never refused.</b> Climbing on one until it
     * stops bucking is how a horse is tamed at all, so the rule can only ever
     * apply to a horse that already has an owner. Read through
     * {@link #ownerOnlyRiding()}; enforced by {@code server/HorseRiding}.
     */
    public static final ModConfigSpec.BooleanValue OWNER_ONLY_RIDING;

    /**
     * <b>{@code behaviour.riding_allows_teams}</b> - does being on somebody's
     * team count as knowing their horse? Three things answer yes, and all three
     * are switched by this one flag: a shared <b>vanilla scoreboard team</b>,
     * <b>FTB Teams</b> membership, and an FTB Teams <b>ally</b> of the owner's
     * team. FTB Chunks creates its claims out of FTB Teams parties, which is
     * the setup this was asked for.
     *
     * <p>Off, only the owner ever rides. It is separate from
     * {@link #OWNER_ONLY_RIDING} because "nobody but me" and "my team" are
     * different servers, and a server that uses scoreboard teams for chat
     * colours alone would rather they did not hand out horses too.
     */
    public static final ModConfigSpec.BooleanValue RIDING_ALLOWS_TEAMS;

    /**
     * <b>{@code commands.horse_give}</b> - does {@code /horsegive} exist? It
     * hands a horse to another player the way a signed transfer paper does,
     * without the walk. A server that wants the paper to stay the only way a
     * horse changes hands turns this off and the command is not registered at
     * all. See {@code server/HorseGiveCommand}.
     */
    public static final ModConfigSpec.BooleanValue HORSE_GIVE_COMMAND;

    /**
     * <b>{@code commands.horse_jockey}</b> - does {@code /horsejockey} exist? It
     * lends a horse to another player for a while, which is what a jockey pass
     * buys with an item. A server that wants the pass to be the only way in
     * turns this off and the command is not registered at all. See
     * {@code server/HorseJockeyCommand}.
     *
     * <p>It is a <b>separate</b> flag from {@code commands.horse_give} because
     * the two do different amounts of damage if misused: giving a horse away is
     * permanent and one-way, lending one expires by itself. A server may well
     * want the second and not the first.
     */
    public static final ModConfigSpec.BooleanValue HORSE_JOCKEY_COMMAND;

    /**
     * <b>{@code undead.convert}</b> - does a vanilla zombie or skeleton horse become
     * one of this mod's horses the first time it is ticked? On by default (undead
     * treatment D2). Off, the converter returns at once and every vanilla undead
     * horse is left exactly as vanilla made it; horses already converted stay the
     * horses they became. See {@code server/UndeadHorseConverter}.
     */
    public static final ModConfigSpec.BooleanValue UNDEAD_CONVERT;

    /**
     * <b>{@code behaviour.jockey_pass_days}</b> - how many Minecraft days one
     * jockey pass, or one bare {@code /horsejockey}, is worth. The owner's
     * number is 1. Zero is not allowed: a pass worth nothing is an item that
     * does nothing, and turning the feature off is what the recipe and
     * {@code commands.horse_jockey} are for.
     */
    public static final ModConfigSpec.IntValue JOCKEY_PASS_DAYS;

    /**
     * <b>What the horse browser's Send home button costs.</b> An item id and a
     * count per trip; blank, or a count of 0, is free - the default. A price
     * replaces tickets for the button. See {@code common/care/SendHome} and
     * {@code server/StallRecall}.
     */
    public static final ModConfigSpec.ConfigValue<String> SEND_HOME_PAYMENT_ITEM;

    /** How many of {@link #SEND_HOME_PAYMENT_ITEM} one trip takes. */
    public static final ModConfigSpec.IntValue SEND_HOME_PAYMENT_COUNT;

    /** Seconds between two Send home trips by one player; 0 is off. */
    public static final ModConfigSpec.IntValue SEND_HOME_COOLDOWN_SECONDS;

    /**
     * <b>What a Chaos allele may never name.</b> The three mob loci each have one
     * {@code Cha} allele standing in for every modded mob ({@code server/ChaosRoster});
     * these take a whole mod, or one mob, off all three lists. Default is
     * "allowed unless listed" - the owner's call - so a pack author removes a
     * modded boss here rather than opting each mob in.
     */
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> CHAOS_EXCLUDE_MODS;

    /** See {@link #CHAOS_EXCLUDE_MODS}: single ids, {@code modid:mob}. */
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> CHAOS_EXCLUDE_IDS;

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
                        "  false - every horse's scale is compressed into 0.85-1.15: big",
                        "          horses are still bigger, but every one fits a two-block",
                        "          stall and tack and hitboxes sit near where vanilla puts them.",
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
        REALM_BREEDING_RATE = builder
                .comment("How fast reproduction runs in the horse realm while a horse is loaded,",
                        "as a whole percent of normal. (default: 100)",
                        "A rate, not a chance: 25 runs every reproduction timer at a quarter speed,",
                        "so heat, the once-a-heat retry, a stallion's day and gestation all take four",
                        "times as long. It does not mean one cover in four takes.",
                        "0 pauses reproduction there completely, existing pregnancies included.",
                        "The horse realm only. The Overworld, and the F6 debug dimension, ignore it.")
                .defineInRange("realm.breeding_rate_percent",
                        com.example.horsegenetics.common.realm.RealmClock.DEFAULT_RATE_PERCENT, 0, 100);
        REALM_PAUSE_WHEN_UNLOADED = builder
                .comment("Whether a horse's breeding in the horse realm stands still while nobody is near",
                        "enough to keep it loaded. (default: true)",
                        "On: a mare nobody is near does not progress - pregnancy, heat and the waits",
                        "between covers - and does not catch up when somebody arrives.",
                        "Off: unloaded time counts at realm.breeding_rate_percent, as if she had been",
                        "watched; at 100 that is plain game time, and realm foals are born off-screen.",
                        "Changing it changes when realm foals are born. Server-side.")
                .define("realm.pause_when_unloaded",
                        com.example.horsegenetics.common.realm.RealmClock.DEFAULT_PAUSE_WHEN_UNLOADED);
        REALM_RELEASE_EMERALDS = builder
                .comment("Emeralds paid for turning one horse out into the horse realm. (default: 2)",
                        "Flat, per horse, whatever the horse is - the realm takes anybody's surplus",
                        "and does not ask what it is worth.",
                        "Paid by the freedom stick and the turnout ticket, which are the two ways a",
                        "player is standing there to be paid. A horse you simply walk away from in",
                        "the realm still goes wild on its own and pays nothing: by the time that",
                        "fires you have left the dimension and there is nobody to hand emeralds to.",
                        "0 turns the payment off.")
                .defineInRange("realm.release_emeralds", 2, 0, 64);
        RESURRECT_GRACE_MINUTES = builder
                .comment("An optional time limit on bringing a dead horse back with /horseresurrect. (default: 0)",
                        "Counted in minutes of the OWNER'S OWN TIME ONLINE since the horse died, not",
                        "wall clock: a player who was logged off when it happened has not spent any",
                        "of their window, because the window is their chance to notice and ask.",
                        "0 is no time limit: a dead horse is then kept until ops.resurrect_budget_mb",
                        "is reached. Only owned horses are kept at all; a wild one has nobody to ask",
                        "for it. The elapsed count is still kept while this is 0, so turning a limit",
                        "on expires the backlog rather than sparing it.")
                .defineInRange("ops.resurrect_grace_minutes", 0, 0, 10_080);
        RESURRECT_BUDGET_MB = builder
                .comment("How big the store of dead horses /horseresurrect can bring back may grow, in",
                        "megabytes. (default: 100)",
                        "Each one is the whole horse - every attachment, its gear, its bond - kept in",
                        "the world save. Past this size the OLDEST deaths are let go first, only as",
                        "many as it takes to fit. A horse let go can no longer be resurrected; its",
                        "pedigree record is kept for ever regardless. 0 is no cap. Server-side.")
                .defineInRange("ops.resurrect_budget_mb",
                        com.example.horsegenetics.common.horse.AfterlifeBudget.DEFAULT_BUDGET_MB, 0, 100_000);
        NEARBY_HORSE_CAP = builder
                .comment("How many other horses may be within 16 blocks of a mare and still let a",
                        "stallion cover her. (default: 50)",
                        "Foals and wild horses count, and only horses she shares a pen with -",
                        "a herd on the other side of a fence is not her crowd.",
                        "This is the only brake on a paddock breeding itself flat, so it is",
                        "deliberately generous rather than absent: past it her owner is told in",
                        "chat which pen is full, if notices.owned_horse_breeding is on.",
                        "Server-side: a client cannot raise its own.")
                .defineInRange("fertility.nearby_horse_cap",
                        com.example.horsegenetics.common.repro.ReproRules.DEFAULT_NATURAL_CAP, 1,
                        com.example.horsegenetics.common.repro.ReproRules.MAX_NATURAL_CAP);
        FREE_COVERS_PER_DAY = builder
                .comment("How many covers a stallion makes in one day before his chance of getting",
                        "a mare in foal is halved until tomorrow. (default: 3, range 0 to 1000)",
                        "Natural covers, breeding carrots and seed-jar fills all count against it,",
                        "and a mare prefers a stallion still under it. It is a taper, never a stop:",
                        "a tired stallion still covers. 0 is always tired; 1000 is never.",
                        "Server-side: a client cannot raise its own.")
                .defineInRange("fertility.free_covers_per_day",
                        com.example.horsegenetics.common.repro.ReproRules.DEFAULT_FREE_COVERS_PER_DAY, 0,
                        com.example.horsegenetics.common.repro.ReproRules.MAX_FREE_COVERS_PER_DAY);
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
        BREEDING_NOTICES = builder
                .comment("Whether a mare's owner is told in chat when a natural cover does not",
                        "happen, and how the cover went when it does. (default: true)",
                        "Only reasons the owner can act on are said: the crowding cap, a hurt",
                        "mare, one being ridden or led, and branded cowboy stock - plus whether",
                        "a cover took. A mare who is simply between heats, or who has no",
                        "stallion in reach, says nothing, because that is nearly every mare",
                        "nearly all of the time.",
                        "Only the owner is told, only while they are in the same world as the",
                        "mare, and at most once every few minutes per mare per reason.")
                .define("notices.owned_horse_breeding", true);
        ESCAPE_HEALTH_FRACTION = builder
                .comment("How low a horse's health has to fall before it runs for its life,",
                        "as a fraction of its own maximum. (default: 0.2, so a fifth)",
                        "At or below it the horse stops being a mount: it throws off whoever",
                        "is riding it, drops any fight it had picked, and runs from whatever",
                        "hurt it - jumping a fence if its jump is good enough to clear one.",
                        "It keeps running for ten seconds after the last blow, and stops once",
                        "it has healed clear of the threshold rather than the moment it",
                        "crosses back over it.",
                        "This is a horse's own maximum, so a frail one and a Percheron bolt at",
                        "different numbers of hearts and at the same fraction of themselves.",
                        "0 turns the whole behaviour off; 1 makes a horse bolt from any blow.")
                .defineInRange("behaviour.escape_health_fraction",
                        com.example.horsegenetics.common.care.Escape.DEFAULT_THRESHOLD, 0.0, 1.0);
        EMERGENCY_STASIS_FRACTION = builder
                .comment("How low a horse's health has to fall before an Emergency Horse Stasis",
                        "Chamber its owner is carrying takes it, as a fraction of its own",
                        "maximum. (default: 0.1, so a tenth)",
                        "The owner does nothing: if the horse is theirs and they are online,",
                        "anywhere in the world, the horse is simply in the bottle - and a horse",
                        "inside a chamber cannot be hurt at all, so this is a hard stop against",
                        "dying rather than a delay. It fires whether or not the blow would have",
                        "been fatal, and whether or not they are anywhere near it.",
                        "Deliberately below behaviour.escape_health_fraction, so a horse still",
                        "gets to run for its life first and is swallowed only if running did not",
                        "work. Raising it above that number takes that chance away.",
                        "One chamber holds one horse: a second horse crossing the line while the",
                        "chamber is full is refused, out loud, and takes its chances.",
                        "0 turns the whole behaviour off - the chambers still craft and still",
                        "work when used by hand.")
                .defineInRange("behaviour.emergency_stasis_fraction",
                        com.example.horsegenetics.common.horse.StasisRescue.DEFAULT_THRESHOLD, 0.0, 1.0);
        LAST_STAND = builder
                .comment("Whether a horse can be killed by a single blow. (default: true, meaning it cannot)",
                        "On, damage that would take a horse to zero takes it to",
                        "behaviour.last_stand_health instead, and nothing can touch it for",
                        "behaviour.last_stand_immunity_ticks afterwards - time to run, which is",
                        "what a horse at that health does (see behaviour.escape_health_fraction).",
                        "The save is then SPENT: the next killing blow lands, until the horse has",
                        "healed back to behaviour.last_stand_rearm_fraction of its maximum.",
                        "It never applies to a genetic defect - a lethal foal still dies, and the",
                        "half-heart a mare loses to an embryonic lethal can still be her last -",
                        "nor to /kill, the void, or anything else that bypasses invulnerability.",
                        "Off restores vanilla exactly: a horse dies when its health reaches zero,",
                        "however it got there.")
                .define("behaviour.last_stand", true);
        LAST_STAND_HEALTH = builder
                .comment("The health a horse saved from a killing blow is left on. (default: 1.0, half a heart)",
                        "Health points, not hearts, and not a fraction: a Falabella and a",
                        "Percheron are both left on the same sliver, because the point of the",
                        "number is that it is nearly nothing rather than that it is proportionate.",
                        "Capped at the horse's own maximum, and never zero - zero is death, which",
                        "would make the save kill what it rescued.")
                .defineInRange("behaviour.last_stand_health",
                        com.example.horsegenetics.common.care.LastStand.DEFAULT_HEALTH_LEFT,
                        com.example.horsegenetics.common.care.LastStand.MIN_HEALTH_LEFT, 1024.0);
        LAST_STAND_IMMUNITY_TICKS = builder
                .comment("How long nothing can touch a horse that was just saved, in ticks (20 = 1 second). (default: 120, so six seconds)",
                        "Real immunity, not vanilla's hurt cooldown: every source is refused for",
                        "the whole window, so standing in the fire that nearly killed it costs",
                        "the horse nothing until the window closes.",
                        "It is there to buy distance. A horse that is saved and hit again on the",
                        "next tick was not saved, and six seconds is roughly what a bolting horse",
                        "needs to get out of a blast radius or off a burning block.",
                        "0 keeps the save and drops the breathing space.")
                .defineInRange("behaviour.last_stand_immunity_ticks",
                        com.example.horsegenetics.common.care.LastStand.DEFAULT_IMMUNITY_TICKS, 0, 1200);
        LAST_STAND_REARM_FRACTION = builder
                .comment("How far back a saved horse must heal before it can be saved again,",
                        "as a fraction of its own maximum health. (default: 1.0, all the way)",
                        "This is what stops the save being immortality: recovering costs food,",
                        "water and time, and until it is paid the horse is as mortal as any",
                        "other animal.",
                        "Lower it and a horse is rescued again before it has really recovered.",
                        "Set it low enough to sit under behaviour.last_stand_health and the horse",
                        "is re-armed by the save itself, which is immortality - deliberately",
                        "allowed, since a server that types that has asked for it.")
                .defineInRange("behaviour.last_stand_rearm_fraction",
                        com.example.horsegenetics.common.care.LastStand.DEFAULT_REARM_FRACTION, 0.0, 1.0);
        LEADS_RETURN = builder
                .comment("Whether a lead comes back to you rather than falling on the ground. (default: true)",
                        "Two things are covered, and they used to be one.",
                        "First, this mod's own teleports. A whistle, an ender whistle and a",
                        "ticket all have to untie a horse before moving it, and vanilla's rule",
                        "for an untied leash is to drop the lead where the animal was standing.",
                        "The horse lands beside you and the lead stays where it was - up to 64",
                        "blocks off for an echo whistle, and in another dimension entirely for",
                        "an ender whistle or an interdimensional ticket, where it is simply lost.",
                        "On, that lead goes to the player who blew the whistle or used the ticket.",
                        "Second, the leads VANILLA drops: a leash that snapped because the horse",
                        "got too far away, a fence knot broken by hand, or a holder that stopped",
                        "existing. On, that lead goes to whoever tied it on, wherever they are -",
                        "another dimension included - rather than falling at the horse's feet.",
                        "Either way it drops at the recipient's own feet if their pack is full,",
                        "and nothing is ever deleted.",
                        "Off restores vanilla's behaviour exactly, for both halves.",
                        "Three cases stay vanilla's whatever this is set to. A horse that walked",
                        "into a portal was untied next to whoever was holding it, so the lead is",
                        "at their feet already. A lead nobody was recorded as tying on - a horse",
                        "leashed by a command, or before this shipped - has no one to send it to.",
                        "And a lead whose owner is offline falls on the ground rather than being",
                        "held for them.")
                .define("behaviour.leads_return", true);
        MOUNTED_MINING_PENALTY_REMOVED = builder
                .comment("Whether a rider mines at full speed. (default: true)",
                        "ON IS NOT VANILLA. Vanilla mines at a fifth speed whenever the player",
                        "is not standing on the ground - the rule that stops you tunnelling as",
                        "you fall - and a player sitting on a horse is not standing on anything,",
                        "so clearing one sapling out of the path means dismounting for it.",
                        "On, a rider whose horse is itself on the ground mines at the speed they",
                        "would standing there. The horse must be on the ground too: mid-jump, or",
                        "on a flying horse, the penalty still applies, because mining out of the",
                        "air is a different thing from mining from the saddle.",
                        "Off restores vanilla's behaviour exactly.")
                .define("behaviour.mounted_mining_penalty_removed", true);
        RIGHTCLICK_EQUIPS_TACK = builder
                .comment("Whether tack goes on with a right-click. (default: true)",
                        "Vanilla already does this for the saddle and the barding, and always",
                        "has in this version - neither is affected by this option either way.",
                        "What it covers is the seventeen gear slots vanilla cannot see: on, a",
                        "right-click with a piece of gear puts it in the first empty slot that",
                        "takes it; off, the Gear tab on the horse screen is the only way.",
                        "It never swaps. A slot that is already full is left alone and the",
                        "click does what it would have done, which is usually mount the horse.",
                        "Your own horse only, and not a foal - a foal wears no tack at all.")
                .define("behaviour.rightclick_equips_tack", true);
        BOND_DECAY_PER_DAY = builder
                .comment("How much bond a horse loses per Minecraft day. (default: 1)",
                        "Charged for every whole day since the horse last decayed, so a horse",
                        "that sat in an unloaded chunk for a week pays for the week the moment",
                        "it loads. Time, not attention.",
                        "One point a day against a gain cap of fifteen is meant to be trivial to",
                        "out-earn: a horse ridden even occasionally never notices it, and what it",
                        "costs is bond you banked once and then stopped paying for.",
                        "It never goes below behaviour.bond_floor.",
                        "0 turns decay off entirely - bond only ever goes up, as it did before.")
                .defineInRange("behaviour.bond_decay_per_day",
                        com.example.horsegenetics.common.care.Bond.DEFAULT_PER_DAY, 0, 100);
        BOND_FLOOR = builder
                .comment("The bond level decay stops at. (default: 31)",
                        "31 is the bottom of behaviour tier 1, where a horse turns its head to",
                        "face its owner and does nothing else. So neglect can cost a horse the",
                        "tiers that walk toward you (61) and steer bareback (81), and it can",
                        "never cost you the one where it looks up as you walk past.",
                        "It is a place decay stops, not a level bond is held at: a horse below it",
                        "is left alone rather than topped up to it.",
                        "0 lets a horse forget you completely.")
                .defineInRange("behaviour.bond_floor",
                        com.example.horsegenetics.common.care.Bond.DEFAULT_FLOOR, 0, 100);
        OWNER_ONLY_RIDING = builder
                .comment("Whether a horse refuses to be ridden by anybody but its owner. (default: true)",
                        "A horse that refuses rears, throws the rider, and says so on the",
                        "action bar. An UNTAMED horse is never refused, whatever this says -",
                        "getting on one until it stops bucking is how a horse is tamed.",
                        "Turn it off for vanilla's rule, where a tamed horse is a saddle",
                        "anybody can sit in.")
                .define("behaviour.owner_only_riding", true);
        RIDING_ALLOWS_TEAMS = builder
                .comment("Whether the owner's team may ride their horses too. (default: true)",
                        "Three things count, and this one flag switches all three:",
                        "  - a shared vanilla scoreboard team,",
                        "  - FTB Teams membership (which is what FTB Chunks claims are made of),",
                        "  - an FTB Teams ally of the owner's team.",
                        "Off, only the owner ever rides. Does nothing when",
                        "behaviour.owner_only_riding is off, since then everybody may ride.")
                .define("behaviour.riding_allows_teams", true);
        HORSE_GIVE_COMMAND = builder
                .comment("Whether /horsegive exists. (default: true)",
                        "It hands the horse you are on, or looking at, to another player -",
                        "the same transfer a signed paper makes, without the walk. Ownership",
                        "is the only thing that moves; who bred it never changes.",
                        "Off, the command is not registered at all and the transfer paper",
                        "stays the only way a horse changes hands.")
                .define("commands.horse_give", true);
        HORSE_JOCKEY_COMMAND = builder
                .comment("Whether /horsejockey exists. (default: true)",
                        "It lends the horse you are on, or looking at, to another player",
                        "for a while - the same thing a jockey pass buys with an item, which",
                        "this does not affect. Off, the command is not registered at all.")
                .define("commands.horse_jockey", true);
        UNDEAD_CONVERT = builder
                .comment("Whether vanilla zombie and skeleton horses become this mod's horses. (default: true)",
                        "Each one converts the first time it is ticked - old saves included - into a",
                        "Graveborn Warmblood or a Great Valley Skeleton Horse, keeping its name, owner,",
                        "saddle, armour, lead, age and health fraction; its stats are re-rolled from its",
                        "genes. A skeleton trap converts only after it has sprung, and a horse with a",
                        "player on it waits for the rider to get off. Off, vanilla undead horses are",
                        "left alone; ones already converted stay converted.")
                .define("undead.convert", true);
        JOCKEY_PASS_DAYS = builder
                .comment("How many Minecraft days one jockey pass is worth. (default: 1)",
                        "Also the length of a bare /horsejockey with no number given.",
                        "Feeding a second pass ADDS another of these rather than replacing",
                        "what is left, so a three-day meeting is three passes.")
                .defineInRange("behaviour.jockey_pass_days", 1, 1, 365);
        SEND_HOME_PAYMENT_ITEM = builder
                .comment("What the horse menu's Send home button costs per trip, as an item id. (default: \"\")",
                        "Empty - the default - makes the button free: it sends a horse to its stall,",
                        "or your holding pen if it has none, from any world and out of a stasis",
                        "chamber, with nothing spent. Name any item, vanilla or modded, such as",
                        "\"minecraft:emerald\", and each trip takes behaviour.send_home_payment_count",
                        "of it. A trip that is refused takes nothing.",
                        "A price REPLACES tickets for the button; it never charges both. Tickets and",
                        "whistles still move a horse from the world with no menu, and the button's",
                        "tooltip and refusal say so.",
                        "An id that is not a registered item is logged once and treated as free.",
                        "Server-side; clients read the synced value only to label the button.")
                .define("behaviour.send_home_payment_item", "",
                        o -> o instanceof String s && (s.isBlank()
                                || net.minecraft.resources.Identifier.tryParse(s.trim()) != null));
        SEND_HOME_PAYMENT_COUNT = builder
                .comment("How many of behaviour.send_home_payment_item one Send home trip takes. (default: 1)",
                        "0 makes the button free whatever item is named.")
                .defineInRange("behaviour.send_home_payment_count", 1, 0, 64);
        SEND_HOME_COOLDOWN_SECONDS = builder
                .comment("Seconds a player waits between two Send home trips. (default: "
                                + com.example.horsegenetics.common.care.SendHome.DEFAULT_COOLDOWN_SECONDS + ")",
                        "Per player, not per horse, and started only by a trip that happened - a",
                        "refusal never starts it. 0 turns it off.")
                .defineInRange("behaviour.send_home_cooldown_seconds",
                        com.example.horsegenetics.common.care.SendHome.DEFAULT_COOLDOWN_SECONDS, 0, 3600);
        CHAOS_EXCLUDE_MODS = builder
                .comment("Mods whose mobs a Chaos allele may never name, by mod id. (default: [])",
                        "Lycanthropy, Leader of the pack and Spawner each have one Chaos allele that",
                        "stands in for every modded mob at once: a seed on the horse picks one from",
                        "the mods this server has loaded. Vanilla mobs are never on that list - they",
                        "have alleles of their own. Spawner reaches monsters and bosses on purpose;",
                        "this list, and the next, are how a boss is taken off it.",
                        "A seed picks by position, so adding or removing a mod here or in the pack",
                        "can change what an existing Chaos horse turns into. That is the point of it.",
                        "Example: [\"somemod\", \"othermod\"]")
                .defineListAllowEmpty("chaos.exclude_mods", java.util.List.of(), () -> "somemod",
                        o -> o instanceof String s && !s.isBlank());
        CHAOS_EXCLUDE_IDS = builder
                .comment("Single mobs a Chaos allele may never name, as modid:mob. (default: [])",
                        "Applied on top of chaos.exclude_mods. Example: [\"somemod:dragon_king\"]")
                .defineListAllowEmpty("chaos.exclude_ids", java.util.List.of(), () -> "somemod:somemob",
                        o -> o instanceof String s && s.indexOf(':') > 0);
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

    /** {@code chaos.exclude_mods}, safely - empty when the config is not loaded yet. */
    public static java.util.List<String> chaosExcludeMods() {
        try {
            return java.util.List.copyOf(CHAOS_EXCLUDE_MODS.get());
        } catch (IllegalStateException notLoaded) {
            return java.util.List.of();
        }
    }

    /** {@code chaos.exclude_ids}, safely - empty when the config is not loaded yet. */
    public static java.util.List<String> chaosExcludeIds() {
        try {
            return java.util.List.copyOf(CHAOS_EXCLUDE_IDS.get());
        } catch (IllegalStateException notLoaded) {
            return java.util.List.of();
        }
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
     * means every horse is compressed to near the vanilla size - see the {@code body.size} section
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
     * <b>How crowded she is allowed to be</b>, for this world, now -
     * {@code fertility.nearby_horse_cap}, safely.
     */
    public static int nearbyHorseCap() {
        try {
            return NEARBY_HORSE_CAP.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.repro.ReproRules.DEFAULT_NATURAL_CAP;
        }
    }

    /** {@code fertility.free_covers_per_day}, safely. */
    public static int freeCoversPerDay() {
        try {
            return FREE_COVERS_PER_DAY.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.repro.ReproRules.DEFAULT_FREE_COVERS_PER_DAY;
        }
    }

    /**
     * <b>A stallion's day, on this world's allowance</b> - the only way the game
     * module hands a cover count to {@code common/}, so no caller can fall back
     * to the default by forgetting the config.
     */
    public static com.example.horsegenetics.common.repro.StallionDay stallionDay(int coversToday) {
        return new com.example.horsegenetics.common.repro.StallionDay(coversToday, freeCoversPerDay());
    }

    /** {@code realm.breeding_rate_percent}, safely. */
    public static int realmBreedingRatePercent() {
        try {
            return REALM_BREEDING_RATE.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.realm.RealmClock.DEFAULT_RATE_PERCENT;
        }
    }

    /** {@code realm.pause_when_unloaded}, safely. */
    public static boolean realmPauseWhenUnloaded() {
        try {
            return REALM_PAUSE_WHEN_UNLOADED.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.realm.RealmClock.DEFAULT_PAUSE_WHEN_UNLOADED;
        }
    }

    /** Both realm breeding settings, as the value {@code common/realm/RealmClock} takes. */
    public static com.example.horsegenetics.common.realm.RealmClock.Pace realmPace() {
        return new com.example.horsegenetics.common.realm.RealmClock.Pace(
                realmBreedingRatePercent(), realmPauseWhenUnloaded());
    }

    /** {@code realm.release_emeralds}, safely. */
    public static int realmReleaseEmeralds() {
        try {
            return REALM_RELEASE_EMERALDS.get();
        } catch (IllegalStateException notLoaded) {
            return 2;
        }
    }

    /**
     * {@code ops.resurrect_grace_minutes} as <b>ticks</b>, safely - {@code 0}
     * meaning "for ever", which every caller has to test for rather than treat
     * as an expired window.
     */
    public static int resurrectGraceTicks() {
        int minutes;
        try {
            minutes = RESURRECT_GRACE_MINUTES.get();
        } catch (IllegalStateException notLoaded) {
            minutes = 0;
        }
        return minutes * 60 * 20;
    }

    /** {@code ops.resurrect_budget_mb} as <b>bytes</b>, safely - {@code 0} meaning "no cap". */
    public static long resurrectBudgetBytes() {
        int mb;
        try {
            mb = RESURRECT_BUDGET_MB.get();
        } catch (IllegalStateException notLoaded) {
            mb = com.example.horsegenetics.common.horse.AfterlifeBudget.DEFAULT_BUDGET_MB;
        }
        return mb * com.example.horsegenetics.common.horse.AfterlifeBudget.BYTES_PER_MB;
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

    /** {@code notices.owned_horse_breeding}, safely. */
    public static boolean breedingNotices() {
        try {
            return BREEDING_NOTICES.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code behaviour.escape_health_fraction}, safely. */
    public static double escapeHealthFraction() {
        try {
            return ESCAPE_HEALTH_FRACTION.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.care.Escape.DEFAULT_THRESHOLD;
        }
    }

    /** {@code behaviour.emergency_stasis_fraction}, safely. */
    public static double emergencyStasisFraction() {
        try {
            return EMERGENCY_STASIS_FRACTION.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.horse.StasisRescue.DEFAULT_THRESHOLD;
        }
    }

    /** {@code behaviour.last_stand}, safely. */
    public static boolean lastStand() {
        try {
            return LAST_STAND.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code behaviour.last_stand_health}, safely. */
    public static double lastStandHealth() {
        try {
            return LAST_STAND_HEALTH.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.care.LastStand.DEFAULT_HEALTH_LEFT;
        }
    }

    /** {@code behaviour.last_stand_immunity_ticks}, safely. */
    public static int lastStandImmunityTicks() {
        try {
            return LAST_STAND_IMMUNITY_TICKS.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.care.LastStand.DEFAULT_IMMUNITY_TICKS;
        }
    }

    /** {@code behaviour.owner_only_riding}, safely. */
    public static boolean ownerOnlyRiding() {
        try {
            return OWNER_ONLY_RIDING.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code behaviour.riding_allows_teams}, safely. */
    public static boolean ridingAllowsTeams() {
        try {
            return RIDING_ALLOWS_TEAMS.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code commands.horse_give}, safely. */
    public static boolean horseGiveCommand() {
        try {
            return HORSE_GIVE_COMMAND.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code undead.convert}, safely. */
    public static boolean undeadConvert() {
        try {
            return UNDEAD_CONVERT.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code commands.horse_jockey}, safely. */
    public static boolean horseJockeyCommand() {
        try {
            return HORSE_JOCKEY_COMMAND.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code behaviour.jockey_pass_days} as ticks, safely. */
    public static long jockeyPassTicks() {
        try {
            return JOCKEY_PASS_DAYS.get() * 24_000L;
        } catch (IllegalStateException notLoaded) {
            return 24_000L;
        }
    }

    /**
     * {@code behaviour.send_home_payment_item} and {@code _count}, safely, as
     * one price. Read on the client too, for the button's label: a SERVER
     * config is synced on connection, and before that lands this is free.
     */
    public static com.example.horsegenetics.common.care.SendHome.Price sendHomePrice() {
        try {
            return com.example.horsegenetics.common.care.SendHome.Price.of(
                    SEND_HOME_PAYMENT_ITEM.get(), SEND_HOME_PAYMENT_COUNT.get());
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.care.SendHome.Price.FREE;
        }
    }

    /** {@code behaviour.send_home_cooldown_seconds}, safely. */
    public static int sendHomeCooldownSeconds() {
        try {
            return SEND_HOME_COOLDOWN_SECONDS.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.care.SendHome.DEFAULT_COOLDOWN_SECONDS;
        }
    }

    /** {@code behaviour.leads_return}, safely. */
    public static boolean leadsReturn() {
        try {
            return LEADS_RETURN.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /**
     * {@code behaviour.mounted_mining_penalty_removed}, safely. The catch is
     * load-bearing on the client as well as at startup here: a SERVER config is
     * synced on connection, so a client reading it at the title screen - or
     * before the sync lands - gets the default rather than an exception.
     */
    public static boolean mountedMiningPenaltyRemoved() {
        try {
            return MOUNTED_MINING_PENALTY_REMOVED.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code behaviour.rightclick_equips_tack}, safely. */
    public static boolean rightClickEquipsTack() {
        try {
            return RIGHTCLICK_EQUIPS_TACK.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** {@code behaviour.last_stand_rearm_fraction}, safely. */
    public static double lastStandRearmFraction() {
        try {
            return LAST_STAND_REARM_FRACTION.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.care.LastStand.DEFAULT_REARM_FRACTION;
        }
    }

    /** {@code behaviour.bond_decay_per_day}, safely. */
    public static int bondDecayPerDay() {
        try {
            return BOND_DECAY_PER_DAY.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.care.Bond.DEFAULT_PER_DAY;
        }
    }

    /** {@code behaviour.bond_floor}, safely. */
    public static int bondFloor() {
        try {
            return BOND_FLOOR.get();
        } catch (IllegalStateException notLoaded) {
            return com.example.horsegenetics.common.care.Bond.DEFAULT_FLOOR;
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
