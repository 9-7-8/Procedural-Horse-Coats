package com.example.horsegenetics.common.coat.pattern;

/**
 * Smooth, continuous <b>patch fields</b> in body space, for the white-spotting
 * genes (tobiano, frame, sabino). Everything is a pure function of
 * {@code (seed, x, y, z)} sampled in the same model-unit coordinates
 * {@link com.example.horsegenetics.common.coat.skin.HorseSkinGeometry} hands
 * every texel, so a patch wraps around the barrel and crosses every part seam
 * without a join - which single-octave {@link BodyNoise#value} does too, but
 * too coarsely (one lattice cell spans the whole horse) and with a visible
 * axis-aligned grid at the low frequencies a big patch needs.
 *
 * <p>{@link #field} fixes both: <b>fractal</b> (three octaves) so the shape has
 * detail at more than one scale, and <b>domain-warped</b> (the sample point is
 * pushed around by a second noise field) so the octaves don't line up into a
 * grid and the patch edges wander. The horse is only ~10 units wide, so the
 * {@code z} axis is stretched before sampling ({@link #Z_STRETCH}) or the field
 * would be near-constant across the width and the two sides would come out
 * mirror-identical.
 */
public final class PatchNoise {

    /** The body is ~22 units long but only ~10 wide; stretch z so patches vary across it. */
    private static final double Z_STRETCH = 1.8;
    private static final double WARP = 2.8;

    private PatchNoise() {}

    /**
     * A smooth patch field, roughly {@code [0, 1]} (a little softer at the
     * ends). {@code scale} is the base spatial frequency - about
     * {@code 1 / patch-size-in-body-units}, so {@code 0.14} gives patches a
     * handful of units across.
     */
    public static double field(long seed, double x, double y, double z, double scale) {
        double zz = z * Z_STRETCH;

        // domain warp - a low-frequency offset so the octaves don't grid up.
        double wx = BodyNoise.value(seed ^ 0xA11CE5L, x * scale * 0.5, y * scale * 0.5, zz * scale * 0.5) - 0.5;
        double wy = BodyNoise.value(seed ^ 0xB22DF6L, x * scale * 0.5, y * scale * 0.5, zz * scale * 0.5) - 0.5;
        double wz = BodyNoise.value(seed ^ 0xC33E07L, x * scale * 0.5, y * scale * 0.5, zz * scale * 0.5) - 0.5;

        double sx = (x + wx * WARP) * scale;
        double sy = (y + wy * WARP) * scale;
        double sz = (zz + wz * WARP) * scale;

        double sum = 0, amp = 1, freq = 1, norm = 0;
        for (int o = 0; o < 3; o++) {
            sum += amp * BodyNoise.value(seed + 131L * o, sx * freq, sy * freq, sz * freq);
            norm += amp;
            amp *= 0.5;
            freq *= 2.13;
        }
        return sum / norm;
    }

    /** Two-octave value noise - cheaper than {@link #field}, for fine speckle (roan, sabino roaning). */
    public static double fbm2(long seed, double x, double y, double z) {
        return 0.62 * BodyNoise.value(seed, x, y, z)
                + 0.38 * BodyNoise.value(seed ^ 0x9E37L, x * 2.11, y * 2.11, z * 2.11);
    }

    /** Hermite fade from 0 at {@code edge0} to 1 at {@code edge1}. */
    public static double smoothstep(double edge0, double edge1, double v) {
        if (edge1 <= edge0) {
            return v < edge0 ? 0 : 1;
        }
        double t = (v - edge0) / (edge1 - edge0);
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }
}
