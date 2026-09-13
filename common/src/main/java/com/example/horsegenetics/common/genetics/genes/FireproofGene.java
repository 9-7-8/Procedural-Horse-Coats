package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
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

    /** The one value a copy carries: how fast it swims through lava. */
    public static final String LAVA_SPEED = "lavaSpeed";

    /**
     * <b>The floor is an ordinary walk, and the ceiling is a swim.</b>
     *
     * <p>Vanilla's lava acceleration is {@code 0.02}, and the same method halves
     * the remaining velocity each tick, so it settles near 0.04 blocks a tick -
     * about a fifth of walking pace. That is the crawl the owner asked to be rid
     * of, and {@code known-gaps.html} gap 179 is the record of it.
     *
     * <p>{@value #MIN_LAVA_SPEED} is five times vanilla and lands near an
     * ordinary walk, which is the <b>founder floor</b>: a wild fireproof horse
     * caught in the world already crosses lava at a sensible pace, because a
     * gene whose headline is "lava stops being a detour" should not need a
     * breeding programme before it is true at all. {@value #MAX_LAVA_SPEED} is
     * twenty times vanilla, and that is what there is to breed <i>for</i>.
     */
    public static final double MIN_LAVA_SPEED = 0.10;
    public static final double MAX_LAVA_SPEED = 0.40;

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(LAVA_SPEED, MIN_LAVA_SPEED, MAX_LAVA_SPEED));
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        // target BOTH on each: the rider is the point of the gene, and the
        // translator grants a rider flag per tick without storing anything, so
        // there is nothing left on a player who steps off.
        //
        // The speed is an ATTRIBUTE and not a traversal flag, and that is not a
        // tidiness choice. A ridden horse is simulated on the rider's client, so
        // anything the server does to its velocity is overwritten by the
        // position packets coming back - which is why the buoyancy beside this
        // is a GRAVITY modifier too. Attributes sync; velocity does not.
        // 'lava_movement' is this mod's own attribute: vanilla's lava travel
        // hardcodes its speed and reads nothing, and a mixin makes that constant
        // consult this. See neoforge ModAttributes and LavaTravelMixin.
        return List.of(
                new GeneAbility.Traversal("fire_immune", "both", GeneAbility.Condition.ALWAYS, 1),
                new GeneAbility.Traversal("lava_swim", "both", GeneAbility.Condition.ALWAYS, 1),
                new GeneAbility.AttributeMod("lava_movement", "add",
                        epi.get(LAVA_SPEED) - VANILLA_LAVA_SPEED,
                        GeneAbility.Condition.ALWAYS, 1));
    }

    /** Vanilla's constant, which the attribute defaults to - so 'add' is the delta from it. */
    private static final double VANILLA_LAVA_SPEED = 0.02;
}
