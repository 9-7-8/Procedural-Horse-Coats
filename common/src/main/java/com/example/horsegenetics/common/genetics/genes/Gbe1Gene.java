package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>GBE1</b> ({@code horsegenetics.gbe1}) - <b>GBED</b>, glycogen branching
 * enzyme deficiency. The foal cannot store sugar in a form it can get back out.
 *
 * <p>The <b>largest heart reduction in the mod</b>, which is the right shape for
 * a disorder whose real form kills most affected foals before they are born at
 * all. It is {@code LETHAL_AT_BIRTH} rather than
 * {@code LETHAL_AT_CONCEPTION} on purpose: <a href="../../../../../../../wiki/gene-met.html">MET</a>
 * owns the conception path and owning it alone is what makes that path legible -
 * one gene where a pairing simply yields nothing, and everything else born and
 * lost where a player can see it happen.
 */
public final class Gbe1Gene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.gbe1";
    public static final int PRIORITY = 100;

    public static final double WILD_CARRIER_PERCENT = 2.0;

    public static final Condition GBED = Condition.lethalAtBirth(
            "gbed", "Glycogen branching enzyme deficiency",
            "The foal cannot store or release sugar. It is weak from birth, cannot keep "
                    + "itself warm, and has nothing to draw on.");

    public Gbe1Gene() {
        super(KEY, "GBE1 (GBED)", PRIORITY,
                "gbed", "GBED (gbed)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, GBED);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-18.0).addSpeed(-0.06).addJump(-0.25);
    }
}
