package com.example.horsegenetics.common.care;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void theRiderIsToldWhereTheirSaddleWent() {
        assertEquals("Ravensong has bolted, and will not be steered until it is calm."
                        + " Your saddle is in your pack.",
                Escape.reinsLost("Ravensong", Escape.Saddle.POCKETED));
        assertEquals("Ravensong has bolted, and will not be steered until it is calm."
                        + " Your saddle is on the ground beside you - your pack was full.",
                Escape.reinsLost("Ravensong", Escape.Saddle.DROPPED));
    }

    @Test
    void aBarebackRiderIsNotPromisedASaddleTheyNeverHad() {
        // The phantom saddle bareback steering lends is not the rider's to keep,
        // so this case must not offer them one to go and pick up.
        assertEquals("Ravensong has bolted, and will not be steered until it is calm.",
                Escape.reinsLost("Ravensong", Escape.Saddle.NONE));
    }

    @Test
    void theJumpBoostClearsAFenceForTheWeakestHorse() {
        // The whole justification for the number: a fence is a block and a half,
        // which wants about 0.5 of jump strength, and the weakest horses sit at
        // 0.4. If this ever fails, "it jumps fences" has quietly become
        // "a good jumper jumps fences" again.
        double weakest = 0.4;
        assertTrue(weakest * (1.0 + Escape.JUMP_BOOST) >= 0.5,
                "a boosted 0.4 jump must still clear a fence");
    }
}
