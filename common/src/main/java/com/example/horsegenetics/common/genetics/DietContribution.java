package com.example.horsegenetics.common.genetics;

import java.util.Optional;

/**
 * <b>A capability a {@link Gene} may additionally implement</b>: "this
 * combination of my alleles says what the horse will eat".
 *
 * <p>Another of the things a gene can be beside the coat ({@link Expression}),
 * the body ({@link com.example.horsegenetics.common.trait.TraitContribution}),
 * game behaviour ({@link AbilityContribution}), the colour lookup
 * ({@link LutContribution}), the cutie mark ({@link CutieMarkContribution}) and
 * the iris ({@link EyeColorContribution}). Like those, it is a separate
 * interface because it is the rare case.
 *
 * <h2>The last claimant wins</h2>
 * A horse has one diet, so several genes may have a claim and exactly one
 * answer comes out. {@link HorseDiet#resolve} walks {@link Genes#codeOrder()}
 * and keeps the <b>last</b> claim, which is the opposite of the eye channel's
 * ranked contest and is the point: a diet is <i>overridable</i>. The
 * {@link com.example.horsegenetics.common.genetics.genes.DietGene} locus sits
 * deliberately early in the order so that any gene which needs to say "and this
 * horse cannot be fed at all" simply has to sort after it, with no special case
 * anywhere and nothing to register.
 *
 * <h2>The contract</h2>
 * <ul>
 *   <li>Pure: a pair, the whole genotype, and the horse's per-copy randomness
 *       in; a claim or {@link Optional#empty()} out.</li>
 *   <li>Every number a claim varies by comes from {@link GeneEpigenetics}, so
 *       it is inherited with the allele copy and a horse asks for the same
 *       metal for its whole life. {@code random} may hand back midpoints - the
 *       question was asked about a genotype rather than about a horse - and a
 *       claim has to stay sane when it does.</li>
 *   <li>A gene with nothing to say returns empty. All but two do.</li>
 * </ul>
 */
public interface DietContribution {

    /**
     * What this gene says the horse eats, or {@link Optional#empty()} if it has
     * nothing to say about it.
     *
     * @param variantOf must be answered for a {@link Diet} with
     *                  {@link Diet#variants()} above zero - the claim carries
     *                  the chosen index itself, in {@link HorseDiet#variant()}
     */
    Optional<HorseDiet> diet(AllelePair pair, Genotype genotype, GeneEpigenetics variantOf);
}
