package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoatGeneratorTest {

    @Test
    void rollsExactlyOneLongForTheEpigeneticSeed() {
        // FakeRng with one long queued: a second draw would throw.
        CoatData data = CoatGenerator.generate(Genotype.parse("EeAawwttcc"), new FakeRng().longs(0xABCDL));
        assertEquals(0xABCDL, data.epigeneticSeed());
        assertEquals(CoatPhenotype.BAY, data.phenotype());
    }

    @Test
    void keepsTheGenotypeVerbatim() {
        Genotype g = Genotype.parse("eeaawwttCc");
        CoatData data = CoatGenerator.generate(g, new FakeRng().longs(1L));
        assertEquals(g, data.genotype());
        assertEquals(CoatPhenotype.CHESTNUT, data.phenotype());
    }
}
