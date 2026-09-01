package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneticCodeCombinerTest {

    private static FakeRng allFirst() {
        boolean[] draws = new boolean[Genes.codeOrder().size() * 2];
        Arrays.fill(draws, true);
        return new FakeRng().booleans(draws);
    }

    @Test
    void homozygousDominantCrossHomozygousRecessiveIsHeterozygous() {
        String dad = "E/E-A/A-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N";
        String mom = "e/e-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N";
        String child = GeneticCodeCombiner.combine(dad, mom, allFirst());
        assertEquals("E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N", child);
        assertEquals(CoatPhenotype.BAY, Genotype.parse(child).phenotype());
    }

    @Test
    void everyGeneSegregates() {
        String a = "E/e-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/prl";
        String child = GeneticCodeCombiner.combine(a, Genotype.wildType().toCode(), allFirst());
        assertEquals("E/E-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/N", child);
        assertTrue(Genotype.parse(child).hasTest());
        assertTrue(Genotype.parse(child).isSplash());
        assertTrue(Genotype.parse(child).isGrey());
        assertTrue(Genotype.parse(child).isChampagne());
    }

    @Test
    void rejectsMalformedCodes() {
        assertThrows(IllegalArgumentException.class,
                () -> GeneticCodeCombiner.combine("nope", Genotype.wildType().toCode(), new FakeRng()));
    }
}
