package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.genetics.Genotype;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>What the browser's whereabouts column says, and when it shrugs.</b>
 *
 * <p>Worth its own class because the rule changed and the old one was written
 * out by hand at two separate drawing sites. It used to be <i>loaded or not</i>,
 * which was the wrong question for two kinds of horse: a dead one, which the
 * roster now drops, and a horse in a stasis chamber, which is not in an unloaded
 * chunk so much as not an entity at all. "Not loaded" reads as <i>somewhere you
 * cannot see right now</i>, and it invited the player to go and fetch a horse
 * that was in a bottle on a shelf.
 */
class WhereaboutsLabelTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    private static HorseListing row(boolean loaded, String where) {
        return HorseListing.of(ID, "Test", "Case", "", "Arabian", 1, Genotype.wildType(),
                true, true, 0, false, loaded, where, "owner", "", false, false);
    }

    @Test
    void aLoadedHorseSaysWhereItIs() {
        HorseListing row = row(true, "overworld 118, 71, -204");
        assertTrue(row.whereKnown());
        assertEquals("overworld 118, 71, -204", row.whereLabel("not loaded"));
    }

    /** Nothing known is the only case that gets the caller's fallback wording. */
    @Test
    void anUnknownHorseGetsTheCallersWording() {
        HorseListing row = row(false, "");
        assertFalse(row.whereKnown());
        assertEquals("not loaded", row.whereLabel("not loaded"));
        assertEquals("not loaded right now", row.whereLabel("not loaded right now"));
    }

    /**
     * <b>A chamber is an answer, not an absence.</b> The horse is unloaded and
     * its whereabouts are nonetheless a fact, which is the whole reason the test
     * is on the string rather than on {@code loaded}.
     */
    @Test
    void aShelvedHorseIsInAChamberRatherThanMissing() {
        HorseListing row = row(false, HorseListing.IN_STASIS);
        assertTrue(row.whereKnown());
        assertEquals(HorseListing.IN_STASIS, row.whereLabel("not loaded"));
    }

    /** And the filter box finds it, because whereabouts are in the haystack. */
    @Test
    void stasisIsAWordTheFilterBoxCanFindThemBy() {
        assertTrue(row(false, HorseListing.IN_STASIS).haystack().contains("stasis"));
        assertFalse(row(false, "").haystack().contains("stasis"));
    }
}
