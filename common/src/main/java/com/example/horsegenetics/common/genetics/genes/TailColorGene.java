package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;

/**
 * <b>Tail colour</b> ({@code horsegenetics.tail_color}) - a <b>magical</b> gene,
 * and {@link ManeColorGene}'s twin: alleles {@code Tlsld} (solid),
 * {@code Tlstrp} (striped) and {@code n}, with the same combination table and
 * the same per-copy colour. See {@link HairColorGene}.
 *
 * <p><b>A separate locus, deliberately.</b> Folding mane and tail into one gene
 * would make a horse with a red mane and a blue tail impossible to breed, which
 * is exactly the kind of horse this family of genes exists to produce. It also
 * doubles what two of them are worth: four independent colours across two loci,
 * before anything else on the horse is considered.
 *
 * <p>Unlike the mane locus this one <b>shows on a foal</b> - the foal mesh has a
 * tail - so it is the earlier of the two to read off a young horse.
 */
public final class TailColorGene extends HairColorGene {

    public static final String KEY = "horsegenetics.tail_color";
    public static final int PRIORITY = 114;

    public static final double WILD_SOLID_FREQUENCY = 0.020;
    public static final double WILD_STRIPED_FREQUENCY = 0.015;

    public TailColorGene() {
        super(KEY, "Tail colour", PRIORITY, Part.TAIL,
                "Tlsld", "Solid tail (Tlsld)",
                "Tlstrp", "Striped tail (Tlstrp)",
                "tail", WILD_SOLID_FREQUENCY, WILD_STRIPED_FREQUENCY);
    }
}
