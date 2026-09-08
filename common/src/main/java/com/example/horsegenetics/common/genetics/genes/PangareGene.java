package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BodyNoise;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Face;
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
 * <b>Pangare</b> ({@code horsegenetics.pangare}), also called <b>mealy</b> - the
 * pale muzzle, eye rings, belly, flanks and inner legs of an Exmoor pony, a
 * Fjord or a Haflinger. The soft parts of the horse go cream and the topline
 * does not.
 *
 * <h2>The exact mirror of {@link SootyGene}, and by the same trick</h2>
 * Sooty <i>keeps</i> black along the topline; pangare <b>takes red away</b> from
 * the underside. Both are one region-weighted move on one pigment, and between
 * them they are the two halves of countershading.
 *
 * <p>Working on red is not a stylistic choice - it is what makes every
 * base-colour rule in the reference fall out of the arithmetic instead of
 * needing a table:
 * <ul>
 *   <li><b>Chestnut</b>: maximum visibility. Nothing masks the red, so removing
 *       it walks the sample toward cream and pale gold.</li>
 *   <li><b>Bay</b>: the red-brown body lightens and the <b>black points do
 *       not</b> - a point is stored as red 0 and black 1, so there is no red on
 *       it to take.</li>
 *   <li><b>Black</b>: nothing visible at all. A black horse's red is fully
 *       masked by its black, and the gradient's bottom row is the same colour
 *       whatever the red says. The reference calls pangare hard to recognise on
 *       black; here it is invisible, exactly.</li>
 *   <li><b>Double dilute</b>: visually redundant, because the coat is already
 *       nearly out of red.</li>
 * </ul>
 * None of that is a special case in this file.
 *
 * <h2>What is actually known</h2>
 * <b>No confirmed causal mutation, and no test.</b> Breeders have treated it as
 * heritable for as long as anyone has kept records - it is near-fixed in some
 * breeds - but its inheritance mode is unresolved, and the variable intensity
 * points at several loci rather than one.
 *
 * <p>The nearest thing to evidence is a 2018 population study of a region on
 * chromosome 22 about 50&nbsp;kb downstream of <i>EDN3</i>, whose allele
 * frequencies split sharply between athletic breeds and the pony and draft
 * breeds where mealy is pervasive: Fjords 99%, Haflingers 99%, Exmoors 98%,
 * North Swedish Draught 90%, Gotlandsruss 81%, Ardennes 80%, Icelandics 74%
 * against Thoroughbreds 1% and Quarter Horses 8%. That is an association
 * between a marker and a <i>breed group</i>, not a cause, and the authors said
 * so. So this locus is a <b>dosage with a per-horse roll</b> - the same shape as
 * {@link FlaxenGene}, {@link SootyGene} and
 * {@link com.example.horsegenetics.common.genetics.BayShade} - and the breed
 * frequencies are where that population evidence actually lands.
 *
 * <p><b>Do not attach a health cost.</b> The same <i>EDN3</i> region has been
 * tied to exercise blood pressure and harness-racing performance in coldblooded
 * trotters, and that is a regulatory haplotype under study in a racing
 * population, not a pangare test. A mealy Fjord is not hypertensive. It is also
 * not <a href="#">frame overo</a>, which is <i>EDNRB</i>, a different gene, and
 * lethal in the homozygote.
 */
public final class PangareGene implements Gene {

    public static final String KEY = "horsegenetics.pangare";

    /** Immediately after {@link SootyGene}, its mirror, and before the dilutions. */
    public static final int PRIORITY = 37;

    /** Half-width of the per-horse expression roll, in dosage units. */
    public static final double EXPRESSION_RANGE = 0.4;

    /** Dosage at or below which nothing shows. */
    public static final double CLEAR_MAX = 0.5;

    /** The highest dosage a horse can carry - two strong copies. */
    public static final double MAX_DOSAGE = 4.0;

    /**
     * How much red a fully mealy texel keeps. Not lower: the chart has no gold
     * band (known gap #49), so a pale red keeps warming toward white and then
     * runs out of warmth - at 0.18 an extreme mealy belly came out grey-cream
     * rather than the cream the reference describes.
     */
    private static final float MEALY_RED = 0.26f;

    // --- the body map, from the reference's region weights -------------

    private static final double W_MUZZLE = 1.00;
    private static final double W_EYE_RING = 0.90;
    private static final double W_JAW = 0.72;
    private static final double W_BELLY = 0.92;
    private static final double W_FLANK = 0.70;
    private static final double W_ELBOW = 0.74;
    private static final double W_LOWER_NECK = 0.30;
    private static final double W_INNER_LEG = 0.70;
    private static final double W_OUTER_LEG = 0.12;

    /** Height up the barrel where the mealy stops, and where it is full. */
    private static final double BELLY_TOP = 0.46;
    /** How far up a leg the inner-thigh lightening reaches. */
    private static final double LEG_REACH = 0.62;
    /** How far from the forehead the pale eye ring reaches, in body units. */
    private static final double EYE_REACH = 2.4;
    /** Width of the elbow and flank humps along the barrel. */
    private static final double HUMP_WIDTH = 0.28;

    /** Mottling, so the edge of the mealy is hairs rather than an airbrush. */
    private static final double MOTTLE_SCALE = 0.38;
    private static final double MOTTLE_DEPTH = 0.26;
    private static final long MOTTLE_SEED = 0x9EA1;

    public final Allele Pa2 = new Allele(KEY, 0, "Pa2", "Pangare, strong (Pa2)");
    public final Allele Pa1 = new Allele(KEY, 1, "Pa1", "Pangare, mild (Pa1)");
    public final Allele pa = new Allele(KEY, 2, "pa", "Wild-type (pa)");
    private final List<Allele> alleles = List.of(Pa2, Pa1, pa);

    private final Expression WILD = Expression.wildType(
            "No mealy lightening - the underside is the same colour as the rest of the horse.");

    private final Expression TRACE = mealy("trace-pangare", "Trace pangare",
            "A pale lower muzzle and faint rings round the eyes, and very little else. Easy to "
                    + "mistake for ordinary shade variation, which is why a pale muzzle on its own "
                    + "does not identify the trait.");
    private final Expression MILD = mealy("mild-pangare", "Mild pangare",
            "A pale muzzle and clear eye rings, a light underline, and soft pale patches behind "
                    + "the elbows.");
    private final Expression MODERATE = mealy("pangare", "Pangare",
            "The familiar mealy horse: pale muzzle and jaw, an obviously light belly, groin and "
                    + "flank, and visible lightening up the inside of the legs.");
    private final Expression STRONG = mealy("strong-pangare", "Strong pangare",
            "Broad cream soft areas - the pale lower face reaching up the cheeks, a creamy "
                    + "underside climbing the barrel, and the insides of the legs pale most of the "
                    + "way up. The outer legs and any black points stay dark.");

    private final List<Expression> expressions = List.of(WILD, TRACE, MILD, MODERATE, STRONG);

    private Expression mealy(String id, String name, String description) {
        return Expression.of(id, name)
                .describe(description)
                .varies()
                .restrict(this::paint);
    }

    /**
     * The wild population, which is a mixture of everything. The breeds where it
     * is near-fixed carry it at their own rates - see {@code Breeds} - and this
     * is what a horse with no breed rolls.
     */
    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), pair -> true);

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(Pa2, 0.10);
        p.put(Pa1, 0.26);
        p.put(pa, 0.64);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Pangare (mealy)"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return pa; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    // ------------------------------------------------------------------

    /** This combination's dosage: {@code 0} to {@link #MAX_DOSAGE}. */
    public double dosage(AllelePair pair) {
        return copyDosage(pair.first()) + copyDosage(pair.second());
    }

    private double copyDosage(Allele a) {
        if (a.equals(Pa2)) {
            return 2.0;
        }
        return a.equals(Pa1) ? 1.0 : 0.0;
    }

    /**
     * The dosage plus this horse's own stored offset - the number that makes two
     * horses of the same dosage read a shade apart.
     */
    public static double score(double dosage, EpiValues epi) {
        return dosage + epi.get("expression");
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        double d = dosage(pair);
        if (d <= CLEAR_MAX) {
            return WILD;
        }
        if (d <= 1.5) {
            return TRACE;
        }
        if (d <= 2.5) {
            return MILD;
        }
        return d <= 3.5 ? MODERATE : STRONG;
    }

    public boolean isPangare(AllelePair pair) {
        return dosage(pair) > CLEAR_MAX;
    }

    /**
     * How far this horse reads from its raw dosage. Stored rather than rolled,
     * so a breeder can see which of two same-dosage horses got the darker end
     * of the range - and inherit it.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform("expression", -EXPRESSION_RANGE, EXPRESSION_RANGE));
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * Take red off the soft parts. <b>Consumes one {@code nextFloat()}</b> - the
     * expression roll.
     */
    private PigmentField paint(CoatBuildContext ctx, PigmentView coat) {
        Skin skin = ctx.skin();
        double intensity = strength(score(dosage(ctx.genotype().pair(this)),
                ctx.epigeneticsFor(KEY)));

        PigmentField out = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            float k = (float) clamp01(intensity * mealyCoverage(skin, part, face, point));
            if (k <= 0) {
                return;
            }
            float red = out.red(px, py);
            if (red > MEALY_RED) {
                out.setRed(px, py, red + (MEALY_RED - red) * k);
            }
        });
        return out;
    }

    /**
     * <b>The mealy field itself</b>, 0 to 1: the region map times the mottle,
     * and nothing about pigment. Shared with {@link HuedPangareGene}, which
     * paints exactly this shape in colour instead of taking red out of it -
     * see that class for why the two must not each carry their own copy.
     */
    static double mealyCoverage(Skin skin, Part part, Face face, BodyPoint point) {
        double w = regionWeight(skin, part, face, point);
        if (w <= 0) {
            return 0;
        }
        double mottle = 1.0 - MOTTLE_DEPTH * BodyNoise.value(MOTTLE_SEED,
                point.x() * MOTTLE_SCALE, point.y() * MOTTLE_SCALE, point.z() * MOTTLE_SCALE);
        return clamp01(w * mottle);
    }

    static double strength(double score) {
        return clamp01((score - CLEAR_MAX) / (MAX_DOSAGE - CLEAR_MAX));
    }

    /**
     * Where the mealy sits. The weights are the reference's own region table;
     * what this adds is the shapes they hang on.
     *
     * <p>The <b>mane, tail, ears and dorsal line get nothing</b>, which the
     * reference is explicit about: pangare is a soft-parts trait, and a horse
     * with a pale mane has flaxen, silver or cream instead.
     */
    private static double regionWeight(Skin skin, Part part, Face face, BodyPoint point) {
        switch (part) {
            case MUZZLE:
                return W_MUZZLE;
            case HEAD: {
                // Two things at once: the pale rings round the eyes, and the
                // lightening that spreads back from the muzzle along the jaw.
                double ring = W_EYE_RING * CoatRegions.faceMask(skin, part, point, EYE_REACH);
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
                double jaw = W_JAW * (1.0 - smooth01(fy / BELLY_TOP));
                return Math.max(ring, jaw);
            }
            case BODY: {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double fx = (point.x() - b.xMin()) / b.span(Axis.X);   // 0 rump .. 1 shoulder
                double fy = (point.y() - b.yMin()) / b.span(Axis.Y);   // 0 belly .. 1 topline
                double down = 1.0 - smooth01(fy / BELLY_TOP);
                double elbow = W_ELBOW * hump(fx, 0.84) * lift(fy);
                double flank = W_FLANK * hump(fx, 0.16) * lift(fy);
                return Math.max(W_BELLY * down, Math.max(elbow, flank));
            }
            case NECK: {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
                return W_LOWER_NECK * (1.0 - smooth01(fy / BELLY_TOP));
            }
            case LEFT_FRONT_LEG:
            case RIGHT_FRONT_LEG:
            case LEFT_HIND_LEG:
            case RIGHT_HIND_LEG: {
                // The inside of the leg, and only a little of the outside - the
                // one place in the mod that asks which face of a box it is on.
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
                double up = smooth01((fy - (1.0 - LEG_REACH)) / LEG_REACH);
                double medial = CoatRegions.medial(skin, part, face);
                return (W_OUTER_LEG + (W_INNER_LEG - W_OUTER_LEG) * medial) * up;
            }
            default:
                return 0;   // mane, tail, ears - not a soft part
        }
    }

    /** A soft hump centred on {@code c} along the barrel. */
    private static double hump(double f, double c) {
        return 1.0 - smooth01(Math.abs(f - c) / HUMP_WIDTH);
    }

    /** The elbow and flank patches sit low, but a little higher than the belly proper. */
    private static double lift(double fy) {
        return 1.0 - smooth01((fy - BELLY_TOP) / BELLY_TOP);
    }

    private static double smooth01(double t) {
        t = clamp01(t);
        return t * t * (3 - 2 * t);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
