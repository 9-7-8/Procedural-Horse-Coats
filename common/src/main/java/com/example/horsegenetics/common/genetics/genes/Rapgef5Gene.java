package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>RAPGEF5</b> ({@code horsegenetics.rapgef5}) - <b>EFIH</b>, equine familial
 * isolated hypoparathyroidism. An affected foal develops no parathyroid glands
 * at all and cannot regulate its own calcium.
 *
 * <p>The most severe of the four foal lethals - it takes the largest bite out of
 * the foal max health, so it dies fastest - and the rarest. That pairing is the
 * point: the disorders a player is most likely to run into are the ones nearest
 * to survivable, and the ones that kill outright are the ones that have been
 * bred down hardest.
 */
public final class Rapgef5Gene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.rapgef5";
    public static final int PRIORITY = 89;

    public static final double WILD_CARRIER_PERCENT = 1.4;

    public static final Condition EFIH = Condition.lethalAtBirth(
            "efih", "Hypoparathyroidism (EFIH)",
            "The foal is born with no parathyroid glands and cannot hold its calcium. "
                    + "It seizes and does not survive.");

    public Rapgef5Gene() {
        super(KEY, "RAPGEF5", PRIORITY,
                "efih", "EFIH (efih)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, EFIH);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-15.0).addSpeed(-0.06).addJump(-0.25);
    }
}
