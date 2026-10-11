package com.example.horsegenetics.common.gear;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The outline a worn piece's rim stands on, and the floor every worn piece obeys.
 *
 * <p>The counts are the point: a rim is one wall per run, so a shape whose runs
 * do not merge costs a face per texel, and a run that is missed is a hole in
 * the side of a piece.
 */
class RimOutlineTest {

    private static boolean[][] grid(String... rows) {
        boolean[][] grid = new boolean[rows.length][];
        for (int v = 0; v < rows.length; v++) {
            grid[v] = new boolean[rows[v].length()];
            for (int u = 0; u < rows[v].length(); u++) {
                grid[v][u] = rows[v].charAt(u) == '#';
            }
        }
        return grid;
    }

    private static long count(RimOutline.Outline outline, RimOutline.Side side) {
        return outline.runs().stream().filter(run -> run.side() == side).count();
    }

    @Test
    @DisplayName("a filled rectangle is four runs, each the whole side")
    void aFilledRectangleIsFourRuns() {
        RimOutline.Outline outline = RimOutline.trace(grid(
                ".....",
                ".###.",
                ".###.",
                "....."));
        assertEquals(List.of(
                new RimOutline.Run(RimOutline.Side.TOP, 1, 1, 4),
                new RimOutline.Run(RimOutline.Side.BOTTOM, 2, 1, 4),
                new RimOutline.Run(RimOutline.Side.LEFT, 1, 1, 3),
                new RimOutline.Run(RimOutline.Side.RIGHT, 3, 1, 3)), outline.runs());
        assertEquals(6, outline.painted());
    }

    @Test
    @DisplayName("a one-texel hole adds an inner loop of four")
    void aHoleIsAnInnerLoop() {
        RimOutline.Outline outline = RimOutline.trace(grid(
                "###",
                "#.#",
                "###"));
        assertEquals(8, outline.runs().size());
        // The hole's four walls face inward: each side of the loop is one texel long.
        assertTrue(outline.runs().contains(new RimOutline.Run(RimOutline.Side.BOTTOM, 0, 1, 2)));
        assertTrue(outline.runs().contains(new RimOutline.Run(RimOutline.Side.TOP, 2, 1, 2)));
        assertTrue(outline.runs().contains(new RimOutline.Run(RimOutline.Side.RIGHT, 0, 1, 2)));
        assertTrue(outline.runs().contains(new RimOutline.Run(RimOutline.Side.LEFT, 2, 1, 2)));
        assertEquals(8, outline.painted());
    }

    @Test
    @DisplayName("an L shape is six runs")
    void anLShapeIsSixRuns() {
        RimOutline.Outline outline = RimOutline.trace(grid(
                "#..",
                "#..",
                "###"));
        assertEquals(6, outline.runs().size());
        assertEquals(2, count(outline, RimOutline.Side.TOP));
        assertEquals(1, count(outline, RimOutline.Side.BOTTOM));
        assertEquals(1, count(outline, RimOutline.Side.LEFT));
        assertEquals(2, count(outline, RimOutline.Side.RIGHT));
    }

    @Test
    @DisplayName("an empty grid has no runs, and neither does no grid at all")
    void anEmptyGridHasNoRuns() {
        assertEquals(0, RimOutline.trace(grid("...", "...")).runs().size());
        assertEquals(0, RimOutline.trace(grid("...", "...")).painted());
        assertEquals(0, RimOutline.trace(new boolean[0][]).runs().size());
    }

    @Test
    @DisplayName("a fully painted face has runs only where the face ends")
    void aFullFaceEndsAtItsEdges() {
        RimOutline.Outline alone = RimOutline.trace(grid("###", "###"));
        assertEquals(List.of(
                new RimOutline.Run(RimOutline.Side.TOP, 0, 0, 3),
                new RimOutline.Run(RimOutline.Side.BOTTOM, 1, 0, 3),
                new RimOutline.Run(RimOutline.Side.LEFT, 0, 0, 2),
                new RimOutline.Run(RimOutline.Side.RIGHT, 2, 0, 2)), alone.runs());

        // The same face, where the piece carries on past every end: no wall at all.
        assertEquals(0, RimOutline.trace(3, 2, (u, v) -> true).runs().size());

        // And where it carries on past the right-hand end only.
        RimOutline.Outline wrapped = RimOutline.trace(3, 2, (u, v) -> u >= 0 && v >= 0 && v < 2);
        assertEquals(0, count(wrapped, RimOutline.Side.RIGHT));
        assertEquals(3, wrapped.runs().size());
    }

    @Test
    @DisplayName("a checkerboard does not merge: one run per texel side")
    void aCheckerboardDoesNotMerge() {
        RimOutline.Outline outline = RimOutline.trace(grid(
                "#.#.",
                ".#.#",
                "#.#.",
                ".#.#"));
        assertEquals(8 * 4, outline.runs().size());
    }

    @Test
    @DisplayName("the same grid twice gives the same runs in the same order")
    void tracingIsDeterministic() {
        boolean[][] shape = grid("##.#", "#..#", "####", ".##.");
        assertEquals(RimOutline.trace(shape), RimOutline.trace(shape));
    }

    @Test
    @DisplayName("a worn piece's lift is refused under a quarter of a model unit")
    void theFloorIsAQuarterUnit() {
        assertEquals(0.25f, WornRule.FLOOR);
        assertEquals(0.25f, WornRule.lift(0.25f));
        assertEquals(0.9f, WornRule.lift(0.9f));
        assertThrows(IllegalArgumentException.class, () -> WornRule.lift(0.24f));
        assertThrows(IllegalArgumentException.class, () -> WornRule.lift(0f));
        assertThrows(IllegalArgumentException.class, () -> WornRule.lift(-1f));
        assertThrows(IllegalArgumentException.class, () -> WornRule.lift(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> WornRule.lift(Float.POSITIVE_INFINITY));
    }
}
