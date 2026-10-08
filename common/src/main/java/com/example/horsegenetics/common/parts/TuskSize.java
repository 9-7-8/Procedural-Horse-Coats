package com.example.horsegenetics.common.parts;

/**
 * <b>How big a boar tusk is</b> - four classes on the tusks locus's {@code length}
 * number, the {@link DragonHornSize} arrangement: the class picks the mesh, the
 * remainder stretches within it. Lengths are along the tusk's curve, in model units.
 * The muzzle is five units deep, so {@link #SMALL} is a nub at the corner of the mouth
 * and {@link #GREAT} curls up past the top of the nose. A first placement, not a spec.
 */
public enum TuskSize {

    /** A nub at the corner of the mouth. */
    SMALL("Small"),
    /** An ordinary boar's tusk, reaching the upper lip. */
    STANDARD("Standard"),
    /** Past the upper lip and up the side of the nose. */
    LONG("Long"),
    /** The largest the renderer draws: over the top of the nose. */
    GREAT("Great");

    /** Length at {@code length = 0}. */
    public static final float SHORTEST = 2.5f;
    /** Length at {@code length = 1}. */
    public static final float LONGEST = 9.0f;

    private final String label;

    TuskSize(String label) {
        this.label = label;
    }

    /** How many classes - the tusk kinds' size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a length, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static TuskSize of(double s) {
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
