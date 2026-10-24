package com.example.horsegenetics.neoforge.client;

/**
 * <b>Where a dropdown opened from a cell goes, on a screen of a given height.</b>
 *
 * <p>The box is one header row (the type-ahead line), then {@code rows} options,
 * then, when some options are not shown, one hint row ("N more - scroll, or type")
 * inside the box. It opens under its cell when the whole list fits there; otherwise
 * on whichever side holds more rows, capped to the room there is (#266: at 427x240
 * the column list ran off the bottom and its hint was drawn on the panel behind).
 *
 * <p>No Minecraft imports, so the module's plain unit tests can reach it.
 *
 * @param top   the box's top edge
 * @param rows  how many options are shown, at least one
 * @param hint  whether the hint row is drawn, because {@code rows} is fewer than the options
 */
public record MenuPlacement(int top, int rows, boolean hint) {

    /** Kept clear between the box and the screen's edge (the box's own 1 px rim, and a pixel). */
    static final int MARGIN = 2;

    /** The box's height. */
    public int height(int rowH) {
        return (1 + rows + (hint ? 1 : 0)) * rowH;
    }

    /**
     * @param cellTop    the top of the cell the menu belongs to
     * @param cellBottom the bottom of that cell, where a menu under it starts
     * @param screenH    the screen's height in GUI pixels
     * @param rowH       one row's height
     * @param maxRows    the most options ever shown at once
     * @param options    how many options there are (zero still shows one empty row)
     */
    public static MenuPlacement of(int cellTop, int cellBottom, int screenH,
                                   int rowH, int maxRows, int options) {
        int want = Math.max(1, Math.min(maxRows, options));
        int below = rowsIn(screenH - MARGIN - cellBottom, rowH, want, options);
        if (below == want) {
            return new MenuPlacement(cellBottom, want, want < options);
        }
        int above = rowsIn(cellTop - MARGIN, rowH, want, options);
        if (above > below) {
            MenuPlacement up = new MenuPlacement(0, above, above < options);
            return new MenuPlacement(cellTop - up.height(rowH), above, up.hint());
        }
        // Neither side has room for even one row (a screen shorter than a menu): keep it
        // under the cell, one row, and let the screen cut it rather than show nothing.
        int rows = Math.max(1, below);
        return new MenuPlacement(cellBottom, rows, rows < options);
    }

    /** The most option rows, up to {@code want}, whose box fits in {@code space} pixels; 0 if none. */
    private static int rowsIn(int space, int rowH, int want, int options) {
        for (int r = want; r >= 1; r--) {
            int h = (1 + r + (r < options ? 1 : 0)) * rowH;
            if (h <= space) {
                return r;
            }
        }
        return 0;
    }
}
