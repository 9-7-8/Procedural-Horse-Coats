package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.AlleleSpec;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Combine;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Knob;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Layer;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Mask;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.MaskType;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Op;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.OpType;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Params;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads a gene JSON file into a {@link GeneSpec}, or throws with a message that
 * says where the file is wrong.
 *
 * <p>Validation is deliberately strict - unknown keys are errors, not warnings.
 * The gene creator writes these files, so an unknown key almost always means the
 * tool and the game are on different versions, and that is exactly the failure
 * you want loud (see {@code CLAUDE.md}, "the wiki is now load-bearing, so it can
 * rot").
 *
 * <h2>Knobs</h2>
 * A numeric parameter can be written three ways:
 * <ul>
 *   <li>{@code 0.4} - a constant.</li>
 *   <li>{@code "$extent"} - the value of a knob declared in {@code knobs}.</li>
 *   <li>{@code {"min": 0.1, "max": 0.9}} - an <b>inline</b> range, which the
 *       parser turns into an anonymous knob appended to the list. Handy in the
 *       tool, identical in effect.</li>
 *   <li>{@code {"perDose": [0, 0.4, 0.9]}} - one value per number of variant
 *       copies, which is how an incomplete dominant makes its homozygote
 *       louder.</li>
 * </ul>
 */
public final class GeneSpecParser {

    private GeneSpecParser() {}

    public static GeneSpec parse(String json) {
        return parse(json, "<string>");
    }

    /** {@code source} only ever appears in error messages. */
    public static GeneSpec parse(String json, String source) {
        try {
            return read(asObject(Json.parse(json), "the file"));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("gene spec " + source + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------

    private static GeneSpec read(Map<String, Object> root) {
        expectKeys(root, "the file", "format", "key", "name", "phase", "dominance",
                "wildOdds", "priority", "alleles", "knobs", "layers", "effects");

        int format = (int) number(root, "format", GeneSpec.FORMAT);
        if (format != GeneSpec.FORMAT) {
            throw new IllegalArgumentException("format " + format + " but this build reads format "
                    + GeneSpec.FORMAT);
        }

        String key = string(root, "key", null);
        if (!key.matches("[a-z0-9_]+\\.[a-z0-9_]+")) {
            throw new IllegalArgumentException("key must be '<modid>.<gene>', lower case, got '" + key + "'");
        }
        String name = string(root, "name", key.substring(key.indexOf('.') + 1));

        String phase = string(root, "phase", "natural").toLowerCase(Locale.ROOT);
        boolean natural = switch (phase) {
            case "natural" -> true;
            case "magical" -> false;
            default -> throw new IllegalArgumentException("phase must be 'natural' or 'magical', got '" + phase + "'");
        };

        DominancePattern dominance = enumValue(DominancePattern.class,
                string(root, "dominance", "DOMINANT"), "dominance");

        int wildOdds = (int) number(root, "wildOdds", 50);
        if (wildOdds < 1) {
            throw new IllegalArgumentException("wildOdds is '1 in N per allele' and must be at least 1, got " + wildOdds);
        }
        int priority = (int) number(root, "priority", 1000);

        // Knobs are collected mutably: inline ranges found while reading layers
        // are appended here, so a "$name" reference and an inline {min,max} end
        // up as the same thing by the time anything paints.
        List<Knob> knobs = new ArrayList<>();
        Map<String, Integer> knobIndex = new LinkedHashMap<>();
        for (Object o : array(root, "knobs")) {
            Knob knob = readKnob(asObject(o, "a knob"));
            if (knobIndex.putIfAbsent(knob.name(), knobs.size()) != null) {
                throw new IllegalArgumentException("two knobs named '" + knob.name() + "'");
            }
            knobs.add(knob);
        }

        List<Object> layerJson = array(root, "layers");
        List<Layer> layers = new ArrayList<>();
        for (int i = 0; i < layerJson.size(); i++) {
            layers.add(readLayer(asObject(layerJson.get(i), "layer " + (i + 1)),
                    "layer " + (i + 1), natural, knobs, knobIndex));
        }

        List<GeneAbility> abilities = readAbilities(root);

        List<AlleleSpec> alleles = readAlleles(root, !knobs.isEmpty());
        return new GeneSpec(key, name, natural, dominance, wildOdds, priority,
                alleles, List.copyOf(knobs), List.copyOf(layers), abilities);
    }

    // ------------------------------------------------------------------
    // effects - the Minecraft-specific abilities a gene grants
    // ------------------------------------------------------------------

    private static List<GeneAbility> readAbilities(Map<String, Object> root) {
        List<Object> raw = array(root, "effects");
        List<GeneAbility> out = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            out.add(readAbility(asObject(raw.get(i), "effect " + (i + 1)), "effect " + (i + 1)));
        }
        return List.copyOf(out);
    }

    /**
     * Reads any effect off the {@link AbilityType} table - no per-verb code. The
     * type declares its parameters (name, kind, default) and how to build its
     * record; this walks that list, type-checks each value, then hands the bag
     * to the builder. See {@link AbilityType} for the "adding an effect" contract.
     */
    private static GeneAbility readAbility(Map<String, Object> o, String where) {
        AbilityType type;
        try {
            type = AbilityType.byName(string(o, "type", null));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(where + ": " + e.getMessage());
        }
        expectKeys(o, where, type.allowedKeys().toArray(new String[0]));

        AbilityType.Values values = new AbilityType.Values();
        values.where = where;
        for (AbilityType.Param p : type.params()) {
            values.raw.put(p.name(), readAbilityParam(o, p, where + " '" + p.name() + "'"));
        }
        values.when = readCondition(o.get("when"), where + " when");
        values.minDose = readMinDose(o, where);
        return type.build(values);
    }

    /** One effect parameter, per its {@link AbilityType.Kind}. Missing + no fallback = an error for STRING / CHOICE. */
    private static Object readAbilityParam(Map<String, Object> o, AbilityType.Param p, String at) {
        boolean present = o.containsKey(p.name());
        return switch (p.kind()) {
            case STRING -> {
                if (!present) {
                    if (p.fallback() == null) {
                        throw new IllegalArgumentException(at + " is required");
                    }
                    yield p.fallback();
                }
                yield asString(o.get(p.name()), at);
            }
            case CHOICE -> {
                String s = present ? asString(o.get(p.name()), at) : (String) p.fallback();
                if (s == null) {
                    throw new IllegalArgumentException(at + " is required");
                }
                yield AbilityType.requireOneOf(p.choices(), s, at);
            }
            case NUMBER -> present ? asNumber(o.get(p.name()), at) : (Double) p.fallback();
            case BOOL -> present ? asBoolean(o.get(p.name()), at) : (Boolean) p.fallback();
            case COLOR -> readColor(present ? o.get(p.name()) : p.fallback(), at);
            case TRIGGER -> readTrigger(o.get(p.name()), at, (GeneAbility.Trigger) p.fallback());
            case PARTS -> present ? PartGroups.expand(strings(o.get(p.name()), at)) : p.fallback();
        };
    }

    private static int readMinDose(Map<String, Object> o, String where) {
        int d = (int) number(o, "minDose", 1);
        if (d != 1 && d != 2) {
            throw new IllegalArgumentException(where + " minDose: must be 1 (any expressing copy) or 2 "
                    + "(homozygous variant), got " + d);
        }
        return d;
    }

    private static GeneAbility.Trigger readTrigger(Object raw, String where, GeneAbility.Trigger fallback) {
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof String s) {
            return switch (s.toLowerCase(Locale.ROOT)) {
                case "continuous" -> new GeneAbility.Trigger.Continuous();
                case "on_move" -> new GeneAbility.Trigger.OnMove();
                default -> throw new IllegalArgumentException(where + ": bare trigger must be "
                        + "'continuous' or 'on_move'; use an object for 'interval' / 'on_interact'");
            };
        }
        Map<String, Object> o = asObject(raw, where);
        if (o.size() != 1) {
            throw new IllegalArgumentException(where + ": name exactly one of "
                    + "[continuous, on_move, interval, on_interact]");
        }
        String kind = o.keySet().iterator().next();
        Object v = o.get(kind);
        return switch (kind) {
            case "continuous" -> new GeneAbility.Trigger.Continuous();
            case "on_move" -> new GeneAbility.Trigger.OnMove();
            case "interval" -> {
                int ticks = (int) asNumber(v, where + " interval");
                if (ticks < 1) {
                    throw new IllegalArgumentException(where + " interval: at least 1 tick, got " + ticks);
                }
                yield new GeneAbility.Trigger.Interval(ticks);
            }
            case "on_interact" -> new GeneAbility.Trigger.OnInteract(v == null ? "" : asString(v, where + " on_interact"));
            default -> throw new IllegalArgumentException(where + ": unknown trigger '" + kind
                    + "'; allowed are [continuous, on_move, interval, on_interact]");
        };
    }

    private static GeneAbility.Condition readCondition(Object raw, String where) {
        if (raw == null) {
            return GeneAbility.Condition.ALWAYS;
        }
        Map<String, Object> o = asObject(raw, where);
        if (o.isEmpty()) {
            return GeneAbility.Condition.ALWAYS;
        }
        if (o.containsKey("flag")) {
            expectKeys(o, where, "flag", "negate");
            String flag = AbilityType.requireOneOf(AbilityType.CONDITION_FLAGS,
                    asString(o.get("flag"), where + " flag"), where + " flag");
            return new GeneAbility.Condition.Flag(flag, flag(o, "negate", false));
        }
        if (o.containsKey("not")) {
            expectKeys(o, where, "not");
            return new GeneAbility.Condition.Not(readCondition(o.get("not"), where + " not"));
        }
        if (o.containsKey("all") || o.containsKey("any")) {
            String key = o.containsKey("all") ? "all" : "any";
            expectKeys(o, where, key);
            List<GeneAbility.Condition> terms = new ArrayList<>();
            List<Object> arr = asArray(o.get(key), where + " " + key);
            for (int i = 0; i < arr.size(); i++) {
                terms.add(readCondition(arr.get(i), where + " " + key + "[" + i + "]"));
            }
            return key.equals("all")
                    ? new GeneAbility.Condition.All(List.copyOf(terms))
                    : new GeneAbility.Condition.Any(List.copyOf(terms));
        }
        throw new IllegalArgumentException(where + ": a condition is {\"flag\":..}, {\"not\":..}, "
                + "{\"all\":[..]} or {\"any\":[..]}");
    }

    private static List<AlleleSpec> readAlleles(Map<String, Object> root, boolean anyKnobs) {
        List<Object> raw = array(root, "alleles");
        if (raw.size() < 2) {
            throw new IllegalArgumentException("a gene needs at least two alleles, most dominant first, "
                    + "with the wild type last");
        }
        List<AlleleSpec> out = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Map<String, Object> a = asObject(raw.get(i), "allele " + (i + 1));
            expectKeys(a, "allele " + (i + 1), "token", "label", "visible", "deterministic");
            String token = string(a, "token", null);
            if (token.isBlank() || token.contains("/") || token.contains("-")) {
                throw new IllegalArgumentException("allele token '" + token + "' must be non-empty and free of "
                        + "'/' and '-' (they separate alleles and genes in a genotype code)");
            }
            boolean wild = i == raw.size() - 1;
            out.add(new AlleleSpec(
                    token,
                    string(a, "label", (wild ? "Wild-type (" : "") + token + (wild ? ")" : "")),
                    flag(a, "visible", !wild),
                    flag(a, "deterministic", wild || !anyKnobs)));
        }
        for (int i = 0; i < out.size(); i++) {
            for (int j = i + 1; j < out.size(); j++) {
                if (out.get(i).token().equals(out.get(j).token())) {
                    throw new IllegalArgumentException("two alleles share the token '" + out.get(i).token() + "'");
                }
            }
        }
        return List.copyOf(out);
    }

    private static Knob readKnob(Map<String, Object> o) {
        expectKeys(o, "a knob", "name", "type", "min", "max", "per", "spread");
        String name = string(o, "name", null);
        String type = string(o, "type", "range").toLowerCase(Locale.ROOT);
        if (type.equals("seed")) {
            return Knob.seed(name);
        }
        if (!type.equals("range")) {
            throw new IllegalArgumentException("knob '" + name + "': type must be 'range' or 'seed'");
        }
        double min = number(o, "min", 0);
        double max = number(o, "max", 1);
        String per = string(o, "per", "horse").toLowerCase(Locale.ROOT);
        boolean perLeg = switch (per) {
            case "horse" -> false;
            case "leg" -> true;
            default -> throw new IllegalArgumentException("knob '" + name + "': per must be 'horse' or 'leg'");
        };
        return new Knob(name, min, max, perLeg, number(o, "spread", 0), false);
    }

    private static Layer readLayer(Map<String, Object> o, String where, boolean natural,
                                   List<Knob> knobs, Map<String, Integer> knobIndex) {
        expectKeys(o, where, "name", "masks", "op");
        String name = string(o, "name", where);

        List<Mask> masks = new ArrayList<>();
        for (Object m : array(o, "masks")) {
            masks.add(readMask(asObject(m, where + " mask"), where + " mask", knobs, knobIndex));
        }
        if (masks.isEmpty()) {
            masks.add(new Mask(MaskType.ALL, Params.EMPTY, Combine.MULTIPLY, false));
        }

        Map<String, Object> opJson = asObject(o.get("op"), where + " op");
        OpType opType = enumValue(OpType.class, string(opJson, "type", null), where + " op type");
        if (opType.isNatural() != natural) {
            throw new IllegalArgumentException(where + ": op '" + opType + "' is a "
                    + (opType.isNatural() ? "natural" : "magical") + " move but the gene declares phase '"
                    + (natural ? "natural" : "magical") + "'. A gene is one or the other, never both - "
                    + "a gene that wants both registers as two genes.");
        }
        Params params = readParams(opJson, SpecSchema.opParams(opType), SpecSchema.opParamNames(opType),
                where + " op '" + opType + "'", knobs, knobIndex, "type");
        return new Layer(name, List.copyOf(masks), new Op(opType, params));
    }

    private static Mask readMask(Map<String, Object> o, String where,
                                 List<Knob> knobs, Map<String, Integer> knobIndex) {
        MaskType type = enumValue(MaskType.class, string(o, "type", null), where + " type");
        Combine combine = enumValue(Combine.class, string(o, "combine", "MULTIPLY"), where + " combine");
        boolean invert = flag(o, "invert", false);
        Params params = readParams(o, SpecSchema.maskParams(type), SpecSchema.maskParamNames(type),
                where + " '" + type + "'", knobs, knobIndex, "type", "combine", "invert");
        return new Mask(type, params, combine, invert);
    }

    private static Params readParams(Map<String, Object> o, List<SpecSchema.Param> schema,
                                     List<String> names, String where,
                                     List<Knob> knobs, Map<String, Integer> knobIndex,
                                     String... alsoAllowed) {
        List<String> allowed = new ArrayList<>(names);
        allowed.addAll(List.of(alsoAllowed));
        expectKeys(o, where, allowed.toArray(new String[0]));

        Map<String, Object> out = new LinkedHashMap<>();
        for (SpecSchema.Param p : schema) {
            Object raw = o.get(p.name());
            if (raw == null) {
                continue;
            }
            out.put(p.name(), switch (p.kind()) {
                case VALUE -> readValue(raw, where + " '" + p.name() + "'", knobs, knobIndex);
                case PARTS -> PartGroups.expand(strings(raw, where + " '" + p.name() + "'"));
                case CHOICE -> {
                    String s = asString(raw, where + " '" + p.name() + "'");
                    if (p.choices().stream().noneMatch(c -> c.equalsIgnoreCase(s))) {
                        throw new IllegalArgumentException(where + " '" + p.name() + "': must be one of "
                                + p.choices() + ", got '" + s + "'");
                    }
                    yield s.toLowerCase(Locale.ROOT);
                }
                case FLAG -> asBoolean(raw, where + " '" + p.name() + "'");
                case COLOR -> readColor(raw, where + " '" + p.name() + "'");
            });
        }
        return new Params(Map.copyOf(out));
    }

    /**
     * A number, a {@code "$knob"} reference, an inline {@code {min,max}} range
     * (appended to {@code knobs} as an anonymous knob) or a
     * {@code {"perDose": [...]}} triple.
     */
    private static Value readValue(Object raw, String where, List<Knob> knobs, Map<String, Integer> knobIndex) {
        if (raw instanceof Double d) {
            return new Value.Const(d);
        }
        if (raw instanceof String s) {
            if (!s.startsWith("$")) {
                throw new IllegalArgumentException(where + ": a knob reference is written \"$name\", got \"" + s + "\"");
            }
            Integer idx = knobIndex.get(s.substring(1));
            if (idx == null) {
                throw new IllegalArgumentException(where + ": no knob named '" + s.substring(1)
                        + "'; declared knobs are " + knobIndex.keySet());
            }
            return new Value.FromKnob(idx);
        }
        if (raw instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> o = (Map<String, Object>) m;
            if (o.containsKey("perDose")) {
                expectKeys(o, where, "perDose");
                List<Object> a = asArray(o.get("perDose"), where + " perDose");
                if (a.size() != 3) {
                    throw new IllegalArgumentException(where + ": perDose needs exactly three values "
                            + "(0, 1 and 2 variant copies), got " + a.size());
                }
                return new Value.PerDose(asNumber(a.get(0), where), asNumber(a.get(1), where),
                        asNumber(a.get(2), where));
            }
            // An inline range: same thing as a declared knob, written where it is used.
            Knob knob = readKnob(withName(o, "inline#" + knobs.size()));
            knobs.add(knob);
            return new Value.FromKnob(knobs.size() - 1);
        }
        throw new IllegalArgumentException(where + ": expected a number, a \"$knob\", "
                + "{\"min\":..,\"max\":..} or {\"perDose\":[..]}");
    }

    private static Map<String, Object> withName(Map<String, Object> o, String name) {
        if (o.containsKey("name")) {
            return o;
        }
        Map<String, Object> copy = new LinkedHashMap<>(o);
        copy.put("name", name);
        return copy;
    }

    private static int readColor(Object raw, String where) {
        String s = asString(raw, where).trim();
        if (!s.matches("#?[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(where + ": colour must be \"#rrggbb\", got \"" + s + "\"");
        }
        return Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16);
    }

    // ------------------------------------------------------------------
    // Typed JSON access, all of it error-message plumbing
    // ------------------------------------------------------------------

    private static void expectKeys(Map<String, Object> o, String where, String... allowed) {
        List<String> ok = List.of(allowed);
        for (String k : o.keySet()) {
            if (!ok.contains(k)) {
                throw new IllegalArgumentException(where + ": unknown key '" + k + "'; allowed keys are " + ok);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object o, String where) {
        if (!(o instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(where + ": expected a JSON object");
        }
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asArray(Object o, String where) {
        if (!(o instanceof List<?>)) {
            throw new IllegalArgumentException(where + ": expected a JSON array");
        }
        return (List<Object>) o;
    }

    private static String asString(Object o, String where) {
        if (!(o instanceof String s)) {
            throw new IllegalArgumentException(where + ": expected a string");
        }
        return s;
    }

    private static double asNumber(Object o, String where) {
        if (!(o instanceof Double d)) {
            throw new IllegalArgumentException(where + ": expected a number");
        }
        return d;
    }

    private static boolean asBoolean(Object o, String where) {
        if (!(o instanceof Boolean b)) {
            throw new IllegalArgumentException(where + ": expected true or false");
        }
        return b;
    }

    private static List<Object> array(Map<String, Object> o, String key) {
        Object v = o.get(key);
        return v == null ? List.of() : asArray(v, "'" + key + "'");
    }

    private static List<String> strings(Object raw, String where) {
        List<String> out = new ArrayList<>();
        if (raw instanceof String s) {
            out.add(s);
            return out;
        }
        for (Object o : asArray(raw, where)) {
            out.add(asString(o, where));
        }
        return out;
    }

    private static String string(Map<String, Object> o, String key, String fallback) {
        Object v = o.get(key);
        if (v == null) {
            if (fallback == null) {
                throw new IllegalArgumentException("'" + key + "' is required");
            }
            return fallback;
        }
        return asString(v, "'" + key + "'");
    }

    private static double number(Map<String, Object> o, String key, double fallback) {
        Object v = o.get(key);
        return v == null ? fallback : asNumber(v, "'" + key + "'");
    }

    private static boolean flag(Map<String, Object> o, String key, boolean fallback) {
        Object v = o.get(key);
        return v == null ? fallback : asBoolean(v, "'" + key + "'");
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String where) {
        for (E e : type.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(raw)) {
                return e;
            }
        }
        throw new IllegalArgumentException(where + ": '" + raw + "' is not one of "
                + List.of(type.getEnumConstants()));
    }
}
