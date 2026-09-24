package com.example.horsegenetics.common.care;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bond decay: that it stops at the floor, never raises anything, charges for
 * days spent unloaded, and can be switched off.
 */
class BondTest {

    private static final int PER_DAY = Bond.DEFAULT_PER_DAY;
    private static final int FLOOR = Bond.DEFAULT_FLOOR;

    @Test
    void oneDayCostsOnePoint() {
        assertEquals(99, Bond.decayed(100, 1, PER_DAY, FLOOR));
        assertEquals(90, Bond.decayed(100, 10, PER_DAY, FLOOR));
    }

    @Test
    void decayStopsAtTheFloorHoweverLongItHasBeen() {
        // A year in an unloaded chunk costs exactly what neglect is allowed to
        // cost: the tiers that walk toward you, and never the one that looks up.
        assertEquals(FLOOR, Bond.decayed(100, 365, PER_DAY, FLOOR));
        assertEquals(FLOOR, Bond.decayed(FLOOR + 1, 2, PER_DAY, FLOOR));
    }

    @Test
    void aHorseBelowTheFloorIsLeftAloneRatherThanToppedUp() {
        // The floor is where decay stops, not a level bond is held at. A foal
        // that inherited a quarter of a poorly-bonded dam must not be handed
        // thirty-one points by the decay pass.
        assertEquals(0, Bond.decayed(0, 100, PER_DAY, FLOOR));
        assertEquals(12, Bond.decayed(12, 100, PER_DAY, FLOOR));
        assertEquals(FLOOR, Bond.decayed(FLOOR, 100, PER_DAY, FLOOR));
    }

    @Test
    void noDaysAndNoFutureStampEverMoveIt() {
        // A stamp from the future - a rolled-back world, or a /time set - must
        // read as nothing owing rather than as bond owed back.
        assertEquals(100, Bond.decayed(100, 0, PER_DAY, FLOOR));
        assertEquals(100, Bond.decayed(100, -5, PER_DAY, FLOOR));
    }

    @Test
    void zeroPerDayTurnsItOff() {
        // behaviour.bond_decay_per_day = 0 restores what shipped before this:
        // bond only ever goes up.
        assertEquals(100, Bond.decayed(100, 9_999, 0, FLOOR));
    }

    @Test
    void aFasterRateStillCannotOvershootTheFloor() {
        assertEquals(FLOOR, Bond.decayed(100, 3, 50, FLOOR));
        assertEquals(50, Bond.decayed(100, 5, 10, 0));
        assertEquals(0, Bond.decayed(100, 50, 10, 0));
    }

    @Test
    void aVeryOldStampCannotOverflowIntoARaise() {
        // days * perDay in int arithmetic wraps negative at a few hundred
        // million ticks' worth of days, which would read as bond owed.
        assertEquals(FLOOR, Bond.decayed(100, Long.MAX_VALUE / 2, 100, FLOOR));
    }
}
