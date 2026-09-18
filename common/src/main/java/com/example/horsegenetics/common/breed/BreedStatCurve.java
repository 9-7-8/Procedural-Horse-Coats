package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TargetBand;

/**
 * Turns a breed's <b>1&ndash;10 stat scores</b> and its <b>size range</b> into
 * {@link TargetBand}s for the five magical body-stat genes.
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
 * <h2>Pull is the score, uncurved</h2>
 * Pulling ability is scored 1&ndash;10 like the three above, and is the one axis
 * where the score <i>is</i> the resolved number: a breed scored {@code 10} wants
 * its horses at a pull of {@code 10}, and {@link #pullBand} is that score padded
 * out rather than run through a curve. Nothing converts it, because nothing in
 * {@code common/} knows what a pull score buys - see {@code MagicPullGene}.
 *
 * <h2>Size</h2>
 * Body scale is not scored 1&ndash;10. A breed writes it directly, as a
 * <b>multiple of the baseline horse</b> - {@code "size": [0.6, 0.75]} is a pony
 * breed, {@code 1.0} is a vanilla-sized horse - and that range <i>is</i> the
 * band. Hands are for reading, not for writing: {@link #handsFor} turns a scale
 * back into the horseman's unit for the info screens, and {@link #sizeForHands}
 * is the forward curve the breed files were converted with when they stopped
 * being written in hands.
 *
 * <h2>Heterozygous in the middle, homozygous at the ends</h2>
 * A founder whose size lands inside {@code [}{@value #HET_SIZE_LO}{@code ,
 * }{@value #HET_SIZE_HI}{@code ]} carries <b>one</b> copy of the size allele and
 * the wild type; outside it, two. One copy is plenty to make a horse a third
 * bigger or smaller, and a breed of ordinary-ish horses whose founders all
 * breed true for size would be a lie about how most size variation is carried.
 * The giants and the miniatures are where a line has been fixed. See
 * {@link #heterozygousSize}.
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

    /** Founders sized inside {@code [HET_SIZE_LO, HET_SIZE_HI]} carry one size copy; outside, two. */
    public static final double HET_SIZE_LO = 0.7;
    public static final double HET_SIZE_HI = 1.3;

    private static final double SPEED_CEIL = 19.6 / 9.71;   // ~2.019
    private static final double HEALTH_CEIL = 50.5 / 22.5;   // ~2.244
    private static final double JUMP_CEIL = 8.57 / 2.5;      // ~3.428
    private static final double LOW_FLOOR = 0.20;            // shared score-1 floor

    /** A resolved band fully inside this range is treated as "no target". */
    public static final double NEUTRAL_LO = 0.94;
    public static final double NEUTRAL_HI = 1.06;

    private BreedStatCurve() {
    }

    /**
     * How far either side of {@link HorseTraits#BASE_PULL} a pull band has to
     * sit before it counts as a target at all. Tight, because {@code 4} and
     * {@code 6} are ordinary scores on the breed sheets and both have to
     * survive; only a breed that wrote {@code 5} is saying "nothing special".
     */
    public static final double PULL_NEUTRAL = 0.4;

    /** The multiplier a single score maps to, for one of the three additive axes. */
    public static double factor(StatAxis axis, double score) {
        double ceil = switch (axis) {
            case SPEED -> SPEED_CEIL;
            case HEALTH -> HEALTH_CEIL;
            case JUMP -> JUMP_CEIL;
            case SCALE -> throw new IllegalArgumentException("scale is not scored 1-10; use sizeBand");
            case PULL -> throw new IllegalArgumentException(
                    "pull is a raw 1-10 score, not a multiplier; use pullBand");
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

    /**
     * <b>The height curve</b>: hands to the body scale a horse that tall is
     * drawn at. The inverse of {@link #handsFor}. Nothing reads a breed's size
     * in hands any more; this is kept so a size can be shown alongside the
     * hands it corresponds to, and because it is the formula the breed files
     * were converted with.
     */
    public static double sizeForHands(double hands) {
        return exaggerated(hands / BASELINE_HH);
    }

    /**
     * Does a founder rolled at body scale {@code size} carry one size copy
     * rather than two? Inside {@code [HET_SIZE_LO, HET_SIZE_HI]}, inclusive.
     */
    public static boolean heterozygousSize(double size) {
        return size >= HET_SIZE_LO && size <= HET_SIZE_HI;
    }

    /**
     * <b>The inverse of the height curve</b>: a body scale back to hands, so a
     * horse the breed sheet calls 17 hands reads as 17 hands on its own info
     * screen. Undoes {@link #exaggerated} above 1.0 and scales by
     * {@link #BASELINE_HH}, so the baseline horse is 15.75 hands (15.3 hh).
     */
    public static double handsFor(double scale) {
        double ratio = scale > 1.0 ? 1.0 + (scale - 1.0) / BIG_GAMMA : scale;
        return BASELINE_HH * ratio;
    }

    /**
     * Hands written the horseman's way: {@code 15.3 hh} is fifteen hands and
     * three <i>inches</i>, not fifteen and three tenths - a hand is four inches,
     * so the digit after the point runs 0 to 3. Rounded to the nearest inch.
     * (The breed files write decimal hands, {@code 15.75}; this is display only.)
     */
    public static String formatHands(double hands) {
        long inches = Math.round(Math.max(0.0, hands) * 4.0);
        return (inches / 4) + "." + (inches % 4) + " hh";
    }

    /**
     * The body-scale band from a size range, in multiples of the baseline horse.
     * Returns {@code null} when near-baseline.
     */
    public static TargetBand sizeBand(double lo, double hi) {
        return finish(Math.min(lo, hi), Math.max(lo, hi));
    }

    /**
     * <b>The pull band</b>, which is the score itself. Unlike the three additive
     * axes there is no curve to apply: pull is stored and resolved on the same
     * 1-10 scale the breed sheet writes, so a breed scored {@code 10} wants its
     * horses at {@code 10} and the band is that number padded out.
     *
     * <p>Returns {@code null} for a breed sitting within {@link #PULL_NEUTRAL}
     * of {@link HorseTraits#BASE_PULL}, which leaves the locus wild - the same
     * near-baseline rule the other four axes follow, on the same reasoning: a
     * breed that pulls like any other horse should carry no pull alleles rather
     * than be forced homozygous for one worth nothing.
     */
    public static TargetBand pullBand(double loScore, double hiScore) {
        return finish(Math.min(loScore, hiScore), Math.max(loScore, hiScore),
                HorseTraits.BASE_PULL,
                HorseTraits.BASE_PULL - PULL_NEUTRAL, HorseTraits.BASE_PULL + PULL_NEUTRAL);
    }

    public static TargetBand pullBand(double score) {
        return pullBand(score, score);
    }

    private static TargetBand finish(double lo, double hi) {
        return finish(lo, hi, 1.0, NEUTRAL_LO, NEUTRAL_HI);
    }

    /**
     * Pad, drop if near-baseline, and keep the result on one side of the
     * baseline. Shared by all five axes; {@code baseline} is {@code 1.0} for the
     * four multiplier ones and {@link HorseTraits#BASE_PULL} for the score - see
     * {@link StatAxis#baseline()}.
     */
    private static TargetBand finish(double lo, double hi,
                                     double baseline, double neutralLo, double neutralHi) {
        // pad a zero-width band so members of the breed still vary a little
        if (hi - lo < 0.03 * baseline) {
            double mid = (lo + hi) / 2.0;
            double pad = Math.max(0.015 * baseline, mid * 0.04);
            lo = mid - pad;
            hi = mid + pad;
        }
        if (lo >= neutralLo && hi <= neutralHi) {
            return null; // near baseline - leave the locus wild
        }
        // keep the band on one side of the baseline so the pushing allele stays consistent
        if (lo < baseline && hi > baseline) {
            if ((lo + hi) / 2.0 >= baseline) {
                lo = Math.max(lo, baseline);
            } else {
                hi = Math.min(hi, baseline);
            }
        }
        return TargetBand.of(lo, hi);
    }
}
