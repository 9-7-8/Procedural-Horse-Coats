package com.example.horsegenetics.common.genetics.genes;

/**
 * <b>Tusk colour</b> ({@code horsegenetics.tusk_colour}) - a <b>magical, codominant</b>
 * locus that colours whatever the tusks locus grows and does nothing at all to a horse
 * without it. The horn colour gene's alleles and rules, through
 * {@link AbstractPartColourGene}, on a locus of its own (owner, 2026-10-01: colour is
 * its own gene per part).
 *
 * <p>One colour locus for the whole tusks gene, so every form on a horse wears the same
 * tints (tusks treatment): a horse with fangs and a narwhal horn has both ivory, or both
 * red. White is the wild type, which reads as ivory. Two different copies make a
 * two-tone part, the earlier allele at the root and the other at the tip; each copy's
 * {@code hue} and {@code tone} nudge its colour, and a chaos copy wears a colour of its
 * own. {@code Bon/Bon} is the bone version - a tusk is a tooth, so on a skeleton it is
 * bare bone. It paints nothing on the coat, so every outcome is a wild type and neither
 * coat golden reads it.
 */
public final class TuskColourGene extends AbstractPartColourGene {

    public static final String KEY = "horsegenetics.tusk_colour";

    /** Magical band, beside {@link TusksGene}, as the horn's colour sits beside the horn. */
    public static final int PRIORITY = 335;

    public TuskColourGene() {
        super(KEY, "Tusk colour", PRIORITY, new Words("tusks", "tusks", true,
                "tusks", "ivory, what nearly every tusked horse has", "a horse with tusks"));
    }
}
