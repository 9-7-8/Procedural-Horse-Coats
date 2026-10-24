package com.example.horsegenetics.common.horse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Issue #273: the bank grid always keeps one empty row, and never drops below the old depth. */
class StasisBankGridTest {

    @Test
    void aSmallBankIsTheOldDepth() {
        assertEquals(StasisBankGrid.MIN_ROWS, StasisBankGrid.rowsFor(-1));
        assertEquals(StasisBankGrid.MIN_ROWS, StasisBankGrid.rowsFor(0));
        assertEquals(StasisBankGrid.MIN_ROWS, StasisBankGrid.rowsFor(20 * 9 + 8), "row 21 filled, row 22 spare");
    }

    @Test
    void theLastRowFilledAddsOne() {
        int lastRow = StasisBankGrid.MIN_ROWS - 1;
        assertEquals(StasisBankGrid.MIN_ROWS + 1, StasisBankGrid.rowsFor(lastRow * 9));
        assertTrue(StasisBankGrid.growsAt(lastRow * 9, StasisBankGrid.MIN_ROWS));
        assertFalse(StasisBankGrid.growsAt(lastRow * 9 - 1, StasisBankGrid.MIN_ROWS));
    }

    @Test
    void aHugeBankStillHasASpareRow() {
        int slot = 99_999;
        int rows = StasisBankGrid.rowsFor(slot);
        assertTrue(rows * 9 > slot);
        assertFalse(StasisBankGrid.growsAt(slot, rows), "the row after the filled one is empty");
    }

    @Test
    void anAbsurdIndexDoesNotOverflow() {
        assertTrue(StasisBankGrid.rowsFor(Integer.MAX_VALUE - 1) > 0);
    }
}
