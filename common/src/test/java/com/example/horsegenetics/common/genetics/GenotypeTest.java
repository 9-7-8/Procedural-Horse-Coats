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

    // --- Phenotype table: W_ -> WHITE (masks all), else ee* -> CHESTNUT, E_ aa -> BLACK, E_ A_ -> BAY ---

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
            "eeaaww, CHESTNUT",
            "EEAAww, BAY",
            "eeaaWw, WHITE",
            "eeaawW, WHITE",
            "EEAAWW, WHITE",
            "EeaawW, WHITE",
    })
    void phenotypeMatchesTable(String code, CoatPhenotype expected) {
        assertEquals(expected, Genotype.parse(code).phenotype());
    }

    @Test
    void whiteMasksEverything() {
        assertEquals(CoatPhenotype.WHITE, Genotype.parse("EEAAWw").phenotype());
        assertEquals(CoatPhenotype.WHITE, Genotype.parse("eeaaWW").phenotype());
    }

    @Test
    void chestnutIgnoresAgouti() {
        assertEquals(CoatPhenotype.CHESTNUT, Genotype.parse("eeAA").phenotype());
        assertEquals(CoatPhenotype.CHESTNUT, Genotype.parse("eeaa").phenotype());
    }

    // --- Legacy 4-char codes read as ww ---

    @Test
    void legacyFourCharCodeParsesAsHomozygousRecessiveWhite() {
        Genotype g = Genotype.parse("EeAa");
        assertEquals("EeAaww", g.toCode());
        assertEquals(CoatPhenotype.BAY, g.phenotype());
    }

    // --- Canonicalization: dominant allele written first, within each locus ---

    @ParameterizedTest(name = "parse({0}) -> {1}")
    @CsvSource({
            "eEaA, EeAaww",
            "eEAa, EeAaww",
            "Eeaa, Eeaaww",
            "eeaa, eeaaww",
            "EEAA, EEAAww",
            "eEaAwW, EeAaWw",
            "EEAAWW, EEAAWW",
    })
    void parseCanonicalizesAlleleOrder(String input, String expectedCode) {
        assertEquals(expectedCode, Genotype.parse(input).toCode());
    }

    @Test
    void ofCanonicalizesRegardlessOfArgOrder() {
        Genotype a = Genotype.of('e', 'E', 'a', 'A', 'w', 'W');
        Genotype b = Genotype.of('E', 'e', 'A', 'a', 'W', 'w');
        assertEquals("EeAaWw", a.toCode());
        assertEquals(a, b);
    }

    @Test
    void fourArgOfDefaultsWhiteLocusToRecessive() {
        assertEquals("EeAaww", Genotype.of('e', 'E', 'a', 'A').toCode());
    }

    @Test
    void parseAndOfAgreeAndRoundTripThroughToCode() {
        Genotype g = Genotype.parse("eEAawW");
        assertEquals(g, Genotype.of('e', 'E', 'A', 'a', 'w', 'W'));
        assertEquals(g, Genotype.parse(g.toCode()));
    }

    // --- Malformed input ---

    @ParameterizedTest
    @ValueSource(strings = {"", "Ee", "EeA", "EeAaa", "EeAa ", "eeaa\n", "EeAaW", "EeAawwx"})
    void parseRejectsWrongLength(String bad) {
        assertThrows(IllegalArgumentException.class, () -> Genotype.parse(bad));
    }

    @ParameterizedTest
    @ValueSource(strings = {"XxAa", "EeYy", "1234", "BbAa", "EEaX", "EeAaXx", "EeAawq"})
    void parseRejectsUnknownAlleles(String bad) {
        assertThrows(IllegalArgumentException.class, () -> Genotype.parse(bad));
    }

    @Test
    void parseRejectsNull() {
        assertThrows(NullPointerException.class, () -> Genotype.parse(null));
    }

    @Test
    void wrongLocusOrderRejected() {
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

    @Test
    void isWhiteTrueWithAnyDominantW() {
        assertTrue(Genotype.parse("eeaaWw").isWhite());
        assertTrue(Genotype.parse("eeaawW").isWhite());
        assertEquals(false, Genotype.parse("eeaaww").isWhite());
    }

    // --- equals / hashCode ---

    @Test
    void equalsIgnoresInputOrdering() {
        assertEquals(Genotype.parse("eEaAwW"), Genotype.parse("EeAaWw"));
        assertEquals(Genotype.parse("eEaAwW").hashCode(), Genotype.parse("EeAaWw").hashCode());
    }

    @Test
    void differentGenotypesAreNotEqual() {
        assertNotEquals(Genotype.parse("EeAa"), Genotype.parse("eeAa"));
        assertNotEquals(Genotype.parse("EeAaww"), Genotype.parse("EeAaWw"));
    }

    // --- random(): E,E,A,A boolean draws then two W int draws (1-in-ODDS for 'W') ---

    @Test
    void randomAllDominantEaDrawsNoWhiteIsBay() {
        Genotype g = Genotype.random(new FakeRng().booleans(true, true, true, true).ints(1, 1));
        assertEquals("EEAAww", g.toCode());
        assertEquals(CoatPhenotype.BAY, g.phenotype());
    }

    @Test
    void randomAllRecessiveDrawsIsChestnut() {
        Genotype g = Genotype.random(new FakeRng().booleans(false, false, false, false).ints(1, 1));
        assertEquals("eeaaww", g.toCode());
        assertEquals(CoatPhenotype.CHESTNUT, g.phenotype());
    }

    @Test
    void randomHeterozygousDraws() {
        Genotype g = Genotype.random(new FakeRng().booleans(true, false, false, true).ints(1, 1));
        assertEquals("EeAaww", g.toCode());
    }

    @Test
    void randomRollsWhiteWhenAWhiteAlleleDrawHitsZero() {
        Genotype g = Genotype.random(new FakeRng().booleans(false, false, false, false).ints(0, 1));
        assertEquals("eeaaWw", g.toCode());
        assertEquals(CoatPhenotype.WHITE, g.phenotype());
    }
}
