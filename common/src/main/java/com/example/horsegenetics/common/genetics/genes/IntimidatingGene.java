package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.List;

/**
 * <b>Intimidating</b> ({@code horsegenetics.intimidating}) - nothing that is not
 * a horse wants to be near it. Passive animals and hostile mobs alike keep
 * outside a radius written on the allele copy.
 *
 * <h2>It keeps its cost</h2>
 * The first specification repelled passive animals only, which made it a
 * drawback with no upside - a gene whose entire effect was emptying your farm.
 * Extending it to hostiles gives it a reason to exist. The passive half is
 * <b>kept</b> rather than dropped, because a magical locus with a real everyday
 * cost is rarer in this mod than one without, and "you cannot pasture this horse
 * with your animals" is a cost a player meets on the first day.
 *
 * <h2>Not the same as a ward</h2>
 * {@link HolyWardGene} stops hostiles <i>spawning</i>; this pushes out the ones
 * already there. They are complementary rather than redundant, and a horse with
 * both is genuinely safe ground.
 */
public final class IntimidatingGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.intimidating";
    public static final int PRIORITY = 162;

    /** The one value a copy carries. */
    public static final String RADIUS = "radius";
    public static final double MIN_RADIUS = 8.0;
    public static final double MAX_RADIUS = 14.0;

    /** Ticks between beats, and the cap every radius effect in the format must state. */
    public static final int INTERVAL_TICKS = 20;
    public static final int MAX_TARGETS = 12;

    public IntimidatingGene() {
        super(KEY, PRIORITY, "Intimidating",
                "Itm", "Intimidating (Itm)",
                Dominance.RECESSIVE, Founders.EXPRESSING, 1.0,
                "Other creatures are no more wary of the horse than of any other horse.",
                "Intimidating",
                "Two copies. Everything that is not a horse - your cows and sheep as readily as any monster - is pushed out of a radius written on the allele copy, at least eight blocks. It is the quietest place in the world to stand, and an intimidating horse cannot be kept in a farm without emptying it.");
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(RADIUS, MIN_RADIUS, MAX_RADIUS));
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        return List.of(new GeneAbility.MobAura("repel", "non_horse", "", epi.get(RADIUS),
                INTERVAL_TICKS, MAX_TARGETS, GeneAbility.Condition.ALWAYS, 1));
    }
}
