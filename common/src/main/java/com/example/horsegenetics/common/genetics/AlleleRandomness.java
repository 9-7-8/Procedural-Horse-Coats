package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.MidpointRng;
import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;

/**
 * One gene's <b>per-horse randomness</b>, drawn from the epigenetic seeds stored
 * on the two allele copies - the twin of {@code CoatBuildContext}'s two
 * epigenetics accessors, and it offers the same two, for the same reason.
 *
 * <p>{@link #expressed()} answers "what does this horse <i>show</i> at this
 * locus", which is the right question wherever one locus produces one result.
 * {@link #copy(int)} answers "what does <i>this allele copy</i> carry", which is
 * the right question for a <b>codominant</b> gene, where both copies contribute
 * at once. Asking for the expressed copy there would count one allele twice and
 * the other not at all.
 *
 * <p>Both draw from the epigenetic seed stored on the allele copy, so both are
 * deterministic and both are inherited with the allele.
 *
 * <h2>Two consumers</h2>
 * It started on the trait side
 * ({@link com.example.horsegenetics.common.trait.EpigeneticTraitContribution},
 * for the magical size locus) and lives here now because the <i>ability</i> side
 * needs exactly the same thing: {@link EpigeneticAbilityContribution} hands a
 * gene this interface so an effect's magnitude - a particle's colour, where on
 * the horse it comes from, how much of it there is - can be written on the
 * allele copy rather than on the allele. Keeping it in {@code genetics/} is what
 * stops {@code genetics/} and {@code trait/} depending on each other.
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

    /**
     * The randomness for {@code gene} on the horse described by
     * {@code genotype} + {@code epigenome}.
     *
     * <p>{@code epigenome} may be {@code null}, in which case every accessor is
     * {@link MidpointRng} and the caller gets the <i>midpoint</i> of what the
     * genotype can produce - the honest answer to a question asked about a
     * genotype rather than about a horse.
     */
    static AlleleRandomness forGene(Gene gene, Genotype genotype, Epigenome epigenome) {
        if (epigenome == null) {
            return MIDPOINT;
        }
        Epigenome.Copies copies = epigenome.copies(gene);
        return new AlleleRandomness() {
            @Override
            public Rng expressed() {
                return new SeededRng(epigenome.expressedSeed(gene, genotype), gene.key());
            }

            @Override
            public Rng copy(int slot) {
                long seed = (slot == 0 ? copies.first() : copies.second()).epigeneticSeed();
                return new SeededRng(seed, gene.key());
            }
        };
    }

    /** Every accessor is the midpoint - see {@link #forGene}. */
    AlleleRandomness MIDPOINT = new AlleleRandomness() {
        @Override
        public Rng expressed() {
            return MidpointRng.INSTANCE;
        }

        @Override
        public Rng copy(int slot) {
            return MidpointRng.INSTANCE;
        }
    };
}
