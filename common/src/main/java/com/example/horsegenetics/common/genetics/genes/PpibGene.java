package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>PPIB</b> ({@code horsegenetics.ppib}) - <b>HERDA</b>, hereditary equine
 * regional dermal asthenia: the collagen holding the skin to the horse is
 * faulty, so it splits and scars where a saddle sits.
 *
 * <p>The <b>most common survivable disorder on the recessive side</b>, and the
 * one whose carrier rate is set highest, because in the real population it hides
 * inside a very narrow and very heavily used set of working lines - exactly the
 * animals a breeder would choose. That is the whole shape of the gene: it is
 * concentrated in the horses you most want.
 *
 * <p>Chronic rather than episodic, which the mod has no way to express yet - see
 * <a href="../../../../../../../wiki/roadmap.html#decisions">roadmap &sect;3</a>.
 * Rendered as a flat cost, like everything else on this layer.
 */
public final class PpibGene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.ppib";
    public static final int PRIORITY = 95;

    public static final double WILD_CARRIER_PERCENT = 3.4;

    public static final Condition HERDA = Condition.impairing(
            "herda", "HERDA (fragile skin)",
            "A collagen defect in the skin. It splits and scars where a saddle sits and "
                    + "never heals cleanly, and the horse is sore enough that it will not work "
                    + "properly. It survives; it is just never sound.");

    public PpibGene() {
        super(KEY, "PPIB (HERDA)", PRIORITY,
                "herda", "HERDA (herda)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, HERDA);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-7.0).addSpeed(-0.025).addJump(-0.15);
    }
}
