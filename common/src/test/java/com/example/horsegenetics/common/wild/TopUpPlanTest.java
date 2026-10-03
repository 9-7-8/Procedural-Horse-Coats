package com.example.horsegenetics.common.wild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.BreedSource;
import com.example.horsegenetics.common.breed.BreedSpawnSettings;
import com.example.horsegenetics.common.wild.TopUpPlan.CellStamp;
import com.example.horsegenetics.common.wild.TopUpPlan.Decision;
import com.example.horsegenetics.common.wild.TopUpPlan.Settings;
import java.util.List;
import org.junit.jupiter.api.Test;

class TopUpPlanTest {

    private static final Settings DEFAULT = Settings.DEFAULT;
    private static final BreedSpawnSettings.Feral FERAL = BreedSpawnSettings.Feral.DEFAULT;

    private static Breed fjord(String description, String... biomes) {
        return Breed.of("fjord", "Fjord").biomes(biomes).description(description).build();
    }

    private static Breed arab() {
        return Breed.of("arab", "Arab").biomes("minecraft:desert").build();
    }

    // --- cells ---

    @Test
    void cellsFloorAtNegativeCoordinates() {
        assertEquals(0, TopUpPlan.cellOf(0, 8));
        assertEquals(0, TopUpPlan.cellOf(127, 8));
        assertEquals(1, TopUpPlan.cellOf(128, 8));
        assertEquals(-1, TopUpPlan.cellOf(-1, 8));
        assertEquals(-1, TopUpPlan.cellOf(-128, 8));
        assertEquals(-2, TopUpPlan.cellOf(-129, 8));
    }

    @Test
    void aCellKeyRoundTripsBothSigns() {
        for (int[] c : new int[][] {{0, 0}, {-1, 5}, {7, -3}, {-40000, -2}, {Integer.MAX_VALUE, Integer.MIN_VALUE}}) {
            long key = TopUpPlan.cellKey(c[0], c[1]);
            assertEquals(c[0], TopUpPlan.cellX(key));
            assertEquals(c[1], TopUpPlan.cellZ(key));
        }
        assertNotEquals(TopUpPlan.cellKey(1, -1), TopUpPlan.cellKey(-1, 1));
    }

    @Test
    void aPlayerInTheMiddleOfACellIsNearOnlyThatCellAtASmallRadius() {
        List<Long> near = TopUpPlan.cellsNear(64, 64, 32, 8);
        assertEquals(List.of(TopUpPlan.cellKey(0, 0)), near);
    }

    @Test
    void aPlayerOnACornerIsNearAllFourAndNotTheDiagonalBeyondTheRadius() {
        List<Long> near = TopUpPlan.cellsNear(0.5, 0.5, 96, 8);
        assertTrue(near.contains(TopUpPlan.cellKey(0, 0)));
        assertTrue(near.contains(TopUpPlan.cellKey(-1, -1)));
        assertTrue(near.contains(TopUpPlan.cellKey(-1, 0)));
        assertTrue(near.contains(TopUpPlan.cellKey(0, -1)));
        // 96 blocks east of 0.5 is 96.5, short of the next cell, which starts at 128.
        assertFalse(near.contains(TopUpPlan.cellKey(1, 0)));
        assertEquals(4, near.size());
    }

    // --- the decision ---

    @Test
    void underTheMinimumRollsAndAtItDoesNot() {
        assertEquals(Decision.ROLL, TopUpPlan.decide(null, 4, 99L, 4, DEFAULT));
        assertEquals(Decision.FULL, TopUpPlan.decide(null, 4, 99L, 5, DEFAULT));
        assertEquals(Decision.FULL, TopUpPlan.decide(null, 4, 99L, 30, DEFAULT));
    }

    @Test
    void yesterdaysStampIsStaleAndTodaysIsNot() {
        CellStamp yesterday = new CellStamp(3, 99L);
        CellStamp today = new CellStamp(4, 99L);
        assertFalse(TopUpPlan.current(yesterday, 4, 99L));
        assertTrue(TopUpPlan.current(today, 4, 99L));
        assertFalse(TopUpPlan.current(null, 4, 99L));
        assertFalse(TopUpPlan.keep(yesterday, 4));
        assertTrue(TopUpPlan.keep(today, 4));
    }

    @Test
    void aNewSignatureMakesTodaysStampStale() {
        assertFalse(TopUpPlan.current(new CellStamp(4, 99L), 4, 100L));
    }

    @Test
    void runningThePlanTwiceInOneDayRollsOnce() {
        long today = 12;
        long sig = TopUpPlan.signature(List.of(arab()), FERAL, DEFAULT);
        CellStamp stamp = null;
        int rolls = 0;
        for (int pass = 0; pass < 2; pass++) {
            Decision d = TopUpPlan.decide(stamp, today, sig, 0, DEFAULT);
            if (d == Decision.ROLL) {
                rolls++;
            }
            if (d != Decision.CURRENT) {
                stamp = new CellStamp(today, sig);
            }
        }
        assertEquals(1, rolls);
        assertEquals(Decision.ROLL, TopUpPlan.decide(stamp, today + 1, sig, 0, DEFAULT), "and again the next morning");
    }

    @Test
    void theDayIsGameTimeOverADay() {
        assertEquals(0, TopUpPlan.dayOf(23_999L));
        assertEquals(1, TopUpPlan.dayOf(24_000L));
    }

    // --- the signature ---

    @Test
    void theSignatureChangesWhenABreedsBiomesChange() {
        long before = TopUpPlan.signature(List.of(fjord("a pony", "minecraft:taiga"), arab()), FERAL, DEFAULT);
        long after = TopUpPlan.signature(List.of(fjord("a pony", "minecraft:taiga", "minecraft:grove"), arab()),
                FERAL, DEFAULT);
        assertNotEquals(before, after);
    }

    @Test
    void theSignatureIgnoresADescriptionAndTheOrderOfThings() {
        long one = TopUpPlan.signature(List.of(fjord("a pony", "minecraft:taiga", "minecraft:grove"), arab()),
                FERAL, DEFAULT);
        long two = TopUpPlan.signature(List.of(arab(), fjord("a dun pony from Norway", "minecraft:grove",
                "minecraft:taiga")), FERAL, DEFAULT);
        assertEquals(one, two);
    }

    @Test
    void theSignatureIgnoresABreedThatNeverComesWild() {
        Breed cowboyOnly = Breed.of("stock", "Stock").biomes("minecraft:plains").sources(BreedSource.COWBOY).build();
        assertEquals(TopUpPlan.signature(List.of(arab()), FERAL, DEFAULT),
                TopUpPlan.signature(List.of(arab(), cowboyOnly), FERAL, DEFAULT));
    }

    @Test
    void theSignatureChangesWithFeralMixedAndTheCellSettings() {
        long base = TopUpPlan.signature(List.of(arab()), FERAL, DEFAULT);
        assertNotEquals(base, TopUpPlan.signature(List.of(arab()),
                new BreedSpawnSettings.Feral(false, List.of(), 0.0), DEFAULT));
        assertNotEquals(base, TopUpPlan.signature(List.of(arab()), FERAL, new Settings(6, 8, 96)));
        assertNotEquals(base, TopUpPlan.signature(List.of(arab()), FERAL, new Settings(5, 4, 96)));
        assertEquals(base, TopUpPlan.signature(List.of(arab()), FERAL, new Settings(5, 8, 200)),
                "the player radius decides who rolls a cell, not what lives there");
    }

    // --- settings and packs ---

    @Test
    void zeroMinimumIsOffAndBadSettingsRefuse() {
        assertTrue(new Settings(0, 8, 96).off());
        assertFalse(DEFAULT.off());
        assertEquals(128, DEFAULT.cellBlocks());
        assertThrows(IllegalArgumentException.class, () -> new Settings(-1, 8, 96));
        assertThrows(IllegalArgumentException.class, () -> new Settings(5, 1, 96));
    }

    @Test
    void aPackIsTheBiomesOwnCount() {
        SeededRng rng = new SeededRng(7L);
        for (int i = 0; i < 200; i++) {
            int n = TopUpPlan.packSize(3, 6, rng);
            assertTrue(n >= 3 && n <= 6, "pack of " + n);
        }
        assertEquals(1, TopUpPlan.packSize(0, 0, rng), "never an empty herd");
    }
}
