package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>Skeletal atavism</b> ({@code horsegenetics.shox}) - a deletion in the
 * pseudoautosomal {@code SHOX} region. The ulna and the fibula, which in a
 * normal horse are reduced splints fused to the bones beside them, grow out to
 * their full ancestral length; the limbs come out malformed and the foal cannot
 * stand on them.
 *
 * <p>Modelled as an ordinary autosomal recessive. The region it sits in is
 * shared between the X and the Y, so it segregates exactly like an autosome even
 * though it lives on a sex chromosome - which is why it needs none of the
 * sex-linked inheritance scaffolding that {@code wiki/roadmap.html} 5.3 is still
 * waiting on. If that scaffolding ever lands, this gene is the one to check it
 * against, because it must keep behaving the way it does now.
 */
public final class ShoxGene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.shox";
    public static final int PRIORITY = 91;

    public static final double WILD_CARRIER_PERCENT = 2.0;

    public static final Condition SKELETAL_ATAVISM = Condition.lethalAtBirth(
            "skeletal-atavism", "Skeletal atavism",
            "The splint bones grow out to their full ancestral length and the limbs come out "
                    + "malformed. The foal cannot stand and does not survive.");

    public ShoxGene() {
        super(KEY, "SHOX (skeletal atavism)", PRIORITY,
                "sa", "Skeletal atavism (sa)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, SKELETAL_ATAVISM);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-11.0).multiplyScale(0.9).addSpeed(-0.06).addJump(-0.3);
    }
}
