package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.BaseCoats;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.Inheritance;
import com.example.horsegenetics.common.genetics.spec.GeneSpec;
import com.example.horsegenetics.common.genetics.spec.SpecGene;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * <b>What every painting outcome actually does to a horse</b>, measured, for the
 * breed designer's markings step: {@code wiki/breed-designer/assets/marking-facts.json}.
 *
 * <p>The designer lets a breed author filter every allele pair of every coat
 * gene by what it does - how much of the horse it covers at least and at most,
 * which parts it reaches, whether it is colourful or only black or only white,
 * whether it paints on a black horse, on white markings, on other colours,
 * whether only one sex can show it, and whether it brings magic with it. None of
 * that is written in a gene file and most of it could not be: coverage depends
 * on the epigenome, "reaches the tail" on the geometry, "only white" on what the
 * layers sum to. So it is <b>measured</b>, the way {@link CoatVisibility} decides
 * which horse to photograph: compose the horse with the pair and without it,
 * through the real pipeline, and look at the texels that moved.
 *
 * <p>It is baked rather than computed in the browser because it is thousands of
 * full coat composes. It is a fact sheet, not a picture - nothing is ever drawn
 * from it, so it does not pre-generate coats (hard rule 4).
 *
 * <h2>The measurements</h2>
 * <ul>
 *   <li><b>The stage</b> - the first base coat, in {@link BaseCoats#all()}
 *       order, the outcome visibly paints on. Bay for nearly everything; a gene
 *       that needs red pigment or somebody else's white falls through to a coat
 *       that has it, exactly as {@link CoatVisibility#firstShowing} chooses a
 *       gene's photograph. Measured on a bay alone, flaxen would read as a
 *       gene that covers nothing.</li>
 *   <li><b>Coverage</b> - share of the horse's mapped texels a pair moves on
 *       its stage, across several epigenomes and up to
 *       {@value #PAIRS_PER_OUTCOME} pairs of the outcome; the least and the most
 *       are both kept, because a filter on "at most 20%" and one on "at least
 *       20%" are different questions.</li>
 *   <li><b>Parts</b> - a part group is reached if at least {@value #PART_MIN}
 *       of its texels moved on any horse, so a small mark still reaches the part
 *       it sits on.</li>
 *   <li><b>Colour</b> - of the moved texels, the share that came out saturated
 *       and a different hue from what was there (colourful, a fifth or more),
 *       dark (black-only, four fifths or more) or pale and near-grey (white-only,
 *       four fifths or more).</li>
 *   <li><b>Interactions</b> - dark texels moved on a black horse; texels that
 *       were white before, moved on a tobiano; coloured texels moved on a bay or
 *       a chestnut. Each needs {@link CoatVisibility#MIN_TEXELS} to count.</li>
 *   <li><b>Sex</b> - on a sex-linked gene, which sexes carry a pair expressing
 *       the outcome, read off the placeholder allele.</li>
 * </ul>
 */
public final class MarkingFactsTool {

    private static final long[] SEEDS = {11L, 23L, 37L, 51L};
    private static final int PART_MIN = 8;
    private static final int PAIRS_PER_OUTCOME = 2;

    /** The part groups the designer filters on, in its order. */
    private static final Map<Part, String> GROUP = new EnumMap<>(Part.class);

    static {
        GROUP.put(Part.HEAD, "head");
        GROUP.put(Part.MUZZLE, "head");
        GROUP.put(Part.LEFT_EAR, "head");
        GROUP.put(Part.RIGHT_EAR, "head");
        GROUP.put(Part.NECK, "neck");
        GROUP.put(Part.BODY, "body");
        GROUP.put(Part.LEFT_FRONT_LEG, "front_legs");
        GROUP.put(Part.RIGHT_FRONT_LEG, "front_legs");
        GROUP.put(Part.LEFT_HIND_LEG, "back_legs");
        GROUP.put(Part.RIGHT_HIND_LEG, "back_legs");
        GROUP.put(Part.MANE, "mane");
        GROUP.put(Part.TAIL, "tail");
    }

    private MarkingFactsTool() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "wiki/breed-designer/assets/marking-facts.json");
        Files.createDirectories(out.toAbsolutePath().getParent());

        int[] template = GeneIconTool.loadTemplate();
        LutSet luts = GeneIconTool.loadLuts();

        int n = HorseSkinGeometry.SHEET_SIZE;
        String[] groupAt = new String[n * n];
        Map<String, Integer> groupSize = new LinkedHashMap<>();
        int[] mapped = {0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            String g = GROUP.get(part);
            groupAt[py * n + px] = g;
            groupSize.merge(g, 1, Integer::sum);
            mapped[0]++;
        });

        Map<String, Genotype> bases = new LinkedHashMap<>();
        for (BaseCoats.BaseCoat c : BaseCoats.all()) {
            bases.put(c.key(), c.genotype());
        }
        Plain plain = new Plain(template, luts);

        StringBuilder json = new StringBuilder(1 << 20);
        json.append("{\n  \"generated\": \"by MarkingFactsTool - do not edit; re-run :common:bakeMarkingFacts\",\n");
        json.append("  \"mappedTexels\": ").append(mapped[0]).append(",\n");
        json.append("  \"genes\": {");
        boolean firstGene = true;
        int outcomes = 0;
        for (Gene gene : Genes.codeOrder()) {
            if (!gene.affectsCoat()) {
                continue;
            }
            Map<String, List<AllelePair>> byOutcome = new LinkedHashMap<>();
            Map<String, Expression> expressionById = new LinkedHashMap<>();
            for (Allele a : gene.alleles()) {
                for (Allele b : gene.alleles()) {
                    if (b.order() < a.order()) {
                        continue;
                    }
                    AllelePair pair = new AllelePair(a, b);
                    if (!gene.canOccur(pair)) {
                        continue;
                    }
                    Expression e = gene.expressionOf(pair);
                    if (e == null || e.wildType()) {
                        continue;
                    }
                    byOutcome.computeIfAbsent(e.id(), k -> new ArrayList<>()).add(pair);
                    expressionById.put(e.id(), e);
                }
            }
            if (byOutcome.isEmpty()) {
                continue;
            }
            json.append(firstGene ? "\n" : ",\n");
            firstGene = false;
            json.append("    ").append(quote(gene.key())).append(": {")
                    .append("\"natural\": ").append(gene.isNatural())
                    .append(", \"family\": ").append(quote(GeneFamily.of(gene).name()))
                    .append(", \"sexLinked\": ").append(gene.inheritance().sexLinked())
                    .append(", \"outcomes\": [");
            boolean firstOutcome = true;
            for (Map.Entry<String, List<AllelePair>> e : byOutcome.entrySet()) {
                Facts f = measure(gene, e.getValue(), bases, plain, groupAt, groupSize, mapped[0]);
                Expression ex = expressionById.get(e.getKey());
                json.append(firstOutcome ? "\n" : ",\n");
                firstOutcome = false;
                json.append("      {\"id\": ").append(quote(ex.id()))
                        .append(", \"name\": ").append(quote(ex.name()))
                        .append(", \"pairs\": [");
                List<String> pairs = new ArrayList<>();
                for (AllelePair p : e.getValue()) {
                    pairs.add(quote(p.first().token() + "/" + p.second().token()));
                }
                json.append(String.join(", ", pairs)).append("]")
                        .append(", \"stage\": ").append(quote(f.stage))
                        .append(", \"coverageMin\": ").append(round1(f.coverageMin))
                        .append(", \"coverageMax\": ").append(round1(f.coverageMax))
                        .append(", \"parts\": [");
                List<String> parts = new ArrayList<>();
                for (String p : f.parts) {
                    parts.add(quote(p));
                }
                json.append(String.join(", ", parts)).append("]")
                        .append(", \"colorful\": ").append(f.colorful)
                        .append(", \"blackOnly\": ").append(f.blackOnly)
                        .append(", \"whiteOnly\": ").append(f.whiteOnly)
                        .append(", \"onBlack\": ").append(f.onBlack)
                        .append(", \"onWhite\": ").append(f.onWhite)
                        .append(", \"onColour\": ").append(f.onColour)
                        .append(", \"sexes\": [").append(sexes(gene, e.getValue())).append("]")
                        .append(", \"effects\": ").append(hasEffects(gene, e.getValue()))
                        .append("}");
                outcomes++;
            }
            json.append("\n    ]}");
        }
        json.append("\n  }\n}\n");
        Files.writeString(out, json.toString(), StandardCharsets.UTF_8);
        System.out.println("wrote facts for " + outcomes + " outcomes to " + out.toAbsolutePath());
    }

    // ------------------------------------------------------------------

    private static final class Facts {
        String stage = "bay";
        double coverageMin = Double.MAX_VALUE;
        double coverageMax = 0;
        Set<String> parts = new LinkedHashSet<>();
        boolean colorful;
        boolean blackOnly;
        boolean whiteOnly;
        boolean onBlack;
        boolean onWhite;
        boolean onColour;
    }

    /** Plain horses, composed once per base and seed and reused for every gene. */
    private static final class Plain {
        private final int[] template;
        private final LutSet luts;
        private final Map<String, int[]> cache = new LinkedHashMap<>();

        Plain(int[] template, LutSet luts) {
            this.template = template;
            this.luts = luts;
        }

        int[] of(String baseKey, Genotype base, long seed) {
            return cache.computeIfAbsent(baseKey + "#" + seed, k -> compose(base, seed));
        }

        int[] compose(Genotype gt, long seed) {
            return CoatTextureComposer.compose(gt, Epigenome.fromSeed(seed), Skin.ADULT, true, template, luts);
        }
    }

    private static Facts measure(Gene gene, List<AllelePair> pairs, Map<String, Genotype> bases, Plain plain,
                                 String[] groupAt, Map<String, Integer> groupSize, int mapped) {
        Facts f = new Facts();
        // The stage: bay unless the outcome shows on nothing there.
        String stageKey = "bay";
        for (String key : bases.keySet()) {
            Genotype b = bases.get(key);
            int[] before = plain.of(key, b, SEEDS[0]);
            int[] after = plain.compose(b.with(pairs.get(0)), SEEDS[0]);
            int count = 0;
            for (int i = 0; i < after.length; i++) {
                if (groupAt[i] != null && moved(before[i], after[i])) {
                    count++;
                }
            }
            if (count >= CoatVisibility.MIN_TEXELS) {
                stageKey = key;
                break;
            }
        }
        f.stage = stageKey;
        Genotype bay = bases.get(stageKey);
        Map<String, Integer> reached = new LinkedHashMap<>();
        int moved = 0;
        int chromatic = 0;
        int dark = 0;
        int light = 0;
        List<AllelePair> sample = pairs.subList(0, Math.min(PAIRS_PER_OUTCOME, pairs.size()));
        for (AllelePair pair : sample) {
            for (long seed : SEEDS) {
                int[] before = plain.of(stageKey, bay, seed);
                int[] after = plain.compose(bay.with(pair), seed);
                int count = 0;
                Map<String, Integer> hereParts = new LinkedHashMap<>();
                for (int i = 0; i < after.length; i++) {
                    if (groupAt[i] == null || !moved(before[i], after[i])) {
                        continue;
                    }
                    count++;
                    hereParts.merge(groupAt[i], 1, Integer::sum);
                    float[] hsv = hsv(after[i]);
                    float[] was = hsv(before[i]);
                    if (hsv[1] > 0.30f && hsv[2] > 0.25f
                            && (was[1] < 0.2f || hueGap(hsv[0], was[0]) > 30f)) {
                        chromatic++;
                    } else if (hsv[2] < 0.30f || (hsv[1] < 0.25f && hsv[2] < 0.5f)) {
                        dark++;
                    } else if (hsv[1] < 0.3f && hsv[2] > 0.6f) {
                        light++;
                    }
                }
                moved += count;
                double cover = 100.0 * count / mapped;
                f.coverageMin = Math.min(f.coverageMin, cover);
                f.coverageMax = Math.max(f.coverageMax, cover);
                for (Map.Entry<String, Integer> e : hereParts.entrySet()) {
                    if (e.getValue() >= PART_MIN) {
                        reached.merge(e.getKey(), 1, Integer::sum);
                    }
                }
            }
        }
        for (String g : new String[]{"head", "neck", "body", "front_legs", "back_legs", "mane", "tail"}) {
            if (reached.containsKey(g)) {
                f.parts.add(g);
            }
        }
        if (moved > 0) {
            f.colorful = chromatic * 5 >= moved;          // a fifth or more saturated and new
            f.blackOnly = dark * 5 >= moved * 4;
            f.whiteOnly = light * 5 >= moved * 4;
        }
        if (f.coverageMin == Double.MAX_VALUE) {
            f.coverageMin = 0;
        }

        AllelePair pair = sample.get(0);
        long seed = SEEDS[0];
        f.onBlack = countWhere(plain, bases, "black", pair, seed, c -> hsv(c)[2] < 0.3f) >= CoatVisibility.MIN_TEXELS;
        f.onWhite = countWhere(plain, bases, "tobiano", pair, seed,
                c -> hsv(c)[1] < 0.15f && hsv(c)[2] > 0.8f) >= CoatVisibility.MIN_TEXELS;
        int coloured = countWhere(plain, bases, "bay", pair, seed, c -> hsv(c)[1] > 0.25f && hsv(c)[2] > 0.25f)
                + countWhere(plain, bases, "chestnut", pair, seed, c -> hsv(c)[1] > 0.25f && hsv(c)[2] > 0.25f);
        f.onColour = coloured >= CoatVisibility.MIN_TEXELS;
        return f;
    }

    private interface Pick {
        boolean test(int argb);
    }

    /** Texels moved by {@code pair} on base {@code key} whose colour before it painted passes {@code was}. */
    private static int countWhere(Plain plain, Map<String, Genotype> bases, String key, AllelePair pair,
                                  long seed, Pick was) {
        Genotype base = bases.get(key);
        if (base == null) {
            return 0;
        }
        int[] before = plain.of(key, base, seed);
        int[] after = plain.compose(base.with(pair), seed);
        int count = 0;
        for (int i = 0; i < after.length; i++) {
            if ((before[i] >>> 24) != 0 && moved(before[i], after[i]) && was.test(before[i])) {
                count++;
            }
        }
        return count;
    }

    private static boolean moved(int a, int b) {
        int d = Math.max(Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)),
                Math.max(Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)), Math.abs((a & 0xFF) - (b & 0xFF))));
        return d >= CoatVisibility.CHANNEL_STEP;
    }

    /** {hue 0-360, saturation 0-1, value 0-1}. */
    private static float[] hsv(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0) {
            h = 0;
        } else if (max == r) {
            h = 60f * (((g - b) / d) % 6f);
        } else if (max == g) {
            h = 60f * (((b - r) / d) + 2f);
        } else {
            h = 60f * (((r - g) / d) + 4f);
        }
        if (h < 0) {
            h += 360f;
        }
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    private static float hueGap(float a, float b) {
        float d = Math.abs(a - b) % 360f;
        return d > 180f ? 360f - d : d;
    }

    /**
     * Which sexes can carry a pair showing this outcome. On a sex-linked locus a
     * hemizygous horse's spare slot holds the placeholder allele, so the count
     * of placeholders in a pair says whose pair it is.
     */
    private static String sexes(Gene gene, List<AllelePair> pairs) {
        Inheritance inh = gene.inheritance();
        if (!inh.sexLinked()) {
            return "\"male\", \"female\"";
        }
        boolean male = false;
        boolean female = false;
        for (AllelePair p : pairs) {
            int placeholders = (p.first().token().equals(inh.placeholderToken()) ? 1 : 0)
                    + (p.second().token().equals(inh.placeholderToken()) ? 1 : 0);
            if (placeholders == 1) {
                male = true;
            } else if (inh == Inheritance.X_LINKED ? placeholders == 0 : placeholders == 2) {
                female = true;
            }
        }
        List<String> out = new ArrayList<>();
        if (male) {
            out.add("\"male\"");
        }
        if (female) {
            out.add("\"female\"");
        }
        return String.join(", ", out);
    }

    /** Does showing this outcome also give the horse an effect - a trail, a behaviour, a yield? */
    private static boolean hasEffects(Gene gene, List<AllelePair> pairs) {
        if (gene instanceof SpecGene spec) {
            for (AllelePair p : pairs) {
                GeneSpec.ExpressionSpec e = spec.expressionSpecOf(p);
                if (e != null && !e.abilities().isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        return gene instanceof AbilityContribution || gene instanceof EpigeneticAbilityContribution;
    }

    private static String round1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String quote(String s) {
        StringBuilder out = new StringBuilder(s.length() + 2).append('"');
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
