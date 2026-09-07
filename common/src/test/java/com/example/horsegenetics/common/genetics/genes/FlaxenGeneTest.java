package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flaxen: that it is a dosage rather than a switch, that it touches the long
 * hair and nothing else, and that it is completely invisible on any horse with
 * black hair while still being carried by it.
 */
class FlaxenGeneTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;
    private static final FlaxenGene FLAXEN = Genes.FLAXEN;

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
        assertEquals(0.0, FLAXEN.dosage(new AllelePair(FLAXEN.f, FLAXEN.f)));
        assertEquals(1.0, FLAXEN.dosage(new AllelePair(FLAXEN.Fl1, FLAXEN.f)));
        assertEquals(2.0, FLAXEN.dosage(new AllelePair(FLAXEN.Fl1, FLAXEN.Fl1)));
        assertEquals(2.0, FLAXEN.dosage(new AllelePair(FLAXEN.Fl2, FLAXEN.f)));
        assertEquals(3.0, FLAXEN.dosage(new AllelePair(FLAXEN.Fl2, FLAXEN.Fl1)));
        assertEquals(FlaxenGene.MAX_DOSAGE, FLAXEN.dosage(new AllelePair(FLAXEN.Fl2, FLAXEN.Fl2)));

        // One strong copy and two mild ones are the same horse. That is what
        // "additive dosage" means, and it is the whole reason for three alleles
        // rather than the two the folklore model uses.
        assertSame(chestnut(FLAXEN.Fl2, FLAXEN.f).expressionOf(Genes.FLAXEN),
                chestnut(FLAXEN.Fl1, FLAXEN.Fl1).expressionOf(Genes.FLAXEN));
    }

    @Test
    void everyStepUpTheDosageIsADifferentOutcome() {
        assertTrue(chestnut(FLAXEN.f, FLAXEN.f).expressionOf(Genes.FLAXEN).wildType());
        assertEquals("mild-flaxen", chestnut(FLAXEN.Fl1, FLAXEN.f).expressionOf(Genes.FLAXEN).id());
        assertEquals("flaxen", chestnut(FLAXEN.Fl1, FLAXEN.Fl1).expressionOf(Genes.FLAXEN).id());
        assertEquals("strong-flaxen", chestnut(FLAXEN.Fl2, FLAXEN.Fl1).expressionOf(Genes.FLAXEN).id());
        assertEquals("extreme-flaxen", chestnut(FLAXEN.Fl2, FLAXEN.Fl2).expressionOf(Genes.FLAXEN).id());
    }

    /** The roll varies the look; it must not carry a horse out of its own band. */
    @Test
    void theExpressionRollIsNarrowerThanABand() {
        assertTrue(FlaxenGene.EXPRESSION_RANGE * 2 < 1.0);
    }

    // ------------------------------------------------------------------
    // Chestnut only
    // ------------------------------------------------------------------

    @Test
    void itIsInvisibleOnAnyHorseThatMakesBlackHair() {
        Genotype bay = Genotype.wildType()
                .with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.a))
                .with(new AllelePair(FLAXEN.Fl2, FLAXEN.Fl2));
        Genotype black = Genotype.wildType().with(new AllelePair(FLAXEN.Fl2, FLAXEN.Fl2));

        assertTrue(bay.expressionOf(Genes.FLAXEN).wildType(), "a bay carries it silently");
        assertTrue(black.expressionOf(Genes.FLAXEN).wildType(), "a black carries it silently");
        // ...but it is still there, and still inherited
        assertEquals(FlaxenGene.MAX_DOSAGE, FLAXEN.dosage(bay.pair(Genes.FLAXEN)));
    }

    /** And so it is out of a non-chestnut's texture key: two bays share one bake. */
    @Test
    void aBaysFlaxenDosageDoesNotSplitTheTextureCache() {
        Genotype plain = Genotype.wildType().with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.a));
        Genotype carrier = plain.with(new AllelePair(FLAXEN.Fl2, FLAXEN.Fl2));
        assertEquals(plain.coatCode(), carrier.coatCode());
        assertNotEquals(chestnut(FLAXEN.f, FLAXEN.f).coatCode(),
                chestnut(FLAXEN.Fl2, FLAXEN.Fl2).coatCode(),
                "on a chestnut it must split them");
    }

    // ------------------------------------------------------------------
    // What it paints
    // ------------------------------------------------------------------

    @Test
    void aHigherDosageMeansPalerLongHairAndAnUntouchedBody() {
        float plainMane = redOf(chestnut(FLAXEN.f, FLAXEN.f), Part.MANE);
        float midMane = redOf(chestnut(FLAXEN.Fl1, FLAXEN.Fl1), Part.MANE);
        float paleMane = redOf(chestnut(FLAXEN.Fl2, FLAXEN.Fl2), Part.MANE);
        assertTrue(plainMane > midMane, "one dose should already lighten the mane");
        assertTrue(midMane > paleMane, "two strong copies should lighten it further");
        assertTrue(paleMane < 0.45f, "an extreme flaxen mane should be nearly white");

        for (Part part : new Part[]{Part.BODY, Part.NECK, Part.HEAD, Part.LEFT_FRONT_LEG}) {
            assertEquals(redOf(chestnut(FLAXEN.f, FLAXEN.f), part),
                    redOf(chestnut(FLAXEN.Fl2, FLAXEN.Fl2), part), 1e-6f,
                    part + " must not be touched - flaxen is a long-hair modifier");
        }
    }

    @Test
    void theManeAndTheTailAreRolledSeparately() {
        boolean sawADifference = false;
        for (long seed = 0; seed < 40 && !sawADifference; seed++) {
            PigmentField f = paint(chestnut(FLAXEN.Fl1, FLAXEN.Fl1), seed);
            sawADifference = Math.abs(mean(f, Part.MANE) - mean(f, Part.TAIL)) > 0.02f;
        }
        assertTrue(sawADifference, "the mane and tail should not always land on the same shade");
    }

    // ------------------------------------------------------------------

    private static PigmentField paint(Genotype gt, long seed) {
        CoatBuildContext ctx = new CoatBuildContext(gt, Epigenome.fromSeed(seed), Skin.ADULT, true);
        PigmentField f = new PigmentField(N);
        // Extension first - flaxen only ever runs on a coat with no black left.
        f = Genes.EXTENSION.expressionIn(gt.pair(Genes.EXTENSION), gt).restrict(ctx, f);
        Expression flax = gt.expressionOf(Genes.FLAXEN);
        return flax.wildType() ? f : flax.restrict(ctx, f);
    }

    private static float redOf(Genotype gt, Part part) {
        return mean(paint(gt, 7L), part);
    }

    private static float mean(PigmentField f, Part part) {
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, part, (px, py, p, face, point) -> {
            acc[0] += f.red(px, py);
            acc[1]++;
        });
        return (float) (acc[0] / Math.max(1, acc[1]));
    }
}
