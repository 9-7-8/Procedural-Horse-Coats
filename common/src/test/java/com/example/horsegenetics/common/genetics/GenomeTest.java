package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.testutil.FakeRng;
import com.example.horsegenetics.common.testutil.LegacyCode;
import com.example.horsegenetics.common.genetics.BayShade;
import com.example.horsegenetics.common.genetics.epi.EpiDrift;
import com.example.horsegenetics.common.testutil.Epi;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenomeTest {

    private static final int GENES = Genes.codeOrder().size();
    /** Wild-type segments for the visual-pattern genes added after the first 11. */
    private static final String T = "-d2/d2-z/z-mu/mu-rn/rn-to/to-N/N-N/N";

    /** Every storing gene gets midpoint values at {@code priority}; agouti is then overridden. */
    private static Epigenome flat(int priority, Epigenome.Copies agouti) {
        Map<String, Epigenome.Copies> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            if (g.epiSchema().isEmpty()) {
                continue;
            }
            m.put(g.key(), new Epigenome.Copies(
                    new AlleleEpigenetics(priority, g.epiSchema().midpoint()),
                    new AlleleEpigenetics(priority + 1, g.epiSchema().midpoint())));
        }
        m.put(Genes.AGOUTI.key(), agouti);
        return Epigenome.of(m);
    }

    private static Genome genome(String code, Epigenome.Copies agouti, int priority) {
        return new Genome(Genotype.parse(code), flat(priority, agouti));
    }

    /**
     * Two agouti copies, told apart by their stored {@link BayShade#SHADE} - the
     * shade offset a bay carries. It stands in for "this copy's numbers" the way
     * a distinct epigenetic seed used to.
     */
    private static Epigenome.Copies agouti(int p1, double shade1, int p2, double shade2) {
        return new Epigenome.Copies(copy(p1, shade1), copy(p2, shade2));
    }

    private static AlleleEpigenetics copy(int priority, double shade) {
        return new AlleleEpigenetics(priority,
                Epi.of(Genes.AGOUTI.epiSchema(), BayShade.SHADE, shade));
    }

    /** What {@code copy} wrote, read back off a foal - within one generation's drift. */
    private static void assertShade(double expected, AlleleEpigenetics actual, String why) {
        assertEquals(expected, actual.values().get(BayShade.SHADE), DRIFT, why);
    }

    /**
     * How far one breeding's drift can move a value. {@link EpiDrift} is
     * exponential with a scale of {@value EpiDrift#SCALE} of the design span and
     * a tail bounded by float resolution at about 17x that, so this is
     * comfortably above anything a single generation can produce while still
     * being far tighter than "the copies got swapped".
     */
    private static final double DRIFT = 0.05 * 2 * BayShade.EXPRESSION_RANGE;

    /** {@code n} booleans, all {@code v}. */
    private static boolean[] repeat(boolean v, int n) {
        boolean[] b = new boolean[n];
        java.util.Arrays.fill(b, v);
        return b;
    }

    @Test
    void anInheritedAlleleBringsItsOwnEpigeneticsAlongUnchanged() {
        // dam A/a: slot one is the A, carrying seed 700
        Genome dam = genome(LegacyCode.keyed("E/e-A/a-N/N-t/t-c/c-N/N-N/N-N/N-n/n-n/n" + T),
                agouti(10, 0.11, 20, 0.12), 100);
        Genome sire = genome(LegacyCode.keyed("E/e-a/a-N/N-t/t-c/c-N/N-N/N-N/N-n/n-n/n" + T),
                agouti(30, 0.21, 40, 0.22), 300);

        // always take each parent's first slot -> foal is A/a with the dam's A
        Genome foal = dam.breedWith(sire, new FakeRng().booleans(repeat(true, GENES * 2)).fallingBackTo(new SeededRng(1L)));

        assertEquals("A/a", segment(foal, Genes.AGOUTI));
        Epigenome.Copies c = foal.epigenome().copies(Genes.AGOUTI);
        assertShade(0.11, c.first(), "the A copy kept the dam's numbers");
        assertEquals(10, c.first().priority(), "...and her priority, exactly - priority does not drift");
        assertShade(0.21, c.second(), "the a copy kept the sire's numbers");
        assertEquals(30, c.second().priority());
    }

    @Test
    void epigeneticsFollowTheirAlleleWhenThePairIsReordered() {
        // dam a/a (only 'a' to give), sire A/a with the A in slot one
        Genome dam = genome(LegacyCode.keyed("E/e-a/a-N/N-t/t-c/c-N/N-N/N-N/N-n/n-n/n" + T),
                agouti(10, 0.11, 20, 0.12), 100);
        Genome sire = genome(LegacyCode.keyed("E/e-A/a-N/N-t/t-c/c-N/N-N/N-N/N-n/n-n/n" + T),
                agouti(30, 0.21, 40, 0.22), 300);

        Genome foal = dam.breedWith(sire, new FakeRng().booleans(repeat(true, GENES * 2)).fallingBackTo(new SeededRng(1L)));

        // the sire's A was drawn second but sorts first - its epigenetics must follow it
        assertEquals("A/a", segment(foal, Genes.AGOUTI));
        Epigenome.Copies c = foal.epigenome().copies(Genes.AGOUTI);
        assertShade(0.21, c.first(), "the A copy is the sire's");
        assertShade(0.11, c.second(), "the a copy is the dam's");
    }

    @Test
    void aTiedPriorityIsBumpedOneStepSoTheTwoCopiesNeverMatch() {
        Genome dam = genome(LegacyCode.keyed("E/e-A/a-N/N-t/t-c/c-N/N-N/N-N/N-n/n-n/n" + T),
                agouti(50, 0.11, 51, 0.12), 100);
        Genome sire = genome(LegacyCode.keyed("E/e-a/a-N/N-t/t-c/c-N/N-N/N-N/N-n/n-n/n" + T),
                agouti(50, 0.21, 52, 0.22), 100);

        // Both parents sit on the same priorities, so roughly half the genes draw
        // a matching pair and have to be deconflicted. However they land, no
        // gene may come out of it holding the same priority twice.
        Genome foal = dam.breedWith(sire, new SeededRng(3L));

        for (Gene g : Genes.codeOrder()) {
            Epigenome.Copies c = foal.epigenome().copies(g);
            assertNotEquals(c.first().priority(), c.second().priority(), g.key());
            assertTrue(c.second().priority() >= AlleleEpigenetics.MIN_PRIORITY);
        }
    }

    @Test
    void deconflictMovesTheSecondCopyOneStepAndOnlyOnATie() {
        AlleleEpigenetics a = copy(50, 0.11);
        AlleleEpigenetics tied = copy(50, 0.21);
        AlleleEpigenetics clear = copy(51, 0.21);

        assertEquals(51, AlleleEpigenetics.deconflict(a, tied, new FakeRng().booleans(true)).priority());
        assertEquals(49, AlleleEpigenetics.deconflict(a, tied, new FakeRng().booleans(false)).priority());
        // the seed is never touched, and an untied pair draws nothing at all
        assertShade(0.21, AlleleEpigenetics.deconflict(a, tied, new FakeRng().booleans(true)),
                "deconflict moves the priority and leaves the numbers alone");
        assertEquals(clear, AlleleEpigenetics.deconflict(a, clear, new FakeRng()));
    }

    @Test
    void aBumpNeverPushesPriorityOutOfRange() {
        assertEquals(2, copy(AlleleEpigenetics.MIN_PRIORITY, 0).bumped(false).priority());
        assertEquals(AlleleEpigenetics.MAX_PRIORITY - 1,
                copy(AlleleEpigenetics.MAX_PRIORITY, 0).bumped(true).priority());
    }

    @Test
    void theGenotypeHalfBreedsExactlyLikeGenotypeBreedWith() {
        Genome dam = Genome.random(new SeededRng(11L));
        Genome sire = Genome.random(new SeededRng(12L));
        Genotype expected = dam.genotype().breedWith(sire.genotype(), new FakeRng().booleans(repeat(false, GENES * 2)).fallingBackTo(new SeededRng(2L)));
        Genome foal = dam.breedWith(sire, new FakeRng().booleans(repeat(false, GENES * 2)).fallingBackTo(new SeededRng(2L)));
        assertEquals(expected, foal.genotype());
    }

    @Test
    void aGenomeRoundTripsThroughItsTwoCodes() {
        Genome g = Genome.random(new SeededRng(77L));
        assertEquals(g, Genome.parse(g.genotypeCode(), g.epigenomeCode()));
    }

    private static String segment(Genome g, Gene gene) {
        var p = g.genotype().pair(gene);
        return p.first().token() + "/" + p.second().token();
    }
}
