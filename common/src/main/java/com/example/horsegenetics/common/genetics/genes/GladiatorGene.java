package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Gladiator</b> ({@code horsegenetics.gladiator}) - it picks fights with
 * anything hostile that comes close, <b>unless you are riding it</b>.
 *
 * <h2>It holds its ground</h2>
 * It does not chase. Anything that walks into range is a problem it will solve;
 * anything that stays away is not its business. Full pursuit and owner-leashed
 * pursuit were both specified and both rejected - a horse that chases is a horse
 * that dies forty blocks from its owner, and the owner blames the gene,
 * correctly. If a later session wants either, it is reopening a settled call.
 *
 * <h2>Why it stops when you get on</h2>
 * A war horse that fights while you are steering it is a horse fighting your
 * steering. The gate is what makes the gene usable: a guard when you are away
 * and a mount when you are there, which are the two states a horse is ever
 * actually in.
 *
 * <p>It makes the horse <i>willing</i>; how hard it hits is
 * {@link MagicFighterGene}, and a gladiator without one bravely inconveniences a
 * zombie.
 */
public final class GladiatorGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.gladiator";
    public static final int PRIORITY = 169;

    public static final double RADIUS = 12.0;
    public static final int INTERVAL_TICKS = 20;
    public static final int MAX_TARGETS = 6;

    public GladiatorGene() {
        super(KEY, PRIORITY, "Gladiator",
                "Gld", "Gladiator (Gld)",
                Dominance.RECESSIVE, Founders.CARRIERS_ONLY, 5.0,
                "The horse minds its own business, as horses do.",
                "Gladiator",
                "Two copies. While nobody is riding it, the horse attacks hostile mobs that come within reach - and returns rather than pursuing them. Mount it and it stops immediately. It picks fights, so it takes damage; pair it with magic health and a real attack or expect losses.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        // hold = true, and the condition is re-checked every beat rather than at
        // engagement, so mounting mid-fight stops it on the next tick.
        return List.of(new GeneAbility.Temper("aggressive", "hostile", RADIUS, INTERVAL_TICKS,
                MAX_TARGETS, true, new GeneAbility.Trigger.Continuous(),
                new GeneAbility.Condition.Flag("has_rider", true), 1));
    }
}
