package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Cheek spike colour</b> ({@code horsegenetics.cheek_spike_colour}) - a <b>magical,
 * codominant</b> locus that colours a horse's cheek spikes and does nothing at all to a
 * horse without them. The horn colour gene's alleles and rules, through
 * {@link AbstractPartColourGene}, on a locus of its own (owner, 2026-10-01: colour is
 * its own gene per part).
 *
 * <p>Two different copies make two-tone spikes, one colour at the root and the other at
 * the point, on every spike. Each copy's {@code hue} and {@code tone} nudge its colour,
 * and a chaos copy wears a colour of its own. {@code Bon/Bon} is the spikes' bone
 * version - bare bone, which the skeleton breeds carry. It paints nothing on the coat,
 * so every outcome is a wild type and neither coat golden reads it.
 */
public final class CheekSpikeColourGene extends AbstractPartColourGene {

    public static final String KEY = "horsegenetics.cheek_spike_colour";

    /** Magical band, beside {@link CheekSpikesGene}, as the horn's colour sits beside the horn. */
    public static final int PRIORITY = 346;

    public CheekSpikeColourGene() {
        super(KEY, "Cheek spike colour", PRIORITY, new Words("cheek-spikes", "cheek spikes", true,
                "spikes", "what nearly every spiked horse has", "a horse with cheek spikes"));
    }

    @Override
    public boolean partGrows(Genotype genotype) {
        return Genes.CHEEK_SPIKES.shows(genotype.pair(Genes.CHEEK_SPIKES));
    }
}
