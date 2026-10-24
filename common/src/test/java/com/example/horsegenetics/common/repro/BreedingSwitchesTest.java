package com.example.horsegenetics.common.repro;

import com.example.horsegenetics.common.repro.BreedingSwitches.Kind;
import com.example.horsegenetics.common.repro.BreedingSwitches.RealmCover;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Issue #276: every kind of breeding has a switch, and the master beats them all. */
class BreedingSwitchesTest {

    private static BreedingSwitches all(boolean master, boolean on, RealmCover realm) {
        return new BreedingSwitches(master, on, realm, on, on, on);
    }

    @Test
    void theDefaultsAreTodaysBehaviour() {
        BreedingSwitches d = BreedingSwitches.defaults();
        assertTrue(d.master());
        assertEquals(ReproRules.DEFAULT_NATURAL_BREEDING, d.allows(Kind.NATURAL_COVER));
        assertEquals(ReproRules.DEFAULT_NATURAL_BREEDING, d.allows(Kind.REALM_COVER), "the realm follows");
        assertTrue(d.allows(Kind.STASIS_STUD));
        assertTrue(d.allows(Kind.GOLDEN_CARROT));
        assertTrue(d.allows(Kind.SEED_JAR));
    }

    @Test
    void theMasterOffRefusesEveryKind() {
        BreedingSwitches s = all(false, true, RealmCover.ON);
        for (Kind k : Kind.values()) {
            assertFalse(s.allows(k), k.id());
            assertTrue(s.own(k), k.id() + "'s own switch is still reported");
        }
    }

    @Test
    void eachKindTurnsOffAlone() {
        for (Kind off : Kind.values()) {
            BreedingSwitches s = new BreedingSwitches(true,
                    off != Kind.NATURAL_COVER,
                    off == Kind.REALM_COVER ? RealmCover.OFF : RealmCover.ON,
                    off != Kind.STASIS_STUD, off != Kind.GOLDEN_CARROT, off != Kind.SEED_JAR);
            for (Kind k : Kind.values()) {
                assertEquals(k != off, s.allows(k), off.id() + " off, asking " + k.id());
            }
        }
    }

    @Test
    void theRealmFollowsOrOverrides() {
        assertTrue(new BreedingSwitches(true, true, RealmCover.FOLLOW, true, true, true).naturalCoverIn(true));
        assertFalse(new BreedingSwitches(true, false, RealmCover.FOLLOW, true, true, true).naturalCoverIn(true));
        assertTrue(new BreedingSwitches(true, false, RealmCover.ON, true, true, true).naturalCoverIn(true));
        assertFalse(new BreedingSwitches(true, false, RealmCover.ON, true, true, true).naturalCoverIn(false));
        assertFalse(new BreedingSwitches(true, true, RealmCover.OFF, true, true, true).naturalCoverIn(true));
        assertTrue(new BreedingSwitches(true, true, RealmCover.OFF, true, true, true).naturalCoverIn(false));
    }

    @Test
    void kindsAreFoundByTheirCommandWord() {
        for (Kind k : Kind.values()) {
            assertEquals(k, Kind.byId(k.id()));
        }
        assertNull(Kind.byId("all"), "'all' is the master, never a kind");
    }
}
