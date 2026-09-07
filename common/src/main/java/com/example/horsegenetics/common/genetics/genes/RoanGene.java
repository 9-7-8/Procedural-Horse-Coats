package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.BodyNoise;
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
 * <b>Roan</b> ({@code horsegenetics.roan}) - {@code Rn} dominant, {@code rn}
 * wild-type. Natural, <b>non-deterministic</b>. This is <i>classic</i> or
 * <i>true</i> roan; the leopard complex's varnish roan
 * ({@link LeopardGene}) shares the word and almost nothing else.
 *
 * <p>An even <b>salt-and-pepper</b> intermixture of white hairs through the
 * body coat, over a base colour the gene does not touch: black plus white hairs
 * is a blue roan, chestnut plus white a strawberry, bay plus white a bay roan.
 * It is <b>not</b> a hard on/off dither and not a white overlay - a texel is a
 * white hair or a coloured one, and what the gene sets is the <i>fraction</i>
 * that are white, near-binary with a one-pixel soft edge so the mix reads as
 * hair rather than as aliased pixels.
 *
 * <h2>The shape is what identifies it</h2>
 * Classic roan is recognised by its <b>distribution</b>, and as much by what it
 * spares as by what it covers:
 * <ul>
 *   <li><b>The whole trunk, evenly.</b> Barrel, shoulder, back, flank, belly
 *       and hip all at one strength. There is no front-to-back gradient and in
 *       particular <b>no dark shoulder shield</b> - a solid dark island there is
 *       a varnish trait, and drawing one is how this gene used to fail.</li>
 *   <li><b>The neck, thinning toward the poll</b>, so the roaning blends into
 *       the shoulder at one end and fades out at the other.</li>
 *   <li><b>A dark head.</b> With the roaned neck this is the "dark mask" that
 *       identifies classic roan across a field. Some individuals carry a little
 *       white on the forehead, which is rolled and is zero most of the time.</li>
 *   <li><b>Dark lower legs ending in a point</b> - the <b>inverted V</b>, roan's
 *       most characteristic edge. See {@link #chevronWeight}.</li>
 *   <li><b>A solid mane and tail.</b></li>
 * </ul>
 *
 * <p>Three knobs off the expressing {@code Rn} copy, in this order:
 * {@code nextLong()} (the field's seed), {@code nextFloat()} for how dense the
 * white is, and {@code nextFloat()} for the forehead. A foal that inherits the
 * copy inherits the exact roaning.
 */
public final class RoanGene implements Gene {

    public static final String KEY = "horsegenetics.roan";
    public static final int WILD_ROAN_ONE_IN = 30;

    /**
     * Body-space frequency of the hair mix. About one lattice cell per texel, so
     * neighbouring texels are near-independent and the result reads as
     * <b>individual white hairs</b> rather than as flecks.
     */
    private static final double FREQ = 2.6;
    /**
     * Fraction of body texels that become white hairs at the light end. Tuned so
     * a blue roan reads <b>slate</b> - a dark horse with white through it - and
     * not as a grey one: the pattern is a mixture, and a mixture that reaches
     * three quarters white has stopped being one.
     */
    private static final double WHITE_FRACTION = 0.28;
    /** Extra white fraction from the epigenetic roll - "the amount can vary". */
    private static final double DENSITY_RANGE = 0.22;
    /** Soft edge on the per-texel white/coloured decision, in noise units - keeps ~1px feathering. */
    private static final double EDGE = 0.05;

    /**
     * How much of its full strength the roaning keeps at the <b>top and front of
     * the neck</b>. The neck is roaned, often substantially, and most so low and
     * mid; it thins toward the poll so the pattern never ends on a hard line
     * against the solid head.
     */
    private static final double NECK_TAPER = 0.34;

    /**
     * The <b>inverted-V</b>, which is classic roan's most characteristic edge:
     * the dark lower leg rises into the roaned limb in a point rather than on a
     * level line. {@value #CHEVRON_EDGE_HEIGHT} is where the dark stops at the
     * side of the leg and {@value #CHEVRON_PEAK_HEIGHT} where it stops at the
     * leg's centre, both as fractions of the leg's height; the difference is the
     * chevron. A horizontal band here was the one thing about roan that looked
     * drawn rather than grown.
     */
    private static final double CHEVRON_EDGE_HEIGHT = 0.26;
    private static final double CHEVRON_PEAK_HEIGHT = 0.62;
    private static final double CHEVRON_SOFT = 0.09;

    /**
     * How much white the forehead may pick up. Usually none - a classic roan's
     * head is the diagnostic dark mask - but some individuals carry a modest
     * amount of white hair on the forehead and above the eyes, so it is rolled
     * and is zero most of the time.
     */
    private static final double FOREHEAD_CHANCE = 0.35;
    private static final double FOREHEAD_MAX = 0.30;

    public final Allele Rn = new Allele(KEY, 0, "Rn", "Roan (Rn)");
    public final Allele rn = new Allele(KEY, 1, "rn", "Wild-type (rn)");
    private final List<Allele> alleles = List.of(Rn, rn);

    private final Expression WILD = Expression.wildType("No white hairs mixed in.");

    private final Expression ROAN = Expression.of("roan", "Roan")
            .describe("An even salt-and-pepper of white hairs through the whole trunk - barrel, "
                    + "shoulder, back, flank and hip alike - over a base colour it does not change: "
                    + "blue roan on a black, strawberry on a chestnut. The head stays dark, giving "
                    + "the mask classic roan is known by, the mane and tail stay solid, and the "
                    + "dark lower leg rises into the roaning in a point.")
            .varies()
            .restrict(RoanGene::paintRoan);

    private final List<Expression> expressions = List.of(WILD, ROAN);

    private final FounderTable founders = FounderTable.hardyWeinberg(Rn, rn, 1.0 / WILD_ROAN_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Roan"; }
    @Override public int priority() { return 70; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return rn; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(Rn) ? ROAN : WILD;
    }

    public boolean isRoan(AllelePair pair) {
        return pair.has(Rn);
    }

    private static PigmentField paintRoan(CoatBuildContext ctx, PigmentView coat) {
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double density = epi.nextFloat() * DENSITY_RANGE;
        float foreheadRoll = epi.nextFloat();
        double forehead = foreheadRoll < FOREHEAD_CHANCE
                ? FOREHEAD_MAX * (1.0 - foreheadRoll / FOREHEAD_CHANCE) : 0.0;

        Skin skin = ctx.skin();
        Bounds neck = HorseSkinGeometry.bounds(skin, Part.NECK);
        Bounds head = HorseSkinGeometry.hasPart(skin, Part.HEAD)
                ? HorseSkinGeometry.bounds(skin, Part.HEAD) : null;

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double region = regionWeight(skin, part, face, point, neck, head, forehead);
            if (region <= 0) {
                return;
            }
            double n = BodyNoise.value(seed, point.x() * FREQ, point.y() * FREQ, point.z() * FREQ * 1.6);
            // A texel is a white hair or a coloured hair; the *fraction* that are
            // white is what the region weight sets. Near-binary with a narrow soft
            // edge, so the flecks read crisp but not aliased - and never through
            // the gradient's warm mid-tones (a half-scaled black texel is orange,
            // not grey).
            double threshold = 1.0 - region * (WHITE_FRACTION + density);
            double w = PatchNoise.smoothstep(threshold - EDGE, threshold + EDGE, n);
            if (w <= 0) {
                return;
            }
            // on the soft edge, pull red down faster than black so the 1px
            // transition greys out instead of going gold.
            f.setRed(px, py, f.red(px, py) * (float) ((1.0 - w) * (1.0 - 0.6 * w)));
            f.setBlack(px, py, f.black(px, py) * (float) (1.0 - w));
        });
        return f;
    }

    /**
     * <b>Where classic roan puts its white hairs</b>, and it is as much about the
     * exclusions as about the coverage.
     *
     * <ul>
     *   <li><b>The whole trunk, evenly</b> - barrel, shoulder, back, flank, belly
     *       and hip all at full strength. The hallmark of classic roan is a broad,
     *       relatively even mixture over the body; in particular there is no dark
     *       <i>shoulder shield</i>, which is a varnish-roan trait and was what
     *       this gene's old front-to-back gradient was accidentally drawing.</li>
     *   <li><b>The neck, thinning toward the poll.</b> Roaned, often
     *       substantially, and most so low and mid - it has to blend into the
     *       shoulder at one end and fade out at the other, because the head is
     *       dark and a hard line there would read as a marking.</li>
     *   <li><b>A dark head</b>, which with the roaned neck is the "dark mask"
     *       effect classic roan is identified by. Some individuals carry a little
     *       white on the forehead, so a rolled amount is allowed there and is
     *       zero on most horses.</li>
     *   <li><b>Dark lower legs, ending in a point</b> - see
     *       {@link #chevronWeight}.</li>
     *   <li><b>Nothing on the mane and tail.</b> They keep the base colour; an
     *       extensively whitening mane is not the classic-roan template.</li>
     * </ul>
     */
    private static double regionWeight(Skin skin, Part part, HorseSkinGeometry.Face face,
                                       HorseSkinGeometry.BodyPoint point,
                                       Bounds neck, Bounds head, double forehead) {
        switch (part) {
            case BODY -> {
                return 1.0;
            }
            case NECK -> {
                // Full at the base, thinning toward the poll. Measured along the
                // neck's own length so it does not depend on the barrel.
                double up = clamp01((point.y() - neck.yMin()) / Math.max(1e-6, neck.span(Axis.Y)));
                return 1.0 - (1.0 - NECK_TAPER) * PatchNoise.smoothstep(0.35, 1.0, up);
            }
            case HEAD -> {
                if (forehead <= 0 || head == null) {
                    return 0.0;
                }
                // The forehead only: the upper, poll-ward end of the head. Larger
                // x is toward the nose, so this is the low-x, high-y corner.
                double back = 1.0 - PatchNoise.smoothstep(0.15, 0.55,
                        clamp01((point.x() - head.xMin()) / Math.max(1e-6, head.span(Axis.X))));
                double up = PatchNoise.smoothstep(0.35, 0.85,
                        clamp01((point.y() - head.yMin()) / Math.max(1e-6, head.span(Axis.Y))));
                return forehead * back * up;
            }
            case LEFT_FRONT_LEG, RIGHT_FRONT_LEG, LEFT_HIND_LEG, RIGHT_HIND_LEG -> {
                return chevronWeight(skin, part, face, point);
            }
            default -> {
                return 0.0;
            }
        }
    }

    /**
     * <b>The inverted V.</b> Roaning covers the upper leg and stops above the
     * knee or hock, and the boundary is not level: the dark rises into the
     * roaned limb in a point, highest along the leg's own axis and lowest at its
     * edges. It is the most characteristic edge classic roan has, and it is what
     * a photograph is read for when the body alone is ambiguous.
     *
     * <p><b>Measured across the face being painted, not from the leg's axis.</b>
     * A leg is a box, so every texel sits on one of its flat sides and is
     * therefore <i>always</i> at the surface - a cone measured from the axis
     * comes out at its outer radius everywhere and draws a level line, which is
     * the bug this replaces. {@link HorseSkinGeometry.Face#spanA()} is the
     * horizontal axis of whichever side you are looking at, so the point rises
     * in the middle of each face and the V reads from any angle.
     *
     * <p>The boundary height runs from {@value #CHEVRON_EDGE_HEIGHT} at the
     * edges of a face to {@value #CHEVRON_PEAK_HEIGHT} at its middle, and the
     * roan fades in across {@value #CHEVRON_SOFT} either side of it.
     */
    private static double chevronWeight(Skin skin, Part leg, HorseSkinGeometry.Face face,
                                        HorseSkinGeometry.BodyPoint point) {
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double up = (point.y() - b.yMin()) / Math.max(1e-6, b.span(Axis.Y));
        Axis across = face.spanA();
        double lo = across == Axis.X ? b.xMin() : b.zMin();
        double hi = across == Axis.X ? b.xMax() : b.zMax();
        double v = across == Axis.X ? point.x() : point.z();
        double t = clamp01((v - lo) / Math.max(1e-6, hi - lo));
        double fromMiddle = Math.abs(2.0 * t - 1.0);   // 0 in the middle of the face, 1 at its edges
        double boundary = CHEVRON_PEAK_HEIGHT - (CHEVRON_PEAK_HEIGHT - CHEVRON_EDGE_HEIGHT) * fromMiddle;
        return PatchNoise.smoothstep(boundary - CHEVRON_SOFT, boundary + CHEVRON_SOFT, up);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
