package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;

import java.util.List;

/**
 * <b>Dryad</b> ({@code horsegenetics.dryad}) - about once a day, a sapling
 * appears where the horse has been.
 *
 * <h2>Saplings, not bone meal</h2>
 * An earlier specification had it bone-mealing the surrounding area. That makes
 * a horse which auto-farms every crop you own, which is an economy lever nobody
 * asked for and very hard to walk back once players have it. It plants saplings
 * and nothing else, and the decision is recorded here so it is not quietly
 * "improved" later.
 *
 * <h2>A gene you notice a week later</h2>
 * Almost everything in the mod expresses immediately or not at all. This one
 * expresses across sessions: you stable a horse, and later there are trees
 * there. The daily cap is what makes it both cheap and slow, and the epigenetic
 * interval is bounded so breeding can hurry it without turning it into a
 * per-tick effect.
 */
public final class DryadGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.dryad";
    public static final int PRIORITY = 161;

    /** The one value a copy carries: how many ticks between plantings. */
    public static final String INTERVAL = "interval";

    /** A day is 24000 ticks. The range is bounded so breeding can hurry it, not transform it. */
    public static final double MIN_INTERVAL = 9000;
    public static final double MAX_INTERVAL = 32000;

    /** Blocks. Small - a sapling should appear where the horse was, not across the field. */
    public static final double RADIUS = 4.0;

    public DryadGene() {
        super(KEY, PRIORITY, "Dryad",
                "Dry", "Dryad (Dry)",
                Dominance.RECESSIVE, Founders.EXPRESSING, 1.0,
                "The ground the horse walks over stays as it was.",
                "Dryad",
                "Two copies. Roughly once a day the horse plants a sapling somewhere near where it is standing. Left in one place long enough it grows a small wood around itself, one tree at a time and entirely without hurrying. How often is written on the allele copy.");
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(INTERVAL, MIN_INTERVAL, MAX_INTERVAL));
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        int interval = (int) Math.round(epi.get(INTERVAL));
        return List.of(new GeneAbility.Spread("sapling", RADIUS, 1.0, interval, GeneAbility.Condition.ALWAYS, 1));
    }
}
