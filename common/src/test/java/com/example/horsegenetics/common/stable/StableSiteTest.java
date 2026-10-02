package com.example.horsegenetics.common.stable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #2: a stable that starts on a hilltop must not float over the valley
 * beside it. Samples are four corners then the centre, as the structure takes them.
 */
class StableSiteTest {

    @Test
    void aHilltopBesideAValleyIsRejected() {
        // The screenshot's shape: centre and one side on the hill, the far
        // corners twenty blocks down. The old placement stood at the centre (90).
        StableSite.Fit fit = StableSite.fit(new int[]{90, 90, 70, 71, 90}, 90, 6);
        assertFalse(fit.accepted());
        assertEquals(20, fit.spread());
    }

    @Test
    void gentleGroundIsBuiltOnAtItsLowestSample() {
        StableSite.Fit fit = StableSite.fit(new int[]{64, 67, 63, 66, 65}, 65, 6);
        assertTrue(fit.accepted());
        assertEquals(4, fit.spread());
        assertEquals(-2, fit.drop()); // down to 63, the high side cut into the slope
    }

    @Test
    void theThresholdItselfIsStillLevelEnough() {
        assertTrue(StableSite.fit(new int[]{60, 66, 60, 60, 60}, 60, 6).accepted());
        assertFalse(StableSite.fit(new int[]{60, 67, 60, 60, 60}, 60, 6).accepted());
    }

    @Test
    void neverRaisesTheStable() {
        // The centre is already the lowest point: nothing to drop to, and
        // lifting it would open the very gap this exists to close.
        assertEquals(0, StableSite.fit(new int[]{66, 66, 66, 66, 64}, 64, 6).drop());
    }

    @Test
    void flatGroundIsUntouched() {
        StableSite.Fit fit = StableSite.fit(new int[]{70, 70, 70, 70, 70}, 70, 6);
        assertTrue(fit.accepted());
        assertEquals(0, fit.spread());
        assertEquals(0, fit.drop());
    }

    @Test
    void noSamplesIsAProgrammingError() {
        assertThrows(IllegalArgumentException.class, () -> StableSite.fit(new int[0], 64, 6));
    }
}
