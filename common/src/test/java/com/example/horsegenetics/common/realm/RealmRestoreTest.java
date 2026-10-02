package com.example.horsegenetics.common.realm;

import com.example.horsegenetics.common.realm.RealmRestore.Action;
import com.example.horsegenetics.common.realm.RealmRestore.Facts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A restore raises what the realm change lost and nothing else: never a second
 * copy of a living horse, never a horse that left or died outside the realm.
 */
class RealmRestoreTest {

    private static Action decide(boolean loaded, boolean loadedInRealm, boolean inStasis,
                                 boolean dead, boolean lastSeenInRealm, boolean inRealmFiles) {
        return RealmRestore.decide(new Facts(loaded, loadedInRealm, inStasis, dead,
                lastSeenInRealm, inRealmFiles));
    }

    @Test
    void aLoadedHorseIsNeverRaised() {
        // Even with a stale death mark: a horse somebody can see is alive, and
        // raising it would put two entities on one UUID.
        assertEquals(Action.RESCUE, decide(true, true, false, true, true, true));
        assertEquals(Action.LEAVE, decide(true, false, false, true, true, false));
    }

    @Test
    void deadInTheRealmIsRaised() {
        assertEquals(Action.RAISE, decide(false, false, false, true, true, false));
    }

    @Test
    void deadOutsideTheRealmIsLeft() {
        // Taken home after the backup and died there - not the realm change's doing.
        assertEquals(Action.LEAVE, decide(false, false, false, true, false, false));
    }

    @Test
    void aBottledHorseIsLeft() {
        assertEquals(Action.LEAVE, decide(false, false, true, false, true, false));
    }

    @Test
    void anUnloadedHorseStillOnDiskIsCheckedWhenItLoads() {
        assertEquals(Action.RESCUE_LATER, decide(false, false, false, false, true, true));
    }

    @Test
    void aHorseInNoWorldNoChamberAndNoFileIsRaised() {
        // Lost without dying: no death mark, no record of leaving, nowhere on disk.
        assertEquals(Action.RAISE, decide(false, false, false, false, true, false));
    }
}
