package com.example.horsegenetics.common.trait;

/**
 * The body axes a {@link com.example.horsegenetics.common.breed.Breed breed}
 * can pin to a target: the three attribute multipliers, body scale, and pulling
 * ability.
 *
 * <p>Each maps one-to-one to a magical body-stat gene
 * ({@code MagicSpeedGene}, {@code MagicHealthGene}, {@code MagicJumpGene},
 * {@code MagicSizeGene}, {@code MagicPullGene}) and to one sink on
 * {@link TraitBuilder}. A breed that names a target for an axis makes every one
 * of its wild founders homozygous for that gene's pushing allele and hands the
 * gene a {@link TargetBand}; the gene then lands the horse somewhere inside the
 * band using the allele copies' epigenetic seeds, so the breed hits its number
 * without depending on the Gaussian tail the gene uses when no breed is set.
 *
 * <h2>Two kinds of band, told apart by {@link #baseline()}</h2>
 * Four of the five are <b>multipliers</b> on a game unit and their bands are
 * measured against {@code 1.0}. {@link #PULL} is a raw <b>1-10 score</b> and its
 * band is measured against {@link HorseTraits#BASE_PULL} - a Shire's pull band
 * is literally {@code [9.6, 10.4]}, not a multiple of anything. Everything that
 * asks a band which way it pushes, or how far from ordinary it sits, has to ask
 * the axis what ordinary <i>is</i>; that is the whole job of {@link #baseline()}
 * and the reason {@link TargetBand#pushesUp(double)} takes an argument.
 */
public enum StatAxis {
    SPEED,
    HEALTH,
    JUMP,
    SCALE,
    PULL;

    /**
     * The value this axis reads at on an ordinary horse - what a
     * {@link TargetBand} on it is measured against. {@code 1.0} for the four
     * multiplier axes, {@link HorseTraits#BASE_PULL} for the score.
     */
    public double baseline() {
        return this == PULL ? HorseTraits.BASE_PULL : 1.0;
    }
}
