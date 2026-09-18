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
 *
 * <p>Brindle takes <b>two</b> copies to show, and a stallion has one {@code X}
 * and so one copy - so no stallion is ever brindle, and every cross below is
 * really asking the same question: did the allele reach a filly from
 * <i>both</i> sides. The segregation is unchanged from the textbook
 * {@code X}-linked case; only which genotypes paint has moved.
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
     * It takes two copies. A stallion has one {@code X} and therefore one copy,
     * which can never reach two - so every stallion carrying the allele is a
     * carrier, and the only horse that shows brindle is a {@code Brn/Brn} mare.
     */
    @Test
    void onlyAHomozygousMareShowsBrindle() {
        assertEquals("wild", outcome(horse("X/X", "n/n")));
        assertEquals("brindle-carrier", outcome(horse("X/X", "Brn/n")));
        assertEquals("brindle", outcome(horse("X/X", "Brn/Brn")));
        assertEquals("wild", outcome(horse("X/Y", "n/Y")));
        assertEquals("brindle-carrier", outcome(horse("X/Y", "Brn/Y")),
                "a stallion's single copy is one, not two - he carries brindle and never shows it");
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
     * <b>The signature cross.</b> A carrier stallion gives his one brindle
     * {@code X} to every daughter and his {@code Y} to every son - so out of a
     * plain mare he throws no brindle at all, and <i>every</i> filly is a
     * carrier. One generation of the allele travelling completely unseen, which
     * is the pedigree read that makes the gene worth having.
     */
    @Test
    void aCarrierStallionThrowsNoBrindleAndAllCarrierDaughters() {
        Ratios r = cross(horse("X/X", "n/n"), horse("X/Y", "Brn/Y"));
        near(0.0, r.coltBrindle(), "brindle colts");
        near(0.0, r.fillyBrindle(), "brindle fillies");
        near(1.0, r.fillyCarrier(), "carrier fillies");
    }

    /**
     * A carrier mare to a plain stallion throws nothing visible either. Half her
     * sons take her {@code Brn}, but one copy is not two, so they are carriers
     * rather than the brindle colts the old one-copy rule produced.
     */
    @Test
    void aCarrierMareThrowsNoBrindleToAPlainStallion() {
        Ratios r = cross(horse("X/X", "Brn/n"), horse("X/Y", "n/Y"));
        near(0.0, r.coltBrindle(), "brindle colts");
        near(0.0, r.fillyBrindle(), "brindle fillies");
        near(0.5, r.fillyCarrier(), "carrier fillies");
    }

    /**
     * Even a <i>brindle</i> mare throws no brindle to a plain stallion: every
     * son gets one copy from her and is a carrier, and every daughter gets his
     * {@code n} alongside her {@code Brn} and is one too.
     */
    @Test
    void aBrindleMareThrowsNoBrindleToAPlainStallion() {
        Ratios r = cross(horse("X/X", "Brn/Brn"), horse("X/Y", "n/Y"));
        near(0.0, r.coltBrindle(), "brindle colts");
        near(0.0, r.fillyBrindle(), "brindle fillies");
        near(1.0, r.fillyCarrier(), "carrier fillies");
    }

    /**
     * A brindle filly needs the allele from <b>both</b> sides - a carrier sire
     * and a dam who at least carries it. Her brothers still cannot be brindle,
     * whatever the pairing.
     */
    @Test
    void aBrindleFillyNeedsTheAlleleFromBothSides() {
        Ratios r = cross(horse("X/X", "Brn/n"), horse("X/Y", "Brn/Y"));
        near(0.0, r.coltBrindle(), "brindle colts");
        near(0.5, r.fillyBrindle(), "brindle fillies");
    }

    /**
     * And the end of the breeding project: a brindle mare to a carrier sire
     * throws a brindle filly <b>every time</b>, because she has only {@code Brn}
     * to give and so has he. Still not one brindle colt.
     */
    @Test
    void aBrindleMareToACarrierSireThrowsNothingButBrindleFillies() {
        Ratios r = cross(horse("X/X", "Brn/Brn"), horse("X/Y", "Brn/Y"));
        near(0.0, r.coltBrindle(), "brindle colts");
        near(1.0, r.fillyBrindle(), "brindle fillies");
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

    /**
     * <b>The same rule through {@code Genome.breedWith}</b> - the draw every real foal
     * comes through: seed jar, carrots, natural covers and plain golden carrots. Only the
     * genotype-level draw was tested, and the in-game ratio pen caught the genome's copy
     * throwing brindle colts and plain fillies (2026-09-14). Both directions, because
     * which parent is "this" must not matter.
     */
    @Test
    void theGenomeDrawEveryRealFoalComesThroughIsSexLinkedToo() {
        com.example.horsegenetics.common.genetics.Genome dam = new com.example.horsegenetics.common.genetics.Genome(
                horse("X/X", "n/n"), com.example.horsegenetics.common.genetics.Epigenome.fromSeed(1L));
        com.example.horsegenetics.common.genetics.Genome sire = new com.example.horsegenetics.common.genetics.Genome(
                horse("X/Y", "Brn/Y"), com.example.horsegenetics.common.genetics.Epigenome.fromSeed(2L));
        Allele brn = GENE.fromToken("Brn");
        int fillies = 0;
        int colts = 0;
        for (int i = 0; i < 400; i++) {
            for (boolean damFirst : new boolean[]{true, false}) {
                com.example.horsegenetics.common.genetics.Genome foal = damFirst
                        ? dam.breedWith(sire, new SeededRng(i))
                        : sire.breedWith(dam, new SeededRng(i));
                AllelePair p = foal.genotype().pair(GENE);
                assertEquals(Inheritance.X_LINKED.copiesIn(foal.sex()), GENE.realAlleles(p).size(),
                        "a " + foal.sex() + " foal came out " + p.toTokens());
                if (foal.sex() == Sex.FEMALE) {
                    fillies++;
                    assertTrue(p.has(brn), "every filly carries her sire's brindle X, got " + p.toTokens());
                } else {
                    colts++;
                    assertFalse(p.has(brn), "a colt gets his sire's Y, not his X, got " + p.toTokens());
                }
            }
        }
        assertTrue(fillies > 200 && colts > 200, fillies + " fillies, " + colts + " colts");
    }

    // ------------------------------------------------------------------
    // The population
    // ------------------------------------------------------------------

    /**
     * The asymmetry, in the wild, and it is total: brindle is a <b>mare-only</b>
     * gene. Showing takes two copies and a stallion has one, so no stallion
     * shows it at any frequency, while a mare needs both of hers and so appears
     * at {@code p}&sup2;.
     */
    @Test
    void noWildStallionIsBrindleAndMaresAppearAtTheSquareOfTheFrequency() {
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
        double mareRate = mareBrindle / (double) mares;
        double expectedMareRate = BrindleGene.WILD_BRN_FREQUENCY * BrindleGene.WILD_BRN_FREQUENCY;
        assertEquals(0, stallionBrindle,
                "brindle takes two copies and a stallion has one, so none of the "
                        + stallions + " drawn should show it");
        assertTrue(Math.abs(mareRate - expectedMareRate) < 0.005,
                "wild brindle mares should sit at p^2 = " + expectedMareRate + ", got " + mareRate);
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
