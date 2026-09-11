package com.example.horsegenetics.common.breed.spec;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedLineage;
import com.example.horsegenetics.common.breed.BreedSource;
import com.example.horsegenetics.common.breed.Commonness;
import com.example.horsegenetics.common.breed.SpawnTime;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.spec.Json;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Reads a breed JSON file into a {@link Breed}.
 *
 * <h2>Two kinds of wrongness, deliberately treated differently</h2>
 * A gene file is parsed strictly - an unknown key is an error - because the
 * gene creator writes those and an unknown key means the tool and the game
 * disagree. A <b>breed</b> file is different, because a breed is mostly a list
 * of references to <i>other people's</i> genes:
 *
 * <ul>
 *   <li><b>The file is malformed</b> - not an object, no {@code id}, a weight
 *       that is not a number. That is thrown, and the loader reports the file
 *       and moves on to the next.</li>
 *   <li><b>The file names something this install has not got</b> - a gene from
 *       a pack the player does not have, an allele token that gene no longer
 *       declares, an epigenetic value that was renamed. That is <b>warned and
 *       skipped</b>, and the breed still loads without that locus. A breed
 *       written for a bigger modpack has to degrade to the horses this install
 *       can actually make, not vanish.</li>
 * </ul>
 *
 * <p>Warnings go to the {@link Consumer} the caller passes, so the game can put
 * them in the log with a file name attached and the browser tool can put them
 * beside the field that caused them.
 *
 * <h2>The format</h2>
 * Every field except {@code id} and {@code name} may be left out; what happens
 * when it is, is documented on {@code wiki/breed-format.html} and repeated in
 * the constants below. Nothing here has a hidden default: an absent field means
 * "the game's own default", which is not the same as a zero.
 */
public final class BreedSpecParser {

    /** Keys a breed file may carry. Anything else is a hard error. */
    private static final Set<String> KEYS = Set.of(
            "id", "name", "description", "kind", "commonness", "spawn_weight", "biomes",
            "spawn", "spawn_time", "price", "stats", "genes", "bands", "notes");

    private BreedSpecParser() {
    }

    public static Breed parse(String json, String source) {
        return parse(json, source, m -> {
        });
    }

    /**
     * @param source   only ever used in messages - a file name
     * @param warnings told about anything skipped rather than thrown
     */
    public static Breed parse(String json, String source, Consumer<String> warnings) {
        try {
            return read(asObject(Json.parse(json), "the file"), source, warnings);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("breed " + source + ": " + e.getMessage(), e);
        }
    }

    /**
     * Read a <b>bundle</b> - a JSON array of breed objects, the same objects a
     * file each would hold.
     *
     * <p>It exists for the browser. The tools compile {@code common/} to
     * WebAssembly, where {@code getResourceAsStream} is the weakest thing TeaVM
     * does, so the page fetches one bundled file and hands the text across
     * instead of the loader walking a classpath index. One breed that will not
     * parse costs that breed and no other: its message joins {@code warnings}
     * and the rest of the array still loads, which is the same promise
     * {@code BreedSpecLoader} makes about a folder.
     */
    public static List<Breed> parseAll(String json, String source, Consumer<String> warnings) {
        Object parsed = Json.parse(json);
        if (!(parsed instanceof List<?> array)) {
            throw new IllegalArgumentException("breed bundle " + source + ": expected an array of breeds");
        }
        List<Breed> out = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            try {
                out.add(read(asObject(array.get(i), source + "[" + i + "]"), source, warnings));
            } catch (RuntimeException e) {
                warnings.accept(source + "[" + i + "]: " + e.getMessage());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------

    private static Breed read(Map<String, Object> root, String source, Consumer<String> warn) {
        expectKeys(root, KEYS);

        String id = requireString(root, "id");
        if (!id.equals(id.toLowerCase(Locale.ROOT)) || id.isBlank()) {
            throw new IllegalArgumentException("\"id\" must be a lower-case token, got \"" + id + "\"");
        }
        // Two ids the label machinery owns. "splice" is the reserved component a
        // gene splice contributes to a lineage, and "feral_mixed" is the absence
        // of one; a breed claiming either would make its own horses unreadable.
        if (id.equals(BreedLineage.SPLICE) || id.equals(BreedLineage.FERAL_ID)) {
            throw new IllegalArgumentException("\"" + id + "\" is reserved by the breed label system");
        }
        Breed.Builder b = Breed.of(id, requireString(root, "name"));
        b.description(string(root, "description", ""));

        // --- kind: natural (default) or magical --------------------------
        String kind = string(root, "kind", "natural");
        if (kind.equals("magical")) {
            b.magical();
        } else if (!kind.equals("natural")) {
            throw new IllegalArgumentException("\"kind\" must be \"natural\" or \"magical\", got \"" + kind + "\"");
        }

        // --- how often it heads a herd -----------------------------------
        if (root.containsKey("commonness") && root.containsKey("spawn_weight")) {
            throw new IllegalArgumentException("name either \"commonness\" or \"spawn_weight\", not both");
        }
        if (root.containsKey("commonness")) {
            b.commonness(commonness(asString(root.get("commonness"), "commonness")));
        } else if (root.containsKey("spawn_weight")) {
            b.spawnWeight(asNumber(root.get("spawn_weight"), "spawn_weight"));
        }

        for (String biome : strings(root, "biomes")) {
            b.biomes(biome);
        }

        // --- the source checklist ----------------------------------------
        if (root.containsKey("spawn")) {
            List<BreedSource> named = new ArrayList<>();
            for (String token : strings(root, "spawn")) {
                BreedSource s = BreedSource.byId(token);
                if (s == null) {
                    throw new IllegalArgumentException("\"spawn\" names no such source: \"" + token
                            + "\" (wild, cowboy, spawn_egg, stable)");
                }
                named.add(s);
            }
            // One call even when the list is empty: "spawn": [] means this breed
            // comes from nowhere, which is a real answer and not an omission.
            b.sources(named.toArray(new BreedSource[0]));
        }

        if (root.containsKey("spawn_time")) {
            String token = asString(root.get("spawn_time"), "spawn_time");
            SpawnTime time = SpawnTime.byId(token);
            if (time == null) {
                throw new IllegalArgumentException("\"spawn_time\" is \"any\", \"day\" or \"night\", got \""
                        + token + "\"");
            }
            b.spawnTime(time);
        }

        if (root.containsKey("price")) {
            List<Object> p = asArray(root.get("price"), "price");
            if (p.size() != 2) {
                throw new IllegalArgumentException("\"price\" is [min, max] in emeralds");
            }
            b.price((int) asNumber(p.get(0), "price[0]"), (int) asNumber(p.get(1), "price[1]"));
        }

        for (String note : strings(root, "notes")) {
            b.note(note);
        }

        readStats(root, b);
        readGenes(root, b, source, warn);
        readBands(root, b, source, warn);
        return b.build();
    }

    /**
     * {@code "stats": {"speed": 9, "jump": [4, 6], "health": 8, "size": [0.9, 1.1]}}
     *
     * <p>Each of the three axes is a 1-10 score, written as a number or as a
     * {@code [lo, hi]} range; {@code size} is a multiple of the baseline horse,
     * written the same way. An absent axis is left wild, which is not the same
     * as a score of 5 - see {@code BreedStatCurve}'s near-baseline rule.
     */
    private static void readStats(Map<String, Object> root, Breed.Builder b) {
        if (!root.containsKey("stats")) {
            return;
        }
        Map<String, Object> stats = asObject(root.get("stats"), "stats");
        expectKeys(stats, Set.of("speed", "jump", "health", "size"));
        if (stats.containsKey("speed")) {
            double[] r = range(stats.get("speed"), "stats.speed");
            b.speed(r[0], r[1]);
        }
        if (stats.containsKey("jump")) {
            b.jump(range(stats.get("jump"), "stats.jump")[0]);
        }
        if (stats.containsKey("health")) {
            b.health(range(stats.get("health"), "stats.health")[0]);
        }
        if (stats.containsKey("size")) {
            double[] r = range(stats.get("size"), "stats.size");
            if (r[0] <= 0.0 || r[1] <= 0.0) {
                throw new IllegalArgumentException("\"stats.size\" is a multiple of the baseline horse and must be"
                        + " above zero");
            }
            b.size(r[0], r[1]);
        }
    }

    /**
     * {@code "genes": {"<gene key>": [{"pair": "E/e", "weight": 44}, ...]}}
     *
     * <p>A gene this install has not registered costs the breed that locus and
     * nothing else; so does an allele token the gene does not declare. Both are
     * warned about by key, because "your Friesian file wants a gene you have not
     * installed" is the single most likely thing to go wrong with a shared breed
     * and the player needs to be able to read which one.
     */
    private static void readGenes(Map<String, Object> root, Breed.Builder b,
                                  String source, Consumer<String> warn) {
        if (!root.containsKey("genes")) {
            return;
        }
        Map<String, Object> genes = asObject(root.get("genes"), "genes");
        for (Map.Entry<String, Object> e : genes.entrySet()) {
            String key = e.getKey();
            Gene gene = Genes.byKeyOrNull(key);
            if (gene == null) {
                warn.accept(source + ": no gene \"" + key + "\" is installed - the breed will roll that locus wild");
                continue;
            }
            List<Object> combos = asArray(e.getValue(), "genes." + key);
            if (combos.isEmpty()) {
                warn.accept(source + ": \"genes." + key + "\" is empty - the breed will roll that locus wild");
                continue;
            }
            List<Breed.Combo> parsed = new ArrayList<>();
            for (int i = 0; i < combos.size(); i++) {
                String at = "genes." + key + "[" + i + "]";
                Map<String, Object> combo = asObject(combos.get(i), at);
                expectKeys(combo, Set.of("pair", "weight"));
                String[] tokens = splitPair(requireString(combo, "pair"), at);
                if (!hasAllele(gene, tokens[0]) || !hasAllele(gene, tokens[1])) {
                    warn.accept(source + ": " + at + " names allele(s) \"" + tokens[0] + "/" + tokens[1]
                            + "\" that " + key + " does not declare - that combination is dropped");
                    continue;
                }
                double weight = combo.containsKey("weight")
                        ? asNumber(combo.get("weight"), at + ".weight") : 1.0;
                if (weight <= 0.0) {
                    warn.accept(source + ": " + at + " has a weight of " + weight + " - dropped");
                    continue;
                }
                parsed.add(new Breed.Combo(tokens[0], tokens[1], weight));
            }
            if (parsed.isEmpty()) {
                warn.accept(source + ": nothing in \"genes." + key
                        + "\" survived - the breed will roll that locus wild");
                continue;
            }
            for (Breed.Combo c : parsed) {
                b.gene(key, c.a(), c.b(), c.weight());
            }
        }
    }

    /**
     * {@code "bands": {"<gene key>": {"<value name>": [lo, hi]}}}
     *
     * <p>A scalar or a category takes {@code [lo, hi]}, or a single number, which
     * locks it. A seed takes one value only - a number, or a string of digits
     * when it is too big for a JSON number to hold exactly, which most are.
     *
     * <p>The four body-stat genes are refused here rather than skipped
     * silently, because a band on one of them is not a missing feature - it is
     * the author fighting their own {@code stats} block, and they need to be
     * told which one wins.
     */
    private static void readBands(Map<String, Object> root, Breed.Builder b,
                                  String source, Consumer<String> warn) {
        if (!root.containsKey("bands")) {
            return;
        }
        Map<String, Object> bands = asObject(root.get("bands"), "bands");
        for (Map.Entry<String, Object> e : bands.entrySet()) {
            String key = e.getKey();
            Gene gene = Genes.byKeyOrNull(key);
            if (gene == null) {
                warn.accept(source + ": no gene \"" + key + "\" is installed - its bands are ignored");
                continue;
            }
            if (BODY_STAT_KEYS.contains(key)) {
                warn.accept(source + ": \"bands." + key + "\" is ignored - the four body-stat genes"
                        + " are driven by the breed's \"stats\" block");
                continue;
            }
            EpiSchema schema = gene.epiSchema();
            Map<String, Object> values = asObject(e.getValue(), "bands." + key);
            for (Map.Entry<String, Object> v : values.entrySet()) {
                String at = "bands." + key + "." + v.getKey();
                int index = schema.indexOf(v.getKey());
                if (index < 0) {
                    warn.accept(source + ": " + key + " declares no epigenetic value \""
                            + v.getKey() + "\" - that band is ignored");
                    continue;
                }
                EpiValue.Kind kind = schema.get(index).kind();
                if (kind == EpiValue.Kind.SEED) {
                    b.seed(key, v.getKey(), seed(v.getValue(), at));
                    continue;
                }
                double[] r = range(v.getValue(), at);
                b.band(key, v.getKey(), r[0], r[1]);
            }
        }
    }

    /** Repeated from {@code BreedFounder}; the two agree by test, not by import. */
    private static final Set<String> BODY_STAT_KEYS = Set.of(
            "horsegenetics.body_size",
            "horsegenetics.magic_speed",
            "horsegenetics.magic_health",
            "horsegenetics.magic_jump");

    // ------------------------------------------------------------------
    // small readers
    // ------------------------------------------------------------------

    /** {@code "E/e"} into its two tokens. A slash is the separator because no allele token has one. */
    private static String[] splitPair(String pair, String where) {
        int slash = pair.indexOf('/');
        if (slash <= 0 || slash == pair.length() - 1) {
            throw new IllegalArgumentException(where + ".pair must be \"<allele>/<allele>\", got \"" + pair + "\"");
        }
        return new String[]{pair.substring(0, slash), pair.substring(slash + 1)};
    }

    private static boolean hasAllele(Gene gene, String token) {
        for (Allele a : gene.alleles()) {
            if (a.token().equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static Commonness commonness(String token) {
        String t = token.trim().toUpperCase(Locale.ROOT);
        for (Commonness c : Commonness.values()) {
            if (c.name().equals(t)) {
                return c;
            }
        }
        throw new IllegalArgumentException("\"commonness\" names no tier: \"" + token + "\"");
    }

    /** A number, or a two-element {@code [lo, hi]}. Returns {@code {lo, hi}} either way. */
    /**
     * A locked seed. A JSON number is a double and cannot carry most 64-bit
     * seeds exactly, so the writer spells one as a string of digits; a small
     * one typed by hand as a number is taken too.
     */
    private static long seed(Object raw, String where) {
        if (raw instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(where + " is a seed - a whole number, got \"" + s + "\"");
            }
        }
        double d = asNumber(raw, where);
        if (d != Math.rint(d)) {
            throw new IllegalArgumentException(where + " is a seed - a whole number, got " + d);
        }
        return (long) d;
    }

    private static double[] range(Object raw, String where) {
        if (raw instanceof List<?> list) {
            if (list.size() != 2) {
                throw new IllegalArgumentException(where + " must be a number or [lo, hi]");
            }
            return new double[]{asNumber(list.get(0), where + "[0]"), asNumber(list.get(1), where + "[1]")};
        }
        double one = asNumber(raw, where);
        return new double[]{one, one};
    }

    private static void expectKeys(Map<String, Object> o, Set<String> allowed) {
        for (String key : o.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("unknown key \"" + key + "\"");
            }
        }
    }

    private static List<String> strings(Map<String, Object> o, String key) {
        if (!o.containsKey(key)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<Object> raw = asArray(o.get(key), key);
        for (int i = 0; i < raw.size(); i++) {
            String s = asString(raw.get(i), key + "[" + i + "]");
            if (seen.add(s)) {
                out.add(s);
            }
        }
        return out;
    }

    private static String requireString(Map<String, Object> o, String key) {
        if (!o.containsKey(key)) {
            throw new IllegalArgumentException("\"" + key + "\" is required");
        }
        return asString(o.get(key), key);
    }

    private static String string(Map<String, Object> o, String key, String fallback) {
        return o.containsKey(key) ? asString(o.get(key), key) : fallback;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object o, String where) {
        if (o instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new IllegalArgumentException(where + " must be an object");
    }

    private static List<Object> asArray(Object o, String where) {
        if (o instanceof List<?> l) {
            return (List<Object>) l;
        }
        throw new IllegalArgumentException(where + " must be an array");
    }

    private static String asString(Object o, String where) {
        if (o instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException(where + " must be a string");
    }

    private static double asNumber(Object o, String where) {
        if (o instanceof Double d) {
            return d;
        }
        throw new IllegalArgumentException(where + " must be a number");
    }
}
