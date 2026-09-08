package com.example.horsegenetics.common.genetics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Whether a gene splice carrot's allele reached the foal - the thing that turns
 * a foal's breed into <b>Spliced (its breed)</b>.
 *
 * <p>The rule under test is "the foal carries an allele <b>neither parent</b>
 * had", and the case worth pinning is the last one: a mare fed a splice for a
 * gene the stallion already carries has not added anything to the foal, and must
 * not mark it. That is the difference between "a carrot was used" and "the
 * carrot did something", and only the second is a fact about the horse.
 */
class SpliceOutcomeTest {

    private static final Gene KIT = Genes.KIT;

    private static Genotype with(Allele a, Allele b) {
        return Genotype.wildType().with(new AllelePair(a, b));
    }

    /** The variant this test splices in - the first allele that is not the wild type. */
    private static Allele variant() {
        for (Allele a : KIT.alleles()) {
            if (!a.equals(KIT.defaultAllele())) {
                return a;
            }
        }
        throw new IllegalStateException("KIT has no variant allele to splice");
    }

    private static GameteBias splicing(Allele variant) {
        return GameteBias.substituting(Map.of(KIT.key(),
                new AllelePair(variant, KIT.defaultAllele())));
    }

    @Test
    void aFoalCarryingAnAlleleNeitherParentHadIsSpliced() {
        Allele v = variant();
        Genotype wild = with(KIT.defaultAllele(), KIT.defaultAllele());
        Genotype foal = with(v, KIT.defaultAllele());
        assertTrue(SpliceOutcome.spliceReached(foal, wild, wild, splicing(v), GameteBias.NONE));
    }

    @Test
    void aFoalThatDrewTheParentsOwnCopyIsNotSpliced() {
        Allele v = variant();
        Genotype wild = with(KIT.defaultAllele(), KIT.defaultAllele());
        assertFalse(SpliceOutcome.spliceReached(wild, wild, wild, splicing(v), GameteBias.NONE));
    }

    /**
     * The undecidable case, answered "no" on purpose: the stallion already
     * carries the allele, so the foal was going to have it whatever the mare's
     * gamete did. Nothing was added, so nothing is claimed.
     */
    @Test
    void aSpliceThatAddsNothingTheOtherParentLacksDoesNotMark() {
        Allele v = variant();
        Genotype wild = with(KIT.defaultAllele(), KIT.defaultAllele());
        Genotype sire = with(v, v);
        Genotype foal = with(v, KIT.defaultAllele());
        assertFalse(SpliceOutcome.spliceReached(foal, wild, sire, splicing(v), GameteBias.NONE));
    }

    @Test
    void anUnfedPairingIsNeverSpliced() {
        Genotype wild = with(KIT.defaultAllele(), KIT.defaultAllele());
        assertFalse(SpliceOutcome.spliceReached(wild, wild, wild, GameteBias.NONE, GameteBias.NONE));
    }
}
