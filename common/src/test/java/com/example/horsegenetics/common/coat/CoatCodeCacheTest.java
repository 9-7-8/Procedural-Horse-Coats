package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The codes-to-coat cache behind the transfer deed, the info screen's
 * portraits and the family tree (#206): one parse per pair of codes, and a
 * bound that holds.
 */
class CoatCodeCacheTest {

    private static final String BAY = Codes.of("extension", "E/e", "agouti", "A/a");

    private static String epi(long seed) {
        return Epigenome.fromSeed(seed).alignedTo(Genotype.parse(BAY)).toCode();
    }

    @Test
    void theSameCodesGiveTheSameCoat() {
        CoatCodeCache cache = new CoatCodeCache(8);
        CoatData first = cache.coatOf(BAY, epi(1L));
        assertSame(first, cache.coatOf(BAY, epi(1L)),
                "a second draw of the same deed must not parse it again");
        assertEquals(new CoatData(Genome.parse(BAY, epi(1L))), first,
                "the cached coat is the one a fresh parse gives");
        assertEquals(new CoatData(Genome.parse(BAY, epi(1L))).textureKey(), first.textureKey());
    }

    @Test
    void differentCodesGiveDifferentCoats() {
        CoatCodeCache cache = new CoatCodeCache(8);
        CoatData one = cache.coatOf(BAY, epi(1L));
        CoatData two = cache.coatOf(BAY, epi(2L));
        assertNotSame(one, two);
        assertEquals(new CoatData(Genome.parse(BAY, epi(2L))), two);
    }

    @Test
    void staysWithinItsBoundAndDropsTheOldest() {
        CoatCodeCache cache = new CoatCodeCache(4);
        CoatData oldest = cache.coatOf(BAY, epi(0L));
        for (long seed = 1; seed < 50; seed++) {
            cache.coatOf(BAY, epi(seed));
            assertEquals(Math.min(seed + 1, 4), cache.size(), "after seed " + seed);
        }
        assertNotSame(oldest, cache.coatOf(BAY, epi(0L)),
                "the first coat was evicted long ago, so it comes back as a new parse");
    }

    @Test
    void clearForgetsEverything() {
        CoatCodeCache cache = new CoatCodeCache(4);
        CoatData before = cache.coatOf(BAY, epi(1L));
        cache.clear();
        assertEquals(0, cache.size());
        assertNotSame(before, cache.coatOf(BAY, epi(1L)));
    }
}
