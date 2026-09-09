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

    // ------------------------------------------------------------------
    // A seeded horse must not move when an unrelated gene is registered
    // ------------------------------------------------------------------

    /**
     * <b>Every gene's seeded epigenetics depend on the seed and that gene's own
     * key - and on nothing else.</b>
     *
     * <p>This is the guard known-gaps gap 47 asked for. {@code fromSeed} used
     * to walk {@link Genes#codeOrder()} drawing from one shared stream, so
     * registering a locus at priority 68 gave every gene above 68 a different
     * draw and the horse at seed 13 became a different horse. Adding natural
     * zebra broke three assertions in {@code WhitePatternGenesTest} that way,
     * all by margins under 0.05, none of them a real regression - and the only
     * signal was a red build after a change that looked unrelated.
     *
     * <p>The test recomputes each gene's copies from {@code (seed, key)} alone
     * and demands the same answer. It does not need to register a gene to prove
     * the point: if anyone puts the shared stream back, a gene's values will
     * stop being reproducible from its own key and this goes red immediately.
     */
    @Test
    void seededEpigeneticsDependOnlyOnTheSeedAndTheGeneKey() {
        for (long seed : new long[]{0L, 1L, 13L, 4242L, -7L}) {
            Epigenome whole = Epigenome.fromSeed(seed);
            for (Gene g : Genes.codeOrder()) {
                if (!Epigenome.carries(g)) {
                    continue;
                }
                // The same gene, drawn on its own, with no other gene in front
                // of it in any stream.
                Epigenome.Copies alone = Epigenome.copiesFor(g, seed);
                assertEquals(whole.copies(g), alone,
                        g.key() + " at seed " + seed + " depends on something other than"
                                + " its own key - the shared-stream bug (gap 47) is back");
            }
        }
    }

    /**
     * The order genes are registered in must not reach a seeded horse at all.
     * A direct statement of the same property: reversing the walk changes
     * nothing, because nothing is carried between genes.
     */
    @Test
    void reversingTheRegistryWalkChangesNoSeededValue() {
        long seed = 99L;
        Epigenome forwards = Epigenome.fromSeed(seed);
        java.util.List<Gene> reversed = new java.util.ArrayList<>(Genes.codeOrder());
        java.util.Collections.reverse(reversed);
        for (Gene g : reversed) {
            if (!Epigenome.carries(g)) {
                continue;
            }
            assertEquals(forwards.copies(g), Epigenome.copiesFor(g, seed),
                    g.key() + " reads differently depending on when it is asked");
        }
    }
}
