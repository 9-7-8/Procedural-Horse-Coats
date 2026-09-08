package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>TOE1</b> ({@code horsegenetics.toe1}) - <b>cerebellar abiotrophy</b>: the
 * Purkinje cells governing balance die off after birth, leaving a horse with no
 * sense of where its own feet are.
 *
 * <p><b>The heaviest jump penalty on the health layer</b>, and the only one where
 * that is the <i>headline</i> symptom rather than a side effect. A CA horse
 * cannot judge a distance, so it is the one disorder whose cost a player will
 * feel through the reins rather than read in a panel.
 *
 * <p>Progressive in reality - a foal looks normal and worsens over months - which
 * the mod cannot express without an age model it deliberately does not have (see
 * <a href="../../../../../../../wiki/known-gaps.html#gap-1">gap 1</a>). It is a
 * flat cost from birth here.
 */
public final class Toe1Gene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.toe1";
    public static final int PRIORITY = 98;

    public static final double WILD_CARRIER_PERCENT = 2.4;

    public static final Condition CEREBELLAR_ABIOTROPHY = Condition.impairing(
            "cerebellar-abiotrophy", "Cerebellar abiotrophy",
            "The part of the brain that governs balance wastes away after birth. The "
                    + "horse has no sense of where its feet are: it moves in a wide, "
                    + "over-reaching stagger and cannot judge a jump at all.");

    public Toe1Gene() {
        super(KEY, "TOE1 (cerebellar abiotrophy)", PRIORITY,
                "ca", "Cerebellar abiotrophy (ca)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, CEREBELLAR_ABIOTROPHY);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-8.0).addSpeed(-0.045).addJump(-0.30);
    }
}
