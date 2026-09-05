package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TargetBand;

/**
 * Turns a breed's <b>1&ndash;10 stat scores</b> and its <b>height range in
 * hands</b> into {@link TargetBand}s for the four magical body-stat genes.
 *
 * <h2>The anchors</h2>
 * A score of <b>5</b> is the value the vanilla-calibrated baseline already
 * produces, so it maps to a multiplier of <b>1.0</b>. A score of <b>10</b> maps
 * to the breed sheet's stated ceiling for that stat:
 * <ul>
 *   <li><b>speed</b> &mdash; 9.71&nbsp;m/s at 5, 19.6&nbsp;m/s at 10 &rArr; &times;2.02</li>
 *   <li><b>health</b> &mdash; 22.5&nbsp;hp at 5, 50.5&nbsp;hp at 10 &rArr; &times;2.24</li>
 *   <li><b>jump</b> &mdash; 2.5&nbsp;m at 5, 8.57&nbsp;m at 10 &rArr; &times;3.43</li>
 * </ul>
 * Between 5 and 10 the multiplier is linear to that ceiling. Below 5 it is
 * linear the other way toward roughly <b>&times;0.2</b> at a score of 1, a
 * common floor for all three (a score-1 horse is a very slow, very fragile,
 * barely-hopping animal, but still a functioning one - the
 * {@code MAGICAL_MIN_FACTOR} guard is well clear).
 *
 * <h2>Height</h2>
 * Body scale is not scored 1&ndash;10; it comes straight from the hands range.
 * The baseline horse is <b>{@value #BASELINE_HH}&nbsp;hh</b>, so a breed whose
 * mid-height is {@code h} targets scale {@code h / 15.75}, and the low/high of
 * its range become the low/high of the band.
 *
 * <h2>Near-baseline axes carry no band</h2>
 * If a resolved band sits inside {@code [}{@value #NEUTRAL_LO}{@code ,
 * }{@value #NEUTRAL_HI}{@code ]} it is dropped ({@code bandFor} returns
 * {@code null}). The breed founder then leaves that locus wild and the horse
 * sits exactly on the baseline, rather than being forced homozygous for a
 * pushing allele whose band straddles 1.0 (which the gene cannot honour - a
 * {@code Swift} copy must not resolve to a sub-1.0 factor).
 */
public final class BreedStatCurve {

    /** The height a body-scale of exactly 1.0 corresponds to, in hands. */
    public static final double BASELINE_HH = 15.75;

    private static final double SPEED_CEIL = 19.6 / 9.71;   // ~2.019
    private static final double HEALTH_CEIL = 50.5 / 22.5;   // ~2.244
    private static final double JUMP_CEIL = 8.57 / 2.5;      // ~3.428
    private static final double LOW_FLOOR = 0.20;            // shared score-1 floor

    /** A resolved band fully inside this range is treated as "no target". */
    public static final double NEUTRAL_LO = 0.94;
    public static final double NEUTRAL_HI = 1.06;

    private BreedStatCurve() {
    }

    /** The multiplier a single score maps to, for one of the three additive axes. */
    public static double factor(StatAxis axis, double score) {
        double ceil = switch (axis) {
            case SPEED -> SPEED_CEIL;
            case HEALTH -> HEALTH_CEIL;
            case JUMP -> JUMP_CEIL;
            case SCALE -> throw new IllegalArgumentException("scale is not scored 1-10; use scaleBand");
        };
        if (score >= 5.0) {
            return 1.0 + (score - 5.0) / 5.0 * (ceil - 1.0);
        }
        return 1.0 - (5.0 - score) / 4.0 * (1.0 - LOW_FLOOR);
    }

    /**
     * The band for one additive axis given a score range ({@code loScore ==
     * hiScore} for the common single-score case, which is padded out slightly).
     * Returns {@code null} when the result is near-baseline.
     */
    public static TargetBand bandFor(StatAxis axis, double loScore, double hiScore) {
        double lo = factor(axis, Math.min(loScore, hiScore));
        double hi = factor(axis, Math.max(loScore, hiScore));
        return finish(lo, hi);
    }

    public static TargetBand bandFor(StatAxis axis, double score) {
        return bandFor(axis, score, score);
    }

    /**
     * How much the <b>above-baseline</b> part of a height ratio is exaggerated.
     * Real horse height varies little - a Shire is only ~10% taller than an
     * average horse - but in game a draught horse should visibly tower, so the
     * part of the ratio above {@code 1.0} is multiplied by this. The
     * below-baseline part is left alone (small breeds already read fine).
     */
    private static final double BIG_GAMMA = 3.0;

    private static double exaggerated(double ratio) {
        return ratio > 1.0 ? 1.0 + (ratio - 1.0) * BIG_GAMMA : ratio;
    }

    /** The body-scale band from a hands range. Returns {@code null} when near-baseline. */
    public static TargetBand scaleBand(double loHh, double hiHh) {
        double lo = exaggerated(Math.min(loHh, hiHh) / BASELINE_HH);
        double hi = exaggerated(Math.max(loHh, hiHh) / BASELINE_HH);
        return finish(lo, hi);
    }

    private static TargetBand finish(double lo, double hi) {
        // pad a zero-width band so members of the breed still vary a little
        if (hi - lo < 0.03) {
            double mid = (lo + hi) / 2.0;
            double pad = Math.max(0.015, mid * 0.04);
            lo = mid - pad;
            hi = mid + pad;
        }
        if (lo >= NEUTRAL_LO && hi <= NEUTRAL_HI) {
            return null; // near baseline - leave the locus wild
        }
        // keep the band on one side of 1.0 so the pushing allele stays consistent
        if (lo < 1.0 && hi > 1.0) {
            if ((lo + hi) / 2.0 >= 1.0) {
                lo = Math.max(lo, 1.0);
            } else {
                hi = Math.min(hi, 1.0);
            }
        }
        return TargetBand.of(lo, hi);
    }
}
