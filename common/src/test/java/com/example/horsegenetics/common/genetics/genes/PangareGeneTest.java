package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Face;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pangare, and the medial-face helper it shares with the seal brown.
 *
 * <p>The claims worth pinning are the ones that are supposed to fall out of the
 * mechanism rather than out of a table: a bay's black points are spared, a black
 * horse shows nothing at all, and the <i>inside</i> of a leg goes pale while the
 * outside stays dark.
 */
class PangareGeneTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;
    private static final PangareGene PANGARE = Genes.PANGARE;

    private static Genotype bay(Allele a, Allele b) {
        return Genotype.wildType()
                .with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.a))
                .with(new AllelePair(a, b));
    }

    private static Genotype chestnut(Allele a, Allele b) {
        return Genotype.wildType()
                .with(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e))
                .with(new AllelePair(a, b));
    }

    // ------------------------------------------------------------------
    // The dosage
    // ------------------------------------------------------------------

    @Test
    void theDosageIsAdditiveAndTheBandsFollowIt() {
        assertEquals(0.0, PANGARE.dosage(new AllelePair(PANGARE.pa, PANGARE.pa)));
        assertEquals(2.0, PANGARE.dosage(new AllelePair(PANGARE.Pa1, PANGARE.Pa1)));
        assertEquals(2.0, PANGARE.dosage(new AllelePair(PANGARE.Pa2, PANGARE.pa)));
        assertEquals(PangareGene.MAX_DOSAGE, PANGARE.dosage(new AllelePair(PANGARE.Pa2, PANGARE.Pa2)));
        assertSame(bay(PANGARE.Pa2, PANGARE.pa).expressionOf(Genes.PANGARE),
                bay(PANGARE.Pa1, PANGARE.Pa1).expressionOf(Genes.PANGARE));
    }

    @Test
    void everyStepUpTheDosageIsADifferentOutcome() {
        assertTrue(bay(PANGARE.pa, PANGARE.pa).expressionOf(Genes.PANGARE).wildType());
        assertEquals("trace-pangare", bay(PANGARE.Pa1, PANGARE.pa).expressionOf(Genes.PANGARE).id());
        assertEquals("mild-pangare", bay(PANGARE.Pa1, PANGARE.Pa1).expressionOf(Genes.PANGARE).id());
        assertEquals("pangare", bay(PANGARE.Pa2, PANGARE.Pa1).expressionOf(Genes.PANGARE).id());
        assertEquals("strong-pangare", bay(PANGARE.Pa2, PANGARE.Pa2).expressionOf(Genes.PANGARE).id());
    }

    // ------------------------------------------------------------------
    // What falls out of working on red
    // ------------------------------------------------------------------

    /** The whole underside lightens and the topline does not - sooty, inverted. */
    @Test
    void theBellyLightensAndTheToplineDoesNot() {
        PigmentField clear = paint(bay(PANGARE.pa, PANGARE.pa), 5L);
        PigmentField mealy = paint(bay(PANGARE.Pa2, PANGARE.Pa2), 5L);

        float bellyClear = bandRed(clear, 0.08);
        float bellyMealy = bandRed(mealy, 0.08);
        float topClear = bandRed(clear, 0.88);
        float topMealy = bandRed(mealy, 0.88);

        assertTrue(bellyClear - bellyMealy > 0.3f,
                "the belly should lose most of its red, " + bellyClear + " -> " + bellyMealy);
        assertTrue(topClear - topMealy < 0.05f,
                "the topline should be nearly untouched, " + topClear + " -> " + topMealy);
    }

    /**
     * <b>A bay's black points are spared, and nothing in the gene says so.</b> A
     * point is stored as red 0 and black 1, so there is no red on it to take -
     * which is the reason pangare works on red rather than on lightness.
     */
    @Test
    void aBaysBlackPointsAreSparedByTheArithmetic() {
        PigmentField mealy = paint(bay(PANGARE.Pa2, PANGARE.Pa2), 5L);
        for (Part part : new Part[]{Part.MANE, Part.TAIL, Part.LEFT_EAR, Part.RIGHT_EAR}) {
            assertTrue(meanBlack(mealy, part) > 0.95f, part + " should still be a black point");
        }
    }

    /** And a black horse shows nothing: its red is fully masked, so removing it changes no colour. */
    @Test
    void aBlackHorseShowsNothing() {
        PigmentField clear = paint(Genotype.wildType(), 3L);
        PigmentField mealy = paint(Genotype.wildType()
                .with(new AllelePair(PANGARE.Pa2, PANGARE.Pa2)), 3L);
        // The stored red moves; what matters is that the black above it does not,
        // and at black = 1 the gradient's bottom row is one colour whatever the
        // red says.
        assertEquals(meanBlack(clear, Part.BODY), meanBlack(mealy, Part.BODY), 1e-6f);
        assertTrue(meanBlack(mealy, Part.BODY) > 0.99f, "a black horse is still fully black");
    }

    @Test
    void aChestnutIsWhereItShowsBrightest() {
        float bayLoss = bandRed(paint(bay(PANGARE.pa, PANGARE.pa), 5L), 0.08)
                - bandRed(paint(bay(PANGARE.Pa2, PANGARE.Pa2), 5L), 0.08);
        float chestnutLoss = bandRed(paint(chestnut(PANGARE.pa, PANGARE.pa), 5L), 0.08)
                - bandRed(paint(chestnut(PANGARE.Pa2, PANGARE.Pa2), 5L), 0.08);
        assertTrue(chestnutLoss >= bayLoss - 1e-6f,
                "a chestnut has the most red to lose, " + chestnutLoss + " vs " + bayLoss);
    }

    // ------------------------------------------------------------------
    // The medial helper
    // ------------------------------------------------------------------

    /**
     * <b>The inside of a leg is pale and the outside is not.</b> This is what
     * {@link CoatRegions#medial} was added for, and it is a per-leg mirror -
     * the medial face of a right-hand leg is the opposite face from the medial
     * face of a left-hand one - so it is worth checking on all four rather than
     * on one.
     */
    @Test
    void theInsideOfEveryLegIsPalerThanTheOutside() {
        PigmentField mealy = paint(chestnut(PANGARE.Pa2, PANGARE.Pa2), 5L);
        for (Part leg : CoatRegions.LEGS) {
            float inner = faceRed(mealy, leg, true);
            float outer = faceRed(mealy, leg, false);
            assertTrue(inner < outer - 0.1f,
                    leg + ": the inner face should be paler, " + inner + " vs " + outer);
        }
    }

    @Test
    void medialAnswersOneForTheInnerFaceAndZeroForTheOuter() {
        for (Part leg : CoatRegions.LEGS) {
            Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, leg);
            boolean rightSide = (b.zMin() + b.zMax()) * 0.5 > 0;
            Face inner = rightSide ? Face.LEFT : Face.RIGHT;
            Face outer = rightSide ? Face.RIGHT : Face.LEFT;
            assertEquals(1.0, CoatRegions.medial(Skin.ADULT, leg, inner), 1e-9, leg + " inner");
            assertEquals(0.0, CoatRegions.medial(Skin.ADULT, leg, outer), 1e-9, leg + " outer");
            assertEquals(0.5, CoatRegions.medial(Skin.ADULT, leg, Face.NOSE), 1e-9, leg + " front");
        }
        assertEquals(0.0, CoatRegions.medial(Skin.ADULT, Part.BODY, Face.LEFT), 1e-9,
                "only a leg has an inside");
    }

    // ------------------------------------------------------------------

    /** Extension, agouti, then pangare - the order they run in for real. */
    private static PigmentField paint(Genotype gt, long seed) {
        CoatBuildContext ctx = new CoatBuildContext(gt, Epigenome.fromSeed(seed), Skin.ADULT, true);
        PigmentField f = new PigmentField(N);
        for (Gene gene : new Gene[]{Genes.EXTENSION, Genes.AGOUTI, Genes.PANGARE}) {
            Expression e = gt.expressionOf(gene);
            if (!e.wildType()) {
                f = e.restrict(ctx, f);
            }
        }
        return f;
    }

    private static float bandRed(PigmentField f, double height) {
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, Part.BODY);
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, Part.BODY, (px, py, p, face, point) -> {
            double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
            if (Math.abs(fy - height) < 0.08) {
                acc[0] += f.red(px, py);
                acc[1]++;
            }
        });
        return (float) (acc[0] / Math.max(1, acc[1]));
    }

    /** Mean red on the inner or the outer face of one leg. */
    private static float faceRed(PigmentField f, Part leg, boolean inner) {
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, leg, (px, py, p, face, point) -> {
            double medial = CoatRegions.medial(Skin.ADULT, leg, face);
            if (inner ? medial < 0.99 : medial > 0.01) {
                return;
            }
            acc[0] += f.red(px, py);
            acc[1]++;
        });
        return (float) (acc[0] / Math.max(1, acc[1]));
    }

    private static float meanBlack(PigmentField f, Part part) {
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, part, (px, py, p, face, point) -> {
            acc[0] += f.black(px, py);
            acc[1]++;
        });
        return (float) (acc[0] / Math.max(1, acc[1]));
    }
}
