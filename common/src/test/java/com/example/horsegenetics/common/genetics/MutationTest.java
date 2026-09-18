package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.BreedFounder;
import com.example.horsegenetics.common.trait.HealthContribution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Magic out of nowhere.</b> The rate, what may be drawn, and that the foal
 * draw actually rolls it - the last one because a mutation nothing calls is the
 * easiest kind of bug to ship, and at one in a thousand nobody would notice.
 */
class MutationTest {

    private static Genome plain(long seed) {
        return new Genome(Genotype.wildType(), Epigenome.fromSeed(seed));
    }

    @Test
    void aboutOneFoalInAThousandMutates() {
        Genome before = plain(1L);
        int n = 400_000;
        int mutated = 0;
        for (int i = 0; i < n; i++) {
            if (!Mutation.mutate(before, new SeededRng(i)).equals(before)) {
                mutated++;
            }
        }
        assertEquals(Mutation.CHANCE, mutated / (double) n, 0.0004,
                mutated + " mutations in " + n + " foals");
    }

    /** One locus moves, and it is always a magical one. */
    @Test
    void aMutationChangesExactlyOneMagicalLocus() {
        Genome before = plain(7L);
        int seen = 0;
        for (int i = 0; i < 400_000 && seen < 40; i++) {
            Genome after = Mutation.mutate(before, new SeededRng(i));
            if (after.equals(before)) {
                continue;
            }
            seen++;
            int changed = 0;
            for (Gene g : Genes.codeOrder()) {
                if (!after.genotype().pair(g).equals(before.genotype().pair(g))) {
                    changed++;
                    assertFalse(g.isNatural(), "a mutation landed on a natural gene: " + g.key());
                }
            }
            assertEquals(1, changed, "a mutation moved more than one locus");
        }
        assertTrue(seen > 0, "no mutation happened at all, so nothing was checked");
    }

    /**
     * The exclusions, which are the same ones a magical herd uses. The health
     * one is the load-bearing entry: a foal that arrives sick for no traceable
     * reason reads as a bug rather than as a surprise.
     */
    @Test
    void whatMayBeDrawnExcludesTheFourAwkwardKinds() {
        assertFalse(Mutation.candidates().isEmpty(), "nothing can mutate at all");
        for (Gene g : Mutation.candidates()) {
            assertFalse(g.isNatural(), g.key() + " is a natural gene");
            assertFalse(g.inheritance().sexLinked(), g.key() + " is sex-linked");
            assertFalse(g.feralOnly(), g.key() + " is feral-only");
            assertFalse(g instanceof HealthContribution, g.key() + " is a health contribution");
            assertFalse(BreedFounder.BODY_STAT_KEYS.contains(g.key()), g.key() + " is a body stat");
        }
    }

    @Test
    void everyDrawableAlleleIsNonWild() {
        for (Gene g : Mutation.candidates()) {
            assertFalse(Mutation.novelAlleles(g).isEmpty(), g.key() + " has nothing to mutate to");
            for (Allele a : Mutation.novelAlleles(g)) {
                assertNotEquals(g.defaultAllele(), a, g.key() + " offered its wild type");
            }
        }
    }

    /**
     * <b>The foal draw rolls it.</b> Two wild-type parents can only produce a
     * magical allele by mutating, so any magical locus that moves is one.
     */
    @Test
    void theFoalDrawActuallyRollsIt() {
        Genome dam = plain(1L);
        Genome sire = plain(2L);
        int mutated = 0;
        for (int i = 0; i < 30_000; i++) {
            Genome foal = dam.breedWith(sire, new SeededRng(i));
            for (Gene g : Genes.magicalOrder()) {
                if (!foal.genotype().pair(g).equals(dam.genotype().pair(g))) {
                    mutated++;
                    break;
                }
            }
        }
        assertTrue(mutated > 5,
                "about 30 of 30,000 foals should have mutated, got " + mutated);
    }
}
