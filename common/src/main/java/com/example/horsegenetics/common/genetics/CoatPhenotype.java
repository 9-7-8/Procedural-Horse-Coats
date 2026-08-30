package com.example.horsegenetics.common.genetics;

/**
 * The visible coat outcome derived from a {@link Genotype}.
 * Only three phenotypes exist at this stage of the mod - more will be added
 * as more loci (cream, dun, gray, etc.) come online.
 */
public enum CoatPhenotype {

    /** ee at the extension locus. No black pigment anywhere, regardless of agouti. */
    CHESTNUT,

    /** E_ aa - black pigment present but not restricted by agouti. Solid black. */
    BLACK,

    /** E_ A_ - black pigment restricted to points (legs/mane/tail) by agouti. */
    BAY
}
