package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * Turns a {@link Genotype} into a {@link CoatData} by rolling the horse's
 * <b>epigenetic seed</b> - the single value that drives every non-deterministic
 * coat gene's per-horse randomness (bay sock heights, seal leg tan, future
 * markings). Rolled <b>once</b> here at birth; persist the result and never
 * call this again for the same horse, or the coat changes between sessions.
 *
 * <p>The actual pixels are produced later, in the game module, by
 * {@code coat.pattern.CoatTextureComposer}.
 */
public final class CoatGenerator {

    private CoatGenerator() {
    }

    public static CoatData generate(Genotype genotype, Rng rng) {
        return new CoatData(genotype, rng.nextLong());
    }
}
