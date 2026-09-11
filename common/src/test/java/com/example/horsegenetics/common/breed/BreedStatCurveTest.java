package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertNull(BreedStatCurve.sizeBand(1.0, 1.0));
        assertNull(BreedStatCurve.sizeBand(0.97, 1.04)); // both sides near baseline
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
    void theSizeRangeIsTheBand() {
        TargetBand tiny = BreedStatCurve.sizeBand(0.38, 0.51);
        assertNotNull(tiny);
        assertEquals(0.38, tiny.lo(), 1e-9);
        assertEquals(0.51, tiny.hi(), 1e-9);

        TargetBand draught = BreedStatCurve.sizeBand(1.14, 1.38);
        assertNotNull(draught);
        assertEquals(1.14, draught.lo(), 1e-9);
        assertEquals(1.38, draught.hi(), 1e-9);
    }

    /** The files were converted with sizeForHands; it is the old hands curve, exaggeration and all. */
    @Test
    void sizeForHandsIsTheOldHeightCurve() {
        assertEquals(6.0 / 15.75, BreedStatCurve.sizeForHands(6.0), 1e-9);
        assertEquals(1.0, BreedStatCurve.sizeForHands(BreedStatCurve.BASELINE_HH), 1e-9);
        // above the baseline the excess is tripled: 17.75 hh is 1.127 raw, 1.381 drawn
        assertEquals(1.0 + (17.75 / 15.75 - 1.0) * 3.0, BreedStatCurve.sizeForHands(17.75), 1e-9);
    }

    @Test
    void oneSizeCopyBetweenSevenTenthsAndOneAndThreeTenths() {
        assertTrue(BreedStatCurve.heterozygousSize(0.7));
        assertTrue(BreedStatCurve.heterozygousSize(1.0));
        assertTrue(BreedStatCurve.heterozygousSize(1.3));
        assertFalse(BreedStatCurve.heterozygousSize(0.69));
        assertFalse(BreedStatCurve.heterozygousSize(1.31));
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
