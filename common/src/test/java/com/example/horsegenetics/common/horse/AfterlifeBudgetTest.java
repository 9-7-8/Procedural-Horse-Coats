package com.example.horsegenetics.common.horse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The afterlife store's size budget, without a game</b> (issue #15): a dead
 * horse is kept until the store is full, then the oldest deaths go first, and
 * only as many as it takes.
 */
class AfterlifeBudgetTest {

    private static AfterlifeBudget.Entry<String> e(String id, long diedAt, long bytes) {
        return new AfterlifeBudget.Entry<>(id, diedAt, bytes);
    }

    /** Under the budget, nothing goes, however long ago anything died. */
    @Test
    void underBudgetKeepsEverything() {
        List<AfterlifeBudget.Entry<String>> kept = List.of(
                e("ancient", 0, 400), e("old", 1_000_000, 400), e("new", 9_000_000, 200));
        assertTrue(AfterlifeBudget.overflow(kept, 1000).isEmpty());
    }

    /** Exactly at the budget is within it. */
    @Test
    void exactlyAtBudgetKeepsEverything() {
        assertTrue(AfterlifeBudget.overflow(List.of(e("a", 1, 500), e("b", 2, 500)), 1000).isEmpty());
    }

    /** Over it, the oldest death goes first, in whatever order they were given. */
    @Test
    void overBudgetDropsTheOldestFirst() {
        List<AfterlifeBudget.Entry<String>> kept = List.of(
                e("middle", 200, 400), e("newest", 300, 400), e("oldest", 100, 400));
        assertEquals(List.of("oldest"), AfterlifeBudget.overflow(kept, 1000));
    }

    /** Only as many as it takes: one big old horse can make room on its own. */
    @Test
    void dropsOnlyAsManyAsItTakes() {
        List<AfterlifeBudget.Entry<String>> kept = List.of(
                e("a", 1, 100), e("b", 2, 100), e("c", 3, 100), e("d", 4, 100), e("e", 5, 100));
        assertEquals(List.of("a", "b"), AfterlifeBudget.overflow(kept, 300));
    }

    /** A budget of 0 is "no cap", not "keep nothing". */
    @Test
    void zeroBudgetIsNoCap() {
        assertTrue(AfterlifeBudget.overflow(List.of(e("a", 1, 1L << 40)), 0).isEmpty());
    }

    /** A single entry bigger than the whole budget is dropped rather than kept for ever. */
    @Test
    void oneEntryOverTheWholeBudgetGoes() {
        assertEquals(List.of("huge"), AfterlifeBudget.overflow(List.of(e("huge", 1, 2000)), 1000));
    }

    @Test
    void totalAddsTheBytes() {
        assertEquals(700, AfterlifeBudget.total(List.of(e("a", 1, 300), e("b", 2, 400))));
    }
}
