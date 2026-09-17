package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>LAMC2</b> ({@code horsegenetics.lamc2}) - <b>junctional epidermolysis
 * bullosa</b>, the draft-horse variant.
 *
 * <p>One of two JEB loci in this mod, and they are deliberately two. JEB in the
 * draft breeds is a <i>LAMC2</i> mutation; the American Saddlebred's is a partial
 * <i>LAMA3</i> deletion (see {@link Lama3Gene}). They are different mutations in
 * different genes producing the same clinical picture, so a horse can in
 * principle carry one and be clear of the other, and a Belgian's test result says
 * nothing about a Saddlebred's. Folding them into one locus with two alleles
 * would have made the two mutually exclusive, which is the one thing they are
 * not.
 *
 * <h2>What an affected foal looks like</h2>
 * The anchoring layer between skin and the tissue beneath it does not form, so
 * the two shear apart wherever the foal takes weight or pressure. The hoof
 * capsules separate and slough; skin is lost over the joints, the muzzle and the
 * pressure points. It is not survivable, and the mod treats it as the ordinary
 * born-then-dies lethal.
 *
 * <p>This is one of exactly two loci on the American Cream Draft's real
 * association test panel - the other being PSSM1 ({@link Gys1Gene}), which
 * already shipped - which is what made its absence the most conspicuous gap on
 * any shipped breed's own panel.
 */
public final class Lamc2Gene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.lamc2";
    public static final int PRIORITY = 103;

    /** Founders unaffiliated with a breed; the draft breeds carry it higher. */
    public static final double WILD_CARRIER_PERCENT = 0.8;

    public static final Condition JEB_DRAFT = Condition.lethalAtBirth(
            "junctional-epidermolysis-bullosa-lamc2", "Junctional epidermolysis bullosa (LAMC2)",
            "The layer that anchors skin to the body never formed. The hoof walls separate "
                    + "and the skin tears away wherever the foal puts weight. It does not survive.");

    public Lamc2Gene() {
        super(KEY, "LAMC2", PRIORITY,
                "jeb", "Epidermolysis bullosa (jeb)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, JEB_DRAFT);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-13.0).addSpeed(-0.06).addJump(-0.20);
    }
}
