package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.CoatPhenotype;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoatGeneratorTest {

    @ParameterizedTest
    @ValueSource(strings = {"eeaa", "eeAA", "eeAa"})
    void chestnutGenotypesProduceSolidChestnutWithoutTouchingRng(String code) {
        // FakeRng with no values queued: if generate() rolls anything, it throws.
        CoatData data = CoatGenerator.generate(Genotype.parse(code), new FakeRng());
        assertEquals(CoatPhenotype.CHESTNUT, data.phenotype());
        assertEquals(0f, data.legBlackHeight());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Eeaa", "EEaa"})
    void blackGenotypesProduceSolidBlackWithoutTouchingRng(String code) {
        CoatData data = CoatGenerator.generate(Genotype.parse(code), new FakeRng());
        assertEquals(CoatPhenotype.BLACK, data.phenotype());
        assertEquals(0f, data.legBlackHeight());
    }

    @ParameterizedTest
    @ValueSource(strings = {"eeaaWw", "EEAAWW", "EeaawW"})
    void whiteGenotypesProduceSolidWhiteWithoutTouchingRng(String code) {
        CoatData data = CoatGenerator.generate(Genotype.parse(code), new FakeRng());
        assertEquals(CoatPhenotype.WHITE, data.phenotype());
        assertEquals(0f, data.legBlackHeight());
    }

    @Test
    void bayGenotypeRollsLegBlackHeightFromRng() {
        CoatData data = CoatGenerator.generate(Genotype.parse("EeAa"), new FakeRng().floats(0.42f));
        assertEquals(CoatPhenotype.BAY, data.phenotype());
        assertEquals(0.42f, data.legBlackHeight());
    }

    @Test
    void bayConsumesExactlyOneFloatDraw() {
        // Only one value queued; a second nextFloat() call would throw.
        CoatGenerator.generate(Genotype.parse("EEAA"), new FakeRng().floats(0.1f));
    }

    @ParameterizedTest
    @ValueSource(floats = {0f, 0.001f, 0.5f, 0.999f, 1f})
    void bayLegBlackHeightStaysInUnitRangeForAnyRngOutput(float roll) {
        CoatData data = CoatGenerator.generate(Genotype.parse("EeAa"), new FakeRng().floats(roll));
        float h = data.legBlackHeight();
        assertTrue(h >= 0f && h <= 1f, "legBlackHeight out of [0,1]: " + h);
    }

    @Test
    void bayRejectsOutOfRangeRngOutput() {
        // Documents current behavior: CoatData.bay() guards the range, so a
        // misbehaving Rng surfaces as an exception rather than a bad coat.
        assertThrows(IllegalArgumentException.class,
                () -> CoatGenerator.generate(Genotype.parse("EeAa"), new FakeRng().floats(1.5f)));
    }
}
