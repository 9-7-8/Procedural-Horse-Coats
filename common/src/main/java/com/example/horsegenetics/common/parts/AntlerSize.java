package com.example.horsegenetics.common.parts;

/**
 * <b>How huge a rack is</b> - the five size classes an antler's {@code size}
 * epigenetic number steps through, and the beam length that number slides along.
 *
 * <p>The {@link HornSize} move applied to antlers, with one difference that is
 * the point: here the classes <b>are</b> steps in the geometry, not only names. A
 * yearling's two-segment spike and a massive stag's seven-segment beam carrying
 * eight tines are different meshes, not one mesh scaled - "a big rack is more
 * segments and tines, not a stretched small one" (owner's treatment). So the
 * floor of {@code size x 5} picks the class, which is the {@code PartShape} size
 * bucket, and the remainder stretches within it, which is a free transform. One
 * number, no seam: the stretch lands every position on its exact length, so a
 * line bred bigger over generations grows through a class boundary without a pop.
 *
 * <p>Lengths are the beam, in model units, from the skull to its end - the
 * adult head box is 7 long. They are a <b>first placement, not a spec</b>: how big
 * a rack can get before it clips a low ceiling, a doorway or the rider's view is a
 * question for a live horse, and it is on the antler page's Verification tab.
 */
public enum AntlerSize {

    /** A pair of bony spikes, or a spike and a fork - a first rack. */
    YEARLING("Yearling"),
    /** A young stag's: a beam and a couple of tines. */
    SMALL("Small"),
    /** A full rack, as tall again as the head is long. */
    STANDARD("Standard"),
    /** A rack that makes a doorway a question. */
    LARGE("Large"),
    /** The largest the renderer draws, and the reason the clamp exists. */
    MASSIVE("Massive");

    /** Beam length at {@code size = 0}. */
    public static final float SHORTEST = 5.0f;
    /** Beam length at {@code size = 1}. */
    public static final float LONGEST = 27.0f;

    private final String label;

    AntlerSize(String label) {
        this.label = label;
    }

    /** How many classes there are - the antler kinds' {@code PartShape} size buckets. */
    public static int classes() {
        return values().length;
    }

    /**
     * {@code s} in {@code [0,1]} to a beam length, geometrically - length is seen as
     * a ratio, so a linear ladder would make the bottom classes indistinguishable.
     * Clamped rather than extrapolated: {@code EpiValue}'s hard bound lets a drifted
     * number arrive outside {@code [0,1]}, and an antler a chunk wide is a hazard.
     */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static AntlerSize of(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return values()[Math.min(classes() - 1, (int) (c * classes()))];
    }

    /** The beam length at this class's middle - the length its mesh is baked at. */
    public float length() {
        return lengthFor((ordinal() + 0.5) / classes());
    }

    /** What the horse screen and the wiki call it. */
    public String label() {
        return label;
    }
}
