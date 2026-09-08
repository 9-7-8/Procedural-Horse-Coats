package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>PRKDC</b> ({@code horsegenetics.prkdc}) - <b>SCID</b>, severe combined
 * immunodeficiency. An affected foal is born with no adaptive immune system.
 *
 * <p>Lethal at birth, so the foal is really born: it gets a name, a record and a
 * place in the family tree before it dies. That is the point of the
 * {@code LETHAL_AT_BIRTH} path - a pairing that silently produced nothing would
 * teach a player nothing about the two horses they just bred.
 *
 * <p>The cruellest of the foal lethals in flavour, because a real SCID foal is
 * perfectly well for its first days on its dam's borrowed antibodies. The mod
 * has no delayed-death path, so it dies with the others; the sentence carries
 * what the mechanism cannot.
 */
public final class PrkdcGene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.prkdc";
    public static final int PRIORITY = 96;

    public static final double WILD_CARRIER_PERCENT = 2.8;

    public static final Condition SCID = Condition.lethalAtBirth(
            "scid", "SCID (no immune system)",
            "The foal is born with no working immune system at all. It is healthy for a "
                    + "few days on its dam's antibodies and then has nothing left to fight "
                    + "with.");

    public PrkdcGene() {
        super(KEY, "PRKDC (SCID)", PRIORITY,
                "scid", "SCID (scid)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, SCID);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-17.0).addSpeed(-0.03).addJump(-0.12);
    }
}
