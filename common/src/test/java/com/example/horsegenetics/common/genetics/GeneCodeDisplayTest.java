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
    void absenceGenesUseLowercaseNWhenHeterozygousAndTheTokenDoubledWhenHomozygous() {
        assertEquals("eeaa CrCr", sf("extension", "e/e", "cream", "Cr/Cr"));
        assertEquals("EEaa nCr", sf("cream", "Cr/N"));
        assertEquals("EEaa prlprl", sf("pearl", "prl/prl"));
        assertEquals("EEaa nprl", sf("pearl", "N/prl"));
    }

    @Test
    void whiteGreyTestPrintBothRealTokensDominantFirst() {
        assertEquals("EEaa Ww", sf("white", "W/w"));
        assertEquals("EEaa Gg", sf("grey", "G/g"));
        assertEquals("EEaa Tt", sf("test", "T/t"));
    }

    @Test
    void everythingAtOnceUsesTheDisplayOrder() {
        // splash, white, champagne, cream, pearl, grey, test
        assertEquals("EeAa nSpl Ww nCh nCr nprl Gg Tt",
                sf("extension", "E/e", "agouti", "A/a", "white", "W/w", "test", "T/t",
                   "champagne", "Ch/c", "splash", "Spl/spl", "grey", "G/g", "cream", "Cr/N", "pearl", "N/prl"));
    }

    @Test
    void theExampleFromTheDocComment() {
        assertEquals("EeAa nSpl nCh CrCr",
                sf("extension", "E/e", "agouti", "A/a", "champagne", "Ch/c", "splash", "Spl/spl", "cream", "Cr/Cr"));
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
