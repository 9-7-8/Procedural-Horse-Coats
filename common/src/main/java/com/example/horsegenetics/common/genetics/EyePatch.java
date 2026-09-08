package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

/**
 * <b>Part of one iris, in a colour of its own</b> - the unit both kinds of
 * heterochromia are built out of.
 *
 * <h2>Four quadrants, because that is all there is</h2>
 * An adult iris on the coat sheet is a <b>2&times;2 block</b> of pure black
 * (a foal's is the same block without the sclera beside it). There is no room
 * for a shape in any richer sense, so a sector is a <b>4-bit mask</b> over
 * those four texels:
 *
 * <pre>
 *   bit 0   bit 1        0b0011  the top half
 *   bit 2   bit 3        0b0101  the left half
 *                        0b1000  one corner
 *                        0b0111  three corners
 * </pre>
 *
 * The quadrants are in <i>texture</i> space, and the two eyes' faces are
 * mirrored on the sheet (see {@code CoatRegions.eyeRects}), so "left half" is
 * not the same side of the horse on both eyes. That does not matter for a
 * randomly placed wedge and it is not worth un-mirroring for.
 *
 * <h2>Why only twelve of the fourteen shapes</h2>
 * {@link #WEDGES} lists every mask that is neither empty nor the whole iris
 * <i>except</i> the two diagonals ({@code 0b1001}, {@code 0b0110}). At four
 * texels a diagonal does not read as a wedge of colour in an eye, it reads as
 * two stray pixels - which is a rendering fact about a 2&times;2 block and not
 * a claim about horses.
 *
 * @param quadrants the mask; {@link #NONE} paints nothing, {@link #WHOLE} the
 *                  entire iris
 * @param color     what that part of the iris is
 */
public record EyePatch(int quadrants, EyeColor color) {

    /** No texels at all - a patch that paints nothing. */
    public static final int NONE = 0b0000;

    /** Every texel of the iris - a patch that is really a whole-eye colour. */
    public static final int WHOLE = 0b1111;

    /**
     * Every mask that reads as a <b>sector</b> of an iris: the four halves, the
     * four single corners, and the four three-corner shapes. See the class
     * documentation for the two that are missing and why.
     */
    public static final int[] WEDGES = {
            0b0011, 0b1100, 0b0101, 0b1010, // halves: top, bottom, left, right
            0b0001, 0b0010, 0b0100, 0b1000, // one corner
            0b1110, 0b1101, 0b1011, 0b0111, // three corners
    };

    public EyePatch {
        quadrants &= WHOLE;
    }

    /** Is quadrant {@code i} (0-3, reading order) part of this patch? */
    public boolean covers(int i) {
        return (quadrants & (1 << i)) != 0;
    }

    /** Nothing to paint - either an empty mask or a fully transparent claim. */
    public boolean empty() {
        return quadrants == NONE || color == null;
    }

    /** The rest of the iris - what a two-colour eye paints its second colour on. */
    public static int complement(int quadrants) {
        return ~quadrants & WHOLE;
    }

    /** The wedge at {@code index} - a stored {@code EpiValue.category} index. */
    public static int wedgeAt(int index) {
        return WEDGES[Math.floorMod(index, WEDGES.length)];
    }

    /**
     * A wedge that is <b>guaranteed not to be</b> {@code other} - so a horse
     * with a sector in both eyes has a visibly different shape in each, rather
     * than a one-in-twelve chance of a matching pair that looks like a bug.
     */
    public static int differentWedgeAt(int other, int step) {
        int i = 0;
        while (i < WEDGES.length && WEDGES[i] != other) {
            i++;
        }
        if (i == WEDGES.length) {
            return wedgeAt(step); // not one of ours - nothing to avoid
        }
        return WEDGES[(i + 1 + Math.floorMod(step, WEDGES.length - 1)) % WEDGES.length];
    }

    /** How many distinct steps {@link #differentWedgeAt} can take. */
    public static int wedgeStepCount() {
        return WEDGES.length - 1;
    }

    /** How many wedges there are - the size of a stored wedge category. */
    public static int wedgeCount() {
        return WEDGES.length;
    }
}
