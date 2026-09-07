package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.coat.pattern.BayCoat;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.BayShade;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bay continuum: the {@link ShadeGene} locus, the two dosage terms
 * {@link BayShade} adds to it, the four labels those land on, and the fact that
 * the label actually changes what {@link BayCoat} paints.
 *
 * <p>The claim under test is the one the whole overhaul rests on: blood bay,
 * bay, liver bay and seal brown are <b>bands of one score</b>, not four alleles
 * and not four painters.
 */
class BayShadeGeneTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    private static Genotype bay(String shadeTokens, String extensionTokens, String agoutiTokens) {
        return Genotype.wildType()
                .with(pair(Genes.SHADE, shadeTokens))
                .with(pair(Genes.EXTENSION, extensionTokens))
                .with(pair(Genes.AGOUTI, agoutiTokens));
    }

    private static AllelePair pair(Gene gene, String tokens) {
        int slash = tokens.indexOf('/');
        return new AllelePair(gene.fromToken(tokens.substring(0, slash)),
                gene.fromToken(tokens.substring(slash + 1)));
    }

    // ------------------------------------------------------------------
    // The locus
    // ------------------------------------------------------------------

    @Test
    void shadeDosageIsAdditiveAcrossTheThreeHaplotypes() {
        assertEquals(-3.0, Genes.SHADE.dosage(pair(Genes.SHADE, "ShL/ShL")));
        assertEquals(-1.5, Genes.SHADE.dosage(pair(Genes.SHADE, "ShL/Sh")));
        assertEquals(0.0, Genes.SHADE.dosage(pair(Genes.SHADE, "ShL/ShD")));
        assertEquals(0.0, Genes.SHADE.dosage(pair(Genes.SHADE, "Sh/Sh")));
        assertEquals(1.5, Genes.SHADE.dosage(pair(Genes.SHADE, "Sh/ShD")));
        assertEquals(3.0, Genes.SHADE.dosage(pair(Genes.SHADE, "ShD/ShD")));
    }

    /**
     * It paints nothing on its own, so it never reaches the coat pipeline and
     * the gallery gives it one pen however many haplotypes it has - the
     * {@code PATN} contract, and what makes the whole thing free.
     */
    @Test
    void shadePaintsNothingItself() {
        assertFalse(Genes.SHADE.affectsCoat());
        for (var expression : Genes.SHADE.expressions()) {
            assertTrue(expression.wildType(), expression.id() + " should be a wild type");
        }
    }

    /** Agouti declares the dependency, which is what folds shade into a bay's texture key. */
    @Test
    void agoutiDeclaresItReadsShade() {
        assertTrue(Genes.AGOUTI.coatDependsOn().contains(ShadeGene.KEY));
        String bayCode = bay("ShD/ShD", "E/E", "A/a").coatCode();
        String lightCode = bay("ShL/ShL", "E/E", "A/a").coatCode();
        assertNotEquals(bayCode, lightCode, "two shades of bay must not share a baked texture");
    }

    /** A chestnut's shade is carried, heritable, and completely invisible - including in its texture key. */
    @Test
    void shadeIsOutOfAChestnutsTextureKey() {
        assertEquals(bay("ShL/ShL", "e/e", "A/a").coatCode(),
                bay("ShD/ShD", "e/e", "A/a").coatCode());
    }

    // ------------------------------------------------------------------
    // The score and its bands
    // ------------------------------------------------------------------

    @Test
    void theTwoDosageTermsDarkenAndAreSmallerThanTheHaplotype() {
        double plain = BayShade.geneticScore(bay("Sh/Sh", "E/e", "A/A"));
        double homozygousExtension = BayShade.geneticScore(bay("Sh/Sh", "E/E", "A/A"));
        double oneAgouti = BayShade.geneticScore(bay("Sh/Sh", "E/e", "A/a"));
        double oneHaplotype = BayShade.geneticScore(bay("Sh/ShD", "E/e", "A/A"));

        assertEquals(0.0, plain);
        assertTrue(homozygousExtension > plain);
        assertTrue(oneAgouti > plain);
        assertTrue(oneAgouti < homozygousExtension, "the agouti term is the weakest of the three");
        assertTrue(oneHaplotype > homozygousExtension, "the haplotype must outweigh either dosage term");
    }

    /**
     * Every {@code Sh/Sh} bay is an ordinary bay, whatever the dosage terms do.
     * If a dosage term could push one out of the band on its own it would be
     * standing in for the shade locus, and {@code E/E} would read as a
     * diagnostic test for liver bay - which is exactly the claim the real
     * research does not support.
     */
    @Test
    void neutralHaplotypeIsAnOrdinaryBayAtEveryDosage() {
        for (String extension : new String[]{"E/E", "E/e"}) {
            for (String agouti : new String[]{"A/A", "A/a"}) {
                assertEquals(BayShade.Shade.BAY, BayShade.shadeOf(bay("Sh/Sh", extension, agouti)),
                        "Sh/Sh " + extension + " " + agouti);
            }
        }
    }

    @Test
    void theHaplotypeExtremesReachBothEndsOfTheRange() {
        assertEquals(BayShade.Shade.BLOOD, BayShade.shadeOf(bay("ShL/ShL", "E/E", "A/a")));
        assertEquals(BayShade.Shade.LIVER, BayShade.shadeOf(bay("Sh/ShD", "E/E", "A/A")));
        assertEquals(BayShade.Shade.SEAL, BayShade.shadeOf(bay("ShD/ShD", "E/E", "A/A")));
        // ShD/ShD alone is not enough - a seal needs a dosage term on top
        assertEquals(BayShade.Shade.LIVER, BayShade.shadeOf(bay("ShD/ShD", "E/e", "A/A")));
    }

    @Test
    void agoutiNamesTheShadeItLandsOn() {
        assertSame(Genes.AGOUTI.expression(BayShade.Shade.BLOOD.id()),
                bay("ShL/ShL", "E/E", "A/a").expressionOf(Genes.AGOUTI));
        assertSame(Genes.AGOUTI.expression(BayShade.Shade.SEAL.id()),
                bay("ShD/ShD", "E/E", "A/A").expressionOf(Genes.AGOUTI));
        // and none of it shows on a horse with no black to move
        assertTrue(bay("ShD/ShD", "e/e", "A/A").expressionOf(Genes.AGOUTI).wildType());
        assertTrue(bay("ShD/ShD", "E/E", "a/a").expressionOf(Genes.AGOUTI).wildType());
    }

    /** The roll never carries a horse more than one band edge from its label. */
    @Test
    void theExpressionRollIsNarrowerThanABand() {
        assertTrue(BayShade.EXPRESSION_RANGE * 2 < BayShade.BAY_MAX - BayShade.BLOOD_MAX,
                "the roll must not span a whole band");
    }

    // ------------------------------------------------------------------
    // What it paints
    // ------------------------------------------------------------------

    @Test
    void bodyBlackRisesWithTheSpreadAndKeepsTheVerifiedMiddle() {
        assertEquals(0.32f, BayCoat.bodyBlack(0.5), 0.005f,
                "spread 0.5 must still be the body black that was verified in-game");
        float previous = -1f;
        for (double t = 0; t <= 1.0001; t += 0.1) {
            float black = BayCoat.bodyBlack(t);
            assertTrue(black > previous, "body black must rise with the spread, stalled at " + t);
            previous = black;
        }
        assertTrue(BayCoat.bodyBlack(0) < 0.2f, "a blood bay's body should be red, not brown");
        assertTrue(BayCoat.bodyBlack(1) > 0.7f, "a seal brown's body should be nearly black");
    }

    @Test
    void softPointsOnlyAppearAtTheDarkEnd() {
        assertEquals(0.0, BayCoat.softPoints(0.0));
        assertEquals(0.0, BayCoat.softPoints(0.5));
        assertTrue(BayCoat.softPoints(1.0) > 0.9, "a seal brown must show its tan points");
    }

    /**
     * A seal brown's muzzle is <b>tan</b> and a blood bay's is <b>black</b> -
     * the single most legible difference between the two ends, and the one the
     * old constant-body bay could not draw at all.
     */
    @Test
    void theSealBrownKeepsATanMuzzleWhereTheBloodBayIsBlack() {
        assertTrue(muzzleBlack(0.0) > 0.9f, "a blood bay's muzzle should be black");
        PigmentField seal = paint(1.0);
        float muzzle = meanBlack(seal, Part.MUZZLE);
        float body = meanBlack(seal, Part.BODY);
        assertTrue(muzzle < muzzleBlack(0.0) - 0.3f, "a seal brown's muzzle should be mealy, got " + muzzle);
        assertTrue(muzzle < body, "a seal brown's muzzle must be lighter than its near-black body, "
                + muzzle + " vs " + body);
    }

    @Test
    void everyBayKeepsItsBlackManeTailAndEars() {
        for (double spread : new double[]{0.0, 0.25, 0.5, 0.75, 1.0}) {
            PigmentField f = paint(spread);
            for (Part part : new Part[]{Part.MANE, Part.TAIL, Part.LEFT_EAR, Part.RIGHT_EAR}) {
                assertTrue(meanBlack(f, part) > 0.95f,
                        part + " should stay black at spread " + spread);
            }
        }
    }

    @Test
    void aDarkerSpreadPutsMoreBlackUpTheLegs() {
        double previous = -1;
        for (double spread : new double[]{0.0, 0.25, 0.5, 0.75, 1.0}) {
            double h = BayCoat.legHeight(spread);
            assertTrue(h > previous);
            previous = h;
        }
        assertTrue(meanBlack(paint(1.0), Part.LEFT_FRONT_LEG)
                > meanBlack(paint(0.0), Part.LEFT_FRONT_LEG));
    }

    /** The five-draw order is a contract - the golden coats lean on it. */
    @Test
    void theRollConsumesFiveFloatsInOneOrder() {
        Genotype gt = bay("Sh/Sh", "E/E", "A/a");
        CoatBuildContext ctx = new CoatBuildContext(gt, Epigenome.fromSeed(11L), Skin.ADULT, true);

        PigmentField once = new PigmentField(N);
        BayCoat.apply(ctx, once, ctx.epigeneticsFor(AgoutiGene.KEY));
        PigmentField twice = new PigmentField(N);
        BayCoat.apply(ctx, twice, ctx.epigeneticsFor(AgoutiGene.KEY));
        assertEquals(meanBlack(once, Part.BODY), meanBlack(twice, Part.BODY),
                "the same horse must regenerate the same coat");

        SeededRng counted = new SeededRng(7L, AgoutiGene.KEY);
        SeededRng reference = new SeededRng(7L, AgoutiGene.KEY);
        BayShade.spread(gt, counted);
        for (int i = 0; i < CoatRegions.LEGS.size(); i++) {
            counted.nextFloat();
        }
        for (int i = 0; i < 5; i++) {
            reference.nextFloat();
        }
        assertEquals(reference.nextFloat(), counted.nextFloat(),
                "a bay must consume exactly five floats");
    }

    // ------------------------------------------------------------------

    private static PigmentField paint(double spread) {
        Genotype gt = bay("Sh/Sh", "E/E", "A/a");
        CoatBuildContext ctx = new CoatBuildContext(gt, Epigenome.fromSeed(1L), Skin.ADULT, true);
        PigmentField f = new PigmentField(N);
        BayCoat.apply(ctx, f, spread);
        return f;
    }

    private static float muzzleBlack(double spread) {
        return meanBlack(paint(spread), Part.MUZZLE);
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
