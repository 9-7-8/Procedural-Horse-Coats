package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.Rng;

/**
 * One gene's per-horse randomness, handed to an
 * {@link EpigeneticTraitContribution} - the trait-side twin of
 * {@code CoatBuildContext}'s two epigenetics accessors, and it offers the same
 * two, for the same reason.
 *
 * <p>{@link #expressed()} answers "what does this horse <i>show</i> at this
 * locus", which is the right question wherever one locus produces one result.
 * {@link #copy(int)} answers "what does <i>this allele copy</i> carry", which is
 * the right question for a <b>codominant</b> gene, where both copies contribute
 * at once and their values add. Asking for the expressed copy there would count
 * one allele twice and the other not at all.
 *
 * <p>Both draw from the epigenetic seed stored on the allele copy, so both are
 * deterministic and both are inherited with the allele.
 */
public interface AlleleRandomness {

    /**
     * The copy this horse shows at the gene - the dominant copy on a
     * heterozygote, the higher-priority one on a homozygote.
     */
    Rng expressed();

    /**
     * One particular copy: {@code slot} 0 is {@code pair.first()}, 1 is
     * {@code pair.second()}. Slots follow the gene's own {@code alleles()}
     * declaration order, because {@code AllelePair} canonicalizes by it.
     */
    Rng copy(int slot);
}
