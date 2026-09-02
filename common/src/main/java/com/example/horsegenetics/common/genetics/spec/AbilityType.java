package com.example.horsegenetics.common.genetics.spec;

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
            "has_rider", "in_water", "submerged", "on_ground", "on_fire",
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
    public enum Kind { STRING, CHOICE, NUMBER, BOOL, COLOR, TRIGGER }

    /**
     * One parameter of an effect. {@code fallback} is what the parser uses when
     * the file omits the key - its runtime type follows {@code kind}: a
     * {@code String} for STRING / CHOICE / COLOR, a {@code Double} for NUMBER, a
     * {@code Boolean} for BOOL, a {@link Trigger} for TRIGGER. A {@code null}
     * fallback on a STRING / CHOICE means the key is <b>required</b>.
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
                            "knockback_resistance", "step_height", "safe_fall_distance", "scale", "swim_speed"),
                            "the attribute to modify"),
                    Param.choice("op", List.of("add", "multiply_base", "multiply_total"), "add",
                            "how 'amount' is applied - vanilla modifier operations"),
                    Param.num("amount", 0, "signed modifier amount")),
            v -> new GeneAbility.AttributeMod(
                    v.str("attribute"), v.str("op"), v.num("amount"), v.when, v.minDose)));

    /** Particle (or, one day, light) emitter fired by a {@link Trigger}. */
    public static final AbilityType EMITTER = register(new AbilityType("emitter",
            List.of(
                    Param.choice("kind", List.of("particle", "light"), "particle",
                            "'particle', or 'light' (not wired yet)"),
                    Param.choice("shape", List.of("point", "ring", "trail", "burst"), "point",
                            "emission shape"),
                    Param.choice("anchor", List.of("feet", "body", "head", "eyes"), "feet",
                            "where on the horse it is centred"),
                    Param.trigger("trigger", new Trigger.OnMove(), "when it fires (default on_move)"),
                    Param.str("particle", "minecraft:dust",
                            "particle id; 'minecraft:dust' is the one that takes 'color'"),
                    Param.color("color", "#ffffff", "0xRRGGBB, used by particle types that take a colour"),
                    Param.num("chance", 1.0, "per-fire probability, in (0, 1]")),
            v -> {
                double chance = v.num("chance");
                if (chance <= 0 || chance > 1) {
                    throw v.bad("chance must be in (0, 1], got " + chance);
                }
                return new GeneAbility.Emitter(v.str("kind"), v.str("shape"), v.str("anchor"),
                        v.trigger("trigger"), v.color("color"), v.str("particle"), chance, v.when, v.minDose);
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
                    Param.num("cooldown", 0, "per-horse cooldown, ticks")),
            v -> {
                if (!(v.trigger("trigger") instanceof Trigger.OnInteract onInteract)) {
                    throw v.bad("a yield fires on 'on_interact' only");
                }
                return new GeneAbility.Yield(onInteract, v.str("consumes"), v.str("produces"),
                        v.intOf("cooldown"), v.when, v.minDose);
            }));
}
