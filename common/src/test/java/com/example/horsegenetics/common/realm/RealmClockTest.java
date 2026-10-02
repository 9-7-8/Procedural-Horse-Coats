package com.example.horsegenetics.common.realm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.realm.RealmClock.Pace;
import org.junit.jupiter.api.Test;

class RealmClockTest {

    private static final long SCAN = 40L;
    private static final Pace DEFAULT = new Pace(RealmClock.DEFAULT_RATE_PERCENT, RealmClock.DEFAULT_PAUSE_WHEN_UNLOADED);

    @Test
    void theDefaultIsNormalSpeedWhileLoaded() {
        assertEquals(100, DEFAULT.ratePercent());
        assertEquals(0L, RealmClock.deferred(SCAN, SCAN, DEFAULT));
        assertEquals(0L, RealmClock.deferred(SCAN * RealmClock.DORMANT_AFTER_SCANS, SCAN, DEFAULT));
    }

    @Test
    void anUnloadedMareLivesThroughNoneOfTheGap() {
        long week = 7L * 24_000L;
        assertEquals(week, RealmClock.deferred(week, SCAN, DEFAULT));
        assertEquals(SCAN * 3 + 1, RealmClock.deferred(SCAN * 3 + 1, SCAN, DEFAULT));
    }

    @Test
    void withThePauseOffTheGapCountsAtTheRate() {
        long week = 7L * 24_000L;
        assertEquals(0L, RealmClock.deferred(week, SCAN, new Pace(100, false)), "off at 100 is plain game time");
        assertEquals(week * 3 / 4, RealmClock.deferred(week, SCAN, new Pace(25, false)));
    }

    @Test
    void aSlowerRateDefersTheRestOfALoadedGap() {
        assertEquals(30L, RealmClock.deferred(SCAN, SCAN, new Pace(25, true)));
        assertEquals(SCAN, RealmClock.deferred(SCAN, SCAN, new Pace(0, true)), "0 stops it while loaded");
    }

    @Test
    void noGapDefersNothing() {
        assertEquals(0L, RealmClock.deferred(0L, SCAN, new Pace(0, true)));
        assertEquals(0L, RealmClock.deferred(-5L, SCAN, DEFAULT));
    }

    @Test
    void theLineSaysWhyTheClockIsStill() {
        assertEquals("Pregnant (stops while nobody is near)", RealmClock.withNote("Pregnant", DEFAULT));
        assertEquals("Pregnant (paused in the realm)", RealmClock.withNote("Pregnant", new Pace(0, true)));
        assertEquals("Pregnant (paused in the realm)", RealmClock.withNote("Pregnant", new Pace(0, false)));
        assertEquals("Pregnant", RealmClock.withNote("Pregnant", new Pace(100, false)));
        assertEquals("", RealmClock.withNote("", DEFAULT), "a stallion's empty line stays empty");
    }

    @Test
    void aRateOutsideZeroToAHundredIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Pace(101, true));
        assertThrows(IllegalArgumentException.class, () -> new Pace(-1, true));
        assertTrue(new Pace(0, true).stopped());
        assertFalse(DEFAULT.stopped());
    }
}
