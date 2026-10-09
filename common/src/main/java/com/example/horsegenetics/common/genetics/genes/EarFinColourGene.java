package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Ear fin colour</b> ({@code horsegenetics.ear_fin_colour}) - a <b>magical,
 * codominant</b> locus that colours a horse's ear fins and does nothing at all to a
 * horse without them. The horn colour gene's alleles and rules, through
 * {@link AbstractPartColourGene}, on a locus of its own (owner, 2026-10-01: colour is
 * its own gene per part).
 *
 * <p>Two different copies make two-tone fins: every ray is the base colour and its
 * point - or, on a frill, the rod down its middle - the other. Each copy's {@code hue}
 * and {@code tone} nudge its colour, and a chaos copy wears a colour of its own. An ear
 * fin has no bone version (owner, 2026-10-09), so {@code Bon/Bon} is simply bone-coloured
 * fins and no breed pins it. It paints nothing on the coat, so every outcome is a wild
 * type and neither coat golden reads it.
 */
public final class EarFinColourGene extends AbstractPartColourGene {

    public static final String KEY = "horsegenetics.ear_fin_colour";

    /** Magical band, beside {@link EarFinsGene}, as the horn's colour sits beside the horn. */
    public static final int PRIORITY = 344;

    public EarFinColourGene() {
        super(KEY, "Ear fin colour", PRIORITY, new Words("ear-fins", "ear fins", true,
                "fins", "what nearly every finned horse has", "a horse with ear fins",
                "each ray is one, the base, and its point or rod the other."));
    }

    @Override
    public boolean partGrows(Genotype genotype) {
        return Genes.EAR_FINS.shows(genotype.pair(Genes.EAR_FINS));
    }
}
