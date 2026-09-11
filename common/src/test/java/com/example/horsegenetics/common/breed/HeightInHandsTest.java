package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.trait.TargetBand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The info screen's height in hands ({@link BreedStatCurve#handsFor} and
 * {@link BreedStatCurve#formatHands}). The load-bearing property is that it is
 * the inverse of the curve that turns a breed's hands range into a scale, so a
 * breed written as 17 hands reads back as 17 hands.
 */
class HeightInHandsTest {

    @Test
    void theBaselineHorseIsFifteenThree() {
        assertEquals(BreedStatCurve.BASELINE_HH, BreedStatCurve.handsFor(1.0), 1e-9);
        assertEquals("15.3 hh", BreedStatCurve.formatHands(BreedStatCurve.handsFor(1.0)));
    }

    /** scaleBand's midpoint is a scale; handsFor must hand the hands straight back, either side of 1.0. */
    @Test
    void handsForUndoesTheBreedCurve() {
        for (double hands : new double[] {9.0, 12.5, 17.0, 18.5}) {
            TargetBand band = BreedStatCurve.scaleBand(hands, hands);
            double scale = (band.lo() + band.hi()) / 2.0;
            assertEquals(hands, BreedStatCurve.handsFor(scale), 0.05, "round trip at " + hands);
        }
    }

    @Test
    void formatsInHandsAndInchesNotDecimals() {
        assertEquals("16.0 hh", BreedStatCurve.formatHands(16.0));
        assertEquals("14.2 hh", BreedStatCurve.formatHands(14.5));   // 14 hands 2 inches
        assertEquals("17.1 hh", BreedStatCurve.formatHands(17.25));
        assertEquals("16.0 hh", BreedStatCurve.formatHands(15.9));   // rounds to the inch
    }
}
