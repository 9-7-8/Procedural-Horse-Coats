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
    void allPairsOfIsEveryUnorderedPairLeastDominantFirst() {
        assertEquals(List.of("ee", "Ee", "EE"), tokens(GenotypeCatalog.allPairsOf(Genes.EXTENSION)));
        for (Gene gene : Genes.codeOrder()) {
            int n = gene.alleles().size();
            assertEquals(n * (n + 1) / 2, GenotypeCatalog.allPairsOf(gene).size(), gene.key());
        }
    }

    @Test
    void aDominantGeneDropsItsHeterozygoteButAnIncompleteOneKeepsIt() {
        assertEquals(List.of("ee", "EE"), tokens(GenotypeCatalog.distinctPairsOf(Genes.EXTENSION)));
        assertEquals(List.of("ww", "WW"), tokens(GenotypeCatalog.distinctPairsOf(Genes.WHITE)));
        assertEquals(List.of("NN", "CrN", "CrCr"), tokens(GenotypeCatalog.distinctPairsOf(Genes.CREAM)));
        assertEquals(List.of("splspl", "Splspl", "SplSpl"), tokens(GenotypeCatalog.distinctPairsOf(Genes.SPLASH)));
    }

    @Test
    void distinctPairsMatchTheDeclaredDominancePattern() {
        for (Gene gene : Genes.codeOrder()) {
            int expected = gene.dominance().heterozygoteIsDistinct()
                    ? GenotypeCatalog.allPairsOf(gene).size()
                    : gene.alleles().size();
            assertEquals(expected, GenotypeCatalog.distinctPairsOf(gene).size(), gene.key());
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
            if (gene.dominance().masksOtherGenes()) {
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
            if (!masking.dominance().masksOtherGenes()) {
                continue;
            }
            List<Genotype> showing = GenotypeCatalog.entries().stream()
                    .filter(g -> !isWildType(masking, g))
                    .toList();
            assertEquals(1, showing.size(), masking.key() + " should own exactly one entry");
            Genotype only = showing.get(0);
            assertTrue(only.pair(masking).homozygous(), masking.key() + " entry should be homozygous");
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

    /** Every entry's label has to fit the three gene lines of a pen sign. */
    @Test
    void everyLabelWrapsOntoThreeSignLines() {
        for (Genotype g : GenotypeCatalog.entries()) {
            List<String> lines = GeneCodeDisplay.wrap(g, 3, 15);
            assertTrue(lines.size() <= 3, "too many lines for " + GeneCodeDisplay.shortForm(g) + ": " + lines);
            for (String line : lines) {
                assertTrue(line.length() <= 15, "line too wide: '" + line + "'");
            }
            assertEquals(GeneCodeDisplay.shortForm(g), String.join(" ", lines));
        }
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
        assertFalse(GenotypeCatalog.size() >= raw, "the dominance reduction should have removed duplicates");
    }

    private static boolean isWildType(Gene gene, Genotype genotype) {
        AllelePair p = genotype.pair(gene);
        return p.homozygous() && p.first().equals(gene.wildType());
    }

    private static List<String> tokens(List<AllelePair> pairs) {
        return pairs.stream().map(GenotypeCatalogTest::pair).toList();
    }

    private static String pair(AllelePair pair) {
        return pair.first().token() + pair.second().token();
    }
}
