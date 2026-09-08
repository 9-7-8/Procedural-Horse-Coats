package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>MYO5A</b> ({@code horsegenetics.myo5a}) - <b>lavender foal syndrome</b>
 * (coat colour dilution lethal). A neurological defect that arrives alongside a
 * pale, silvery coat.
 *
 * <p><b>The dilution is not painted, and that is deliberate.</b> MYO5A is the
 * only lethal in the mod whose real presentation includes a colour, and it would
 * be the obvious thing to draw - but the foal dies within seconds of being born,
 * so a player would essentially never see it, and phase 1 can only <i>remove</i>
 * pigment (see <a href="../../../../../../../wiki/gene-st14.html">ST14</a> for
 * the same problem stated at length). Drawing a wrong lavender for a moment is
 * worse than drawing nothing. If a delayed-death path ever lands, this is the
 * first gene that would want a coat.
 */
public final class Myo5aGene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.myo5a";
    public static final int PRIORITY = 97;

    public static final double WILD_CARRIER_PERCENT = 2.2;

    public static final Condition LAVENDER_FOAL = Condition.lethalAtBirth(
            "lavender-foal-syndrome", "Lavender foal syndrome",
            "A neurological defect that arrives with a diluted, silvery coat. The foal "
                    + "cannot stand, lies rigid with its head thrown back, and does not "
                    + "survive.");

    public Myo5aGene() {
        super(KEY, "MYO5A (lavender foal)", PRIORITY,
                "lfs", "Lavender foal (lfs)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, LAVENDER_FOAL);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-16.0).addSpeed(-0.05).addJump(-0.25);
    }
}
