package com.example.horsegenetics.common.genetics;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Did a gene splice carrot's allele actually <b>reach the foal</b>?
 *
 * <p>A splice carrot does not rewrite the parent - it swaps the pair that
 * parent's gamete is drawn from, once ({@link GameteBias#substitutePairs}). So
 * feeding one is a gamble: the foal draws one of the two copies of the
 * substitute pair, and often that is a copy the parent already had. The carrot
 * is spent either way.
 *
 * <p>This is the test for the times it worked, and it is deliberately phrased as
 * a question about the <b>foal</b> rather than about the carrot:
 *
 * <blockquote>at a locus a carrot substituted, does the foal carry an allele
 * that <b>neither parent</b> had?</blockquote>
 *
 * <p>Neither, not "the fed one", and that is not a shortcut. Consider a mare fed
 * a Known Gene Splice for {@code G} whose stallion is already {@code G/G}: the
 * foal is going to carry {@code G} no matter which copy the mare passed on, so
 * no outside observer - and no record - can say whether the carrot did anything.
 * The rule above answers "no", which is the useful reading as well as the
 * decidable one: the splice is marked exactly when the foal has something it
 * could not otherwise have had.
 *
 * <p>It handles every carrot without a case each. A Known Gene Splice onto an
 * {@code n/n} parent substitutes {@code n/G} and marks the foal exactly when it
 * drew {@code G}; the same carrot onto a parent already {@code G/G} adds nothing
 * and correctly never marks; an Unknown Gene Splice, whose substitute is
 * whatever its table rolled, needs no separate handling at all.
 *
 * <p>What the answer is <i>for</i> is the foal's breed label: a spliced foal is
 * <b>Spliced (its breed)</b> for good, and the mark passes down its line - see
 * {@code BreedLineage}. Nothing about the horse's body or genotype depends on
 * it; it is a claim about how the animal came to be, which is the same sort of
 * claim "purebred" is.
 */
public final class SpliceOutcome {

    private SpliceOutcome() {
    }

    /**
     * @param foal     the foal's genotype
     * @param dam      the dam's real genotype - what she actually carried, not
     *                 the pair a carrot had her breed from
     * @param sire     the sire's real genotype, likewise
     * @param damBias  the carrot bias applied to the dam's gamete, or
     *                 {@link GameteBias#NONE}
     * @param sireBias the same for the sire
     */
    public static boolean spliceReached(Genotype foal, Genotype dam, Genotype sire,
                                        GameteBias damBias, GameteBias sireBias) {
        Set<String> loci = new LinkedHashSet<>(damBias.substitutePairs().keySet());
        loci.addAll(sireBias.substitutePairs().keySet());
        for (String key : loci) {
            Gene gene = Genes.byKeyOrNull(key);
            if (gene == null) {
                continue;   // a gene uninstalled since; nothing to claim either way
            }
            AllelePair got = foal.pair(gene);
            if (isNew(got.first(), dam.pair(gene), sire.pair(gene))
                    || isNew(got.second(), dam.pair(gene), sire.pair(gene))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNew(Allele allele, AllelePair dam, AllelePair sire) {
        return !dam.has(allele) && !sire.has(allele);
    }
}
