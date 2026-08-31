package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

/**
 * Combines two parents' genetic codes into a child's. A "genetic code" is a
 * {@link Genotype} string for now (see {@link Genotype#parse}); this class is
 * the seam so callers that only deal in strings - like the game-integration
 * layer - never have to know that.
 *
 * <p>Pure Layer-1 logic: deterministic given the {@link Rng}, no game
 * dependency.
 */
public final class GeneticCodeCombiner {

    /**
     * @return the child's genetic code, one allele drawn at random from each
     *         parent per locus. Order of the arguments does not matter.
     * @throws IllegalArgumentException if either code is not a valid genotype
     */
    public static String combine(String motherCode, String fatherCode, Rng rng) {
        Genotype mother = Genotype.parse(motherCode);
        Genotype father = Genotype.parse(fatherCode);
        return mother.breedWith(father, rng).toCode();
    }

    private GeneticCodeCombiner() {
    }
}
