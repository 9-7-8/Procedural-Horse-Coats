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
 * The {@link GeneEpigenetics} handed in is not a fresh die roll: it is the
 * literal values stored on an allele copy - rolled once for a founder, written
 * on the record, and inherited with the allele. The same horse produces the same
 * effect every time it is asked, on the server, after a reload, in a test; and a
 * foal that inherits the copy inherits the look, which is the whole point.
 *
 * <p>Ask with no epigenome and every value reports its schema's midpoint, so the
 * answer describes the genotype rather than a horse nobody owns.
 *
 * <h2>Read by name</h2>
 * Values are declared on {@code Gene.epiSchema()} and read by name, so a gene
 * reads whichever it needs in whatever order suits it. This used to be a fixed
 * draw order whose <i>position</i> was the meaning of each number - so
 * re-ordering, or inserting one while tuning, silently rewrote every horse in
 * every save. The particle locus is the gene that paid for that lesson.
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
     * @param epigenetics this horse's numbers for this gene.
     *                    {@link GeneEpigenetics#expressed()} where one locus
     *                    gives one result; {@link GeneEpigenetics#copy(int)}
     *                    where both copies contribute at once, as they do
     *                    wherever two codominant alleles each grant an effect.
     */
    List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype, GeneEpigenetics epigenetics);
}
