package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoatDataTest {

    private static CoatData coat(String code, long seed) {
        return new CoatData(Genotype.parse(code), seed);
    }

    @Test
    void carriesGenotypeAndSeedAndDerivesPhenotype() {
        CoatData c = coat("Eeaawwttcc", 42L);
        assertEquals(CoatPhenotype.BLACK, c.phenotype());
        assertEquals(42L, c.epigeneticSeed());
        assertEquals("Eeaawwttcc", c.genotype().toCode());
    }

    @Test
    void deterministicCoatsIgnoreTheSeedInTheirTextureKey() {
        assertTrue(coat("Eeaawwttcc", 1L).isDeterministic());
        assertEquals(coat("Eeaawwttcc", 1L).textureKey(), coat("Eeaawwttcc", 999L).textureKey());
    }

    @Test
    void nonDeterministicCoatsKeyOnTheSeed() {
        CoatData bay1 = coat("EeAawwttcc", 1L);
        CoatData bay2 = coat("EeAawwttcc", 2L);
        assertFalse(bay1.isDeterministic());
        assertNotEquals(bay1.textureKey(), bay2.textureKey());
    }

    @Test
    void equalityIsGenotypePlusSeed() {
        assertEquals(coat("Eeaawwttcc", 7L), coat("eEaawwttcc", 7L)); // code canonicalizes
        assertNotEquals(coat("Eeaawwttcc", 7L), coat("Eeaawwttcc", 8L));
    }

    @Test
    void defaultIsAPlainBlackHorse() {
        assertEquals(CoatPhenotype.BLACK, CoatData.DEFAULT.phenotype());
        assertTrue(CoatData.DEFAULT.isDeterministic());
    }
}
