package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.testutil.Codes;
import com.example.horsegenetics.common.genetics.BayShade;
import com.example.horsegenetics.common.testutil.Epi;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpigenomeTest {

    private static final Genotype HET_AGOUTI = Genotype.parse(Codes.of("extension", "E/e", "agouti", "A/a"));
    private static final Genotype HOM_AGOUTI = Genotype.parse(Codes.of("agouti", "A/A"));

    /**
     * An epigenome with the given agouti copies and midpoints everywhere else.
     * Agouti copies are told apart by their stored {@link BayShade#SHADE}, which
     * stands in for "this copy's numbers" the way a distinct seed used to.
     */
    private static Epigenome withAgouti(AlleleEpigenetics first, AlleleEpigenetics second) {
        Map<String, Epigenome.Copies> m = new LinkedHashMap<>();
        int p = 1;
        for (Gene g : Genes.codeOrder()) {
            if (g.epiSchema().isEmpty()) {
                continue;
            }
            m.put(g.key(), new Epigenome.Copies(
                    new AlleleEpigenetics(p++, g.epiSchema().midpoint()),
                    new AlleleEpigenetics(p++, g.epiSchema().midpoint())));
        }
        m.put(Genes.AGOUTI.key(), new Epigenome.Copies(first, second));
        return Epigenome.of(m);
    }

    /** One agouti copy at {@code priority}, carrying {@code shade}. */
    private static AlleleEpigenetics copy(int priority, double shade) {
        return new AlleleEpigenetics(priority,
                Epi.of(Genes.AGOUTI.epiSchema(), BayShade.SHADE, shade));
    }

    /** The shade the horse actually shows at agouti. */
    private static double shadeOf(Epigenome e, Genotype gt) {
        return e.expressedValues(Genes.AGOUTI, gt).get(BayShade.SHADE);
    }

    @Test
    void roundTripsThroughItsCode() {
        Epigenome e = Epigenome.fromSeed(4242L);
        assertEquals(e, Epigenome.parse(e.toCode()));
    }

    @Test
    void aCodeWithTheWrongNumberOfSegmentsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Epigenome.parse("1:a/2:b"));
    }

    @Test
    void everyGeneGetsTwoDistinctPrioritiesWhenRolledFresh() {
        Epigenome e = Epigenome.random(new SeededRng(9L));
        for (Gene g : Genes.codeOrder()) {
            if (g.epiSchema().isEmpty()) {
                continue;   // stores nothing, so it has no copies to deconflict
            }
            Epigenome.Copies c = e.copies(g);
            assertNotEquals(c.first().priority(), c.second().priority(), g.key());
            assertTrue(c.first().priority() >= AlleleEpigenetics.MIN_PRIORITY);
        }
    }

    @Test
    void aHeterozygoteExpressesTheDominantCopy() {
        // pair is canonicalized A/a, so slot one is the A - regardless of priority
        Epigenome e = withAgouti(copy(5, 0.11), copy(900, 0.21));
        assertEquals(0.11, shadeOf(e, HET_AGOUTI), 1e-9);
    }

    @Test
    void aHomozygoteBreaksTheTieOnPriorityHighestWins() {
        assertEquals(0.21, shadeOf(withAgouti(copy(5, 0.11), copy(900, 0.21)), HOM_AGOUTI), 1e-9);
        assertEquals(0.11, shadeOf(withAgouti(copy(900, 0.11), copy(5, 0.21)), HOM_AGOUTI), 1e-9);
    }

    @Test
    void theFingerprintOnlyMovesForEpigeneticsThatCanBeSeen() {
        Epigenome base = withAgouti(copy(5, 0.11), copy(900, 0.21));
        // the unexpressed 'a' copy on a heterozygous bay changes nothing visible
        Epigenome other = withAgouti(copy(5, 0.11), copy(900, 0.29));
        assertEquals(base.visibleFingerprint(HET_AGOUTI), other.visibleFingerprint(HET_AGOUTI));

        // ...but the expressed 'A' copy does
        Epigenome moved = withAgouti(copy(5, 0.19), copy(900, 0.21));
        assertNotEquals(base.visibleFingerprint(HET_AGOUTI), moved.visibleFingerprint(HET_AGOUTI));
    }
}
