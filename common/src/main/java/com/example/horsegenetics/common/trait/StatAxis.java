package com.example.horsegenetics.common.trait;

/**
 * The four body axes a {@link com.example.horsegenetics.common.breed.Breed breed}
 * can pin to a target: the three attribute multipliers and body scale.
 *
 * <p>Each maps one-to-one to a magical body-stat gene
 * ({@code MagicSpeedGene}, {@code MagicHealthGene}, {@code MagicJumpGene},
 * {@code MagicSizeGene}) and to one {@code multiply&hellip;Unclamped} hook on
 * {@link TraitBuilder}. A breed that names a target for an axis makes every one
 * of its wild founders homozygous for that gene's pushing allele and hands the
 * gene a {@link TargetBand}; the gene then lands the horse somewhere inside the
 * band using the allele copies' epigenetic seeds, so the breed hits its number
 * without depending on the Gaussian tail the gene uses when no breed is set.
 */
public enum StatAxis {
    SPEED,
    HEALTH,
    JUMP,
    SCALE
}
