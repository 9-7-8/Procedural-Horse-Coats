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
 * answer and several genes have a claim on it. Two kinds, and they are not the
 * same kind of claim:
 * <ul>
 *   <li><b>pigment genes</b> - tiger eye, which changes what colour the iris
 *       <i>is</i>;</li>
 *   <li><b>white-spotting genes</b> - splash, dominant white, broad sabino,
 *       frame - which take the pigment out of the iris the same way they take it
 *       out of the coat, and produce the blue eye those patterns are known for.
 *       That is not a new gene and never should be: it is a property the white
 *       loci already have, and the alternative is a "blue eyes" locus that
 *       mysteriously only ever appears on white horses.</li>
 * </ul>
 * {@link EyeColor#rank()} orders them, and the ordering that matters is settled
 * there: <b>blue beats amber</b>, because a depigmented iris has nothing left
 * for a pigment gene to recolour.
 *
 * <h2>The contract</h2>
 * <ul>
 *   <li>Pure: a pair, the genotype and the horse's resolved white coverage in;
 *       a claim or {@link Optional#empty()} out.</li>
 *   <li><b>Deterministic.</b> The eye is written into the baked coat texture, so
 *       a claim that varied per horse would fork the texture cache for four
 *       texels. Every implementor so far answers from alleles alone.</li>
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
     * @param whiteCoverage fraction of the finished coat left unpigmented, {@code [0,1]}
     */
    Optional<EyeColor> eyeColor(AllelePair pair, Genotype genotype, double whiteCoverage);
}
