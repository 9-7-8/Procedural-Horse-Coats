package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenomeTest {

    private static final int GENES = Genes.codeOrder().size();
    /** Wild-type segments for the visual-pattern genes added after the first 11. */
    private static final String T = "-d/d-z/z-mu/mu-rn/rn-to/to-ov/ov-sb1/sb1";

    /** Every copy gets {@code priority} and {@code seed}; agouti is then overridden. */
    private static Epigenome flat(int priority, long seed, Epigenome.Copies agouti) {
        Map<String, Epigenome.Copies> m = new LinkedHashMap<>();
        for (Gene g : Genes.codeOrder()) {
            m.put(g.key(), new Epigenome.Copies(
                    new AlleleEpigenetics(priority, seed), new AlleleEpigenetics(priority + 1, seed + 1)));
        }
        m.put(Genes.AGOUTI.key(), agouti);
        return Epigenome.of(m);
    }

    private static Genome genome(String code, Epigenome.Copies agouti, int priority, long seed) {
        return new Genome(Genotype.parse(code), flat(priority, seed, agouti));
    }

    /** {@code n} booleans, all {@code v}. */
    private static boolean[] repeat(boolean v, int n) {
        boolean[] b = new boolean[n];
        java.util.Arrays.fill(b, v);
        return b;
    }

    @Test
    void anInheritedAlleleBringsItsOwnEpigeneticsAlongUnchanged() {
        // dam A/a: slot one is the A, carrying seed 700
        Genome dam = genome("E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T,
                new Epigenome.Copies(new AlleleEpigenetics(10, 700L), new AlleleEpigenetics(20, 701L)), 100, 5000L);
        Genome sire = genome("E/e-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T,
                new Epigenome.Copies(new AlleleEpigenetics(30, 900L), new AlleleEpigenetics(40, 901L)), 300, 9000L);

        // always take each parent's first slot -> foal is A/a with the dam's A
        Genome foal = dam.breedWith(sire, new FakeRng().booleans(repeat(true, GENES * 2)));

        assertEquals("A/a", segment(foal, Genes.AGOUTI));
        Epigenome.Copies c = foal.epigenome().copies(Genes.AGOUTI);
        assertEquals(700L, c.first().epigeneticSeed(), "the A copy kept the dam's seed");
        assertEquals(10, c.first().priority(), "...and her priority");
        assertEquals(900L, c.second().epigeneticSeed(), "the a copy kept the sire's seed");
        assertEquals(30, c.second().priority());
    }

    @Test
    void epigeneticsFollowTheirAlleleWhenThePairIsReordered() {
        // dam a/a (only 'a' to give), sire A/a with the A in slot one
        Genome dam = genome("E/e-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T,
                new Epigenome.Copies(new AlleleEpigenetics(10, 700L), new AlleleEpigenetics(20, 701L)), 100, 5000L);
        Genome sire = genome("E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T,
                new Epigenome.Copies(new AlleleEpigenetics(30, 900L), new AlleleEpigenetics(40, 901L)), 300, 9000L);

        Genome foal = dam.breedWith(sire, new FakeRng().booleans(repeat(true, GENES * 2)));

        // the sire's A was drawn second but sorts first - its epigenetics must follow it
        assertEquals("A/a", segment(foal, Genes.AGOUTI));
        Epigenome.Copies c = foal.epigenome().copies(Genes.AGOUTI);
        assertEquals(900L, c.first().epigeneticSeed());
        assertEquals(700L, c.second().epigeneticSeed());
    }

    @Test
    void aTiedPriorityIsBumpedOneStepSoTheTwoCopiesNeverMatch() {
        Genome dam = genome("E/e-A/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T,
                new Epigenome.Copies(new AlleleEpigenetics(50, 700L), new AlleleEpigenetics(51, 701L)), 100, 5000L);
        Genome sire = genome("E/e-a/a-w/w-t/t-c/c-spl/spl-g/g-N/N-N/N-n/n-n/n" + T,
                new Epigenome.Copies(new AlleleEpigenetics(50, 900L), new AlleleEpigenetics(52, 901L)), 100, 5000L);

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
        AlleleEpigenetics a = new AlleleEpigenetics(50, 700L);
        AlleleEpigenetics tied = new AlleleEpigenetics(50, 900L);
        AlleleEpigenetics clear = new AlleleEpigenetics(51, 900L);

        assertEquals(51, AlleleEpigenetics.deconflict(a, tied, new FakeRng().booleans(true)).priority());
        assertEquals(49, AlleleEpigenetics.deconflict(a, tied, new FakeRng().booleans(false)).priority());
        // the seed is never touched, and an untied pair draws nothing at all
        assertEquals(900L, AlleleEpigenetics.deconflict(a, tied, new FakeRng().booleans(true)).epigeneticSeed());
        assertEquals(clear, AlleleEpigenetics.deconflict(a, clear, new FakeRng()));
    }

    @Test
    void aBumpNeverPushesPriorityOutOfRange() {
        assertEquals(2, new AlleleEpigenetics(AlleleEpigenetics.MIN_PRIORITY, 0L).bumped(false).priority());
        assertEquals(AlleleEpigenetics.MAX_PRIORITY - 1,
                new AlleleEpigenetics(AlleleEpigenetics.MAX_PRIORITY, 0L).bumped(true).priority());
    }

    @Test
    void theGenotypeHalfBreedsExactlyLikeGenotypeBreedWith() {
        Genome dam = Genome.random(new SeededRng(11L));
        Genome sire = Genome.random(new SeededRng(12L));
        Genotype expected = dam.genotype().breedWith(sire.genotype(), new FakeRng().booleans(repeat(false, GENES * 2)));
        Genome foal = dam.breedWith(sire, new FakeRng().booleans(repeat(false, GENES * 2)));
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
