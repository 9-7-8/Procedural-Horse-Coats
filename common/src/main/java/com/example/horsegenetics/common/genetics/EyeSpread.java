package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

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
     * Roll one. Draw order is the contract - {@code nextFloat}, then
     * {@code nextBoolean}, then the wedges - so a horse's eyes do not change
     * when something else in the gene changes.
     */
    public static EyeSpread roll(Rng rng) {
        float roll = rng.nextFloat();
        if (roll < BOTH_EYES) {
            return BOTH;
        }
        boolean rightFirst = rng.nextBoolean();
        if (roll < BOTH_EYES + ONE_EYE) {
            return rightFirst
                    ? new EyeSpread(EyePatch.WHOLE, EyePatch.NONE)
                    : new EyeSpread(EyePatch.NONE, EyePatch.WHOLE);
        }
        int wedge = EyePatch.randomWedge(rng);
        int other = rng.nextFloat() < SECTORAL_IN_BOTH
                ? EyePatch.differentWedge(rng, wedge)
                : EyePatch.WHOLE;
        return rightFirst ? new EyeSpread(wedge, other) : new EyeSpread(other, wedge);
    }
}
