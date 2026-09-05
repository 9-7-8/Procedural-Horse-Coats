package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>A capability a hand-written {@link Gene} may additionally implement</b>:
 * "this combination of my alleles makes the horse <i>do</i> something" - emit
 * light, heal what stands near it, spread moss where it walks, hand a player a
 * bucket of lava.
 *
 * <p>It is the third of the three things a gene can be, alongside the coat
 * ({@link Expression}'s paint functions) and the body
 * ({@link com.example.horsegenetics.common.trait.TraitContribution}), and like
 * the other two it is a separate interface because most genes have nothing to
 * say here.
 *
 * <h2>Why it reuses the data-driven vocabulary</h2>
 * A gene declares its behaviour as {@link GeneAbility} records - the same closed
 * set of verbs a JSON gene's {@code effects} block parses into. That was a
 * choice, and the alternative (a second vocabulary for built-ins) was worse in
 * every direction: the NeoForge translator would have had to be written twice,
 * a behaviour available to a Java gene would silently not be available to a gene
 * file, and {@code wiki/gene-effects.html} would have stopped being the single
 * description of what a gene can do. Sharing the vocabulary means a new verb
 * lands once and both kinds of gene get it.
 *
 * <h2>Doses</h2>
 * {@link GeneAbility#minDose()} exists for the data-driven path, where an author
 * has no expression language and needs <i>some</i> way to say "only if
 * homozygous". A hand-written gene has the whole of Java and answers the
 * question directly in {@link #abilitiesFor}, so it should return the list it
 * actually means and leave {@code minDose} at 1.
 *
 * <p>Purity: the same contract as everywhere else. The pair and the genotype in,
 * a description out. Nothing here touches the game - the returned records are
 * inert data until the NeoForge translator reads them.
 */
@FunctionalInterface
public interface AbilityContribution {

    /**
     * What this combination makes the horse do. Return {@link List#of()} for a
     * combination that does nothing - which is most of them.
     *
     * @param genotype the whole genotype, complete by the time this is called,
     *                 for the rare ability that depends on another locus.
     */
    List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype);
}
