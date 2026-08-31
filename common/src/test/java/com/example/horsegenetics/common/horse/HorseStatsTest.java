package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HorseStatsTest {

    // band = [0.75 * min(parents), 1.5 * max(parents)]

    @Test
    void rollAtZeroPicksBottomOfBand() {
        // [0.75*10, 1.5*20] = [7.5, 30]
        double v = HorseStats.rollFoalStat(10.0, 20.0, new FakeRng().floats(0.0f));
        assertEquals(7.5, v, 1e-9);
    }

    @Test
    void rollAtOnePicksTopOfBand() {
        double v = HorseStats.rollFoalStat(20.0, 10.0, new FakeRng().floats(1.0f));
        assertEquals(30.0, v, 1e-9);
    }

    @Test
    void rollAtHalfIsBandMidpoint() {
        double v = HorseStats.rollFoalStat(10.0, 20.0, new FakeRng().floats(0.5f));
        assertEquals((7.5 + 30.0) / 2.0, v, 1e-9);
    }

    @Test
    void equalParentsStillGiveAWideSpread() {
        assertEquals(0.75 * 15.0, HorseStats.rollFoalStat(15.0, 15.0, new FakeRng().floats(0.0f)), 1e-9);
        assertEquals(1.5 * 15.0, HorseStats.rollFoalStat(15.0, 15.0, new FakeRng().floats(1.0f)), 1e-9);
    }

    @Test
    void topOfBandCanExceedBothParents() {
        // a foal *can* come out faster than either parent - "obnoxious horses" are allowed
        double v = HorseStats.rollFoalStat(0.20, 0.24, new FakeRng().floats(1.0f));
        assertEquals(0.36, v, 1e-9); // 1.5 * 0.24
    }
}
