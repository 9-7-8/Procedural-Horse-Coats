package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.testutil.Codes;
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

    /** An epigenome with the given (priority, seed) on both copies of agouti and junk elsewhere. */
    private static Epigenome withAgouti(AlleleEpigenetics first, AlleleEpigenetics second) {
        Map<String, Epigenome.Copies> m = new LinkedHashMap<>();
        int p = 1;
        for (Gene g : Genes.codeOrder()) {
            m.put(g.key(), new Epigenome.Copies(
                    new AlleleEpigenetics(p++, 111L), new AlleleEpigenetics(p++, 222L)));
        }
        m.put(Genes.AGOUTI.key(), new Epigenome.Copies(first, second));
        return Epigenome.of(m);
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
            Epigenome.Copies c = e.copies(g);
            assertNotEquals(c.first().priority(), c.second().priority(), g.key());
            assertTrue(c.first().priority() >= AlleleEpigenetics.MIN_PRIORITY);
        }
    }

    @Test
    void aHeterozygoteExpressesTheDominantCopy() {
        // pair is canonicalized A/a, so slot one is the A - regardless of priority
        Epigenome e = withAgouti(new AlleleEpigenetics(5, 700L), new AlleleEpigenetics(900, 800L));
        assertEquals(700L, e.expressedSeed(Genes.AGOUTI, HET_AGOUTI));
    }

    @Test
    void aHomozygoteBreaksTheTieOnPriorityHighestWins() {
        assertEquals(800L, withAgouti(new AlleleEpigenetics(5, 700L), new AlleleEpigenetics(900, 800L))
                .expressedSeed(Genes.AGOUTI, HOM_AGOUTI));
        assertEquals(700L, withAgouti(new AlleleEpigenetics(900, 700L), new AlleleEpigenetics(5, 800L))
                .expressedSeed(Genes.AGOUTI, HOM_AGOUTI));
    }

    @Test
    void theFingerprintOnlyMovesForEpigeneticsThatCanBeSeen() {
        Epigenome base = withAgouti(new AlleleEpigenetics(5, 700L), new AlleleEpigenetics(900, 800L));
        // the unexpressed 'a' copy on a heterozygous bay changes nothing visible
        Epigenome other = withAgouti(new AlleleEpigenetics(5, 700L), new AlleleEpigenetics(900, 12345L));
        assertEquals(base.visibleFingerprint(HET_AGOUTI), other.visibleFingerprint(HET_AGOUTI));

        // ...but the expressed 'A' copy does
        Epigenome moved = withAgouti(new AlleleEpigenetics(5, 999L), new AlleleEpigenetics(900, 800L));
        assertNotEquals(base.visibleFingerprint(HET_AGOUTI), moved.visibleFingerprint(HET_AGOUTI));
    }
}
