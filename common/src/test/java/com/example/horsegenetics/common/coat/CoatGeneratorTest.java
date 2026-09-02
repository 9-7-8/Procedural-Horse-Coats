package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CoatGeneratorTest {

    private static final Genotype BAY = Genotype.parse("E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n");

    @Test
    void keepsTheGenotypeVerbatim() {
        Genotype g = Genotype.parse("e/e-a/a-w/w-t/t-Ch/c-spl/spl-g/g-N/N-N/N-n/n-n/n");
        CoatData data = CoatGenerator.generate(g, new SeededRng(1L));
        assertEquals(g, data.genotype());
        assertEquals(CoatPhenotype.CHESTNUT, data.phenotype());
    }

    @Test
    void givesEveryAlleleCopyItsOwnEpigenetics() {
        CoatData data = CoatGenerator.generate(BAY, new SeededRng(0xABCDL));
        var agouti = data.epigenome().copies(Genes.AGOUTI);
        assertNotEquals(agouti.first().epigeneticSeed(), agouti.second().epigeneticSeed(),
                "the A and the a copy get independent seeds");
        assertNotEquals(agouti.first().priority(), agouti.second().priority(),
                "no gene may carry the same priority twice");
    }

    @Test
    void twoFounderRollsOfTheSameGenotypeAreDifferentHorses() {
        CoatData a = CoatGenerator.generate(BAY, new SeededRng(1L));
        CoatData b = CoatGenerator.generate(BAY, new SeededRng(2L));
        assertEquals(a.genotype(), b.genotype());
        assertNotEquals(a.textureKey(), b.textureKey());
    }
}
