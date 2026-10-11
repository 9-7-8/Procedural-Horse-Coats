package com.example.horsegenetics.common.gear;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Where a painted region ends</b>, as straight runs of texel edges.
 *
 * <p>A worn piece is a region of a mesh face picked out by a texture's alpha.
 * To give it thickness the way a held item has thickness, something has to
 * stand on every edge between a painted texel and an unpainted one. This class
 * finds those edges and merges the ones that lie end to end, so a filled
 * rectangle costs four side faces rather than one per texel.
 *
 * <p>It knows nothing about a model or a game: a face is a {@code width} by
 * {@code height} grid and a question, "is this texel painted?". The question is
 * also asked one texel <i>beyond</i> the grid on every side, which is how the
 * caller says what lies past the end of the face - nothing (so the face ends in
 * an edge) or the neighbouring face of the same box (so a piece that wraps a
 * corner gets no wall inside itself).
 */
public final class RimOutline {

    /** Which side of its painted texels a run lies on. */
    public enum Side {
        /** Toward smaller v. A run along a row. */
        TOP,
        /** Toward larger v. A run along a row. */
        BOTTOM,
        /** Toward smaller u. A run along a column. */
        LEFT,
        /** Toward larger u. A run along a column. */
        RIGHT
    }

    /**
     * One straight stretch of boundary.
     *
     * @param side which side of the painted texels it is on
     * @param at   the row (TOP, BOTTOM) or column (LEFT, RIGHT) of the painted
     *             texels it borders - the texels whose colour the rim takes
     * @param from the first texel along the run, inclusive
     * @param to   the end of the run, exclusive
     */
    public record Run(Side side, int at, int from, int to) {
    }

    /**
     * A face's outline.
     *
     * @param runs    every boundary run, in a fixed order (sides in declaration
     *                order, then by row or column, then along it)
     * @param painted how many texels of the face are painted; zero means the
     *                piece does not touch this face at all
     */
    public record Outline(List<Run> runs, int painted) {
    }

    /** Is the texel at ({@code u}, {@code v}) painted? Asked from -1 to the size inclusive. */
    @FunctionalInterface
    public interface Painted {
        boolean at(int u, int v);
    }

    private RimOutline() {
    }

    /** A face whose surroundings are unpainted: {@code grid[v][u]}, every row the same length. */
    public static Outline trace(boolean[][] grid) {
        int height = grid.length;
        int width = height == 0 ? 0 : grid[0].length;
        return trace(width, height,
                (u, v) -> u >= 0 && v >= 0 && u < width && v < height && grid[v][u]);
    }

    /**
     * Trace a {@code width} by {@code height} face.
     *
     * @param painted answers for u in -1..width and v in -1..height; the ring
     *                outside the face is what lies beyond its end
     */
    public static Outline trace(int width, int height, Painted painted) {
        List<Run> runs = new ArrayList<>();
        int count = 0;
        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                if (painted.at(u, v)) {
                    count++;
                }
            }
        }
        if (count == 0) {
            return new Outline(List.of(), 0);
        }
        rows(runs, Side.TOP, width, height, painted, -1);
        rows(runs, Side.BOTTOM, width, height, painted, 1);
        columns(runs, Side.LEFT, width, height, painted, -1);
        columns(runs, Side.RIGHT, width, height, painted, 1);
        return new Outline(List.copyOf(runs), count);
    }

    private static void rows(List<Run> out, Side side, int width, int height, Painted painted, int step) {
        for (int v = 0; v < height; v++) {
            int start = -1;
            for (int u = 0; u <= width; u++) {
                boolean edge = u < width && painted.at(u, v) && !painted.at(u, v + step);
                if (edge && start < 0) {
                    start = u;
                } else if (!edge && start >= 0) {
                    out.add(new Run(side, v, start, u));
                    start = -1;
                }
            }
        }
    }

    private static void columns(List<Run> out, Side side, int width, int height, Painted painted, int step) {
        for (int u = 0; u < width; u++) {
            int start = -1;
            for (int v = 0; v <= height; v++) {
                boolean edge = v < height && painted.at(u, v) && !painted.at(u + step, v);
                if (edge && start < 0) {
                    start = v;
                } else if (!edge && start >= 0) {
                    out.add(new Run(side, u, start, v));
                    start = -1;
                }
            }
        }
    }
}
