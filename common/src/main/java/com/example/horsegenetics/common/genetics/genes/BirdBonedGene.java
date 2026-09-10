package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Bird boned</b> ({@code horsegenetics.bird_boned}) - hollow-boned and light
 * with it. Neither the horse nor its rider takes falling damage, from any
 * height.
 *
 * <h2>A gene whose value is mostly in another locus</h2>
 * On its own it is a safety net you rarely notice. Bred alongside
 * {@link MagicJumpGene} it turns a stat that mostly kills horses into one worth
 * pushing, which is the same shape {@link MagicMobAuraGene}'s baiting allele has
 * beside health and fighting: a gene that only becomes a strategy when a second
 * locus is bred with it.
 *
 * <p>It removes fall <i>damage</i>, not falling. A horse that goes into a ravine
 * is unhurt and still in a ravine.
 */
public final class BirdBonedGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.bird_boned";
    public static final int PRIORITY = 156;

    public BirdBonedGene() {
        super(KEY, PRIORITY, "Bird boned",
                "Brd", "Bird-boned (Brd)",
                Dominance.RECESSIVE, Founders.CARRIERS_ONLY, 7.0,
                "The horse takes falling damage on the usual schedule, and so do you.",
                "Bird-boned",
                "Two copies. Neither the horse nor its rider takes any falling damage at all, from any height. Mountains stop having a wrong side and a ravine becomes a shortcut.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(new GeneAbility.Traversal("fall_immune", "both", GeneAbility.Condition.ALWAYS, 1));
    }
}
