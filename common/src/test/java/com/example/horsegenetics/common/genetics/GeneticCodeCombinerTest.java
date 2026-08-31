package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneticCodeCombinerTest {

    // breedWith draws six booleans, in order: child E from parent1, child E from
    // parent2, child A from parent1, child A from parent2, child W from parent1,
    // child W from parent2. true = first allele of that locus.

    @Test
    void homozygousDominantCrossHomozygousRecessiveIsAlwaysHeterozygous() {
        // EEAA x eeaa -> EeAaww no matter how the coin falls (both parents ww)
        for (boolean[] draws : new boolean[][] {
                {true, true, true, true, true, true},
                {false, false, false, false, false, false},
                {true, false, true, false, true, false},
        }) {
            String child = GeneticCodeCombiner.combine("EEAA", "eeaa", new FakeRng().booleans(draws));
            assertEquals("EeAaww", child);
        }
    }

    @Test
    void picksTheSelectedAlleleFromEachParent() {
        // parent1 = EeAa (E,e / A,a), parent2 = eEaA -> canonical Ee / Aa (E,e / A,a); both ww
        // draws (E1=true, E2=false, A1=false, A2=true, W1/W2 irrelevant):
        //   childE1 = parent1.e1 = 'E', childE2 = parent2.e2 = 'e'  -> "Ee"
        //   childA1 = parent1.a2 = 'a', childA2 = parent2.a1 = 'A'  -> "Aa"
        String child = GeneticCodeCombiner.combine("EeAa", "eEaA",
                new FakeRng().booleans(true, false, false, true, true, true));
        assertEquals("EeAaww", child);
    }

    @Test
    void argumentOrderDoesNotMatterForAGivenDrawSequence() {
        String ab = GeneticCodeCombiner.combine("EEaa", "eeAA",
                new FakeRng().booleans(true, false, true, false, true, false));
        String ba = GeneticCodeCombiner.combine("eeAA", "EEaa",
                new FakeRng().booleans(false, true, false, true, false, true));
        assertEquals("EeAaww", ab);
        assertEquals(ab, ba);
    }

    @Test
    void whiteAlleleIsInheritedAndDominant() {
        // Ww x ww, all-first-allele draws: W from parent1 ('W'), w from parent2 -> white foal.
        String white = GeneticCodeCombiner.combine("EeAaWw", "EeAaww",
                new FakeRng().booleans(true, true, true, true, true, true));
        assertEquals("EEAAWw", white);
        assertEquals(CoatPhenotype.WHITE, Genotype.parse(white).phenotype());

        // Flip only the W-from-parent1 draw: now 'w' from parent1 too -> not white.
        String notWhite = GeneticCodeCombiner.combine("EeAaWw", "EeAaww",
                new FakeRng().booleans(true, true, true, true, false, true));
        assertEquals("EEAAww", notWhite);
        assertEquals(CoatPhenotype.BAY, Genotype.parse(notWhite).phenotype());
    }

    @Test
    void rejectsMalformedCodes() {
        assertThrows(IllegalArgumentException.class,
                () -> GeneticCodeCombiner.combine("nope", "eeaa", new FakeRng().booleans(true, true, true, true)));
        assertThrows(IllegalArgumentException.class,
                () -> GeneticCodeCombiner.combine("EeAa", "XyZw", new FakeRng().booleans(true, true, true, true)));
    }

    @Test
    void breedWithIsExposedOnGenotypeToo() {
        Genotype child = Genotype.parse("EEAA").breedWith(Genotype.parse("eeaa"),
                new FakeRng().booleans(true, false, true, false, true, false));
        assertEquals("EeAaww", child.toCode());
        assertEquals(CoatPhenotype.BAY, child.phenotype());
    }
}
