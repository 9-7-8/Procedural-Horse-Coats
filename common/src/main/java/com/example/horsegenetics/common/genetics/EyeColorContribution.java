package com.example.horsegenetics.common.genetics;

import java.util.Optional;

/**
 * <b>A capability a {@link Gene} may additionally implement</b>: "this
 * combination of my alleles says what colour the horse's eyes are".
 *
 * <p>It is the <b>sixth</b> thing a gene can be, beside the coat
 * ({@link Expression}), the body
 * ({@link com.example.horsegenetics.common.trait.TraitContribution}), game
 * behaviour ({@link AbilityContribution}), the colour lookup
 * ({@link LutContribution}) and the cutie mark
 * ({@link CutieMarkContribution}).
 *
 * <h2>Why the iris is a channel and not a gene</h2>
 * A horse has one iris colour, so "what colour are its eyes" has exactly one
 * answer and several genes have a claim on it. Three kinds, which are the three
 * routes a real horse's eye changes colour by, and {@link EyeColor#rank()}
 * orders them:
 * <ul>
 *   <li><b>whole-body dilutions</b> - cream, pearl, champagne - which dilute
 *       every pigment the horse has, iris included;</li>
 *   <li><b>iris-specific pigment genes</b> - tiger eye, which changes what
 *       colour the iris <i>is</i> and touches nothing else;</li>
 *   <li><b>white-spotting genes</b> - splash, dominant white, broad sabino,
 *       frame - which take the pigment out of the iris the same way they take it
 *       out of the coat, and produce the blue eye those patterns are known for.
 *       That is not a new gene and never should be: it is a property the white
 *       loci already have, and the alternative is a "blue eyes" locus that
 *       mysteriously only ever appears on white horses.</li>
 * </ul>
 * The ordering that matters is settled in {@link EyeColor}: <b>blue beats
 * amber</b>, because a depigmented iris has nothing left for a pigment gene to
 * recolour.
 *
 * <h2>The contract</h2>
 * <ul>
 *   <li>Pure: a pair, the genotype, the horse's epigenome and its resolved white
 *       coverage in; a claim or {@link Optional#empty()} out.</li>
 *   <li>{@code epigenome} may be {@code null} - the question was asked about a
 *       genotype rather than about a horse. {@link AlleleRandomness#forGene}
 *       handles that by handing back midpoints, which is the honest answer.</li>
 *   <li>A claim <b>may vary per horse</b> - champagne's iris runs from amber
 *       through hazel to olive - but only if every number comes from
 *       {@link AlleleRandomness}, so it is inherited with the allele copy, and
 *       only if the outcome declares itself
 *       {@link Expression.Builder#varies()}. The eye is baked into the coat
 *       texture, so an undeclared variation would let two visibly different
 *       horses share one cached texture.</li>
 *   <li>{@code whiteCoverage} is the fraction of the horse's mapped texels that
 *       the white loci have left with <b>no pigment at all</b>, measured on the
 *       finished coat. It is passed rather than recomputed because it is the
 *       honest signal for "is this horse white enough to have blue eyes" - a
 *       horse that is broadly white from <i>two</i> mild loci stacking has the
 *       same claim as one that is white from a single bold allele, and no
 *       per-locus test can see that.</li>
 *   <li>A gene with nothing to say returns empty. Most do.</li>
 * </ul>
 *
 * <h2>What this channel does not answer</h2>
 * Two things, both of which are asymmetric and so cannot be one colour:
 * <ul>
 *   <li>{@link EyeSpread} - how much of each iris a <i>depigmenting</i> claim
 *       actually reached. One blue eye and the blue wedge come from there, not
 *       from a second claim here.</li>
 *   <li>{@link EyePatchContribution} - a gene painting part of an iris its own
 *       colour, over the top of whatever won here.</li>
 * </ul>
 *
 * <p>The composer collects the claims in {@link Genes#codeOrder()} during the
 * overlay phase and applies the winner once - after
 * {@link com.example.horsegenetics.common.coat.pattern.CoatRegions#redrawEyes}
 * has put the template's eyes back, which is the only point in the bake where
 * an eye survives being written.
 */
public interface EyeColorContribution {

    /**
     * This gene's claim on the iris, or empty.
     *
     * @param pair          this gene's combination on the horse
     * @param genotype      the whole genotype, for a genotype-context read
     * @param epigenome     the horse's epigenetics, or {@code null} for a
     *                      genotype-only question
     * @param whiteCoverage fraction of the finished coat left unpigmented, {@code [0,1]}
     */
    Optional<EyeColor> eyeColor(AllelePair pair, Genotype genotype, Epigenome epigenome,
                                double whiteCoverage);
}
