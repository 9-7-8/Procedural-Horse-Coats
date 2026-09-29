package com.example.horsegenetics.common.care;

import com.example.horsegenetics.common.care.WhistleSelection.Candidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which whistles one key press blows: the largest area whistle and no other,
 * every distinct binding, and nothing twice.
 */
class WhistleSelectionTest {

    private static Candidate area(int slot, int radius) {
        return new Candidate(slot, radius, null);
    }

    private static Candidate ender(int slot, String horse) {
        return new Candidate(slot, 0, horse);
    }

    private static List<Integer> slots(List<Candidate> picked) {
        return picked.stream().map(Candidate::slot).toList();
    }

    @Test
    void nothingCarriedBlowsNothing() {
        assertTrue(WhistleSelection.pick(List.of()).isEmpty());
    }

    @Test
    void onlyTheLargestAreaWhistleIsBlown() {
        // The whole point: three tiers on one belt is one blow, not three
        // cooldowns spent doing what the echo whistle did by itself.
        List<Candidate> picked = WhistleSelection.pick(List.of(area(0, 16), area(1, 64), area(2, 32)));
        assertEquals(List.of(1), slots(picked));
    }

    @Test
    void twoAreaWhistlesOfOneTierBlowOnce() {
        assertEquals(List.of(3), slots(WhistleSelection.pick(List.of(area(3, 32), area(7, 32)))));
    }

    @Test
    void everyDistinctBindingIsBlown() {
        List<Candidate> picked = WhistleSelection.pick(
                List.of(ender(0, "biscuit"), ender(1, "juniper"), ender(2, "marrow")));
        assertEquals(List.of(0, 1, 2), slots(picked));
    }

    @Test
    void twoWhistlesBoundToOneHorseCallItOnce() {
        // Binding is per whistle, not per horse, so this is a state a player can
        // really be in - and calling the horse twice would chime twice for it.
        assertEquals(List.of(0), slots(WhistleSelection.pick(
                List.of(ender(0, "biscuit"), ender(4, "biscuit")))));
    }

    @Test
    void anUnboundEnderWhistleIsNotBlown() {
        assertTrue(WhistleSelection.pick(List.of(ender(0, null), ender(1, "   "))).isEmpty());
    }

    @Test
    void theAreaWhistleComesFirstSoTheDedupeHasSomethingToWorkWith() {
        // The caller does the area call, collects the horses it handled, and
        // skips an ender whistle bound to one of them. That only works if the
        // area whistle is blown first, so the order here is a contract.
        List<Candidate> picked = WhistleSelection.pick(
                List.of(ender(0, "biscuit"), area(1, 64), ender(2, "juniper")));
        assertEquals(List.of(1, 0, 2), slots(picked));
        assertTrue(picked.get(0).isArea(), "the area whistle must be blown first, wherever it was carried");
    }

    @Test
    void areaAndEnderMix() {
        List<Candidate> picked = WhistleSelection.pick(
                List.of(area(0, 16), ender(1, "biscuit"), area(2, 64), ender(3, "biscuit"), ender(4, "juniper")));
        assertEquals(List.of(2, 1, 4), slots(picked));
    }
}
