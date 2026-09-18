package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.trait.HealthContribution;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A foal is born with magic neither parent had.</b> One foal in a thousand
 * takes a magical allele out of nowhere; one in a million takes two.
 *
 * <h2>The rolls</h2>
 * {@value #CHANCE} that a foal mutates at all. If it does, a magical gene and a
 * non-wild allele of it are drawn, and that allele replaces one of the two the
 * foal inherited. Then the <i>same</i> chance again for the second copy, which
 * is what makes a two-copy mutation {@value #CHANCE} squared - one in a million
 * (owner, 2026-09-17).
 *
 * <p>Each success guarantees a <b>non-wild allele</b> and nothing more. The
 * second copy is drawn independently of the first, so a double mutation on a
 * gene with several alleles can land on two different ones - a pair that may do
 * nothing at all. That is deliberate: what is promised is novelty, not a working
 * combination.
 *
 * <h2>Foals only</h2>
 * This happens where mutations happen, in the making of a foal, and never to a
 * horse the world spawned (owner's call, 2026-09-17). That keeps
 * {@link com.example.horsegenetics.common.breed.MagicalVariant}'s rule intact -
 * a wild breed carries exactly its sheet and no stray magic - and makes a
 * mutation something a breeder finds rather than something lying around in a
 * herd. It applies to <i>every</i> way a foal is bred: a natural cover, a
 * golden carrot, the seed jar.
 *
 * <h2>What may be drawn</h2>
 * The same exclusions {@code MagicalVariant} uses, and for the same reasons:
 * <ul>
 *   <li>the four body-stat loci, which the breed's stat scores own;</li>
 *   <li>a {@link Gene#feralOnly() feral-only} curiosity;</li>
 *   <li>a sex-linked gene, whose copy count depends on the foal's sex;</li>
 *   <li>a gene that is a {@link HealthContribution}, so a mutation is never a
 *       new illness - a foal that arrives sick for no reason a player can trace
 *       is a bug report, not a surprise.</li>
 * </ul>
 * A gene the foal's breed sheet names is <b>not</b> excluded: a mutation
 * contradicting the breed is the whole idea of one.
 *
 * <h2>The mutated locus gets fresh epigenetics</h2>
 * The per-copy numbers at that locus are re-rolled rather than inherited,
 * because the allele they described is no longer there - a new allele has no
 * parent copy for its numbers to have come from. That is the same reasoning
 * {@code Genome.breedWith} already applies to a spliced gamete.
 */
public final class Mutation {

    /**
     * Per foal, and again for the second copy. Deliberately a flat rate rather
     * than anything the parents can influence: a mutation nobody can breed
     * toward is the one kind of novelty a closed gene pool cannot exhaust.
     */
    public static final double CHANCE = 0.001;

    private Mutation() {
    }

    /**
     * {@code foal}, possibly with one magical locus replaced. Consumes at least
     * one draw from {@code rng} either way.
     *
     * <p>Called at the end of the foal draw, after every allele is locked, so a
     * mutation cannot shift any other gene's inheritance.
     */
    public static Genome mutate(Genome foal, Rng rng) {
        if (rng.nextFloat() >= CHANCE) {
            return foal;
        }
        List<Gene> candidates = candidates();
        if (candidates.isEmpty()) {
            return foal;
        }
        Gene gene = candidates.get(rng.nextInt(candidates.size()));
        List<Allele> novel = novelAlleles(gene);
        if (novel.isEmpty()) {
            return foal;
        }

        Allele mutated = novel.get(rng.nextInt(novel.size()));
        AllelePair inherited = foal.genotype().pair(gene);
        // The second roll. Independently drawn, so two successes need not agree.
        Allele other = rng.nextFloat() < CHANCE
                ? novel.get(rng.nextInt(novel.size()))
                : inherited.first();

        Genotype genotype = foal.genotype().with(new AllelePair(mutated, other));
        // Only genes that store something have copies to re-roll; asking for the
        // rest throws. Most magical loci carry nothing at all.
        Epigenome epigenome = Epigenome.carries(gene)
                ? foal.epigenome().with(gene.key(), Epigenome.copiesFor(gene, rng.nextLong()))
                : foal.epigenome();
        return new Genome(genotype, epigenome);
    }

    /**
     * <b>Did this foal mutate?</b> True when it carries a non-wild allele, at a
     * locus a mutation may land on, that neither parent had to give it.
     *
     * <p>A detector rather than a flag, because nothing on a horse records how
     * it came by its genes and a foal is a foal however it was drawn. The
     * trade-off is one-sided and deliberate: it <b>under-reports</b> and never
     * over-reports. A mutation that happens to land on an allele a parent
     * already carried is indistinguishable from ordinary inheritance and is not
     * counted, while anything it does report genuinely came from nowhere, since
     * every other allele a foal owns came from one of the two parents.
     */
    public static boolean happened(Genotype foal, Genotype dam, Genotype sire) {
        for (Gene gene : candidates()) {
            AllelePair pair = foal.pair(gene);
            for (Allele allele : List.of(pair.first(), pair.second())) {
                if (!allele.equals(gene.defaultAllele())
                        && !dam.pair(gene).has(allele)
                        && !sire.pair(gene).has(allele)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Every magical gene a mutation may land on, in {@link Genes#magicalOrder()}. */
    public static List<Gene> candidates() {
        List<Gene> out = new ArrayList<>();
        for (Gene gene : Genes.magicalOrder()) {
            if (BreedFounder.BODY_STAT_KEYS.contains(gene.key())
                    || gene.feralOnly()
                    || gene.inheritance().sexLinked()
                    || gene instanceof HealthContribution) {
                continue;
            }
            out.add(gene);
        }
        return out;
    }

    /** Every allele of {@code gene} that is not its wild type. */
    public static List<Allele> novelAlleles(Gene gene) {
        List<Allele> out = new ArrayList<>();
        for (Allele allele : gene.alleles()) {
            if (!allele.equals(gene.defaultAllele())) {
                out.add(allele);
            }
        }
        return out;
    }
}
