package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.epi.EpiValues;

/**
 * One gene's <b>per-horse numbers</b>, read off the two allele copies - the twin
 * of {@code CoatBuildContext}'s two epigenetics accessors, and it offers the
 * same two, for the same reason.
 *
 * <p>{@link #expressed()} answers "what does this horse <i>show</i> at this
 * locus", which is the right question wherever one locus produces one result.
 * {@link #copy(int)} answers "what does <i>this allele copy</i> carry", which is
 * the right question for a <b>codominant</b> gene, where both copies contribute
 * at once. Asking for the expressed copy there would count one allele twice and
 * the other not at all.
 *
 * <p>Both return the literal {@link EpiValues} stored on the copy, so both are
 * deterministic and both are inherited with the allele.
 *
 * <h2>This was called AlleleRandomness</h2>
 * It handed out an {@link com.example.horsegenetics.common.Rng} seeded on the
 * copy's epigenetic seed, and a gene recovered its numbers by drawing from it in
 * a fixed order. The order <i>was</i> the meaning of each number, so inserting a
 * draw while tuning a gene silently rewrote every horse in every save. Nothing
 * about it was random - it was a stored value the long way round - and the name
 * said otherwise, so both the name and the mechanism are gone.
 *
 * <h2>Two consumers</h2>
 * It started on the trait side
 * ({@link com.example.horsegenetics.common.trait.EpigeneticTraitContribution},
 * for the magical size locus) and lives here now because the <i>ability</i> side
 * needs exactly the same thing: {@link EpigeneticAbilityContribution} hands a
 * gene this so an effect's magnitude - a particle's colour, where on the horse
 * it comes from, how much of it there is - can be written on the allele copy
 * rather than on the allele. Keeping it in {@code genetics/} is what stops
 * {@code genetics/} and {@code trait/} depending on each other.
 */
public interface GeneEpigenetics {

    /**
     * The copy this horse shows at the gene - the dominant copy on a
     * heterozygote, the higher-priority one on a homozygote.
     */
    EpiValues expressed();

    /**
     * One particular copy: {@code slot} 0 is {@code pair.first()}, 1 is
     * {@code pair.second()}. Slots follow the gene's own {@code alleles()}
     * declaration order, because {@code AllelePair} canonicalizes by it.
     */
    EpiValues copy(int slot);

    /**
     * The numbers for {@code gene} on the horse described by {@code genotype} +
     * {@code epigenome}.
     *
     * <p>{@code epigenome} may be {@code null}, in which case every accessor
     * returns the schema's <b>midpoints</b> and the caller gets the middle of
     * what the genotype can produce - the honest answer to a question asked
     * about a genotype rather than about a horse. That is what
     * {@code MidpointRng} used to provide by returning 0.5 from every draw.
     */
    static GeneEpigenetics forGene(Gene gene, Genotype genotype, Epigenome epigenome) {
        if (epigenome == null) {
            EpiValues mid = gene.epiSchema().midpoint();
            return new GeneEpigenetics() {
                @Override public EpiValues expressed() { return mid; }
                @Override public EpiValues copy(int slot) { return mid; }
            };
        }
        Epigenome.Copies copies = epigenome.copies(gene);
        return new GeneEpigenetics() {
            @Override
            public EpiValues expressed() {
                return epigenome.expressedValues(gene, genotype);
            }

            @Override
            public EpiValues copy(int slot) {
                return (slot == 0 ? copies.first() : copies.second()).values();
            }
        };
    }
}
