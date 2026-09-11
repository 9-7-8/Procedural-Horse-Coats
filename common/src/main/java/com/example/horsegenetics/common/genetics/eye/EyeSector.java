package com.example.horsegenetics.common.genetics.eye;

import com.example.horsegenetics.common.genetics.EyePatch;

/**
 * <b>Which part of one iris the second colour covers</b> - the ten named
 * sectors the two heterochromia-sector loci are written in, and the reason
 * sectoral heterochromia is now something a horse <i>inherits</i> rather than
 * something rolled off whichever white locus happened to win.
 *
 * <h2>Four texels, named from the viewer's side</h2>
 * An adult iris on the coat sheet is a {@code 2x2} block, so a sector is a
 * four-bit quadrant mask - the same {@link EyePatch} primitive both kinds of
 * heterochromia have always been drawn with. What is new is that the masks are
 * <b>named</b>, because an allele has to be nameable:
 *
 * <pre>
 *   bit 0  bit 1      upper left   upper right
 *   bit 2  bit 3      lower left   lower right
 * </pre>
 *
 * <h2>The mirror</h2>
 * The two eyes' faces are mirrored on the sheet (see
 * {@code CoatRegions.eyeRects}), so bit 0 is <i>not</i> the same corner of the
 * horse on both eyes. That did not matter while a wedge was a random shape
 * nobody had named; it matters now that a player can breed for "upper left" and
 * expect upper left. {@link #maskFor} flips the mask horizontally for the eye
 * on the head's east face so a named sector means the same thing on both.
 *
 * <p><b>Unverified:</b> which of the two faces needs the flip is derived from
 * {@code CoatRegions}' own note that the template's raw column order is
 * mirrored between the eyes, not from a render. If a bred pair of "upper left"
 * eyes comes out mirrored in game, {@link #MIRRORED_EYE} is the one line to
 * change.
 *
 * <h2>The two diagonals</h2>
 * {@link #UPPER_RIGHT_LOWER_LEFT} and {@link #UPPER_LEFT_LOWER_RIGHT} are the
 * two masks {@link EyePatch#WEDGES} deliberately leaves out, on the grounds
 * that at four texels a diagonal reads as two stray pixels rather than as a
 * wedge of colour. They are alleles here all the same, by the owner's call:
 * the whole point of naming the sectors was to let a breeder aim at one, and a
 * sector nobody can reach is not a shape argument, it is a missing allele.
 */
public enum EyeSector {

    /** The wild type of both sector loci: one colour, no sector at all. */
    WILD("WT", "Wild type", EyePatch.NONE),

    UPPER_LEFT("UL", "Upper left", 0b0001),
    TOP("Top", "Top", 0b0011),
    UPPER_RIGHT("UR", "Upper right", 0b0010),
    RIGHT("Rt", "Right", 0b1010),
    LOWER_RIGHT("LR", "Lower right", 0b1000),
    BOTTOM("Bot", "Bottom", 0b1100),
    LOWER_LEFT("LL", "Lower left", 0b0100),
    LEFT("Lf", "Left", 0b0101),
    UPPER_RIGHT_LOWER_LEFT("URLL", "Upper right + lower left", 0b0110),
    UPPER_LEFT_LOWER_RIGHT("ULLR", "Upper left + lower right", 0b1001);

    /**
     * The {@code CoatRegions.eyeRects} index whose quadrants are flipped in
     * {@code x} before painting, so that a named sector is the same corner of
     * the <b>horse</b> on both eyes. See the class note - this is the one line
     * to change if it comes out backwards.
     */
    public static final int MIRRORED_EYE = com.example.horsegenetics.common.coat.pattern.CoatRegions.LEFT_EYE;

    private final String token;
    private final String label;
    private final int mask;

    EyeSector(String token, String label, int mask) {
        this.token = token;
        this.label = label;
        this.mask = mask;
    }

    public String token() {
        return token;
    }

    public String label() {
        return label;
    }

    /** The raw quadrant mask, in <b>texture</b> space for the unmirrored eye. */
    public int mask() {
        return mask;
    }

    /** Nothing to paint - the wild type. */
    public boolean empty() {
        return mask == EyePatch.NONE;
    }

    /**
     * This sector's mask for one eye, mirrored if that eye's face is.
     *
     * @param eye a {@code CoatRegions.eyeRects} index
     */
    public int maskFor(int eye) {
        return eye == MIRRORED_EYE ? mirrorX(mask) : mask;
    }

    /** Swap the left and right columns of a {@code 2x2} quadrant mask. */
    public static int mirrorX(int m) {
        return ((m & 0b0001) << 1) | ((m & 0b0010) >> 1)
                | ((m & 0b0100) << 1) | ((m & 0b1000) >> 1);
    }

    /** The sector under {@code token}, or {@code null}. */
    public static EyeSector byToken(String token) {
        for (EyeSector s : values()) {
            if (s.token.equals(token)) {
                return s;
            }
        }
        return null;
    }

    /** The non-wild sectors, in declaration order - what a founder roll draws from. */
    public static EyeSector[] sectors() {
        EyeSector[] all = values();
        EyeSector[] out = new EyeSector[all.length - 1];
        System.arraycopy(all, 1, out, 0, out.length);
        return out;
    }
}
