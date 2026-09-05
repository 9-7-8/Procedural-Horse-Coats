package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * {@link AbilityContribution}'s <b>per-horse</b> twin: "this combination of my
 * alleles makes the horse do something, and <i>what it looks like</i> is written
 * on the allele copy rather than on the allele".
 *
 * <p>It is to {@link AbilityContribution} exactly what
 * {@link com.example.horsegenetics.common.trait.EpigeneticTraitContribution} is
 * to {@link com.example.horsegenetics.common.trait.TraitContribution}, and it
 * exists for the same reason: the plain interface is a pure function of the
 * genotype, which is right for every gene whose behaviour is fixed by its
 * alleles - two {@code Hlr/Hlr} horses heal identically, and should. The
 * particle locus is the case it cannot express. Forty alleles each naming one
 * particle would be forty fixed effects; what makes the locus worth breeding is
 * that the <i>colour</i>, the <i>body site</i> and the <i>density</i> vary
 * between horses carrying the same allele, and are inherited with the copy that
 * carries them.
 *
 * <h2>This does not weaken determinism</h2>
 * The {@link AlleleRandomness} handed in is not a fresh die roll: it is a
 * {@link com.example.horsegenetics.common.SeededRng} on the epigenetic seed
 * stored on an allele copy - rolled once for a founder, written on the record,
 * and inherited verbatim. The same horse produces the same effect every time it
 * is asked, on the server, after a reload, in a test; and a foal that inherits
 * the copy inherits the look, which is the whole point.
 *
 * <p>Ask with no epigenome and every draw comes from
 * {@link com.example.horsegenetics.common.MidpointRng}, so the answer describes
 * the genotype rather than a horse nobody owns.
 *
 * <h2>Draw order is the contract</h2>
 * A gene must draw the same values in the same order every time, whatever it
 * ends up using them for - the order <i>is</i> the meaning of each number, and
 * re-ordering them silently rewrites every horse in every save. Draw the values
 * you might need, then decide what to do with them.
 *
 * <p>A gene implements this <b>or</b> {@link AbilityContribution}, not both;
 * {@code HorseAbilities} checks for this one first.
 */
@FunctionalInterface
public interface EpigeneticAbilityContribution {

    /**
     * What this combination makes the horse do. Return {@link List#of()} for a
     * combination that does nothing.
     *
     * @param epigenetics this horse's randomness for this gene.
     *                    {@link AlleleRandomness#expressed()} where one locus
     *                    gives one result; {@link AlleleRandomness#copy(int)}
     *                    where both copies contribute at once, as they do
     *                    wherever two codominant alleles each grant an effect.
     */
    List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype, AlleleRandomness epigenetics);
}
