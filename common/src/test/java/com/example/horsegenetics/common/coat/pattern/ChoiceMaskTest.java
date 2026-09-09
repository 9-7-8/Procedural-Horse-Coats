package com.example.horsegenetics.common.coat.pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The <b>{@code CHOICE} mask's draw</b> - the per-horse decision that
 * known-gaps gap 102 was opened for.
 *
 * <p>Quarter has to pick which quadrant goes pale, and the mask fold has no
 * branch in it: a knob can move a boundary but cannot choose between two of
 * them. The old answer was to sample {@code NOISE} at a scale of 4000 body
 * units - a hundred times the length of a horse, so the whole animal sat inside
 * one lattice cell - and stretch it onto {@code [-200, 201]} so the clamp turned
 * it into a hard yes or no. It worked and it was <i>nearly</i> constant rather
 * than constant, in two ways that traded against each other: the field still
 * drifted a little across the body, and the strip of draws landing between 0 and
 * 1 was 1/401 wide, so about one horse in four hundred wore a half-strength
 * quadrant.
 *
 * <p>These test the draw itself rather than a rendered coat, because that is
 * where the two properties the trick could not give live: <b>exactness</b> and
 * <b>fairness</b>.
 */
class ChoiceMaskTest {

    /**
     * The outcomes must be near-uniform. A seed knob's low bits are not a fair
     * coin on their own, and taking {@code % 2} straight off a raw seed is
     * exactly the sort of thing that comes out 60/40 and is never noticed -
     * which is why {@code choiceOf} mixes before it takes the modulus.
     */
    @Test
    void theChoiceIsFairAcrossSeeds() {
        for (int options : new int[]{2, 3, 4, 5, 8}) {
            int[] counts = new int[options];
            int n = 60000;
            for (int seed = 0; seed < n; seed++) {
                counts[SpecPainter.choiceOf(seed, options)]++;
            }
            double expected = n / (double) options;
            for (int i = 0; i < options; i++) {
                double error = Math.abs(counts[i] - expected) / expected;
                assertTrue(error < 0.05,
                        "choice " + i + " of " + options + " came up " + counts[i]
                                + " times in " + n + ", expected about " + expected);
            }
        }
    }

    /**
     * <b>Sequential seeds must not walk through the outcomes.</b> This is the
     * failure that the old {@code java.util.Random} seed handling had and that
     * a weak mix would reintroduce here: neighbouring seeds staying
     * neighbours. Two horses bred moments apart should not reliably draw
     * different quadrants.
     */
    @Test
    void neighbouringSeedsDoNotAlternate() {
        int agreements = 0;
        int n = 20000;
        for (int seed = 0; seed < n; seed++) {
            if (SpecPainter.choiceOf(seed, 2) == SpecPainter.choiceOf(seed + 1, 2)) {
                agreements++;
            }
        }
        double rate = agreements / (double) n;
        assertTrue(rate > 0.45 && rate < 0.55,
                "consecutive seeds agree " + (rate * 100) + "% of the time - they should be "
                        + "independent, i.e. about half. A run near 0% means the mix is "
                        + "alternating; near 100% means it is not mixing at all.");
    }

    /** Every outcome is reachable, and none outside the range is produced. */
    @Test
    void everyOutcomeIsReachableAndInRange() {
        for (int options : new int[]{2, 3, 7}) {
            boolean[] seen = new boolean[options];
            for (int seed = 0; seed < 500; seed++) {
                int c = SpecPainter.choiceOf(seed, options);
                assertTrue(c >= 0 && c < options,
                        "choiceOf returned " + c + " for options " + options);
                seen[c] = true;
            }
            for (int i = 0; i < options; i++) {
                assertTrue(seen[i], "outcome " + i + " of " + options + " never came up");
            }
        }
    }

    /** The draw is a pure function of the seed - the same horse, the same quadrant. */
    @Test
    void theSameSeedAlwaysDrawsTheSameOutcome() {
        for (long seed : new long[]{0L, 1L, 42L, -7L, 123456789L}) {
            int first = SpecPainter.choiceOf(seed, 4);
            for (int i = 0; i < 5; i++) {
                assertEquals(first, SpecPainter.choiceOf(seed, 4),
                        "choiceOf is not deterministic at seed " + seed);
            }
        }
    }

    /**
     * A single option degenerates to "always", rather than dividing by zero.
     * The painter floors {@code options} at 1, so this is the boundary it
     * guarantees.
     */
    @Test
    void oneOptionAlwaysDrawsZero() {
        for (long seed = 0; seed < 100; seed++) {
            assertEquals(0, SpecPainter.choiceOf(seed, 1));
        }
    }
}
