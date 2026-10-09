package com.example.horsegenetics.common.parts;

/**
 * <b>How long a horse's cheek spikes are</b> - three classes on the {@code length}
 * epigenetic number, the {@link SpineSize} arrangement: the class picks the mesh, the
 * remainder stretches each spike within it. A length is the rearmost, longest spike's,
 * root to point, in model units. A first placement, not a spec.
 */
public enum CheekSpikeSize {

    /** Studs along the cheek. */
    SHORT("Short"),
    /** Spikes about as long as the cheek is deep. */
    MEDIUM("Medium"),
    /** The longest the renderer draws. */
    LONG("Long");

    /** The longest spike at {@code size = 0}. */
    public static final float SHORTEST = 1.0f;
    /** The longest spike at {@code size = 1}. */
    public static final float LONGEST = 3.0f;

    private final String label;

    CheekSpikeSize(String label) {
        this.label = label;
    }

    /** How many classes - the cheek spike kinds' size buckets. */
    public static int classes() {
        return values().length;
    }

    /** {@code s} in {@code [0,1]} to a length, geometrically; clamped, never extrapolated. */
    public static float lengthFor(double s) {
        double c = s < 0.0 ? 0.0 : (s > 1.0 ? 1.0 : s);
        return (float) (SHORTEST * Math.pow(LONGEST / SHORTEST, c));
    }

    /** The class {@code s} falls in. */
    public static CheekSpikeSize of(double s) {
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
