package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.SeededRng;
import org.junit.jupiter.api.Test;

/**
 * The composition a breed asks its founding bands for, and what happens when the
 * pack that spawned cannot satisfy it.
 *
 * <p>That second half is the point. The game spawns the pack before the breed is
 * chosen, so a count is a <b>preference clamped by the clump</b>; these pin the
 * documented precedence - the stallion range first, then the mare maximum, then
 * the mare minimum - so it cannot drift into "whatever the arithmetic did".
 */
class BreedHerdTest {

    private SeededRng rng() {
        return new SeededRng(20260915L);
    }

    // ---- the defaults, which are what the game always did ---------------

    @Test
    void aDefaultFamilyBandIsOneStallionAndTheRestMares() {
        assertEquals(1, BreedHerd.DEFAULT.traditional().stallionCount(4, rng()));
        assertEquals(1, BreedHerd.DEFAULT.traditional().stallionCount(6, rng()));
    }

    @Test
    void aDefaultBachelorBandIsAllMaleAtAnySize() {
        for (int adults = 1; adults <= 8; adults++) {
            assertEquals(adults, BreedHerd.DEFAULT.bachelor().stallionCount(adults, rng()),
                    "a bachelor band must never grow a mare, at " + adults + " adults");
        }
    }

    @Test
    void bandOfPicksTheKindByBandType() {
        assertEquals(BreedHerd.DEFAULT.bachelor(), BreedHerd.DEFAULT.bandOf(true));
        assertEquals(BreedHerd.DEFAULT.traditional(), BreedHerd.DEFAULT.bandOf(false));
    }

    // ---- the clump clamps it --------------------------------------------

    @Test
    void amMareMaximumTurnsTheSurplusStallion() {
        // One stallion wanted, at most four mares - but eight horses spawned, so
        // the four the band will not take as mares are stallions instead.
        BreedHerd.Band band = new BreedHerd.Band(1, 1, 2, 4);
        assertEquals(4, band.stallionCount(8, rng()));
    }

    @Test
    void aMareMinimumIsBestEffortAndNeverCostsTheLastStallion() {
        // Wants two mares; one horse spawned. The stallion range wins, because
        // the only way to insist on a mare here would be to spawn one.
        BreedHerd.Band band = new BreedHerd.Band(1, 1, 2, 4);
        assertEquals(1, band.stallionCount(1, rng()));
    }

    @Test
    void aStallionRangeIsClampedToWhatSpawned() {
        BreedHerd.Band band = new BreedHerd.Band(3, 5, 0, 0);
        assertEquals(2, band.stallionCount(2, rng()), "never more stallions than there are horses");
    }

    @Test
    void aBandMayAskForNoStallionsAtAll() {
        BreedHerd.Band band = new BreedHerd.Band(0, 0, 1, BreedHerd.MANY);
        assertEquals(0, band.stallionCount(3, rng()), "a mare-led group is a breed's to ask for");
    }

    @Test
    void anEmptyClumpIsNoStallions() {
        assertEquals(0, BreedHerd.DEFAULT.traditional().stallionCount(0, rng()));
    }

    @Test
    void aRangeIsDrawnInsideItsBounds() {
        BreedHerd.Band band = new BreedHerd.Band(1, 3, 0, BreedHerd.MANY);
        for (int seed = 0; seed < 40; seed++) {
            int n = band.stallionCount(6, new SeededRng(seed));
            assertTrue(n >= 1 && n <= 3, "drew " + n + " stallions, outside 1..3");
        }
    }

    // ---- shapes and defaults --------------------------------------------

    @Test
    void aBandNormalisesBackwardsAndNegativeRanges() {
        BreedHerd.Band band = new BreedHerd.Band(-2, -5, 4, 2);
        assertEquals(0, band.minStallions());
        assertEquals(0, band.maxStallions());
        assertEquals(4, band.minMares());
        assertEquals(4, band.maxMares(), "a backwards range reads as its own floor");
    }

    @Test
    void aShareOutsideZeroToOneIsClamped() {
        assertEquals(1.0, new BreedHerd(4.0, null, null).bachelorChance());
        assertEquals(0.0, new BreedHerd(-1.0, null, null).bachelorChance());
    }

    @Test
    void isDefaultKnowsWhichKindItIsBeingAskedAbout() {
        assertTrue(BreedHerd.DEFAULT.traditional().isDefault(false));
        assertTrue(BreedHerd.DEFAULT.bachelor().isDefault(true));
        // The two defaults are different shapes, so each must be asked about itself.
        assertTrue(!BreedHerd.DEFAULT.traditional().isDefault(true));
    }
}
