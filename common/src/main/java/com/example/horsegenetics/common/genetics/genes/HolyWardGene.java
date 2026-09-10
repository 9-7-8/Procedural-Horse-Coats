package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.List;

/**
 * <b>Holy ward</b> ({@code horsegenetics.holy_ward}) - hostile mobs do not
 * appear near this horse. Not pushed away: never spawned.
 *
 * <h2>The cap is load-bearing</h2>
 * The radius is epigenetic and drifts with breeding, and it is capped at
 * {@link #MAX_RADIUS}. Uncapped, a well-bred horse becomes a permanent
 * peaceful-mode bubble over a whole base and every other defensive gene stops
 * mattering. Sixteen blocks covers a camp and not a base, which is the line the
 * gene is meant to sit on.
 *
 * <h2>The most useful gene in the family, and the one to get right</h2>
 * A mobile safe camp is a real reason to breed a specific animal, which is why
 * it is worth the awkward implementation: the ward runs on somebody else's event
 * rather than on the horse's own tick, and it must cancel <i>natural</i> spawns
 * only. Cancelling spawner, egg or breeding spawns would break mob farms - the
 * player's and other mods' - invisibly, because nothing would error.
 */
public final class HolyWardGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.holy_ward";
    public static final int PRIORITY = 165;

    public static final String RADIUS = "radius";

    /** The floor, and the ceiling that stops this becoming a world setting. */
    public static final double MIN_RADIUS = 8.0;
    public static final double MAX_RADIUS = 16.0;

    public HolyWardGene() {
        super(KEY, PRIORITY, "Holy ward",
                "Hly", "Warding (Hly)",
                Dominance.RECESSIVE, Founders.CARRIERS_ONLY, 5.0,
                "Monsters appear near the horse exactly as they appear anywhere else.",
                "Warding",
                "Two copies. Hostile mobs cannot spawn within a radius written on the allele copy - at least eight blocks and never more than sixteen. Anywhere the horse is standing is somewhere nothing appears, which makes it a camp that walks: a mining stop, an overnight halt, a half-built base with no lighting yet.");
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(RADIUS, MIN_RADIUS, MAX_RADIUS));
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(new GeneAbility.Ward(epi.get(RADIUS), GeneAbility.Condition.ALWAYS, 1));
    }
}
