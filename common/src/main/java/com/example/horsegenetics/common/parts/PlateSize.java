package com.example.horsegenetics.common.parts;

/**
 * <b>How big a horse's shoulder and hip plates are</b> - three classes on the
 * {@code size} epigenetic number, the {@link SpineSize} arrangement: the class picks
 * the mesh (where the slabs step down the flank, so a bigger plate does not stack off
 * the bottom of the body), the remainder stretches each slab within it. A length is
 * one slab's height down the flank, in model units; its length along the body follows
 * from it ({@link PlateGenerator}). A first placement, not a spec.
 */
public enum PlateSize {

    /** Scales a few units across - a scatter of scutes at the shoulder and hip. */
    SMALL("Small"),
    /** Plates about as tall as a hand's breadth on the flank. */
    BROAD("Broad"),
    /** The largest the renderer draws - pauldrons that cover the shoulder and the hip. */
    MASSIVE("Massive");

    /** Slab height at {@code size = 0}. */
    public static final float SHORTEST = 1.6f;
    /** Slab height at {@code size = 1}. */
    public static final float LONGEST = 4.0f;

    private final String label;

    PlateSize(String label) {
        this.label = label;
    }

    /** How many classes - the plate kinds' size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a slab height, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static PlateSize of(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return values()[Math.min(classes() - 1, (int) (c * classes()))];
    }

    /** The slab height this class's mesh is baked at - its middle. */
    public float length() {
        return lengthFor((ordinal() + 0.5) / classes());
    }

    /** What the wiki calls it. */
    public String label() {
        return label;
    }
}
