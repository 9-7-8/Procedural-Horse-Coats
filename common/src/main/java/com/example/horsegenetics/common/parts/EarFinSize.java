package com.example.horsegenetics.common.parts;

/**
 * <b>How big a horse's ear fins are</b> - two classes on the {@code size} epigenetic
 * number, the {@link PlateSize} arrangement: the class picks the mesh, the remainder is
 * a uniform grow within it. Two rather than three because a fin's proportions do not
 * change with its size - the grow does the work - and the part cache is bounded in
 * boxes. A length is the longest ray's, root to point, in model units; the vanilla ear
 * beside it is three tall. A first placement, not a spec.
 */
public enum EarFinSize {

    /** Fins about as long as the ear they sit under. */
    SMALL("Small"),
    /** Fins that stand well clear of the ears, up to as long as the head is deep. */
    GRAND("Grand");

    /** The longest ray at {@code size = 0}. */
    public static final float SHORTEST = 2.5f;
    /** The longest ray at {@code size = 1}. */
    public static final float LONGEST = 6.5f;

    private final String label;

    EarFinSize(String label) {
        this.label = label;
    }

    /** How many classes - the ear fin kinds' size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a length, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static EarFinSize of(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return values()[Math.min(classes() - 1, (int) (c * classes()))];
    }

    /** The length this class's mesh is baked at - its middle. */
    public float length() {
        return lengthFor((ordinal() + 0.5) / classes());
    }

    /** What the wiki calls it. */
    public String label() {
        return label;
    }
}
