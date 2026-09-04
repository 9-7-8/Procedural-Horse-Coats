package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>ST14</b> ({@code horsegenetics.st14}) - <b>naked foal syndrome</b>, the one
 * health gene that is supposed to be a <i>coat</i> gene as well: an affected
 * foal is born near-hairless.
 *
 * <p><b>The coat half is not built.</b> Phase 1 of the pipeline can only push
 * pigment down, and "no hair" is not "less pigment" - a de-pigmented mane reads
 * as a <i>white</i> mane, which is a different horse entirely and a worse lie
 * than drawing nothing at all. Doing it honestly means a bare-skin template or a
 * real phase-3 pass, and it is logged as a follow-up in
 * {@code wiki/roadmap.html} 4.4 rather than approximated here. What ships is the
 * disorder itself: the foal is born, the info panel names it as hairless, and it
 * does not survive.
 */
public final class St14Gene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.st14";
    public static final int PRIORITY = 90;

    public static final double WILD_CARRIER_PERCENT = 1.8;

    public static final Condition NAKED_FOAL = Condition.lethalAtBirth(
            "naked-foal-syndrome", "Naked foal syndrome",
            "The foal is born with almost no hair and cannot hold its own temperature. "
                    + "It does not survive. (The bare coat itself is not drawn yet.)");

    public St14Gene() {
        super(KEY, "ST14", PRIORITY,
                "nfs", "Naked foal (nfs)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, NAKED_FOAL);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-13.0).addSpeed(-0.04).addJump(-0.15);
    }
}
