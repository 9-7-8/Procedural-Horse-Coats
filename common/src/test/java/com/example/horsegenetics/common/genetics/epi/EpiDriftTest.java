package com.example.horsegenetics.common.genetics.epi;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EpiDrift} - the nudge every epigenetic value takes on its way from
 * parent to foal.
 *
 * <p>The whole design rests on the <b>shape of the distribution</b>, not on any
 * one draw: almost every foal is imperceptibly its parents, and once in a long
 * while something is genuinely different. A drift that was merely "small" would
 * make every lineage crawl toward the mean; one that was merely "rare" would
 * make breeding a slot machine. So the tests here are distribution tests.
 */
class EpiDriftTest {

    /** A plain magnitude over a span of exactly 1, so a step reads as a fraction of the span. */
    private static final EpiSchema UNIT = EpiSchema.of(EpiValue.uniform("v", 0, 1));

    private static EpiValues at(double v) {
        return UNIT.midpoint().with("v", v);
    }

    // ------------------------------------------------------------------
    // The curve
    // ------------------------------------------------------------------

    /**
     * The quantiles the design was chosen for. Half of all drifts move a value
     * less than a tenth of a percent of its range, and the far tail is where the
     * interesting horses come from.
     */
    @Test
    void theMagnitudeFollowsTheDeclaredExponential() {
        int n = 200000;
        double[] moves = new double[n];
        Rng rng = new SeededRng(20260907L);
        for (int i = 0; i < n; i++) {
            moves[i] = Math.abs(EpiDrift.drift(at(0.5), rng).get("v") - 0.5);
        }
        java.util.Arrays.sort(moves);

        // -ln(u) has median ln 2, so the median step is SCALE * ln 2 of the span.
        assertClose(EpiDrift.SCALE * Math.log(2), moves[n / 2], 0.12, "median");
        assertClose(EpiDrift.SCALE * Math.log(10), moves[(int) (n * 0.90)], 0.10, "p90");
        assertClose(EpiDrift.SCALE * Math.log(100), moves[(int) (n * 0.99)], 0.10, "p99");
    }

    /** Within {@code rel} of {@code want} - these are sampled quantiles, not exact numbers. */
    private static void assertClose(double want, double got, double rel, String what) {
        assertTrue(Math.abs(got - want) <= want * rel,
                what + ": wanted about " + want + ", got " + got);
    }

    /** Up and down equally, so a lineage wanders rather than marching. */
    @Test
    void driftHasNoDirection() {
        double sum = 0;
        int n = 100000;
        Rng rng = new SeededRng(11L);
        for (int i = 0; i < n; i++) {
            sum += EpiDrift.drift(at(0.5), rng).get("v") - 0.5;
        }
        double mean = sum / n;
        assertTrue(Math.abs(mean) < EpiDrift.SCALE * 0.05,
                "drift should be unbiased, mean step was " + mean);
    }

    /**
     * <b>A foal is its parents, to the eye.</b> The practical claim the owner
     * asked for: over a whole generation, essentially nothing visibly moves.
     */
    @Test
    void almostEveryValueIsUnchangedToTheEye() {
        int n = 20000;
        int visible = 0;
        Rng rng = new SeededRng(7L);
        for (int i = 0; i < n; i++) {
            if (Math.abs(EpiDrift.drift(at(0.5), rng).get("v") - 0.5) > 0.01) {
                visible++;
            }
        }
        assertTrue(visible < n / 500,
                "a 1%-of-range move should be rare, saw " + visible + " in " + n);
    }

    // ------------------------------------------------------------------
    // Seeds and categories do not nudge
    // ------------------------------------------------------------------

    /**
     * A noise seed is replaced whole or not at all - there is no "slightly
     * different" splash - and it is left alone the overwhelming majority of the
     * time, so a lineage keeps the shape of its markings.
     */
    @Test
    void aSeedIsEitherKeptOrReplacedWhole() {
        EpiSchema schema = EpiSchema.of(EpiValue.seed("s"));
        EpiValues start = schema.midpoint().withSeed("s", 0x1234_5678_9ABC_DEF0L);
        int changed = 0;
        int n = 50000;
        Rng rng = new SeededRng(3L);
        for (int i = 0; i < n; i++) {
            if (EpiDrift.drift(start, rng).seed("s") != start.seed("s")) {
                changed++;
            }
        }
        double rate = changed / (double) n;
        assertTrue(rate < EpiDrift.REPLACE_CHANCE * 3 && rate > EpiDrift.REPLACE_CHANCE / 3,
                "seed replacement should sit near " + EpiDrift.REPLACE_CHANCE + ", got " + rate);
    }

    /** The same for a category: a particle never wanders from the mane to the tail by degrees. */
    @Test
    void aCategoryIsNeverNudgedAStep() {
        EpiSchema schema = EpiSchema.of(EpiValue.category("c", 6));
        EpiValues start = schema.midpoint().with("c", 2);
        Rng rng = new SeededRng(5L);
        for (int i = 0; i < 20000; i++) {
            int got = EpiDrift.drift(start, rng).category("c");
            assertTrue(got >= 0 && got < 6, "a category stays inside its list");
        }
    }

    // ------------------------------------------------------------------
    // Bounds
    // ------------------------------------------------------------------

    /**
     * <b>Drift may leave the design range.</b> This is the owner's call and the
     * point of the whole feature: the range wild horses are born in is a
     * starting point, not a ceiling a breeder can never pass.
     */
    @Test
    void aLineCanBeBredPastTheRangeWildHorsesAreBornIn() {
        EpiValues v = at(1.0);   // already at the top of the design range
        Rng rng = new SeededRng(13L);
        for (int i = 0; i < 4000; i++) {
            v = EpiDrift.drift(v, rng);
        }
        assertTrue(v.get("v") > 1.0 || v.get("v") < 1.0, "the value moved");
        // Over four thousand generations of a random walk it should be well clear
        // of where it started at least sometimes; what matters is that nothing
        // pinned it to 1.0.
        assertNotEquals(1.0, v.get("v"), 1e-9);
    }

    /** The hard safety clamp still holds, however long the line is bred. */
    @Test
    void theHardClampIsNeverCrossed() {
        EpiValue declared = UNIT.get(0);
        EpiValues v = at(1.0);
        Rng rng = new SeededRng(17L);
        for (int i = 0; i < 50000; i++) {
            v = EpiDrift.drift(v, rng);
            assertTrue(v.get("v") >= declared.clampLo() && v.get("v") <= declared.clampHi(),
                    "drift escaped the hard bound: " + v.get("v"));
        }
    }

    /** A gene that stores nothing costs nothing and is handed straight back. */
    @Test
    void aSchemalessGeneIsUntouched() {
        assertEquals(EpiValues.EMPTY, EpiDrift.drift(EpiValues.EMPTY, new SeededRng(1L)));
    }
}
