package com.example.horsegenetics.common.parts;

/**
 * <b>How big a ram's horn is</b> - five classes on the {@code size} epigenetic number,
 * the {@link AntlerSize} arrangement: the class picks the mesh (a bigger horn is
 * more segments, so a full curl stays smooth), the remainder stretches within it.
 * Lengths are along the horn, in model units, so a curl's are long - it wraps round.
 * A first placement, not a spec.
 */
public enum RamHornSize {

    /** A lamb's buds - barely out of the skull. */
    BUD("Bud"),
    /** A young ram's: half a turn at most. */
    SMALL("Small"),
    /** A grown ram's. */
    STANDARD("Standard"),
    /** A heavy, full-curled ram's. */
    HEAVY("Heavy"),
    /** The largest the renderer draws - a bighorn's. */
    MASSIVE("Massive");

    /** Length at {@code size = 0}. */
    public static final float SHORTEST = 4.0f;
    /** Length at {@code size = 1}. */
    public static final float LONGEST = 34.0f;

    private final String label;

    RamHornSize(String label) {
        this.label = label;
    }

    /** How many classes - the ram horn kinds' size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a length, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static RamHornSize of(double s) {
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
