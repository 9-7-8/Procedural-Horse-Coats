package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BodyNoise;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Sooty</b> ({@code horsegenetics.sooty}), also called smutty - the dark
 * countershading that lies over a horse's topline, crest, shoulder and croup
 * and makes it look substantially darker than its base colour says it should
 * be. A muddy palomino, a smutty buckskin, a liver-leaning chestnut and a dark
 * bay that people keep calling black are all this.
 *
 * <h2>Sooty in this model is a <i>failure to restrict</i></h2>
 * That is worth stating first because it is what makes the gene possible at all
 * here. Phase 1 only ever pushes pigment <b>down</b>: every texel starts at a
 * full load of both pigments and each gene takes some away. A gene that
 * "adds black" cannot exist.
 *
 * <p>Sooty does not add any. It <b>declines to remove</b> what agouti and the
 * dilutions were about to take: the eumelanin was always on that texel, and
 * this locus is the horse not switching it off along its topline. So the
 * painter walks the black level back <i>up</i> toward - never past - the load
 * the texel began the pipeline with, which is both the honest mechanical
 * description and, as it happens, what the biology is thought to be. The
 * chromosome-22 region the evidence points at sits beside <i>ASIP</i>, whose
 * whole job is telling a follicle to stop making black.
 *
 * <h2>What is actually known</h2>
 * <b>Not much, and nothing testable.</b> There is no validated commercial
 * "sooty" test and no demonstrated inheritance model - not dominant, not
 * recessive, not codominant. What exists is a 2020 genome-wide association
 * study of 126 bay horses finding a very strong, <i>dose-responsive</i>
 * association between a chromosome-22 region near <i>ASIP</i> and <i>RALY</i>
 * and how far black spreads over a bay. The causal mutation was not identified.
 *
 * <p>So this locus is a <b>dose-dependent modifier with a per-horse roll</b>,
 * exactly like {@link FlaxenGene} and
 * {@link com.example.horsegenetics.common.genetics.BayShade}, and it is
 * labelled as a modelled modifier rather than as settled science.
 *
 * <h2>Why it is not the shade locus, given they are the same paper</h2>
 * {@link ShadeGene} models the <i>same</i> chromosome-22 signal, and the two
 * are deliberately separate:
 * <ul>
 *   <li><b>Shade is where the evidence is.</b> The study measured bay horses and
 *       ranked bay shade; that is the claim it supports, and shade is scoped to
 *       exactly that - it does nothing on a chestnut.</li>
 *   <li><b>Sooty is where the phenotype is.</b> A muddy palomino and a smutty
 *       buckskin are real, common and obviously not bay shade, and no evidence
 *       says one mutation causes both. Folding them into shade would have
 *       claimed a great deal more than the paper does.</li>
 * </ul>
 * Two abstractions of overlapping evidence, each honest about its own scope. A
 * horse carries both, and a dark bay can be dark for either reason or both.
 *
 * <h2>The dosage</h2>
 * <table>
 *   <tr><th>combination</th><th>dosage</th><th>what shows</th></tr>
 *   <tr><td>{@code s/s}</td><td>0</td><td>clear - the base colour reads cleanly</td></tr>
 *   <tr><td>{@code S1/s}</td><td>1</td><td>a dorsal haze and a little shading at the croup</td></tr>
 *   <tr><td>{@code S1/S1}, {@code S2/s}</td><td>2</td><td>an obviously dark topline over a clearer belly</td></tr>
 *   <tr><td>{@code S2/S1}</td><td>3</td><td>a dark cape over the shoulder and back</td></tr>
 *   <tr><td>{@code S2/S2}</td><td>4</td><td>dark enough that a bay or a buckskin reads nearly black</td></tr>
 * </table>
 *
 * <h2>The same score looks different on every base</h2>
 * The genotype sets the <i>intensity</i>; what darkening looks like is up to
 * whatever pigment is there to keep. That falls out of the mechanism rather than
 * needing a table: on a bay the kept eumelanin is black, so it caps the
 * shoulder; on a palomino it drags the gold toward bronze; on a chestnut the
 * chart's warm edge stays brown a long way down, so it reads as a liver
 * overlay rather than as black hairs. On a plain black horse there is no
 * removed pigment to keep and the locus does nothing at all - which is exactly
 * what the reference says about identifying sooty on black.
 *
 * <p>Natural, non-deterministic, and <b>no health association whatsoever</b> -
 * see {@code wiki/gene-sooty.html} for why that is worth saying out loud.
 */
public final class SootyGene implements Gene {

    public static final String KEY = "horsegenetics.sooty";

    /**
     * After agouti and after {@link DunGene}, so there is something to keep and
     * a dun's primitive markings are already drawn; before the dilutions, so a
     * smutty buckskin's cape dilutes with the rest of it.
     */
    public static final int PRIORITY = 36;

    /** Half-width of the per-horse expression roll, in dosage units. */
    public static final double EXPRESSION_RANGE = 0.4;

    /** Dosage at or below which nothing shows. */
    public static final double CLEAR_MAX = 0.5;

    /** The highest dosage a horse can carry - two strong copies. */
    public static final double MAX_DOSAGE = 4.0;

    /**
     * How much black a fully sooted texel keeps. Short of 1 on purpose: sooty
     * makes a horse look nearly black and is not supposed to <i>be</i> black,
     * and the last of the gap is what leaves a dark bay still readable as a bay.
     */
    private static final float SOOT_BLACK = 0.92f;

    // --- the body map -------------------------------------------------

    /** Height up the barrel where the soot starts, and where it is full. */
    private static final double BELLY = 0.26;
    private static final double TOPLINE = 0.78;
    /** Extra weight at the croup and the shoulder - the two places it concentrates. */
    private static final double CAPE_BOOST = 0.30;
    private static final double CAPE_WIDTH = 0.26;
    /** How much of the neck's crest and the head take. */
    private static final double NECK_SHARE = 0.90;
    private static final double HEAD_SHARE = 0.34;
    /** Muddy lower legs - a chestnut-based signature, and only there. */
    private static final double LEG_SHARE = 0.55;
    private static final double LEG_TOP = 0.55;

    /** Mottling, so the overlay is hairs rather than an airbrush. */
    private static final double MOTTLE_SCALE = 0.42;
    private static final double MOTTLE_DEPTH = 0.28;
    private static final long MOTTLE_SEED = 0x50075CA5E7B0DD1EL;

    public final Allele S2 = new Allele(KEY, 0, "S2", "Sooty, strong (S2)");
    public final Allele S1 = new Allele(KEY, 1, "S1", "Sooty, mild (S1)");
    public final Allele s = new Allele(KEY, 2, "s", "Wild-type (s)");
    private final List<Allele> alleles = List.of(S2, S1, s);

    private final Expression WILD = Expression.wildType(
            "No dark overlay - whatever the base colour and the dilutions made, that is what shows.");

    private final Expression FAINT = sooty("faint-sooty", "Faint sooty",
            "A dark haze along the spine and a little shading over the croup. Easy to miss, and "
                    + "easy to mistake for a dun's dorsal stripe - which it is not: there are no "
                    + "leg bars, no shoulder bar and no mask with it.");
    private final Expression SOOTY = sooty("sooty", "Sooty",
            "An obviously darker topline, crest and upper barrel over a clearer belly and flank - "
                    + "the countershading the word usually means.");
    private final Expression HEAVY = sooty("heavy-sooty", "Heavy sooty",
            "A dark cape over the shoulder, back and croup, with the base colour left showing "
                    + "mostly low on the sides. On a chestnut-based coat the lower legs go muddy "
                    + "with it.");
    private final Expression EXTREME = sooty("extreme-sooty", "Extreme sooty",
            "Dark over nearly the whole body. A bay reads as brown or black, a buckskin as a dark "
                    + "bay, and a palomino as a bronze horse - which is the commonest reason a coat "
                    + "cannot be identified from a photograph.");

    private final List<Expression> expressions = List.of(WILD, FAINT, SOOTY, HEAVY, EXTREME);

    private Expression sooty(String id, String name, String description) {
        return Expression.of(id, name)
                .describe(description)
                .varies()
                .restrict(this::paint);
    }

    /**
     * Common: the reference calls sooty widespread across domestic horses rather
     * than a breed curiosity, and the visible rate is driven as much by which
     * base it lands on as by the allele. The <i>heavy</i> end is the rare part.
     */
    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), pair -> true);

    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(S2, 0.12);
        p.put(S1, 0.30);
        p.put(s, 0.58);
        return p;
    }

    @Override public String key() { return KEY; }
    @Override public String name() { return "Sooty"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return s; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    // ------------------------------------------------------------------

    /** This combination's dosage: {@code 0} to {@link #MAX_DOSAGE}. */
    public double dosage(AllelePair pair) {
        return copyDosage(pair.first()) + copyDosage(pair.second());
    }

    private double copyDosage(Allele a) {
        if (a.equals(S2)) {
            return 2.0;
        }
        return a.equals(S1) ? 1.0 : 0.0;
    }

    /** The dosage plus this horse's own roll. <b>Consumes one {@code nextFloat()}</b>. */
    public static double score(double dosage, Rng epi) {
        return dosage + (epi.nextFloat() * 2.0 - 1.0) * EXPRESSION_RANGE;
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        double d = dosage(pair);
        if (d <= CLEAR_MAX) {
            return WILD;
        }
        if (d <= 1.5) {
            return FAINT;
        }
        if (d <= 2.5) {
            return SOOTY;
        }
        return d <= 3.5 ? HEAVY : EXTREME;
    }

    public boolean isSooty(AllelePair pair) {
        return dosage(pair) > CLEAR_MAX;
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * Keep the black the rest of the pipeline was going to take, weighted by
     * where on the horse it is. <b>Consumes one {@code nextFloat()}</b> - the
     * expression roll.
     *
     * <p><b>It never raises a texel past where it started.</b> The field begins
     * at a full pigment load, so walking toward {@link #SOOT_BLACK} can only
     * ever restore something an earlier gene removed - it cannot invent
     * eumelanin a horse does not have. On a plain black, where nothing was
     * removed, that makes the whole pass a no-op.
     */
    private PigmentField paint(CoatBuildContext ctx, PigmentView coat) {
        Skin skin = ctx.skin();
        double intensity = strength(score(dosage(ctx.genotype().pair(this)),
                ctx.epigeneticsFor(KEY)));
        // Muddy lower legs are a chestnut-based signature; on a bay the legs are
        // already black-pointed and there is nothing for it to add.
        boolean muddyLegs = !ctx.genotype().hasBlackPigment();

        PigmentField out = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double w = regionWeight(skin, part, point, muddyLegs);
            if (w <= 0) {
                return;
            }
            double mottle = 1.0 - MOTTLE_DEPTH * BodyNoise.value(MOTTLE_SEED,
                    point.x() * MOTTLE_SCALE, point.y() * MOTTLE_SCALE, point.z() * MOTTLE_SCALE);
            float k = (float) clamp01(intensity * w * mottle);
            float black = out.black(px, py);
            if (SOOT_BLACK > black) {
                out.setBlack(px, py, black + (SOOT_BLACK - black) * k);
            }
        });
        return out;
    }

    /** Score to a {@code [0,1]} intensity. */
    private static double strength(double score) {
        return clamp01((score - CLEAR_MAX) / (MAX_DOSAGE - CLEAR_MAX));
    }

    /**
     * Where the soot sits: strongest along the topline and thinning to nothing
     * at the belly, with extra at the croup and the shoulder, some on the crest
     * and a little on the face.
     *
     * <p>The <b>mane, tail and ears are excluded outright</b>. Sooty is a
     * body-coat modifier; a dark mane on a sooty horse is the base colour's
     * doing, and the reference is explicit that mane and tail changes belong to
     * silver, flaxen, cream and sun rather than here.
     */
    private static double regionWeight(Skin skin, Part part, BodyPoint point, boolean muddyLegs) {
        switch (part) {
            case BODY: {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double fx = (point.x() - b.xMin()) / b.span(Axis.X);   // 0 rump .. 1 shoulder
                double fy = (point.y() - b.yMin()) / b.span(Axis.Y);   // 0 belly .. 1 topline
                double up = smooth01((fy - BELLY) / (TOPLINE - BELLY));
                double cape = bell(fx, 0.86) + bell(fx, 0.10);
                return clamp01(up * (1.0 + CAPE_BOOST * cape));
            }
            case NECK: {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
                return NECK_SHARE * smooth01((fy - BELLY) / (TOPLINE - BELLY));
            }
            case HEAD:
                return HEAD_SHARE;
            case LEFT_FRONT_LEG:
            case RIGHT_FRONT_LEG:
            case LEFT_HIND_LEG:
            case RIGHT_HIND_LEG: {
                if (!muddyLegs) {
                    return 0;
                }
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
                return LEG_SHARE * (1.0 - smooth01(fy / LEG_TOP));
            }
            default:
                return 0;   // mane, tail, ears, muzzle - not this gene's business
        }
    }

    /** A soft hump centred on {@code c}, {@link #CAPE_WIDTH} wide. */
    private static double bell(double f, double c) {
        return 1.0 - smooth01(Math.abs(f - c) / CAPE_WIDTH);
    }

    private static double smooth01(double t) {
        t = clamp01(t);
        return t * t * (3 - 2 * t);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
