package com.example.horsegenetics.common.repro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cover notice's rules: every line names the mare, the crowding line carries
 * the numbers the owner has to act on, a changed reason always speaks, and the
 * same reason cannot fill the chat log.
 */
class CoverNoticeTest {

    @Test
    void everyReasonNamesTheMare() {
        for (CoverNotice.Reason reason : CoverNotice.Reason.values()) {
            String line = CoverNotice.line("Ravensong", reason, NaturalCover.Crowd.of(12));
            assertTrue(line.contains("Ravensong"), reason + ": " + line);
            assertTrue(line.endsWith("."), reason + ": " + line);
        }
    }

    @Test
    void theCrowdingLineCarriesBothNumbers() {
        String line = CoverNotice.line("Ravensong", CoverNotice.Reason.CROWDED, new NaturalCover.Crowd(12, 8));
        assertTrue(line.contains("12 other horses"), line);
        assertTrue(line.contains("the limit is 8"), line);
        assertTrue(line.contains(String.valueOf((int) ReproRules.NATURAL_CAP_RADIUS)), line);
    }

    @Test
    void onlyConceivingIsGoodNews() {
        for (CoverNotice.Reason reason : CoverNotice.Reason.values()) {
            if (reason == CoverNotice.Reason.CONCEIVED) {
                assertTrue(reason.good());
            } else {
                assertFalse(reason.good(), reason + " is a failure");
            }
        }
    }

    @Test
    void aMareNeverSpokenAboutIsAlwaysDue() {
        assertTrue(CoverNotice.dueAgain(null, 0L, CoverNotice.Reason.CROWDED, 0L));
    }

    @Test
    void theSameReasonWaitsOutTheQuietPeriod() {
        long told = 1_000L;
        assertFalse(CoverNotice.dueAgain(CoverNotice.Reason.CROWDED, told, CoverNotice.Reason.CROWDED,
                told + CoverNotice.QUIET_TICKS - 1L));
        assertTrue(CoverNotice.dueAgain(CoverNotice.Reason.CROWDED, told, CoverNotice.Reason.CROWDED,
                told + CoverNotice.QUIET_TICKS));
    }

    @Test
    void aChangedReasonSpeaksImmediately() {
        // The owner who just emptied the paddock has to hear what stopped it this
        // time, not five minutes of silence.
        assertTrue(CoverNotice.dueAgain(CoverNotice.Reason.CROWDED, 1_000L, CoverNotice.Reason.HURT, 1_001L));
    }

    @Test
    void aClockThatWentBackwardsDoesNotSilenceHerForever() {
        // /time set, or a world restored from a backup: HurtNotice.dueAgain has the
        // same guard, and for the same reason.
        assertTrue(CoverNotice.dueAgain(CoverNotice.Reason.CROWDED, 10_000L, CoverNotice.Reason.CROWDED, 5L));
    }

    @Test
    void theTwoEmptyRollsReadDifferently() {
        assertNotEquals(CoverNotice.line("Ravensong", CoverNotice.Reason.DID_NOT_TAKE, NaturalCover.Crowd.of(0)),
                CoverNotice.line("Ravensong", CoverNotice.Reason.NOT_RECEPTIVE, NaturalCover.Crowd.of(0)));
    }
}
