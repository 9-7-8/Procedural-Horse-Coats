package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoatDataTest {

    @Test
    void solidChestnutHasZeroLegBlackHeight() {
        CoatData data = CoatData.solid(CoatPhenotype.CHESTNUT);
        assertEquals(CoatPhenotype.CHESTNUT, data.phenotype());
        assertEquals(0f, data.legBlackHeight());
    }

    @Test
    void solidRejectsBay() {
        assertThrows(IllegalArgumentException.class, () -> CoatData.solid(CoatPhenotype.BAY));
    }

    @ParameterizedTest
    @ValueSource(floats = {0f, 0.25f, 1f})
    void bayKeepsItsLegBlackHeight(float height) {
        CoatData data = CoatData.bay(height);
        assertEquals(CoatPhenotype.BAY, data.phenotype());
        assertEquals(height, data.legBlackHeight());
    }

    @ParameterizedTest
    @ValueSource(floats = {-0.01f, 1.01f, -1f, 2f})
    void bayRejectsHeightOutsideUnitRange(float bad) {
        assertThrows(IllegalArgumentException.class, () -> CoatData.bay(bad));
    }

    @Test
    void fromRawRebuildsSolidCoats() {
        CoatData chestnut = CoatData.fromRaw(CoatPhenotype.CHESTNUT, 0f);
        CoatData black = CoatData.fromRaw(CoatPhenotype.BLACK, 0f);
        assertEquals(CoatPhenotype.CHESTNUT, chestnut.phenotype());
        assertEquals(CoatPhenotype.BLACK, black.phenotype());
    }

    @Test
    void fromRawRebuildsBayWithHeight() {
        CoatData data = CoatData.fromRaw(CoatPhenotype.BAY, 0.6f);
        assertEquals(CoatPhenotype.BAY, data.phenotype());
        assertEquals(0.6f, data.legBlackHeight());
    }

    @Test
    void fromRawIgnoresHeightForSolidCoats() {
        // Persistence stores legBlackHeight=0 for non-bay, but a stray non-zero
        // value must not blow up reconstruction - it's simply dropped.
        CoatData data = CoatData.fromRaw(CoatPhenotype.BLACK, 0.9f);
        assertEquals(CoatPhenotype.BLACK, data.phenotype());
        assertEquals(0f, data.legBlackHeight());
    }
}
