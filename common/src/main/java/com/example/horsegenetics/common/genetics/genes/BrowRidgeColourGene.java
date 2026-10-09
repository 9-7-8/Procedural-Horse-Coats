package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Brow ridge colour</b> ({@code horsegenetics.brow_ridge_colour}) - a <b>magical,
 * codominant</b> locus that colours a horse's brow ridge and does nothing at all to a
 * horse without one. The horn colour gene's alleles and rules, through
 * {@link AbstractPartColourGene}, on a locus of its own (owner, 2026-10-01: colour is
 * its own gene per part).
 *
 * <p>Two different copies make a two-tone ridge: the bar is the base colour, and what
 * stands on it - a plain ridge's step, a notched one's bumps, a spined one's points -
 * the other. Each copy's {@code hue} and {@code tone} nudge its colour, and a chaos copy
 * wears a colour of its own. {@code Bon/Bon} is the ridge's bone version - bare bone,
 * which the skeleton breeds carry. It paints nothing on the coat, so every outcome is a
 * wild type and neither coat golden reads it.
 */
public final class BrowRidgeColourGene extends AbstractPartColourGene {

    public static final String KEY = "horsegenetics.brow_ridge_colour";

    /** Magical band, beside {@link BrowRidgeGene}, as the horn's colour sits beside the horn. */
    public static final int PRIORITY = 348;

    public BrowRidgeColourGene() {
        super(KEY, "Brow ridge colour", PRIORITY, new Words("brow-ridge", "brow ridge", false,
                "ridges", "what nearly every ridged horse has", "a horse with a brow ridge",
                "the bar is one, the base, and its step, bumps or spine points the other."));
    }

    @Override
    public boolean partGrows(Genotype genotype) {
        return Genes.BROW_RIDGE.shows(genotype.pair(Genes.BROW_RIDGE));
    }
}
