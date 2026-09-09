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
 *   <li>a section in {@code wiki/gene-effects.html}.</li>
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
            "day", "night", "raining", "thundering", "sky_visible");

    /**
     * Trigger names. {@code continuous} / {@code on_move} take no argument (and
     * may be written as a bare string); {@code interval} takes a tick count and
     * {@code on_interact} an item id, so those must be an object.
     */
    public static final List<String> TRIGGERS = List.of("continuous", "on_move", "interval", "on_interact");

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

    /** Every registered effect type, in declaration order - what {@code wiki/gene-effects.html} lists. */
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
            List.of(Param.requiredChoice("flag", List.of(
                            "walk_on_water", "walk_on_lava", "fire_immune", "fall_immune",
                            "underwater_breathing", "water_averse"),
                    "the movement / survival flag to grant")),
            v -> new GeneAbility.Traversal(v.str("flag"), v.when, v.minDose)));

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
                                    + "OTHER gene can grant extra uses of it. \"\" opts out")),
            v -> {
                if (!(v.trigger("trigger") instanceof Trigger.OnInteract onInteract)) {
                    throw v.bad("a yield fires on 'on_interact' only");
                }
                return new GeneAbility.Yield(onInteract, v.str("consumes"), v.str("produces"),
                        v.intOf("cooldown"), v.num("denied_damage"), v.str("denied_message"),
                        v.str("kind"), v.when, v.minDose);
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
                    Param.choice("target", List.of("players", "rider", "self", "animals"), "players",
                            "who the aura heals"),
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
                return new GeneAbility.Healing(v.str("target"), radius, v.num("amount"), interval,
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
                    Param.requiredChoice("cover", List.of("mycelium", "moss", "grass"),
                            "which ground cover spreads from the horse"),
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
                    Param.requiredChoice("mode", List.of("repel", "attract"),
                            "'repel' keeps mobs outside the radius; 'attract' makes hostiles "
                                    + "inside it prefer the horse to anything else"),
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
                return new GeneAbility.MobAura(v.str("mode"), radius, interval, maxTargets,
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
}
