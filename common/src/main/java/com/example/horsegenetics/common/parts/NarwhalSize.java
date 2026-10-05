package com.example.horsegenetics.common.parts;

/**
 * <b>Named narwhal horn lengths</b>, in model units, from the front of the muzzle to
 * the point. The adult head and muzzle together are about twelve units long, so
 * {@link #SHORT} already reaches past the end of the face and {@link #GREAT} is longer
 * than the body is deep.
 *
 * <p>The unicorn horn's ladder ({@link HornSize}) starts at a nub, and a nub on the end
 * of the nose reads as a wart, not a tusk. This one starts long and goes further: the
 * treatment's "longer length ceiling". Where the ceiling should sit is a call for the
 * owner's eyes in the dev yard - it has to read as a long tusk without the horse
 * looking like a unicorn on its face - so these are a first proposal, not a tuned set.
 *
 * <p>As with the horn, these are names, not steps: the length is continuous and
 * {@link #lengthFor} is the ladder it slides along, geometric for the same reason.
 */
public enum NarwhalSize {

    /** Just past the end of the face. */
    SHORT(10.0f, "Short"),
    /** A tusk you walk around rather than past. */
    LONG(17.0f, "Long"),
    /** The ceiling, and the reason it exists: longer than the body is deep. */
    GREAT(30.0f, "Great");

    /** Model units from the muzzle to the point. */
    public final float length;

    private final String label;

    NarwhalSize(float length, String label) {
        this.length = length;
        this.label = label;
    }

    /**
     * {@code s} in {@code [0,1]} to a length between {@link #SHORT} and {@link #GREAT},
     * geometric and clamped - an epigenetic value can drift well outside its range, and
     * a tusk a chunk long is a hazard (the {@link HornSize#lengthFor} note).
     */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORT.length * Math.pow(GREAT.length / SHORT.length, c));
    }

    /** The name closest to {@code length}, by ratio, as {@link HornSize#of} measures. */
    public static NarwhalSize of(double length) {
        NarwhalSize best = SHORT;
        double bestGap = Double.MAX_VALUE;
        for (NarwhalSize size : values()) {
            double gap = Math.abs(Math.log(Math.max(0.01, length) / size.length));
            if (gap < bestGap) {
                bestGap = gap;
                best = size;
            }
        }
        return best;
    }

    /** What the horse screen calls it - "a long narwhal horn". */
    public String label() {
        return label;
    }
}
