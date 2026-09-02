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
        assertEquals("Eeaa", shortForm("E/e-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n"));
        assertEquals("eeAa", shortForm("e/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n"));
    }

    @Test
    void onlyGenesWithAVariantAlleleAreListed_spaceSeparated() {
        assertEquals("EeAa nSpl", shortForm("E/e-A/a-w/w-t/t-c/c-Spl/spl-g/g-N/N-N/N-n/n-n/n"));
    }

    @Test
    void absenceGenesUseLowercaseNWhenHeterozygousAndTheTokenDoubledWhenHomozygous() {
        assertEquals("eeaa CrCr", shortForm("e/e-a/a-w/w-t/t-c/c-spl/spl-g/g-Cr/Cr-N/N-n/n-n/n"));
        assertEquals("EEaa nCr", shortForm("E/E-a/a-w/w-t/t-c/c-spl/spl-g/g-Cr/N-N/N-n/n-n/n"));
        assertEquals("EEaa prlprl", shortForm("E/E-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-prl/prl-n/n-n/n"));
        assertEquals("EEaa nprl", shortForm("E/E-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/prl-n/n-n/n"));
    }

    @Test
    void whiteGreyTestPrintBothRealTokensDominantFirst() {
        assertEquals("EEaa Ww", shortForm("E/E-a/a-W/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n"));
        assertEquals("EEaa Gg", shortForm("E/E-a/a-w/w-t/t-c/c-spl/spl-G/g-N/N-N/N-n/n-n/n"));
        assertEquals("EEaa Tt", shortForm("E/E-a/a-w/w-T/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n"));
    }

    @Test
    void everythingAtOnceUsesTheDisplayOrder() {
        // splash, white, champagne, cream, pearl, grey, test
        assertEquals("EeAa nSpl Ww nCh nCr nprl Gg Tt",
                shortForm("E/e-A/a-W/w-T/t-Ch/c-Spl/spl-G/g-Cr/N-N/prl-n/n-n/n"));
    }

    @Test
    void theExampleFromTheDocComment() {
        assertEquals("EeAa nSpl nCh CrCr",
                shortForm("E/e-A/a-w/w-t/t-Ch/c-Spl/spl-g/g-Cr/Cr-N/N-n/n-n/n"));
    }

    @Test
    void stringOverloadParsesAValidCode() {
        assertEquals("EeAa nSpl", GeneCodeDisplay.shortForm("E/e-A/a-w/w-t/t-c/c-Spl/spl-g/g-N/N-N/N-n/n-n/n"));
    }

    @Test
    void stringOverloadDegradesGracefullyOnAnUnparseableCode() {
        // an older, shorter gene set - must not blow up, must not show slash/dash soup
        assertEquals("EE aa ww tt cc", GeneCodeDisplay.shortForm("E/E-a/a-w/w-t/t-c/c"));
        assertEquals("E/e aa", GeneCodeDisplay.shortForm("E/e-a/a")); // het segments keep the slash
    }
}
