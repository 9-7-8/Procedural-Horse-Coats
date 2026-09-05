package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals("EEaa Gg", sf("grey", "G/g"));
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
        assertEquals("EeAa W22N nSW1 nCh nCr Gg",
                sf("extension", "E/e", "agouti", "A/a", "kit", "W22/N",
                   "champagne", "Ch/c", "mitf", "SW1/N", "grey", "G/g", "matp", "Cr/N"));
    }

    @Test
    void theExampleFromTheDocComment() {
        assertEquals("EeAa nSW1 nCh CrCr",
                sf("extension", "E/e", "agouti", "A/a", "champagne", "Ch/c", "mitf", "SW1/N", "matp", "Cr/Cr"));
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
