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
        assertEquals("EeAa nSpl", sf("extension", "E/e", "agouti", "A/a", "splash", "Spl/spl"));
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

    @Test
    void whiteGreyTestPrintBothRealTokensDominantFirst() {
        assertEquals("EEaa Ww", sf("white", "W/w"));
        assertEquals("EEaa Gg", sf("grey", "G/g"));
        assertEquals("EEaa Tt", sf("test", "T/t"));
    }

    @Test
    void everythingAtOnceUsesTheDisplayOrder() {
        // splash, white, champagne, MATP, grey, test
        assertEquals("EeAa nSpl Ww nCh nCr Gg Tt",
                sf("extension", "E/e", "agouti", "A/a", "white", "W/w", "test", "T/t",
                   "champagne", "Ch/c", "splash", "Spl/spl", "grey", "G/g", "matp", "Cr/N"));
    }

    @Test
    void theExampleFromTheDocComment() {
        assertEquals("EeAa nSpl nCh CrCr",
                sf("extension", "E/e", "agouti", "A/a", "champagne", "Ch/c", "splash", "Spl/spl", "matp", "Cr/Cr"));
    }

    @Test
    void stringOverloadParsesAValidCode() {
        assertEquals("EeAa nSpl",
                GeneCodeDisplay.shortForm(Codes.of("extension", "E/e", "agouti", "A/a", "splash", "Spl/spl")));
    }

    @Test
    void stringOverloadDegradesGracefullyOnAnUnparseableCode() {
        // a legacy positional string (no gene keys) - must not blow up, must not
        // show slash/dash soup
        assertEquals("EE aa ww tt cc", GeneCodeDisplay.shortForm("E/E-a/a-w/w-t/t-c/c"));
        assertEquals("E/e aa", GeneCodeDisplay.shortForm("E/e-a/a")); // het segments keep the slash
    }
}
