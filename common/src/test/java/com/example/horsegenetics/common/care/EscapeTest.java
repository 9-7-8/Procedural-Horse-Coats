package com.example.horsegenetics.common.care;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bolting rules: the owner's twenty percent, the hysteresis that stops one
 * bolt becoming a stutter, and the window that outlasts the blow.
 */
class EscapeTest {

    private static final double MAX = 30.0;

    @Test
    void twentyPercentIsTheDefault() {
        assertTrue(Escape.bolts(6.0, MAX, Escape.DEFAULT_THRESHOLD), "exactly at the threshold bolts");
        assertTrue(Escape.bolts(5.9, MAX, Escape.DEFAULT_THRESHOLD));
        assertFalse(Escape.bolts(6.1, MAX, Escape.DEFAULT_THRESHOLD));
    }

    @Test
    void theThresholdIsWhateverTheServerSays() {
        assertTrue(Escape.bolts(15.0, MAX, 0.5), "half of thirty");
        assertFalse(Escape.bolts(15.0, MAX, 0.1));
    }

    @Test
    void zeroTurnsItOff() {
        // What a server that does not want the behaviour sets. A horse on its
        // last half-heart must not bolt, or the setting does nothing.
        assertFalse(Escape.bolts(0.5, MAX, 0.0));
        assertFalse(Escape.keepsBolting(0.5, MAX, 0.0));
        assertFalse(Escape.bolts(0.5, MAX, -1.0));
    }

    @Test
    void noMaximumHealthIsNotAnInfiniteFraction() {
        // A horse whose attributes have not resolved yet reads zero, and
        // health <= 0 * anything is true for a dead one. Neither bolts.
        assertFalse(Escape.bolts(0.0, 0.0, Escape.DEFAULT_THRESHOLD));
        assertFalse(Escape.keepsBolting(0.0, 0.0, Escape.DEFAULT_THRESHOLD));
    }

    @Test
    void stoppingTakesMoreHealthThanStarting() {
        // The whole point of the margin: a horse that heals one point mid-flight
        // keeps running. 25% of thirty is 7.5.
        assertFalse(Escape.bolts(7.0, MAX, Escape.DEFAULT_THRESHOLD), "would not start here");
        assertTrue(Escape.keepsBolting(7.0, MAX, Escape.DEFAULT_THRESHOLD), "but does not stop here either");
        assertFalse(Escape.keepsBolting(7.6, MAX, Escape.DEFAULT_THRESHOLD), "clear of it now");
    }

    @Test
    void theThreatOutlastsTheBlow() {
        assertTrue(Escape.threatFresh(1_000L, 1_000L));
        assertTrue(Escape.threatFresh(1_000L, 1_000L + Escape.THREAT_LINGERS_TICKS));
        assertFalse(Escape.threatFresh(1_000L, 1_001L + Escape.THREAT_LINGERS_TICKS));
    }

    @Test
    void aHorseNothingHasHurtIsNotRunningFromAnything() {
        assertFalse(Escape.threatFresh(-1L, 500L));
    }

    @Test
    void aClockThatWentBackwardsErrsTowardStayingAlive() {
        assertTrue(Escape.threatFresh(1_000L, 40L));
    }
}
