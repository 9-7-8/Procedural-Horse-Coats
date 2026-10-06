package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Crystal growth colour</b> ({@code horsegenetics.back_crystal_colour}) - a <b>magical,
 * codominant</b> locus that colours a horse's crystal growths and does nothing at all to
 * a horse without them. The horn colour gene's alleles and rules, through
 * {@link AbstractPartColourGene}, on a locus of its own (owner, 2026-10-01: colour is its
 * own gene per part); a gem colour is exactly what it gives (body-parts treatment).
 *
 * <p>Two different copies make two-tone crystals. The earlier allele is the see-through
 * shafts and the other the solid points, the crystal antler's mapping (base see-through,
 * tip solid). Each copy's {@code hue} and {@code tone} nudge its colour, and a chaos copy
 * wears a colour of its own. {@code Bon/Bon} is simply bone-coloured crystal: a crystal
 * has no bone, so there is no bone version to draw, and the skeleton breeds grow
 * ordinary crystals in any colour (owner, 2026-10-05). It paints nothing on the coat, so
 * every outcome is a wild type and neither coat golden reads it.
 */
public final class BackCrystalColourGene extends AbstractPartColourGene {

    public static final String KEY = "horsegenetics.back_crystal_colour";

    /** Magical band, beside {@link BackCrystalsGene}, as the horn's colour sits beside the horn. */
    public static final int PRIORITY = 337;

    public BackCrystalColourGene() {
        super(KEY, "Crystal growth colour", PRIORITY, new Words("back-crystals", "crystals", true,
                "crystals", "what nearly every crystal-grown horse has", "a horse with crystal growths",
                "the crystals are one, the base, and their points the other."));
    }

    @Override
    public boolean partGrows(Genotype genotype) {
        return Genes.BACK_CRYSTALS.shows(genotype.pair(Genes.BACK_CRYSTALS));
    }
}
