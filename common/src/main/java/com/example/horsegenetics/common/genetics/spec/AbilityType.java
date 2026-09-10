package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.genetics.spec.GeneAbility.Condition;
import com.example.horsegenetics.common.genetics.spec.GeneAbility.Trigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One <b>effect verb</b>, as a self-contained module.
 *
 * <p>Everything about an effect's <i>shape</i> - its JSON {@code "type"} name,
 * the parameters it accepts (name, kind, default, doc), the validation, and how
 * to build its {@link GeneAbility} record - lives in a single
 * {@link #register registered} instance below. {@link GeneSpecParser} reads any
 * effect <b>generically</b> off this table; it has no per-verb code. So adding
 * an effect is exactly:
 *
 * <ol>
 *   <li>a {@code record} on {@link GeneAbility} (its fields + {@code when} +
 *       {@code minDose}, and a {@link Trigger} field if it is event-driven);</li>
 *   <li>one {@code register(new AbilityType(...))} line here;</li>
 *   <li>a branch in the NeoForge translator ({@code server/GeneAbilityHandler}
 *       for tick/triggered effects, {@code server/GeneYieldHandler} for
 *       interaction effects);</li>
 *   <li>a section in {@code wiki/making-a-gene.html}.</li>
 * </ol>
 *
 * <p>This class also owns the two vocabularies <b>shared by every effect</b>:
 * the {@link #CONDITION_FLAGS} a {@code "when"} may name, and the trigger names.
 * Per-effect vocabularies (traversal flags, emitter shapes, ...) live in the
 * relevant {@link AbilityType} declaration, not here - the point is that one
 * place describes one effect.
 *
 * <p>Pure {@code common/}: no Minecraft imports. The translator maps every name
 * here to a game call.
 */
public final class AbilityType {

    private AbilityType(String name, List<Param> params, Builder builder) {
        this.name = name;
        this.params = List.copyOf(params);
        this.builder = builder;
    }

    // ================================================================
    // Shared vocabulary: conditions and triggers
    // ================================================================

    /**
     * Predicate flags a {@code "when"} condition may name. Boolean reads off the
     * live horse; the translator ({@code GeneAbilityHandler.flagHolds}) owns the
     * mapping to game state. Add a flag here <b>and</b> there in the same change.
     */
    public static final List<String> CONDITION_FLAGS = List.of(
            "sex_female", "sex_male", "tamed", "untamed", "adult", "baby",
            "full_health", "has_rider", "in_water", "submerged", "on_ground", "on_fire",
            "day", "night", "raining", "thundering", "sky_visible",
            // The three that read the WORLD rather than a field on the horse -
            // see WORLD_FLAGS below, and the interval sampling in the translator.
            "dark", "near_jukebox", "snowing");

    /**
     * The flags whose answer comes from a <b>world lookup</b> rather than a
     * field read on the horse: a block-light sample, a block search, a biome
     * query.
     *
     * <p>Every other flag is a getter and costs nothing to ask every tick. These
     * three are not, and a condition is evaluated once per ability per tick - so
     * a horse expressing three conditioned effects would do three block searches
     * a tick if they were treated alike. The translator samples them on an
     * interval and caches; this list is what tells it which ones to treat that
     * way, so that adding a fourth world-reading flag is one line here rather
     * than a performance bug nobody notices.
     */
    public static final List<String> WORLD_FLAGS = List.of("dark", "near_jukebox", "snowing");

    /**
     * <b>Which creatures a radius effect is about.</b> Shared by
     * {@link #MOB_AURA}, {@link #HEALING} and {@link #TEMPER}, deliberately: five
     * genes want to name a set of mobs and three verbs would otherwise grow three
     * different vocabularies for it.
     *
     * <p>The translator is required to resolve these against <b>entity type
     * tags</b> rather than a hardcoded list of vanilla mobs. A hardcoded list
     * makes every modded creature invisible to all five genes at once, and the
     * failure is silent - the aura simply never fires on anything from another
     * mod.
     *
     * <p>{@code non_horse} is the odd one out and earns its place: several genes
     * are about everything <i>except</i> the horse's own kind, and expressing
     * that as a negation would need a combinator the vocabulary does not have.
     */
    public static final List<String> MOB_GROUPS = List.of(
            "players", "passive", "hostile", "undead", "animals", "non_horse", "all");

    /**
     * Trigger names. {@code continuous} / {@code on_move} take no argument (and
     * may be written as a bare string); {@code interval} takes a tick count and
     * {@code on_interact} an item id, so those must be an object.
     */
    public static final List<String> TRIGGERS = List.of("continuous", "on_move", "interval", "on_interact",
            "on_hurt", "on_owner_hurt");

    // ================================================================
    // A parameter, and the parsed bag handed to a builder
    // ================================================================

    /** How a parameter's JSON value is read. Simpler than {@code SpecSchema.Kind} - an effect never varies per horse. */
    public enum Kind { STRING, CHOICE, NUMBER, BOOL, COLOR, TRIGGER, PARTS }

    /**
     * One parameter of an effect. {@code fallback} is what the parser uses when
     * the file omits the key - its runtime type follows {@code kind}: a
     * {@code String} for STRING / CHOICE / COLOR, a {@code Double} for NUMBER, a
     * {@code Boolean} for BOOL, a {@link Trigger} for TRIGGER, an immutable
     * {@code List<Part>} for PARTS. A {@code null} fallback on a STRING / CHOICE
     * means the key is <b>required</b>.
     */
    public record Param(String name, Kind kind, Object fallback, List<String> choices, String doc) {

        static Param str(String name, String fallback, String doc) {
            return new Param(name, Kind.STRING, fallback, List.of(), doc);
        }

        static Param required(String name, String doc) {
            return new Param(name, Kind.STRING, null, List.of(), doc);
        }

        static Param choice(String name, List<String> choices, String fallback, String doc) {
            return new Param(name, Kind.CHOICE, fallback, choices, doc);
        }

        static Param requiredChoice(String name, List<String> choices, String doc) {
            return new Param(name, Kind.CHOICE, null, choices, doc);
        }

        static Param num(String name, double fallback, String doc) {
            return new Param(name, Kind.NUMBER, fallback, List.of(), doc);
        }

        static Param bool(String name, boolean fallback, String doc) {
            return new Param(name, Kind.BOOL, fallback, List.of(), doc);
        }

        static Param color(String name, String fallback, String doc) {
            return new Param(name, Kind.COLOR, fallback, List.of(), doc);
        }

        static Param trigger(String name, Trigger fallback, String doc) {
            return new Param(name, Kind.TRIGGER, fallback, List.of(), doc);
        }

        static Param parts(String name, String doc) {
            return new Param(name, Kind.PARTS, List.of(), List.of(), doc);
        }
    }

    /**
     * Every parameter of one effect instance, already type- and choice-checked
     * by {@link GeneSpecParser}, plus the shared {@code when} / {@code minDose}.
     * A {@link Builder} reads it and may throw {@link #bad} for a range check.
     * Package-private: only the parser builds one, only a builder reads one.
     */
    static final class Values {
        final Map<String, Object> raw = new LinkedHashMap<>();
        Condition when = Condition.ALWAYS;
        int minDose = 1;
        String where = "effect";

        String str(String key)     { return (String) raw.get(key); }
        double num(String key)     { return (Double) raw.get(key); }
        int intOf(String key)      { return (int) (double) (Double) raw.get(key); }
        boolean bool(String key)   { return (Boolean) raw.get(key); }
        int color(String key)      { return (Integer) raw.get(key); }
        Trigger trigger(String key) { return (Trigger) raw.get(key); }
        @SuppressWarnings("unchecked")
        List<Part> parts(String key) { return (List<Part>) raw.get(key); }

        IllegalArgumentException bad(String message) {
            return new IllegalArgumentException(where + ": " + message);
        }
    }

    /** Builds the record for one effect from its parsed {@link Values}. May validate and throw {@link Values#bad}. */
    @FunctionalInterface
    interface Builder {
        GeneAbility build(Values values);
    }

    // ================================================================
    // The registry
    // ================================================================

    private static final Map<String, AbilityType> BY_NAME = new LinkedHashMap<>();

    private final String name;
    private final List<Param> params;
    private final Builder builder;

    private static AbilityType register(AbilityType type) {
        BY_NAME.put(type.name, type);
        return type;
    }

    public String name() {
        return name;
    }

    public List<Param> params() {
        return params;
    }

    /** {@code "type"} + every param name + {@code when} + {@code minDose} - the keys an effect object may hold. */
    public List<String> allowedKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("type");
        for (Param p : params) {
            keys.add(p.name());
        }
        keys.add("when");
        keys.add("minDose");
        return keys;
    }

    GeneAbility build(Values values) {
        return builder.build(values);
    }

    /** The effect type for a JSON {@code "type"} value, or an error that lists them all. */
    public static AbilityType byName(String typeName) {
        AbilityType type = BY_NAME.get(typeName.toLowerCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("unknown effect type '" + typeName
                    + "'; allowed are " + BY_NAME.keySet());
        }
        return type;
    }

    /** Every registered effect type, in declaration order - what {@code wiki/making-a-gene.html} lists. */
    public static Collection<AbilityType> all() {
        return BY_NAME.values();
    }

    // ------------------------------------------------------------------

    /** {@code value} lower-cased, or an error naming it and listing {@code choices}. */
    static String requireOneOf(List<String> choices, String value, String where) {
        String v = value.toLowerCase(Locale.ROOT);
        if (!choices.contains(v)) {
            throw new IllegalArgumentException(where + ": '" + value + "' is not one of " + choices);
        }
        return v;
    }

    // ================================================================
    // The effects - one self-contained declaration each.
    // ================================================================

    /** Movement / survival flag held up while {@code when} is true. */
    public static final AbilityType TRAVERSAL = register(new AbilityType("traversal",
            List.of(
                    Param.requiredChoice("flag", List.of(
                                    "walk_on_water", "walk_on_lava", "lava_swim", "fire_immune",
                                    "fall_immune", "underwater_breathing", "water_averse"),
                            "the movement / survival flag to grant"),
                    Param.choice("target", List.of("self", "rider", "both"), "self",
                            "who the flag protects - 'rider' and 'both' reach the PLAYER, which "
                                    + "the translator must take back off on dismount")),
            v -> new GeneAbility.Traversal(v.str("flag"), v.str("target"), v.when, v.minDose)));

    /** Temporary attribute modifier, present while {@code when} holds. */
    public static final AbilityType ATTRIBUTE = register(new AbilityType("attribute",
            List.of(
                    Param.requiredChoice("attribute", List.of(
                            "movement_speed", "jump_strength", "max_health", "armor", "armor_toughness",
                            "knockback_resistance", "step_height", "safe_fall_distance", "scale",
                            "water_movement_efficiency", "movement_efficiency", "oxygen_bonus",
                            "gravity"),
                            "the attribute to modify. There is no 'swim_speed' - vanilla has no "
                                    + "such attribute; what it has is 'water_movement_efficiency', "
                                    + "the share of its land speed a mob keeps in water"),
                    Param.choice("op", List.of("add", "multiply_base", "multiply_total"), "add",
                            "how 'amount' is applied - vanilla modifier operations"),
                    Param.num("amount", 0, "signed modifier amount")),
            v -> new GeneAbility.AttributeMod(
                    v.str("attribute"), v.str("op"), v.num("amount"), v.when, v.minDose)));

    /**
     * Where on the horse an {@link #EMITTER} is centred. The first four are
     * single points; the last five are <b>body sites</b> the translator picks a
     * point within, which is what lets one emitter trail off four hooves or run
     * the length of the spine rather than beading out of one spot.
     */
    public static final List<String> EMITTER_ANCHORS = List.of(
            "feet", "body", "head", "eyes",
            "spine", "hooves", "front_hooves", "back_hooves", "tail");

    /** The most particles one firing may spawn. A guard, not a design - see the particle locus. */
    public static final int MAX_EMITTER_COUNT = 16;

    /**
     * The slowest a {@code cycle} may turn - ten Minecraft minutes for one lap
     * of the hue circle. A guard against a number that would read as "stuck on
     * green" rather than as a rainbow, not a design.
     */
    public static final int MAX_CYCLE_TICKS = 12_000;

    /** Particle (or, one day, light) emitter fired by a {@link Trigger}. */
    public static final AbilityType EMITTER = register(new AbilityType("emitter",
            List.of(
                    Param.choice("kind", List.of("particle", "light"), "particle",
                            "'particle', or 'light' (not wired yet)"),
                    Param.choice("shape", List.of("point", "ring", "trail", "burst"), "point",
                            "emission shape"),
                    Param.choice("anchor", EMITTER_ANCHORS, "feet",
                            "where on the horse it is centred"),
                    Param.trigger("trigger", new Trigger.OnMove(), "when it fires (default on_move)"),
                    Param.str("particle", "minecraft:dust",
                            "particle id; 'minecraft:dust' is the one that takes 'color'"),
                    Param.color("color", "#ffffff", "0xRRGGBB, used by particle types that take a colour"),
                    Param.color("color2", "#ffffff",
                            "the second 0xRRGGBB, for a particle that fades between two"),
                    Param.num("count", 1, "particles per firing, 1.." + MAX_EMITTER_COUNT),
                    Param.num("data", 0.0,
                            "a normalised [0,1) number for whatever else the particle takes - "
                                    + "a shriek's delay, a note's pitch, a sculk charge's roll"),
                    Param.num("chance", 1.0, "per-fire probability, in (0, 1]"),
                    Param.num("cycle", 0,
                            "ticks for one full rotation of the hue circle - the trail cycles the "
                                    + "rainbow and 'color' / 'color2' are ignored; 0 = fixed colours")),
            v -> {
                double chance = v.num("chance");
                if (chance <= 0 || chance > 1) {
                    throw v.bad("chance must be in (0, 1], got " + chance);
                }
                int count = v.intOf("count");
                if (count < 1 || count > MAX_EMITTER_COUNT) {
                    throw v.bad("count must be in [1, " + MAX_EMITTER_COUNT + "], got " + count);
                }
                double data = v.num("data");
                if (data < 0 || data >= 1) {
                    throw v.bad("data must be in [0, 1), got " + data);
                }
                int cycle = v.intOf("cycle");
                if (cycle < 0 || cycle > MAX_CYCLE_TICKS) {
                    throw v.bad("cycle must be in [0, " + MAX_CYCLE_TICKS + "] ticks, got " + cycle);
                }
                return new GeneAbility.Emitter(v.str("kind"), v.str("shape"), v.str("anchor"),
                        v.trigger("trigger"), v.color("color"), v.color("color2"), count, data,
                        v.str("particle"), chance, cycle, v.when, v.minDose);
            }));

    /** Mob effect kept topped up on self or rider while {@code when} holds. */
    public static final AbilityType MOB_EFFECT = register(new AbilityType("mob_effect",
            List.of(
                    Param.required("effect", "mob effect id, e.g. 'minecraft:dolphins_grace'"),
                    Param.choice("target", List.of("self", "rider"), "self", "who the effect lands on"),
                    Param.num("amplifier", 0, "0-based amplifier"),
                    Param.num("refresh", 40, "re-apply every N ticks (at least 1)")),
            v -> {
                int refresh = v.intOf("refresh");
                if (refresh < 1) {
                    throw v.bad("refresh must be at least 1 tick, got " + refresh);
                }
                return new GeneAbility.SelfEffect(v.str("effect"), v.str("target"),
                        v.intOf("amplifier"), refresh, v.when, v.minDose);
            }));

    /** Something the horse hands back on a right-click. Fires on {@code on_interact} only. */
    public static final AbilityType YIELD = register(new AbilityType("yield",
            List.of(
                    Param.trigger("trigger", new Trigger.OnInteract(""),
                            "on_interact only - the item that triggers it, or \"\" for anything"),
                    Param.str("consumes", "", "item id taken from the hand, or \"\" for nothing"),
                    Param.str("produces", "", "item id handed back"),
                    Param.num("cooldown", 0, "per-horse cooldown, ticks"),
                    Param.num("denied_damage", 0, "damage dealt when 'when' fails (e.g. a stallion kick); 0 = none"),
                    Param.str("denied_message", "", "message shown when 'when' fails, or \"\" for silent"),
                    Param.str("kind", "",
                            "a name for what sort of yield this is, so a 'charges' effect on some "
                                    + "OTHER gene can grant extra uses of it. \"\" opts out"),
                    Param.str("potion_effect", "",
                            "a mob effect id to attach to what is produced, or \"\" for a plain "
                                    + "item. Two yields of the same 'kind' that both name one are "
                                    + "MERGED into a single item carrying both - which is what "
                                    + "makes a compound heterozygote hand back one bottle rather "
                                    + "than the first of two"),
                    Param.num("potion_amplifier", 0, "0-based amplifier for potion_effect"),
                    Param.num("potion_duration", 900, "duration of potion_effect, in ticks")),
            v -> {
                if (!(v.trigger("trigger") instanceof Trigger.OnInteract onInteract)) {
                    throw v.bad("a yield fires on 'on_interact' only");
                }
                int duration = v.intOf("potion_duration");
                if (duration < 1) {
                    throw v.bad("potion_duration must be at least 1 tick, got " + duration);
                }
                return new GeneAbility.Yield(onInteract, v.str("consumes"), v.str("produces"),
                        v.intOf("cooldown"), v.num("denied_damage"), v.str("denied_message"),
                        v.str("kind"), v.str("potion_effect"), v.intOf("potion_amplifier"), duration,
                        v.when, v.minDose);
            }));

    /**
     * Makes the horse a <b>light source</b> and/or marks body {@code parts} as
     * <b>full-bright</b> in the coat render - the "this gene glows" verb.
     * {@code light} is a 0-15 world light level (0 = no dynamic light);
     * {@code parts} names the emissive coat regions (empty = none). The
     * emissive colour is whatever the coat layers already painted there, drawn
     * again at full brightness.
     */
    public static final AbilityType GLOW = register(new AbilityType("glow",
            List.of(
                    Param.num("light", 0, "world light level 0-15 the horse emits (0 = none)"),
                    Param.parts("parts", "coat regions that render full-bright, e.g. [\"HAIR\"] (empty = none)")),
            v -> {
                int light = v.intOf("light");
                if (light < 0 || light > 15) {
                    throw v.bad("light must be 0-15, got " + light);
                }
                return new GeneAbility.Glow(light, v.parts("parts"), v.when, v.minDose);
            }));

    /**
     * A <b>healing aura</b> around the horse - the first effect that reaches
     * anything other than the horse and its rider. {@code target} says who is
     * caught by it, {@code radius} how far it reaches, {@code amount} how much
     * health each beat restores and {@code interval} how far apart the beats
     * are.
     */
    public static final AbilityType HEALING = register(new AbilityType("healing",
            List.of(
                    Param.choice("target", List.of("players", "rider", "self", "animals", "group"),
                            "players",
                            "who the aura reaches; 'group' defers to the 'group' parameter below"),
                    Param.choice("group", MOB_GROUPS, "animals",
                            "used when target is 'group' - notably 'undead', which with a negative "
                                    + "amount is a damaging aura rather than a healing one"),
                    Param.num("radius", 3, "reach in blocks, 1-16"),
                    Param.num("amount", 1, "health points restored per beat (two per heart)"),
                    Param.num("interval", 40, "ticks between beats (at least 1)"),
                    Param.num("max_targets", 8, "most entities one beat may reach, 1-64")),
            v -> {
                double radius = v.num("radius");
                if (radius < 1 || radius > 16) {
                    throw v.bad("radius must be 1-16 blocks, got " + radius);
                }
                int interval = v.intOf("interval");
                if (interval < 1) {
                    throw v.bad("interval must be at least 1 tick, got " + interval);
                }
                int maxTargets = v.intOf("max_targets");
                if (maxTargets < 1 || maxTargets > 64) {
                    throw v.bad("max_targets must be 1-64, got " + maxTargets);
                }
                return new GeneAbility.Healing(v.str("target"), v.str("group"), radius, v.num("amount"), interval,
                        maxTargets, v.when, v.minDose);
            }));

    /**
     * <b>Ground cover spreading from the hooves</b>. {@code cover} names one of
     * a small closed set of conversions the translator knows how to make -
     * {@code mycelium} / {@code moss} / {@code grass} - rather than a block id,
     * because "spreading moss" is a family of conversions plus a rule about
     * what it will and will not eat.
     */
    public static final AbilityType SPREAD = register(new AbilityType("spread",
            List.of(
                    Param.requiredChoice("cover", List.of("mycelium", "moss", "grass", "sapling", "melt"),
                            "what spreads from the horse. 'sapling' plants one; 'melt' takes snow "
                                    + "and ice away. Both are vocabulary words, not block ids - "
                                    + "which blocks may be converted stays with the translator, and "
                                    + "is what stops either eating a block somebody placed"),
                    Param.num("radius", 2, "reach in blocks, 1-8"),
                    Param.num("chance", 0.5, "per-beat probability, in (0, 1]"),
                    Param.num("interval", 40, "ticks between beats (at least 1)")),
            v -> {
                double radius = v.num("radius");
                if (radius < 1 || radius > 8) {
                    throw v.bad("radius must be 1-8 blocks, got " + radius);
                }
                double chance = v.num("chance");
                if (chance <= 0 || chance > 1) {
                    throw v.bad("chance must be in (0, 1], got " + chance);
                }
                int interval = v.intOf("interval");
                if (interval < 1) {
                    throw v.bad("interval must be at least 1 tick, got " + interval);
                }
                return new GeneAbility.Spread(v.str("cover"), radius, chance, interval, v.when, v.minDose);
            }));

    /** The most extra charges one locus may hand a yield. A guard against a typo, not a design. */
    public static final int MAX_EXTRA_CHARGES = 64;

    /**
     * <b>Extra uses of somebody else's yield</b> before its cooldown bites -
     * the "this horse can be milked more than once a day" verb.
     *
     * <p>It names a {@link #YIELD} {@code kind} rather than a gene, and that is
     * the whole design. The gene that <i>produces</i> a thing and the gene that
     * decides <i>how often</i> are different loci, and neither can see the
     * other's epigenome; naming the kind means one volume locus governs every
     * gene that produces that kind - the plain milk, the water, the lava, and
     * anything written later - instead of each new producer having to remember
     * to ask.
     */
    public static final AbilityType CHARGES = register(new AbilityType("charges",
            List.of(
                    Param.required("kind", "the yield 'kind' this grants extra uses of"),
                    Param.num("extra", 1, "additional uses per cooldown window, 1.." + MAX_EXTRA_CHARGES)),
            v -> {
                int extra = v.intOf("extra");
                if (extra < 1 || extra > MAX_EXTRA_CHARGES) {
                    throw v.bad("extra must be in [1, " + MAX_EXTRA_CHARGES + "], got " + extra);
                }
                return new GeneAbility.YieldCharges(v.str("kind"), extra, v.when, v.minDose);
            }));

    /** The widest either way a {@link #BREATH} multiplier may go. A guard, not a design. */
    public static final double MIN_BREATH_FACTOR = 0.05;
    public static final double MAX_BREATH_FACTOR = 40.0;

    /**
     * <b>How long the horse lasts under water</b>, as a multiplier on its air
     * supply. The graded counterpart of the {@code underwater_breathing}
     * traversal flag, which is absolute; the two compose the way you would
     * expect, in that a horse which cannot drown does not care how slowly it
     * would have.
     */
    public static final AbilityType BREATH = register(new AbilityType("breath",
            List.of(Param.num("factor", 1.0,
                    "multiplier on the air supply - 2 lasts twice as long, 0.5 half")),
            v -> {
                double factor = v.num("factor");
                if (factor < MIN_BREATH_FACTOR || factor > MAX_BREATH_FACTOR) {
                    throw v.bad("factor must be in [" + MIN_BREATH_FACTOR + ", "
                            + MAX_BREATH_FACTOR + "], got " + factor);
                }
                return new GeneAbility.Breath(factor, v.when, v.minDose);
            }));

    /**
     * What the world does where the horse died. Items are deliberately a
     * different verb ({@link #ITEM_DROP}): what a horse leaves behind and what
     * happens to the ground it died on are two questions, and folding them
     * together would make "drops diamonds and leaves a crater" impossible to
     * express as the two independent loci it is.
     */
    public static final AbilityType ON_DEATH = register(new AbilityType("on_death",
            List.of(Param.requiredChoice("effect", List.of("lava", "water", "explode"),
                    "what happens at the horse's feet when it dies")),
            v -> new GeneAbility.OnDeath(v.str("effect"), v.when, v.minDose)));

    /** The most items one death may produce. A guard, not a design. */
    public static final int MAX_DROP_COUNT = 64;

    /** What the horse leaves behind. Nothing here touches the world - see {@link #ON_DEATH}. */
    public static final AbilityType ITEM_DROP = register(new AbilityType("item_drop",
            List.of(
                    Param.requiredChoice("drop",
                            List.of("vanilla", "diamonds", "spawn_egg", "enchanted_sword", "meat"),
                            "what the horse drops. Every value but 'meat' REPLACES the vanilla "
                                    + "drop; 'meat' is added beside it"),
                    Param.num("min", 1, "fewest items, 0.." + MAX_DROP_COUNT),
                    Param.num("max", 1, "most items, at least 'min'")),
            v -> {
                int min = v.intOf("min");
                int max = v.intOf("max");
                if (min < 0 || max > MAX_DROP_COUNT || min > max) {
                    throw v.bad("min/max must satisfy 0 <= min <= max <= " + MAX_DROP_COUNT
                            + ", got " + min + ".." + max);
                }
                return new GeneAbility.ItemDrop(v.str("drop"), min, max, v.when, v.minDose);
            }));

    /** How mobs feel about the horse. */
    public static final AbilityType MOB_AURA = register(new AbilityType("mob_aura",
            List.of(
                    Param.requiredChoice("mode", List.of("repel", "attract", "follow"),
                            "'repel' keeps mobs outside the radius; 'attract' makes hostiles "
                                    + "inside it prefer the horse to anything else; 'follow' "
                                    + "makes them trail it"),
                    Param.choice("group", MOB_GROUPS, "hostile", "which creatures it is about"),
                    Param.str("mob", "", "a single mob id instead of a group, or \"\" to use the group"),
                    Param.num("radius", 8, "reach in blocks, 1-32"),
                    Param.num("interval", 20, "ticks between beats (at least 1)"),
                    Param.num("max_targets", 12, "most entities one beat may reach, 1-64")),
            v -> {
                double radius = v.num("radius");
                if (radius < 1 || radius > 32) {
                    throw v.bad("radius must be 1-32 blocks, got " + radius);
                }
                int interval = v.intOf("interval");
                if (interval < 1) {
                    throw v.bad("interval must be at least 1 tick, got " + interval);
                }
                int maxTargets = v.intOf("max_targets");
                if (maxTargets < 1 || maxTargets > 64) {
                    throw v.bad("max_targets must be 1-64, got " + maxTargets);
                }
                return new GeneAbility.MobAura(v.str("mode"), v.str("group"), v.str("mob"),
                        radius, interval, maxTargets, v.when, v.minDose);
            }));

    /** Who a {@link #NIGHT_TEMPER} feels something about. */
    public static final List<String> NIGHT_TARGETS = List.of("players", "passive", "hostile", "all");


    /**
     * <b>How the horse feels about other creatures after dark.</b>
     *
     * <p>The night is in the verb rather than in a {@code when} condition. A
     * temperament that merely happened to be gated on darkness would need that
     * gate written onto every allele of every gene using it, and the whole
     * point of the locus this was built for is that the animal is one thing by
     * day and another after sunset - so the verb says so once.
     */
    public static final AbilityType NIGHT_TEMPER = register(new AbilityType("night_temper",
            List.of(
                    Param.requiredChoice("mood", List.of("aggressive", "flee"),
                            "whether the horse goes for them or runs from them"),
                    Param.requiredChoice("towards", NIGHT_TARGETS,
                            "who it feels that about - 'passive' is animals, 'hostile' is "
                                    + "monsters, 'all' is both plus players"),
                    Param.num("radius", 16, "how far it notices, in blocks, 1-48"),
                    Param.num("interval", 20, "ticks between scans (at least 1)"),
                    Param.num("max_targets", 8, "most entities one scan may consider, 1-64")),
            v -> {
                double radius = v.num("radius");
                if (radius < 1 || radius > 48) {
                    throw v.bad("radius must be 1-48 blocks, got " + radius);
                }
                int interval = v.intOf("interval");
                if (interval < 1) {
                    throw v.bad("interval must be at least 1 tick, got " + interval);
                }
                int maxTargets = v.intOf("max_targets");
                if (maxTargets < 1 || maxTargets > 64) {
                    throw v.bad("max_targets must be 1-64, got " + maxTargets);
                }
                return new GeneAbility.NightTemper(v.str("mood"), v.str("towards"), radius,
                        interval, maxTargets, v.when, v.minDose);
            }));

    /**
     * <b>What the horse does about the nearest player after dark.</b>
     *
     * <p>Five modes, and they are a deliberate progression from "does not care
     * where you are" to "is directly behind you": {@code stare} never moves and
     * does not need line of sight, {@code approach} closes to
     * {@code radius} first, {@code line_of_sight} watches only while it can
     * actually see you, {@code unseen} tries to stand where you cannot see
     * <i>it</i>, and {@code behind} closes to arm's length behind your back.
     */
    public static final AbilityType NIGHT_WATCH = register(new AbilityType("night_watch",
            List.of(
                    Param.requiredChoice("mode",
                            List.of("stare", "approach", "line_of_sight", "unseen", "behind"),
                            "what it does about the nearest player"),
                    Param.num("radius", 10,
                            "the distance the mode is measured against, in blocks, 1-48 - what "
                                    + "'approach' closes to, and how far the others look"),
                    Param.bool("silent_steps", true,
                            "suppress the horse's footfall sound while the mode is active")),
            v -> {
                double radius = v.num("radius");
                if (radius < 1 || radius > 48) {
                    throw v.bad("radius must be 1-48 blocks, got " + radius);
                }
                return new GeneAbility.NightWatch(v.str("mode"), radius, v.bool("silent_steps"),
                        v.when, v.minDose);
            }));

    /** The most damage one hit may deal. A guard against a typo, not a balance number. */
    public static final double MAX_COMBAT_DAMAGE = 200.0;

    /**
     * What the horse hits for, in health points - two per heart. A vanilla
     * horse has no attack at all, so this is the whole of the mod's combat
     * side. The number is <b>absolute</b> rather than a modifier, so that a
     * reader of a horse's sheet sees the damage it deals and not an adjustment
     * to a baseline they would have to go and look up.
     */
    public static final AbilityType COMBAT = register(new AbilityType("combat",
            List.of(Param.num("damage", 3, "health points per hit - two per heart")),
            v -> {
                double damage = v.num("damage");
                if (damage < 0 || damage > MAX_COMBAT_DAMAGE) {
                    throw v.bad("damage must be in [0, " + MAX_COMBAT_DAMAGE + "], got " + damage);
                }
                return new GeneAbility.Combat(damage, v.when, v.minDose);
            }));

    /**
     * <b>A sound the horse makes.</b> The verb four genes were waiting on -
     * singer, meowing, echolocate and base alarm.
     *
     * <p>{@code duration} is the stop, and it is why this is a verb rather than
     * a parameter on something else: a Minecraft sound plays from its beginning
     * or not at all, so "a section of a record" can only mean "the opening, then
     * stopped".
     */
    public static final AbilityType SOUND = register(new AbilityType("sound",
            List.of(
                    Param.required("sound", "sound id, e.g. 'minecraft:entity.cat.ambient'"),
                    Param.trigger("trigger", new Trigger.Interval(200), "when it fires"),
                    Param.num("volume", 1.0, "volume, in (0, 4]"),
                    Param.num("pitch", 1.0, "pitch, in [0.5, 2]"),
                    Param.num("duration", 0, "stop it after N ticks; 0 = let it run to its own end"),
                    Param.num("cooldown", 100,
                            "minimum ticks between firings. The hazard on every sound gene is spam, "
                                    + "not tick cost - a firing per beat per target is unbearable "
                                    + "within seconds")),
            v -> {
                double volume = v.num("volume");
                if (volume <= 0 || volume > 4) {
                    throw v.bad("volume must be in (0, 4], got " + volume);
                }
                double pitch = v.num("pitch");
                if (pitch < 0.5 || pitch > 2) {
                    throw v.bad("pitch must be in [0.5, 2], got " + pitch);
                }
                int duration = v.intOf("duration");
                if (duration < 0) {
                    throw v.bad("duration must not be negative, got " + duration);
                }
                int cooldown = v.intOf("cooldown");
                if (cooldown < 0) {
                    throw v.bad("cooldown must not be negative, got " + cooldown);
                }
                return new GeneAbility.Sound(v.str("sound"), v.trigger("trigger"), volume, pitch,
                        duration, cooldown, v.when, v.minDose);
            }));

    /** The most items one firing of a {@link #PRODUCE} may drop. A guard against a typo, not a balance number. */
    public static final int MAX_PRODUCE = 16;

    /**
     * <b>Something the horse produces on a clock.</b> The third leg of a pattern
     * that already had two - {@code item_drop} on death, {@code yield} on
     * interaction, this on an interval.
     *
     * <p>{@code nearby_cap} is the accumulation guard, and it is not optional in
     * spirit: a timer that drops an item and never looks is how a chunk fills
     * with entities.
     */
    public static final AbilityType PRODUCE = register(new AbilityType("produce",
            List.of(
                    Param.required("item", "item id to drop; resolved live, and an id this game "
                            + "has never heard of simply never lays"),
                    Param.num("interval", 6000, "ticks between firings (at least 20)"),
                    Param.num("min", 1, "fewest items one firing drops"),
                    Param.num("max", 1, "most items one firing drops"),
                    Param.num("nearby_cap", 8,
                            "skip the drop when this many of the same item are already lying "
                                    + "within a short radius. 0 disables the guard, which is "
                                    + "almost always wrong")),
            v -> {
                int interval = v.intOf("interval");
                if (interval < 20) {
                    throw v.bad("interval must be at least 20 ticks, got " + interval);
                }
                int min = v.intOf("min");
                int max = v.intOf("max");
                if (min < 1 || max < min || max > MAX_PRODUCE) {
                    throw v.bad("need 1 <= min <= max <= " + MAX_PRODUCE + ", got " + min + ".." + max);
                }
                int cap = v.intOf("nearby_cap");
                if (cap < 0) {
                    throw v.bad("nearby_cap must not be negative, got " + cap);
                }
                return new GeneAbility.Produce(v.str("item"), interval, min, max, cap,
                        v.when, v.minDose);
            }));

    /** The furthest one {@link #TELEPORT} may move a horse. Beyond this it stops being a dodge and becomes transport. */
    public static final double MAX_TELEPORT = 32.0;

    /**
     * <b>The horse blinks somewhere else.</b> Built for ender echo, and the one
     * verb here whose difficulty is entirely in the translator rather than in the
     * vocabulary - destination validation and the client-authoritative move.
     */
    public static final AbilityType TELEPORT = register(new AbilityType("teleport",
            List.of(
                    Param.num("distance", 8, "how far one blink may go, in blocks, (0, 32]"),
                    Param.bool("with_rider", true, "whether a rider comes along"),
                    Param.trigger("trigger", new Trigger.OnHurt(), "what fires it"),
                    Param.num("cooldown", 40, "minimum ticks between blinks")),
            v -> {
                double distance = v.num("distance");
                if (distance <= 0 || distance > MAX_TELEPORT) {
                    throw v.bad("distance must be in (0, " + MAX_TELEPORT + "], got " + distance);
                }
                int cooldown = v.intOf("cooldown");
                if (cooldown < 0) {
                    throw v.bad("cooldown must not be negative, got " + cooldown);
                }
                return new GeneAbility.Teleport(distance, v.bool("with_rider"),
                        v.trigger("trigger"), cooldown, v.when, v.minDose);
            }));

    /** The largest local population a {@link #SUMMON} may top up to. */
    public static final int MAX_SUMMON_POPULATION = 8;

    /**
     * <b>The horse makes creatures.</b> {@code up_to} rather than "how many" is
     * the whole design: it counts what is already there first, so a horse in a
     * stocked field does nothing at all and the ceiling lives in the definition
     * rather than in a cooldown.
     */
    public static final AbilityType SUMMON = register(new AbilityType("summon",
            List.of(
                    Param.required("mob", "mob id to spawn; resolved live against the registry"),
                    Param.num("radius", 32, "how far it looks and places, in blocks, 1-64"),
                    Param.num("up_to", 2, "top the local population up to this many, 1-8"),
                    Param.trigger("trigger", new Trigger.Interval(24000), "when it fires")),
            v -> {
                double radius = v.num("radius");
                if (radius < 1 || radius > 64) {
                    throw v.bad("radius must be 1-64 blocks, got " + radius);
                }
                int upTo = v.intOf("up_to");
                if (upTo < 1 || upTo > MAX_SUMMON_POPULATION) {
                    throw v.bad("up_to must be 1-" + MAX_SUMMON_POPULATION + ", got " + upTo);
                }
                return new GeneAbility.Summon(v.str("mob"), radius, upTo, v.trigger("trigger"),
                        v.when, v.minDose);
            }));

    /**
     * <b>How the horse feels about other creatures</b> - {@link #NIGHT_TEMPER}
     * with the night gate lifted into an ordinary {@code when}.
     *
     * <p>{@code hold} defaults true and is the safety rail: attack what comes
     * into reach and return, rather than pursue. Both genes using this verb
     * settled on holding, and a horse that chases is a horse that dies forty
     * blocks from its owner.
     */
    public static final AbilityType TEMPER = register(new AbilityType("temper",
            List.of(
                    Param.requiredChoice("mood", List.of("aggressive", "flee"),
                            "whether the horse goes for them or runs from them"),
                    Param.choice("towards", MOB_GROUPS, "hostile", "who it feels that about"),
                    Param.num("radius", 16, "how far it notices, in blocks, 1-48"),
                    Param.num("interval", 20, "ticks between scans (at least 1)"),
                    Param.num("max_targets", 8, "most entities one scan may consider, 1-64"),
                    Param.bool("hold", true,
                            "hold a radius and return, rather than pursuing. Turning this off is a "
                                    + "gameplay decision, not a tuning one"),
                    Param.trigger("trigger", new Trigger.Continuous(), "what fires it")),
            v -> {
                double radius = v.num("radius");
                if (radius < 1 || radius > 48) {
                    throw v.bad("radius must be 1-48 blocks, got " + radius);
                }
                int interval = v.intOf("interval");
                if (interval < 1) {
                    throw v.bad("interval must be at least 1 tick, got " + interval);
                }
                int maxTargets = v.intOf("max_targets");
                if (maxTargets < 1 || maxTargets > 64) {
                    throw v.bad("max_targets must be 1-64, got " + maxTargets);
                }
                return new GeneAbility.Temper(v.str("mood"), v.str("towards"), radius, interval,
                        maxTargets, v.bool("hold"), v.trigger("trigger"), v.when, v.minDose);
            }));

    /**
     * <b>Bond earned by something other than the player's attention.</b> The
     * translator must put it through the same daily cap as every other bond
     * source - a source that ignores the cap is an AFK exploit, not a feature.
     */
    public static final AbilityType BOND = register(new AbilityType("bond",
            List.of(
                    Param.num("amount", 1, "bond points per beat, 1-10"),
                    Param.num("interval", 200, "ticks between beats (at least 20)")),
            v -> {
                int amount = v.intOf("amount");
                if (amount < 1 || amount > 10) {
                    throw v.bad("amount must be 1-10, got " + amount);
                }
                int interval = v.intOf("interval");
                if (interval < 20) {
                    throw v.bad("interval must be at least 20 ticks, got " + interval);
                }
                return new GeneAbility.Bond(amount, interval, v.when, v.minDose);
            }));

    /** The furthest a {@link #WARD} may suppress spawning. Above this it stops being a camp and becomes a world setting. */
    public static final double MAX_WARD_RADIUS = 16.0;

    /**
     * <b>Nothing hostile spawns near the horse.</b> Deliberately not a
     * {@link #MOB_AURA} mode: every other radius effect runs on the horse's tick
     * and looks outward, and this one runs on somebody else's event and looks
     * inward.
     */
    public static final AbilityType WARD = register(new AbilityType("ward",
            List.of(Param.num("radius", 8,
                    "how far spawning is suppressed, in blocks, 1-16. The ceiling is a balance "
                            + "decision: uncapped, a well-bred horse is a permanent peaceful-mode "
                            + "bubble over a whole base and every other defensive gene stops "
                            + "mattering")),
            v -> {
                double radius = v.num("radius");
                if (radius < 1 || radius > MAX_WARD_RADIUS) {
                    throw v.bad("radius must be 1-" + MAX_WARD_RADIUS + " blocks, got " + radius);
                }
                return new GeneAbility.Ward(radius, v.when, v.minDose);
            }));
}
