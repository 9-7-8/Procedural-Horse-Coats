package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;

/**
 * <b>Mane colour</b> ({@code horsegenetics.mane_color}) - a <b>magical</b> gene.
 * Alleles {@code Mnsld} (solid), {@code Mnstrp} (striped) and {@code n}; the
 * whole table, and why the heterozygote is the interesting combination, is on
 * {@link HairColorGene}.
 *
 * <p>It is one of a matched pair with {@link TailColorGene}, which is a separate
 * locus doing the same thing to the tail - so a horse can be one colour at one
 * end and another at the other, or magenta-maned and perfectly ordinary behind.
 * Keeping them apart is the same rule the white-pattern loci follow: only
 * alleles at the same locus compete for a slot, and mane and tail plainly do not.
 *
 * <p><b>A foal shows nothing here.</b> The foal mesh has no {@code MANE} part at
 * all, so the colour arrives with adulthood - the same way pink hair gives a foal
 * a pink tail and nothing else.
 *
 * <p>Three alleles is a starting point, not the shape of the gene. A third
 * pattern - dip-dyed, tipped, roots - is one more allele, one more expression
 * and one more painter, and every existing combination keeps its meaning.
 */
public final class ManeColorGene extends HairColorGene {

    public static final String KEY = "horsegenetics.mane_color";
    public static final int PRIORITY = 112;

    public static final double WILD_SOLID_FREQUENCY = 0.020;
    public static final double WILD_STRIPED_FREQUENCY = 0.015;

    public ManeColorGene() {
        super(KEY, "Mane colour", PRIORITY, Part.MANE,
                "Mnsld", "Solid mane (Mnsld)",
                "Mnstrp", "Striped mane (Mnstrp)",
                "mane", WILD_SOLID_FREQUENCY, WILD_STRIPED_FREQUENCY);
    }
}
