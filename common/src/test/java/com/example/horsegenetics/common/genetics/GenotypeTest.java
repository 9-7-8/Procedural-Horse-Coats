package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenotypeTest {

    // --- Phenotype table (CLAUDE.md): ee* -> CHESTNUT, E_ aa -> BLACK, E_ A_ -> BAY ---

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "eeaa, CHESTNUT",
            "eeAa, CHESTNUT",
            "eeAA, CHESTNUT",
            "Eeaa, BLACK",
            "EEaa, BLACK",
            "EeAa, BAY",
            "EeAA, BAY",
            "EEAa, BAY",
            "EEAA, BAY",
    })
    void phenotypeMatchesTable(String code, CoatPhenotype expected) {
        assertEquals(expected, Genotype.parse(code).phenotype());
    }

    @Test
    void chestnutIgnoresAgouti() {
        // ee masks the A locus entirely - both must still be chestnut.
        assertEquals(CoatPhenotype.CHESTNUT, Genotype.parse("eeAA").phenotype());
        assertEquals(CoatPhenotype.CHESTNUT, Genotype.parse("eeaa").phenotype());
    }

    // --- Canonicalization: dominant allele written first, within each locus ---

    @ParameterizedTest(name = "parse({0}) -> {1}")
    @CsvSource({
            "eEaA, EeAa",
            "eEAa, EeAa",
            "Eeaa, Eeaa",
            "eeaa, eeaa",
            "EEAA, EEAA",
    })
    void parseCanonicalizesAlleleOrder(String input, String expectedCode) {
        assertEquals(expectedCode, Genotype.parse(input).toCode());
    }

    @Test
    void ofCanonicalizesRegardlessOfArgOrder() {
        Genotype a = Genotype.of('e', 'E', 'a', 'A');
        Genotype b = Genotype.of('E', 'e', 'A', 'a');
        assertEquals("EeAa", a.toCode());
        assertEquals(a, b);
    }

    @Test
    void parseAndOfAgreeAndRoundTripThroughToCode() {
        Genotype g = Genotype.parse("eEAa");
        assertEquals(g, Genotype.of('e', 'E', 'A', 'a'));
        assertEquals(g, Genotype.parse(g.toCode()));
    }

    // --- Malformed input ---

    @ParameterizedTest
    @ValueSource(strings = {"", "Ee", "EeA", "EeAaa", "EeAa ", "eeaa\n"})
    void parseRejectsWrongLength(String bad) {
        assertThrows(IllegalArgumentException.class, () -> Genotype.parse(bad));
    }

    @ParameterizedTest
    @ValueSource(strings = {"XxAa", "EeYy", "1234", "BbAa", "EEaX"})
    void parseRejectsUnknownAlleles(String bad) {
        assertThrows(IllegalArgumentException.class, () -> Genotype.parse(bad));
    }

    @Test
    void parseRejectsNull() {
        assertThrows(NullPointerException.class, () -> Genotype.parse(null));
    }

    @Test
    void wrongLocusOrderRejected() {
        // A-locus letters in the E-locus slots and vice versa is not accepted.
        assertThrows(IllegalArgumentException.class, () -> Genotype.parse("aAeE"));
    }

    // --- Allele predicates ---

    @Test
    void hasBlackPigmentTrueWithAnyDominantE() {
        assertTrue(Genotype.parse("Eeaa").hasBlackPigment());
        assertTrue(Genotype.parse("EEaa").hasBlackPigment());
    }

    @Test
    void hasBlackPigmentFalseForHomozygousRecessiveE() {
        assertEquals(false, Genotype.parse("eeAA").hasBlackPigment());
    }

    @Test
    void isAgoutiTrueWithAnyDominantA() {
        assertTrue(Genotype.parse("EeAa").isAgouti());
        assertEquals(false, Genotype.parse("Eeaa").isAgouti());
    }

    // --- equals / hashCode ---

    @Test
    void equalsIgnoresInputOrdering() {
        assertEquals(Genotype.parse("eEaA"), Genotype.parse("EeAa"));
        assertEquals(Genotype.parse("eEaA").hashCode(), Genotype.parse("EeAa").hashCode());
    }

    @Test
    void differentGenotypesAreNotEqual() {
        assertNotEquals(Genotype.parse("EeAa"), Genotype.parse("eeAa"));
    }

    // --- random() consumes exactly four boolean draws, E,E,A,A order ---

    @Test
    void randomAllDominantDrawsIsBay() {
        Genotype g = Genotype.random(new FakeRng().booleans(true, true, true, true));
        assertEquals("EEAA", g.toCode());
        assertEquals(CoatPhenotype.BAY, g.phenotype());
    }

    @Test
    void randomAllRecessiveDrawsIsChestnut() {
        Genotype g = Genotype.random(new FakeRng().booleans(false, false, false, false));
        assertEquals("eeaa", g.toCode());
        assertEquals(CoatPhenotype.CHESTNUT, g.phenotype());
    }

    @Test
    void randomHeterozygousDraws() {
        // E-locus draws (true,false) -> Ee ; A-locus draws (false,true) -> Aa
        Genotype g = Genotype.random(new FakeRng().booleans(true, false, false, true));
        assertEquals("EeAa", g.toCode());
    }
}
