package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.genes.PassificationGene;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The Netherhorse and the Nightmare</b> - the two breeds on the other side of
 * a doorway, and the first invented ones in the catalogue.
 *
 * <p>{@link BreedFilesTest} already guards that both files load, warn about
 * nothing and bake byte-for-byte. That is a real claim and not this one: it says
 * the files <i>parse</i>, not that a founder rolled off them is the horse the
 * file describes. The Dhampir has had a test of that second kind since the day it
 * shipped and these two did not, which is how a band on a knob the expressed
 * allele never reads got all the way to {@code main} - see
 * {@link BreedBandsReachTest}, which is the general form of that one.
 *
 * <p>Everything asserted here is a sentence somewhere in the breed file's own
 * {@code notes}, deliberately: the notes are the design and this is the thing
 * that stops them drifting away from the pool underneath them.
 */
class NetherBreedsTest {

    /** Enough founders that a 1-in-100 leak out of a pinned pool would show. */
    private static final int FOUNDERS = 500;

    // ------------------------------------------------------------------
    // The Netherhorse
    // ------------------------------------------------------------------

    @Test
    void theNetherhorseIsAMagicalGiantThatBreedsItsSizeTrue() {
        Breed b = breed("netherhorse");
        assertTrue(b.magical(), "the Netherhorse is a magical breed");
        assertEquals(Commonness.VERY_RARE.weight, b.spawnWeight(), 1e-9);

        // Outside the heterozygous window at BOTH ends, so every founder carries
        // two size copies and the breed cannot throw an ordinary-sized foal. The
        // file says so in as many words; this is what makes it true.
        assertFalse(BreedStatCurve.heterozygousSize(1.85));
        assertFalse(BreedStatCurve.heterozygousSize(1.95));
    }

    @Test
    void everyNetherhorseFounderIsTheLiverBayTankTheFileDescribes() {
        Breed b = breed("netherhorse");
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genotype g = BreedFounder.roll(b, new SeededRng(seed)).genotype();
            String at = " at seed " + seed;

            // The coat: red pigment on, agouti on, shade pushed dark. Liver bay.
            assertPair("E/E", g.pair(Genes.EXTENSION), at);
            assertPair("A/A", g.pair(Genes.AGOUTI), at);
            assertPair("Sh/ShD", g.pair(Genes.SHADE), at);

            // The temperament, and the single way past it.
            assertPair("Aaa/Aaa", g.pair(Genes.AGGRESSION), at);
            assertPair("APr/APr", g.pair(Genes.PASSIFICATION), at);

            // What it is for: fire, lava, and more of it than one bottle a day.
            assertPair("Frp/Frp", g.pair(Genes.FIREPROOF), at);
            assertPair("Lava/Lava", g.pair(Genes.MILK), at);
            assertPair("Mlk/Mlk", g.pair(Genes.MAGIC_MILK_VOLUME), at);

            // What it looks like coming at you.
            assertPair("Lava/Lava", g.pair(Genes.PARTICLE), at);
            assertPair("Emb/Emb", g.pair(gene("horsegenetics.emberveins")), at);
            assertPair("MltC/MltC", g.pair(Genes.MOLTEN_HOOVES), at);
            assertPair("Red/Red", g.pair(Genes.EYE_COLOUR_LEFT), at);
            assertPair("Red/Red", g.pair(Genes.EYE_COLOUR_RIGHT), at);
        }
    }

    @Test
    void aNetherhorseWantsNetherWartAndOnlyOnceItIsGrown() {
        Breed b = breed("netherhorse");
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            List<PassificationGene.Route> routes = routes(g);

            // APr/APr is one variant twice over, so there is one way in, not two.
            assertEquals(1, routes.size(), "one route at seed " + seed);
            PassificationGene.Route route = routes.get(0);
            assertEquals("minecraft:nether_wart", route.item(), "at seed " + seed);
            assertEquals(PassificationGene.Kind.PERMANENT, route.kind());
            // The cruel half of the breed: a foal cannot be talked down at all.
            assertEquals(PassificationGene.Window.ADULT, route.window());
            assertTrue(route.amount() >= 2 && route.amount() <= 4,
                    "the band asks for two to four, got " + route.amount() + " at seed " + seed);
        }
    }

    @Test
    void aNetherhorseGlowsRedEverywhereItWasBandedTo() {
        Breed b = breed("netherhorse");
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            String at = " at seed " + seed;

            // Bands are written on both copies, so the expressed view is enough.
            assertRed(values(g, Genes.MOLTEN_HOOVES), "color", 200, 30, at);
            assertRed(values(g, Genes.MANE_COLOR), "hair", 200, 25, at);
            assertRed(values(g, Genes.TAIL_COLOR), "hair", 200, 25, at);
            // There is no fixed red sclera hue, so the whites are CHAOS banded red.
            assertRed(values(g, Genes.EYE_SCLERA_LEFT), "eye_chaos", 210, 30, at);
            assertRed(values(g, Genes.EYE_SCLERA_RIGHT), "eye_chaos", 210, 30, at);
        }
    }

    // ------------------------------------------------------------------
    // The Nightmare
    // ------------------------------------------------------------------

    @Test
    void theNightmareIsTinyAndBreedsThatTrueToo() {
        Breed b = breed("nightmare");
        assertTrue(b.magical());
        assertEquals(Commonness.VERY_RARE.weight, b.spawnWeight(), 1e-9);
        assertFalse(BreedStatCurve.heterozygousSize(0.32));
        assertFalse(BreedStatCurve.heterozygousSize(0.42));
    }

    @Test
    void everyNightmareFounderRunsAndCannotBeMadeToFight() {
        Breed b = breed("nightmare");
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genotype g = BreedFounder.roll(b, new SeededRng(seed)).genotype();
            String at = " at seed " + seed;

            // Flees everything, always - and aggression is pinned wild-type on
            // purpose, so the wild chance of the opposite temperament can never
            // land on this breed. Both halves matter; neither alone says it.
            assertPair("Faa/Faa", g.pair(Genes.SKITTISH), at);
            assertPair("n/n", g.pair(Genes.AGGRESSION), at);

            assertPair("Tf/Tf", g.pair(gene("horsegenetics.flying")), at);
            assertPair("Frp/Frp", g.pair(Genes.FIREPROOF), at);
            assertPair("Prm/Prm", g.pair(Genes.PASSIFICATION), at);
            assertPair("Rflm/Rflm", g.pair(Genes.PARTICLE), at);
            assertPair("Emb/Emb", g.pair(gene("horsegenetics.emberveins")), at);
            assertPair("MltC/MltC", g.pair(Genes.MOLTEN_HOOVES), at);
            assertPair("Gld/Gld", g.pair(Genes.EYE_COLOUR_LEFT), at);
            assertPair("Gld/Gld", g.pair(Genes.EYE_COLOUR_RIGHT), at);

            // A compound heterozygote, so the bottle carries BOTH at the weak
            // grade. Written as Wkn/Drk in the file; the order is not the claim.
            assertPair("Drk/Wkn", g.pair(Genes.POTION_MILK), at);
        }
    }

    @Test
    void aNightmareTakesCakeAtAnyAge() {
        Breed b = breed("nightmare");
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            List<PassificationGene.Route> routes = routes(g);

            assertEquals(1, routes.size(), "one route at seed " + seed);
            PassificationGene.Route route = routes.get(0);
            assertEquals("minecraft:cake", route.item(), "at seed " + seed);
            assertEquals(PassificationGene.Kind.PERMANENT, route.kind());
            // Unlike the Netherhorse's: a foal can be won over as readily as an
            // adult, which is the whole difference between the two ways in.
            assertEquals(PassificationGene.Window.ANY, route.window());
            assertTrue(route.amount() >= 1 && route.amount() <= 3,
                    "the band asks for one to three, got " + route.amount() + " at seed " + seed);
        }
    }

    @Test
    void aNightmareIsGoldWhereTheNetherhorseIsRed() {
        Breed b = breed("nightmare");
        for (long seed = 0; seed < FOUNDERS; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            String at = " at seed " + seed;

            assertGold(values(g, Genes.MOLTEN_HOOVES), "color", at);
            assertGold(values(g, Genes.MANE_COLOR), "hair", at);
            assertGold(values(g, Genes.EYE_SCLERA_LEFT), "eye_chaos", at);
            assertGold(values(g, Genes.EYE_SCLERA_RIGHT), "eye_chaos", at);
        }
    }

    /**
     * The tail is the one hair locus the Nightmare does <b>not</b> name, and the
     * file says why: nothing was asked about it, so it rolls wild. That is easy
     * to "tidy up" later by copying the mane's band across, which would quietly
     * make the breed a second golden locus it was never meant to have.
     */
    @Test
    void theNightmaresTailIsDeliberatelyNotGold() {
        Breed b = breed("nightmare");
        boolean sawSomethingOtherThanGold = false;
        for (long seed = 0; seed < FOUNDERS && !sawSomethingOtherThanGold; seed++) {
            Genome g = BreedFounder.roll(b, new SeededRng(seed));
            EpiValues v = values(g, Genes.TAIL_COLOR);
            if (v.isEmpty()) {
                continue; // the locus is wild, which is the point
            }
            sawSomethingOtherThanGold = !isGold(v, "hair");
        }
        assertTrue(sawSomethingOtherThanGold,
                "every tail came out gold - the tail locus has been banded, "
                        + "which the breed file says it deliberately is not");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Breed breed(String id) {
        Breed b = Breeds.get(id);
        assertNotNull(b, id + " should be a registered breed");
        return b;
    }

    private static Gene gene(String key) {
        Gene g = Genes.byKeyOrNull(key);
        assertNotNull(g, key + " should be a registered gene");
        return g;
    }

    private static List<PassificationGene.Route> routes(Genome g) {
        return Genes.PASSIFICATION.routesOf(g.genotype().pair(Genes.PASSIFICATION),
                GeneEpigenetics.forGene(Genes.PASSIFICATION, g.genotype(), g.epigenome()));
    }

    private static EpiValues values(Genome g, Gene gene) {
        return GeneEpigenetics.forGene(gene, g.genotype(), g.epigenome()).expressed();
    }

    /** Token order is a gamete accident, so compare the pair as a set. */
    private static void assertPair(String expected, AllelePair actual, String at) {
        assertEquals(sorted(expected), sorted(actual.toTokens()), "expected " + expected + at);
    }

    private static String sorted(String tokens) {
        String[] parts = tokens.split("/", 2);
        return parts[0].compareTo(parts[1]) <= 0
                ? parts[0] + "/" + parts[1]
                : parts[1] + "/" + parts[0];
    }

    private static void assertRed(EpiValues v, String prefix, double minRed, double maxRest, String at) {
        assertTrue(v.get(prefix + "_r") >= minRed, prefix + "_r too low" + at);
        assertTrue(v.get(prefix + "_g") <= maxRest, prefix + "_g too high" + at);
        assertTrue(v.get(prefix + "_b") <= maxRest, prefix + "_b too high" + at);
    }

    private static void assertGold(EpiValues v, String prefix, String at) {
        assertTrue(isGold(v, prefix), prefix + " is not gold" + at);
    }

    /** Bright red, plenty of green, almost no blue - which is what gold is. */
    private static boolean isGold(EpiValues v, String prefix) {
        return v.get(prefix + "_r") >= 200
                && v.get(prefix + "_g") >= 160 && v.get(prefix + "_g") <= 215
                && v.get(prefix + "_b") <= 60;
    }
}
