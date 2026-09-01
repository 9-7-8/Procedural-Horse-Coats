package com.example.horsegenetics.common.genetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneCodeDisplayTest {

    private static String shortForm(String code) {
        return GeneCodeDisplay.shortForm(Genotype.parse(code));
    }

    @Test
    void extensionAndAgoutiAlwaysLeadAndRunTogether() {
        assertEquals("EEaa", GeneCodeDisplay.shortForm(Genotype.wildType()));
        assertEquals("Eeaa", shortForm("E/e-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N"));
        assertEquals("eeAa", shortForm("e/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N"));
    }

    @Test
    void onlyGenesWithAVariantAlleleAreListed_spaceSeparated() {
        assertEquals("EeAa nSpl", shortForm("E/e-A/a-w/w-t/t-c/c-Spl/spl-g/g-N/N-N/N"));
    }

    @Test
    void absenceGenesUseLowercaseNWhenHeterozygousAndTheTokenDoubledWhenHomozygous() {
        assertEquals("eeaa CrCr", shortForm("e/e-a/a-w/w-t/t-c/c-spl/spl-g/g-Cr/Cr-N/N"));
        assertEquals("EEaa nCr", shortForm("E/E-a/a-w/w-t/t-c/c-spl/spl-g/g-Cr/N-N/N"));
        assertEquals("EEaa prlprl", shortForm("E/E-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-prl/prl"));
        assertEquals("EEaa nprl", shortForm("E/E-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/prl"));
    }

    @Test
    void whiteGreyTestPrintBothRealTokensDominantFirst() {
        assertEquals("EEaa Ww", shortForm("E/E-a/a-W/w-t/t-c/c-spl/spl-g/g-N/N-N/N"));
        assertEquals("EEaa Gg", shortForm("E/E-a/a-w/w-t/t-c/c-spl/spl-G/g-N/N-N/N"));
        assertEquals("EEaa Tt", shortForm("E/E-a/a-w/w-T/t-c/c-spl/spl-g/g-N/N-N/N"));
    }

    @Test
    void everythingAtOnceUsesTheDisplayOrder() {
        // splash, white, champagne, cream, pearl, grey, test
        assertEquals("EeAa nSpl Ww nCh nCr nprl Gg Tt",
                shortForm("E/e-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/prl"));
    }

    @Test
    void theExampleFromTheDocComment() {
        assertEquals("EeAa nSpl nCh CrCr",
                shortForm("E/e-A/a-w/w-t/t-Ch/c-Spl/spl-g/g-Cr/Cr-N/N"));
    }

    @Test
    void stringOverloadParsesAValidCode() {
        assertEquals("EeAa nSpl", GeneCodeDisplay.shortForm("E/e-A/a-w/w-t/t-c/c-Spl/spl-g/g-N/N-N/N"));
    }

    @Test
    void stringOverloadDegradesGracefullyOnAnUnparseableCode() {
        // an older, shorter gene set - must not blow up, must not show slash/dash soup
        assertEquals("EE aa ww tt cc", GeneCodeDisplay.shortForm("E/E-a/a-w/w-t/t-c/c"));
        assertEquals("E/e aa", GeneCodeDisplay.shortForm("E/e-a/a")); // het segments keep the slash
    }
}
