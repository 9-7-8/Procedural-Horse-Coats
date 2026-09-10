package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Spontaneous breeding</b> ({@code horsegenetics.spontaneous_breeding}) - two
 * of these will breed without being asked, and without being fed anything.
 *
 * <h2>It grants no effect at all</h2>
 * Every verb in the effect vocabulary is something the horse <i>does</i> on a
 * tick. This is something the breeding system <b>asks about</b>, the way
 * {@code DietContribution} is asked about food - so this class contributes no
 * {@link GeneAbility} and exists to answer {@link #expresses}. Building it as an
 * effect would put breeding logic in the ability translator, where nothing else
 * about breeding lives.
 *
 * <h2>It takes two</h2>
 * One expressing horse that bred with anything would spread itself through a
 * herd in a few generations and stop being rare. Requiring both parents to
 * express makes the trait self-limiting genetically as well as mechanically: the
 * population can only grow where somebody has already gathered two.
 *
 * <h2>A gene the testing layer exists for</h2>
 * Invisible in the wild, invisible in a carrier, and only observable once you
 * have already succeeded - which makes it close to unfindable by looking. The
 * gene database is the intended route, and this locus is one of the clearest
 * arguments for its existing at all.
 */
public final class SpontaneousBreedingGene extends AbstractAbilityGene {

    public static final String KEY = "horsegenetics.spontaneous_breeding";
    public static final int PRIORITY = 173;

    public SpontaneousBreedingGene() {
        super(KEY, PRIORITY, "Spontaneous breeding",
                "Spb", "Spontaneous (Spb)",
                Dominance.RECESSIVE, Founders.CARRIERS_ONLY, 9.0,
                "The horse breeds when you feed it, like any other.",
                "Spontaneous",
                "Two copies - and it does nothing at all unless there is another horse nearby that also has two. A pair of them will breed on their own, on a long cycle, with no golden carrots and no involvement from you. Wild horses carry this and never show it, so the first pair in a world is one somebody bred.");
    }

    @Override
    protected List<GeneAbility> abilitiesWhenExpressed(EpiValues epi) {
        // No effect verb: the breeding system asks expresses() directly.
        return List.of();
    }
}
