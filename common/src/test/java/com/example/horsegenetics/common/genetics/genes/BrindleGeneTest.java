package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.GenotypeCatalog;
import com.example.horsegenetics.common.genetics.Inheritance;
import com.example.horsegenetics.common.horse.Sex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Brindle</b>, and through it the whole sex-linked scaffolding. The
 * assertions that matter are the crosses: an {@code X}-linked gene segregates
 * differently from every other gene in the mod, and the difference is a set of
 * ratios a horseman would recognise.
 */
class BrindleGeneTest {

    private static final BrindleGene GENE = Genes.BRINDLE;

    private static Genotype horse(String sex, String brindle) {
        return Genotype.parse("horsegenetics.sex=" + sex + "-horsegenetics.brindle=" + brindle);
    }

    private static String outcome(Genotype g) {
        return GENE.expressionIn(g.pair(GENE), g).id();
    }

    // ------------------------------------------------------------------
    // The declaration
    // ------------------------------------------------------------------

    @Test
    void itIsXLinkedAndDeclaresItsPlaceholderLast() {
        assertEquals(Inheritance.X_LINKED, GENE.inheritance());
        assertEquals(GENE.Y, GENE.hemizygousPlaceholder());
        assertEquals(GENE.alleles().size() - 1, GENE.Y.order(),
                "the reserved Y must sort to the second slot, or a stallion's real allele lands there");
        assertTrue(GENE.isPlaceholder(GENE.Y));
        assertFalse(GENE.isPlaceholder(GENE.Brn));
    }

    @Test
    void aStallionHasOneRealCopyAndAMareTwo() {
        assertEquals(2, GENE.realAlleles(horse("X/X", "Brn/n").pair(GENE)).size());
        assertEquals(1, GENE.realAlleles(horse("X/Y", "Brn/Y").pair(GENE)).size());
        assertEquals(2, Inheritance.X_LINKED.copiesIn(Sex.FEMALE));
        assertEquals(1, Inheritance.X_LINKED.copiesIn(Sex.MALE));
        assertTrue(Inheritance.X_LINKED.hemizygousIn(Sex.MALE));
        assertEquals("X-", Inheritance.X_LINKED.displayPrefix());
    }

    /**
     * A stallion is never a carrier - one copy is all he has, so if he has the
     * allele he wears it. A mare with one copy shows nothing.
     */
    @Test
    void aStallionCannotCarryBrindleWithoutShowingIt() {
        assertEquals("wild", outcome(horse("X/X", "n/n")));
        assertEquals("brindle-carrier", outcome(horse("X/X", "Brn/n")));
        assertEquals("brindle", outcome(horse("X/X", "Brn/Brn")));
        assertEquals("wild", outcome(horse("X/Y", "n/Y")));
        assertEquals("brindle", outcome(horse("X/Y", "Brn/Y")));
    }

    /** No horse has two Y chromosomes, so the catalogue must not enumerate {@code Y/Y}. */
    @Test
    void theCatalogueDropsTheImpossibleCombination() {
        assertFalse(GENE.sexConsistent(new AllelePair(GENE.Y, GENE.Y)));
        assertTrue(GENE.sexConsistent(new AllelePair(GENE.Brn, GENE.Y)));
        assertTrue(GENE.sexConsistent(new AllelePair(GENE.Brn, GENE.Brn)));
        for (AllelePair p : GenotypeCatalog.allPairsOf(GENE)) {
            assertFalse(p.first().equals(GENE.Y) && p.second().equals(GENE.Y));
        }
        assertEquals(5, GenotypeCatalog.allPairsOf(GENE).size(), "six combinations minus Y/Y");
    }

    // ------------------------------------------------------------------
    // The crosses - this is the point of the gene
    // ------------------------------------------------------------------

    private record Ratios(double coltBrindle, double fillyBrindle, double fillyCarrier) {}

    private static Ratios cross(Genotype dam, Genotype sire) {
        int colts = 0;
        int coltB = 0;
        int fillies = 0;
        int fillyB = 0;
        int fillyC = 0;
        for (int i = 0; i < 4000; i++) {
            Genotype foal = dam.breedWith(sire, new SeededRng(i * 7919L));
            String e = outcome(foal);
            if (foal.sex() == Sex.MALE) {
                colts++;
                if (e.equals("brindle")) {
                    coltB++;
                }
            } else {
                fillies++;
                if (e.equals("brindle")) {
                    fillyB++;
                }
                if (e.equals("brindle-carrier")) {
                    fillyC++;
                }
            }
        }
        return new Ratios(coltB / (double) colts, fillyB / (double) fillies, fillyC / (double) fillies);
    }

    private static void near(double expected, double actual, String what) {
        assertTrue(Math.abs(expected - actual) < 0.05,
                what + ": expected about " + expected + ", got " + actual);
    }

    /**
     * <b>The signature cross.</b> A brindle stallion gives his one brindle
     * {@code X} to every daughter and his {@code Y} to every son - so he throws
     * no brindle colts at all and every filly is a carrier. This is the
     * generation-skipping that makes the gene worth having.
     */
    @Test
    void aBrindleStallionThrowsNoBrindleSonsAndAllCarrierDaughters() {
        Ratios r = cross(horse("X/X", "n/n"), horse("X/Y", "Brn/Y"));
        near(0.0, r.coltBrindle(), "brindle colts");
        near(0.0, r.fillyBrindle(), "brindle fillies");
        near(1.0, r.fillyCarrier(), "carrier fillies");
    }

    /** And a generation later it comes back through those daughters, in half their sons. */
    @Test
    void aCarrierMareThrowsHalfHerSonsBrindle() {
        Ratios r = cross(horse("X/X", "Brn/n"), horse("X/Y", "n/Y"));
        near(0.5, r.coltBrindle(), "brindle colts");
        near(0.0, r.fillyBrindle(), "brindle fillies");
        near(0.5, r.fillyCarrier(), "carrier fillies");
    }

    /** A brindle mare throws brindle colts every time - she has nothing else to give. */
    @Test
    void aBrindleMareThrowsOnlyBrindleSons() {
        Ratios r = cross(horse("X/X", "Brn/Brn"), horse("X/Y", "n/Y"));
        near(1.0, r.coltBrindle(), "brindle colts");
        near(0.0, r.fillyBrindle(), "brindle fillies");
        near(1.0, r.fillyCarrier(), "carrier fillies");
    }

    /** The only pairing that produces a brindle filly needs a brindle sire. */
    @Test
    void aBrindleFillyNeedsABrindleSire() {
        Ratios r = cross(horse("X/X", "Brn/n"), horse("X/Y", "Brn/Y"));
        near(0.5, r.coltBrindle(), "brindle colts");
        near(0.5, r.fillyBrindle(), "brindle fillies");
    }

    /**
     * A foal never ends up with a combination its sex cannot have - no mare with
     * a {@code Y} in her brindle locus, no stallion with two real alleles.
     */
    @Test
    void everyFoalGetsACombinationItsSexCanActuallyHave() {
        Genotype dam = horse("X/X", "Brn/n");
        Genotype sire = horse("X/Y", "Brn/Y");
        for (int i = 0; i < 2000; i++) {
            Genotype foal = dam.breedWith(sire, new SeededRng(i));
            AllelePair p = foal.pair(GENE);
            int real = GENE.realAlleles(p).size();
            assertEquals(Inheritance.X_LINKED.copiesIn(foal.sex()), real,
                    "a " + foal.sex() + " foal came out with " + real + " real copies: " + p.toTokens());
            assertTrue(GENE.sexConsistent(p));
        }
    }

    // ------------------------------------------------------------------
    // The population
    // ------------------------------------------------------------------

    /**
     * The asymmetry, in the wild. A stallion needs one copy and a mare two, so
     * brindle stallions outnumber brindle mares by about {@code 1/p} - fifty to
     * one at this frequency. That is the thing a player notices before they know
     * why.
     */
    @Test
    void brindleStallionsHugelyOutnumberBrindleMaresInTheWild() {
        int mares = 0;
        int mareBrindle = 0;
        int stallions = 0;
        int stallionBrindle = 0;
        for (int i = 0; i < 60_000; i++) {
            Genotype g = Genotype.random(new SeededRng(i));
            boolean brindle = outcome(g).equals("brindle");
            if (g.sex() == Sex.FEMALE) {
                mares++;
                if (brindle) {
                    mareBrindle++;
                }
            } else {
                stallions++;
                if (brindle) {
                    stallionBrindle++;
                }
            }
        }
        double stallionRate = stallionBrindle / (double) stallions;
        double mareRate = mareBrindle / (double) mares;
        assertTrue(Math.abs(stallionRate - BrindleGene.WILD_BRN_FREQUENCY) < 0.005,
                "a stallion's rate is the allele frequency itself, got " + stallionRate);
        assertTrue(mareRate < stallionRate / 10.0,
                "brindle mares should be far rarer than brindle stallions: "
                        + mareRate + " vs " + stallionRate);
    }

    /** No founder is ever handed a combination its sex cannot have. */
    @Test
    void noFounderGetsAnImpossibleCombination() {
        for (int i = 0; i < 20_000; i++) {
            Genotype g = Genotype.random(new SeededRng(i));
            AllelePair p = g.pair(GENE);
            assertEquals(Inheritance.X_LINKED.copiesIn(g.sex()), GENE.realAlleles(p).size(),
                    "founder " + i + " (" + g.sex() + ") got " + p.toTokens());
        }
    }

    // ------------------------------------------------------------------
    // The coat
    // ------------------------------------------------------------------

    /** It is a natural gene, so it paints - and only by taking pigment away. */
    @Test
    void itIsANaturalCoatGene() {
        assertTrue(GENE.isNatural());
        assertTrue(GENE.affectsCoat());
        assertTrue(Genes.naturalOrder().contains(GENE));
        for (Allele a : GENE.alleles()) {
            assertEquals(BrindleGene.KEY, a.geneKey());
        }
    }
}
