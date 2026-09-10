package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Guardian</b> ({@code horsegenetics.guardian}) - it starts nothing, but
 * whatever hurts its owner has a horse to deal with.
 *
 * <h2>Owner, not rider</h2>
 * It defends whoever tamed it, whether or not they are on it. A horse standing
 * in a paddock that comes for the skeleton shooting at its owner across the
 * fence is the behaviour people will remember, and gating on rider instead would
 * throw that away for no gain.
 *
 * <h2>The trigger is the expensive part</h2>
 * {@code on_owner_hurt} does not start at the horse - it is the only way a gene
 * can react to something that happened to the <i>player</i>, and it fires on
 * every damage event to every player in the world. The translator establishes
 * cheaply that the hurt player owns a horse at all before it looks anything up.
 *
 * <p>It holds a radius like {@link GladiatorGene}, and for the same reason.
 */
public final class GuardianGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.guardian";
    public static final int PRIORITY = 171;

    public static final double RADIUS = 16.0;
    public static final int INTERVAL_TICKS = 10;
    public static final int MAX_TARGETS = 4;

    public GuardianGene() {
        super(KEY, PRIORITY, "Guardian",
                "Grd", "Guardian (Grd)",
                Dominance.RECESSIVE, Founders.CARRIERS_ONLY, 5.0,
                "The horse watches you get hurt with mild interest.",
                "Guardian",
                "Two copies, and the horse must be tamed. Whatever damages its owner within about sixteen blocks gets attacked - only that, and only after the fact. Unlike a gladiator it keeps working while you are riding, and unlike a gladiator it never picks the fight.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(new GeneAbility.Temper("aggressive", "all", RADIUS, INTERVAL_TICKS,
                MAX_TARGETS, true, new GeneAbility.Trigger.OnOwnerHurt(),
                new GeneAbility.Condition.Flag("tamed", false), 1));
    }
}
