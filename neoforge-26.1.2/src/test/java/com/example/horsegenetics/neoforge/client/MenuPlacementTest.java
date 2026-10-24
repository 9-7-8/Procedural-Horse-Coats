package com.example.horsegenetics.neoforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>A query dropdown stays on the screen (#266).</b>
 *
 * <p>The layout audit's window was 427x240. A column menu has hundreds of options, so
 * it wants twelve rows plus a header and a hint: 168 px. Opened under a cell low on
 * that screen it ran off the bottom, and its "N more" hint was drawn under the box,
 * on the panel behind.
 */
class MenuPlacementTest {

    private static final int ROW = 12;
    private static final int MAX = 12;
    private static final int SCREEN = 240;

    @Test
    @DisplayName("a cell anywhere on a 240 px screen gets a menu that fits it")
    void fitsAShortScreen() {
        for (int cellTop = 0; cellTop + 14 <= SCREEN; cellTop += 2) {
            MenuPlacement m = MenuPlacement.of(cellTop, cellTop + 14, SCREEN, ROW, MAX, 390);
            assertTrue(m.top() >= MenuPlacement.MARGIN,
                    "cell at " + cellTop + ": menu starts at " + m.top());
            assertTrue(m.top() + m.height(ROW) <= SCREEN - MenuPlacement.MARGIN,
                    "cell at " + cellTop + ": menu ends at " + (m.top() + m.height(ROW)));
            assertTrue(m.top() >= cellTop + 14 || m.top() + m.height(ROW) <= cellTop,
                    "cell at " + cellTop + ": menu covers its own cell");
            assertTrue(m.hint(), "390 options and the hint row is gone");
        }
    }

    @Test
    @DisplayName("a low cell opens its menu above, with the full twelve rows when they fit")
    void lowCellOpensAbove() {
        MenuPlacement m = MenuPlacement.of(200, 214, SCREEN, ROW, MAX, 390);
        assertEquals(200 - m.height(ROW), m.top(), "should sit right above the cell");
        assertEquals(MAX, m.rows());
    }

    @Test
    @DisplayName("on a tall screen nothing moves: under the cell, twelve rows")
    void tallScreenUnchanged() {
        MenuPlacement m = MenuPlacement.of(200, 214, 1080, ROW, MAX, 390);
        assertEquals(214, m.top());
        assertEquals(MAX, m.rows());
        assertTrue(m.hint());
    }

    @Test
    @DisplayName("a short list has no hint row, and caps to the room under a mid cell")
    void shortListAndCap() {
        MenuPlacement two = MenuPlacement.of(100, 114, SCREEN, ROW, MAX, 2);
        assertEquals(114, two.top());
        assertEquals(2, two.rows());
        assertFalse(two.hint());

        // 106 px each side of a mid cell: fewer than twelve rows either way, so capped.
        MenuPlacement mid = MenuPlacement.of(108, 122, SCREEN, ROW, MAX, 390);
        assertTrue(mid.rows() < MAX && mid.rows() >= 1);
        assertTrue(mid.top() + mid.height(ROW) <= SCREEN - MenuPlacement.MARGIN);
    }
}
