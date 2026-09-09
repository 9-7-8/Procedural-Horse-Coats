package com.example.horsegenetics.common.genetics;

/**
 * <b>The white lock.</b> A gene implementing this asks the coat pipeline to
 * make every <i>white</i> texel permanent: once a texel is white, nothing
 * painted afterwards may change it.
 *
 * <p>Exactly one gene implements it -
 * {@link com.example.horsegenetics.common.genetics.genes.ExtremeWhiteDominantGene}
 * - and it is a capability rather than an {@code if} in the composer for the
 * same reason {@link LutContribution} is: the composer must not know the name
 * of any particular gene.
 *
 * <p><b>It is not a priority.</b> A gene's {@link Gene#priority()} says where
 * it paints in the order; this says the rule applies at <i>every</i> step of
 * the order, including the ones above the locking gene's own slot. That is what
 * makes it "extreme" rather than "high priority": there is no slot a marking
 * could take that would get it over the white.
 *
 * <p>What counts as white, and why the eyes are exempt, is
 * {@code CoatTextureComposer.whiteLock}.
 */
public interface WhiteLockContribution {

    /**
     * Does this pair, on this horse, lock the white?
     *
     * @param pair     the horse's copies at this locus
     * @param genotype the whole genome, for a gene that reads another locus
     */
    boolean locksWhite(AllelePair pair, Genotype genotype);
}
