package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoatDataTest {

    private static final String BLACK = Genotype.wildType().toCode();
    private static final String BAY = "E/e-A/a-w/w-t/t-c/c-sl/sl-spl/spl";

    private static CoatData coat(String code, long seed) {
        return new CoatData(Genotype.parse(code), seed);
    }

    @Test
    void carriesGenotypeAndSeedAndDerivesPhenotype() {
        CoatData c = coat(BLACK, 42L);
        assertEquals(CoatPhenotype.BLACK, c.phenotype());
        assertEquals(42L, c.epigeneticSeed());
    }

    @Test
    void deterministicCoatsIgnoreTheSeedInTheirTextureKey() {
        assertTrue(coat(BLACK, 1L).isDeterministic());
        assertEquals(coat(BLACK, 1L).textureKey(), coat(BLACK, 999L).textureKey());
    }

    @Test
    void nonDeterministicCoatsKeyOnTheSeed() {
        assertFalse(coat(BAY, 1L).isDeterministic());
        assertNotEquals(coat(BAY, 1L).textureKey(), coat(BAY, 2L).textureKey());
    }

    @Test
    void equalityIsGenotypePlusSeed() {
        assertEquals(coat(BLACK, 7L), coat(BLACK, 7L));
        assertNotEquals(coat(BLACK, 7L), coat(BLACK, 8L));
    }

    @Test
    void defaultIsAPlainBlackDeterministicHorse() {
        assertEquals(CoatPhenotype.BLACK, CoatData.DEFAULT.phenotype());
        assertTrue(CoatData.DEFAULT.isDeterministic());
    }
}
