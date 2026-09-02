package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoatDataTest {

    private static final String BLACK = Genotype.wildType().toCode();
    private static final String BAY = "E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n";
    /** Same bay, but also carrying grey - whose epigenetics are invisible on a foal-free adult check. */
    private static final String BAY_GREY = "E/e-A/a-w/w-t/t-c/c-spl/spl-G/g-N/N-N/N-n/n-n/n";

    private static CoatData coat(String code, long seed) {
        return new CoatData(Genotype.parse(code), Epigenome.fromSeed(seed));
    }

    @Test
    void carriesGenotypeAndEpigenomeAndDerivesPhenotype() {
        CoatData c = coat(BLACK, 42L);
        assertEquals(CoatPhenotype.BLACK, c.phenotype());
        assertEquals(Epigenome.fromSeed(42L), c.epigenome());
    }

    @Test
    void deterministicCoatsIgnoreEpigeneticsInTheirTextureKey() {
        assertTrue(coat(BLACK, 1L).isDeterministic());
        assertEquals(coat(BLACK, 1L).textureKey(), coat(BLACK, 999L).textureKey());
    }

    @Test
    void nonDeterministicCoatsKeyOnTheirVisibleEpigenetics() {
        assertFalse(coat(BAY, 1L).isDeterministic());
        assertNotEquals(coat(BAY, 1L).textureKey(), coat(BAY, 2L).textureKey());
    }

    @Test
    void greyIsPerHorseToo() {
        assertFalse(coat(BAY_GREY, 1L).isDeterministic());
        assertNotEquals(coat(BAY_GREY, 1L).textureKey(), coat(BAY_GREY, 2L).textureKey());
    }

    @Test
    void equalityIsGenotypePlusEpigenome() {
        assertEquals(coat(BLACK, 7L), coat(BLACK, 7L));
        assertNotEquals(coat(BLACK, 7L), coat(BLACK, 8L));
    }

    @Test
    void defaultIsAPlainBlackDeterministicHorse() {
        assertEquals(CoatPhenotype.BLACK, CoatData.DEFAULT.phenotype());
        assertTrue(CoatData.DEFAULT.isDeterministic());
    }
}
