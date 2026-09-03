package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.testutil.FakeRng;
import com.example.horsegenetics.common.testutil.LegacyCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneticCodeCombinerTest {

    /** Wild-type segments for the visual-pattern genes added after the first 11. */
    private static final String T = "-d/d-z/z-mu/mu-rn/rn-to/to-ov/ov-sb1/sb1";

    private static FakeRng allFirst() {
        boolean[] draws = new boolean[Genes.codeOrder().size() * 2];
        Arrays.fill(draws, true);
        return new FakeRng().booleans(draws);
    }

    @Test
    void homozygousDominantCrossHomozygousRecessiveIsHeterozygous() {
        String dad = LegacyCode.keyed("E/E-A/A-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T);
        String mom = LegacyCode.keyed("e/e-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T);
        String child = GeneticCodeCombiner.combine(dad, mom, allFirst());
        assertEquals(
                Genotype.parse(LegacyCode.keyed("E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T)),
                Genotype.parse(child));
        assertEquals(CoatPhenotype.BAY, Genotype.parse(child).phenotype());
    }

    @Test
    void everyGeneSegregates() {
        String a = LegacyCode.keyed("E/e-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/prl-n/n-n/n" + T);
        String child = GeneticCodeCombiner.combine(a, Genotype.wildType().toCode(), allFirst());
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
