package com.example.horsegenetics.common.horse;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Issue #272: /horsebank files the oldest horses first, in the player's chamber order. */
class HorseBankPlanTest {

    private static final UUID OLD = new UUID(0, 1);
    private static final UUID MID = new UUID(0, 2);
    private static final UUID YOUNG = new UUID(0, 3);
    private static final UUID MYSTERY = new UUID(0, 4);

    private static List<HorseBankPlan.Candidate> herd() {
        List<HorseBankPlan.Candidate> h = new ArrayList<>();
        h.add(new HorseBankPlan.Candidate(YOUNG, 9_000));
        h.add(new HorseBankPlan.Candidate(OLD, 100));
        h.add(new HorseBankPlan.Candidate(MID, 5_000));
        return h;
    }

    private static Map<String, Integer> empties(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }

    @Test
    void oldestFirstUntilTheChambersRunOut() {
        HorseBankPlan.Plan p = HorseBankPlan.plan(herd(), empties("basic", 2), HorseBankPlan.DEFAULT_ORDER, false);
        assertEquals(2, p.filings().size());
        assertEquals(OLD, p.filings().get(0).horse());
        assertEquals(MID, p.filings().get(1).horse());
        assertEquals(1, p.leftOut(), "the youngest is left out");
    }

    @Test
    void anUnknownBirthCountsAsOldest() {
        List<HorseBankPlan.Candidate> h = herd();
        h.add(new HorseBankPlan.Candidate(MYSTERY, HorseBankPlan.UNKNOWN_BIRTH));
        HorseBankPlan.Plan p = HorseBankPlan.plan(h, empties("basic", 1), HorseBankPlan.DEFAULT_ORDER, false);
        assertEquals(MYSTERY, p.filings().get(0).horse());
    }

    @Test
    void chambersAreUsedInThePlayersOrder() {
        Map<String, Integer> e = empties("spacer", 1, "basic", 1, "advanced", 1);
        HorseBankPlan.Plan cheap = HorseBankPlan.plan(herd(), e, HorseBankPlan.DEFAULT_ORDER, false);
        assertEquals("basic", cheap.filings().get(0).kind(), "the oldest gets the first kind in the order");
        assertEquals("advanced", cheap.filings().get(1).kind());
        assertEquals("spacer", cheap.filings().get(2).kind());
        HorseBankPlan.Plan best = HorseBankPlan.plan(herd(), e, List.of("spacer", "advanced", "basic"), false);
        assertEquals("spacer", best.filings().get(0).kind());
    }

    @Test
    void emergencyChambersAreSkippedUnlessListed() {
        HorseBankPlan.Plan p = HorseBankPlan.plan(herd(), empties("emergency", 3), HorseBankPlan.DEFAULT_ORDER, false);
        assertTrue(p.filings().isEmpty());
        HorseBankPlan.Plan q = HorseBankPlan.plan(herd(), empties("emergency", 3), List.of("emergency"), false);
        assertEquals(3, q.filings().size());
    }

    @Test
    void forceMakesBasicsForTheRest() {
        HorseBankPlan.Plan p = HorseBankPlan.plan(herd(), empties("advanced", 1), HorseBankPlan.DEFAULT_ORDER, true);
        assertEquals(3, p.filings().size());
        assertEquals("advanced", p.filings().get(0).kind(), "the player's own empties first");
        assertNull(p.filings().get(1).kind(), "then new Basics");
        assertTrue(p.filings().get(2).madeNew());
        assertEquals(0, p.leftOut());
    }

    @Test
    void aNewBankGoesInFrontOfThePlayer() {
        List<int[]> east = HorseBankPlan.placeOrder(10, 1, 0);
        int[] first = east.get(0);
        assertEquals(2, first[0]);
        assertEquals(0, first[1]);
        assertEquals(0, first[2]);
        List<int[]> north = HorseBankPlan.placeOrder(10, 0, -1);
        assertEquals(-2, north.get(0)[2]);
        for (int[] o : east) {
            assertTrue(o[0] * o[0] + o[1] * o[1] + o[2] * o[2] <= 100, "inside the radius");
            assertTrue(!(o[0] == 0 && o[2] == 0 && (o[1] == 0 || o[1] == 1)), "never where the player stands");
        }
    }

    @Test
    void aBadOrderListFallsBackToTheDefault() {
        assertEquals(HorseBankPlan.DEFAULT_ORDER, HorseBankPlan.cleanOrder(List.of("gold", "")));
        assertEquals(HorseBankPlan.DEFAULT_ORDER, HorseBankPlan.cleanOrder(null));
        assertEquals(List.of("spacer", "basic"), HorseBankPlan.cleanOrder(List.of("Spacer", "basic", "spacer")));
    }
}
