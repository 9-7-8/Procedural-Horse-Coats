package com.example.horsegenetics.common.care;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The death notice's rules: one sentence, naming the horse, whose it was and
 * what killed it - and never the hurt notice's advice, which has nothing left to
 * prevent.
 */
class DeathNoticeTest {

    @Test
    void theLineNamesTheHorseTheOwnerAndTheKiller() {
        assertEquals("Ravensong, Ixora's horse, was killed by a zombie.",
                DeathNotice.line("Ravensong", "Ixora", HurtNotice.Cause.ATTACK, "a zombie"));
    }

    @Test
    void theCauseSpeaksWhenNothingDealtTheBlow() {
        assertEquals("Ravensong, Ixora's horse, was killed by lava.",
                DeathNotice.line("Ravensong", "Ixora", HurtNotice.Cause.LAVA, ""));
    }

    @Test
    void anUnknownDamageTypeIsStillASentence() {
        assertEquals("Ravensong, Ixora's horse, was killed by something.",
                DeathNotice.line("Ravensong", "Ixora", HurtNotice.Cause.UNKNOWN, ""));
    }

    @Test
    void anOwnerNoCacheKnowsIsStillSomebody() {
        assertEquals("Ravensong, somebody's horse, was killed by a fall.",
                DeathNotice.line("Ravensong", "", HurtNotice.Cause.FALL, null));
    }

    @Test
    void aLethalGenotypeIsNotSomethingThatKilledIt() {
        String line = DeathNotice.line("Foal", "Ixora", HurtNotice.Cause.GENETIC_DEFECT, "");
        assertEquals("Foal, Ixora's horse, did not survive a genetic defect.", line);
        assertFalse(line.contains("killed by"), line);
    }

    @Test
    void theHurtNoticesAdviceIsNeverAppended() {
        for (HurtNotice.Cause cause : HurtNotice.Cause.values()) {
            String line = DeathNotice.line("Ravensong", "Ixora", cause, "");
            assertFalse(!cause.advice().isEmpty() && line.contains(cause.advice()),
                    "nothing is left to prevent: " + line);
            assertTrue(line.endsWith("."), line);
        }
    }
}
