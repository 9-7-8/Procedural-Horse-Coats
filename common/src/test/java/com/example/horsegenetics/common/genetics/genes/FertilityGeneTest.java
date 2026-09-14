package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fertility locus: what each combination does to the odds, that it paints
 * nothing, that wild horses only ever carry it, and that its number sits where
 * the design says.
 */
class FertilityGeneTest {

    private static final FertilityGene F = Genes.FERTILITY;

    private static AllelePair pair(Allele a, Allele b) {
        return new AllelePair(a, b);
    }

    @Test
    void onlyTwoSubfertileCopiesChangeTheAlleleFactor() {
        assertEquals(1.0, F.alleleFactor(pair(F.n, F.n)));
        assertEquals(1.0, F.alleleFactor(pair(F.sf, F.n)), "one sf copy is silent");
        assertEquals(1.0, F.alleleFactor(pair(F.sf, F.tw)));
        assertEquals(FertilityGene.SUBFERTILE_FACTOR, F.alleleFactor(pair(F.sf, F.sf)));
    }

    @Test
    void twinChanceRisesWithEachTwinningCopy() {
        double none = F.twinChance(pair(F.n, F.n));
        double one = F.twinChance(pair(F.tw, F.n));
        double two = F.twinChance(pair(F.tw, F.tw));
        assertTrue(none < one && one < two, none + " < " + one + " < " + two);
        assertEquals(one, F.twinChance(pair(F.tw, F.sf)), "an sf carrier does not change twinning");
    }

    @Test
    void expressionsNameEachOutcome() {
        assertEquals("wild", F.expressionOf(pair(F.n, F.n)).id());
        assertEquals("wild", F.expressionOf(pair(F.sf, F.n)).id());
        assertEquals("subfertile", F.expressionOf(pair(F.sf, F.sf)).id());
        assertEquals("twin-prone", F.expressionOf(pair(F.tw, F.n)).id());
        assertEquals("twinning", F.expressionOf(pair(F.tw, F.tw)).id());
    }

    @Test
    void itPaintsNothingAndIsFiledWithTheOtherNaturals() {
        assertFalse(F.affectsCoat());
        assertTrue(F.isNatural());
        assertEquals(GeneFamily.NATURAL_OTHER, GeneFamily.of(F));
        Genotype sf = Genotype.wildType().with(pair(F.sf, F.sf));
        assertEquals(Genotype.wildType().coatCode(), sf.coatCode());
    }

    /** Wild horses carry sf, never express it. */
    @Test
    void aFounderIsNeverSubfertile() {
        boolean sawCarrier = false;
        boolean sawTwinning = false;
        for (long seed = 0; seed < 4000; seed++) {
            AllelePair p = Genotype.random(new SeededRng(seed)).pair(F);
            assertFalse(F.subfertile(p), "a founder is sf/sf at seed " + seed);
            sawCarrier |= p.has(F.sf);
            sawTwinning |= p.has(F.tw);
        }
        assertTrue(sawCarrier, "4000 founders should turn up an sf carrier");
        assertTrue(sawTwinning, "4000 founders should turn up a tw carrier");
    }

    @Test
    void theFertilityNumberStaysInItsDesignSpan() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (long seed = 0; seed < 2000; seed++) {
            double v = F.fertility(Genome.random(new SeededRng(seed)));
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        assertTrue(min >= 0.7 && max <= 1.3, "fertility spread " + min + ".." + max);
        assertTrue(max - min > 0.1, "founders should actually differ, spread " + min + ".." + max);
    }
}
