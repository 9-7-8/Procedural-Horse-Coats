package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rabicano: that it is dominant to <i>inherit</i> and variable to <i>see</i>,
 * that its white starts at the tail and flank rather than covering the trunk,
 * and that it is not classic roan.
 */
class RabicanoGeneTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;
    private static final RabicanoGene RABICANO = Genes.RABICANO;

    private static Genotype bay(Allele a, Allele b) {
        return Genotype.wildType()
                .with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.a))
                .with(new AllelePair(a, b));
    }

    // ------------------------------------------------------------------
    // Inheritance
    // ------------------------------------------------------------------

    @Test
    void oneCopyIsEnoughAndTwoIsItsOwnOutcome() {
        assertTrue(bay(RABICANO.rb, RABICANO.rb).expressionOf(Genes.RABICANO).wildType());
        assertEquals("rabicano", bay(RABICANO.Rb, RABICANO.rb).expressionOf(Genes.RABICANO).id());
        assertEquals("strong-rabicano", bay(RABICANO.Rb, RABICANO.Rb).expressionOf(Genes.RABICANO).id());
        assertTrue(RABICANO.isRabicano(new AllelePair(RABICANO.Rb, RABICANO.rb)));
    }

    /** No founder is affected in a way that costs it anything - it is cosmetic, so none is excluded. */
    @Test
    void theHomozygoteIsAViableFounderAndCostsNothing() {
        assertTrue(RABICANO.canOccur(new AllelePair(RABICANO.Rb, RABICANO.Rb)));
        // No health or body effect of any kind. Asserted through the resolver
        // rather than by an instanceof, because the class not implementing
        // TraitContribution is a compile-time fact and this is the runtime one:
        // a rabicano horse resolves to exactly the same body as a plain one.
        assertEquals(
                com.example.horsegenetics.common.trait.HorseTraits.resolve(Genotype.wildType()),
                com.example.horsegenetics.common.trait.HorseTraits.resolve(
                        Genotype.wildType().with(new AllelePair(RABICANO.Rb, RABICANO.Rb))),
                "rabicano must cost a horse nothing at all");
        boolean sawOne = false;
        for (long seed = 0; seed < 3000 && !sawOne; seed++) {
            sawOne = Genotype.random(new SeededRng(seed)).pair(Genes.RABICANO).has(RABICANO.Rb);
        }
        assertTrue(sawOne, "3000 founders should turn up at least one rabicano");
    }

    // ------------------------------------------------------------------
    // Variable expression - the whole point
    // ------------------------------------------------------------------

    /**
     * <b>A heterozygote can show nothing.</b> This is the claim that makes the
     * gene worth having: a horse recorded as solid can carry it and throw a
     * ticked foal, which a locus whose phenotype is fixed by its genotype cannot
     * represent.
     */
    @Test
    void someCarriersShowNothingAndSomeShowAGreatDeal() {
        double least = 1;
        double most = 0;
        for (long seed = 0; seed < 120; seed++) {
            double w = whiteFraction(paint(bay(RABICANO.Rb, RABICANO.rb), seed));
            least = Math.min(least, w);
            most = Math.max(most, w);
        }
        // "Invisible" as a fraction of the barrel rather than as literal zero:
        // a few per cent of scattered hairs is the minimal rabicano the
        // reference describes - a frosted patch at the dock and nothing else -
        // and it is what gets a parent recorded as solid.
        assertTrue(least < 0.05, "some carriers should be barely ticked, least was " + least);
        assertTrue(most > 0.10, "some should be obviously ticked, most was " + most);
        assertTrue(most > least * 4, "the range should be wide, " + least + " .. " + most);
    }

    /** Two copies raise the floor rather than forcing the maximum. */
    @Test
    void theHomozygoteRaisesTheFloorAndTheRangesStillOverlap() {
        double hetTotal = 0;
        double homTotal = 0;
        double homLeast = 1;
        for (long seed = 0; seed < 40; seed++) {
            hetTotal += whiteFraction(paint(bay(RABICANO.Rb, RABICANO.rb), seed));
            double hom = whiteFraction(paint(bay(RABICANO.Rb, RABICANO.Rb), seed));
            homTotal += hom;
            homLeast = Math.min(homLeast, hom);
        }
        assertTrue(homTotal > hetTotal, "two copies should average more white");
        assertTrue(homLeast > 0.005, "no homozygote should come out entirely plain");
    }

    // ------------------------------------------------------------------
    // The shape - and what separates it from roan
    // ------------------------------------------------------------------

    /** Tail and flank first: the rear of the barrel carries far more than the shoulder. */
    @Test
    void theWhiteStartsAtTheRearNotAcrossTheWholeTrunk() {
        PigmentField f = paint(bay(RABICANO.Rb, RABICANO.Rb), 6L);
        double rear = zoneWhite(f, 0.05, 0.35);
        double shoulder = zoneWhite(f, 0.70, 1.0);
        assertTrue(rear > shoulder + 0.05,
                "rabicano is flank-first, rear " + rear + " vs shoulder " + shoulder);
    }

    /** And the belly carries more than the topline - roan does not care which. */
    @Test
    void theBellyCarriesMoreThanTheTopline() {
        PigmentField f = paint(bay(RABICANO.Rb, RABICANO.Rb), 6L);
        assertTrue(bandWhite(f, 0.10) > bandWhite(f, 0.90) + 0.05, "the topline is nearly spared");
    }

    @Test
    void thereIsWhiteInTheTail() {
        double best = 0;
        for (long seed = 0; seed < 20; seed++) {
            best = Math.max(best, partWhite(paint(bay(RABICANO.Rb, RABICANO.Rb), seed), Part.TAIL));
        }
        assertTrue(best > 0.05, "the coon tail is the hallmark, best was " + best);
    }

    /** The head and the lower legs are other genes' business - rabicano makes no markings. */
    @Test
    void theHeadAndLegsAreUntouched() {
        PigmentField f = paint(bay(RABICANO.Rb, RABICANO.Rb), 6L);
        for (Part part : new Part[]{Part.HEAD, Part.MUZZLE, Part.NECK,
                Part.LEFT_FRONT_LEG, Part.RIGHT_HIND_LEG}) {
            assertEquals(0.0, partWhite(f, part), 1e-9,
                    part + " must be left to the white-marking loci");
        }
    }

    /** The comparison the gene exists to survive: roan covers the trunk, rabicano does not. */
    @Test
    void itIsNotClassicRoan() {
        Genotype roan = Genotype.wildType()
                .with(new AllelePair(Genes.AGOUTI.A, Genes.AGOUTI.a))
                .with(new AllelePair(Genes.ROAN.Rn, Genes.ROAN.rn));
        PigmentField roaned = paintWith(roan, Genes.ROAN, 6L);
        PigmentField ticked = paint(bay(RABICANO.Rb, RABICANO.Rb), 6L);

        double roanEvenness = Math.abs(zoneWhite(roaned, 0.05, 0.35) - zoneWhite(roaned, 0.70, 1.0));
        double rabEvenness = Math.abs(zoneWhite(ticked, 0.05, 0.35) - zoneWhite(ticked, 0.70, 1.0));
        assertTrue(rabEvenness > roanEvenness,
                "roan is even front-to-back and rabicano is not: " + roanEvenness + " vs " + rabEvenness);
        assertEquals(0.0, partWhite(roaned, Part.TAIL), 1e-9, "classic roan never frosts the tail");
    }

    // ------------------------------------------------------------------

    private static PigmentField paint(Genotype gt, long seed) {
        return paintWith(gt, Genes.RABICANO, seed);
    }

    private static PigmentField paintWith(Genotype gt, Gene gene, long seed) {
        CoatBuildContext ctx = new CoatBuildContext(gt, Epigenome.fromSeed(seed), Skin.ADULT, true);
        PigmentField f = new PigmentField(N);
        for (Gene g : new Gene[]{Genes.EXTENSION, Genes.AGOUTI, gene}) {
            Expression e = gt.expressionOf(g);
            if (!e.wildType()) {
                f = e.restrict(ctx, f);
            }
        }
        return f;
    }

    /** Fraction of the barrel's texels the pattern has whitened at all. */
    private static double whiteFraction(PigmentField f) {
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, Part.BODY, (px, py, p, face, point) -> {
            acc[1]++;
            if (f.red(px, py) < 0.5f && f.black(px, py) < 0.5f) {
                acc[0]++;
            }
        });
        return acc[0] / Math.max(1, acc[1]);
    }

    /** The same, restricted to a front-to-back slice of the barrel. */
    private static double zoneWhite(PigmentField f, double fromX, double toX) {
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, Part.BODY);
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, Part.BODY, (px, py, p, face, point) -> {
            double fx = (point.x() - b.xMin()) / b.span(Axis.X);
            if (fx < fromX || fx > toX) {
                return;
            }
            acc[1]++;
            if (f.red(px, py) < 0.5f && f.black(px, py) < 0.5f) {
                acc[0]++;
            }
        });
        return acc[0] / Math.max(1, acc[1]);
    }

    /** The same, in a horizontal band: {@code 0} belly to {@code 1} topline. */
    private static double bandWhite(PigmentField f, double height) {
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, Part.BODY);
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, Part.BODY, (px, py, p, face, point) -> {
            double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
            if (Math.abs(fy - height) > 0.10) {
                return;
            }
            acc[1]++;
            if (f.red(px, py) < 0.5f && f.black(px, py) < 0.5f) {
                acc[0]++;
            }
        });
        return acc[0] / Math.max(1, acc[1]);
    }

    private static double partWhite(PigmentField f, Part part) {
        double[] acc = {0, 0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, part, (px, py, p, face, point) -> {
            acc[1]++;
            if (f.red(px, py) < 0.5f && f.black(px, py) < 0.5f) {
                acc[0]++;
            }
        });
        return acc[0] / Math.max(1, acc[1]);
    }
}
