package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>A capability a {@link Gene} may additionally implement</b>: "this
 * combination of my alleles does something to the horse's body that is not a
 * colour". Speed, max health, jump strength, size, and the
 * {@link Condition}s a horse expresses all arrive this way.
 *
 * <p>It is a separate interface rather than more methods on {@code Gene}
 * because most genes have nothing to say here - a coat gene would be
 * implementing an empty method - and because a gene that <i>only</i> does this
 * (every gene in {@code wiki/roadmap.html} §4.3 and §4.4) then consists of an
 * allele list, a combination table and one method.
 *
 * <h2>One interface, not four</h2>
 * The roadmap sketched four ({@code StatContribution},
 * {@code BodyContribution}, {@code ViabilityRule}, {@code ConditionRule}). They
 * collapsed into this one for two reasons. They all run in the same single walk
 * of the genotype, so four of them would be four sinks threaded through one
 * loop; and the genes that need any of them mostly need several at once -
 * chondrodysplastic dwarfism is a stat change <i>and</i> a size change
 * <i>and</i> a condition, and splitting that across three interfaces would
 * scatter one gene's answer over three methods that have to agree. Viability is
 * not an interface at all any more: it is derived from the severity of the
 * conditions reported, so a gene can never declare a horse lethal without
 * saying what killed it.
 *
 * <h2>Purity</h2>
 * The same contract the coat hooks keep: no RNG, no epigenetics, no entity.
 * The pair and the whole genotype in, a contribution out. Two horses with the
 * same genotype are the same horse.
 */
@FunctionalInterface
public interface TraitContribution {

    /**
     * Push this gene's contribution for {@code pair} into {@code out}.
     *
     * @param genotype the whole genotype, for the handful of cases that need
     *                 to read another locus. Complete by the time this runs.
     */
    void contribute(AllelePair pair, Genotype genotype, TraitBuilder out);
}
