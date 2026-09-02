package com.example.horsegenetics.common.genetics;

/**
 * How a {@link Gene}'s variant allele expresses against its wild type. Gene
 * <b>metadata</b>: it doesn't do the expressing (that's {@link Gene#restrict} /
 * {@link Gene#tint}), it tells the rest of the mod what to expect, so
 * code can reason about a gene without special-casing it by key.
 *
 * <p>Today its one consumer is {@link GenotypeCatalog}, which uses it to decide
 * which allele pairs are worth their own pen in the horse dimension's gallery.
 * It's declared per gene rather than derived so a future consumer (breeding
 * UI, punnett squares, a phenotype summary) has it to hand.
 *
 * <p>Every gene here has exactly two alleles, so a "pair" is one of three
 * things: homozygous wild type, heterozygous, homozygous variant.
 */
public enum DominancePattern {

    /**
     * One variant allele is enough for the full effect - the heterozygote is
     * indistinguishable from the homozygous variant.
     */
    DOMINANT,

    /**
     * The variant only shows when homozygous - the heterozygote is
     * indistinguishable from the homozygous wild type (a carrier).
     */
    RECESSIVE,

    /**
     * The heterozygote has a look of its own, between the two homozygotes -
     * all three pairs are visually distinct.
     */
    INCOMPLETE_DOMINANT,

    /**
     * {@link #DOMINANT} <b>and epistatic</b>: while the variant is present this
     * is the <i>only</i> gene you can see - every other gene is masked, so every
     * horse carrying it looks the same whatever else it has (dominant white,
     * and the diagnostic test overlay).
     */
    COMPLETE_DOMINANT;

    /** Does the heterozygote look like neither homozygote? */
    public boolean heterozygoteIsDistinct() {
        return this == INCOMPLETE_DOMINANT;
    }

    /** Does the variant, when present, hide every other gene's contribution? */
    public boolean masksOtherGenes() {
        return this == COMPLETE_DOMINANT;
    }
}
