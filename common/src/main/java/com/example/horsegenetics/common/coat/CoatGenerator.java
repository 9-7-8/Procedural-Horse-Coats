package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * Turns a Genotype into a concrete, renderable CoatData.
 *
 * <p>Chestnut and black currently map straight to the game's default coat -
 * there's nothing to generate. Bay is where the procedural part lives: every
 * bay horse gets its own random leg-black height, generated once here and
 * expected to be persisted by the caller (do not call this again on world
 * load - re-rolling would make the horse's coat change every time it's
 * reloaded).
 */
public final class CoatGenerator {

    private CoatGenerator() {
    }

    public static CoatData generate(Genotype genotype, Rng rng) {
        CoatPhenotype phenotype = genotype.phenotype();
        if (phenotype != CoatPhenotype.BAY) {
            return CoatData.solid(phenotype);
        }
        float legBlackHeight = rollBayLegHeight(rng);
        return CoatData.bay(legBlackHeight);
    }

    /**
     * Rolls how high the black comes up the leg for a bay horse.
     * Uniform for now - biasing toward "wild type" low socks (e.g. via
     * rng.nextFloat() * rng.nextFloat()) is a easy follow-up tweak once
     * you've seen the distribution in-game.
     */
    private static float rollBayLegHeight(Rng rng) {
        return rng.nextFloat();
    }
}
