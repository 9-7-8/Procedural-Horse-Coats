package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Body plate colour</b> ({@code horsegenetics.body_plate_colour}) - a <b>magical,
 * codominant</b> locus that colours a horse's shoulder and hip plates and does nothing
 * at all to a horse without them. The horn colour gene's alleles and rules, through
 * {@link AbstractPartColourGene}, on a locus of its own (owner, 2026-10-01: colour is
 * its own gene per part).
 *
 * <p>Two different copies make two-tone plates: every slab is the base colour, and its
 * edge - the lip along its bottom, and the rib or spike on a ridged or spiked slab -
 * the other (owner, 2026-10-05). Each copy's {@code hue} and {@code tone} nudge its
 * colour, and a chaos copy wears a colour of its own. {@code Bon/Bon} is the plates'
 * bone version - bare bone, which the skeleton breeds carry. It paints nothing on the
 * coat, so every outcome is a wild type and neither coat golden reads it.
 */
public final class BodyPlateColourGene extends AbstractPartColourGene {

    public static final String KEY = "horsegenetics.body_plate_colour";

    /** Magical band, beside {@link BodyPlatesGene}, as the horn's colour sits beside the horn. */
    public static final int PRIORITY = 246;

    public BodyPlateColourGene() {
        super(KEY, "Body plate colour", PRIORITY, new Words("body-plates", "body plates", true,
                "plates", "what nearly every plated horse has", "a horse with body plates",
                "each plate is one, the base, and its edge, rib or spike the other."));
    }

    @Override
    public boolean partGrows(Genotype genotype) {
        return Genes.BODY_PLATES.shows(genotype.pair(Genes.BODY_PLATES));
    }
}
