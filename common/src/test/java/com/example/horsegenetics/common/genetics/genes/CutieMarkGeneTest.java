package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cutie-mark locus: recessive, magical, paints nothing in the pipeline (a
 * client layer draws the emblem), and everything about the emblem is drawn off
 * the expressing copy's epigenetic seed.
 */
class CutieMarkGeneTest {

    private static final CutieMarkGene GENE = Genes.CUTIE_MARK;

    private static AllelePair pair(String tokens) {
        return Genotype.parse(Codes.of("cutie_mark", tokens)).pair(GENE);
    }

    @Test
    void onlyTwoCopiesShow() {
        assertTrue(GENE.expressionOf(pair("n/n")).wildType());
        Expression carrier = GENE.expressionOf(pair("Cutmrk/n"));
        assertTrue(carrier.wildType());
        assertEquals("cutie-mark-carrier", carrier.id());
        Expression mark = GENE.expressionOf(pair("Cutmrk/Cutmrk"));
        assertEquals("cutie-mark", mark.id());
        assertTrue(mark.wildType(), "it paints nothing in the pipeline - a client layer draws it");
        assertTrue(GENE.shows(pair("Cutmrk/Cutmrk")));
        assertFalse(GENE.shows(pair("Cutmrk/n")));
    }

    @Test
    void itIsMagicalAndPaintsNothing() {
        assertFalse(GENE.isNatural());
        assertFalse(GENE.affectsCoat(), "all three outcomes are wild types");
        assertTrue(Genes.magicalOrder().contains(GENE));
        assertFalse(Genes.naturalOrder().contains(GENE));
    }

    @Test
    void markForIsEmptyUnlessHomozygous() {
        Genotype carrier = Genotype.parse(Codes.of("cutie_mark", "Cutmrk/n"));
        assertTrue(GENE.markFor(carrier, Epigenome.fromSeed(1)).isEmpty());
        assertTrue(GENE.markFor(Genotype.wildType(), Epigenome.fromSeed(1)).isEmpty());
    }

    @Test
    void markForIsDeterministicHeritableAndWellFormed() {
        Genotype gt = Genotype.parse(Codes.of("cutie_mark", "Cutmrk/Cutmrk"));
        for (long seed : new long[]{0L, 1L, 7L, 42L, 999L, 123456L}) {
            Epigenome epi = Epigenome.fromSeed(seed);
            Optional<CutieMarkGene.Mark> a = GENE.markFor(gt, epi);
            Optional<CutieMarkGene.Mark> b = GENE.markFor(gt, Epigenome.fromSeed(seed));
            assertTrue(a.isPresent());
            CutieMarkGene.Mark m = a.get();
            assertEquals(m.count(), b.get().count(), "same seed -> same emblem");
            assertTrue(m.count() >= 1 && m.count() <= 3, "count " + m.count());
            assertEquals(3, m.picks().length);
            for (double p : m.picks()) {
                assertTrue(p >= 0.0 && p < 1.0, "pick out of [0,1): " + p);
            }
            assertFalse(m.triangle() && m.count() != 3, "triangle only with three items");
            assertTrue(m.scale() > 0.0);
        }
    }

    @Test
    void aHomozygoteCanTurnUpInTheWildButRarely() {
        SeededRng rng = new SeededRng(31337L);
        int hom = 0;
        int carriers = 0;
        int n = 50_000;
        for (int i = 0; i < n; i++) {
            int copies = Genotype.random(rng).pair(GENE).count(GENE.Cutmrk);
            if (copies == 2) {
                hom++;
            } else if (copies == 1) {
                carriers++;
            }
        }
        // p = 0.06 -> ~0.36% homozygous, ~11% carriers
        assertTrue(hom > 60 && hom < 350, "homozygote rate off: " + hom + " / " + n);
        assertTrue(carriers > 4000 && carriers < 7500, "carrier rate off: " + carriers + " / " + n);
    }
}
