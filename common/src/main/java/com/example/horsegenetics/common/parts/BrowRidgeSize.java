package com.example.horsegenetics.common.parts;

/**
 * <b>How tall a horse's brow ridge stands</b> - three classes on the {@code height}
 * epigenetic number, the {@link PlateSize} arrangement: the class picks the mesh, the
 * remainder stretches the ridge within it. A length is the ridge's own height off the
 * skull (and its depth front to back, which the mesh keeps equal), in model units; its
 * width across the head is another number. A first placement, not a spec.
 */
public enum BrowRidgeSize {

    /** A low bar across the brow. */
    LOW("Low"),
    /** A ridge that shades the eyes. */
    HEAVY("Heavy"),
    /** The tallest the renderer draws. */
    TOWERING("Towering");

    /** The ridge height at {@code size = 0}. */
    public static final float SHORTEST = 0.7f;
    /** The ridge height at {@code size = 1}. */
    public static final float LONGEST = 1.7f;

    private final String label;

    BrowRidgeSize(String label) {
        this.label = label;
    }

    /** How many classes - the brow ridge kind' size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a length, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static BrowRidgeSize of(double s) {
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
