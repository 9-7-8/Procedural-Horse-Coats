package com.example.horsegenetics.common.gear;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.example.horsegenetics.common.gear.Reins.BAREBACK_TIER;
import static com.example.horsegenetics.common.gear.Reins.Who.BAREBACK;
import static com.example.horsegenetics.common.gear.Reins.Who.BRIDLE;
import static com.example.horsegenetics.common.gear.Reins.Who.NOBODY;
import static com.example.horsegenetics.common.gear.Reins.steers;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Who steers a ridden horse (#73): the bridle, the top-bond owner, or nobody. */
class ReinsTest {

    @Test
    @DisplayName("a bridle lets anyone steer, owner or not, at any bond")
    void aBridleSteersForAnyone() {
        assertEquals(BRIDLE, steers(true, true, true, false, 0, false));
        assertEquals(BRIDLE, steers(true, true, true, true, 0, false));
    }

    @Test
    @DisplayName("bare, only the owner at the top bond tier steers")
    void bareNeedsTheOwnerAndTheBond() {
        assertEquals(BAREBACK, steers(true, true, false, true, BAREBACK_TIER, false));
        assertEquals(NOBODY, steers(true, true, false, true, BAREBACK_TIER - 1, false));
        assertEquals(NOBODY, steers(true, true, false, false, BAREBACK_TIER, false));
    }

    @Test
    @DisplayName("a bridled owner at the top tier is steering by the bridle, not bareback")
    void theBridleWinsOverTheBond() {
        // Matters for the checklist: steer_bareback must not fire for a bridled ride.
        assertEquals(BRIDLE, steers(true, true, true, true, BAREBACK_TIER, false));
    }

    @Test
    @DisplayName("a bolting horse, an untamed one, or no player aboard: nobody steers")
    void nobodySteers() {
        assertEquals(NOBODY, steers(true, true, true, true, BAREBACK_TIER, true));
        assertEquals(NOBODY, steers(true, false, true, true, BAREBACK_TIER, false));
        assertEquals(NOBODY, steers(false, true, true, true, BAREBACK_TIER, false));
    }
}
