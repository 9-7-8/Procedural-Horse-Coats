package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
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
 * Sooty: that it is a dosage, that it darkens the topline and spares the belly,
 * and - the claim the whole design rests on - that it never puts more pigment on
 * a texel than that texel started the pipeline with.
 */
class SootyGeneTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;
    private static final SootyGene SOOTY = Genes.SOOTY;

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
        assertEquals(0.0, SOOTY.dosage(new AllelePair(SOOTY.s, SOOTY.s)));
        assertEquals(1.0, SOOTY.dosage(new AllelePair(SOOTY.S1, SOOTY.s)));
        assertEquals(2.0, SOOTY.dosage(new AllelePair(SOOTY.S1, SOOTY.S1)));
        assertEquals(2.0, SOOTY.dosage(new AllelePair(SOOTY.S2, SOOTY.s)));
        assertEquals(SootyGene.MAX_DOSAGE, SOOTY.dosage(new AllelePair(SOOTY.S2, SOOTY.S2)));

        assertSame(bay(SOOTY.S2, SOOTY.s).expressionOf(Genes.SOOTY),
                bay(SOOTY.S1, SOOTY.S1).expressionOf(Genes.SOOTY));
    }

    @Test
    void everyStepUpTheDosageIsADifferentOutcome() {
        assertTrue(bay(SOOTY.s, SOOTY.s).expressionOf(Genes.SOOTY).wildType());
        assertEquals("faint-sooty", bay(SOOTY.S1, SOOTY.s).expressionOf(Genes.SOOTY).id());
        assertEquals("sooty", bay(SOOTY.S1, SOOTY.S1).expressionOf(Genes.SOOTY).id());
        assertEquals("heavy-sooty", bay(SOOTY.S2, SOOTY.S1).expressionOf(Genes.SOOTY).id());
        assertEquals("extreme-sooty", bay(SOOTY.S2, SOOTY.S2).expressionOf(Genes.SOOTY).id());
    }

    // ------------------------------------------------------------------
    // The mechanism
    // ------------------------------------------------------------------

    /**
     * <b>The claim the design rests on.</b> Sooty walks black back up, which is
     * the one thing phase 1 is not supposed to do - so it must never exceed the
     * load the texel began with. If it ever does, the gene has stopped being "a
     * failure to restrict" and become "invents pigment", and the pigment model's
     * downward-only guarantee is gone with it.
     */
    @Test
    void itNeverPutsBackMoreThanTheTexelStartedWith() {
        for (double d : new double[]{1, 2, 3, 4}) {
            Genotype gt = bay(d >= 3 ? SOOTY.S2 : SOOTY.S1, d % 2 == 0 ? SOOTY.S1 : SOOTY.s);
            for (long seed = 0; seed < 12; seed++) {
                PigmentField f = paint(gt, seed);
                for (int py = 0; py < N; py++) {
                    for (int px = 0; px < N; px++) {
                        assertTrue(f.black(px, py) <= 1.0f + 1e-6f,
                                "sooty put black above the starting load at " + px + "," + py);
                    }
                }
            }
        }
    }

    /** And so it does nothing at all on a plain black, which has nothing removed to keep. */
    @Test
    void aPlainBlackHorseIsUnchanged() {
        Genotype plain = Genotype.wildType();
        Genotype sooted = Genotype.wildType().with(new AllelePair(SOOTY.S2, SOOTY.S2));
        assertEquals(mean(paint(plain, 3L), Part.BODY), mean(paint(sooted, 3L), Part.BODY), 1e-6f,
                "there is no restricted pigment on a black horse for sooty to keep");
    }

    // ------------------------------------------------------------------
    // The shape
    // ------------------------------------------------------------------

    /** Countershading: dark along the topline, and the belly left alone. */
    @Test
    void theToplineDarkensAndTheBellyDoesNot() {
        PigmentField clear = paint(bay(SOOTY.s, SOOTY.s), 5L);
        PigmentField sooted = paint(bay(SOOTY.S2, SOOTY.S2), 5L);

        float topClear = bandBlack(clear, 0.85);
        float topSooty = bandBlack(sooted, 0.85);
        float bellyClear = bandBlack(clear, 0.08);
        float bellySooty = bandBlack(sooted, 0.08);

        assertTrue(topSooty > topClear + 0.3f, "the topline should darken hard, "
                + topClear + " -> " + topSooty);
        assertTrue(bellySooty - bellyClear < 0.05f, "the belly should be nearly untouched, "
                + bellyClear + " -> " + bellySooty);
        assertTrue(topSooty > bellySooty + 0.3f, "that is what countershading means");
    }

    @Test
    void aHeavierDosageIsDarker() {
        float previous = -1f;
        Allele[][] steps = {
                {SOOTY.s, SOOTY.s}, {SOOTY.S1, SOOTY.s}, {SOOTY.S1, SOOTY.S1},
                {SOOTY.S2, SOOTY.S1}, {SOOTY.S2, SOOTY.S2}};
        for (Allele[] step : steps) {
            float black = bandBlack(paint(bay(step[0], step[1]), 5L), 0.85);
            assertTrue(black > previous, "every step up the dosage should darken the topline");
            previous = black;
        }
    }

    /** Muddy lower legs are a chestnut-based signature; a bay's legs are already black. */
    @Test
    void onlyAChestnutBasedCoatGetsMuddyLegs() {
        float chestnutLeg = mean(paint(chestnut(SOOTY.S2, SOOTY.S2), 5L), Part.LEFT_FRONT_LEG)
                - mean(paint(chestnut(SOOTY.s, SOOTY.s), 5L), Part.LEFT_FRONT_LEG);
        assertTrue(chestnutLeg > 0.05f, "a sooty chestnut should get muddy legs, got " + chestnutLeg);
    }

    /** The mane and tail belong to silver, flaxen and cream - not to this. */
    @Test
    void theManeAndTailAreNotTouched() {
        for (Part part : new Part[]{Part.MANE, Part.TAIL, Part.LEFT_EAR, Part.RIGHT_EAR}) {
            assertEquals(mean(paint(chestnut(SOOTY.s, SOOTY.s), 5L), part),
                    mean(paint(chestnut(SOOTY.S2, SOOTY.S2), 5L), part), 1e-6f,
                    part + " is not sooty's business");
        }
    }

    // ------------------------------------------------------------------

    /** Extension, agouti and then sooty - the order they run in for real. */
    private static PigmentField paint(Genotype gt, long seed) {
        CoatBuildContext ctx = new CoatBuildContext(gt, Epigenome.fromSeed(seed), Skin.ADULT, true);
        PigmentField f = new PigmentField(N);
        for (Gene gene : new Gene[]{Genes.EXTENSION, Genes.AGOUTI, Genes.SOOTY}) {
            Expression e = gt.expressionOf(gene);
            if (!e.wildType()) {
                f = e.restrict(ctx, f);
            }
        }
        return f;
    }

    /** Mean black in a horizontal band of the barrel, {@code 0} belly to {@code 1} topline. */
    private static float bandBlack(PigmentField f, double height) {
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, Part.BODY);
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, Part.BODY, (px, py, p, face, point) -> {
            double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
            if (Math.abs(fy - height) < 0.08) {
                acc[0] += f.black(px, py);
                acc[1]++;
            }
        });
        return (float) (acc[0] / Math.max(1, acc[1]));
    }

    private static float mean(PigmentField f, Part part) {
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, part, (px, py, p, face, point) -> {
            acc[0] += f.black(px, py);
            acc[1]++;
        });
        return (float) (acc[0] / Math.max(1, acc[1]));
    }
}
