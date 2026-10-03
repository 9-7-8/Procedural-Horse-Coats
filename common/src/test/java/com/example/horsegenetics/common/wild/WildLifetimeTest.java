package com.example.horsegenetics.common.wild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.wild.WildLifetime.Fate;
import com.example.horsegenetics.common.wild.WildLifetime.Verdict;
import org.junit.jupiter.api.Test;

class WildLifetimeTest {

    private static final long DAY = WildLifetime.DAY_TICKS;
    private static final int DAYS = WildLifetime.DEFAULT_DAYS;

    @Test
    void theDefaultIsThreeDays() {
        assertEquals(3, DAYS);
    }

    @Test
    void aHorseStaysUntilDayThreeAndGoesOnIt() {
        long born = 5 * DAY + 1234;
        assertEquals(Verdict.STAY, WildLifetime.judge(born, born, DAYS));
        assertEquals(Verdict.STAY, WildLifetime.judge(born, born + 3 * DAY - 1, DAYS));
        assertEquals(Verdict.GO, WildLifetime.judge(born, born + 3 * DAY, DAYS));
        assertEquals(Verdict.GO, WildLifetime.judge(born, born + 40 * DAY, DAYS), "unloaded for weeks still counts");
    }

    @Test
    void zeroDaysSwitchesTheLifetimeOff() {
        assertEquals(Verdict.STAY, WildLifetime.judge(0L, 1000 * DAY, 0));
    }

    @Test
    void aStampFromTheFutureStaysAndIsRestamped() {
        long now = 10 * DAY;
        long future = now + 50 * DAY;
        assertEquals(Verdict.STAY, WildLifetime.judge(future, now, DAYS));
        assertTrue(WildLifetime.needsRestamp(future, now));
        assertFalse(WildLifetime.needsRestamp(now, now));
    }

    @Test
    void nobodyLeavesWhileWatched() {
        assertEquals(Fate.STAY, WildLifetime.fate(Verdict.GO, true, false, true));
        assertEquals(Fate.STAY, WildLifetime.fate(Verdict.GO, true, true, true));
    }

    @Test
    void anUntouchedHorseIsRemovedAndATouchedOneGoesToTheRealm() {
        assertEquals(Fate.REMOVE, WildLifetime.fate(Verdict.GO, false, false, true));
        assertEquals(Fate.TO_REALM, WildLifetime.fate(Verdict.GO, false, true, true));
    }

    @Test
    void withTheHandOffOffATouchedHorseIsRemovedLikeTheRest() {
        assertEquals(Fate.REMOVE, WildLifetime.fate(Verdict.GO, false, true, false));
    }

    @Test
    void aHorseNotYetDueStays() {
        assertEquals(Fate.STAY, WildLifetime.fate(Verdict.STAY, false, true, true));
        assertEquals(Fate.STAY, WildLifetime.fate(Verdict.STAY, false, false, true));
    }

    @Test
    void theLineSaysWhereItWent() {
        assertTrue(WildLifetime.leftLine("Bay mare", "1, 2, 3", Fate.TO_REALM, 3 * DAY).contains("horse realm"));
        assertTrue(WildLifetime.leftLine("Bay mare", "1, 2, 3", Fate.REMOVE, 3 * DAY).contains("3.0 days"));
    }
}
