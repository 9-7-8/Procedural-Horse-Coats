package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>LAMA3</b> ({@code horsegenetics.lama3}) - <b>junctional epidermolysis
 * bullosa</b>, the American Saddlebred variant, and the best-measured disorder
 * frequency in this mod.
 *
 * <p>The second of two JEB loci; {@link Lamc2Gene} is the draft-horse one, and
 * the class note there explains why they are two genes rather than two alleles.
 * This one is a partial deletion of <i>LAMA3</i>.
 *
 * <h2>The number is real, which is rare here</h2>
 * Most founder frequencies in this mod are chosen so that a wild horse is healthy
 * and an inbred line is not. This one is not chosen: a <b>random 2007 foal-crop
 * sample found 9 carriers in 175 foals</b>, a mutant allele frequency of 0.026.
 * That is a random cohort rather than a tested-because-suspected one, which makes
 * it better evidence than most of the health layer carries, and it is what the
 * Saddlebred's own breed file uses.
 *
 * <p>{@link #WILD_CARRIER_PERCENT} here is the unaffiliated wild horse, which is
 * a different and much smaller population - the measured figure belongs in the
 * breed file, not in the global table.
 */
public final class Lama3Gene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.lama3";
    public static final int PRIORITY = 104;

    /** Founders unaffiliated with a breed. The Saddlebred's measured rate is in its own file. */
    public static final double WILD_CARRIER_PERCENT = 1.0;

    public static final Condition JEB_SADDLEBRED = Condition.lethalAtBirth(
            "junctional-epidermolysis-bullosa-lama3", "Junctional epidermolysis bullosa (LAMA3)",
            "A missing piece of LAMA3, and the skin has nothing to hold on to. The hoof "
                    + "capsules come away and the skin splits at every pressure point. The foal "
                    + "does not survive.");

    public Lama3Gene() {
        super(KEY, "LAMA3", PRIORITY,
                "jeb", "Epidermolysis bullosa (jeb)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, JEB_SADDLEBRED);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-13.0).addSpeed(-0.06).addJump(-0.20);
    }
}
