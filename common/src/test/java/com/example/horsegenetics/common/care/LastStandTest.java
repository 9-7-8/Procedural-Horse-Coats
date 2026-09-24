package com.example.horsegenetics.common.care;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one-blow rules: what counts as a killing blow, the window, the clock with
 * a "never" in it, and the two ways a server can turn the save into immortality
 * by accident.
 */
class LastStandTest {

    private static final double MAX = 30.0;

    @Test
    void exactlyEnoughToEmptyTheBarIsFatal() {
        // The off-by-one that would let a horse be killed by one blow after all:
        // damage equal to the remaining health leaves zero, and zero is dead.
        assertTrue(LastStand.fatal(10.0, 10.0), "exactly lethal is lethal");
        assertTrue(LastStand.fatal(10.1, 10.0));
        assertFalse(LastStand.fatal(9.9, 10.0));
    }

    @Test
    void aHorseWithNoHealthLeftIsNotSaveable() {
        // Nothing should read as a killing blow against a horse that is already
        // dead - the save must not fire on the way out.
        assertFalse(LastStand.fatal(100.0, 0.0));
        assertFalse(LastStand.fatal(100.0, -1.0));
    }

    @Test
    void anUnspentSaveIsArmed() {
        assertTrue(LastStand.armed(LastStand.NEVER));
        // What the cooldown store answers for a key it has never been given.
        assertTrue(LastStand.armed(Long.MIN_VALUE));
        assertFalse(LastStand.armed(0L));
        assertFalse(LastStand.armed(5_000L));
    }

    @Test
    void theWindowIsTheOwnersSixSeconds() {
        int ticks = LastStand.DEFAULT_IMMUNITY_TICKS;
        assertEquals(120, ticks, "the owner asked for 120 ticks");
        assertTrue(LastStand.immune(1_000L, 1_000L, ticks), "the tick it was saved on");
        assertTrue(LastStand.immune(1_000L, 1_119L, ticks), "the last tick of the window");
        assertFalse(LastStand.immune(1_000L, 1_120L, ticks), "and out the other side");
    }

    @Test
    void anUnspentSaveIsNotImmunity() {
        // A horse that has never been saved must take damage normally, or every
        // horse in the world is invulnerable.
        assertFalse(LastStand.immune(LastStand.NEVER, 1_000L, LastStand.DEFAULT_IMMUNITY_TICKS));
        assertFalse(LastStand.immune(Long.MIN_VALUE, 1_000L, LastStand.DEFAULT_IMMUNITY_TICKS));
    }

    @Test
    void zeroTicksIsTheSaveWithoutTheBreathingSpace() {
        assertFalse(LastStand.immune(1_000L, 1_000L, 0));
        assertFalse(LastStand.immune(1_000L, 1_000L, -5));
    }

    @Test
    void aClockThatWentBackwardsErrsTowardStayingAlive() {
        assertTrue(LastStand.immune(1_000L, 40L, LastStand.DEFAULT_IMMUNITY_TICKS));
    }

    @Test
    void theSaveComesBackOnlyAtFullHealth() {
        double full = LastStand.DEFAULT_REARM_FRACTION;
        assertTrue(LastStand.rearms(MAX, MAX, full));
        assertFalse(LastStand.rearms(MAX - 0.5, MAX, full), "one half-heart short is still short");
        assertFalse(LastStand.rearms(1.0, MAX, full));
    }

    @Test
    void aServerMayHandItBackEarly() {
        assertTrue(LastStand.rearms(15.0, MAX, 0.5), "half of thirty");
        assertFalse(LastStand.rearms(14.9, MAX, 0.5));
    }

    @Test
    void noMaximumHealthIsNotAnInfiniteFraction() {
        // A horse whose attributes have not resolved yet reads zero, and
        // health >= 0 * anything is true for a dead one. Neither re-arms.
        assertFalse(LastStand.rearms(0.0, 0.0, LastStand.DEFAULT_REARM_FRACTION));
    }

    @Test
    void theSaveNeverLeavesAHorseDead() {
        // The whole point: a health of zero is death, so a server that typed 0
        // would have written a setting that kills the horse it rescues.
        assertTrue(LastStand.healthLeft(0.0, MAX) > 0.0F);
        assertTrue(LastStand.healthLeft(-5.0, MAX) > 0.0F);
        assertEquals((float) LastStand.MIN_HEALTH_LEFT, LastStand.healthLeft(0.0, MAX), 1.0e-6);
    }

    @Test
    void theSaveNeverLeavesAHorseAboveItsOwnMaximum() {
        // A falabella can have fewer health points than a server would think to
        // type, and a save that healed it past its maximum would be a bug the
        // health bar shows.
        assertEquals(4.0F, LastStand.healthLeft(10.0, 4.0), 1.0e-6);
        assertEquals(1.0F, LastStand.healthLeft(LastStand.DEFAULT_HEALTH_LEFT, MAX), 1.0e-6);
    }

    @Test
    void theOwnerIsToldTheWholeRule() {
        // Both halves have to be in it: the six seconds they have to act in, and
        // the fact that the next killing blow lands.
        String line = LastStand.survived("Ravensong", LastStand.DEFAULT_IMMUNITY_TICKS);
        assertEquals("Ravensong took a killing blow and is still standing, on its last breath."
                + " Nothing can touch it for 6 seconds - get it out."
                + " It will not be saved again until it is back to full health.", line);
    }

    @Test
    void aServerWithNoWindowDoesNotPromiseOne() {
        assertEquals("Ravensong took a killing blow and is still standing, on its last breath."
                + " It will not be saved again until it is back to full health.",
                LastStand.survived("Ravensong", 0));
    }

    @Test
    void secondsReadTheWayAPlayerCountsThem() {
        assertEquals("6", LastStand.seconds(120));
        assertEquals("1", LastStand.seconds(20));
        assertEquals("1.5", LastStand.seconds(30));
        assertEquals("0.5", LastStand.seconds(10));
    }
}
