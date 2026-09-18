package com.example.horsegenetics.common.trait;

/**
 * A closed range a breed wants one {@link StatAxis} of its horses to fall in -
 * e.g. {@code [1.90, 2.10]} for a breed whose speed score is 10/10.
 * The magical body-stat gene picks a point in it per horse from that horse's
 * epigenetic seeds ({@link #lerp}), so the spread <i>is</i> the band width and
 * two horses of the breed differ only by as much as the band allows.
 *
 * <p><b>The units are the axis's, not this record's.</b> On the four multiplier
 * axes a band is a multiple of the game unit and ordinary is {@code 1.0}; on
 * {@link StatAxis#PULL} it is a raw 1-10 score and ordinary is {@code 5.0}.
 * That is why {@link #pushesUp(double)} has to be told the baseline rather than
 * assuming one - see {@link StatAxis#baseline()}.
 *
 * <p>A band is only ever built when it is clearly directional - entirely above
 * the baseline or entirely below it. A near-baseline breed carries no band for
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

    /**
     * Which direction this band pushes - {@code true} if it sits above
     * {@code baseline}, which is the axis's ordinary value
     * ({@link StatAxis#baseline()}).
     */
    public boolean pushesUp(double baseline) {
        return (lo + hi) / 2.0 >= baseline;
    }

    /** How far from {@code baseline} the middle of this band sits, unsigned. */
    public double distanceFrom(double baseline) {
        return Math.abs((lo + hi) / 2.0 - baseline);
    }
}
