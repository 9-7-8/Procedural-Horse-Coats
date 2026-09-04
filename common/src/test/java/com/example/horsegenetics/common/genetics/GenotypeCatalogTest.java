package com.example.horsegenetics.common.genetics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenotypeCatalogTest {

    @Test
    void allPairsOfIsEveryUnorderedPairAHorseCanCarry() {
        assertEquals(List.of("ee", "Ee", "EE"), tokens(GenotypeCatalog.allPairsOf(Genes.EXTENSION)));
        for (Gene gene : Genes.codeOrder()) {
            int n = gene.alleles().size();
            int impossible = 0;
            for (Allele a : gene.alleles()) {
                for (Allele b : gene.alleles()) {
                    if (a.order() <= b.order() && !gene.canOccur(new AllelePair(a, b))) {
                        impossible++;
                    }
                }
            }
            assertEquals(n * (n + 1) / 2 - impossible, GenotypeCatalog.allPairsOf(gene).size(), gene.key());
        }
    }

    /**
     * The sex locus is the one gene with a combination that cannot happen:
     * a foal always takes an {@code X} from its dam, so there is no
     * {@code Y/Y} horse to give a pen to - or to count.
     */
    @Test
    void theSexLocusHasNoYYCombination() {
        assertEquals(List.of("XY", "XX"), tokens(GenotypeCatalog.allPairsOf(Genes.SEX)));
    }

    /**
     * Sex changes nothing about the coat - both its outcomes are wild types -
     * so the gallery gives the whole locus one pen rather than doubling it.
     */
    @Test
    void sexDoesNotWidenTheGallery() {
        assertEquals(1, GenotypeCatalog.distinctPairsOf(Genes.SEX).size());
        assertFalse(Genes.SEX.affectsCoat());
    }

    /**
     * Pairs collapse by the {@link Expression} they land on, and nothing else -
     * so the reduction is exactly as coarse as the gene's own combination table
     * says, with no dominance metadata to disagree with it.
     */
    @Test
    void pairsCollapseByTheExpressionTheyLandOn() {
        // two outcomes, so two pens, and the homozygote represents each group
        assertEquals(List.of("ee", "EE"), tokens(GenotypeCatalog.distinctPairsOf(Genes.EXTENSION)));
        assertEquals(List.of("ww", "WW"), tokens(GenotypeCatalog.distinctPairsOf(Genes.WHITE)));
        // three alleles, six combinations. Five outcomes, but "wild" and the
        // pearl carrier are both wild types and look the same, so four pens.
        assertEquals(List.of("NN", "prlprl", "CrN", "CrCr"),
                tokens(GenotypeCatalog.distinctPairsOf(Genes.MATP)));
        // pink hair's carrier likewise folds into its wild type
        assertEquals(List.of("PihrPihr", "nn"), tokens(GenotypeCatalog.distinctPairsOf(Genes.PINK_HAIR)));
        // sabino reads its dose, so all three combinations are their own pen
        assertEquals(List.of("sb1sb1", "SB1sb1", "SB1SB1"),
                tokens(GenotypeCatalog.distinctPairsOf(Genes.SABINO)));
        // splash does *not* yet read its dose, and the catalogue says so out
        // loud: both variant combinations land on one expression, so one pen
        assertEquals(List.of("splspl", "SplSpl"), tokens(GenotypeCatalog.distinctPairsOf(Genes.SPLASH)));
    }

    @Test
    void thereIsOneDistinctPairPerExpressionTheGeneCanActuallyProduce() {
        for (Gene gene : Genes.codeOrder()) {
            Set<String> reachable = new HashSet<>();
            for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
                Expression e = gene.expressionOf(pair);
                reachable.add(e.wildType() ? "" : e.id());   // every wild type is one look
            }
            assertEquals(reachable.size(), GenotypeCatalog.distinctPairsOf(gene).size(), gene.key());
        }
    }

    /** The first gene in codeOrder is the fastest-varying digit. */
    @Test
    void extensionIsExhaustedBeforeAgoutiMoves() {
        List<String> first = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Genotype g = GenotypeCatalog.get(i);
            first.add(pair(g.pair(Genes.EXTENSION)) + pair(g.pair(Genes.AGOUTI)));
        }
        assertEquals(List.of("eeaa", "EEaa", "eeAA", "EEAA"), first);
    }

    @Test
    void sizeIsTheProductOfTheDistinctPairsPlusOnePenPerMaskingGene() {
        long unmasked = 1L;
        int masking = 0;
        for (Gene gene : Genes.codeOrder()) {
            if (masks(gene)) {
                masking++;                       // collapses to a single entry
            } else {
                unmasked *= GenotypeCatalog.distinctPairsOf(gene).size();
            }
        }
        assertEquals(unmasked + masking, GenotypeCatalog.size());
    }

    /** A masking gene gets exactly one pen, and it's the plain wild-type horse plus that gene. */
    @Test
    void eachMaskingGeneContributesExactlyOneEntry() {
        for (Gene masking : Genes.codeOrder()) {
            if (!masks(masking)) {
                continue;
            }
            List<Genotype> showing = GenotypeCatalog.entries().stream()
                    .filter(g -> !isWildType(masking, g))
                    .toList();
            assertEquals(1, showing.size(), masking.key() + " should own exactly one entry");
            Genotype only = showing.get(0);
            assertTrue(only.pair(masking).homozygous(), masking.key() + " entry should be homozygous");
            assertTrue(masking.expressionOf(only.pair(masking)).masks(),
                    masking.key() + " entry should be the combination that masks");
            for (Gene other : Genes.codeOrder()) {
                if (other != masking) {
                    assertTrue(isWildType(other, only),
                            "everything but " + masking.key() + " should be wild type, " + other.key() + " isn't");
                }
            }
        }
    }

    @Test
    void everyEntryIsDistinctAndRoundTripsThroughTheCode() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < GenotypeCatalog.size(); i++) {
            Genotype g = GenotypeCatalog.get(i);
            String code = g.toCode();
            assertTrue(codes.add(code), "duplicate genotype at index " + i + ": " + code);
            assertEquals(g, Genotype.parse(code));
        }
    }

    /** No two entries can share a short-form label, or two pens would read alike. */
    @Test
    void noTwoEntriesShareADisplayLabel() {
        Set<String> labels = new HashSet<>();
        for (Genotype g : GenotypeCatalog.entries()) {
            assertTrue(labels.add(GeneCodeDisplay.shortForm(g)), "duplicate label: " + GeneCodeDisplay.shortForm(g));
        }
    }

    /**
     * Every entry's label wraps onto the three gene lines of a pen sign without
     * losing anything.
     *
     * <p><b>The last line is allowed to run wide.</b> Eighteen genes come
     * nowhere near fitting three 15-character lines - a horse loaded up on the
     * white-pattern and dilution genes needs well over a hundred characters -
     * and {@code wrap} deliberately overflows the last line rather than
     * dropping a gene, because a sign that reads wide is better than a sign
     * that lies. The cap below is a tripwire against a regression in the wrap
     * logic, not a claim that it fits. The real fix is the gallery's planned
     * revert to random pens ({@code wiki/roadmap.html} §9), which retires
     * the per-genotype sign.
     */
    @Test
    void everyLabelWrapsOntoThreeSignLinesWithoutLosingAnything() {
        int widestLast = 0;
        for (Genotype g : GenotypeCatalog.entries()) {
            List<String> lines = GeneCodeDisplay.wrap(g, 3, 15);
            assertTrue(lines.size() <= 3, "too many lines for " + GeneCodeDisplay.shortForm(g) + ": " + lines);
            for (int i = 0; i < lines.size() - 1; i++) {
                assertTrue(lines.get(i).length() <= 15, "line too wide: '" + lines.get(i) + "'");
            }
            widestLast = Math.max(widestLast, lines.get(lines.size() - 1).length());
            assertEquals(GeneCodeDisplay.shortForm(g), String.join(" ", lines));
        }
        assertTrue(widestLast <= 140, "the overflowing last sign line has grown to " + widestLast + " chars");
    }

    @Test
    void indexOutsideTheCatalogueIsRejected() {
        assertThrows(IndexOutOfBoundsException.class, () -> GenotypeCatalog.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> GenotypeCatalog.get(GenotypeCatalog.size()));
    }

    @Test
    void theCatalogueIsFarSmallerThanTheRawAlleleProduct() {
        long raw = 1L;
        for (Gene gene : Genes.codeOrder()) {
            raw *= GenotypeCatalog.allPairsOf(gene).size();
        }
        assertFalse(GenotypeCatalog.size() >= raw, "the same-expression reduction should have removed duplicates");
    }

    /** Can any combination of this gene hide every other gene? */
    private static boolean masks(Gene gene) {
        for (Expression e : gene.expressions()) {
            if (e.masks()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWildType(Gene gene, Genotype genotype) {
        return gene.expressionOf(genotype.pair(gene)).wildType();
    }

    private static List<String> tokens(List<AllelePair> pairs) {
        return pairs.stream().map(GenotypeCatalogTest::pair).toList();
    }

    private static String pair(AllelePair pair) {
        return pair.first().token() + pair.second().token();
    }
}
