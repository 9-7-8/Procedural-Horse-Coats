package com.example.horsegenetics.common.care;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hurt notice's rules: the line names the horse, what hurt it and what to do
 * about it; the cure line only appears when the horse is actually in danger; and
 * one horse cannot fill the chat log with the same fire.
 */
class HurtNoticeTest {

    @Test
    void theLineNamesTheHorseTheSourceAndTheAdvice() {
        String line = HurtNotice.line("Ravensong", HurtNotice.Cause.FREEZING, "", 3.0, 20.0, 24.0);
        assertTrue(line.startsWith("Ravensong took 1.5 hearts from the cold."), line);
        assertTrue(line.contains(HurtNotice.Cause.FREEZING.advice()), line);
    }

    @Test
    void anAttackerOutranksTheCausePhrase() {
        String line = HurtNotice.line("Ravensong", HurtNotice.Cause.ATTACK, "a zombie", 2.0, 20.0, 24.0);
        assertTrue(line.startsWith("Ravensong took 1 heart from a zombie."), line);
        assertFalse(line.contains("from an attack"), line);
    }

    @Test
    void theCureLineWaitsUntilTheHorseIsInDanger() {
        assertFalse(HurtNotice.line("Ravensong", HurtNotice.Cause.FALL, "", 2.0, 20.0, 24.0).contains("beside water"),
                "a scratch on a healthy horse is not a nursing lesson");
        assertTrue(HurtNotice.line("Ravensong", HurtNotice.Cause.FALL, "", 2.0, 4.0, 24.0).contains("beside water"),
                "a horse on two hearts of twelve needs to be told how to heal it");
    }

    @Test
    void aLethalGenotypeIsNeverOfferedACure() {
        String line = HurtNotice.line("Foal", HurtNotice.Cause.GENETIC_DEFECT, "", 6.0, 1.0, 20.0);
        assertFalse(line.contains("beside water"), "nothing heals a lethal genotype: " + line);
        assertTrue(line.contains("not to pair those two again"), line);
    }

    @Test
    void unknownDamageIsNamedButNotExplained() {
        assertEquals(HurtNotice.Cause.UNKNOWN, HurtNotice.of("some_other_mods_damage"));
        assertEquals(HurtNotice.Cause.UNKNOWN, HurtNotice.of(null));
        assertEquals("Ravensong took 1 heart from something.",
                HurtNotice.line("Ravensong", HurtNotice.Cause.UNKNOWN, "", 2.0, 20.0, 24.0));
    }

    @Test
    void vanillaFireIdsAllReachTheOneFireAdvice() {
        for (String id : new String[] {"inFire", "onFire", "campfire", "hotFloor"}) {
            assertEquals(HurtNotice.Cause.FIRE, HurtNotice.of(id), id);
        }
        assertEquals(HurtNotice.Cause.LAVA, HurtNotice.of("lava"), "lava earns its own advice");
        assertEquals(HurtNotice.Cause.GENETIC_DEFECT, HurtNotice.of("genetic_defect"));
    }

    @Test
    void sunlightIsNeverGuessedFromTheDamageType() {
        // It is vanilla's on-fire damage on the wire; only the game module, which
        // can ask the genome, may choose it.
        for (String id : new String[] {"inFire", "onFire", "campfire", "hotFloor", "lava"}) {
            assertFalse(HurtNotice.of(id) == HurtNotice.Cause.SUNLIGHT, id);
        }
    }

    @Test
    void oneHorseCannotFillTheChatLog() {
        assertTrue(HurtNotice.dueAgain(-1L, 0L), "a horse never spoken about is always due");
        assertFalse(HurtNotice.dueAgain(1_000L, 1_000L + HurtNotice.QUIET_TICKS - 1L));
        assertTrue(HurtNotice.dueAgain(1_000L, 1_000L + HurtNotice.QUIET_TICKS));
        assertTrue(HurtNotice.dueAgain(1_000L, 40L), "a world clock that went backwards must not mute a horse for ever");
    }

    @Test
    void aScratchIsNotWorthALine() {
        assertFalse(HurtNotice.worthTelling(0.4));
        assertTrue(HurtNotice.worthTelling(HurtNotice.MIN_DAMAGE));
    }

    @Test
    void healthPointsAreReadAsHearts() {
        assertEquals("half a heart", HurtNotice.hearts(1.0));
        assertEquals("1 heart", HurtNotice.hearts(2.0));
        assertEquals("1.5 hearts", HurtNotice.hearts(3.0));
        assertEquals("5 hearts", HurtNotice.hearts(10.0));
        assertEquals("no hearts", HurtNotice.hearts(0.0));
    }
}
