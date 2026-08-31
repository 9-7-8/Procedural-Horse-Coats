package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneticCodeCombinerTest {

    // combine() -> Genotype.breedWith: 2 booleans per gene, genes in code order
    // (extension, agouti, white, test, champagne). true = first allele of that
    // parent's pair.

    private static final boolean[] ALL_FIRST = {
            true, true, true, true, true, true, true, true, true, true};

    @Test
    void homozygousDominantCrossHomozygousRecessiveIsHeterozygous() {
        for (boolean[] draws : new boolean[][]{
                ALL_FIRST,
                {false, false, false, false, false, false, false, false, false, false},
                {true, false, true, false, true, false, true, false, true, false},
        }) {
            assertEquals("EeAawwttcc",
                    GeneticCodeCombiner.combine("EEAA", "eeaa", new FakeRng().booleans(draws)));
        }
    }

    @Test
    void picksTheSelectedAlleleFromEachParent() {
        // mom EeAa, dad eEaA(->Ee/Aa); draws E1=first('E'), E2=second('e'), A1=second('a'), A2=first('A')
        String child = GeneticCodeCombiner.combine("EeAa", "eEaA",
                new FakeRng().booleans(true, false, false, true, true, true, true, true, true, true));
        assertEquals("EeAawwttcc", child);
    }

    @Test
    void whiteIsInheritedAndDominant() {
        String white = GeneticCodeCombiner.combine("EeAaWw", "EeAaww", new FakeRng().booleans(ALL_FIRST));
        assertEquals("EEAAWwttcc", white);
        assertEquals(CoatPhenotype.WHITE, Genotype.parse(white).phenotype());

        String notWhite = GeneticCodeCombiner.combine("EeAaWw", "EeAaww",
                new FakeRng().booleans(true, true, true, true, false, true, true, true, true, true));
        assertEquals("EEAAwwttcc", notWhite);
    }

    @Test
    void testAndChampagneAreInherited() {
        String child = GeneticCodeCombiner.combine("EeAawwTtCc", "EeAawwttcc",
                new FakeRng().booleans(ALL_FIRST));
        assertEquals("EEAAwwTtCc", child);
        assertTrue(Genotype.parse(child).hasTest());
        assertTrue(Genotype.parse(child).isChampagne());
    }

    @Test
    void rejectsMalformedCodes() {
        assertThrows(IllegalArgumentException.class,
                () -> GeneticCodeCombiner.combine("nope", "eeaa", new FakeRng()));
        assertThrows(IllegalArgumentException.class,
                () -> GeneticCodeCombiner.combine("EeAa", "XyZw", new FakeRng()));
    }
}
