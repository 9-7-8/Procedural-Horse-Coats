package com.example.horsegenetics.common.care;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dormancy keeps every scan on time and every beat once per interval (#204). These pin the arithmetic that
 * promise rests on; which horses go dormant is the game module's.
 */
class DormancyTest {

    @Test
    void everyScanTickIsARunTick() {
        // The intervals the per-horse handlers use today.
        for (int interval : new int[] {20, 30, 40, 100, 200}) {
            assertTrue(Dormancy.alignedInterval(interval), "interval " + interval);
            for (int id = 0; id < 50; id++) {
                for (int tick = 0; tick < 400; tick++) {
                    if (Math.floorMod(tick + id, interval) == 0) {
                        assertTrue(Dormancy.runTick(tick, id), "scan tick " + tick + " id " + id);
                    }
                }
            }
        }
    }

    @Test
    void anUnalignedIntervalIsCaught() {
        assertFalse(Dormancy.alignedInterval(15));
        assertFalse(Dormancy.alignedInterval(25));
    }

    @Test
    void aDormantHorseRunsOneTickInStride() {
        int runs = 0;
        for (int tick = 0; tick < 1000; tick++) {
            if (Dormancy.runTick(tick, 7)) {
                runs++;
            }
        }
        assertEquals(1000 / Dormancy.STRIDE, runs);
    }

    @Test
    void anAwakeBeatIsTheOldExactTest() {
        for (long t = 0; t < 200; t++) {
            assertEquals(Math.floorMod(t + 3, 40L) == 0, Dormancy.beatWithin(t, 3, 40, 1));
        }
    }

    @Test
    void aDormantBeatFiresOncePerInterval() {
        // Runs every STRIDE ticks at offset 4; a 70-tick beat must fire once per 70 ticks, never skipped.
        int fired = 0;
        for (long t = 4; t < 7000; t += Dormancy.STRIDE) {
            if (Dormancy.beatWithin(t, 11, 70, Dormancy.STRIDE)) {
                fired++;
            }
        }
        assertEquals(100, fired);
    }

    @Test
    void aBeatShorterThanTheStrideFiresEveryRun() {
        assertTrue(Dormancy.beatWithin(123, 0, 5, Dormancy.STRIDE));
        assertTrue(Dormancy.beatWithin(124, 0, 1, Dormancy.STRIDE));
    }
}
