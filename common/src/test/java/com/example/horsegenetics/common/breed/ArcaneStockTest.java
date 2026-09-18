package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneFamily;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.trait.HealthContribution;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The arcane dealer's stock (owner, 2026-09-18): one showing gene from each
 * magical family, no two combinations alike in the herd, no breed underneath.
 */
class ArcaneStockTest {

    /**
     * The biggest herd the dealer can be given - {@code Cowboy.MAX_HERD} in the
     * NeoForge module, which {@code common/} cannot import. If that constant
     * ever rises above this, this number moves with it and the headroom test
     * below is what tells you.
     */
    private static final int LARGEST_HERD = 10;

    private static Rng rng(String tag) {
        return new SeededRng(20260918L, tag);
    }

    @Test
    void everyRequiredFamilyHasRoomForAWholeHerd() {
        for (GeneFamily family : ArcaneStock.REQUIRED_FAMILIES) {
            int pairs = ArcaneStock.pool(family).size();
            assertTrue(pairs >= LARGEST_HERD,
                    family.name() + " holds only " + pairs + " showing pairs, so a herd of "
                            + LARGEST_HERD + " cannot give every horse a distinct one");
        }
        assertFalse(ArcaneStock.pool(ArcaneStock.OPTIONAL_FAMILY).isEmpty(),
                ArcaneStock.OPTIONAL_FAMILY.name() + " is rolled on a chance, so it still needs stock");
    }

    /**
     * The scarcest family draws first, so the one that runs dry is the one with
     * stock to spare. Derived from the pools rather than written down, so a
     * folder of new mane genes re-sorts it with no edit - this asserts the
     * sorting happened, not a particular answer.
     */
    @Test
    void theScarcestFamilyDrawsFirst() {
        List<GeneFamily> order = ArcaneStock.drawOrder(new LinkedHashSet<>());
        assertEquals(ArcaneStock.REQUIRED_FAMILIES.size(), order.size());
        assertTrue(order.containsAll(ArcaneStock.REQUIRED_FAMILIES), "a family was dropped from the draw order");
        for (int i = 1; i < order.size(); i++) {
            int previous = ArcaneStock.pool(order.get(i - 1)).size();
            int current = ArcaneStock.pool(order.get(i)).size();
            assertTrue(previous <= current,
                    order.get(i - 1).name() + " (" + previous + ") drew before "
                            + order.get(i).name() + " (" + current + ")");
        }
    }

    /** The draw order answers to what is left, not only to what shipped. */
    @Test
    void theDrawOrderFollowsWhatIsStillFree() {
        GeneFamily roomiest = ArcaneStock.drawOrder(new LinkedHashSet<>()).get(ArcaneStock.REQUIRED_FAMILIES.size() - 1);
        Set<String> taken = new LinkedHashSet<>();
        List<AllelePair> pool = ArcaneStock.pool(roomiest);
        for (int i = 0; i < pool.size() - 1; i++) {
            taken.add(ArcaneStock.token(pool.get(i)));
        }
        assertEquals(roomiest, ArcaneStock.drawOrder(taken).get(0),
                "the family stripped down to one free pair should now draw first");
    }

    @Test
    void everyPooledPairActuallyShows() {
        List<GeneFamily> all = new ArrayList<>(ArcaneStock.REQUIRED_FAMILIES);
        all.add(ArcaneStock.OPTIONAL_FAMILY);
        for (GeneFamily family : all) {
            for (AllelePair pair : ArcaneStock.pool(family)) {
                Gene gene = pair.gene();
                assertEquals(family, GeneFamily.of(gene), pair.geneKey() + " is filed under another family");
                assertTrue(MagicalVariant.showingPairs(gene).contains(pair),
                            ArcaneStock.token(pair) + " is a carrier, not a showing pair");
            }
        }
    }

    /** Owner, 2026-09-18: a paddock of ten horses all making a noise is unusable. */
    @Test
    void noNoisyGeneIsEverStocked() {
        for (String key : ArcaneStock.NOISY) {
            assertNotNull(Genes.byKeyOrNull(key),
                    key + " is on the noisy list but is not a registered gene - a typo here fails open,"
                            + " because an unknown key silently excludes nothing");
        }
        List<GeneFamily> all = new ArrayList<>(ArcaneStock.REQUIRED_FAMILIES);
        all.add(ArcaneStock.OPTIONAL_FAMILY);
        for (GeneFamily family : all) {
            for (AllelePair pair : ArcaneStock.pool(family)) {
                assertFalse(ArcaneStock.NOISY.contains(pair.geneKey()),
                        pair.geneKey() + " makes a noise and should not be in his string");
            }
        }
    }

    @Test
    void theExcludedLociAreNeverStocked() {
        for (Gene gene : Genes.magicalOrder()) {
            if (!ArcaneStock.eligible(gene)) {
                continue;
            }
            assertFalse(BreedFounder.BODY_STAT_KEYS.contains(gene.key()),
                    gene.key() + " is owned by the stat scores");
            assertFalse(gene.feralOnly(), gene.key() + " is feral-only");
            assertFalse(gene.inheritance().sexLinked(), gene.key() + " is sex-linked");
            assertFalse(gene instanceof HealthContribution, gene.key() + " is a disorder");
        }
        for (Gene gene : Genes.naturalOrder()) {
            assertFalse(ArcaneStock.eligible(gene), gene.key() + " is natural and he sells magic");
        }
    }

    @Test
    void oneHorseCoversEveryRequiredFamily() {
        List<AllelePair> forced = ArcaneStock.rollHorse(rng("one-horse"), new LinkedHashSet<>());
        Set<GeneFamily> covered = new HashSet<>();
        for (AllelePair pair : forced) {
            covered.add(GeneFamily.of(pair.gene()));
        }
        for (GeneFamily family : ArcaneStock.REQUIRED_FAMILIES) {
            assertTrue(covered.contains(family), "no " + family.name() + " gene on the horse");
        }
        assertTrue(forced.size() >= ArcaneStock.REQUIRED_FAMILIES.size(), "a family was skipped");
        assertTrue(forced.size() <= ArcaneStock.REQUIRED_FAMILIES.size() + 1, "more than one optional family");
    }

    @Test
    void noTwoHorsesInAHerdShareACombination() {
        Rng rng = rng("herd");
        Set<String> taken = new LinkedHashSet<>();
        int stamped = 0;
        for (int horse = 0; horse < LARGEST_HERD; horse++) {
            List<AllelePair> forced = ArcaneStock.rollHorse(rng, taken);
            assertEquals(ArcaneStock.REQUIRED_FAMILIES.size(), countRequired(forced),
                    "horse " + horse + " lost a required family to the no-two-alike rule");
            stamped += forced.size();
        }
        assertEquals(stamped, taken.size(), "two horses were handed the same allele combination");
    }

    /** The optional family is a chance, not a certainty - both outcomes happen. */
    @Test
    void theColourModifierIsSometimesThereAndSometimesNot() {
        int with = 0;
        int without = 0;
        for (int i = 0; i < 200; i++) {
            List<AllelePair> forced = ArcaneStock.rollHorse(new SeededRng(i, "modifier"), new LinkedHashSet<>());
            if (countRequired(forced) == forced.size()) {
                without++;
            } else {
                with++;
            }
        }
        assertTrue(with > 0, "the colour modifier never landed in 200 horses");
        assertTrue(without > 0, "the colour modifier landed on all 200 horses");
    }

    /** §2: the same seed is the same horse, here as everywhere else. */
    @Test
    void theSameSeedRollsTheSameHorse() {
        List<AllelePair> a = ArcaneStock.rollHorse(rng("same"), new LinkedHashSet<>());
        List<AllelePair> b = ArcaneStock.rollHorse(rng("same"), new LinkedHashSet<>());
        assertEquals(a, b);
    }

    /**
     * The whole point of the man: what he sells is <b>showing</b>, so the genome
     * that comes out reports every forced combination back.
     */
    @Test
    void theStampedGenomeShowsWhatWasStamped() {
        Rng rng = rng("genome");
        List<AllelePair> forced = ArcaneStock.rollHorse(rng, new LinkedHashSet<>());
        Genome genome = BreedFounder.roll(Breeds.FERAL_MIXED, rng, Sex.FEMALE, forced);
        assertNotNull(genome);
        Set<String> showing = ArcaneStock.showingTokens(genome.genotype());
        for (AllelePair pair : forced) {
            assertTrue(showing.contains(ArcaneStock.token(pair)),
                    ArcaneStock.token(pair) + " did not survive the founder roll");
        }
    }

    /**
     * The whole dealer, end to end, the way {@code CowboyHandler} runs him: roll
     * a horse, build the genome, read the taken combinations back <b>off the
     * finished genotype</b> rather than off the list that was asked for, and
     * roll the next one against that.
     *
     * <p>That read-back is the join between the two halves and the place a bug
     * would hide - a forced pair that {@link ArcaneStock#showingTokens} failed to
     * report would let the next horse take it again, and the string would
     * quietly fill with duplicates that no other test here would see.
     */
    @Test
    void aWholeStringComesOutDistinctAndFullyMagical() {
        Rng rng = rng("string");
        Set<String> herd = new LinkedHashSet<>();
        for (int horse = 0; horse < LARGEST_HERD; horse++) {
            List<AllelePair> forced = ArcaneStock.rollHorse(rng, new LinkedHashSet<>(herd));
            Genome genome = BreedFounder.roll(Breeds.FERAL_MIXED, rng, forced);

            Set<GeneFamily> covered = new HashSet<>();
            for (AllelePair pair : forced) {
                covered.add(GeneFamily.of(pair.gene()));
            }
            for (GeneFamily family : ArcaneStock.REQUIRED_FAMILIES) {
                assertTrue(covered.contains(family), "horse " + horse + " has no " + family.name() + " gene");
            }

            Set<String> showing = ArcaneStock.showingTokens(genome.genotype());
            for (AllelePair pair : forced) {
                assertTrue(showing.contains(ArcaneStock.token(pair)),
                        "horse " + horse + " lost " + ArcaneStock.token(pair) + " on the way through the founder");
            }
            for (String token : showing) {
                assertFalse(herd.contains(token),
                        "horse " + horse + " shares " + token + " with one already in the string");
            }
            herd.addAll(showing);
        }
    }

    private static int countRequired(List<AllelePair> forced) {
        int n = 0;
        for (AllelePair pair : forced) {
            if (ArcaneStock.REQUIRED_FAMILIES.contains(GeneFamily.of(pair.gene()))) {
                n++;
            }
        }
        return n;
    }
}
