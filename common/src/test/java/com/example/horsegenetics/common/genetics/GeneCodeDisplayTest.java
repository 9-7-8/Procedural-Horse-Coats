package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.genetics.genes.AbstractPartColourGene;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneCodeDisplayTest {

    /** {@code shortForm} of the wild type with the named genes overridden. */
    private static String sf(String... geneThenPair) {
        return GeneCodeDisplay.shortForm(Genotype.parse(Codes.of(geneThenPair)));
    }

    @Test
    void extensionAndAgoutiAlwaysLeadAndRunTogether() {
        assertEquals("EEaa", GeneCodeDisplay.shortForm(Genotype.wildType()));
        assertEquals("Eeaa", sf("extension", "E/e"));
        assertEquals("eeAa", sf("extension", "e/e", "agouti", "A/a"));
    }

    @Test
    void onlyGenesWithAVariantAlleleAreListed_spaceSeparated() {
        assertEquals("EeAa nSW1", sf("extension", "E/e", "agouti", "A/a", "mitf", "SW1/N"));
    }

    @Test
    void absenceGenesUseLowercaseNAgainstTheBaselineAlleleAndPrintBothOtherwise() {
        assertEquals("eeaa CrCr", sf("extension", "e/e", "matp", "Cr/Cr"));
        assertEquals("EEaa nCr", sf("matp", "Cr/N"));
        assertEquals("EEaa prlprl", sf("matp", "prl/prl"));
        assertEquals("EEaa nprl", sf("matp", "N/prl"));
    }

    /**
     * A three-allele locus can hold two <i>different</i> non-baseline alleles,
     * which the {@code n}-for-absent shorthand cannot describe - so both real
     * tokens are printed.
     */
    @Test
    void twoDifferentVariantAllelesPrintBothTokens() {
        assertEquals("EEaa Crprl", sf("matp", "Cr/prl"));
    }

    /**
     * A gene that can <b>mask</b> - and grey, which is special-cased - never
     * uses the {@code n}-for-absent shorthand, because "carries one copy of
     * something that hides the whole coat" is not an absence worth hiding.
     */
    @Test
    void maskingGenesAndGreyPrintBothRealTokensDominantFirst() {
        assertEquals("EEaa W22N", sf("kit", "W22/N"));
        assertEquals("EEaa ON", sf("ednrb", "O/N"));
        assertEquals("EEaa G3N", sf("grey", "G3/N"));
    }

    /**
     * The two splash loci are separate genes, so a horse carrying one copy at
     * each prints both - which is the whole thing a single splash gene could
     * not say.
     */
    @Test
    void theTwoSplashLociPrintSeparately() {
        assertEquals("EEaa nSW1 nSW2", sf("mitf", "SW1/N", "pax3", "SW2/N"));
    }

    @Test
    void everythingAtOnceUsesTheDisplayOrder() {
        // KIT, MITF, champagne, MATP, grey
        assertEquals("EeAa W22N nSW1 nCh nCr G3N",
                sf("extension", "E/e", "agouti", "A/a", "kit", "W22/N",
                   "champagne", "Ch/c", "mitf", "SW1/N", "grey", "G3/N", "matp", "Cr/N"));
    }

    @Test
    void theExampleFromTheDocComment() {
        assertEquals("EeAa nSW1 nCh CrCr",
                sf("extension", "E/e", "agouti", "A/a", "champagne", "Ch/c", "mitf", "SW1/N", "matp", "Cr/Cr"));
    }

    /**
     * A part's colour shows on nothing without the part, so the short form leaves it
     * off (owner, 2026-10-05: the pen sign had outgrown its line). The full code still
     * holds it.
     */
    @Test
    void aPartColourIsListedOnlyOnAHorseThatGrowsThePart() {
        assertEquals("EEaa", sf("dorsal_spine_colour", "Red/Red"));
        String spined = sf("dorsal_spines", "Dsp/n", "dorsal_spine_colour", "Red/Red");
        assertTrue(spined.contains("Dsp") && spined.contains("Red"), spined);

        // A recessive part: a carrier grows nothing, so its colour stays off.
        assertFalse(sf("back_sail", "Sail/n", "back_sail_colour", "Blu/Blu").contains("Blu"));
        assertTrue(sf("back_sail", "Sail/Sail", "back_sail_colour", "Blu/Blu").contains("Blu"));

        assertEquals("EEaa", sf("tusk_colour", "Blk/Blk"));
        assertTrue(sf("tusks", "Nar/n", "tusk_colour", "Blk/Blk").contains("Blk"));
    }

    /** Every part-colour locus answers for its own part, never for another's. */
    @Test
    void everyPartColourLocusIsHiddenOnAPartlessHorse() {
        Genotype wild = Genotype.wildType();
        int colours = 0;
        for (Gene gene : Genes.codeOrder()) {
            if (gene instanceof AbstractPartColourGene colour) {
                colours++;
                assertFalse(colour.partGrows(wild), gene.key() + " says a wild horse grows its part");
                for (AllelePair pair : GenotypeCatalog.allPairsOf(gene)) {
                    assertEquals("EEaa", GeneCodeDisplay.shortForm(wild.with(pair)),
                            gene.key() + " " + pair.toTokens() + " showed on a horse with no part");
                }
            }
        }
        assertTrue(colours > 0, "no part-colour loci found - the instanceof test is matching nothing");
    }

    @Test
    void stringOverloadParsesAValidCode() {
        assertEquals("EeAa nSW1",
                GeneCodeDisplay.shortForm(Codes.of("extension", "E/e", "agouti", "A/a", "mitf", "SW1/N")));
    }

    @Test
    void stringOverloadDegradesGracefullyOnAnUnparseableCode() {
        // a legacy positional string (no gene keys) - must not blow up, must not
        // show slash/dash soup
        assertEquals("EE aa ww tt cc", GeneCodeDisplay.shortForm("E/E-a/a-w/w-t/t-c/c"));
        assertEquals("E/e aa", GeneCodeDisplay.shortForm("E/e-a/a")); // het segments keep the slash
    }
}
