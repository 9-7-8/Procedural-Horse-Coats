package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.testutil.Codes;
import com.example.horsegenetics.common.testutil.FakeRng;
import com.example.horsegenetics.common.testutil.LegacyCode;
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
    /** Wild-type visual-pattern segments in the pre-rewrite positional order. */
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
        Genotype a = Genotype.parse(LegacyCode.keyed("e/E-a/A-w/W-t/T-c/Ch-spl/Spl-g/G-N/Cr-prl/N-n/n-n/n" + T));
        Genotype b = Genotype.parse(LegacyCode.keyed("E/e-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/prl-n/n-n/n" + T));
        assertEquals(b, a);
        assertEquals(b.toCode(), a.toCode());
    }

    @Test
    void parseIgnoresSegmentOrderAndFillsOrDropsGenes() {
        // segments in any order; a gene left out reads as wild type; a segment
        // naming a gene that is not registered is dropped, not an error.
        Genotype x = Genotype.parse("horsegenetics.agouti=A/a-horsegenetics.extension=E/e");
        assertEquals(Genotype.parse(Codes.of("extension", "E/e", "agouti", "A/a")), x);
        assertEquals(x, Genotype.parse(
                "horsegenetics.agouti=A/a-horsegenetics.extension=E/e-example.nosuchgene=q/q"));
        // the empty string is the all-wild-type genotype
        assertEquals(Genotype.wildType(), Genotype.parse(""));
    }

    @Test
    void multiCharTokensParse() {
        Genotype x = Genotype.parse(LegacyCode.keyed("E/e-A/a-w/w-t/t-c/c-Spl/spl-g/g-Cr/Cr-prl/prl-n/n-n/n" + T));
        assertTrue(x.isSplash());
        assertTrue(x.pair(Genes.CREAM).homozygous());
        assertTrue(x.pair(Genes.PEARL).homozygous());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "horsegenetics.extensionE/e",           // a segment with no '='
            "horsegenetics.extension=E",             // one allele, not two
            "horsegenetics.extension=E/e/E",         // three alleles in a segment
            "horsegenetics.extension=E/x",           // unknown allele token on a known gene
            "horsegenetics.extension=E/e-agouti",    // a later segment with no '='
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
        Genotype x = Genotype.parse(LegacyCode.keyed("E/e-A/a-w/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/N-n/n-n/n" + T));
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

    /**
     * {@code random} rolls each gene's pair in {@code codeOrder()} order.
     * Extension and agouti each draw a boolean pair; the diagnostic test gene
     * draws a single int; every other built-in draws an int pair.
     */
    @Test
    void randomConsumesItsDeclaredDrawsPerGeneInGeneOrder() {
        FakeRng rng = new FakeRng();
        for (Gene gene : Genes.codeOrder()) {
            if (gene == Genes.EXTENSION || gene == Genes.AGOUTI) {
                rng.booleans(true, true);
            } else if (gene == Genes.TEST) {
                rng.ints(1); // one draw, non-zero -> not a carrier
            } else {
                rng.ints(1, 1); // non-zero -> no rare allele
            }
        }
        Genotype x = Genotype.random(rng);
        assertEquals(CoatPhenotype.BAY, x.phenotype()); // E/E + A/A
        rng.assertExhausted();
    }

    @Test
    void randomRollsRarerAllelesOnZero() {
        FakeRng rng = new FakeRng();
        for (Gene gene : Genes.codeOrder()) {
            if (gene == Genes.EXTENSION || gene == Genes.AGOUTI) {
                rng.booleans(false, false);
            } else if (gene == Genes.TEST) {
                rng.ints(0); // one draw, zero -> carrier
            } else {
                rng.ints(0, 0); // zero -> the rare / variant allele
            }
        }
        Genotype x = Genotype.random(rng);
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
        rng.assertExhausted();
    }

    @Test
    void breedWithIsMendelianAndSymmetric() {
        Genotype dad = Genotype.parse(LegacyCode.keyed("E/E-A/A-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T));
        Genotype mom = Genotype.wildType();
        boolean[] allFirst = new boolean[Genes.codeOrder().size() * 2];
        java.util.Arrays.fill(allFirst, true);
        Genotype ab = dad.breedWith(mom, new FakeRng().booleans(allFirst));
        Genotype ba = mom.breedWith(dad, new FakeRng().booleans(allFirst));
        // dad E/E x mom E/E -> E/E; dad A/A x mom a/a -> A/a
        assertEquals(Genotype.parse(Codes.of("agouti", "A/a")), ab);
        assertEquals(ab.pair(Genes.AGOUTI), ba.pair(Genes.AGOUTI));
    }

    @Test
    void breedInheritsEveryGene() {
        Genotype a = Genotype.parse(LegacyCode.keyed("E/e-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/prl-n/n-n/n" + T));
        boolean[] draws = new boolean[Genes.codeOrder().size() * 2];
        java.util.Arrays.fill(draws, true);
        Genotype child = a.breedWith(Genotype.wildType(), new FakeRng().booleans(draws));
        assertTrue(child.hasTest());
        assertTrue(child.isSplash());
        assertTrue(child.isGrey());
        assertTrue(child.isChampagne());
        assertTrue(child.has(Genes.CREAM.Cr));
    }

    @Test
    void differentGenotypesNotEqual() {
        assertNotEquals(Genotype.wildType(), Genotype.parse(Codes.of("extension", "e/e")));
    }
}
