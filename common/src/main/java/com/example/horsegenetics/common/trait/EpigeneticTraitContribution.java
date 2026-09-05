package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * {@link TraitContribution}'s <b>per-horse</b> twin: "this combination of my
 * alleles does something to the body, and <i>how much</i> is written on the
 * allele copy rather than on the allele".
 *
 * <p>The plain interface is a pure function of the genotype, and for every
 * natural gene that is exactly right - {@code p/p} is one particular pony and
 * always the same one. A magical size gene is the case it cannot express: the
 * point of it is that two horses can both be "big" and one of them be twice the
 * other, which needs a number that varies between horses carrying identical
 * alleles.
 *
 * <h2>This does not weaken determinism</h2>
 * The {@link Rng} handed in is <b>not</b> a fresh die roll. It is a
 * {@link com.example.horsegenetics.common.SeededRng} on the epigenetic seed of
 * the allele copy that expresses at this gene - the same seed the coat pipeline
 * draws its per-horse variation from ({@code CoatBuildContext.epigeneticsFor}).
 * That seed is rolled once for a founder, stored on the record, and inherited
 * verbatim with the allele, so:
 * <ul>
 *   <li>the same horse resolves to the same body every time it is looked at,
 *       on the server, after a reload, in a test;</li>
 *   <li>a foal that inherits the copy inherits the number, which is what makes
 *       an enormous horse worth breeding from;</li>
 *   <li>nothing is stored on the record that could go stale - the trait is
 *       still resolved on demand, from the genotype and the epigenome that were
 *       already there.</li>
 * </ul>
 *
 * <p>Ask for a genotype's body with no epigenome
 * ({@link HorseTraits#resolve(Genotype)}) and the draws come from
 * {@link com.example.horsegenetics.common.MidpointRng} instead, so the answer is
 * the midpoint of what the genotype can produce rather than a horse nobody owns.
 *
 * <h2>Both copies, when the gene is codominant</h2>
 * {@link AlleleRandomness} offers the expressing copy <i>and</i> either copy by
 * slot. A gene whose two alleles each contribute - the magical size locus, where
 * the percentages add - needs the second form; asking for the expressed copy
 * would count one allele twice and the other not at all.
 *
 * <h2>Order</h2>
 * The same guarantee as {@link TraitBuilder}: additions before multipliers, so
 * where a gene sits in {@link com.example.horsegenetics.common.genetics.Genes#codeOrder()}
 * still buys it nothing here.
 */
@FunctionalInterface
public interface EpigeneticTraitContribution {

    /**
     * Push this gene's contribution for {@code pair} into {@code out}.
     *
     * @param epigenetics this horse's randomness for this {@link Gene}:
     *                    {@link AlleleRandomness#expressed()} for a gene with one
     *                    result per locus, {@link AlleleRandomness#copy(int)} for
     *                    a codominant one where both copies contribute at once.
     *                    Draw in a fixed order; the order <i>is</i> the meaning
     *                    of each number.
     */
    void contribute(AllelePair pair, Genotype genotype, AlleleRandomness epigenetics, TraitBuilder out);
}
