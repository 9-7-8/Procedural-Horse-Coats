package com.example.horsegenetics.common.genetics.genes;

/**
 * <b>Dorsal spine colour</b> ({@code horsegenetics.dorsal_spine_colour}) - a
 * <b>magical, codominant</b> locus that colours a horse's dorsal spines and does
 * nothing at all to a horse without them. The horn colour gene's alleles and rules,
 * through {@link AbstractPartColourGene}, on a locus of its own (owner, 2026-10-01:
 * colour is its own gene per part).
 *
 * <p>Two different copies make a two-tone row: every spine is the earlier allele at
 * its root and the other at its point, fading between (owner, 2026-10-05: base to tip
 * on each spine, not along the row). Each copy's {@code hue} and {@code tone} nudge
 * its colour, and a chaos copy wears a colour of its own. {@code Bon/Bon} is the
 * spines' bone version - bare bone, which the skeleton breeds carry. It paints
 * nothing on the coat, so every outcome is a wild type and neither coat golden reads it.
 */
public final class DorsalSpineColourGene extends AbstractPartColourGene {

    public static final String KEY = "horsegenetics.dorsal_spine_colour";

    /** Magical band, beside {@link DorsalSpinesGene}, as the horn's colour sits beside the horn. */
    public static final int PRIORITY = 242;

    public DorsalSpineColourGene() {
        super(KEY, "Dorsal spine colour", PRIORITY, new Words("dorsal-spines", "dorsal spines", true,
                "spine rows", "what nearly every spined horse has", "a horse with dorsal spines"));
    }
}
