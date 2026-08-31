package com.example.horsegenetics.common.genetics;

/**
 * The visible coat outcome derived from a {@link Genotype}. More will be added
 * as more loci (cream, dun, gray, etc.) come online.
 */
public enum CoatPhenotype {

    /** ee at the extension locus. No black pigment anywhere, regardless of agouti. */
    CHESTNUT,

    /** E_ aa - black pigment present but not restricted by agouti. Solid black. */
    BLACK,

    /** E_ A_ - black pigment restricted to points (legs/mane/tail) by agouti. */
    BAY,

    /**
     * W_ at the white locus. Dominant over everything else - a solid white
     * horse, no markings, whatever the E / A alleles say. (Real-horse term:
     * "dominant white" / white, no markings.)
     */
    WHITE
}
