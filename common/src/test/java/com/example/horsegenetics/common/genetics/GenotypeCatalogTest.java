package com.example.horsegenetics.common.genetics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
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
     * A combination that cannot happen gets no pen and is not counted. The sex
     * locus is the clearest case - a foal always takes an {@code X} from its
     * dam, so there is no {@code Y/Y} horse at all.
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
        // EDNRB: three combinations, three outcomes - the carrier and the
        // homozygous lethal white are not the same horse
        assertEquals(List.of("NN", "ON", "OO"), tokens(GenotypeCatalog.distinctPairsOf(Genes.EDNRB)));
        // three alleles, six combinations. Five outcomes, but "wild" and the
        // pearl carrier are both wild types and look the same, so four pens.
        assertEquals(List.of("NN", "prlprl", "CrN", "CrCr"),
                tokens(GenotypeCatalog.distinctPairsOf(Genes.MATP)));
        // pink hair's carrier likewise folds into its wild type
        assertEquals(List.of("PihrPihr", "nn"), tokens(GenotypeCatalog.distinctPairsOf(Genes.PINK_HAIR)));
        // KIT: eight alleles, thirty-two carryable combinations, eight outcomes.
        // This is the reduction doing real work - twenty-four of those
        // combinations look like one of the other eight.
        assertEquals(List.of("NN", "W20N", "W20W20", "SB1N", "SB1W20", "SB1SB1", "W23SB1", "W22N"),
                tokens(GenotypeCatalog.distinctPairsOf(Genes.KIT)));
        // the two splash loci: MITF has four outcomes, PAX3 three
        assertEquals(List.of("NN", "SW5N", "SW5SW5", "SW3SW5"),
                tokens(GenotypeCatalog.distinctPairsOf(Genes.MITF)));
        assertEquals(List.of("NN", "SW4N", "SW2SW2"),
                tokens(GenotypeCatalog.distinctPairsOf(Genes.PAX3)));
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

    /**
     * Size is arithmetic, not a count of a list: the product of every gene's
     * <b>non-masking</b> distinct pairs, plus one entry for each masking
     * combination anywhere in the registry. A gene can contribute to both
     * halves - {@code KIT} has seven ordinary outcomes and one that masks.
     */
    @Test
    void sizeIsTheProductOfTheUnmaskedPairsPlusOnePenPerMaskingCombination() {
        long unmasked = 1L;
        int masking = 0;
        for (Gene gene : Genes.codeOrder()) {
            int plain = 0;
            for (AllelePair pair : GenotypeCatalog.distinctPairsOf(gene)) {
                if (gene.expressionOf(pair).masks()) {
                    masking++;
                } else {
                    plain++;
                }
            }
            unmasked *= plain;
        }
        assertEquals(unmasked + masking, GenotypeCatalog.size());
    }

    /**
     * A masking combination gets exactly one pen, and it is the plain wild-type
     * horse plus that one gene.
     *
     * <p><b>Why this samples.</b> The catalogue is millions of entries now, and
     * walking all of them to find the handful that mask would be minutes spent
     * building {@link Genotype}s to throw away. The claim splits in two and
     * both halves are cheap: the arithmetic above already fixes how <i>many</i>
     * masked entries there are, so this checks the <i>shape</i> of each one and
     * then that a wide random sample of ordinary entries masks nothing.
     */
    @Test
    void eachMaskingCombinationContributesExactlyOneWildTypeEntry() {
        int size = GenotypeCatalog.size();
        int maskingCombinations = 0;
        for (Gene gene : Genes.codeOrder()) {
            for (AllelePair pair : GenotypeCatalog.distinctPairsOf(gene)) {
                if (gene.expressionOf(pair).masks()) {
                    maskingCombinations++;
                }
            }
        }
        assertTrue(maskingCombinations > 0, "the model should still have masking combinations to check");

        for (int i = size - maskingCombinations; i < size; i++) {
            Genotype only = GenotypeCatalog.get(i);
            Gene masker = null;
            for (Gene gene : Genes.codeOrder()) {
                if (gene.expressionOf(only.pair(gene)).masks()) {
                    masker = gene;
                    break;
                }
            }
            assertTrue(masker != null, "entry " + i + " should be a masked one: " + only.toCode());
            for (Gene other : Genes.codeOrder()) {
                if (other != masker) {
                    assertTrue(isWildType(other, only),
                            "everything but " + masker.key() + " should be wild type, " + other.key() + " isn't");
                }
            }
        }

        // ...and nothing in the ordinary run of the catalogue masks anything.
        Random rng = new Random(20260903L);
        long plain = size - maskingCombinations;
        for (int n = 0; n < 4000; n++) {
            Genotype g = GenotypeCatalog.get((int) (rng.nextDouble() * plain));
            for (Gene gene : Genes.codeOrder()) {
                assertFalse(gene.expressionOf(g.pair(gene)).masks(),
                        "an unmasked entry carries a masking combination: " + g.toCode());
            }
        }
    }

    /**
     * Every entry is a different genotype and every one round-trips through its
     * code string. Sampled - see
     * {@link #eachMaskingCombinationContributesExactlyOneWildTypeEntry()} for
     * why the whole catalogue is no longer walked - but sampled across the
     * <i>whole</i> range, both ends and the masked tail included.
     */
    @Test
    void everyEntryIsDistinctAndRoundTripsThroughTheCode() {
        Set<String> codes = new HashSet<>();
        for (int i : sampleIndices(6000)) {
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
        for (int i : sampleIndices(6000)) {
            String label = GeneCodeDisplay.shortForm(GenotypeCatalog.get(i));
            assertTrue(labels.add(label), "duplicate label: " + label);
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
        for (int i : sampleIndices(6000)) {
            Genotype g = GenotypeCatalog.get(i);
            List<String> lines = GeneCodeDisplay.wrap(g, 3, 15);
            assertTrue(lines.size() <= 3, "too many lines for " + GeneCodeDisplay.shortForm(g) + ": " + lines);
            for (int k = 0; k < lines.size() - 1; k++) {
                assertTrue(lines.get(k).length() <= 15, "line too wide: '" + lines.get(k) + "'");
            }
            widestLast = Math.max(widestLast, lines.get(lines.size() - 1).length());
            assertEquals(GeneCodeDisplay.shortForm(g), String.join(" ", lines));
        }
        assertTrue(widestLast <= 200, "the overflowing last sign line has grown to " + widestLast + " chars");
    }

    /**
     * {@code n} catalogue indices: the first and last few hundred - where the
     * odometer's low digits and the masked tail live - plus a seeded random
     * spread over everything in between.
     */
    private static int[] sampleIndices(int n) {
        int size = GenotypeCatalog.size();
        Set<Integer> picked = new LinkedHashSet<>();
        int ends = Math.min(size / 2, n / 6);
        for (int i = 0; i < ends; i++) {
            picked.add(i);
            picked.add(size - 1 - i);
        }
        Random rng = new Random(4242L);
        while (picked.size() < Math.min(n, size)) {
            picked.add((int) (rng.nextDouble() * size));
        }
        int[] out = new int[picked.size()];
        int k = 0;
        for (int i : picked) {
            out[k++] = i;
        }
        return out;
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
