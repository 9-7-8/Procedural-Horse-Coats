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
 * <b>Tobiano</b> ({@code horsegenetics.tobiano}) - {@code To} dominant,
 * {@code to} wild-type. Natural, <b>non-deterministic</b>.
 *
 * <p>Large, smooth-edged white patches that <b>cross the topline</b> - the shape
 * that separates tobiano from <a href="">frame overo</a>. The patch field is a
 * single warped fractal noise ({@link PatchNoise#field}) sampled in body space
 * over the whole trunk, neck, mane, tail <i>and legs at once</i>, so a patch
 * flows unbroken from the barrel down a leg and over the back without a seam.
 * A bias toward the spine lifts the field where it is high on the body, so the
 * patches reliably run up and over the topline. The head stays coloured.
 *
 * <p>Three knobs off the expressing {@code To} copy: {@code nextLong()} (the
 * patch field's seed), {@code nextFloat()} for how much white, {@code nextFloat()}
 * for the patch size.
 */
public final class TobianoGene implements Gene {

    public static final String KEY = "horsegenetics.tobiano";
    public static final int WILD_TOBIANO_ALLELE_ODDS = 50;

    private static final double SCALE_MIN = 0.15;   // body-space frequency of the patch field
    private static final double SCALE_RANGE = 0.07;
    private static final double COVER_MIN = 0.40;   // white where field+bias exceeds 1 - cover
    private static final double COVER_RANGE = 0.16;
    /** How hard the field is lifted toward the spine so patches cross the back. */
    private static final double TOPLINE_BIAS = 0.18;
    /** Tobiano tends to white legs: an extra lift low on each leg. */
    private static final double LEG_LIFT = 0.20;

    public final Allele To = new Allele(KEY, "To", "Tobiano (To)", true, false);
    public final Allele to = new Allele(KEY, "to", "Wild-type (to)", false, true);
    private final List<Allele> alleles = List.of(To, to);

    @Override public String key() { return KEY; }
    @Override public int priority() { return 72; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return to; }

    /** Dominant: one {@code To} gives the pattern. */
    @Override public DominancePattern dominance() { return DominancePattern.DOMINANT; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_TOBIANO_ALLELE_ODDS) == 0 ? To : to,
                rng.nextInt(WILD_TOBIANO_ALLELE_ODDS) == 0 ? To : to);
    }

    public boolean isTobiano(AllelePair pair) {
        return pair.has(To);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isTobiano(pair);
    }

    @Override
    public boolean isDeterministic(AllelePair pair, Genotype genotype) {
        return !isTobiano(pair);
    }

    @Override
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!isTobiano(pair)) {
            return null;
        }
        Rng epi = ctx.epigeneticsFor(KEY);
        long seed = epi.nextLong();
        double cover = COVER_MIN + epi.nextFloat() * COVER_RANGE;
        double scale = SCALE_MIN + epi.nextFloat() * SCALE_RANGE;

        Skin skin = ctx.skin();
        Bounds bb = HorseSkinGeometry.bodyBounds(skin);
        double topY = bb.yMax();
        double midY = bb.yMin() + bb.span(Axis.Y) * 0.42;
        double threshold = 1.0 - cover;   // crisp binary: tobiano edges are sharp, and a
                                          // half-scaled black texel reads gold, not grey.

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (part == Part.HEAD || part == Part.MUZZLE || part == Part.LEFT_EAR || part == Part.RIGHT_EAR) {
                return; // the head stays coloured
            }
            double v = PatchNoise.field(seed, point.x(), point.y(), point.z(), scale);
            v += TOPLINE_BIAS * clamp01((point.y() - midY) / Math.max(1e-4, topY - midY));
            if (isLeg(part)) {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double frac = (point.y() - b.yMin()) / b.span(Axis.Y);
                v += LEG_LIFT * (1.0 - PatchNoise.smoothstep(0.35, 0.8, frac));
            }
            if (v <= threshold) {
                return;
            }
            f.setRed(px, py, 0f);
            f.setBlack(px, py, 0f);
        });
        return f;
    }

    private static boolean isLeg(Part part) {
        return part == Part.LEFT_FRONT_LEG || part == Part.RIGHT_FRONT_LEG
                || part == Part.LEFT_HIND_LEG || part == Part.RIGHT_HIND_LEG;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
