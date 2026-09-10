package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Ender echo</b> ({@code horsegenetics.ender_echo}) - shoot at it and it is
 * not there any more.
 *
 * <h2>It takes you with it</h2>
 * The rider comes along, which is either the best thing about the gene or the
 * worst depending on what is eight blocks away. Leaving the rider behind was
 * considered and rejected: "my horse abandoned me under fire" is a hard sell,
 * and a horse that teleports out from under you is a bug-shaped feature.
 *
 * <p>{@link #DISTANCE} is deliberately small. Far enough to break a line of
 * fire, not far enough to be transport - uncapped this is free combat mobility
 * and the best movement item in the mod attached to an animal.
 *
 * <h2>A survival trait, borrowed</h2>
 * The mod's magical loci mostly grant things no animal has. This one grants
 * something another mob in the same world plainly does, which makes it the
 * easiest magical gene to believe in - the question it raises is not "how" but
 * "where did a horse get enderman in it".
 */
public final class EnderEchoGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.ender_echo";
    public static final int PRIORITY = 172;

    /** Blocks. Small on purpose - a dodge, not transport. */
    public static final double DISTANCE = 8.0;

    /** Ticks between blinks, so a burst of arrows is one dodge rather than five. */
    public static final int COOLDOWN_TICKS = 40;

    public EnderEchoGene() {
        super(KEY, PRIORITY, "Ender echo",
                "End", "Echoing (End)",
                Dominance.RECESSIVE, Founders.CARRIERS_ONLY, 4.0,
                "Arrows hit the horse, as arrows do.",
                "Echoing",
                "Two copies. When something damages the horse it blinks about eight blocks away, carrying whoever is riding it. Against skeletons this is close to immunity; against a skeleton on the far side of a ravine it is a short trip you did not plan.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(new GeneAbility.Teleport(DISTANCE, true,
                new GeneAbility.Trigger.OnHurt(), COOLDOWN_TICKS, GeneAbility.Condition.ALWAYS, 1));
    }
}
