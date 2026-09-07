package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PatchNoise;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
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

import java.util.List;

/**
 * <b>Tobiano</b> ({@code horsegenetics.tobiano}) - {@code To} dominant,
 * {@code to} wild-type. Natural, <b>non-deterministic</b>.
 *
 * <p>Large, smooth-edged white patches that <b>cross the topline</b> - the shape
 * that separates tobiano from {@link EdnrbGene frame overo}, whose splotches sit
 * in the middle of the side and are framed by colour above and below. The patch
 * field is a single warped fractal noise ({@link PatchNoise#field}) sampled in
 * body space over the whole trunk, neck, mane, tail <i>and legs at once</i>, so
 * a patch flows unbroken from the barrel down a leg and over the back without a
 * seam. A bias toward the spine lifts the field where it is high on the body, so
 * the patches reliably run up and over the topline. The head stays coloured.
 *
 * <p><b>How much</b> white a horse gets is a quantile of its own patch field
 * rather than a constant threshold on it, for the reasons set out on
 * {@link #COVER_MIN}; the biases decide only where that white pools. Every
 * vertical measurement is against the <b>barrel's own</b> bounds, not the
 * whole-horse box that runs to the ear tips.
 *
 * <p>Three knobs off the expressing {@code To} copy: {@code nextLong()} (the
 * patch field's seed), {@code nextFloat()} for how much white, {@code nextFloat()}
 * for the patch size.
 */
public final class TobianoGene implements Gene {

    public static final String KEY = "horsegenetics.tobiano";
    public static final int WILD_TOBIANO_ONE_IN = 50;

    /**
     * <b>Patch size</b>, as counts of patch periods along the <b>barrel's own</b>
     * bounding box rather than as absolute body-space frequencies. The adult
     * numbers are unchanged - {@value #PATCHES_MIN} periods over a 22-unit
     * barrel is the {@code 0.15} this used to hardcode - but a <b>foal</b> is
     * now marked at the same relative scale as the adult it grows into instead
     * of inheriting a coarser version of the same field.
     */
    private static final double PATCHES_MIN = 3.3;
    private static final double PATCHES_RANGE = 1.6;

    /**
     * <b>How much of the horse goes white</b>, as a genuine area fraction of
     * everything tobiano is allowed to mark - see {@link #paintTobiano}, which
     * sorts the field and cuts it at the matching quantile.
     *
     * <p>It did not used to be. The roll fed a bare {@code 1 - cover} threshold
     * on {@link PatchNoise#field}, which is an average of three octaves and so
     * concentrates hard around {@code 0.5}; a range written as {@code 0.40} to
     * {@code 0.56} therefore straddled the steepest part of the field's
     * distribution and delivered <b>31% to 86%</b> coverage - a 2.8&times;
     * swing behind a knob that reads like a 1.4&times; one. That is the same
     * defect {@link EdnrbGene} had, differing only in <i>where</i> on the bell
     * the range landed: frame's sat outside the field's spread and came out
     * blank-or-flooded, tobiano's sat across the middle and came out
     * over-sensitive.
     *
     * <p>The numbers below are the measured range the old code actually
     * produced, so <b>tobiano looks the way it did</b> - what changed is that
     * the knob now says so, and stays true if the field or the geometry moves
     * under it. {@value #COVER_GAMMA} reproduces the old roll's skew toward the
     * bold end, which came out of the field's own shape rather than out of any
     * decision.
     */
    // Package-private, not private: WhitePatternGenesTest reads these two and
    // asserts the delivered coverage actually lands in the band they declare.
    // That identity is the whole point of the quantile, so the test that pins
    // it should track the numbers rather than restate them.
    static final double COVER_MIN = 0.31;
    static final double COVER_RANGE = 0.55;
    private static final double COVER_GAMMA = 0.82;

    /**
     * <b>How hard the field is lifted toward the spine</b> so patches cross the
     * back - the shape that separates tobiano from frame overo, and the thing
     * this constant exists for.
     *
     * <p>It is measured up the <b>barrel's own height</b> now, from
     * {@value #TOPLINE_FROM} of it to the spine. It used to ramp across
     * {@link HorseSkinGeometry#bodyBounds}, the whole-horse box that runs to the
     * <i>ear tips</i> - on which the barrel's spine sits only 35% of the way
     * up, so the bias meant for the back was delivering about a third of its
     * nominal strength there and its full strength on the <b>mane</b>. The ramp
     * saturates above the barrel, which is what a bias toward the topline
     * should do: the crest and mane are over it, not beyond it.
     *
     * <p>Because the threshold is a quantile, this only decides <b>where</b> the
     * white goes and no longer leaks into how much there is. It is set to hold
     * the back at the coverage the old code gave it.
     */
    private static final double TOPLINE_BIAS = 0.048;
    private static final double TOPLINE_FROM = 0.42;

    /** Tobiano tends to white legs: an extra lift low on each leg. */
    private static final double LEG_LIFT = 0.19;

    public final Allele To = new Allele(KEY, 0, "To", "Tobiano (To)");
    public final Allele to = new Allele(KEY, 1, "to", "Wild-type (to)");
    private final List<Allele> alleles = List.of(To, to);

    private final Expression WILD = Expression.wildType("No white patches.");

    private final Expression TOBIANO = Expression.of("tobiano", "Tobiano")
            .describe("Large, smooth-edged white patches that run up and over the topline, flowing "
                    + "unbroken from the barrel down the legs. The legs tend to white; the head "
                    + "stays coloured.")
            .varies()
            .restrict(TobianoGene::paintTobiano);

    private final List<Expression> expressions = List.of(WILD, TOBIANO);

    private final FounderTable founders = FounderTable.hardyWeinberg(To, to, 1.0 / WILD_TOBIANO_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Tobiano"; }
    @Override public int priority() { return 72; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return to; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(To) ? TOBIANO : WILD;
    }

    public boolean isTobiano(AllelePair pair) {
        return pair.has(To);
    }

    /**
     * <b>Two passes.</b> The first scores every texel tobiano is allowed to
     * mark; the second whitens the top {@code cover} fraction of them. Scoring
     * twice costs two noise evaluations per texel and buys a threshold that is
     * a quantile of <i>this horse's own field</i> rather than a constant hoped
     * to sit somewhere inside it - see {@link #COVER_MIN} for what that was
     * costing.
     *
     * <p>It also separates two things that were one: {@code cover} decides how
     * much white there is, and {@link #TOPLINE_BIAS} / {@link #LEG_LIFT} decide
     * where it pools. A bias that only re-ranks texels cannot change how many of
     * them clear a quantile, so the back and the legs can be tuned without
     * moving the total.
     *
     * <p><b>Draw order</b>, off the expressing {@code To} copy:
     * {@code nextLong()} (the patch field's seed), then {@code nextFloat()}s for
     * the cover and the patch size. Unchanged, so a horse keeps its identity
     * across this rewrite even though its coat moves.
     */
    private static PigmentField paintTobiano(CoatBuildContext ctx, PigmentView coat) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double cover = COVER_MIN + COVER_RANGE * Math.pow(epi.nextFloat(), COVER_GAMMA);
        double patches = PATCHES_MIN + epi.nextFloat() * PATCHES_RANGE;

        Skin skin = ctx.skin();
        Shape shape = new Shape(skin, seed, patches);

        // Pass one: this horse's field, over the surface tobiano can mark.
        final double[][] scores = { new double[4096] };
        final int[] count = { 0 };
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double s = shape.score(part, point);
            if (s == EXCLUDED) {
                return;
            }
            if (count[0] == scores[0].length) {
                double[] bigger = new double[scores[0].length * 2];
                System.arraycopy(scores[0], 0, bigger, 0, count[0]);
                scores[0] = bigger;
            }
            scores[0][count[0]++] = s;
        });
        double[] sorted = java.util.Arrays.copyOf(scores[0], count[0]);
        java.util.Arrays.sort(sorted);
        int keep = (int) Math.round(cover * count[0]);
        double threshold = keep <= 0 ? Double.POSITIVE_INFINITY : sorted[count[0] - keep];

        // Pass two. A crisp binary cut: tobiano edges are sharp, and a
        // half-scaled black texel reads gold, not grey.
        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (shape.score(part, point) >= threshold) {
                f.setRed(px, py, 0f);
                f.setBlack(px, py, 0f);
            }
        });
        return f;
    }

    /** The score of a texel tobiano may not mark at all - below every threshold. */
    private static final double EXCLUDED = Double.NEGATIVE_INFINITY;

    /**
     * One horse's tobiano field: the patch frequency derived from this skin's
     * barrel, plus the two biases that decide where the white pools.
     * {@link #score} is a pure function of {@code (part, point)} so the
     * calibration pass and the paint pass cannot disagree about a texel.
     */
    private static final class Shape {

        private final long seed;
        private final double scale;
        private final double biasFrom;
        private final double biasTo;
        private final Skin skin;

        Shape(Skin skin, long seed, double patches) {
            this.skin = skin;
            this.seed = seed;
            Bounds barrel = HorseSkinGeometry.bounds(skin, Part.BODY);
            double length = Math.max(1e-6, barrel.span(Axis.X));
            double height = Math.max(1e-6, barrel.span(Axis.Y));
            this.scale = patches / length;
            // The ramp toward the spine, up the barrel's own height - and it
            // saturates above the barrel, because the crest and mane are over
            // the topline rather than beyond it.
            this.biasFrom = barrel.yMin() + height * TOPLINE_FROM;
            this.biasTo = barrel.yMax();
        }

        double score(Part part, HorseSkinGeometry.BodyPoint p) {
            if (part == Part.HEAD || part == Part.MUZZLE
                    || part == Part.LEFT_EAR || part == Part.RIGHT_EAR) {
                return EXCLUDED; // the head stays coloured
            }
            double v = PatchNoise.field(seed, p.x(), p.y(), p.z(), scale);
            v += TOPLINE_BIAS * clamp01((p.y() - biasFrom) / Math.max(1e-4, biasTo - biasFrom));
            if (isLeg(part)) {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double frac = (p.y() - b.yMin()) / b.span(Axis.Y);
                v += LEG_LIFT * (1.0 - PatchNoise.smoothstep(0.35, 0.8, frac));
            }
            return v;
        }
    }

    private static boolean isLeg(Part part) {
        return part == Part.LEFT_FRONT_LEG || part == Part.RIGHT_FRONT_LEG
                || part == Part.LEFT_HIND_LEG || part == Part.RIGHT_HIND_LEG;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
