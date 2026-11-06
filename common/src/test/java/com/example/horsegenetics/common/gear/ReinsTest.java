package com.example.horsegenetics.common.gear;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.example.horsegenetics.common.gear.Reins.BAREBACK_TIER;
import static com.example.horsegenetics.common.gear.Reins.Who.BAREBACK;
import static com.example.horsegenetics.common.gear.Reins.Who.BRIDLE;
import static com.example.horsegenetics.common.gear.Reins.Who.NOBODY;
import static com.example.horsegenetics.common.gear.Reins.Who.SADDLE;
import static com.example.horsegenetics.common.gear.Reins.steers;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Who steers a ridden horse (#73, #74): the bridle, the top-bond owner, a saddle where the server allows it, or nobody. */
class ReinsTest {

    @Test
    @DisplayName("a bridle lets anyone steer, owner or not, at any bond")
    void aBridleSteersForAnyone() {
        assertEquals(BRIDLE, steers(true, true, true, false, false, 0, false, true));
        assertEquals(BRIDLE, steers(true, true, true, false, true, 0, false, true));
    }

    @Test
    @DisplayName("bare, only the owner at the top bond tier steers")
    void bareNeedsTheOwnerAndTheBond() {
        assertEquals(BAREBACK, steers(true, true, false, false, true, BAREBACK_TIER, false, true));
        assertEquals(NOBODY, steers(true, true, false, false, true, BAREBACK_TIER - 1, false, true));
        assertEquals(NOBODY, steers(true, true, false, false, false, BAREBACK_TIER, false, true));
    }

    @Test
    @DisplayName("a bridled owner at the top tier is steering by the bridle, not bareback")
    void theBridleWinsOverTheBond() {
        // Matters for the checklist: steer_bareback must not fire for a bridled ride.
        assertEquals(BRIDLE, steers(true, true, true, false, true, BAREBACK_TIER, false, true));
    }

    @Test
    @DisplayName("a bolting horse, an untamed one, or no player aboard: nobody steers")
    void nobodySteers() {
        assertEquals(NOBODY, steers(true, true, true, false, true, BAREBACK_TIER, true, true));
        assertEquals(NOBODY, steers(true, false, true, false, true, BAREBACK_TIER, false, true));
        assertEquals(NOBODY, steers(false, true, true, false, true, BAREBACK_TIER, false, true));
    }

    @Test
    @DisplayName("a saddle with no bridle carries a passenger below the bond tier, owner or not (#74)")
    void aSaddleAloneIsAPassengerSeat() {
        assertEquals(NOBODY, steers(true, true, false, true, false, 0, false, true));
        assertEquals(NOBODY, steers(true, true, false, true, true, BAREBACK_TIER - 1, false, true));
    }

    @Test
    @DisplayName("the top-bond owner steers through a saddle too, and a bridle still wins")
    void theBondAndTheBridleWorkThroughASaddle() {
        assertEquals(BAREBACK, steers(true, true, false, true, true, BAREBACK_TIER, false, true));
        assertEquals(BRIDLE, steers(true, true, true, true, false, 0, false, true));
    }

    @Test
    @DisplayName("with the server's saddle rule off, a saddle steers for anyone - and nothing worn still does not")
    void theSwitchGivesTheSaddleBackItsReins() {
        assertEquals(SADDLE, steers(true, true, false, true, false, 0, false, false));
        assertEquals(SADDLE, steers(true, true, false, true, true, BAREBACK_TIER, false, false));
        assertEquals(NOBODY, steers(true, true, false, false, false, 0, false, false));
    }

    @Test
    @DisplayName("a bolting or untamed horse withholds a saddle's reins too, whatever the switch says")
    void aSaddleNeverOutranksBoltingOrTaming() {
        assertEquals(NOBODY, steers(true, true, false, true, true, BAREBACK_TIER, true, false));
        assertEquals(NOBODY, steers(true, false, false, true, true, BAREBACK_TIER, false, false));
    }
}
