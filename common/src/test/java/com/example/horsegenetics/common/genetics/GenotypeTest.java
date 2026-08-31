package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenotypeTest {

    // canonical code = 10 chars: EE | AA | WW | TT | CC   (extension, agouti, white, test, champagne)

    // --- phenotype table (E/A/W only; T and C never move it) ---

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "eeaa, CHESTNUT",
            "eeAa, CHESTNUT",
            "Eeaa, BLACK",
            "EEaa, BLACK",
            "EeAa, BAY",
            "EEAA, BAY",
            "eeaawwttcc, CHESTNUT",
            "EEAAwwttcc, BAY",
            "eeaaWwttcc, WHITE",
            "EEaaWWttcc, WHITE",
            "EeaawwttCc, BLACK",     // champagne doesn't change the coarse phenotype
            "EeAawwTtcc, BAY",       // test doesn't either
            "EeSawwttcc, BAY",       // seal reports as BAY (foal *_baby fallback)
    })
    void phenotypeMatchesTable(String code, CoatPhenotype expected) {
        assertEquals(expected, Genotype.parse(code).phenotype());
    }

    // --- legacy short codes pad with wild-type for the newer loci ---

    @ParameterizedTest(name = "parse({0}) -> {1}")
    @CsvSource({
            "EeAa,      EeAawwttcc",
            "eEaA,      EeAawwttcc",
            "EeAaWw,    EeAaWwttcc",
            "EeAawwtt,  EeAawwttcc",
            "eEaAwWtTcC, EeAaWwTtCc",
            "EEAAWWTTCC, EEAAWWTTCC",
    })
    void legacyAndCanonicalCodesRoundTrip(String input, String expected) {
        assertEquals(expected, Genotype.parse(input).toCode());
    }

    @Test
    void parseIsCanonicalAndOrderIndependent() {
        assertEquals(Genotype.parse("eEaAwWtTcC"), Genotype.parse("EeAaWwTtCc"));
        assertEquals(Genotype.parse("eEaAwWtTcC").hashCode(), Genotype.parse("EeAaWwTtCc").hashCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Ee", "EeA", "EeAaa", "EeAa ", "EeAawwttc", "EeAawwttccc", "EeAaw"})
    void parseRejectsWrongLength(String bad) {
        assertThrows(IllegalArgumentException.class, () -> Genotype.parse(bad));
    }

    @ParameterizedTest
    @ValueSource(strings = {"XxAa", "EeYy", "EeAaZz", "EeAawwQq", "EeAawwttZz"})
    void parseRejectsUnknownAlleles(String bad) {
        assertThrows(IllegalArgumentException.class, () -> Genotype.parse(bad));
    }

    @Test
    void parseRejectsNull() {
        assertThrows(NullPointerException.class, () -> Genotype.parse(null));
    }

    // --- allele predicates ---

    @Test
    void predicates() {
        assertTrue(Genotype.parse("Eeaa").hasBlackPigment());
        assertFalse(Genotype.parse("eeAA").hasBlackPigment());
        assertTrue(Genotype.parse("EeAa").isAgouti());
        assertTrue(Genotype.parse("eeaaWwttcc").isWhite());
        assertTrue(Genotype.parse("EeAawwTtcc").hasTest());
        assertTrue(Genotype.parse("EeaawwttCc").isChampagne());
        assertTrue(Genotype.parse("EeSawwttcc").isSeal());
        assertFalse(Genotype.parse("EeAawwttcc").isSeal());
    }

    @Test
    void ofFromAllelePairsFillsMissingGenesWithWildType() {
        Genotype g = Genotype.of(new AllelePair(Genes.EXTENSION.e, Genes.EXTENSION.e));
        assertEquals("eeaawwttcc", g.toCode());
        assertEquals(CoatPhenotype.CHESTNUT, g.phenotype());
    }

    @Test
    void hasAllele() {
        Genotype g = Genotype.parse("EeAawwTtcc");
        assertTrue(g.has(Genes.TEST.T));
        assertFalse(g.has(Genes.WHITE.W));
        assertEquals(Genes.AGOUTI.A, g.pair(Genes.AGOUTI).dominant());
    }

    // --- determinism ---

    @Test
    void determinismFollowsTheVisibleAlleles() {
        assertTrue(Genotype.parse("Eeaawwttcc").isDeterministic());   // black
        assertTrue(Genotype.parse("eeaawwttcc").isDeterministic());   // chestnut
        assertTrue(Genotype.parse("EeaawwttCc").isDeterministic());   // champagne
        assertTrue(Genotype.parse("eeaaWwttcc").isDeterministic());   // white
        assertFalse(Genotype.parse("EeAawwttcc").isDeterministic());  // bay - random points
        assertFalse(Genotype.parse("EeSawwttcc").isDeterministic());  // seal
        // a hidden non-deterministic allele that isn't expressed doesn't count:
        assertTrue(Genotype.parse("eeAawwttcc").isDeterministic());   // chestnut masks agouti
    }

    // --- random(): E,E bools | A,A ints(20) | W,W ints(50) | T int(4) | C,C ints(40) ---

    @Test
    void randomConsumesDrawsInGeneOrder() {
        Genotype g = Genotype.random(new FakeRng()
                .booleans(true, true)           // EE
                .ints(0, 12, 1, 1, 1, 39, 39)); // A=0->A, a=12->a ; W ww ; T tt ; C cc
        assertEquals("EEAawwttcc", g.toCode());
        assertEquals(CoatPhenotype.BAY, g.phenotype());
    }

    @Test
    void randomRollsWhiteAndTestAndChampagneOnZero() {
        Genotype g = Genotype.random(new FakeRng()
                .booleans(false, false)
                .ints(12, 12, 0, 1, 0, 0, 39));
        assertEquals("eeaaWwTtCc", g.toCode());
        assertTrue(g.isWhite());
        assertTrue(g.hasTest());
        assertTrue(g.isChampagne());
    }

    // --- breedWith: 2 booleans per gene (child allele from each parent), code order ---

    @Test
    void breedWithIsMendelianAndSymmetric() {
        Genotype ab = Genotype.parse("EEAA").breedWith(Genotype.parse("eeaa"),
                new FakeRng().booleans(true, false, true, false, true, false, true, false, true, false));
        Genotype ba = Genotype.parse("eeaa").breedWith(Genotype.parse("EEAA"),
                new FakeRng().booleans(false, true, false, true, false, true, false, true, false, true));
        assertEquals("EeAawwttcc", ab.toCode());
        assertEquals(ab, ba);
    }

    @Test
    void breedWithInheritsTestAndChampagne() {
        // all-"first allele" draws: E from EeAawwTtCc = E, E from eeaawwttcc = e -> Ee ; etc.
        Genotype child = Genotype.parse("EeAawwTtCc").breedWith(Genotype.parse("eeaawwttcc"),
                new FakeRng().booleans(true, true, true, true, true, true, true, true, true, true));
        assertEquals("EeAawwTtCc", child.toCode());
        assertTrue(child.hasTest());
        assertTrue(child.isChampagne());
    }
}
