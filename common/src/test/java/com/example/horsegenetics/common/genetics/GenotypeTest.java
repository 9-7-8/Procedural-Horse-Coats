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

    // code = 9 gene segments joined by '-', alleles by '/', dominant first:
    //   extension / agouti / white / test / champagne / splash / grey / cream / pearl
    private static final String WT = "E/E-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N";

    private static Genotype g(AllelePair... pairs) {
        return Genotype.of(pairs);
    }

    private static AllelePair p(Allele a, Allele b) {
        return new AllelePair(a, b);
    }

    @Test
    void wildTypeCodeIsBlackAndRoundTrips() {
        Genotype wt = Genotype.wildType();
        assertEquals(WT, wt.toCode());
        assertEquals(CoatPhenotype.BLACK, wt.phenotype());
        assertEquals(wt, Genotype.parse(wt.toCode()));
    }

    @Test
    void parseIsCanonicalAndOrderIndependent() {
        Genotype a = Genotype.parse("e/E-a/A-w/W-t/T-c/Ch-spl/Spl-g/G-N/Cr-prl/N");
        Genotype b = Genotype.parse("E/e-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/prl");
        assertEquals(b, a);
        assertEquals(b.toCode(), a.toCode());
    }

    @Test
    void multiCharTokensParse() {
        Genotype x = Genotype.parse("E/e-A/a-w/w-t/t-c/c-Spl/spl-g/g-Cr/Cr-prl/prl");
        assertTrue(x.isSplash());
        assertTrue(x.pair(Genes.CREAM).homozygous());
        assertTrue(x.pair(Genes.PEARL).homozygous());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "E/e",
            "E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N",     // 8 segments, need 9
            "E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-x/x", // 10
            "E/e/E-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N",   // 3 alleles in a segment
            "E/x-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N",     // unknown allele
    })
    void parseRejectsMalformed(String bad) {
        assertThrows(IllegalArgumentException.class, () -> Genotype.parse(bad));
    }

    @Test
    void parseRejectsNull() {
        assertThrows(NullPointerException.class, () -> Genotype.parse(null));
    }

    @Test
    void phenotype() {
        assertEquals(CoatPhenotype.CHESTNUT, g(p(Genes.EXTENSION.e, Genes.EXTENSION.e)).phenotype());
        assertEquals(CoatPhenotype.BLACK, g(p(Genes.EXTENSION.E, Genes.EXTENSION.e)).phenotype());
        assertEquals(CoatPhenotype.BAY, g(p(Genes.EXTENSION.E, Genes.EXTENSION.e),
                p(Genes.AGOUTI.A, Genes.AGOUTI.a)).phenotype());
        assertEquals(CoatPhenotype.WHITE, g(p(Genes.WHITE.W, Genes.WHITE.w)).phenotype());
        // champagne / grey / cream / pearl / test / splash never move the coarse phenotype
        assertEquals(CoatPhenotype.BLACK, g(p(Genes.EXTENSION.E, Genes.EXTENSION.e),
                p(Genes.CHAMPAGNE.Ch, Genes.CHAMPAGNE.c),
                p(Genes.GREY.G, Genes.GREY.g),
                p(Genes.CREAM.Cr, Genes.CREAM.N)).phenotype());
    }

    @Test
    void predicates() {
        Genotype x = Genotype.parse("E/e-A/a-w/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/N");
        assertTrue(x.hasBlackPigment());
        assertTrue(x.isAgouti());
        assertTrue(x.hasTest());
        assertTrue(x.isChampagne());
        assertTrue(x.isSplash());
        assertTrue(x.isGrey());
        assertFalse(x.isWhite());
        assertTrue(x.has(Genes.CREAM.Cr));
    }

    @Test
    void determinism() {
        assertTrue(Genotype.wildType().isDeterministic());                             // black
        assertTrue(g(p(Genes.EXTENSION.e, Genes.EXTENSION.e)).isDeterministic());       // chestnut
        assertTrue(g(p(Genes.WHITE.W, Genes.WHITE.w)).isDeterministic());               // white
        assertTrue(g(p(Genes.CHAMPAGNE.Ch, Genes.CHAMPAGNE.c)).isDeterministic());      // champagne
        assertTrue(g(p(Genes.CREAM.Cr, Genes.CREAM.Cr)).isDeterministic());             // perlino-on-black

        assertFalse(g(p(Genes.EXTENSION.E, Genes.EXTENSION.e),
                p(Genes.AGOUTI.A, Genes.AGOUTI.a)).isDeterministic());                  // bay
        assertFalse(g(p(Genes.SPLASH.Spl, Genes.SPLASH.spl)).isDeterministic());        // splash
        assertFalse(g(p(Genes.GREY.G, Genes.GREY.g)).isDeterministic());                // grey - dapples vary

        // chestnut masks agouti -> deterministic
        assertTrue(g(p(Genes.EXTENSION.e, Genes.EXTENSION.e),
                p(Genes.AGOUTI.A, Genes.AGOUTI.a)).isDeterministic());
    }

    @Test
    void randomConsumesDrawsInGeneOrder() {
        // ext 2 bool | agouti 2 bool | white 2 int(50) | test 1 int(4)
        //   | champ 2 int(40) | splash 2 int(20) | grey 2 int(16) | cream 2 int(30) | pearl 2 int(22)
        Genotype x = Genotype.random(new FakeRng()
                .booleans(true, true, true, false)
                .ints(1, 1, 1, 39, 39, 5, 5, 12, 12, 20, 20, 10, 10));
        assertEquals("E/E-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N", x.toCode());
        assertEquals(CoatPhenotype.BAY, x.phenotype());
    }

    @Test
    void randomRollsRarerAllelesOnZero() {
        Genotype x = Genotype.random(new FakeRng()
                .booleans(false, false, false, false)
                .ints(0, 1, 0, 0, 39, 0, 15, 0, 15, 0, 25, 0, 20));
        assertTrue(x.isWhite());
        assertTrue(x.hasTest());
        assertTrue(x.isChampagne());
        assertTrue(x.isSplash());
        assertTrue(x.isGrey());
        assertTrue(x.has(Genes.CREAM.Cr));
        assertTrue(x.has(Genes.PEARL.prl));
    }

    @Test
    void breedWithIsMendelianAndSymmetric() {
        Genotype dad = Genotype.parse("E/E-A/A-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N");
        Genotype mom = Genotype.wildType();
        boolean[] allFirst = new boolean[Genes.codeOrder().size() * 2];
        java.util.Arrays.fill(allFirst, true);
        Genotype ab = dad.breedWith(mom, new FakeRng().booleans(allFirst));
        Genotype ba = mom.breedWith(dad, new FakeRng().booleans(allFirst));
        assertEquals("E/E-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N", ab.toCode());
        assertEquals(ab.pair(Genes.AGOUTI), ba.pair(Genes.AGOUTI));
    }

    @Test
    void breedInheritsEveryGene() {
        Genotype a = Genotype.parse("E/e-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/prl");
        boolean[] draws = new boolean[Genes.codeOrder().size() * 2];
        java.util.Arrays.fill(draws, true);
        Genotype child = a.breedWith(Genotype.wildType(), new FakeRng().booleans(draws));
        assertEquals("E/E-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/N", child.toCode());
    }

    @Test
    void differentGenotypesNotEqual() {
        assertNotEquals(Genotype.wildType(), Genotype.parse("e/e-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N"));
    }
}
