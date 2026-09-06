package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.CutieMarkGene;

/**
 * <b>A capability a {@link Gene} may additionally implement</b>: "this
 * combination of my alleles changes what the horse's cutie mark looks like".
 *
 * <p>It is the <b>fifth</b> thing a gene can be, alongside the coat
 * ({@link Expression}), the body
 * ({@link com.example.horsegenetics.common.trait.TraitContribution}), game
 * behaviour ({@link AbilityContribution}) and the colour lookup
 * ({@link LutContribution}) - and, like {@code LutContribution}, it exists
 * because one locus owns a channel and every other gene has to reach it through
 * a hook rather than by owning a second copy of it.
 *
 * <h2>Why the cutie mark can afford this and the coat cannot</h2>
 * The emblem is the <b>last thing drawn on a horse</b>. It sits on top of the
 * finished coat, on top of every white pattern, on top of the emissive layer -
 * nothing composes over it and nothing reads it back. So a gene may change it
 * freely without any of the ordering arguments that make cross-locus reads
 * discouraged elsewhere (see {@code wiki/roadmap.html#settled}): there is no
 * accumulator to corrupt, no later painter to surprise, and no texture-cache
 * key to invalidate, because the mark is not in the baked coat at all.
 *
 * <p>That is the same reason {@link CutieMarkGene} is allowed to be the one
 * locus that owns the channel: a horse has one mark, so "which gene decides the
 * emblem" has exactly one answer, and everything else is a <b>modifier</b>.
 *
 * <h2>The contract</h2>
 * <ul>
 *   <li>{@link CutieMarkGene#markFor} draws the base mark off the cutie-mark
 *       allele's own seed, then folds every implementor over it in
 *       {@link Genes#codeOrder()} order. <b>Order is stable and priority-driven</b>,
 *       so two modifiers that both set the same field compose predictably.</li>
 *   <li>An implementor is called for <b>every</b> combination it can have,
 *       including its own wild type, and decides for itself whether that
 *       combination has anything to say. The fold does not filter on
 *       {@code expressionIn(...).wildType()}, because most of the magical genes
 *       that would want to reach the mark - particle, milk, the body-stat loci -
 *       paint nothing and therefore declare every outcome a wild type, so that
 *       filter would exclude precisely the genes this hook exists for.</li>
 *   <li>The hook is <b>pure</b>: a mark in, a mark out. Draw any randomness off
 *       {@code AlleleRandomness.forGene(this, genotype, epigenome)} so the
 *       result stays deterministic and heritable, like every other per-horse
 *       number in the mod.</li>
 *   <li>Returning the mark unchanged is always legal, and is what a combination
 *       with nothing to say should do.</li>
 *   <li>A horse that is <b>not</b> {@code Cutmrk/Cutmrk} has no mark, so no
 *       implementor is called at all. A modifier can make an existing mark
 *       stranger; it can never grant one.</li>
 * </ul>
 *
 * <h2>Adding a field to {@link CutieMarkGene.Mark}</h2>
 * The record's fields are exactly what the client render layer knows how to
 * honour, and that is a rule rather than a coincidence - a field nothing draws
 * is a promise the game does not keep. A new one lands in two places in the
 * same change: the record here, and {@code CutieMarkLayer}.
 */
public interface CutieMarkContribution {

    /**
     * This gene's change to the mark, or {@code mark} unchanged.
     *
     * @param pair      this gene's combination on the horse
     * @param genotype  the whole genotype, for a genotype-context read
     * @param epigenome the horse's epigenome, for {@code AlleleRandomness}
     * @param mark      the mark as the genes before this one left it
     */
    CutieMarkGene.Mark modifyCutieMark(AllelePair pair, Genotype genotype, Epigenome epigenome,
                                       CutieMarkGene.Mark mark);
}
