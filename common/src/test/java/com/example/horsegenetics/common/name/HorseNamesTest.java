package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;
import com.example.horsegenetics.common.testutil.FakeRng;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HorseNamesTest {

    private static final NameParts DAM = new NameParts("Bright", "Meadow");
    private static final NameParts SIRE = new NameParts("Dark", "Ridge");

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
}
