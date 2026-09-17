package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Silver's dapples, measured rather than looked at.</b>
 *
 * <p>The dapple is a modulation of how much black a body texel keeps, at a
 * depth of {@value SilverGene#DAPPLE_DEPTH}. That is a few percent of
 * brightness - visible on a horse, and <i>below</i> what can honestly be judged
 * from a 128-texel sheet viewed small. The render that went with this gene
 * confirmed the dilution and the paler mane and could not confirm the dappling
 * either way, which is the whole reason this test exists: "I could not see it"
 * is not evidence that it is absent, and would not have been evidence that it
 * was present.
 *
 * <p>So these measure the field instead. What they are really guarding is the
 * failure this design invites: the seed, spacing and strength live in the
 * <i>allele's own epigenetics</i>, so the way to break it is to read them from
 * the wrong place and give every silver horse an identical coat. A render of
 * one horse cannot show that. {@link #dappleStrengthVariesBetweenHorses} can.
 */
class SilverDappleTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** A wild-type horse carrying one silver copy. */
    private static Genotype silver(String pair) {
        String[] segs = Genotype.wildType().toCode().split("-");
        for (int i = 0; i < segs.length; i++) {
            if (segs[i].startsWith(SilverGene.KEY + "=")) {
                segs[i] = SilverGene.KEY + "=" + pair;
            }
        }
        return Genotype.parse(String.join("-", segs));
    }

    private static CoatBuildContext ctx(long seed) {
        return new CoatBuildContext(silver("Z/z"), Epigenome.fromSeed(seed), Skin.ADULT, true);
    }

    /** Paint one silver horse and hand back its field. */
    private static PigmentField paint(CoatBuildContext ctx) {
        AllelePair pair = new AllelePair(Genes.SILVER.Z, Genes.SILVER.z);
        PigmentField out = Genes.SILVER.expressionOf(pair).restrict(ctx, new PigmentField(N));
        assertTrue(out != null, "silver's expression must paint");
        return out;
    }

    private static boolean isHair(Part part) {
        return part == Part.MANE || part == Part.TAIL
                || part == Part.LEFT_EAR || part == Part.RIGHT_EAR;
    }

    /** Spread of surviving black over the body (or the long hair), max minus min. */
    private static float spread(PigmentField f, boolean hair) {
        float[] lo = {Float.MAX_VALUE};
        float[] hi = {-Float.MAX_VALUE};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (isHair(part) != hair) {
                return;
            }
            float b = f.black(px, py);
            lo[0] = Math.min(lo[0], b);
            hi[0] = Math.max(hi[0], b);
        });
        return hi[0] < lo[0] ? 0f : hi[0] - lo[0];
    }

    private static double strengthOf(CoatBuildContext c) {
        EpiValues epi = c.epigeneticsFor(SilverGene.KEY);
        return epi.get(SilverGene.DAPPLE_STRENGTH);
    }

    /**
     * <b>The one a render cannot do.</b> Dapple strength is an epigenetic value
     * on the allele, so a paddock of silvers must not all dapple alike - and a
     * gene that read it from a fixed constant, or from the genotype, would look
     * perfectly correct in any single picture.
     */
    @Test
    void dappleStrengthVariesBetweenHorses() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (long seed = 0; seed < 64; seed++) {
            double s = strengthOf(ctx(seed));
            min = Math.min(min, s);
            max = Math.max(max, s);
            assertTrue(s >= 0 && s <= 1, "strength out of range at seed " + seed + ": " + s);
        }
        assertTrue(max - min > 0.5,
                "silver dapple strength barely varies across 64 horses (" + min + ".." + max
                        + ") - the epigenetics are probably not being read per allele");
    }

    /**
     * A strongly dappled horse's body really is modulated, and a barely dappled
     * one really is not. Both ends matter: the first proves the lattice does
     * something, the second proves strength is what controls it rather than the
     * dapple being painted at a fixed depth regardless.
     */
    @Test
    void strongDapplingModulatesTheBodyAndWeakDapplingDoesNot() {
        CoatBuildContext strongest = null;
        CoatBuildContext weakest = null;
        double best = -1;
        double worst = 2;
        for (long seed = 0; seed < 64; seed++) {
            CoatBuildContext c = ctx(seed);
            double s = strengthOf(c);
            if (s > best) {
                best = s;
                strongest = c;
            }
            if (s < worst) {
                worst = s;
                weakest = c;
            }
        }

        float strongSpread = spread(paint(strongest), false);
        float weakSpread = spread(paint(weakest), false);

        assertTrue(strongSpread > 0.02f,
                "a strongly dappled silver (strength " + best + ") barely modulates its body: spread "
                        + strongSpread + " - the dapple lattice is doing nothing");
        assertTrue(strongSpread > weakSpread * 3f,
                "dapple depth does not track strength: strong " + strongSpread
                        + " vs weak " + weakSpread);
    }

    /**
     * The dapple is a body treatment. The mane and tail take silver's flaxen
     * path, which is a flat dilution - so their black must come out uniform
     * however hard the body is dappling.
     */
    @Test
    void theManeAndTailAreNotDappled() {
        CoatBuildContext strongest = null;
        double best = -1;
        for (long seed = 0; seed < 64; seed++) {
            CoatBuildContext c = ctx(seed);
            if (strengthOf(c) > best) {
                best = strengthOf(c);
                strongest = c;
            }
        }
        assertEquals(0f, spread(paint(strongest), true), 1e-6f,
                "the long hair is flat-diluted, so it must not vary texel to texel");
    }
}
