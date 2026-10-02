package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.HairPattern;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.GeneRarity;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>The colour locus of a grown part</b> - the unicorn horn's colour rule, cut
 * out so every part that wants its own colour gene is one short subclass. Each part
 * gets its <i>own</i> locus (owner, 2026-10-01: "colour is its own gene per part"),
 * so a horse can have a white horn and black dragon horns; this class is only the
 * shared arithmetic.
 *
 * <table>
 *   <tr><th>alleles</th><th>part</th></tr>
 *   <tr><td>{@code Wht}</td><td>white - the wild type</td></tr>
 *   <tr><td>{@code Red Org Yel Grn Blu Ind Vio}</td><td>the rainbow, in order</td></tr>
 *   <tr><td>{@code Pnk Blk Gry}</td><td>pink, black, grey</td></tr>
 *   <tr><td>{@code Cha}</td><td>chaos - a colour written on the allele copy</td></tr>
 * </table>
 *
 * <h2>Two different copies make a two-tone part</h2>
 * Nothing here is dominant (owner's call, 2026-09-30). Two copies of one colour give
 * a part of that colour; two different ones give a part of one colour with a tip of
 * the other, fading between. So every allele a horse carries is on show, and a
 * breeder reads the genotype straight off the part.
 *
 * <p><b>Which colour is the base</b> is the allele that comes first in the order
 * above - so a white horse carrying red has a white part with a red tip, a red/blue
 * one a red part with a blue tip. It is fixed by the alleles, not by which parent
 * gave which copy, so the same pair always looks the same.
 *
 * <h2>Every red is its own red</h2>
 * Each copy carries two epigenetic numbers, {@link #HUE} and {@link #TONE}, that
 * nudge its colour a little off the allele's own: every red part is clearly red, but
 * no two are quite the same red, and a line's shade drifts over generations. White,
 * black and grey move only in lightness, and only a touch. A chaos copy ignores both
 * and wears {@link #CHAOS}, a whole colour of its own.
 *
 * <h2>What a subclass gives, and what it must not change</h2>
 * A key, a name, a priority and the {@link Words} its outcomes are written in. The
 * allele set, its order, the founder shares and the epigenome schema are this
 * class's and are the same for every part: <b>allele order is saved</b> (CLAUDE.md
 * hard rule 10), and {@code PartColourGeneTest} pins the horn colour gene's whole
 * observable state to the snapshot taken before it moved onto this base.
 *
 * <p>It paints nothing on the coat, so every outcome is a wild type and neither coat
 * golden reads it. A subclass also needs a {@code GeneFamily.MAGICAL_OVERRIDES} line
 * ({@code MAGIC_PARTS}), a {@code GeneDescriptions} entry, and its granting locus in
 * {@code GrownParts.shapes}'s every-part baseline - or the editors think it invisible.
 */
public abstract class AbstractPartColourGene implements Gene {

    /** Up to this far round the hue circle either way, at {@code hue = +-1}. About nine degrees. */
    static final double HUE_REACH = 0.025;
    /** Saturation and value move this fraction either way at {@code tone = +-1}. */
    static final double TONE_REACH = 0.10;
    /** White, black and grey move this much in value, and nothing else. */
    static final double GREY_REACH = 0.04;

    /** Per-copy nudge round the hue circle, {@code [-1,1]}. */
    public static final String HUE = "hue";
    /** Per-copy nudge to saturation and value, {@code [-1,1]}. */
    public static final String TONE = "tone";
    /** Prefix of the chaos copy's own colour. */
    public static final String CHAOS = "chaos";

    /**
     * How a part's outcomes are worded - the only text that differs between parts.
     *
     * @param idPrefix  outcome ids start with this: {@code horn} gives {@code horn-red}
     * @param noun      the part, as it follows a colour: "horn", "dragon horns"
     * @param plural    whether {@code noun} is plural (a pair) - "Red dragon horns."
     *                  rather than "A red horn."
     * @param chaosNoun the plural in "no two chaos ... match": "horns"
     * @param whiteNote what follows the white outcome's dash: "what nearly every unicorn has"
     * @param bearer    who it shows on: "a horse with a horn"
     */
    public record Words(String idPrefix, String noun, boolean plural, String chaosNoun,
                        String whiteNote, String bearer) {}

    /**
     * One allele's colour: its hue in turns, saturation and value, and how common
     * it is among wild founders. Negative hue means achromatic.
     */
    private record Hue(String token, String label, double h, double s, double v, double frequency) {
        boolean grey() {
            return h < 0;
        }
    }

    private static final List<Hue> HUES = List.of(
            new Hue("Wht", "White", -1, 0.00, 1.00, 0.70),
            new Hue("Red", "Red", 0.000, 0.85, 0.88, 0.027),
            new Hue("Org", "Orange", 0.078, 0.85, 0.95, 0.027),
            new Hue("Yel", "Yellow", 0.145, 0.80, 0.97, 0.027),
            new Hue("Grn", "Green", 0.347, 0.70, 0.75, 0.027),
            new Hue("Blu", "Blue", 0.597, 0.78, 0.90, 0.027),
            new Hue("Ind", "Indigo", 0.694, 0.70, 0.62, 0.027),
            new Hue("Vio", "Violet", 0.778, 0.60, 0.85, 0.027),
            new Hue("Pnk", "Pink", 0.917, 0.40, 0.98, 0.027),
            new Hue("Blk", "Black", -1, 0.00, 0.16, 0.027),
            new Hue("Gry", "Grey", -1, 0.00, 0.58, 0.027),
            new Hue("Cha", "Chaos", -1, 0.00, 1.00, 0.03));

    /** The chaos allele's index - its copy's colour is {@link #CHAOS}, not its row. */
    private static final int CHAOS_INDEX = HUES.size() - 1;

    private final String key;
    private final String name;
    private final int priority;
    private final List<Allele> alleles;
    private final List<Expression> expressions;
    private final List<Expression> solid;
    private final Expression twoTone;
    private final FounderTable founders;

    protected AbstractPartColourGene(String key, String name, int priority, Words words) {
        this.key = key;
        this.name = name;
        this.priority = priority;
        String shows = " Shows only on " + words.bearer() + ".";
        List<Allele> as = new ArrayList<>();
        List<Expression> solids = new ArrayList<>();
        Map<Allele, Double> frequencies = new LinkedHashMap<>();
        for (int i = 0; i < HUES.size(); i++) {
            Hue hue = HUES.get(i);
            Allele allele = new Allele(key, i, hue.token(), hue.label() + " (" + hue.token() + ")");
            as.add(allele);
            solids.add(Expression.wildType(words.idPrefix() + "-" + hue.label().toLowerCase(),
                    hue.label() + " " + words.noun(), describe(i, hue, words) + shows));
            if (i > 0) {
                frequencies.put(allele, hue.frequency());
            }
        }
        // White last: every founder table ends on its plain combination, so a
        // maximal roll is the ordinary horse (GenotypeTest holds every gene to it).
        frequencies.put(as.get(0), HUES.get(0).frequency());
        alleles = Collections.unmodifiableList(as);
        solid = Collections.unmodifiableList(solids);
        twoTone = Expression.wildType(words.idPrefix() + "-two-tone", "Two-tone " + words.noun(),
                "Two different colours: the " + words.noun() + (words.plural() ? " are" : " is")
                        + " one and " + (words.plural() ? "their tips" : "its tip")
                        + " the other, fading between. "
                        + "Which is the base is fixed by the pair - the one earlier in white, "
                        + "the rainbow, pink, black, grey, chaos." + shows);
        List<Expression> all = new ArrayList<>(solids);
        all.add(twoTone);
        expressions = Collections.unmodifiableList(all);
        founders = FounderTable.hardyWeinberg(frequencies, pair -> true);
    }

    /** One solid outcome's sentence, before "Shows only on...". */
    private static String describe(int index, Hue hue, Words words) {
        if (index == CHAOS_INDEX) {
            return "A colour written on the allele copy rather than on the allele, so no two chaos "
                    + words.chaosNoun() + " match and a line of them drifts.";
        }
        String colour = hue.label().toLowerCase();
        String phrase = words.plural()
                ? hue.label() + " " + words.noun()
                : (startsWithVowel(colour) ? "An " : "A ") + colour + " " + words.noun();
        return index == 0 ? phrase + " - " + words.whiteNote() + "." : phrase + ".";
    }

    private static boolean startsWithVowel(String s) {
        return !s.isEmpty() && "aeiou".indexOf(s.charAt(0)) >= 0;
    }

    @Override public final String key() { return key; }
    @Override public final String name() { return name; }
    @Override public final int priority() { return priority; }
    @Override public final boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.UNCOMMON; }
    @Override public final List<Allele> alleles() { return alleles; }
    @Override public final Allele defaultAllele() { return alleles.get(0); }
    @Override public final List<Expression> expressions() { return expressions; }
    @Override public final FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public final Expression expressionOf(AllelePair pair) {
        if (pair.first().equals(pair.second())) {
            return solid.get(pair.first().order());
        }
        return twoTone;
    }

    @Override
    public final EpiSchema epiSchema() {
        return EpiSchema.of(
                        EpiValue.uniform(HUE, -1.0, 1.0),
                        EpiValue.uniform(TONE, -1.0, 1.0))
                .and(EpiValue.colour(CHAOS, 0.55, 1.00, 0.70, 1.00));
    }

    /** A part's two colours, root and tip - equal for a one-colour part. Opaque ARGB. */
    public record Tints(int base, int tip) {}

    /**
     * The colours this horse's part would be, whether or not it has one - the
     * granting locus decides that. Each copy's colour comes off its own epigenetics;
     * the base is the copy whose allele comes first in the list.
     */
    public final Tints tintsFor(Genotype genotype, Epigenome epigenome) {
        AllelePair pair = genotype.pair(this);
        GeneEpigenetics epi = GeneEpigenetics.forGene(this, genotype, epigenome);
        int first = colourOf(pair.first(), epi.copy(0));
        int second = colourOf(pair.second(), epi.copy(1));
        return pair.second().order() < pair.first().order()
                ? new Tints(second, first)
                : new Tints(first, second);
    }

    /** One copy's colour: its allele's, nudged by that copy's own numbers. */
    static int colourOf(Allele allele, EpiValues epi) {
        if (allele.order() == CHAOS_INDEX) {
            return 0xFF000000 | epi.rgb(CHAOS);
        }
        Hue hue = HUES.get(allele.order());
        double tone = clampUnit(epi.get(TONE));
        if (hue.grey()) {
            double v = clamp01(hue.v() + GREY_REACH * tone);
            return 0xFF000000 | HairPattern.hsvToRgb(0.0, 0.0, v);
        }
        double h = hue.h() + HUE_REACH * clampUnit(epi.get(HUE));
        double s = clamp01(hue.s() * (1.0 + TONE_REACH * tone));
        double v = clamp01(hue.v() * (1.0 + TONE_REACH * tone));
        return 0xFF000000 | HairPattern.hsvToRgb(h, s, v);
    }

    private static double clampUnit(double x) {
        return x < -1.0 ? -1.0 : (x > 1.0 ? 1.0 : x);
    }

    private static double clamp01(double x) {
        return x < 0.0 ? 0.0 : (x > 1.0 ? 1.0 : x);
    }
}
