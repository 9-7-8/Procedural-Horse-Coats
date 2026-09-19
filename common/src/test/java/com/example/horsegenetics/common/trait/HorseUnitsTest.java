package com.example.horsegenetics.common.trait;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The display conversions, pinned against the numbers the rest of the mod
 * already states in prose.
 *
 * <p>The point of this class is that four places - {@code HorseSpeedFloor}'s
 * javadoc, the breed designer's vanilla ceiling, the breed sheet's metre
 * figures and now the horse screen - all quote metres, and until now nothing
 * checked that they were quoting the <i>same</i> conversion. A refit that moves
 * {@link HorseUnits#METRES_PER_SECOND_PER_SPEED} without moving that prose
 * should fail here rather than ship two mod-wide answers to "how fast is a
 * horse".
 */
class HorseUnitsTest {

    /**
     * Deliberately loose. Every figure below is checked against a sentence
     * somewhere in the mod, and those sentences say "about": HorseSpeedFloor
     * rounds {@code 0.1125} down to 4.8 where the conversion gives 4.86. The
     * test is asking "is the prose still describing this curve", not "is the
     * prose to three places".
     */
    private static final double EPS = 0.1;

    @Test
    @DisplayName("a walking player's 0.1 is vanilla's 4.317 m/s")
    void playerWalkIsTheCalibration() {
        // The conversion is the player movement model's, and this is the one
        // value of it everybody already knows. If this moves, the derivation in
        // HorseUnits' class note is wrong, not the constant.
        assertEquals(4.317, HorseUnits.metresPerSecond(0.1), 0.01);
    }

    @Test
    @DisplayName("vanilla's horse roll is the 4.8-14.6 m/s the wiki and HorseSpeedFloor quote")
    void vanillaHorseRangeMatchesTheProse() {
        // HorseSpeedFloor's javadoc: "its 0.1125 is about 4.8 blocks a second".
        assertEquals(4.8, HorseUnits.metresPerSecond(0.1125), EPS);
        // bd-steps.js: "up to about 14.6 m/s".
        assertEquals(14.6, HorseUnits.metresPerSecond(0.3375), EPS);
    }

    @Test
    @DisplayName("the breed curve's 9.71 m/s anchor is the average vanilla horse")
    void breedCurveAnchorIsTheAverageVanillaHorse() {
        // BreedStatCurve's javadoc anchors score 5 at 9.71 m/s. That is not
        // HorseTraits.BASE_SPEED (0.1875, which is 8.09 m/s) - it is the
        // midpoint of vanilla's own roll. Pinned so the discrepancy is on the
        // record as a deliberate difference of anchor rather than of
        // conversion: both numbers come out of this one constant.
        assertEquals(9.71, HorseUnits.metresPerSecond((0.1125 + 0.3375) / 2.0), 0.01);
        assertEquals(8.09, HorseUnits.metresPerSecond(HorseTraits.BASE_SPEED), 0.01);
    }

    @Test
    @DisplayName("vanilla's 0.4-0.8 jump roll clears about 1.1 to 3.6 blocks")
    void vanillaJumpRangeMatchesTheKnownFit() {
        assertEquals(1.09, HorseUnits.jumpMetres(0.4), EPS);
        assertEquals(3.63, HorseUnits.jumpMetres(0.8), EPS);
        // The widely quoted "a horse jumps 5.3 blocks" is jump strength 1.0,
        // which vanilla never rolls - it needs a modifier, or this mod's
        // genetics. Pinned because it is the number someone will check against.
        assertEquals(5.29, HorseUnits.jumpMetres(1.0), EPS);
    }

    @Test
    @DisplayName("the breed sheet's 8.57 m ceiling is reachable, and 2.5 m is mid-range")
    void breedSheetJumpFiguresAreOnThisCurve() {
        // BreedStatCurve quotes 2.5 m at score 5 and 8.57 m at score 10. Both
        // are points on this fit, which is what says the breed sheet and the
        // horse screen are speaking the same language.
        assertEquals(2.5, HorseUnits.jumpMetres(0.645), EPS);
        assertEquals(8.57, HorseUnits.jumpMetres(1.329), EPS);
    }

    @Test
    @DisplayName("jump height never reads negative, however feeble the horse")
    void feebleHorsesReadZeroRatherThanNegative() {
        // The cubic crosses zero around 0.14, and this mod's genetics can
        // resolve well below that even though vanilla never rolls there.
        assertEquals(0.0, HorseUnits.jumpMetres(0.0));
        assertEquals(0.0, HorseUnits.jumpMetres(0.1));
        assertEquals(0.0, HorseUnits.jumpMetres(-1.0));
        assertTrue(HorseUnits.jumpMetres(0.2) > 0.0, "a 0.2 horse still hops");
    }

    @Test
    @DisplayName("both conversions rise with the attribute across the playable range")
    void bothAreMonotonic() {
        double lastSpeed = -1.0;
        double lastJump = -1.0;
        for (double x = 0.02; x <= 2.0; x += 0.01) {
            final double speed = HorseUnits.metresPerSecond(x);
            final double jump = HorseUnits.jumpMetres(x);
            assertTrue(speed > lastSpeed, "speed fell at " + x);
            assertTrue(jump >= lastJump, "jump fell at " + x);
            lastSpeed = speed;
            lastJump = jump;
        }
    }
}
