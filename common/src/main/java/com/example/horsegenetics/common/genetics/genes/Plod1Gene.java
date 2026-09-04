package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>PLOD1</b> ({@code horsegenetics.plod1}) - <b>fragile foal syndrome</b>
 * (FFS1), a collagen defect. An affected foal is born with skin that tears at a
 * touch and joints that will not hold, and does not survive it.
 *
 * <p>Lethal <i>at birth</i>, so the foal really is born: it gets a name, a
 * record and a place in the family tree, and then dies over a few seconds with a
 * chat line naming what took it. That is deliberate. A pairing that silently
 * produced nothing would teach a player nothing about the two horses they just
 * bred, and the whole value of a recessive lethal is that it tells you something
 * about both parents at once.
 *
 * <p>The highest carrier rate of the four foal lethals, because in the real
 * population it is the one hiding inside the largest number of otherwise
 * excellent animals.
 */
public final class Plod1Gene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.plod1";
    public static final int PRIORITY = 88;

    public static final double WILD_CARRIER_PERCENT = 2.6;

    public static final Condition FRAGILE_FOAL = Condition.lethalAtBirth(
            "fragile-foal-syndrome", "Fragile foal syndrome",
            "A collagen defect: the skin tears at a touch and the joints will not hold. "
                    + "The foal is born alive and does not survive it.");

    public Plod1Gene() {
        super(KEY, "PLOD1", PRIORITY,
                "ffs", "Fragile foal (ffs)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, FRAGILE_FOAL);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-13.0).addSpeed(-0.05).addJump(-0.2);
    }
}
