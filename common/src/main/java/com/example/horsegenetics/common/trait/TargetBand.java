package com.example.horsegenetics.common.trait;

/**
 * A closed multiplier range a breed wants one {@link StatAxis} of its horses to
 * fall in - e.g. {@code [1.90, 2.10]} for a breed whose speed score is 10/10.
 * The magical body-stat gene picks a point in it per horse from that horse's
 * epigenetic seeds ({@link #lerp}), so the spread <i>is</i> the band width and
 * two horses of the breed differ only by as much as the band allows.
 *
 * <p>A band is only ever built when it is clearly directional - entirely above
 * {@code 1.0} or entirely below it. A near-baseline breed carries no band for
 * that axis and the locus is left wild, so the horse sits exactly on the
 * baseline.
 */
public record TargetBand(double lo, double hi) {

    public TargetBand {
        if (hi < lo) {
            double t = lo;
            lo = hi;
            hi = t;
        }
    }

    public static TargetBand of(double lo, double hi) {
        return new TargetBand(lo, hi);
    }

    /** {@code u} is clamped to {@code [0,1]}; {@code lerp(0)==lo}, {@code lerp(1)==hi}. */
    public double lerp(double u) {
        double c = u < 0.0 ? 0.0 : (u > 1.0 ? 1.0 : u);
        return lo + (hi - lo) * c;
    }

    /** Which direction this band pushes - {@code true} if it sits above the baseline. */
    public boolean pushesUp() {
        return (lo + hi) / 2.0 >= 1.0;
    }
}
