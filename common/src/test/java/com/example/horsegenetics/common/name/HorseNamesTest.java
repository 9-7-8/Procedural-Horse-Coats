package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.name.HorseNameGenerator.NameParts;
import com.example.horsegenetics.common.name.NamingPolicy.InheritedHalf;
import com.example.horsegenetics.common.name.NamingPolicy.ParentSource;
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

    private static NameParts foal(Sex sex, NamingPolicy policy) {
        return HorseNames.foal(DAM, SIRE, sex, policy, GEN, new FakeRng().ints(0, 0));
    }

    // --- the default: surname down the same-sex line, first name rolled ---

    @Test
    void fillyTakesHerDamsLastName() {
        NameParts c = foal(Sex.FEMALE, NamingPolicy.DEFAULT);
        assertEquals("Meadow", c.last());
        assertEquals("Rolled", c.first());
    }

    @Test
    void coltTakesHisSiresLastName() {
        NameParts c = foal(Sex.MALE, NamingPolicy.DEFAULT);
        assertEquals("Ridge", c.last());
        assertEquals("Rolled", c.first());
    }

    @Test
    void theFirstNameIsNeverInheritedByDefault() {
        assertNotEquals(DAM.first(), foal(Sex.FEMALE, NamingPolicy.DEFAULT).first());
        assertNotEquals(SIRE.first(), foal(Sex.MALE, NamingPolicy.DEFAULT).first());
    }

    // --- the parent source ---

    @Test
    void damSourceIgnoresTheFoalsSex() {
        NamingPolicy fromDam = new NamingPolicy(InheritedHalf.LAST, ParentSource.DAM);
        assertEquals("Meadow", foal(Sex.MALE, fromDam).last());
        assertEquals("Meadow", foal(Sex.FEMALE, fromDam).last());
    }

    @Test
    void sireSourceIgnoresTheFoalsSex() {
        NamingPolicy fromSire = new NamingPolicy(InheritedHalf.LAST, ParentSource.SIRE);
        assertEquals("Ridge", foal(Sex.MALE, fromSire).last());
        assertEquals("Ridge", foal(Sex.FEMALE, fromSire).last());
    }

    // --- the inherited half ---

    @Test
    void firstHalfPolicyInheritsTheFirstNameAndRollsTheLast() {
        NamingPolicy firstNames = new NamingPolicy(InheritedHalf.FIRST, ParentSource.BY_SEX);
        NameParts filly = foal(Sex.FEMALE, firstNames);
        assertEquals("Bright", filly.first());
        assertEquals("Random", filly.last());
        NameParts colt = foal(Sex.MALE, firstNames);
        assertEquals("Dark", colt.first());
        assertEquals("Random", colt.last());
    }

    // --- exactly one half is ever inherited ---

    @Test
    void exactlyOneHalfComesFromAParent() {
        for (Sex sex : Sex.values()) {
            for (InheritedHalf half : InheritedHalf.values()) {
                for (ParentSource source : ParentSource.values()) {
                    NameParts c = foal(sex, new NamingPolicy(half, source));
                    assertEquals(1, halvesFromParents(c),
                            sex + " " + half + " " + source);
                }
            }
        }
    }

    private static int halvesFromParents(NameParts child) {
        int n = 0;
        if (child.first().equals(DAM.first()) || child.first().equals(SIRE.first())) n++;
        if (child.last().equals(DAM.last()) || child.last().equals(SIRE.last())) n++;
        return n;
    }
}
