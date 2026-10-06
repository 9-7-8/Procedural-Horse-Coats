package com.example.horsegenetics.common.parts;

/**
 * <b>How big a horse's crystal growths are</b> - three classes on the {@code size}
 * epigenetic number, the {@link SpineSize} arrangement: the class picks the mesh (a
 * bigger class is a chunkier crystal, not only a longer one), the remainder grows each
 * cluster within it. A length is the tallest crystal's in a cluster, in model units.
 * A first placement, not a spec.
 */
public enum CrystalSize {

    /** Points that barely clear the coat. */
    SMALL("Small"),
    /** Crystals about as tall as an ear. */
    LARGE("Large"),
    /** The largest the renderer draws - a geode's worth on the back. */
    GREAT("Great");

    /** Length at {@code size = 0}. */
    public static final float SHORTEST = 2.0f;
    /** Length at {@code size = 1}. */
    public static final float LONGEST = 9.0f;

    private final String label;

    CrystalSize(String label) {
        this.label = label;
    }

    /** How many classes - the crystal kind's size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a length, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static CrystalSize of(double s) {
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
