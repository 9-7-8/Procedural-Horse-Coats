package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Hot blooded</b> ({@code horsegenetics.hot_blooded}) - snow will not lie
 * near it and ice does not last.
 *
 * <h2>The rate is the safety</h2>
 * Melting ice makes flowing water, which is a well-known tick cost, and in a
 * snowy biome the snow re-forms and is melted again - a melt/refreeze loop
 * running for as long as the chunk is loaded. So the beat is deliberately slow
 * and the radius small. This is the most expensive {@code spread} gene in the
 * mod and the numbers are the reason it is affordable.
 *
 * <p>A hot-blooded horse on a frozen lake melts the ice it is standing on and
 * is then in the water. That is funny rather than broken, and it is the reason
 * the gene is a liability in exactly one biome.
 */
public final class HotBloodedGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.hot_blooded";
    public static final int PRIORITY = 159;

    /** Blocks. Small: this is the most expensive spread gene and the radius is what pays for it. */
    public static final double RADIUS = 2.0;

    /** Ticks between beats. Slow on purpose - see the class note on the melt/refreeze loop. */
    public static final int INTERVAL_TICKS = 80;

    /** Odds of converting on any given beat. */
    public static final double CHANCE = 0.5;

    public HotBloodedGene() {
        super(KEY, PRIORITY, "Hot blooded",
                "Hot", "Hot-blooded (Hot)",
                Dominance.RECESSIVE, Founders.EXPRESSING, 1.2,
                "Snow and ice are entirely unbothered by the horse standing on them.",
                "Hot-blooded",
                "Two copies. Snow and ice within a couple of blocks melt away, slowly and continuously, leaving a thawed circle wherever the horse stands. It does not make the horse immune to anything cold - it only removes the cold thing.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(new GeneAbility.Spread("melt", RADIUS, CHANCE, INTERVAL_TICKS, GeneAbility.Condition.ALWAYS, 1));
    }
}
