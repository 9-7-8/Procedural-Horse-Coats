package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Back sail colour</b> ({@code horsegenetics.back_sail_colour}) - a <b>magical,
 * codominant</b> locus that colours a horse's back sail and does nothing at all to a
 * horse without one. The horn colour gene's alleles and rules, through
 * {@link AbstractPartColourGene}, on a locus of its own (owner, 2026-10-01: colour is
 * its own gene per part).
 *
 * <p>Two different copies make a two-tone sail. The earlier allele is the membrane
 * and the other the spines, the crystal antler's mapping (base see-through, tip solid;
 * body-parts treatment). A one-colour horse has a sail all one colour. Each copy's
 * {@code hue} and {@code tone} nudge its colour, and a chaos copy wears a colour of its
 * own. {@code Bon/Bon} is the sail's bone version: bare bone rays with no membrane, the
 * owner's wing rule ({@code GrownParts.of} leaves the skin off). It paints nothing on the
 * coat, so every outcome is a wild type and neither coat golden reads it.
 */
public final class BackSailColourGene extends AbstractPartColourGene {

    public static final String KEY = "horsegenetics.back_sail_colour";

    /** Magical band, beside {@link BackSailGene}, as the horn's colour sits beside the horn. */
    public static final int PRIORITY = 244;

    public BackSailColourGene() {
        super(KEY, "Back sail colour", PRIORITY, new Words("back-sail", "sail", false,
                "sails", "what nearly every sailed horse has", "a horse with a back sail",
                "the membrane is one, the base, and the spines the other."));
    }

    @Override
    public boolean partGrows(Genotype genotype) {
        return Genes.BACK_SAIL.shows(genotype.pair(Genes.BACK_SAIL));
    }
}
