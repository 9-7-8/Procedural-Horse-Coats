package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BodyNoise;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PatchNoise;
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
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.List;

/**
 * <b>Rabicano</b> ({@code horsegenetics.rabicano}) - white ticking that starts
 * at the tail dock and the flank and works forward: a <b>coon tail</b>, a
 * scatter of white hairs over the rear barrel and belly, and at full strength a
 * set of broken vertical <b>rib bars</b>.
 *
 * <h2>Inheritance and appearance are two different things here</h2>
 * That separation is the whole model, and it is the honest one. Pedigrees behave
 * <i>as if</i> rabicano were a simple autosomal dominant - an apparently
 * rabicano foal usually has a rabicano parent - but no causal variant has been
 * identified, there is no test, and the expression varies from a few frosted
 * hairs at the dock to something that reads almost roan. A parent can be so
 * minimal that it was recorded as solid.
 *
 * <p>So: the <b>allele is dominant</b> and inherits like one, and what it
 * <b>shows</b> is a per-horse roll that can come out at nothing. A visibly plain
 * horse can carry it and throw a strongly ticked foal, which in a real barn is
 * the commonest thing rabicano does and is impossible to model with a locus
 * whose phenotype is fixed by its genotype.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code rb/rb}</td><td>wild type</td></tr>
 *   <tr><td>{@code Rb/rb}</td><td>{@code rabicano} - anything from nothing to a well-ticked flank</td></tr>
 *   <tr><td>{@code Rb/Rb}</td><td>{@code strong-rabicano} - the same range with its floor raised</td></tr>
 * </table>
 *
 * <p>The homozygote shifts the <i>mean</i> up rather than forcing the maximum.
 * A dosage effect has never been demonstrated for rabicano - there are no
 * confirmed {@code RR} horses to compare - so it is offered as the game's
 * hypothesis, with the ranges deliberately overlapping.
 *
 * <h2>Not classic roan</h2>
 * They share "white hairs mixed through the coat" and nothing else.
 * {@link RoanGene} is broad and even over the whole trunk with a
 * <b>dark head and dark lower legs</b>; this starts at the <b>tail and
 * flank</b> and radiates forward, leaves the topline nearly alone, and has a
 * coon tail, which roan never does. A heavily ticked rabicano can look roan at a
 * distance, and the tail is what tells them apart.
 *
 * <p><b>No health association at all.</b> Not a lethal homozygote, not deafness,
 * not an eye or hoof effect - and the page for it says so at length, because
 * every <i>other</i> white pattern in this mod carries something and it would be
 * easy to assume this one does too.
 */
public final class RabicanoGene implements Gene {

    public static final String KEY = "horsegenetics.rabicano";

    /** With the other white-hair pattern: after {@link RoanGene}, before the spotting loci. */
    public static final int PRIORITY = 71;

    /** How many founders in a hundred carry at least one copy. */
    private static final double CARRIER_PERCENT = 6.0;
    private static final double HOMOZYGOTE_PERCENT = 0.2;

    /** Expression floor and ceiling for one copy, and for two. */
    private static final double HET_MIN = 0.0;
    private static final double HET_MAX = 0.78;
    private static final double HOM_MIN = 0.24;
    private static final double HOM_MAX = 1.0;

    /** Fraction of hairs that go white where the pattern is at full strength. */
    private static final double WHITE_FRACTION = 0.62;
    /** Noise frequency for the individual hairs, and the softness of the flecked edge. */
    private static final double HAIR_FREQ = 2.9;
    private static final double EDGE = 0.10;

    // --- the four layers ------------------------------------------------

    /** Tail frosting: how much of the dock is touched, and the coon-tail banding. */
    private static final double TAIL_DOCK = 0.62;
    private static final double TAIL_BANDS = 3.4;
    /** Flank: where the field is centred along and up the barrel, and how far it reaches. */
    private static final double FLANK_X = 0.17;
    private static final double FLANK_Y = 0.34;
    private static final double FLANK_REACH = 0.46;
    /** Belly: how high up the barrel the underside ticking climbs. */
    private static final double BELLY_TOP = 0.30;
    /** Rib bars: spacing along the barrel, duty cycle, and how far forward they reach. */
    private static final double BAR_SPACING = 0.115;
    private static final double BAR_DUTY = 0.40;
    private static final double BAR_REACH = 0.72;

    public final Allele Rb = new Allele(KEY, 0, "Rb", "Rabicano (Rb)");
    public final Allele rb = new Allele(KEY, 1, "rb", "Wild-type (rb)");
    private final List<Allele> alleles = List.of(Rb, rb);

    private final Expression WILD = Expression.wildType("No white ticking.");

    private final Expression RABICANO = Expression.of("rabicano", "Rabicano")
            .describe("White ticking from the tail dock and flank forward - a frosted, banded coon "
                    + "tail and a scatter of white hairs over the rear barrel and belly. How much "
                    + "shows varies enormously and can be almost nothing: a horse recorded as solid "
                    + "can carry it and throw a strongly ticked foal.")
            .varies()
            .restrict(this::paint);

    private final Expression STRONG = Expression.of("strong-rabicano", "Strong rabicano")
            .describe("Two copies. The same pattern with its floor raised - a horse that never "
                    + "comes out entirely plain, often with broken vertical rib bars up the barrel "
                    + "and enough white to be mistaken for a roan until you look at its tail.")
            .varies()
            .restrict(this::paint);

    private final List<Expression> expressions = List.of(WILD, RABICANO, STRONG);

    /**
     * Reported across many breeds and quantified in none of them - there is no
     * test, so there are no population frequencies to copy. Uncommon but not
     * rare, which is what "familiar to Arabian and Quarter Horse breeders"
     * amounts to.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(Rb, Rb, HOMOZYGOTE_PERCENT)
            .weight(Rb, rb, CARRIER_PERCENT)
            .weight(rb, rb, 100.0 - CARRIER_PERCENT - HOMOZYGOTE_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Rabicano"; }
    @Override public int priority() { return PRIORITY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return rb; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(Rb)) {
            return STRONG;
        }
        return pair.has(Rb) ? RABICANO : WILD;
    }

    public boolean isRabicano(AllelePair pair) {
        return pair.has(Rb);
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * <b>Four draws</b>: the hair seed, the expression roll, the tail's own
     * emphasis, and the flank's asymmetry between the two sides.
     *
     * <p>The expression roll is the gene. It runs from zero on a heterozygote -
     * a real, ordinary outcome, not an edge case - and from a floor on a
     * homozygote, with the two ranges overlapping most of the way.
     */
    /**
     * Where in its dosage's range this horse sits, how tail-heavy its ticking
     * is, and which flank carries more of it.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.seed("seed"),
                EpiValue.uniform("expression", 0, 1),
                EpiValue.uniform("tail_emphasis", 0.65, 1.0),
                EpiValue.uniform("side_bias", -0.15, 0.15));
    }

    private PigmentField paint(CoatBuildContext ctx, PigmentView coat) {
        EpiValues epi = ctx.epigeneticsFor(KEY);
        long seed = epi.seed("seed");
        boolean twoCopies = ctx.genotype().pair(this).homozygousFor(Rb);
        double lo = twoCopies ? HOM_MIN : HET_MIN;
        double hi = twoCopies ? HOM_MAX : HET_MAX;
        // Stored as a position in the range, because the range itself depends on
        // dosage - a horse that gains a second copy keeps its place in the range
        // rather than being re-rolled inside the wider one.
        double expression = lo + epi.get("expression") * (hi - lo);
        double tailEmphasis = epi.get("tail_emphasis");
        // The two flanks are related and are not mirror images - one side always
        // carries a little more than the other.
        double sideBias = epi.get("side_bias");

        Skin skin = ctx.skin();
        PigmentField f = coat.mutableCopy();
        if (expression <= 0.02) {
            return f;   // a carrier that shows nothing at all
        }
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double region = regionWeight(skin, part, point, expression, tailEmphasis, sideBias, seed);
            if (region <= 0) {
                return;
            }
            // The same near-binary dither classic roan uses: a texel is a white
            // hair or a coloured one, and the region weight sets what fraction
            // of them are white.
            double n = BodyNoise.value(seed, point.x() * HAIR_FREQ, point.y() * HAIR_FREQ,
                    point.z() * HAIR_FREQ * 1.6);
            double threshold = 1.0 - region * WHITE_FRACTION;
            double w = PatchNoise.smoothstep(threshold - EDGE, threshold + EDGE, n);
            if (w > 0) {
                f.whiten(px, py, (float) w);
            }
        });
        return f;
    }

    /**
     * Where the ticking is, as four layers laid over each other: the tail dock,
     * the flank field, the belly spread and the rib bars.
     *
     * <p><b>The head and the lower legs get nothing.</b> Rabicano does not make
     * face white or stockings - those are other loci - and that is one of the
     * things that separates it from sabino and splash.
     */
    private static double regionWeight(Skin skin, Part part, BodyPoint point, double expression,
                                       double tailEmphasis, double sideBias, long seed) {
        switch (part) {
            case TAIL: {
                // The coon tail: frosting at the dock, fading down the hairs,
                // with pale transverse bands through it.
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double down = 1.0 - clamp01((point.y() - b.yMin()) / Math.max(1e-6, b.span(Axis.Y)));
                double dock = 1.0 - PatchNoise.smoothstep(TAIL_DOCK, 1.0, down);
                double band = 0.55 + 0.45 * Math.abs(Math.sin(point.y() * TAIL_BANDS));
                return clamp01(dock * band * tailEmphasis * (0.45 + 0.55 * expression));
            }
            case BODY: {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double fx = (point.x() - b.xMin()) / b.span(Axis.X);   // 0 rump .. 1 shoulder
                double fy = (point.y() - b.yMin()) / b.span(Axis.Y);   // 0 belly .. 1 topline
                double side = point.z() >= 0 ? 1.0 + sideBias : 1.0 - sideBias;

                // 1. the flank field, centred just ahead of the hip and above the
                //    groin, feathering outward
                double dx = (fx - FLANK_X) / FLANK_REACH;
                double dy = (fy - FLANK_Y) / (FLANK_REACH * 1.3);
                double flank = 1.0 - PatchNoise.smoothstep(0.0, 1.0, Math.sqrt(dx * dx + dy * dy));

                // 2. the belly, connecting visually to the flank rather than
                //    forming an edge of its own
                double belly = (1.0 - PatchNoise.smoothstep(0.0, BELLY_TOP, fy))
                        * (1.0 - PatchNoise.smoothstep(0.5, 1.0, fx));

                // 3. rib bars - broken vertical concentrations, strongest on the
                //    rear and lower barrel, and only once there is enough of a
                //    pattern for them to be part of
                double bars = 0;
                if (expression > 0.45) {
                    double phase = fx / BAR_SPACING;
                    int band = (int) Math.floor(phase);
                    double d = Math.abs((phase - band) - 0.5) * 2.0;
                    double core = 1.0 - PatchNoise.smoothstep(BAR_DUTY, BAR_DUTY + 0.30, d);
                    // Broken, not painted: a second noise field frozen to the bar
                    // index decides how much of each one is actually there.
                    double presence = BodyNoise.value(seed ^ 0x5AB1CA0DEADBEEFL, band * 3.0,
                            point.y() * 0.5, point.z() * 0.4);
                    double window = (1.0 - PatchNoise.smoothstep(BAR_REACH - 0.2, BAR_REACH, fx))
                            * (1.0 - PatchNoise.smoothstep(0.55, 0.95, fy));
                    bars = core * PatchNoise.smoothstep(0.35, 0.75, presence) * window
                            * (expression - 0.45) / 0.55;
                }

                double reach = 0.35 + 0.65 * expression;
                return clamp01(Math.max(Math.max(flank, belly), bars) * side * reach);
            }
            default:
                return 0;   // head, neck, mane, legs - other genes' business
        }
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
