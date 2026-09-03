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
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Roan</b> ({@code horsegenetics.roan}) - {@code Rn} dominant, {@code rn}
 * wild-type. Natural, <b>non-deterministic</b>.
 *
 * <p>White hairs mixed evenly through the <b>body coat</b> while the head, mane,
 * tail and lower legs stay solid. It is <b>not</b> a hard on/off dither: real
 * roan is an intimate mix of white and coloured hairs, so every body texel is
 * whitened by a <i>fraction</i> - a smooth base amount plus a soft mottle from
 * a two-octave {@link PatchNoise#fbm2 noise} field - which reads as roan rather
 * than as aliased white pixels.
 *
 * <p>The intensity <b>falls off from back to front</b>: strongest over the
 * hindquarters and barrel, tapering across the neck and gone by the head, so
 * there is no hard line where the roan meets the solid face. On the legs it
 * fades to nothing toward the hoof.
 *
 * <p>Two knobs off the expressing {@code Rn} copy: {@code nextLong()} (the
 * field's seed) then {@code nextFloat()} for how dense the white is. A foal
 * that inherits the copy inherits the exact roaning.
 */
public final class RoanGene implements Gene {

    public static final String KEY = "horsegenetics.roan";
    public static final int WILD_ROAN_ALLELE_ODDS = 30;

    /** Body-space frequency of the hair-by-hair mottle. */
    private static final double FREQ = 2.6;
    /** Fraction of body texels that become white hairs at full intensity. */
    private static final double WHITE_FRACTION = 0.42;
    /** Extra white fraction from the epigenetic roll. */
    private static final double DENSITY_RANGE = 0.20;
    /** Soft edge on the per-texel white/coloured decision, in noise units - keeps ~1px feathering. */
    private static final double EDGE = 0.05;

    public final Allele Rn = new Allele(KEY, "Rn", "Roan (Rn)", true, false);
    public final Allele rn = new Allele(KEY, "rn", "Wild-type (rn)", false, true);
    private final List<Allele> alleles = List.of(Rn, rn);

    @Override public String key() { return KEY; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return rn; }

    /** Dominant: one {@code Rn} roans the horse. */
    @Override public DominancePattern dominance() { return DominancePattern.DOMINANT; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_ROAN_ALLELE_ODDS) == 0 ? Rn : rn,
                rng.nextInt(WILD_ROAN_ALLELE_ODDS) == 0 ? Rn : rn);
    }

    public boolean isRoan(AllelePair pair) {
        return pair.has(Rn);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isRoan(pair);
    }

    @Override
    public boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return !isRoan(pair);
    }

    @Override
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!isRoan(pair)) {
            return null;
        }
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double density = epi.nextFloat() * DENSITY_RANGE;

        Skin skin = ctx.skin();
        // Front/back falloff: densest at the hindquarters, tapering forward and
        // feathering out across the neck so the roan never ends on a hard line
        // against the solid face. Measured from mid-barrel to the front of the neck.
        Bounds body = HorseSkinGeometry.bounds(skin, Part.BODY);
        Bounds neck = HorseSkinGeometry.bounds(skin, Part.NECK);
        double frontStart = body.xMin() + body.span(Axis.X) * 0.45;
        double frontEnd = neck.xMax();

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            double region = regionWeight(skin, part, point, frontStart, frontEnd);
            if (region <= 0) {
                return;
            }
            double n = PatchNoise.fbm2(seed, point.x() * FREQ, point.y() * FREQ, point.z() * FREQ * 1.6);
            // a texel is a white hair or a coloured hair; the *fraction* that are
            // white varies smoothly with the front/back region weight. Near-binary
            // with a narrow soft edge, so the flecks read crisp but not aliased -
            // and never through the gradient's warm mid-tones (a half-scaled black
            // texel is orange, not grey).
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

    /** 1 over the hindquarters + barrel, tapering across the neck, 0 on head / mane / tail / lower legs. */
    private static double regionWeight(Skin skin, Part part, HorseSkinGeometry.BodyPoint point,
                                       double frontStart, double frontEnd) {
        switch (part) {
            case BODY, NECK -> {
                return 1.0 - PatchNoise.smoothstep(frontStart, frontEnd, point.x());
            }
            case LEFT_FRONT_LEG, RIGHT_FRONT_LEG, LEFT_HIND_LEG, RIGHT_HIND_LEG -> {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double frac = (point.y() - b.yMin()) / b.span(Axis.Y);
                return PatchNoise.smoothstep(0.25, 0.6, frac);   // 0 at the hoof, 1 by mid-leg
            }
            default -> {
                return 0.0;
            }
        }
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
