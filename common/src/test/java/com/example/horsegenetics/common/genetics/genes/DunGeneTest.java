package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
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
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dun is the locus where two dominance orders run over the same three alleles -
 * {@code D > d1 = d2} for dilution, {@code D = d1 > d2} for the markings - so
 * these tests are mostly about which allele does which half.
 */
class DunGeneTest {

    private static final DunGene DUN = Genes.DUN;
    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    private static AllelePair pair(Allele a, Allele b) {
        return new AllelePair(a, b);
    }

    @Test
    void sixCombinationsLandOnThreeOutcomes() {
        assertEquals("dun", DUN.expressionOf(pair(DUN.D, DUN.D)).id());
        assertEquals("dun", DUN.expressionOf(pair(DUN.D, DUN.d1)).id());
        assertEquals("dun", DUN.expressionOf(pair(DUN.D, DUN.d2)).id());
        assertEquals("primitive-marks", DUN.expressionOf(pair(DUN.d1, DUN.d1)).id());
        assertEquals("primitive-marks", DUN.expressionOf(pair(DUN.d1, DUN.d2)).id());
        assertTrue(DUN.expressionOf(pair(DUN.d2, DUN.d2)).wildType());
    }

    /**
     * The marking half of the locus. {@code d1} is the whole point of the third
     * allele: it dilutes nothing but still draws a stripe, so a horse can carry
     * primitive markings without being a dun.
     */
    @Test
    void d1MarksWithoutDiluting() {
        assertTrue(DUN.isMarked(pair(DUN.d1, DUN.d2)));
        assertFalse(DUN.isDun(pair(DUN.d1, DUN.d1)));

        assertTrue(DUN.isMarked(pair(DUN.D, DUN.d2)));
        assertTrue(DUN.isDun(pair(DUN.D, DUN.d2)));

        assertFalse(DUN.isMarked(pair(DUN.d2, DUN.d2)));
        assertFalse(DUN.isDun(pair(DUN.d2, DUN.d2)));
    }

    /** {@code d2} - not the old catch-all {@code d} - is the allele that does nothing. */
    @Test
    void theBaselineAlleleIsTheUnmarkedOne() {
        assertEquals(DUN.d2, DUN.defaultAllele());
        assertTrue(Genotype.wildType().expressionOf(DUN).wildType());
    }

    /**
     * Adding {@code d1} split the non-dun population rather than eating into
     * the duns: the three {@code D} rows still sum to what the old two-allele
     * table gave at {@code p(D) = 1/24}.
     */
    @Test
    void addingD1DidNotMakeDunsRarer() {
        var table = DUN.founderTable(null);
        assertEquals(6, table.pairs().size());

        double dun = table.share(pair(DUN.D, DUN.D))
                + table.share(pair(DUN.D, DUN.d1))
                + table.share(pair(DUN.D, DUN.d2));
        double p = 1.0 / DunGene.WILD_DUN_ONE_IN;
        assertEquals(1.0 - (1 - p) * (1 - p), dun, 1e-4);

        double marked = table.share(pair(DUN.d1, DUN.d1)) + table.share(pair(DUN.d1, DUN.d2));
        assertTrue(marked > dun, "d1 is the commoner of the two variant alleles");
    }

    // ------------------------------------------------------------------
    // What the two marked outcomes actually paint
    // ------------------------------------------------------------------

    private static PigmentField painted(String pairCode, PigmentField base) {
        Genotype gt = Genotype.parse(Codes.of("dun", pairCode));
        Expression e = gt.expressionOf(DUN);
        PigmentField out = e.restrict(new CoatBuildContext(gt, Epigenome.fromSeed(4242L), Skin.ADULT, true), base);
        assertNotNull(out, pairCode + " should paint something");
        return out;
    }

    /** A chestnut coat: all red, no black - the base a stripe reads best on. */
    private static PigmentField chestnut() {
        PigmentField f = new PigmentField(N);
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> f.setBlack(px, py, 0f));
        return f;
    }

    /**
     * The stripe is the region that keeps its pigment while everything around
     * it loses some - so on a chestnut, a spine texel must end up redder than a
     * flank texel. True for both marked outcomes; that is what makes them
     * "marked" at all.
     */
    @Test
    void bothMarkedOutcomesLeaveTheSpineRedderThanTheFlank() {
        for (String pairCode : new String[]{"D/d2", "d1/d2"}) {
            PigmentField out = painted(pairCode, chestnut());
            float[] spine = {0f, 0f};   // sum, count
            float[] flank = {0f, 0f};
            HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
                if (part != Part.BODY) {
                    return;
                }
                double dorsal = CoatRegions.dorsalStripe(Skin.ADULT, part, point, 1.5);
                float[] bucket = dorsal > 0.9 ? spine : (dorsal == 0.0 ? flank : null);
                if (bucket != null) {
                    bucket[0] += out.red(px, py);
                    bucket[1]++;
                }
            });
            assertTrue(spine[1] > 0 && flank[1] > 0, pairCode + ": expected both regions on the body");
            assertTrue(spine[0] / spine[1] > flank[0] / flank[1] + 0.02f,
                    pairCode + ": the dorsal stripe should hold more red than the flank");
        }
    }

    /**
     * {@code d1} is <b>non</b>-dun, and the pigment model has to say so: on a
     * fully black coat there is no red to take and black is never touched, so
     * the painter is a byte-for-byte no-op. A real non-dun black shows no
     * primitive markings either, and moving a black texel off the gradient's
     * pure-black row would make it darker, not lighter - see {@link DunGene}.
     */
    @Test
    void d1DoesNothingToABlackCoat() {
        PigmentField base = new PigmentField(N);
        PigmentField out = painted("d1/d1", base);
        for (int py = 0; py < N; py++) {
            for (int px = 0; px < N; px++) {
                assertEquals(base.red(px, py), out.red(px, py), 0f, "red at " + px + "," + py);
                assertEquals(base.black(px, py), out.black(px, py), 0f, "black at " + px + "," + py);
            }
        }
    }

    /** {@code D} is the half that dilutes, and a black coat is where it shows most. */
    @Test
    void dunDilutesABlackCoatWhereD1DoesNot() {
        PigmentField out = painted("D/d2", new PigmentField(N));
        boolean diluted = false;
        for (int py = 0; py < N && !diluted; py++) {
            for (int px = 0; px < N; px++) {
                if (out.black(px, py) < 0.9f) {
                    diluted = true;
                    break;
                }
            }
        }
        assertTrue(diluted, "D should take black off a black coat - that is grullo");
    }

    /**
     * Leg barring belongs to {@code D} alone. Bars are a <i>black</i> effect,
     * so a black coat is where to look: {@code D} leaves the leg holding a
     * range of black values (band, gap, band), {@code d1} leaves it flat.
     */
    @Test
    void onlyDunBarsTheLegs() {
        PigmentField dun = painted("D/d2", new PigmentField(N));
        PigmentField marked = painted("d1/d2", new PigmentField(N));
        float[] dunRange = {1f, 0f};      // min, max
        float[] markedRange = {1f, 0f};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != Part.LEFT_FRONT_LEG) {
                return;
            }
            dunRange[0] = Math.min(dunRange[0], dun.black(px, py));
            dunRange[1] = Math.max(dunRange[1], dun.black(px, py));
            markedRange[0] = Math.min(markedRange[0], marked.black(px, py));
            markedRange[1] = Math.max(markedRange[1], marked.black(px, py));
        });
        assertTrue(dunRange[1] - dunRange[0] > 0.05f, "D should band the legs");
        assertEquals(markedRange[0], markedRange[1], 1e-6, "d1 should not band the legs");
    }
}
