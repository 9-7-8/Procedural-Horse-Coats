package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Horn colour</b> ({@code horsegenetics.horn_colour}) - a <b>magical,
 * codominant</b> locus that colours a unicorn's horn and does nothing at all to a
 * horse without one. One of the four genes the horn is built from: the
 * {@link UnicornHornGene unicorn locus} grows its shape, this colours it,
 * {@link HornGlowGene} lights it and {@link HornDustGene} makes it shed.
 *
 * <p>The alleles ({@code Wht}, the rainbow, {@code Pnk Blk Gry}, {@code Cha}), the
 * two-tone rule and the per-copy {@code hue} / {@code tone} / {@code chaos} numbers
 * are {@link AbstractPartColourGene}'s, which was cut out of this class (2026-10-02)
 * so every grown part can have a colour locus of its own. Nothing about this gene
 * moved when it did: {@code PartColourGeneTest} pins its whole observable state to
 * a snapshot taken before.
 *
 * <p>It paints nothing on the coat, so every outcome is a wild type and neither
 * coat golden reads it.
 */
public final class HornColourGene extends AbstractPartColourGene {

    public static final String KEY = "horsegenetics.horn_colour";

    /** Magical band, in a free run of three with {@link HornGlowGene} and {@link HornDustGene}. */
    public static final int PRIORITY = 227;

    public HornColourGene() {
        super(KEY, "Horn colour", PRIORITY, new Words("horn", "horn", false, "horns",
                "what nearly every unicorn has", "a horse with a horn"));
    }

    @Override
    public boolean partGrows(Genotype genotype) {
        return Genes.UNICORN_HORN.shows(genotype.pair(Genes.UNICORN_HORN));
    }
}
