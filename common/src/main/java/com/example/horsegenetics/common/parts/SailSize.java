package com.example.horsegenetics.common.parts;

/**
 * <b>How tall a back sail is</b> - four classes on the {@code length} epigenetic
 * number, the {@link SpineSize} arrangement: the class picks the mesh (a taller sail
 * is more boxes per spine, so its taper stays smooth), the remainder stretches each
 * spine and its membrane within it. A length is the tallest spine's, in model units -
 * the peak of a tall sail, the crest of a low one. A first placement, not a spec.
 */
public enum SailSize {

    /** A fin that barely clears the back. */
    LOW("Low"),
    /** A sail about as tall as the head is long. */
    STANDARD("Standard"),
    /** A sail that stands over the rider's knee. */
    TALL("Tall"),
    /** The largest the renderer draws - a Spinosaurus crest, near a block high. */
    TOWERING("Towering");

    /** Height at {@code length = 0}. */
    public static final float SHORTEST = 3.0f;
    /** Height at {@code length = 1}. */
    public static final float LONGEST = 14.0f;

    private final String label;

    SailSize(String label) {
        this.label = label;
    }

    /** How many classes - the sail kind's size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a height, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static SailSize of(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return values()[Math.min(classes() - 1, (int) (c * classes()))];
    }

    /** The height this class's mesh is baked at - its middle. */
    public float length() {
        return lengthFor((ordinal() + 0.5) / classes());
    }

    /** What the wiki calls it. */
    public String label() {
        return label;
    }
}
