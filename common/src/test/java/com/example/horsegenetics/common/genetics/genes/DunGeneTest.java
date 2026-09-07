package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
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
 * the first half of these tests is about which allele does which half. The
 * second half is about the painter: that grullo comes out neutral, that a bay's
 * points survive the dilution, and that the leg bars are the uneven,
 * joint-centred, per-leg thing a real dun has rather than the bracelets they
 * used to be.
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

    /** Both marked outcomes vary per horse now - the accessory markings are rolled. */
    @Test
    void bothMarkedOutcomesVaryPerHorse() {
        assertFalse(DUN.expressionOf(pair(DUN.D, DUN.d2)).deterministic());
        assertFalse(DUN.expressionOf(pair(DUN.d1, DUN.d2)).deterministic());
    }

    // ------------------------------------------------------------------
    // What the two marked outcomes actually paint
    // ------------------------------------------------------------------

    private static PigmentField painted(String pairCode, long seed, PigmentField base) {
        Genotype gt = Genotype.parse(Codes.of("dun", pairCode));
        Expression e = gt.expressionOf(DUN);
        PigmentField out = e.restrict(new CoatBuildContext(gt, Epigenome.fromSeed(seed), Skin.ADULT, true), base);
        assertNotNull(out, pairCode + " should paint something");
        return out;
    }

    private static PigmentField painted(String pairCode, PigmentField base) {
        return painted(pairCode, 4242L, base);
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
     * "marked" at all. The stripe half-width is jittered per horse, so the
     * flank sample is taken well clear of the widest it can be.
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
                double narrow = CoatRegions.dorsalStripe(Skin.ADULT, part, point, 1.2);
                double wide = CoatRegions.dorsalStripe(Skin.ADULT, part, point, 2.0);
                float[] bucket = narrow > 0.85 ? spine : (wide == 0.0 ? flank : null);
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
     * The stripe <b>reaches the tail</b>, which is the one feature a field guide
     * calls diagnostic: a fuzzy topline shadow that stops at the dock is
     * countershading, not dun.
     */
    @Test
    void theDorsalStripeCarriesIntoTheTail() {
        PigmentField out = painted("D/d2", chestnut());
        float[] tail = {0f, 0f};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part == Part.TAIL) {
                tail[0] += out.red(px, py);
                tail[1]++;
            }
        });
        assertTrue(tail[1] > 0, "the adult mesh has a tail");
        assertEquals(1.0f, tail[0] / tail[1], 1e-6, "the tail is a point - the dilution never reaches it");
    }

    /**
     * {@code d1} is <b>non</b>-dun, and the pigment model has to say so: on a
     * fully black coat there is no <i>visible</i> red to take and black is never
     * touched, so the painter is a byte-for-byte no-op. A real non-dun black
     * shows no primitive markings either, and moving a black texel off the
     * gradient's pure-black row would make it darker, not lighter - see
     * {@link DunGene}.
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
     * <b>Grullo is a blue-grey, not a mouse-brown.</b> A black horse carries
     * {@code red = 1} that its eumelanin hides; a dilution that takes the black
     * off first unmasks it and walks the sample into the warm browns. So the
     * one thing the black base must come out with is <b>no red at all</b> -
     * which puts every diluted texel on the gradient's neutral column, wherever
     * the marking mask left it.
     */
    @Test
    void grulloKeepsNoRedAnywhereItDiluted() {
        PigmentField out = painted("D/d2", new PigmentField(N));
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (out.black(px, py) < 1.0f) {
                assertEquals(0f, out.red(px, py), 1e-6f,
                        "diluted black texel at " + px + "," + py + " kept red - that is a brown grullo");
            }
        });
    }

    /**
     * A bay's points are painted <b>absolutely</b> ({@code red = 0,
     * black = 1}), and dun leaves them alone: a bay dun has black points over a
     * tan body, not grey ones. This is {@code DunGene.alreadyAPoint} - the
     * signal that separates a point from a black horse's body, which is
     * {@code (1, 1)} and does dilute.
     */
    @Test
    void aBaysPointsSurviveTheDilution() {
        Bounds leg = HorseSkinGeometry.bounds(Skin.ADULT, Part.LEFT_FRONT_LEG);
        // A seal bay's black climbs most of the way up the leg - well past
        // anything the height-based point mask covers - and it is painted
        // absolutely, red = 0. That, and not the height, is what must save it.
        PigmentField base = new PigmentField(N);
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part == Part.LEFT_FRONT_LEG
                    && (point.y() - leg.yMin()) / leg.span(Axis.Y) > 0.75) {
                base.setRed(px, py, 0f);
            }
        });
        PigmentField out = painted("D/d2", base);
        int[] checked = {0};
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part == Part.LEFT_FRONT_LEG
                    && (point.y() - leg.yMin()) / leg.span(Axis.Y) > 0.80) {
                checked[0]++;
                assertTrue(out.black(px, py) > 0.95f,
                        "a point high on the leg must stay black at " + px + "," + py
                                + ", got " + out.black(px, py));
            }
        });
        assertTrue(checked[0] > 0, "expected texels high on the front leg");
    }

    // ------------------------------------------------------------------
    // Leg barring
    // ------------------------------------------------------------------

    private static double barCoverage(Part leg, long seed, double heightFraction) {
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, leg);
        double y = b.yMin() + b.span(Axis.Y) * heightFraction;
        double best = 0;
        for (double x = b.xMin() + 0.25; x < b.xMax(); x += 0.25) {
            for (double z = b.zMin() + 0.25; z < b.zMax(); z += 0.25) {
                best = Math.max(best, CoatRegions.legBar(Skin.ADULT, leg,
                        new HorseSkinGeometry.BodyPoint(x, y, z), seed, 0.56, 0.48, 3.2, 0.42));
            }
        }
        return best;
    }

    /**
     * Bars sit <b>at and above the joint</b>. The pastern is where the old
     * field put them, and where a real dun has none: what is down there is the
     * dark lower leg, which is a point rather than a bar.
     */
    @Test
    void legBarsAvoidThePastern() {
        for (long seed = 0; seed < 24; seed++) {
            assertEquals(0.0, barCoverage(Part.LEFT_FRONT_LEG, seed, 0.03), 1e-9,
                    "seed " + seed + ": nothing at the hoof");
            assertTrue(barCoverage(Part.LEFT_FRONT_LEG, seed, 0.99) < 0.35,
                    "seed " + seed + ": bars fade where the leg meets the body");
        }
    }

    /**
     * They are <b>strokes, not bracelets</b>. A ring would give every texel at
     * one height the same coverage; a stroke does not, and a broken one drops to
     * nothing somewhere round the limb.
     */
    @Test
    void aBarDoesNotWrapTheWholeLimb() {
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, Part.LEFT_FRONT_LEG);
        int broken = 0;
        int examined = 0;
        for (long seed = 0; seed < 40; seed++) {
            // find this leg's strongest bar, then walk right round the limb at
            // that height: a ring would hold its coverage all the way round.
            double best = 0;
            double bestY = 0;
            for (double h = 0.25; h < 0.95; h += 0.01) {
                double c = barCoverage(Part.LEFT_FRONT_LEG, seed, h);
                if (c > best) {
                    best = c;
                    bestY = b.yMin() + b.span(Axis.Y) * h;
                }
            }
            if (best <= 0.4) {
                continue;
            }
            examined++;
            double min = 1;
            for (double x = b.xMin() + 0.25; x < b.xMax(); x += 0.25) {
                for (double z = b.zMin() + 0.25; z < b.zMax(); z += 0.25) {
                    min = Math.min(min, CoatRegions.legBar(Skin.ADULT, Part.LEFT_FRONT_LEG,
                            new HorseSkinGeometry.BodyPoint(x, bestY, z), seed, 0.56, 0.48, 3.2, 0.42));
                }
            }
            if (min < 0.05) {
                broken++;
            }
        }
        assertTrue(examined > 20, "expected most legs to carry a bar, got " + examined + "/40");
        // A near-complete band that does encircle the limb is possible, just not
        // typical - so this is a majority, not an absolute.
        assertTrue(broken * 4 >= examined * 3,
                "most bars should fade out somewhere round the limb, got " + broken + "/" + examined);
    }

    /** Four legs, four seeds: a real dun's set does not match. */
    @Test
    void theFourLegsDoNotMatch() {
        long seed = 99;
        double[] peaks = new double[CoatRegions.LEGS.size()];
        for (int i = 0; i < peaks.length; i++) {
            for (double h = 0.30; h < 0.95; h += 0.02) {
                peaks[i] = Math.max(peaks[i],
                        barCoverage(CoatRegions.LEGS.get(i), seed + i * 0x9E3779B97F4A7C15L, h));
            }
        }
        for (int i = 1; i < peaks.length; i++) {
            assertTrue(Math.abs(peaks[i] - peaks[0]) > 1e-6,
                    "leg " + i + " drew the same bars as leg 0");
        }
    }

    /**
     * Barring belongs to {@code D} alone. Bars are a <i>black</i> effect, so a
     * black coat is where to look: {@code D} leaves the leg holding a range of
     * black values (body, bar, point), {@code d1} leaves it flat.
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

    /**
     * The accessories really are optional: over a population, some horses draw
     * a shoulder bar and some do not. If this ever went to "all" or "none", the
     * roll has stopped working and every dun looks the same again.
     */
    @Test
    void theAccessoryMarkingsAreNotOnEveryHorse() {
        int withShoulder = 0;
        for (long seed = 0; seed < 40; seed++) {
            PigmentField out = painted("D/d2", seed, new PigmentField(N));
            // Anything on the barrel that escaped the dilution and is not the
            // dorsal stripe is a shoulder bar - it is the only other marking
            // this gene draws on the BODY part.
            boolean[] found = {false};
            HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
                if (part == Part.BODY
                        && CoatRegions.dorsalStripe(Skin.ADULT, part, point, 2.2) == 0
                        && out.black(px, py) > 0.60f) {
                    found[0] = true;
                }
            });
            if (found[0]) {
                withShoulder++;
            }
        }
        assertTrue(withShoulder > 2 && withShoulder < 38,
                "shoulder bars should be a sometimes thing, got " + withShoulder + "/40");
    }
}
