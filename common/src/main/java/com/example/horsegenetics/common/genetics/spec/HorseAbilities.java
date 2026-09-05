package com.example.horsegenetics.common.genetics.spec;

import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.AlleleRandomness;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the {@link GeneAbility}s a horse's {@link Genotype} actually
 * expresses - the list the NeoForge translator walks every tick.
 *
 * <p>Pure {@code common/}: it decides <i>which</i> abilities are in play. It
 * does <b>not</b> evaluate {@link GeneAbility.Condition}s or
 * {@link GeneAbility.Trigger}s - those need a live horse and belong to the
 * translator.
 *
 * <h2>Both kinds of gene contribute</h2>
 * <ul>
 *   <li><b>Data-driven genes</b> carry an {@code effects} block on each
 *       expression; a horse gets the block belonging to the outcome its
 *       combination landed on, filtered by {@code minDose}.</li>
 *   <li><b>Hand-written genes</b> implement {@link AbilityContribution} and
 *       answer the same question directly. The magical utility genes - light,
 *       healer, verdant, milk - are all here.</li>
 * </ul>
 * Both produce the same records, so the translator has one code path and a
 * behaviour is never available to one kind of gene and not the other. (This
 * class was called {@code SpecAbilities} while only the first half existed.)
 *
 * <p>Effects hang off an <b>expression</b>, so this is simply "which outcome did
 * this horse's combination land on, and what does that outcome do" - a
 * homozygote and a heterozygote can carry entirely different effects because
 * they are different expressions, not because anything here compares doses.
 */
public final class HorseAbilities {

    private HorseAbilities() {}

    /** One expressed ability, tagged with the gene it came from (for logging / attribute-modifier ids). */
    public record Active(String geneKey, GeneAbility ability) {}

    /**
     * Every ability the {@code genotype} expresses, with <b>no epigenome</b> -
     * so an {@link EpigeneticAbilityContribution} answers with its midpoint. The
     * right call for "what does this genotype do", and for any consumer that
     * only reads the parts of an ability an epigenome cannot move (the renderer
     * asking which body parts glow, for one).
     */
    public static List<Active> activeFor(Genotype genotype) {
        return activeFor(genotype, null);
    }

    /**
     * Every ability this <b>horse</b> expresses. {@code epigenome} may be
     * {@code null}; see {@link #activeFor(Genotype)}.
     *
     * <p>It is passed rather than looked up because an ability's magnitude can
     * live on the allele copy, exactly as a trait's can - see
     * {@link EpigeneticAbilityContribution}. A caller that caches this list must
     * key the cache on the epigenome as well as the genotype, or two horses with
     * the same alleles will share one horse's colours.
     */
    public static List<Active> activeFor(Genotype genotype, Epigenome epigenome) {
        List<Active> out = new ArrayList<>();
        for (Gene gene : Genes.codeOrder()) {
            AllelePair pair = genotype.pair(gene);
            if (gene instanceof EpigeneticAbilityContribution contribution) {
                AlleleRandomness epi = AlleleRandomness.forGene(gene, genotype, epigenome);
                for (GeneAbility ability : contribution.abilitiesFor(pair, genotype, epi)) {
                    out.add(new Active(gene.key(), ability));
                }
                continue;
            }
            if (gene instanceof AbilityContribution contribution) {
                for (GeneAbility ability : contribution.abilitiesFor(pair, genotype)) {
                    out.add(new Active(gene.key(), ability));
                }
                continue;
            }
            if (!(gene instanceof SpecGene spec) || !spec.spec().hasAbilities()) {
                continue;
            }
            GeneSpec.ExpressionSpec expressed = spec.expressionSpecOf(pair);
            if (expressed == null || expressed.abilities().isEmpty()) {
                continue;
            }
            int dose = spec.dose(pair);
            for (GeneAbility ability : expressed.abilities()) {
                if (dose >= ability.minDose()) {
                    out.add(new Active(gene.key(), ability));
                }
            }
        }
        return List.copyOf(out);
    }

    /**
     * {@code true} if any registered gene could produce an ability at all - the
     * cheap check that lets the translator skip its per-tick work entirely.
     *
     * <p>It has been true since the magical utility genes were built in, so the
     * short-circuit no longer fires in practice; the per-horse ability list is
     * cached by genetic code on the translator side, which is where the cost
     * actually was.
     */
    public static boolean anyLoaded() {
        for (Gene gene : Genes.codeOrder()) {
            if (gene instanceof AbilityContribution || gene instanceof EpigeneticAbilityContribution) {
                return true;
            }
            if (gene instanceof SpecGene spec && spec.spec().hasAbilities()) {
                return true;
            }
        }
        return false;
    }
}
