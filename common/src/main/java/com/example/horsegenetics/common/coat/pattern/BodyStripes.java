package com.example.horsegenetics.common.coat.pattern;

/**
 * A field of <b>stripes</b> in body space - bands that run mostly across the
 * horse (constant body-space X), so a stripe wraps from the barrel's side over
 * the spine without a seam.
 *
 * <p><b>Mostly</b>, because a pure plane of constant X has a failure mode: any
 * face perpendicular to X - the chest, the rump, the front and back of every
 * leg - sits at one phase and renders as a flat band of solid stripe or solid
 * coat. So the phase also carries a small {@link #SLANT} on {@code |z|}, which
 * bends each stripe into a shallow chevron: symmetric left to right (the horse
 * doesn't look lopsided), and enough variation across a constant-X face that
 * the stripes carry on over it.
 *
 * <p>Pure, like {@link BodyNoise}, which it warps itself with: same
 * {@code (seed, point, knobs)} in, same coverage out, so a coat rebuilt next
 * session is identical.
 *
 * <p>Deliberately generic. Magic zebra is the first caller, but the same field
 * is what a natural <b>dun</b>'s leg barring and <b>brindle</b>'s striping
 * want - they differ in where they apply it and what they do with it, not in
 * the maths. Keep gene-specific decisions (how far down the horse the stripes
 * reach, what colour they are) in the gene.
 */
public final class BodyStripes {

    /** How gently the warp field bends a stripe - small = long, lazy curves. */
    private static final double WARP_SCALE = 1.0 / 9.0;
    /** Scale of the along-the-stripe width modulation - stripes taper and swell. */
    private static final double WIDTH_SCALE = 0.26;
    /** Soft edge, as a fraction of the half-period. Narrow: stripes want to read crisp. */
    private static final double EDGE = 0.10;
    /** How far, per body unit away from the centreline, a stripe leans along X. */
    private static final double SLANT = 0.35;

    private BodyStripes() {}

    /**
     * Stripe coverage at a body-space point: <b>1</b> deep inside a stripe,
     * <b>0</b> in the gap between two, with a soft edge in between.
     *
     * @param spacing centre-to-centre distance in body units (model units, 1 =
     *                1/16 block); the adult barrel is 22 units long
     * @param duty    how much of each period is stripe, {@code (0, 1)} - 0.5 is
     *                equal stripe and gap
     * @param warp    how far, in body units, the noise field may bend a stripe
     *                off its plane; 0 gives ruler-straight bands
     */
    public static double coverage(long seed, double x, double y, double z,
                                  double spacing, double duty, double warp) {
        double bend = (BodyNoise.value(seed, x * WARP_SCALE, y * WARP_SCALE, z * WARP_SCALE) - 0.5) * 2.0 * warp;
        double phase = (x + bend + Math.abs(z) * SLANT) / spacing;

        // 0 at the middle of a stripe, 1 at the middle of the gap after it.
        double offset = phase - Math.floor(phase);
        double d = Math.abs(offset - 0.5) * 2.0;

        // A real stripe is not one width all the way down - swell and taper it.
        double width = duty * (0.75 + 0.5 * BodyNoise.value(seed ^ 0x9E3779B97F4A7C15L,
                x * WIDTH_SCALE, y * WIDTH_SCALE, z * WIDTH_SCALE));

        return 1.0 - smoothstep(width - EDGE, width + EDGE, d);
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
