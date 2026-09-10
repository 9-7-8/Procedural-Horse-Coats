package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Fireproof</b> ({@code horsegenetics.fireproof}) - the horse does not burn,
 * neither does its rider, and it <b>swims through lava</b> rather than walking
 * on top of it.
 *
 * <h2>Swimming, not walking</h2>
 * Walking on lava was the cheaper build - the {@code walk_on_water} branch
 * already does the buoyancy trick and pointing it at lava is a few lines. It was
 * rejected because a horse trotting across a lava lake reads as a bug in
 * somebody else's mod, whereas a horse wading <i>through</i> it reads as an
 * animal that belongs there.
 *
 * <h2>One locus, not two</h2>
 * Fire immunity and lava swimming could have been separate genes, the way
 * swimming and breathing are separate on the water side. They are one here
 * because immunity without the swimming is a horse that stands in fire unharmed
 * and can do nothing with it - there is no interesting animal in between, so
 * there is no reason to make a breeder find two.
 */
public final class FireproofGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.fireproof";
    public static final int PRIORITY = 155;

    public FireproofGene() {
        super(KEY, PRIORITY, "Fireproof",
                "Frp", "Fireproof (Frp)",
                Dominance.RECESSIVE, Founders.CARRIERS_ONLY, 6.0,
                "The horse burns like anything else, and so do you.",
                "Fireproof",
                "Two copies. Neither the horse nor its rider takes fire damage, and the horse swims through lava the way an ordinary horse swims through water - slowly, at the surface, entirely unbothered. A lava lake stops being a detour.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        // target BOTH on each: the rider is the point of the gene, and the
        // translator grants a rider flag per tick without storing anything, so
        // there is nothing left on a player who steps off.
        return List.of(
                new GeneAbility.Traversal("fire_immune", "both", GeneAbility.Condition.ALWAYS, 1),
                new GeneAbility.Traversal("lava_swim", "both", GeneAbility.Condition.ALWAYS, 1));
    }
}
