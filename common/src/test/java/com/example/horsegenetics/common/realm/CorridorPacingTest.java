package com.example.horsegenetics.common.realm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Issue #31: entering the F6 corridor builds a bounded slice, and the tick keeps ahead. */
class CorridorPacingTest {

    // The corridor's own numbers (DebugPenManager / DebugTestYard), as of writing.
    private static final int PERIOD = 7;
    private static final int YARD_EAST_DX = 29;
    private static final int LOOKAHEAD = 30;
    private static final int LAST = 2_000;

    @Test
    void entryBuildsOnlyTheSegmentsTheYardReaches() {
        int entry = CorridorPacing.entryTarget(YARD_EAST_DX, PERIOD, LAST);
        assertEquals(4, entry, "the yard reaches x+29, which is in segment 4");
        assertTrue((entry + 1) * PERIOD > YARD_EAST_DX, "the yard's east edge must stand on built corridor");
        assertTrue(entry < LOOKAHEAD, "entry must build less than the whole lookahead, or nothing was spread");
    }

    @Test
    void entryNeverPassesTheEndOfAShortCorridor() {
        assertEquals(2, CorridorPacing.entryTarget(YARD_EAST_DX, PERIOD, 2));
        assertEquals(0, CorridorPacing.entryTarget(-5, PERIOD, LAST));
    }

    @Test
    void aTickBuildsAtMostOneSegment() {
        assertEquals(5, CorridorPacing.tickTarget(4, 30, LAST));
        assertEquals(30, CorridorPacing.tickTarget(30, 30, LAST), "caught up: nothing to build");
        assertEquals(12, CorridorPacing.tickTarget(12, 8, LAST), "player walked back: nothing to build");
        assertEquals(LAST, CorridorPacing.tickTarget(LAST, LAST + 30, LAST));
    }

    /**
     * A player sprinting (0.28 blocks a tick) or flying fast (1.1) from the spawn
     * point never gains on the frontier, and the full lookahead is back about as
     * many ticks later as it is segments long (a little more for a fast flyer).
     */
    @Test
    void theFrontierStaysAheadOfAMovingPlayer() {
        for (double speed : new double[] {0.0, 0.28, 1.1, 3.0}) {
            int built = CorridorPacing.entryTarget(YARD_EAST_DX, PERIOD, LAST);
            double x = 3.5;
            int firstMargin = built - (int) Math.floor(x / PERIOD);
            int caughtUpAt = -1;
            for (int tick = 1; tick <= 400; tick++) {
                x += speed;
                int playerSeg = (int) Math.floor(x / PERIOD);
                int needed = playerSeg + LOOKAHEAD;
                built = CorridorPacing.tickTarget(built, needed, LAST);
                int margin = built - playerSeg;
                assertTrue(margin >= firstMargin,
                        "at " + speed + " blocks/tick the player gained on the frontier at tick " + tick
                                + " (margin " + margin + ")");
                if (caughtUpAt < 0 && built == needed) {
                    caughtUpAt = tick;
                }
            }
            // The frontier closes the gap at one segment a tick less the player's own pace.
            double bound = Math.ceil((LOOKAHEAD - firstMargin) / (1.0 - speed / PERIOD)) + 1;
            assertTrue(caughtUpAt > 0 && caughtUpAt <= bound,
                    "at " + speed + " blocks/tick the lookahead was back at tick " + caughtUpAt
                            + ", not within " + bound);
        }
    }
}
