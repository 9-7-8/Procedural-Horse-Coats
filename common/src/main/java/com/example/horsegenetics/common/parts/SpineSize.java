package com.example.horsegenetics.common.parts;

/**
 * <b>How tall a row of dorsal spines is</b> - four classes on the {@code length}
 * epigenetic number, the {@link DragonHornSize} arrangement: the class picks the mesh
 * (a taller spine is more boxes, so its taper stays smooth), the remainder stretches
 * each spine within it. A length is the tallest spine's, in model units - the one at
 * the withers on a graduated row. A first placement, not a spec.
 */
public enum SpineSize {

    /** A low ridge of nubs along the back. */
    LOW("Low"),
    /** A row of short spikes. */
    STANDARD("Standard"),
    /** Spines about as tall as the ears. */
    TALL("Tall"),
    /** The largest the renderer draws - a crest that stands well clear of the back. */
    TOWERING("Towering");

    /** Height at {@code length = 0}. */
    public static final float SHORTEST = 1.5f;
    /** Height at {@code length = 1}. */
    public static final float LONGEST = 9.0f;

    private final String label;

    SpineSize(String label) {
        this.label = label;
    }

    /** How many classes - the spine kind's size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a height, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static SpineSize of(double s) {
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
