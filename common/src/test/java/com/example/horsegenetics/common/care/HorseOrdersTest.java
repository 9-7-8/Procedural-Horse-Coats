package com.example.horsegenetics.common.care;

import com.example.horsegenetics.common.care.HorseOrders.Refusal;
import com.example.horsegenetics.common.care.HorseOrders.Situation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The command whistle's gate: the lead and the cart before the bond, every order against
 * every bond tier, Rejoin herd never refused; the saved name's fallback; the summary line.
 */
class HorseOrdersTest {

    private static Situation free(int tier) {
        return new Situation(tier, false, false);
    }

    @Test
    void eachOrderNeedsItsBondTier() {
        for (HorseOrder order : HorseOrder.values()) {
            for (int tier = 0; tier <= 3; tier++) {
                Refusal r = HorseOrders.refusal(order, free(tier));
                if (tier >= order.bondTier()) {
                    assertNull(r, order + " at tier " + tier);
                } else {
                    assertEquals(Refusal.NOT_BONDED, r, order + " at tier " + tier);
                }
            }
        }
    }

    @Test
    void theTiersAreTheOwnersCalls() {
        // Stay and Follow need tier 1 (bond 31+); every other order tier 2 (61+); clearing none.
        assertEquals(1, HorseOrder.STAY.bondTier());
        assertEquals(1, HorseOrder.FOLLOW.bondTier());
        assertEquals(2, HorseOrder.WANDER.bondTier());
        assertEquals(0, HorseOrder.REJOIN_HERD.bondTier());
        assertEquals("Needs bond 61", HorseOrders.bondNeeded(HorseOrder.WANDER));
        assertEquals("Needs bond 31", HorseOrders.bondNeeded(HorseOrder.STAY));
    }

    @Test
    void theLeadAndTheCartWinOverTheBond() {
        // An unbonded horse on a lead says "on a lead": that is what the player can fix first.
        assertEquals(Refusal.ON_A_LEAD, HorseOrders.refusal(HorseOrder.STAY, new Situation(0, true, false)));
        assertEquals(Refusal.PULLING_A_CART, HorseOrders.refusal(HorseOrder.FOLLOW, new Situation(3, false, true)));
        assertEquals(Refusal.ON_A_LEAD, HorseOrders.refusal(HorseOrder.WANDER, new Situation(3, true, true)));
    }

    @Test
    void rejoinHerdIsNeverRefused() {
        assertNull(HorseOrders.refusal(HorseOrder.REJOIN_HERD, new Situation(0, true, true)));
    }

    @Test
    void anUnknownSavedNameLoadsAsNoOrder() {
        assertEquals(HorseOrder.STAY, HorseOrder.byName("STAY"));
        assertEquals(HorseOrder.STAY, HorseOrder.byName(" stay "));
        assertEquals(HorseOrder.REJOIN_HERD, HorseOrder.byName("HUNT_DRAGONS"));
        assertEquals(HorseOrder.REJOIN_HERD, HorseOrder.byName(""));
        assertEquals(HorseOrder.REJOIN_HERD, HorseOrder.byName(null));
    }

    @Test
    void onlyStayIsAnchored() {
        assertTrue(HorseOrder.STAY.anchored());
        assertEquals(1, Arrays.stream(HorseOrder.values()).filter(HorseOrder::anchored).count());
    }

    @Test
    void theSummaryCountsEachReason() {
        String line = HorseOrders.summary(HorseOrder.STAY, Arrays.asList(
                null, Refusal.NOT_BONDED, null, Refusal.ON_A_LEAD, null, Refusal.NOT_BONDED));
        assertEquals("Stay: 3 obeyed; 1 on a lead; 2 not bonded enough.", line);
        assertEquals("Follow: 2 obeyed.", HorseOrders.summary(HorseOrder.FOLLOW, Arrays.asList(null, null)));
        assertEquals("No horses of yours within 16 blocks.", HorseOrders.summary(HorseOrder.STAY, List.of()));
    }

    @Test
    void oneHorseSaysWhatItDid() {
        assertEquals("Bramble stays here.", HorseOrders.oneLine("Bramble", HorseOrder.STAY, null));
        assertEquals("Bramble is not bonded enough.",
                HorseOrders.oneLine("Bramble", HorseOrder.WANDER, Refusal.NOT_BONDED));
    }

    @Test
    void theWheelHoldsEveryOrderOnce() {
        assertEquals(HorseOrder.values().length, HorseOrders.wheel().size());
        assertEquals(HorseOrders.wheel().size(), HorseOrders.wheel().stream().distinct().count());
    }
}
