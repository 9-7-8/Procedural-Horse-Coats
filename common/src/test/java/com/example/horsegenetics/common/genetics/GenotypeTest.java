package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.testutil.Codes;
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

    private static final String WT = Codes.wildType();
    /** Wild-type segments for the visual-pattern genes added after the first 11. */
    private static final String T = "-d/d-z/z-mu/mu-rn/rn-to/to-ov/ov-sb1/sb1";

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
        Genotype a = Genotype.parse("e/E-a/A-w/W-t/T-c/Ch-spl/Spl-g/G-N/Cr-prl/N-n/n-n/n" + T);
        Genotype b = Genotype.parse("E/e-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/prl-n/n-n/n" + T);
        assertEquals(b, a);
        assertEquals(b.toCode(), a.toCode());
    }

    @Test
    void multiCharTokensParse() {
        Genotype x = Genotype.parse("E/e-A/a-w/w-t/t-c/c-Spl/spl-g/g-Cr/Cr-prl/prl-n/n-n/n" + T);
        assertTrue(x.isSplash());
        assertTrue(x.pair(Genes.CREAM).homozygous());
        assertTrue(x.pair(Genes.PEARL).homozygous());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "E/e",
            "E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N",     // 8 segments, need 18
            "E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-x/x", // 10
            "E/e/E-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N",   // 3 alleles in a segment
            "E/x-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n",     // unknown allele
    })
    void parseRejectsMalformed(String bad) {
        assertThrows(IllegalArgumentException.class, () -> Genotype.parse(bad));
    }

    @Test
    void parseRejectsNull() {
        assertThrows(NullPointerException.class, () -> Genotype.parse(null));
    }

    @Test
    void parseRejectsUnknownAllele() {
        assertThrows(IllegalArgumentException.class,
                () -> Genotype.parse(Codes.of("extension", "E/x")));
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
        Genotype x = Genotype.parse("E/e-A/a-w/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/N-n/n-n/n" + T);
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
        //   | champ 2 int(40) | splash 2 int(20) | grey 2 int(16) | cream 2 int(30)
        //   | pearl 2 int(22) | magic zebra 2 int(100) | pink hair 2 int(12)
        //   | dun 2 int(24) | silver 2 int(60) | mushroom 2 int(34) | roan 2 int(30)
        //   | tobiano 2 int(50) | frame 2 int(55) | sabino 2 int(45)
        Genotype x = Genotype.random(new FakeRng()
                .booleans(true, true, true, false)
                .ints(1, 1, 1, 39, 39, 5, 5, 12, 12, 20, 20, 10, 10, 50, 50, 5, 5,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1));
        assertEquals("E/E-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T, x.toCode());
        assertEquals(CoatPhenotype.BAY, x.phenotype());
    }

    @Test
    void randomRollsRarerAllelesOnZero() {
        Genotype x = Genotype.random(new FakeRng()
                .booleans(false, false, false, false)
                .ints(0, 1, 0, 0, 39, 0, 15, 0, 15, 0, 25, 0, 20, 0, 50, 0, 5,
                        0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1));
        assertTrue(x.isWhite());
        assertTrue(x.hasTest());
        assertTrue(x.isChampagne());
        assertTrue(x.isSplash());
        assertTrue(x.isGrey());
        assertTrue(x.has(Genes.CREAM.Cr));
        assertTrue(x.has(Genes.PEARL.prl));
        assertTrue(x.has(Genes.MAGIC_ZEBRA.Mzeb));
        assertTrue(x.has(Genes.PINK_HAIR.Pihr));
        assertTrue(x.has(Genes.DUN.D));
        assertTrue(x.has(Genes.SILVER.Z));
        assertTrue(x.has(Genes.MUSHROOM.Mu));
        assertTrue(x.has(Genes.ROAN.Rn));
        assertTrue(x.has(Genes.TOBIANO.To));
        assertTrue(x.has(Genes.FRAME.Ov));
        assertTrue(x.has(Genes.SABINO.SB1));
    }

    @Test
    void breedWithIsMendelianAndSymmetric() {
        Genotype dad = Genotype.parse("E/E-A/A-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T);
        Genotype mom = Genotype.wildType();
        boolean[] allFirst = new boolean[Genes.codeOrder().size() * 2];
        java.util.Arrays.fill(allFirst, true);
        Genotype ab = dad.breedWith(mom, new FakeRng().booleans(allFirst));
        Genotype ba = mom.breedWith(dad, new FakeRng().booleans(allFirst));
        assertEquals("E/E-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T, ab.toCode());
        assertEquals(ab.pair(Genes.AGOUTI), ba.pair(Genes.AGOUTI));
    }

    @Test
    void breedInheritsEveryGene() {
        Genotype a = Genotype.parse("E/e-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/prl-n/n-n/n" + T);
        boolean[] draws = new boolean[Genes.codeOrder().size() * 2];
        java.util.Arrays.fill(draws, true);
        Genotype child = a.breedWith(Genotype.wildType(), new FakeRng().booleans(draws));
        assertEquals("E/E-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/N-n/n-n/n" + T, child.toCode());
    }

    @Test
    void differentGenotypesNotEqual() {
        assertNotEquals(Genotype.wildType(), Genotype.parse("e/e-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T));
    }
}
