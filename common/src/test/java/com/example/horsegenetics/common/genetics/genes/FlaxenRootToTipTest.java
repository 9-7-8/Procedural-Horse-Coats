package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.HairPattern;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Flaxen's root-to-tip gradient, measured.</b> A real flaxen mane is darkest
 * where it leaves the crest and palest at the ends; the gene used to draw one
 * flat score per part, which is the one thing a uniform wash cannot look like
 * however well its strands are mixed.
 *
 * <p>Measured rather than looked at, for the reason the silver dapple test
 * gives: this is a few percent of red on a handful of mane texels, well below
 * what can be judged from a sheet viewed small. The metric here is direct
 * though, not statistical like roan's - the gradient is a function of position,
 * so surviving red near the root can be compared against surviving red near the
 * tip <i>on the same horse</i>.
 *
 * <p>The axis is deliberately the one {@code GreyCoat.hairRootLead} uses -
 * {@link HairPattern#axesBySpan}' second, whose middle is the root. This test
 * reads it the same way on purpose: if that shared convention is ever changed,
 * three genes and this test move together rather than drifting apart.
 */
class FlaxenRootToTipTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** A chestnut, because flaxen only shows on one, carrying two strong copies. */
    private static Genotype flaxenChestnut() {
        String[] segs = Genotype.wildType().toCode().split("-");
        for (int i = 0; i < segs.length; i++) {
            if (segs[i].startsWith(FlaxenGene.KEY + "=")) {
                segs[i] = FlaxenGene.KEY + "=Fl2/Fl2";
            } else if (segs[i].startsWith(Genes.EXTENSION.key() + "=")) {
                segs[i] = Genes.EXTENSION.key() + "=e/e";
            }
        }
        return Genotype.parse(String.join("-", segs));
    }

    private static CoatBuildContext ctx(long seed) {
        return new CoatBuildContext(flaxenChestnut(), Epigenome.fromSeed(seed), Skin.ADULT, true);
    }

    private static double rootDepth(CoatBuildContext c) {
        EpiValues epi = c.epigeneticsFor(FlaxenGene.KEY);
        return epi.get(FlaxenGene.ROOT_DEPTH);
    }

    private static PigmentField paint(CoatBuildContext c) {
        AllelePair pair = new AllelePair(Genes.FLAXEN.Fl2, Genes.FLAXEN.Fl2);
        PigmentField out = Genes.FLAXEN.expressionOf(pair).restrict(c, new PigmentField(N));
        assertTrue(out != null, "flaxen's expression must paint");
        return out;
    }

    /** 0 at the root of the hair, 1 at the tip - the shared convention. */
    private static double rootFraction(Part part, HorseSkinGeometry.BodyPoint point) {
        Axis across = HairPattern.axesBySpan(Skin.ADULT, part)[1];
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, part);
        double span = b.span(across);
        if (span <= 0) {
            return 1.0;
        }
        double centre = (b.min(across) + b.max(across)) * 0.5;
        return Math.abs(point.along(across) - centre) / span;
    }

    /** Mean surviving red over mane texels whose root fraction is inside a band. */
    private static double meanRed(PigmentField f, double from, double to) {
        double[] sum = {0};
        int[] n = {0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != Part.MANE) {
                return;
            }
            double r = rootFraction(part, point);
            if (r < from || r >= to) {
                return;
            }
            sum[0] += f.red(px, py);
            n[0]++;
        });
        return n[0] == 0 ? -1 : sum[0] / n[0];
    }

    /** The horse in 0..seeds with the deepest gradient. */
    private static CoatBuildContext deepest(int seeds) {
        CoatBuildContext best = null;
        double bestDepth = -1;
        for (long s = 0; s < seeds; s++) {
            CoatBuildContext c = ctx(s);
            if (rootDepth(c) > bestDepth) {
                bestDepth = rootDepth(c);
                best = c;
            }
        }
        return best;
    }

    /**
     * <b>The root keeps more red than the tip.</b> Flaxen removes red to
     * lighten, so a darker root is more surviving red near the middle of the
     * hair box than out at its edges.
     */
    @Test
    void theRootStaysDarkerThanTheTip() {
        CoatBuildContext c = deepest(64);
        PigmentField f = paint(c);

        double atRoot = meanRed(f, 0.0, 0.15);
        double atTip = meanRed(f, 0.40, 1.0);

        assertTrue(atRoot >= 0 && atTip >= 0,
                "no mane texels in one of the bands: root " + atRoot + ", tip " + atTip);
        assertTrue(atRoot > atTip,
                "the root is not darker than the tip (root red " + atRoot + " vs tip red " + atTip
                        + ") - the gradient is doing nothing, or it is inverted");
    }

    /**
     * <b>Depth varies between horses and reaches zero.</b> It is an epigenetic
     * value on the allele, so a field of flaxen chestnuts must not all carry the
     * same two-tone mane - and a horse at zero draws the flat mane this gene
     * drew before the gradient existed, which is a real coat and not a bug.
     */
    @Test
    void gradientDepthVariesBetweenHorses() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (long s = 0; s < 64; s++) {
            double d = rootDepth(ctx(s));
            min = Math.min(min, d);
            max = Math.max(max, d);
            assertTrue(d >= 0, "root depth must not go negative: " + d);
        }
        assertTrue(max - min > 0.2,
                "flaxen's root depth barely varies across 64 horses (" + min + ".." + max
                        + ") - the epigenetics are probably not being read per allele");
    }

    /** The gradient is long-hair only: it must not reach the body coat. */
    @Test
    void theBodyIsUntouched() {
        PigmentField f = paint(deepest(64));
        double[] worst = {1.0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part == Part.MANE || part == Part.TAIL) {
                return;
            }
            worst[0] = Math.min(worst[0], f.red(px, py));
        });
        assertTrue(worst[0] >= 1.0,
                "flaxen lightened something outside the mane and tail (lowest red " + worst[0] + ")");
    }
}
