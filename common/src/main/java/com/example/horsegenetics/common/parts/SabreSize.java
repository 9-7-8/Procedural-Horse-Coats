package com.example.horsegenetics.common.parts;

/**
 * <b>How long a sabre fang is</b> - four classes on the tusks locus's {@code length}
 * number, the {@link DragonHornSize} arrangement: the class picks the mesh, the
 * remainder stretches within it. Lengths are down the blade, in model units. A fang
 * roots on the mouth line a unit and a half above the chin, so even {@link #SHORT}
 * reaches below it (tusks treatment) and {@link #GREAT} hangs twice the depth of the
 * muzzle. A first placement, not a spec.
 */
public enum SabreSize {

    /** Just past the chin. */
    SHORT("Short"),
    /** An ordinary pair of sabre fangs. */
    STANDARD("Standard"),
    /** As long as the muzzle is deep. */
    LONG("Long"),
    /** The largest the renderer draws. */
    GREAT("Great");

    /** Length at {@code length = 0}. */
    public static final float SHORTEST = 3.0f;
    /** Length at {@code length = 1}. */
    public static final float LONGEST = 10.0f;

    private final String label;

    SabreSize(String label) {
        this.label = label;
    }

    /** How many classes - the sabre kinds' size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a length, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static SabreSize of(double s) {
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
