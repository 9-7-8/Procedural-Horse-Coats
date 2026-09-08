package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.TraitBuilder;

/**
 * <b>CVM</b> ({@code horsegenetics.cvm}) - <b>cervical vertebral
 * malformation</b>, the &ldquo;wobbler&rdquo; presentation, modelled here in its
 * severe early-lethal form.
 *
 * <h2>This one has no confirmed causal variant, and says so</h2>
 * Every other natural gene in the mod names a real locus with a real published
 * mutation behind it. CVM does not have one: it is a well-attested heritable
 * malformation with a strong breed concentration and no agreed single gene, so
 * the key is the condition's own name rather than a gene symbol invented to look
 * respectable. Treating it as one recessive locus is <b>the mod's
 * simplification</b>, not a claim about horse genetics, and it is written down
 * here so a later reader does not go looking for the paper.
 *
 * <p>The alternative was to leave it out. It earns its place because the
 * roadmap's disorder table wants a spread of breeds represented and because the
 * simplification is honest at the level the mod models anything - every locus
 * here is one gene, one number, one outcome.
 */
public final class CvmGene extends RecessiveDisorderGene {

    public static final String KEY = "horsegenetics.cvm";
    public static final int PRIORITY = 99;

    public static final double WILD_CARRIER_PERCENT = 1.6;

    public static final Condition CERVICAL_MALFORMATION = Condition.lethalAtBirth(
            "cervical-malformation", "Cervical vertebral malformation",
            "The neck vertebrae form crooked and pinch the spinal cord where it passes "
                    + "through them. The foal cannot coordinate its hindquarters and does not "
                    + "get up.");

    public CvmGene() {
        super(KEY, "CVM (cervical malformation)", PRIORITY,
                "cvm", "Cervical malformation (cvm)", "N", "Wild-type (N)",
                WILD_CARRIER_PERCENT, CERVICAL_MALFORMATION);
    }

    @Override
    protected void affect(TraitBuilder out) {
        out.addHealth(-15.0).addSpeed(-0.05).addJump(-0.28);
    }
}
