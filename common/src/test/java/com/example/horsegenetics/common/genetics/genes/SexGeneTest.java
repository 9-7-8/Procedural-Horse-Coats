package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sex is an ordinary locus, and everything interesting about it follows from
 * that: it is drawn like any gene for a founder, it segregates like any gene
 * for a foal, and it paints nothing.
 */
class SexGeneTest {

    private static final SexGene SEX = Genes.SEX;

    private static Genotype mare() {
        return Genotype.parse(Codes.of("sex", "X/X"));
    }

    private static Genotype stallion() {
        return Genotype.parse(Codes.of("sex", "X/Y"));
    }

    @Test
    void xxIsAMareAndXyIsAStallion() {
        assertEquals(Sex.FEMALE, mare().sex());
        assertEquals(Sex.MALE, stallion().sex());
        assertEquals("mare", SEX.expressionOf(SEX.pairFor(Sex.FEMALE)).id());
        assertEquals("stallion", SEX.expressionOf(SEX.pairFor(Sex.MALE)).id());
    }

    /** A code with no sex segment (the blank record sentinel) reads as a mare. */
    @Test
    void theParsingDefaultIsAMare() {
        assertEquals(Sex.FEMALE, Genotype.wildType().sex());
        assertEquals(Sex.FEMALE, Genotype.sexOf(""));
        assertEquals(SEX.X, SEX.defaultAllele());
    }

    /** {@link Genotype#sexOf(String)} is a shortcut past a full parse - it must agree with one. */
    @Test
    void theStringShortcutAgreesWithAFullParse() {
        for (String code : new String[]{
                Codes.of("sex", "X/X"),
                Codes.of("sex", "X/Y"),
                Codes.of("sex", "X/Y", "agouti", "A/a", "grey", "G/g"),
                Genotype.wildType().toCode(),
                ""}) {
            assertEquals(Genotype.parse(code).sex(), Genotype.sexOf(code), code);
        }
    }

    @Test
    void withSexRewritesOnlyTheSexLocus() {
        Genotype bayMare = Genotype.parse(Codes.of("sex", "X/X", "agouti", "A/a"));
        Genotype bayStallion = bayMare.withSex(Sex.MALE);
        assertEquals(Sex.MALE, bayStallion.sex());
        assertEquals(bayMare.pair(Genes.AGOUTI), bayStallion.pair(Genes.AGOUTI));
        assertEquals(bayMare, bayStallion.withSex(Sex.FEMALE));
    }

    // --- inheritance -----------------------------------------------------

    /**
     * The whole point of making sex a gene: a foal's sex comes out of the same
     * Mendelian draw as its colour, with no special case anywhere. The dam is
     * {@code X/X} so she can only give an {@code X}; the sire gives his
     * {@code X} (a filly) or his {@code Y} (a colt).
     */
    @Test
    void theSireDecidesTheFoalsSex() {
        Genome dam = Genome.of(mare(), new SeededRng(1L));
        Genome sire = Genome.of(stallion(), new SeededRng(2L));

        boolean sawColt = false;
        boolean sawFilly = false;
        for (long seed = 0; seed < 60; seed++) {
            Sex foal = dam.breedWith(sire, new SeededRng(seed)).sex();
            sawColt |= foal == Sex.MALE;
            sawFilly |= foal == Sex.FEMALE;
        }
        assertTrue(sawColt && sawFilly, "both sexes should turn up among 60 foals");
    }

    /** Two mares can only make a filly - the dam side has no {@code Y} to give. */
    @Test
    void aFoalOfTwoMaresIsAlwaysAFilly() {
        Genome a = Genome.of(mare(), new SeededRng(3L));
        Genome b = Genome.of(mare(), new SeededRng(4L));
        for (long seed = 0; seed < 40; seed++) {
            assertEquals(Sex.FEMALE, a.breedWith(b, new SeededRng(seed)).sex());
        }
    }

    /**
     * Y/Y is not a horse, so nothing that can actually happen produces one:
     * not a founder draw, and not a foal (a dam is always {@code X/X}, so
     * every foal gets an {@code X} from her).
     */
    @Test
    void nothingEverProducesAYYHorse() {
        AllelePair yy = new AllelePair(SEX.Y, SEX.Y);
        assertFalse(SEX.canOccur(yy));
        assertFalse(SEX.founderTable(null).pairs().contains(yy));
        for (long seed = 0; seed < 200; seed++) {
            assertNotEquals(yy, Genotype.random(new SeededRng(seed)).pair(Genes.SEX));
        }
        Genome dam = Genome.of(mare(), new SeededRng(5L));
        Genome sire = Genome.of(stallion(), new SeededRng(6L));
        for (long seed = 0; seed < 200; seed++) {
            assertNotEquals(yy, dam.breedWith(sire, new SeededRng(seed)).genotype().pair(Genes.SEX));
        }
    }

    /** Hand-written or not, a {@code Y/Y} code answers rather than throwing. */
    @Test
    void aHandWrittenYYReadsAsAStallion() {
        assertEquals(Sex.MALE, Genotype.parse(Codes.of("sex", "Y/Y")).sex());
    }

    // --- it paints nothing ------------------------------------------------

    /**
     * Both outcomes are wild types, so sex never reaches the coat pipeline and
     * a mare and a stallion of the same colour share one baked texture instead
     * of doubling the cache.
     */
    @Test
    void sexNeverTouchesTheCoat() {
        assertFalse(SEX.affectsCoat());
        for (var e : SEX.expressions()) {
            assertTrue(e.wildType(), e.id() + " should be a wild type");
        }
        Genotype bayMare = Genotype.parse(Codes.of("sex", "X/X", "agouti", "A/a"));
        assertEquals(bayMare.coatCode(), bayMare.withSex(Sex.MALE).coatCode());
        assertNotEquals(bayMare.toCode(), bayMare.withSex(Sex.MALE).toCode());
        assertFalse(bayMare.visibleGenes().contains(Genes.SEX));
    }

    /** It is resolved before every coat gene, so a later sex-linked locus can read it. */
    @Test
    void sexIsTheFirstGeneInTheOrder() {
        assertEquals(Genes.SEX, Genes.codeOrder().get(0));
        assertTrue(Genes.SEX.priority() < Genes.EXTENSION.priority());
    }
}
