package com.example.horsegenetics.common.horse;

/**
 * <b>How many rows a stasis bank's chamber grid has</b> (issue #273: a bank with
 * no practical size limit, owner 2026-10-24).
 *
 * <p>The grid never runs out: whenever a chamber lands in its last row, another
 * row is added, so there is always one empty row at the bottom. It never has
 * fewer than {@link #MIN_ROWS}, the depth every bank had before it could grow,
 * so a small bank looks exactly as it always did. It does not shrink while the
 * server runs - an open menu's slots are fixed when it opens, and a grid that lost
 * rows under one would point its slots at nothing - only on load, where trailing
 * empty rows past the minimum are simply not rebuilt.
 */
public final class StasisBankGrid {

    private StasisBankGrid() {
    }

    /** Nine to a row - a chest's own pitch. */
    public static final int COLS = 9;
    /** The depth every bank had before it could grow (207 chambers). */
    public static final int MIN_ROWS = 23;

    /**
     * The rows a grid needs whose highest filled slot is {@code highestFilled}
     * ({@code -1} for an empty grid): enough to hold it, plus one empty row, and
     * never fewer than {@link #MIN_ROWS}.
     */
    public static int rowsFor(int highestFilled) {
        if (highestFilled < 0) {
            return MIN_ROWS;
        }
        long rows = (long) highestFilled / COLS + 2;
        return (int) Math.max(MIN_ROWS, Math.min(rows, Integer.MAX_VALUE / COLS));
    }

    /** Does a chamber put in {@code slot} of a grid {@code rows} deep need another row? */
    public static boolean growsAt(int slot, int rows) {
        return slot >= (rows - 1) * COLS;
    }
}
