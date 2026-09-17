package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.pattern.WhitePattern;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Markings</b> ({@code horsegenetics.markings}) - the ordinary star, stripe,
 * snip, sock and stocking that a horse can have <i>without</i> carrying any
 * white pattern gene at all.
 *
 * <h2>Why this locus exists</h2>
 * Most white markings in life are <b>polygenic</b>: many small-effect variants
 * plus plain developmental variation in how far the pigment cells migrated. There
 * is no single locus for them, and the study that identified
 * {@link MitfGene#SW1} found no splash allele at all in 112 horses deliberately
 * picked for minimal white.
 *
 * <p>Until this gene, the mod had no such system, so {@code SW1} was standing in
 * for one - a majority of founders carried a copy, and its minimal expression was
 * painted over a range from a snip to a full splash. That gave the world its
 * ordinary marked horses at the cost of attributing them to a locus that mostly
 * does not cause them. <b>This locus takes that job back</b>, and {@code SW1}
 * drops to a frequency its own evidence supports.
 *
 * <h2>It is one locus standing in for many, and says so</h2>
 * A single additive locus is <i>not</i> what polygenic means. The honest
 * description is that this is a <b>dosage knob for a whole family of
 * small-effect variants</b>, in the same shape as {@link SootyGene} and
 * {@link ShadeGene} - both of which model a real signal whose causal mutation
 * nobody has identified. What it buys over an allele-free per-horse roll is that
 * a breeder can actually <i>select</i> for it: the dosage is inherited
 * Mendelianly, shows in the genotype code, and appears on a paper.
 *
 * <h2>The dosage</h2>
 * <table>
 *   <tr><th>combination</th><th>dosage</th><th>what shows</th></tr>
 *   <tr><td>{@code m/m}</td><td>0</td><td>clean - no white anywhere</td></tr>
 *   <tr><td>{@code M1/m}</td><td>1</td><td>a star, or a coronet or two</td></tr>
 *   <tr><td>{@code M1/M1}, {@code M2/m}</td><td>2</td><td>a star and a snip, socks on a leg or two</td></tr>
 *   <tr><td>{@code M2/M1}</td><td>3</td><td>a stripe or a narrow blaze, and socks</td></tr>
 *   <tr><td>{@code M2/M2}</td><td>4</td><td>a broad blaze and high stockings</td></tr>
 * </table>
 *
 * <h2>Why it runs at priority 65</h2>
 * <b>Before every white pattern locus</b> - tobiano is 72 and the rest run to 79
 * - so that {@code KIT}, {@code MITF}, {@code PAX3} and {@code EDNRB} stack their
 * white <i>on top of</i> whatever this horse already had, rather than the two
 * fighting. A sabino horse with three markings copies is a horse with a blaze
 * that its sabino then widens, which is the right relationship.
 *
 * <p>Natural, non-deterministic, and no health association: a marking is white
 * hair, and the deafness that rides along with the <i>splash</i> loci is a
 * property of those alleles rather than of white as such.
 */
public final class MarkingsGene implements Gene {

    public static final String KEY = "horsegenetics.markings";

    /** Before every white pattern locus, so they stack onto it. See the class note. */
    public static final int PRIORITY = 65;

    /** Half-width of the per-horse expression roll, in dosage units. */
    public static final double EXPRESSION_RANGE = 0.45;

    /** Dosage at or below which nothing shows at all. */
    public static final double CLEAR_MAX = 0.5;

    /** The highest dosage a horse can carry - two strong copies. */
    public static final double MAX_DOSAGE = 4.0;

    /**
     * How far up the leg the tallest stocking can reach, as a fraction of the
     * leg's own height. Short of the body on purpose: a marking that runs onto
     * the barrel is a <i>pattern</i> gene's business, not this one's.
     */
    private static final double MAX_LEG_HEIGHT = 0.62;

    /**
     * A leg's roll has to clear this before it whitens anything, which is what
     * leaves a three-socked horse with one clean leg. Without it every leg of a
     * marked horse would carry the same sock and the result reads as a costume.
     */
    private static final double LEG_THRESHOLD = 0.40;

    /** The face is a little readier to mark than the legs, as in life. */
    private static final double FACE_SHARE = 1.15;

    /**
     * How far the margin wanders. Small: ordinary markings have the clean edge of
     * a splash rather than the torn one of a sabino roaning out into the coat.
     */
    private static final double FACE_JAG = 0.03;

    /** Per-leg white height, indexed over {@link CoatRegions#LEGS}. */
    private static final String LEG = "leg";
    /** This horse's own offset from its raw dosage. */
    private static final String EXPRESSION = "expression";

    public final Allele M2 = new Allele(KEY, 0, "M2", "Markings, strong (M2)");
    public final Allele M1 = new Allele(KEY, 1, "M1", "Markings, mild (M1)");
    public final Allele m = new Allele(KEY, 2, "m", "Wild-type (m)");
    private final List<Allele> alleles = List.of(M2, M1, m);

    private final Expression WILD = Expression.wildType(
            "No ordinary white - a clean face and four coloured legs, unless a pattern gene "
                    + "puts white somewhere itself.");

    private final Expression MINIMAL = marked("minimal-markings", "Minimal markings",
            "A star, or nothing more than a coronet above a hoof. The commonest thing a horse "
                    + "can have and still be called solid.");
    private final Expression MARKED = marked("markings", "White markings",
            "A star and often a snip, with socks on one or two legs - the ordinary marked "
                    + "riding horse.");
    private final Expression BOLD = marked("bold-markings", "Bold markings",
            "A stripe or a narrow blaze down the face and socks on most legs.");
    private final Expression BROAD = marked("broad-markings", "Broad markings",
            "A wide blaze and high stockings. At this end it is easy to mistake for a "
                    + "minimally expressed sabino or splash, which is exactly the confusion the "
                    + "real-world literature describes.");

    private final List<Expression> expressions = List.of(WILD, MINIMAL, MARKED, BOLD, BROAD);

    private Expression marked(String id, String name, String description) {
        return Expression.of(id, name)
                .describe(description)
                .varies()
                .restrict(this::paint);
    }

    /**
     * Common, because ordinary markings are common: a clear majority of horses
     * carry at least one copy, and the broad end is the rare part.
     */
    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), pair -> true);

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(M2, 0.14);
        p.put(M1, 0.34);
        p.put(m, 0.52);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Markings"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return m; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    // ------------------------------------------------------------------

    /** This combination's dosage: {@code 0} to {@link #MAX_DOSAGE}. */
    public double dosage(AllelePair pair) {
        return copyDosage(pair.first()) + copyDosage(pair.second());
    }

    private double copyDosage(Allele a) {
        if (a.equals(M2)) {
            return 2.0;
        }
        return a.equals(M1) ? 1.0 : 0.0;
    }

    /** The dosage plus this horse's stored offset - why two same-dosage horses differ. */
    public static double score(double dosage, EpiValues epi) {
        return dosage + epi.get(EXPRESSION);
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        double d = dosage(pair);
        if (d <= CLEAR_MAX) {
            return WILD;
        }
        if (d <= 1.5) {
            return MINIMAL;
        }
        if (d <= 2.5) {
            return MARKED;
        }
        return d <= 3.5 ? BOLD : BROAD;
    }

    public boolean isMarked(AllelePair pair) {
        return dosage(pair) > CLEAR_MAX;
    }

    /**
     * The face vocabulary is shared with the white loci, so a markings blaze and
     * a sabino blaze are drawn by the same code and cannot drift apart.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                        EpiValue.uniform(EXPRESSION, -EXPRESSION_RANGE, EXPRESSION_RANGE),
                        EpiValue.perLeg(LEG, 0, 1))
                .and(WhitePattern.faceSchema().values().toArray(new EpiValue[0]));
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * Whiten the face marking and the legs this horse rolled. <b>Consumes no
     * {@code nextFloat()}</b> - every decision is stored epigenetics, so the same
     * horse marks identically every time it is drawn.
     */
    private PigmentField paint(CoatBuildContext ctx, PigmentView coat) {
        PigmentField out = coat.mutableCopy();
        Skin skin = ctx.skin();
        EpiValues epi = ctx.epigeneticsFor(KEY);

        double intensity = strength(score(dosage(ctx.genotype().pair(this)), epi));
        if (intensity <= 0) {
            return out;
        }

        WhitePattern.FaceMarking faceMark = WhitePattern.faceMarking(
                epi, skin, clamp01(intensity * FACE_SHARE), FACE_JAG);

        // Height on each leg, resolved once rather than per texel.
        List<Part> legs = CoatRegions.LEGS;
        double[] cutoff = new double[legs.size()];
        for (int i = 0; i < cutoff.length; i++) {
            double reach = legReach(intensity, epi.get(LEG, i));
            if (reach <= 0) {
                cutoff[i] = Double.NEGATIVE_INFINITY;
                continue;
            }
            Bounds b = HorseSkinGeometry.bounds(skin, legs.get(i));
            cutoff[i] = b.yMin() + b.span(Axis.Y) * reach;
        }

        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            int leg = legs.indexOf(part);
            if (leg >= 0) {
                if (point.y() <= cutoff[leg]) {
                    out.whiten(px, py, 1.0f);
                }
                return;
            }
            if (faceMark.covers(part, face, point)) {
                out.whiten(px, py, 1.0f);
            }
        });
        return out;
    }

    /** Score to a {@code [0,1]} intensity. */
    private static double strength(double score) {
        return clamp01((score - CLEAR_MAX) / (MAX_DOSAGE - CLEAR_MAX));
    }

    /**
     * How far up one leg the white runs, or {@code 0} for a clean leg. The
     * threshold is what makes a horse three-socked rather than four-socked.
     */
    private static double legReach(double intensity, double roll) {
        double bias = roll * (0.35 + 0.65 * intensity);
        if (bias < LEG_THRESHOLD) {
            return 0;
        }
        double over = (bias - LEG_THRESHOLD) / (1.0 - LEG_THRESHOLD);
        return clamp01(over) * MAX_LEG_HEIGHT * intensity;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
