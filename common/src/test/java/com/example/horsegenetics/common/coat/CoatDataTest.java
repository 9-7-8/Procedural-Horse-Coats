package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoatDataTest {

    private static final String BLACK = Genotype.wildType().toCode();
    private static final String BAY = Codes.of("extension", "E/e", "agouti", "A/a");
    /** Same bay, but also carrying grey - whose epigenetics are invisible on a foal-free adult check. */
    private static final String BAY_GREY = Codes.of("extension", "E/e", "agouti", "A/a", "grey", "G3/N");

    private static CoatData coat(String code, long seed) {
        return new CoatData(Genotype.parse(code), Epigenome.fromSeed(seed));
    }

    @Test
    void carriesGenotypeAndEpigenomeAndDerivesPhenotype() {
        CoatData c = coat(BLACK, 42L);
        assertEquals(CoatPhenotype.BLACK, c.phenotype());
        // Fitted to the alleles: a wild-type copy carries no numbers (2026-09-14).
        assertEquals(Epigenome.fromSeed(42L).alignedTo(Genotype.parse(BLACK)), c.epigenome());
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
        // A bay, not a black: a horse that is wild type everywhere carries no numbers at all,
        // so two blacks from different seeds are now the same horse - which is the point.
        assertNotEquals(coat(BAY, 7L), coat(BAY, 8L));
        assertEquals(coat(BLACK, 7L), coat(BLACK, 8L));
    }

    @Test
    void defaultIsAPlainBlackDeterministicHorse() {
        assertEquals(CoatPhenotype.BLACK, CoatData.DEFAULT.phenotype());
        assertTrue(CoatData.DEFAULT.isDeterministic());
    }
}
