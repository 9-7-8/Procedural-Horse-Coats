package com.example.horsegenetics.common.parts;

/**
 * <b>Named horn lengths</b>, in model units. The adult horse's head box is
 * 6 wide by 5 tall by 7 long, so {@link #STANDARD} is twice the height of the
 * skull it grows out of and {@link #NARWHAL} is longer than the whole head.
 *
 * <h2>These are names, not steps</h2>
 * A horn's length is a <b>continuous</b> epigenetic number, and
 * {@link #lengthFor} is the ladder it slides along - geometric, so every step
 * reads as "bigger" rather than the low end being indistinguishable and the top
 * end doubling. The five names exist for the two jobs a continuous number cannot
 * do: they are what the gene's own description and the wiki page can say out
 * loud, and {@link #of} turns a measured length back into one for the horse
 * screen. Nothing in the geometry branches on them.
 */
public enum HornSize {

    /** Barely out of the skull - a bump you would miss at a distance. */
    NUB(3.0f, "Nub"),
    /** A finger's length; unmistakable close up. */
    SHORT(6.0f, "Short"),
    /** The storybook unicorn: about twice the height of the head. */
    STANDARD(10.0f, "Standard"),
    /** Longer than the head, and it leads the horse into doorways. */
    LONG(15.0f, "Long"),
    /** A tusk. Longer than the skull is long, and the reason the clamp exists. */
    NARWHAL(24.0f, "Narwhal");

    /** Model units from the forehead to the point. */
    public final float length;

    private final String label;

    HornSize(float length, String label) {
        this.length = length;
        this.label = label;
    }

    /**
     * {@code s} in {@code [0,1]} to a length between a nub and a narwhal.
     *
     * <p>Geometric rather than linear, because length is perceived as a ratio: a
     * linear ladder puts eleven of its twenty-one units between LONG and NARWHAL
     * and leaves NUB and SHORT three apart, so the bottom half of the range would
     * all look like the same small horn.
     *
     * <p>Out-of-range values are clamped rather than extrapolated. That is not
     * defensive tidying - {@code EpiValue}'s hard bound is eight design spans
     * wide on purpose, so a line bred for three hundred generations really can
     * arrive here with a number outside {@code [0,1]}, and a horn a chunk long is
     * a rendering hazard rather than a triumph.
     */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (NUB.length * Math.pow(NARWHAL.length / NUB.length, c));
    }

    /**
     * The name closest to {@code length}, measured the way the ladder is built -
     * by ratio, not by difference. A 12-unit horn is nearer STANDARD (10) than
     * LONG (15) on a linear reading and nearer LONG on a geometric one; the
     * geometric answer is the one that matches what the eye says, because the
     * jump from 10 to 12 is a fifth and the jump from 12 to 15 is a quarter.
     */
    public static HornSize of(double length) {
        HornSize best = NUB;
        double bestGap = Double.MAX_VALUE;
        for (HornSize size : values()) {
            double gap = Math.abs(Math.log(Math.max(0.01, length) / size.length));
            if (gap < bestGap) {
                bestGap = gap;
                best = size;
            }
        }
        return best;
    }

    /** What the horse screen calls it - "a standard horn", "a narwhal horn". */
    public String label() {
        return label;
    }
}
