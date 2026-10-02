package com.example.horsegenetics.common.parts;

/**
 * <b>How big a dragon horn is</b> - five classes on the {@code length} epigenetic
 * number, the {@link RamHornSize} arrangement: the class picks the mesh (a longer
 * horn is more segments, so a sweep or a curl stays smooth), the remainder stretches
 * within it. Lengths are along the horn, in model units. A first placement, not a spec.
 */
public enum DragonHornSize {

    /** Two knobs behind the ears - barely out of the skull. */
    BUDDING("Budding"),
    /** Short spikes. */
    SHORT("Short"),
    /** A full pair, reaching back toward the neck. */
    STANDARD("Standard"),
    /** Long - the tips reach the crest of the neck. */
    LONG("Long"),
    /** The largest the renderer draws. */
    GREAT("Great");

    /** Length at {@code length = 0}. */
    public static final float SHORTEST = 3.0f;
    /** Length at {@code length = 1}. */
    public static final float LONGEST = 22.0f;

    private final String label;

    DragonHornSize(String label) {
        this.label = label;
    }

    /** How many classes - the dragon horn kinds' size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a length, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static DragonHornSize of(double s) {
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
