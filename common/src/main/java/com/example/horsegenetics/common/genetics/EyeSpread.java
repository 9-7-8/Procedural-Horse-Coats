package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

/**
 * <b>How much of each iris a depigmenting claim actually reaches</b> - which is
 * where a horse's one blue eye, and the blue wedge in an otherwise brown one,
 * come from.
 *
 * <h2>Why this is not a gene, and not a claim either</h2>
 * The white loci already answer "<i>is</i> this horse blue-eyed" through
 * {@link EyeColorContribution}, and that answer is about alleles: splash is the
 * blue-eyed pattern, {@code KIT} only from broad white upward, and so on. This
 * is the <b>separate</b> question of how far the blue got, and the reference
 * literature is unambiguous that it is not an allele question at all - one
 * {@code MITF} copy produces "anything from subtle facial white or one blue eye
 * to extensive splash". Complete heterochromia (one iris short of pigment
 * cells) and sectoral heterochromia (a patch of one iris short of them) are the
 * <i>same</i> failure of melanocyte colonisation as two blue eyes, stopped
 * earlier.
 *
 * <p>So it is rolled, once, off the epigenetic seed of the locus that won the
 * eye-colour claim - inherited with that allele copy like everything else
 * epigenetic, and stable for the life of the horse. It is deliberately
 * <b>one</b> roll rather than one per white locus: a horse carrying splash and
 * frame at once would otherwise stack two independent masks and end up unable
 * to have a single blue eye at all, which is backwards.
 *
 * <h2>The distribution</h2>
 * Two blue eyes is much the commonest outcome, one blue eye is the classic
 * splash-carrier tell, and a sector is the rarest and the one people photograph.
 *
 * @param right quadrant mask ({@link EyePatch}) for the eye on the head's west
 *              face - {@code CoatRegions.eyeRects} index 0
 * @param left  the same for the east face - index 1
 */
public record EyeSpread(int right, int left) {

    /** The ordinary outcome: both irises fully depigmented. */
    public static final EyeSpread BOTH = new EyeSpread(EyePatch.WHOLE, EyePatch.WHOLE);

    /** Share of blue-eyed horses with two whole blue eyes. */
    public static final float BOTH_EYES = 0.62f;

    /** Share with exactly one whole blue eye - complete heterochromia. */
    public static final float ONE_EYE = 0.22f;

    /**
     * Of the remainder - the sectoral horses - the share that get a wedge in
     * <i>both</i> eyes rather than a wedge in one and a whole blue other.
     */
    public static final float SECTORAL_IN_BOTH = 0.45f;

    public EyeSpread {
        right &= EyePatch.WHOLE;
        left &= EyePatch.WHOLE;
    }

    /** Does the depigmentation reach neither iris at all? */
    public boolean empty() {
        return right == EyePatch.NONE && left == EyePatch.NONE;
    }

    /** The two irises differ - one blue eye, or a sector in only one of them. */
    public boolean heterochromatic() {
        return right != left;
    }

    /** Part of an iris, but not all of it - the sectoral case. */
    public boolean sectoral() {
        return partial(right) || partial(left);
    }

    private static boolean partial(int q) {
        return q != EyePatch.NONE && q != EyePatch.WHOLE;
    }

    /**
     * How a depigmenting gene's blue lands on this horse's two eyes, read off
     * the stored values of whichever gene claimed the eye colour.
     *
     * <p>{@link #SPREAD} is a position rather than a decision: it is tested
     * against {@link #BOTH_EYES} and {@link #ONE_EYE}, so both eyes / one eye /
     * a sector stays a weighted outcome and stays heritable as a tendency.
     */
    public static EyeSpread roll(EpiValues epi) {
        double roll = epi.get(SPREAD);
        if (roll < BOTH_EYES) {
            return BOTH;
        }
        boolean rightFirst = epi.category(FIRST) == 0;
        if (roll < BOTH_EYES + ONE_EYE) {
            return rightFirst
                    ? new EyeSpread(EyePatch.WHOLE, EyePatch.NONE)
                    : new EyeSpread(EyePatch.NONE, EyePatch.WHOLE);
        }
        int wedge = EyePatch.wedgeAt(epi.category(WEDGE));
        int other = epi.get(BOTH_SECTORS) < SECTORAL_IN_BOTH
                ? EyePatch.differentWedgeAt(wedge, epi.category(WEDGE_STEP))
                : EyePatch.WHOLE;
        return rightFirst ? new EyeSpread(wedge, other) : new EyeSpread(other, wedge);
    }

    // ------------------------------------------------------------------
    // Epigenetics
    // ------------------------------------------------------------------

    /** Both eyes / one eye / a sector - a weighted position, see {@link #roll}. */
    public static final String SPREAD = "eye_spread";
    /** Which eye a one-eyed or sectoral effect lands on. */
    public static final String FIRST = "eye_first";
    public static final String WEDGE = "eye_wedge";
    public static final String WEDGE_STEP = "eye_wedge_step";
    public static final String BOTH_SECTORS = "eye_both_sectors";

    /**
     * What {@link #roll} reads. Every gene that can claim an eye colour composes
     * this into its own schema, because the spread is read off <b>whichever
     * gene won the claim</b> - so the values have to be sitting on that gene's
     * allele copy, whichever one it turns out to be.
     */
    public static EpiSchema schema() {
        return EpiSchema.of(
                EpiValue.uniform(SPREAD, 0, 1),
                EpiValue.category(FIRST, 2),
                EpiValue.category(WEDGE, EyePatch.wedgeCount()),
                EpiValue.category(WEDGE_STEP, EyePatch.wedgeStepCount()),
                EpiValue.uniform(BOTH_SECTORS, 0, 1));
    }
}
