package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenotypeTest {

    // code = 7 gene segments joined by '-', alleles joined by '/', dominant first:
    //   extension / agouti / white / test / champagne / seal / splash

    private static Genotype g(AllelePair... pairs) {
        return Genotype.of(pairs);
    }

    private static AllelePair p(Allele a, Allele b) {
        return new AllelePair(a, b);
    }

    // --- code round-trip -------------------------------------------------

    @Test
    void wildTypeCodeIsBlackAndRoundTrips() {
        Genotype wt = Genotype.wildType();
        assertEquals("E/E-a/a-w/w-t/t-c/c-sl/sl-spl/spl", wt.toCode());
        assertEquals(CoatPhenotype.BLACK, wt.phenotype());
        assertEquals(wt, Genotype.parse(wt.toCode()));
    }

    @Test
    void parseIsCanonicalAndOrderIndependent() {
        Genotype a = Genotype.parse("e/E-a/A-w/W-t/T-c/Ch-sl/Sl-spl/Spl");
        Genotype b = Genotype.parse("E/e-A/a-W/w-T/t-Ch/c-Sl/sl-Spl/spl");
        assertEquals(b, a);
        assertEquals("E/e-A/a-W/w-T/t-Ch/c-Sl/sl-Spl/spl", a.toCode());
    }

    @Test
    void multiCharTokensParse() {
        Genotype seal = Genotype.parse("E/e-a/a-w/w-t/t-c/c-Sl/sl-spl/spl");
        assertTrue(seal.isSeal());
        Genotype splash = Genotype.parse("E/e-a/a-w/w-t/t-c/c-sl/sl-Spl/Spl");
        assertTrue(splash.isSplash());
        assertTrue(splash.pair(Genes.SPLASH).homozygous());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "E/e",
            "E/e-A/a-w/w-t/t-c/c-sl/sl",              // 6 segments, need 7
            "E/e-A/a-w/w-t/t-c/c-sl/sl-spl/spl-x/x",  // 8 segments
            "E/e/E-A/a-w/w-t/t-c/c-sl/sl-spl/spl",    // 3 alleles in a segment
            "E/x-A/a-w/w-t/t-c/c-sl/sl-spl/spl",      // unknown allele
    })
    void parseRejectsMalformed(String bad) {
        assertThrows(IllegalArgumentException.class, () -> Genotype.parse(bad));
    }

    @Test
    void parseRejectsNull() {
        assertThrows(NullPointerException.class, () -> Genotype.parse(null));
    }

    // --- phenotype ----------------------------------------------------

    @Test
    void phenotype() {
        assertEquals(CoatPhenotype.CHESTNUT, g(p(Genes.EXTENSION.e, Genes.EXTENSION.e)).phenotype());
        assertEquals(CoatPhenotype.BLACK, g(p(Genes.EXTENSION.E, Genes.EXTENSION.e)).phenotype());
        assertEquals(CoatPhenotype.BAY, g(p(Genes.EXTENSION.E, Genes.EXTENSION.e),
                p(Genes.AGOUTI.A, Genes.AGOUTI.a)).phenotype());
        assertEquals(CoatPhenotype.WHITE, g(p(Genes.WHITE.W, Genes.WHITE.w)).phenotype());
        // seal reports as BAY for the foal-texture fallback
        assertEquals(CoatPhenotype.BAY, g(p(Genes.EXTENSION.E, Genes.EXTENSION.e),
                p(Genes.SEAL.Sl, Genes.SEAL.sl)).phenotype());
        // test / champagne / splash never move the coarse phenotype
        assertEquals(CoatPhenotype.BLACK, g(p(Genes.EXTENSION.E, Genes.EXTENSION.e),
                p(Genes.CHAMPAGNE.Ch, Genes.CHAMPAGNE.c),
                p(Genes.TEST.T, Genes.TEST.t)).phenotype());
    }

    @Test
    void predicates() {
        Genotype x = Genotype.parse("E/e-A/a-w/w-T/t-Ch/c-Sl/sl-Spl/spl");
        assertTrue(x.hasBlackPigment());
        assertTrue(x.isAgouti());
        assertTrue(x.hasTest());
        assertTrue(x.isChampagne());
        assertTrue(x.isSeal());
        assertTrue(x.isSplash());
        assertFalse(x.isWhite());
        assertTrue(x.has(Genes.SPLASH.Spl));
        assertTrue(x.has(Genes.SPLASH.spl));
    }

    // --- determinism ------------------------------------------------

    @Test
    void determinism() {
        assertTrue(Genotype.wildType().isDeterministic());                                  // black
        assertTrue(g(p(Genes.EXTENSION.e, Genes.EXTENSION.e)).isDeterministic());           // chestnut
        assertTrue(g(p(Genes.WHITE.W, Genes.WHITE.w)).isDeterministic());                   // white
        assertTrue(g(p(Genes.CHAMPAGNE.Ch, Genes.CHAMPAGNE.c)).isDeterministic());          // champagne
        assertTrue(g(p(Genes.TEST.T, Genes.TEST.t)).isDeterministic());                     // test

        assertFalse(g(p(Genes.EXTENSION.E, Genes.EXTENSION.e),
                p(Genes.AGOUTI.A, Genes.AGOUTI.a)).isDeterministic());                       // bay
        assertFalse(g(p(Genes.EXTENSION.E, Genes.EXTENSION.e),
                p(Genes.SEAL.Sl, Genes.SEAL.sl)).isDeterministic());                         // seal
        assertFalse(g(p(Genes.SPLASH.Spl, Genes.SPLASH.spl)).isDeterministic());            // splash

        // a hidden non-deterministic allele that isn't expressed doesn't count:
        assertTrue(g(p(Genes.EXTENSION.e, Genes.EXTENSION.e),
                p(Genes.AGOUTI.A, Genes.AGOUTI.a)).isDeterministic());                       // chestnut masks agouti
    }

    // --- random ---------------------------------------------------

    @Test
    void randomConsumesDrawsInGeneOrder() {
        // extension: 2 bool | agouti: 2 bool | white: 2 int(50) | test: 1 int(4)
        //          | champagne: 2 int(40) | seal: 2 int(16) | splash: 2 int(20)
        Genotype x = Genotype.random(new FakeRng()
                .booleans(true, true, true, false)                       // EE, Aa
                .ints(1, 1, 1, 39, 39, 5, 5, 7, 7));                     // ww t cc slsl splspl
        assertEquals("E/E-A/a-w/w-t/t-c/c-sl/sl-spl/spl", x.toCode());
        assertEquals(CoatPhenotype.BAY, x.phenotype());
    }

    @Test
    void randomRollsTheRarerAllelesOnZero() {
        Genotype x = Genotype.random(new FakeRng()
                .booleans(false, false, false, false)
                .ints(0, 1, 0, 0, 39, 0, 15, 0, 19));                    // Ww T Chc Slsl Splspl
        assertTrue(x.isWhite());
        assertTrue(x.hasTest());
        assertTrue(x.isChampagne());
        assertTrue(x.isSeal());
        assertTrue(x.isSplash());
    }

    // --- breeding -------------------------------------------------

    @Test
    void breedWithIsMendelianAndSymmetric() {
        Genotype dad = Genotype.parse("E/E-A/A-w/w-t/t-c/c-sl/sl-spl/spl");
        Genotype mom = Genotype.wildType(); // E/E-a/a-...
        boolean[] allFirst = {true, true, true, true, true, true, true, true, true, true,
                true, true, true, true};
        Genotype ab = dad.breedWith(mom, new FakeRng().booleans(allFirst));
        Genotype ba = mom.breedWith(dad, new FakeRng().booleans(allFirst));
        assertEquals("E/E-A/a-w/w-t/t-c/c-sl/sl-spl/spl", ab.toCode());
        // ba: mom-first then dad-first -> agouti a (mom) / A (dad) -> canonical A/a
        assertEquals(ab.pair(Genes.AGOUTI), ba.pair(Genes.AGOUTI));
    }

    @Test
    void breedInheritsEveryGene() {
        Genotype a = Genotype.parse("E/e-A/a-W/w-T/t-Ch/c-Sl/sl-Spl/spl");
        Genotype b = Genotype.wildType();
        boolean[] draws = new boolean[Genes.codeOrder().size() * 2];
        java.util.Arrays.fill(draws, true); // always the first allele of each parent's pair
        Genotype child = a.breedWith(b, new FakeRng().booleans(draws));
        assertEquals("E/E-A/a-W/w-T/t-Ch/c-Sl/sl-Spl/spl", child.toCode());
    }

    @Test
    void differentGenotypesNotEqual() {
        assertNotEquals(Genotype.wildType(), Genotype.parse("e/e-a/a-w/w-t/t-c/c-sl/sl-spl/spl"));
    }
}
