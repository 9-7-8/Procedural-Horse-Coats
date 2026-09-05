package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TargetBand;
import org.junit.jupiter.api.Test;

class BreedStatCurveTest {

    @Test
    void scoreFiveIsBaseline() {
        assertEquals(1.0, BreedStatCurve.factor(StatAxis.SPEED, 5), 1e-9);
        assertEquals(1.0, BreedStatCurve.factor(StatAxis.HEALTH, 5), 1e-9);
        assertEquals(1.0, BreedStatCurve.factor(StatAxis.JUMP, 5), 1e-9);
    }

    @Test
    void scoreTenHitsTheSheetCeilings() {
        assertEquals(19.6 / 9.71, BreedStatCurve.factor(StatAxis.SPEED, 10), 1e-6);
        assertEquals(50.5 / 22.5, BreedStatCurve.factor(StatAxis.HEALTH, 10), 1e-6);
        assertEquals(8.57 / 2.5, BreedStatCurve.factor(StatAxis.JUMP, 10), 1e-6);
    }

    @Test
    void scoreOneIsAboutOneFifth() {
        assertEquals(0.20, BreedStatCurve.factor(StatAxis.SPEED, 1), 1e-9);
        assertEquals(0.20, BreedStatCurve.factor(StatAxis.JUMP, 1), 1e-9);
    }

    @Test
    void nearBaselineScoresCarryNoBand() {
        assertNull(BreedStatCurve.bandFor(StatAxis.SPEED, 5));
        assertNull(BreedStatCurve.bandFor(StatAxis.HEALTH, 5));
        assertNull(BreedStatCurve.scaleBand(BreedStatCurve.BASELINE_HH, BreedStatCurve.BASELINE_HH));
        assertNull(BreedStatCurve.scaleBand(15.5, 16.0)); // both sides near baseline even after big-end exaggeration
    }

    @Test
    void directionalScoresKeepTheBandOnOneSideOfOne() {
        TargetBand fast = BreedStatCurve.bandFor(StatAxis.SPEED, 10);
        assertNotNull(fast);
        assertTrue(fast.lo() >= 1.0, "fast band should not dip below 1.0: " + fast);

        TargetBand slow = BreedStatCurve.bandFor(StatAxis.SPEED, 2);
        assertNotNull(slow);
        assertTrue(slow.hi() <= 1.0, "slow band should not rise above 1.0: " + slow);
    }

    @Test
    void heightBandIsMidHeightOverBaseline() {
        TargetBand tiny = BreedStatCurve.scaleBand(6.0, 8.0);
        assertNotNull(tiny);
        assertEquals(6.0 / 15.75, tiny.lo(), 1e-9);
        assertEquals(8.0 / 15.75, tiny.hi(), 1e-9);

        TargetBand draught = BreedStatCurve.scaleBand(16.5, 17.75);
        assertNotNull(draught);
        assertTrue(draught.lo() > 1.0, "draught band should sit above 1.0: " + draught);
        // the big end is exaggerated well past the raw 17.75/15.75 = 1.13 ratio
        assertTrue(draught.hi() > 1.25, "draught top end should be dramatic: " + draught);
    }

    @Test
    void aScoreRangeWidensTheBand() {
        TargetBand narrow = BreedStatCurve.bandFor(StatAxis.SPEED, 4, 4);
        TargetBand wide = BreedStatCurve.bandFor(StatAxis.SPEED, 4, 6);
        // 4..6 straddles baseline so it may drop; 4..4 is a real slow band
        assertNotNull(narrow);
        if (wide != null) {
            assertTrue(wide.hi() - wide.lo() >= narrow.hi() - narrow.lo());
        }
    }
}
