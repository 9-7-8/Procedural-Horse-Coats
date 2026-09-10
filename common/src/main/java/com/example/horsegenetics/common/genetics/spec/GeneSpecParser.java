package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.CommonMaps;
import com.example.horsegenetics.common.coat.pattern.SvgPath;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.AlleleSpec;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.Combine;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.ExpressionSpec;
import com.example.horsegenetics.common.genetics.spec.GeneSpec.FounderWeight;
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
import java.util.function.Consumer;

/**
 * Reads a gene JSON file into a {@link GeneSpec}, or throws with a message that
 * says where the file is wrong.
 *
 * <p>Validation is deliberately strict - unknown keys are errors, not warnings.
 * The gene creator writes these files, so an unknown key almost always means the
 * tool and the game are on different versions, and that is exactly the failure
 * you want loud (see {@code wiki/known-gaps.html}, "the wiki is now load-bearing,
 * so it can rot").
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

    /**
     * Parse a <b>bundle</b>: one JSON array holding what would otherwise be a
     * folder of gene files. It exists for the browser, which cannot walk a
     * classpath index - see {@code DesignerApi.registerGenes} - and it is
     * written by {@link GeneFileTool} out of the same files the game reads, so
     * the tool and the mod cannot describe different genes without somebody
     * skipping a re-bake.
     *
     * <p>A file that will not parse is reported through {@code problems} and
     * skipped, exactly as in {@link GeneSpecLoader}: one bad gene should not
     * cost the reader the other eighty.
     */
    public static List<GeneSpec> parseAll(String json, String source, Consumer<String> problems) {
        Object parsed = Json.parse(json);
        if (!(parsed instanceof List<?> entries)) {
            throw new IllegalArgumentException(source + ": expected an array of gene objects");
        }
        List<GeneSpec> out = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            String where = source + " [" + i + "]";
            try {
                out.add(read(asObject(entries.get(i), where)));
            } catch (RuntimeException e) {
                problems.accept("gene spec " + where + ": " + e.getMessage());
            }
        }
        return List.copyOf(out);
    }

    // ------------------------------------------------------------------

    private static GeneSpec read(Map<String, Object> root) {
        expectKeys(root, "the file", "format", "key", "name", "phase",
                "priority", "alleles", "knobs", "expressions", "founders",
                "blurb", "rarity", "carrot", "splice", "preview", "notes");

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

        List<AlleleSpec> alleles = readAlleles(root);
        List<String> combinations = allCombinations(alleles);
        List<ExpressionSpec> expressions =
                readExpressions(root, natural, alleles, combinations, knobs, knobIndex);
        List<FounderWeight> founders = readWeightTable(root, "founders", combinations, true);

        // --- gameplay-economy metadata (roadmap §19), all optional ---
        String blurb = string(root, "blurb", "");
        GeneRarity rarity = GeneRarity.fromString(string(root, "rarity", ""));
        GeneSpec.Carrot carrot = readCarrot(root);
        List<FounderWeight> splice = readWeightTable(root, "splice", combinations, false);
        GeneSpec.Preview preview = readPreview(root, expressions);
        List<String> notes = readNotes(root, "the file");

        return new GeneSpec(key, name, natural, priority,
                alleles, List.copyOf(knobs), expressions, founders,
                blurb, rarity, carrot, splice, preview, notes);
    }

    /**
     * The optional {@code notes} block - free prose, one entry per paragraph,
     * on the file or on a single expression. See {@link GeneSpec#notes()}.
     *
     * <p>A bare string is accepted as a one-paragraph list, because that is
     * what an author writes the first time and refusing it would teach nothing.
     * Blank entries are dropped rather than rendered as an empty paragraph.
     */
    private static List<String> readNotes(Map<String, Object> o, String where) {
        Object raw = o.get("notes");
        if (raw == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if (raw instanceof String one) {
            if (!one.isBlank()) {
                out.add(one.trim());
            }
            return List.copyOf(out);
        }
        for (Object p : array(o, "notes")) {
            if (!(p instanceof String text)) {
                throw new IllegalArgumentException(where
                        + ": every entry in \"notes\" must be a paragraph of text");
            }
            if (!text.isBlank()) {
                out.add(text.trim());
            }
        }
        return List.copyOf(out);
    }

    /**
     * The optional {@code preview} block - defaults to "measure it", which is
     * what all but a handful of genes want. See {@link GeneSpec.Preview}.
     *
     * <p>{@code expression} is checked here, because the ids it may name are
     * right beside it in the same file. {@code base} deliberately is
     * <b>not</b>: resolving a {@code BaseCoats} key builds genotypes out of
     * {@code Genes}, and {@code Genes} is what is loading this file. It is
     * checked where it is used instead.
     */
    private static GeneSpec.Preview readPreview(Map<String, Object> root,
                                                List<ExpressionSpec> expressions) {
        Object raw = root.get("preview");
        if (raw == null) {
            return GeneSpec.Preview.AUTO;
        }
        Map<String, Object> o = asObject(raw, "preview");
        expectKeys(o, "preview", "base", "expression");
        String base = string(o, "base", "");
        String expression = string(o, "expression", "");
        if (!expression.isEmpty()) {
            boolean known = false;
            for (ExpressionSpec e : expressions) {
                known |= e.id().equals(expression);
            }
            if (!known) {
                throw new IllegalArgumentException("preview.expression '" + expression
                        + "' is not an expression of this gene");
            }
        }
        return new GeneSpec.Preview(base.isEmpty() ? null : base,
                expression.isEmpty() ? null : expression);
    }

    /** The optional {@code carrot} block - defaults to "enabled, heterozygous, no flavour". */
    private static GeneSpec.Carrot readCarrot(Map<String, Object> root) {
        Object raw = root.get("carrot");
        if (raw == null) {
            return GeneSpec.Carrot.DEFAULT;
        }
        Map<String, Object> o = asObject(raw, "carrot");
        expectKeys(o, "carrot", "enabled", "behaviour", "flavour");
        boolean enabled = flag(o, "enabled", true);
        String behaviour = string(o, "behaviour", "heterozygous").toLowerCase(Locale.ROOT);
        boolean homozygous = switch (behaviour) {
            case "heterozygous" -> false;
            case "homozygous" -> true;
            default -> throw new IllegalArgumentException(
                    "carrot.behaviour must be 'heterozygous' or 'homozygous', got '" + behaviour + "'");
        };
        List<String> flavour = new ArrayList<>();
        for (Object f : array(o, "flavour")) {
            flavour.add(String.valueOf(f));
        }
        return new GeneSpec.Carrot(enabled, homozygous, List.copyOf(flavour));
    }

    // ------------------------------------------------------------------
    // expressions - one entry per distinct outcome
    // ------------------------------------------------------------------

    /**
     * Every unordered combination of {@code alleles}, as canonical
     * {@code "<a>/<b>"} tokens with the earlier-declared allele first - the
     * same order {@code AllelePair} canonicalizes to, so a combination written
     * either way round in the file resolves to one entry here.
     */
    private static List<String> allCombinations(List<AlleleSpec> alleles) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < alleles.size(); i++) {
            for (int j = i; j < alleles.size(); j++) {
                out.add(alleles.get(i).token() + "/" + alleles.get(j).token());
            }
        }
        return List.copyOf(out);
    }

    /**
     * Reads the {@code expressions} table and checks it is <b>total and
     * unambiguous</b>: every one of the gene's combinations is claimed by
     * exactly one expression, counting at most one catch-all (an entry with no
     * {@code when}). A gap or an overlap is a load error - the whole value of
     * declaring the table is that it cannot quietly be wrong.
     *
     * <p>An entry carrying {@code needs} is exempt from the count, because it
     * does not <i>claim</i> a combination - it <b>overrides</b> one, and only
     * when a second locus agrees. Several may name the same combination; the
     * first one in file order whose condition holds is the outcome, and the
     * ordinary entry underneath is what a horse gets when none of them does. So
     * the table stays total whatever the other locus says, which is the
     * property worth keeping.
     */
    private static List<ExpressionSpec> readExpressions(Map<String, Object> root, boolean natural,
                                                        List<AlleleSpec> alleles, List<String> combinations,
                                                        List<Knob> knobs, Map<String, Integer> knobIndex) {
        List<Object> raw = array(root, "expressions");
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("a gene needs at least one expression - "
                    + "one entry per distinct thing a combination of its alleles does");
        }

        Map<String, String> claimedBy = new LinkedHashMap<>();
        List<ExpressionSpec> out = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        int catchAllAt = -1;

        for (int i = 0; i < raw.size(); i++) {
            String where = "expression " + (i + 1);
            Map<String, Object> o = asObject(raw.get(i), where);
            expectKeys(o, where, "id", "name", "description", "wildType", "masks", "varies",
                    "when", "needs", "layers", "effects", "notes");

            String id = string(o, "id", null);
            if (ids.contains(id)) {
                throw new IllegalArgumentException("two expressions share the id '" + id + "'");
            }
            ids.add(id);
            where = "expression '" + id + "'";

            boolean wildType = flag(o, "wildType", false);
            boolean masks = flag(o, "masks", false);

            List<Layer> layers = new ArrayList<>();
            List<Object> layerJson = array(o, "layers");
            for (int k = 0; k < layerJson.size(); k++) {
                String lw = where + " layer " + (k + 1);
                layers.add(readLayer(asObject(layerJson.get(k), lw), lw, natural, knobs, knobIndex));
            }
            List<GeneAbility> abilities = readAbilities(o);

            if (wildType && (!layers.isEmpty() || !abilities.isEmpty())) {
                throw new IllegalArgumentException(where + ": a wildType expression is the outcome for a"
                        + " combination that changes nothing, so it cannot carry layers or effects");
            }
            if (!wildType && layers.isEmpty() && abilities.isEmpty()) {
                throw new IllegalArgumentException(where + ": has neither layers nor effects, so it does"
                        + " nothing - mark it \"wildType\": true if that is what you meant");
            }

            List<GeneSpec.LocusCondition> needs = readNeeds(o, where);
            List<String> claims = readCombinations(o, id, alleles, combinations);
            checkPerDoseVaries(layers, claims, alleles, where);
            if (!needs.isEmpty()) {
                if (claims.isEmpty()) {
                    throw new IllegalArgumentException(where + ": has \"needs\" but no \"when\"."
                            + " A conditional entry overrides named combinations; it cannot also be"
                            + " the catch-all");
                }
                boolean varies0 = flag(o, "varies", !wildType && !layers.isEmpty() && !knobs.isEmpty());
                out.add(new ExpressionSpec(id, string(o, "name", id), string(o, "description", ""),
                        wildType, masks, !varies0, claims, needs, List.copyOf(layers), abilities,
                        readNotes(o, where)));
                continue;
            }
            if (claims.isEmpty()) {
                if (catchAllAt >= 0) {
                    throw new IllegalArgumentException("expressions '" + ids.get(catchAllAt) + "' and '" + id
                            + "' both omit \"when\" - only one expression can be the catch-all");
                }
                catchAllAt = i;
            }
            for (String c : claims) {
                String prior = claimedBy.putIfAbsent(c, id);
                if (prior != null) {
                    throw new IllegalArgumentException("combination '" + c + "' is claimed by both '"
                            + prior + "' and '" + id + "'");
                }
            }

            boolean varies = flag(o, "varies", !wildType && !layers.isEmpty() && !knobs.isEmpty());
            out.add(new ExpressionSpec(id, string(o, "name", id), string(o, "description", ""),
                    wildType, masks, !varies, claims, List.of(), List.copyOf(layers), abilities,
                    readNotes(o, where)));
        }

        List<String> unclaimed = new ArrayList<>();
        for (String c : combinations) {
            if (!claimedBy.containsKey(c)) {
                unclaimed.add(c);
            }
        }
        if (catchAllAt < 0 && !unclaimed.isEmpty()) {
            throw new IllegalArgumentException("no expression covers " + unclaimed
                    + " - every combination of a gene's alleles has to produce something."
                    + " Add them to a \"when\", or give one expression no \"when\" at all to catch"
                    + " whatever is left");
        }
        if (catchAllAt >= 0 && unclaimed.isEmpty()) {
            throw new IllegalArgumentException("expression '" + ids.get(catchAllAt) + "' omits \"when\" but"
                    + " every combination is already claimed, so it can never happen");
        }
        if (catchAllAt >= 0) {
            ExpressionSpec fallback = out.get(catchAllAt);
            out.set(catchAllAt, new ExpressionSpec(fallback.id(), fallback.name(), fallback.description(),
                    fallback.wildType(), fallback.masks(), fallback.deterministic(),
                    List.copyOf(unclaimed), fallback.needs(), fallback.layers(), fallback.abilities(),
                    fallback.notes()));
        }
        return List.copyOf(out);
    }

    /**
     * An expression's {@code needs}: {@code {"<gene key>": {"<token>": copies}}}.
     *
     * <p>The other gene's tokens are <b>not</b> checked here, and cannot be:
     * gene files load in an order nobody controls, and the gene named may not
     * be registered yet - or at all, if it lives in another mod. A token that
     * never resolves simply never matches, which is the same outcome as the
     * gene being absent and is the behaviour a soft dependency wants.
     */
    private static List<GeneSpec.LocusCondition> readNeeds(Map<String, Object> o, String where) {
        Object raw = o.get("needs");
        if (raw == null) {
            return List.of();
        }
        Map<String, Object> byGene = asObject(raw, where + " needs");
        List<GeneSpec.LocusCondition> out = new ArrayList<>();
        for (Map.Entry<String, Object> entry : byGene.entrySet()) {
            String w = where + " needs '" + entry.getKey() + "'";
            Map<String, Object> counts = asObject(entry.getValue(), w);
            if (counts.isEmpty()) {
                throw new IllegalArgumentException(w + ": name at least one allele token and how many"
                        + " copies of it the horse has to carry");
            }
            Map<String, Integer> copies = new LinkedHashMap<>();
            for (Map.Entry<String, Object> c : counts.entrySet()) {
                int n = (int) number(counts, c.getKey(), 0);
                if (n < 1 || n > 2) {
                    throw new IllegalArgumentException(w + " '" + c.getKey() + "': a horse carries one or"
                            + " two copies of an allele, not " + n);
                }
                copies.put(c.getKey(), n);
            }
            out.add(new GeneSpec.LocusCondition(entry.getKey(), CommonMaps.copyOf(copies)));
        }
        return List.copyOf(out);
    }

    /**
     * An expression's {@code when}: a list of {@code "<a>/<b>"} combinations, or
     * an object of allele token to required copy count
     * ({@code {"Cr": 1, "prl": 1}}), which expands to every combination
     * matching those counts. Absent means "the catch-all".
     */
    /**
     * <b>Refuse a {@code perDose} triple on an expression that can only ever be
     * reached at one dose.</b>
     *
     * <p>{@code perDose} is indexed by how many copies of the gene's
     * <b>first-declared</b> allele the horse carries. An expression claimed only
     * by combinations that all share a dose therefore reads one fixed element of
     * the triple, always - it is a constant, written in the form that looks most
     * like a variable.
     *
     * <p>That is not a style complaint. A <i>coloured</i> expression is normally
     * reached by two copies of the <b>second</b> allele, so its dose is 0 and it
     * reads {@code perDose[0]} - which authors write as {@code 0.0}, because a
     * dose of nothing should draw nothing. A {@code chance} of 0 means the mask
     * never fires. <b>Five shipped genes painted nothing at all</b> that way
     * (crescents, lasertae, polymoon, stellar, wormholes) and three of them were
     * found by the owner opening the pages. See known-gaps gap 105.
     *
     * <p>The catch-all expression is skipped: its claims are whatever no other
     * entry took, which is not known until every entry has been read, and a
     * catch-all spanning a single dose is not a shape that occurs. Everything
     * with an explicit {@code when} - which is every expression that has ever
     * had this bug - is checked.
     */
    private static void checkPerDoseVaries(List<Layer> layers, List<String> claims,
                                           List<AlleleSpec> alleles, String where) {
        if (claims.isEmpty() || layers.isEmpty()) {
            return;
        }
        String variant = alleles.get(0).token();
        int dose = doseOf(claims.get(0), variant);
        for (String c : claims) {
            if (doseOf(c, variant) != dose) {
                return;     // genuinely varies - the triple is doing its job
            }
        }
        for (Layer layer : layers) {
            for (Mask mask : layer.masks()) {
                requireNoPerDose(mask.params(), dose, variant, where + " mask '" + mask.type() + "'");
            }
            requireNoPerDose(layer.op().params(), dose, variant,
                    where + " op '" + layer.op().type() + "'");
        }
    }

    /** How many copies of {@code token} the combination {@code "a/b"} carries. */
    private static int doseOf(String combination, String token) {
        String[] parts = combination.split("/");
        return (parts[0].equals(token) ? 1 : 0) + (parts[1].equals(token) ? 1 : 0);
    }

    private static void requireNoPerDose(Params params, int dose, String variant, String where) {
        for (Map.Entry<String, Object> e : params.raw().entrySet()) {
            if (!(e.getValue() instanceof Value.PerDose p)) {
                continue;
            }
            double reads = dose == 0 ? p.zero() : (dose == 1 ? p.one() : p.two());
            throw new IllegalArgumentException(where + " '" + e.getKey() + "': a perDose here is a "
                    + "constant. Every combination this expression claims carries " + dose
                    + " cop" + (dose == 1 ? "y" : "ies") + " of '" + variant + "' - the gene's "
                    + "first-declared allele, which is what perDose counts - so the triple always "
                    + "reads its element " + dose + ", which is " + reads + ". Write " + reads
                    + " instead."
                    + (reads == 0 ? " (And note that 0 there means this layer never paints at all,"
                            + " which is how five genes once shipped invisible.)" : ""));
        }
    }

    private static List<String> readCombinations(Map<String, Object> o, String id,
                                                 List<AlleleSpec> alleles, List<String> combinations) {
        Object when = o.get("when");
        if (when == null) {
            return List.of();
        }
        String where = "expression '" + id + "' when";
        List<String> out = new ArrayList<>();

        if (when instanceof List<?> list) {
            if (list.isEmpty()) {
                throw new IllegalArgumentException(where + ": an empty list claims nothing."
                        + " Omit \"when\" entirely to make this the catch-all");
            }
            for (Object item : list) {
                out.add(canonicalCombination(String.valueOf(item), where, alleles, combinations));
            }
            return List.copyOf(out);
        }

        Map<String, Object> counts = asObject(when, where);
        if (counts.isEmpty()) {
            throw new IllegalArgumentException(where + ": an empty count table claims nothing."
                    + " Omit \"when\" entirely to make this the catch-all");
        }
        for (Map.Entry<String, Object> e : counts.entrySet()) {
            if (!hasToken(alleles, e.getKey())) {
                throw new IllegalArgumentException(where + ": no allele '" + e.getKey() + "' on this gene");
            }
            if (!(e.getValue() instanceof Number n) || n.intValue() < 0 || n.intValue() > 2) {
                throw new IllegalArgumentException(where + " '" + e.getKey()
                        + "': a copy count is 0, 1 or 2, got " + e.getValue());
            }
        }
        for (String combination : combinations) {
            if (matchesCounts(combination, counts)) {
                out.add(combination);
            }
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException(where + ": no combination of this gene's alleles matches "
                    + counts + ", so the expression can never happen");
        }
        return List.copyOf(out);
    }

    private static boolean matchesCounts(String combination, Map<String, Object> counts) {
        String[] tokens = combination.split("/");
        for (Map.Entry<String, Object> e : counts.entrySet()) {
            int want = ((Number) e.getValue()).intValue();
            int have = (tokens[0].equals(e.getKey()) ? 1 : 0) + (tokens[1].equals(e.getKey()) ? 1 : 0);
            if (have != want) {
                return false;
            }
        }
        return true;
    }

    /** {@code "a/b"} in either order, resolved to the canonical form and checked against the gene. */
    private static String canonicalCombination(String text, String where,
                                               List<AlleleSpec> alleles, List<String> combinations) {
        String[] parts = text.split("/", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException(where + ": '" + text
                    + "' is not a combination - write two allele tokens as \"<a>/<b>\"");
        }
        for (String p : parts) {
            if (!hasToken(alleles, p)) {
                throw new IllegalArgumentException(where + ": no allele '" + p + "' on this gene");
            }
        }
        String forward = parts[0] + "/" + parts[1];
        String reversed = parts[1] + "/" + parts[0];
        if (combinations.contains(forward)) {
            return forward;
        }
        if (combinations.contains(reversed)) {
            return reversed;
        }
        throw new IllegalArgumentException(where + ": '" + text + "' is not a combination of this gene");
    }

    private static boolean hasToken(List<AlleleSpec> alleles, String token) {
        for (AlleleSpec a : alleles) {
            if (a.token().equals(token)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // founders - the wild population
    // ------------------------------------------------------------------

    /**
     * Reads {@code founders}: an object of combination to percentage. A
     * combination left out simply never turns up in the wild, which is how a
     * gene forbids (say) a lethal homozygote among founders. The percentages
     * should sum to 100; {@code genetics.FounderTable} normalises and warns if
     * they do not.
     */
    /**
     * A weight-per-combination table: {@code "founders"} (required, the wild
     * distribution) and {@code "splice"} (optional, what the Unknown Gene Splice carrot rolls
     * on this gene - roadmap &sect;14.1) share the shape exactly, kept as two
     * keys so they can differ. An absent optional table returns an empty list.
     */
    private static List<FounderWeight> readWeightTable(Map<String, Object> root, String tableKey,
                                                       List<String> combinations, boolean required) {
        Object raw = root.get(tableKey);
        if (raw == null) {
            if (required) {
                throw new IllegalArgumentException("a gene needs a \"" + tableKey + "\" table - the share of"
                        + " wild horses carrying each combination of its alleles, as percentages summing to 100."
                        + " Legal combinations here: " + combinations);
            }
            return List.of();
        }
        Map<String, Object> o = asObject(raw, tableKey);
        List<FounderWeight> out = new ArrayList<>();
        double total = 0;
        for (Map.Entry<String, Object> e : o.entrySet()) {
            String combination = e.getKey();
            if (!combinations.contains(combination)) {
                String[] parts = combination.split("/", -1);
                String flipped = parts.length == 2 ? parts[1] + "/" + parts[0] : null;
                if (flipped != null && combinations.contains(flipped)) {
                    combination = flipped;
                } else {
                    throw new IllegalArgumentException(tableKey + ": '" + e.getKey()
                            + "' is not a combination of this gene's alleles. Legal ones: " + combinations);
                }
            }
            if (!(e.getValue() instanceof Number n) || n.doubleValue() < 0) {
                throw new IllegalArgumentException(tableKey + " '" + e.getKey()
                        + "': a share is a percentage >= 0, got " + e.getValue());
            }
            out.add(new FounderWeight(combination, n.doubleValue()));
            total += n.doubleValue();
        }
        if (total <= 0) {
            throw new IllegalArgumentException(tableKey + ": every share is zero, so no horse can carry"
                    + " this gene at all");
        }
        return List.copyOf(out);
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

    /**
     * A palette or a ramp: an array of {@code "#rrggbb"}. One stop is legal but
     * pointless, so the parser asks for two - a one-stop ramp is a {@code TOWARD}
     * and saying so is a better error than a gradient that never moves.
     */
    private static List<Integer> readColors(Object raw, String where) {
        List<Object> a = asArray(raw, where);
        if (a.size() < 2) {
            throw new IllegalArgumentException(where + ": needs at least two colours, got " + a.size()
                    + " - a single colour is a TOWARD op, not a ramp or a palette");
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            out.add(readColor(a.get(i), where + " [" + i + "]"));
        }
        return List.copyOf(out);
    }

    /**
     * A {@code PATH}'s control points: a flat array of numbers,
     * {@code [u0, v0, u1, v1, ...]}.
     *
     * <p>Flat rather than an array of pairs because that is what survives being
     * hand-edited. A nested form puts a bracket between every coordinate and
     * every one of them is a chance to close the wrong one; a flat list has a
     * single failure - an odd length - and it can be named exactly, which is
     * what the error below does.
     */
    private static double[] readPoints(Object raw, String where) {
        List<Object> a = asArray(raw, where);
        if (a.size() % 2 != 0) {
            throw new IllegalArgumentException(where + ": needs an even number of coordinates - "
                    + "they are read as [u0, v0, u1, v1, ...] - but got " + a.size()
                    + ", so one coordinate is missing or one too many");
        }
        int count = a.size() / 2;
        if (count < 2) {
            throw new IllegalArgumentException(where + ": needs at least two points, got " + count
                    + " - a one-point path has no length and paints nothing");
        }
        if (count > SpecSchema.MAX_PATH_POINTS) {
            throw new IllegalArgumentException(where + ": at most " + SpecSchema.MAX_PATH_POINTS
                    + " points, got " + count + " - every texel of every skin walks all of them");
        }
        double[] out = new double[a.size()];
        for (int i = 0; i < a.size(); i++) {
            out[i] = asNumber(a.get(i), where + " [" + i + "]");
        }
        return out;
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
                case "on_hurt" -> new GeneAbility.Trigger.OnHurt();
                case "on_owner_hurt" -> new GeneAbility.Trigger.OnOwnerHurt();
                default -> throw new IllegalArgumentException(where + ": bare trigger must be "
                        + "'continuous', 'on_move', 'on_hurt' or 'on_owner_hurt'; use an object "
                        + "for 'interval' / 'on_interact'");
            };
        }
        Map<String, Object> o = asObject(raw, where);
        if (o.size() != 1) {
            throw new IllegalArgumentException(where + ": name exactly one of "
                    + "[continuous, on_move, interval, on_interact, on_hurt, on_owner_hurt]");
        }
        String kind = o.keySet().iterator().next();
        Object v = o.get(kind);
        return switch (kind) {
            case "continuous" -> new GeneAbility.Trigger.Continuous();
            case "on_move" -> new GeneAbility.Trigger.OnMove();
            case "on_hurt" -> new GeneAbility.Trigger.OnHurt();
            case "on_owner_hurt" -> new GeneAbility.Trigger.OnOwnerHurt();
            case "interval" -> {
                int ticks = (int) asNumber(v, where + " interval");
                if (ticks < 1) {
                    throw new IllegalArgumentException(where + " interval: at least 1 tick, got " + ticks);
                }
                yield new GeneAbility.Trigger.Interval(ticks);
            }
            case "on_interact" -> new GeneAbility.Trigger.OnInteract(v == null ? "" : asString(v, where + " on_interact"));
            default -> throw new IllegalArgumentException(where + ": unknown trigger '" + kind
                    + "'; allowed are [continuous, on_move, interval, on_interact, on_hurt, "
                    + "on_owner_hurt]");
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

    private static List<AlleleSpec> readAlleles(Map<String, Object> root) {
        List<Object> raw = array(root, "alleles");
        if (raw.size() < 2) {
            throw new IllegalArgumentException("a gene needs at least two alleles; the last one is the "
                    + "population's baseline, and a genotype code with no segment for this gene reads as "
                    + "two copies of it");
        }
        List<AlleleSpec> out = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Map<String, Object> a = asObject(raw.get(i), "allele " + (i + 1));
            expectKeys(a, "allele " + (i + 1), "token", "label");
            String token = string(a, "token", null);
            if (token.isBlank() || token.contains("/") || token.contains("-")) {
                throw new IllegalArgumentException("allele token '" + token + "' must be non-empty and free of "
                        + "'/' and '-' (they separate alleles and genes in a genotype code)");
            }
            boolean baseline = i == raw.size() - 1;
            out.add(new AlleleSpec(token,
                    string(a, "label", (baseline ? "Wild-type (" : "") + token + (baseline ? ")" : ""))));
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
        expectKeys(o, where, "name", "masks", "op", "emissive");
        String name = string(o, "name", where);
        boolean emissive = flag(o, "emissive", false);
        if (emissive && natural) {
            throw new IllegalArgumentException(where + ": 'emissive' is a magical-phase property - a "
                    + "natural gene moves pigment, and pigment does not glow. Move the glow to a "
                    + "magical gene, or drop the flag.");
        }

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
        // Coverage starts at 1 and the first mask folds into that, so MAX and
        // ADD in that position can only ever return 1 - the layer paints the
        // whole horse, whatever the mask says. It is always a mistake and it
        // looks exactly like a mask that is too generous, which is a bad
        // afternoon. Refuse it and say which one.
        if (!masks.isEmpty()) {
            Combine first = masks.get(0).combine();
            if (first == Combine.MAX || first == Combine.ADD) {
                throw new IllegalArgumentException(where + ": the FIRST mask cannot combine by "
                        + first + ". Coverage starts at 1, so " + first + " against it is always 1 "
                        + "and the layer covers the whole horse. Put a MULTIPLY mask first and "
                        + first + " the others into it.");
            }
        }
        if (emissive) {
            for (Mask m : masks) {
                if (m.type() == MaskType.PIGMENT || m.type() == MaskType.LUMA) {
                    throw new IllegalArgumentException(where + ": an emissive layer cannot use a "
                            + m.type() + " mask. Glow is decided in the overlay pass, after the "
                            + "texture is baked, and there is neither a pigment field nor a colour "
                            + "accumulator left there to read. Split the layer: paint the colour "
                            + "with the " + m.type() + " mask, glow with a shape one.");
                }
            }
        }
        // LUMA reads the colour the gradient chart resolved. The natural phase
        // runs before there is one - it is the pass that decides the pigment the
        // chart will be asked about - so the mask can only ever read zero there,
        // which looks exactly like a threshold that is too tight.
        if (natural) {
            for (Mask m : masks) {
                if (m.type() == MaskType.LUMA) {
                    throw new IllegalArgumentException(where + ": a natural gene cannot use a LUMA "
                            + "mask. It reads the colour phase 2 resolved through the gradient "
                            + "chart, and the natural phase is what decides the pigment that chart "
                            + "is handed - there is no colour to read yet. Use a PIGMENT mask to "
                            + "threshold the pigment levels themselves.");
                }
            }
        }
        return new Layer(name, List.copyOf(masks), new Op(opType, params), emissive);
    }

    private static Mask readMask(Map<String, Object> o, String where,
                                 List<Knob> knobs, Map<String, Integer> knobIndex) {
        MaskType type = enumValue(MaskType.class, string(o, "type", null), where + " type");
        Combine combine = enumValue(Combine.class, string(o, "combine", "MULTIPLY"), where + " combine");
        boolean invert = flag(o, "invert", false);
        Params params = readParams(o, SpecSchema.maskParams(type), SpecSchema.maskParamNames(type),
                where + " '" + type + "'", knobs, knobIndex, "type", "combine", "invert");
        checkBandNotReversed(type, params, knobs, where);
        if (type == MaskType.SVG) {
            params = flattenSvg(params, where);
        }
        return new Mask(type, params, combine, invert);
    }

    /**
     * Replace an {@code SVG} mask's {@code d} string with the polylines the
     * painter walks, and fill in the {@code viewBox} the file left out.
     *
     * <p>It happens here rather than in {@link #readParams} because flattening
     * needs the {@code transform} too, and that loop sees one parameter at a
     * time. Doing it at load rather than per texel is the whole performance
     * story of the mask; doing it <b>eagerly</b> rather than on first paint is
     * what turns a malformed path into a startup error that names the character
     * it choked on, instead of a horse that quietly has no marking.
     *
     * <p>A file with no {@code viewBox} gets the drawing's own bounding box,
     * which is right for a single mark and wrong for one of a set: two marks
     * that shared a canvas in the drawing program each fill their own viewport
     * here and lose the positions that related them. That is why the parameter
     * exists and why the importer always writes it.
     */
    private static Params flattenSvg(Params params, String where) {
        String d = params.text("d", null);
        if (d == null || d.trim().isEmpty()) {
            throw new IllegalArgumentException(where + " SVG: needs a 'd' - the path data. It is the "
                    + "one parameter with no useful default, since there is no such thing as an "
                    + "empty drawing");
        }
        SvgPath.Shape shape;
        try {
            shape = SvgPath.parse(d, params.text("transform", null));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(where + " SVG 'd': " + e.getMessage(), e);
        }
        Map<String, Object> out = new LinkedHashMap<>(params.raw());
        out.put("d", shape);
        if (!params.has("viewBox")) {
            out.put("viewBox", new double[]{shape.box()[0], shape.box()[1],
                    Math.max(1e-9, shape.width()), Math.max(1e-9, shape.height())});
        }
        return new Params(CommonMaps.copyOf(out));
    }

    /**
     * Mask types whose {@code from} / {@code to} become a <b>band</b> - either
     * {@code SpecPainter.band} or a bare {@code smoothstep}.
     *
     * <p>The {@code RAMP} op is deliberately absent even though it has the same
     * two parameter names. Its {@code from} and {@code to} are the ends of a
     * <i>linear interpolation</i> ({@code (t - from) / (to - from)}), so putting
     * them the other way round genuinely reverses the gradient and is a thing an
     * author might mean. On these masks it is never a thing anybody means.
     */
    private static final java.util.Set<MaskType> BANDED_MASKS = java.util.EnumSet.of(
            MaskType.AXIS, MaskType.PIGMENT, MaskType.LUMA, MaskType.WAVES);

    /**
     * <b>Refuse a band whose {@code to} can land below its {@code from}.</b>
     *
     * <p>{@code BodyStripes.smoothstep} is documented to treat {@code edge1 <=
     * edge0} as a hard step <i>in the original direction</i> - it does not
     * reverse the ramp, it removes it. That behaviour is correct and load-
     * bearing; what is wrong is that a gene file could ask for it by accident
     * and get something that looks nothing like what it said.
     *
     * <p>Both halves of known-gaps gap 106 are caught here.
     * <a href="../../../../../wiki/gene-patina.html">Patina</a> wrote
     * {@code from: 0.75, to: 0.35} meaning "fade in as the coat gets lighter"
     * and got "hard on wherever the coat is dark" - the exact opposite, with no
     * edge at all. <a href="../../../../../wiki/gene-integration.html">Integration</a>
     * pointed {@code to} at a knob whose <i>range straddled its own
     * {@code from}</i>, so about half the horses it drew were reversed and the
     * other half were not, which is the worse of the two failures because
     * nothing about the file looks wrong.
     *
     * <p>So the test is <b>possibility</b>, not certainty: a knob is a range,
     * two knobs are drawn independently, and "this can come out reversed on some
     * horses" is exactly the bug. At the time of writing no shipped gene trips
     * it - 513 banded masks, 130 of them pointing at knobs - so this is
     * prevention rather than a migration.
     */
    private static void checkBandNotReversed(MaskType type, Params params,
                                             List<Knob> knobs, String where) {
        if (!BANDED_MASKS.contains(type)) {
            return;
        }
        SpecSchema.Param fromParam = SpecSchema.maskParam(type, "from");
        SpecSchema.Param toParam = SpecSchema.maskParam(type, "to");
        if (fromParam == null || toParam == null) {
            return;
        }
        Value from = params.value("from", fromParam.fallback());
        Value to = params.value("to", toParam.fallback());
        for (int dose = 0; dose <= 2; dose++) {
            double highestFrom = highest(from, knobs, dose);
            double lowestTo = lowest(to, knobs, dose);
            if (lowestTo < highestFrom) {
                throw new IllegalArgumentException(where + " '" + type + "': 'to' can be below "
                        + "'from' (from reaches " + trim(highestFrom) + ", to can be as low as "
                        + trim(lowestTo) + (dose > 0 || isPerDose(from) || isPerDose(to)
                                ? " at dose " + dose : "")
                        + "). smoothstep treats that as a HARD STEP in the original direction "
                        + "rather than as a reversed ramp - the band loses its edge entirely and "
                        + "the layer paints the opposite of what the numbers read like. If you "
                        + "meant to select the other side, swap the two numbers and add "
                        + "\"invert\": true; if a knob is the problem, narrow its range so it "
                        + "cannot cross the other end.");
            }
        }
    }

    private static boolean isPerDose(Value v) {
        return v instanceof Value.PerDose;
    }

    /**
     * The largest number this value can take at {@code dose}.
     *
     * <p>Written as an {@code instanceof} chain rather than a pattern switch:
     * {@code common/} compiles for three targets and pattern switches are not
     * available on all of them (hard rule 2).
     */
    private static double highest(Value v, List<Knob> knobs, int dose) {
        if (v instanceof Value.Const c) {
            return c.v();
        }
        if (v instanceof Value.FromKnob k) {
            return knobs.get(k.index()).max();
        }
        return atDose((Value.PerDose) v, dose);
    }

    /** The smallest number this value can take at {@code dose}. See {@link #highest}. */
    private static double lowest(Value v, List<Knob> knobs, int dose) {
        if (v instanceof Value.Const c) {
            return c.v();
        }
        if (v instanceof Value.FromKnob k) {
            return knobs.get(k.index()).min();
        }
        return atDose((Value.PerDose) v, dose);
    }

    private static double atDose(Value.PerDose p, int dose) {
        return dose == 0 ? p.zero() : (dose == 1 ? p.one() : p.two());
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
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
                case COLORS -> readColors(raw, where + " '" + p.name() + "'");
                case POINTS -> readPoints(raw, where + " '" + p.name() + "'");
                case TEXT -> asString(raw, where + " '" + p.name() + "'");
                case BOX -> readBox(raw, where + " '" + p.name() + "'");
                // Held as the source string here and flattened in readMask,
                // which is the only place that can also see the 'transform'
                // this path has to be flattened under.
                case SVG -> asString(raw, where + " '" + p.name() + "'");
            });
        }
        return new Params(CommonMaps.copyOf(out));
    }

    /**
     * An SVG {@code viewBox}: {@code [minU, minV, width, height]}, in that
     * order and with both extents positive - the same four numbers, in the same
     * meaning, as the attribute it is copied from.
     */
    private static double[] readBox(Object raw, String where) {
        double[] box;
        if (raw instanceof String s) {
            box = com.example.horsegenetics.common.coat.pattern.SvgPath.numbers(s);
        } else {
            List<Object> a = asArray(raw, where);
            box = new double[a.size()];
            for (int i = 0; i < a.size(); i++) {
                box[i] = asNumber(a.get(i), where + " [" + i + "]");
            }
        }
        if (box.length != 4) {
            throw new IllegalArgumentException(where + ": a viewBox is exactly four numbers - "
                    + "minU, minV, width, height - but got " + box.length);
        }
        if (box[2] <= 0 || box[3] <= 0) {
            throw new IllegalArgumentException(where + ": a viewBox's width and height must both be "
                    + "above 0, got " + trim(box[2]) + " and " + trim(box[3]));
        }
        return box;
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
