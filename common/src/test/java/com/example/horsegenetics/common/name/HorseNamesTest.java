package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;
import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HorseNamesTest {

    private static final NameParts DAM = new NameParts("Bright", "Meadow");
    private static final NameParts SIRE = new NameParts("Dark", "Ridge");
    // one-word tables so generateParts() is deterministic without queued ints
    private static final HorseNameGenerator GEN =
            new HorseNameGenerator(List.of("Rolled"), List.of("Random"));

    @Test
    void firstFromDamMeansLastFromSire() {
        // breed() draws one boolean: true = first half comes from the dam
        NameParts child = HorseNames.breed(DAM, SIRE, new FakeRng().booleans(true));
        assertEquals("Bright", child.first());
        assertEquals("Ridge", child.last());
    }

    @Test
    void firstFromSireMeansLastFromDam() {
        NameParts child = HorseNames.breed(DAM, SIRE, new FakeRng().booleans(false));
        assertEquals("Dark", child.first());
        assertEquals("Meadow", child.last());
    }

    @Test
    void neverBothHalvesFromOneParent() {
        NameParts a = HorseNames.breed(DAM, SIRE, new FakeRng().booleans(true));
        NameParts b = HorseNames.breed(DAM, SIRE, new FakeRng().booleans(false));
        // exactly one half of each child matches the dam
        assertEquals(1, halvesFrom(a, DAM));
        assertEquals(1, halvesFrom(b, DAM));
    }

    private static int halvesFrom(NameParts child, NameParts parent) {
        int n = 0;
        if (child.first().equals(parent.first())) n++;
        if (child.last().equals(parent.last())) n++;
        return n;
    }

    // --- breedNth: foal-count-varied naming ---

    @Test
    void firstFoalIsDamFirstSireLast() {
        NameParts c = HorseNames.breedNth(DAM, SIRE, 0, GEN, new FakeRng());
        assertEquals("Bright", c.first());
        assertEquals("Ridge", c.last());
    }

    @Test
    void secondFoalIsTheOtherCombo() {
        NameParts c = HorseNames.breedNth(DAM, SIRE, 1, GEN, new FakeRng());
        assertEquals("Dark", c.first());
        assertEquals("Meadow", c.last());
    }

    @Test
    void foalsThreeThroughSixKeepOneParentHalfPlusRandom() {
        // generateParts() draws two ints; then a parent pick, then a half pick
        NameParts c = HorseNames.breedNth(DAM, SIRE, 2, GEN,
                new FakeRng().ints(0, 0).booleans(true, true));
        assertEquals("Bright", c.first());  // dam's first name kept
        assertEquals("Random", c.last());   // last name rolled
    }

    @Test
    void seventhFoalOnwardIsFullyRandom() {
        NameParts c = HorseNames.breedNth(DAM, SIRE, 6, GEN, new FakeRng().ints(0, 0));
        assertEquals("Rolled", c.first());
        assertEquals("Random", c.last());
        assertNotEquals(DAM.first(), c.first());
        assertNotEquals(SIRE.last(), c.last());
    }
}
