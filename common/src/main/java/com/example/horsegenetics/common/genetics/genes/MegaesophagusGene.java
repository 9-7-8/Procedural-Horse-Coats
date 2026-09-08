package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>Megaesophagus</b> ({@code horsegenetics.megaesophagus}) - a slack gullet
 * that will not move milk, so an affected foal chokes on what it swallows.
 *
 * <p>Like <a href="../../../../../../../wiki/gene-cvm.html">CVM</a> this is a
 * <b>breed condition without a confirmed causal variant</b> - well attested and
 * clearly heritable, with no agreed single gene - so it is keyed on the
 * condition rather than on a gene symbol, and the simplification to one
 * recessive locus is the mod's rather than the literature's. See {@code CvmGene}
 * for the reasoning, which is the same reasoning.
 *
 * <p>The gentlest of the lethals on the numbers, because the failure is
 * mechanical rather than systemic: nothing is wrong with the foal except that it
 * cannot eat.
 */
public final class MegaesophagusGene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.megaesophagus";
    public static final int PRIORITY = 101;

    public static final double WILD_CARRIER_PERCENT = 1.7;

    public static final Condition MEGAESOPHAGUS = Condition.lethalAtBirth(
            "megaesophagus", "Megaesophagus",
            "The gullet is slack and does not push milk down. The foal chokes on what it "
                    + "swallows and inhales the rest.");

    public MegaesophagusGene() {
        super(KEY, "Megaesophagus", PRIORITY,
                "meg", "Megaesophagus (meg)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, MEGAESOPHAGUS);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-14.0).addSpeed(-0.03).addJump(-0.10);
    }
}
