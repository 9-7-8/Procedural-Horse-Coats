package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * Turns a {@link Genotype} into a {@link CoatData} by rolling a fresh
 * {@link Epigenome} - a priority + epigenetic seed on <b>every allele copy</b>,
 * which is what drives each non-deterministic coat gene's per-horse randomness
 * (bay point heights, grey dapples, splash markings).
 *
 * <p>This is the <b>founder</b> path only: a wild spawn, a {@code /summon}, a
 * gallery horse. A <b>foal</b> must not come through here - it inherits each
 * allele's epigenetics from the parent copy it received
 * ({@link Genome#breedWith}). Either way the result is rolled <b>once</b>;
 * persist it and never call this again for the same horse, or the coat changes
 * between sessions.
 *
 * <p>The actual pixels are produced later, in the game module, by
 * {@code coat.pattern.CoatTextureComposer}.
 */
public final class CoatGenerator {

    private CoatGenerator() {
    }

    public static CoatData generate(Genotype genotype, Rng rng) {
        return new CoatData(Genome.of(genotype, rng));
    }

    public static CoatData of(Genome genome) {
        return new CoatData(genome);
    }
}
