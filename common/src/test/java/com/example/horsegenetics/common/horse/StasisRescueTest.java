package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.care.Escape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The emergency chamber's threshold, and the quiet period on its refusal.</b>
 *
 * <p>Both are cheap to get subtly wrong and impossible to notice in a running
 * game: a rescue that fires one point too late is a dead horse with no message,
 * and a refusal with no quiet period is a wall of red text every half-second
 * while a horse stands in a fire.
 */
class StasisRescueTest {

    private static final float MAX = 30.0F;

    /** A tenth is a tenth of <i>this</i> horse, not a fixed number of hearts. */
    @Test
    void catchesBelowATenthOfTheHorsesOwnMaximum() {
        assertTrue(StasisRescue.rescues(2.9F, MAX, StasisRescue.DEFAULT_THRESHOLD));
        assertFalse(StasisRescue.rescues(3.1F, MAX, StasisRescue.DEFAULT_THRESHOLD));
        // A frail pony and a Percheron are caught at different numbers of hearts
        // and at the same fraction of themselves.
        assertTrue(StasisRescue.rescues(0.9F, 10.0F, StasisRescue.DEFAULT_THRESHOLD));
        assertFalse(StasisRescue.rescues(0.9F, 4.0F, StasisRescue.DEFAULT_THRESHOLD));
    }

    /** Exactly on the line has not fallen <i>below</i> it. */
    @Test
    void theLineItselfIsNotLowEnough() {
        assertFalse(StasisRescue.rescues(3.0F, MAX, StasisRescue.DEFAULT_THRESHOLD));
    }

    /**
     * <b>A killing blow is the case this exists for.</b> The handler asks about
     * the health the horse would have <em>after</em> the hit, and that number is
     * routinely zero or negative - the whole point being that the chamber takes
     * the horse instead of the grave.
     */
    @Test
    void catchesAHorseOutOfAKillingBlow() {
        assertTrue(StasisRescue.rescues(0.0F, MAX, StasisRescue.DEFAULT_THRESHOLD));
        assertTrue(StasisRescue.rescues(-12.0F, MAX, StasisRescue.DEFAULT_THRESHOLD));
    }

    /** Zero is the off switch, and a horse with no maximum is not a question. */
    @Test
    void zeroTurnsItOff() {
        assertFalse(StasisRescue.rescues(0.0F, MAX, 0.0));
        assertFalse(StasisRescue.rescues(0.0F, 0.0F, StasisRescue.DEFAULT_THRESHOLD));
    }

    /**
     * <b>The chamber sits below the bolt, and must stay there.</b> A horse runs
     * for its life first and is swallowed only if running did not work; a
     * default that drifted above {@code Escape}'s would capture horses that were
     * never given the chance to escape.
     */
    @Test
    void theRescueIsLowerThanTheEscape() {
        assertTrue(StasisRescue.DEFAULT_THRESHOLD < Escape.DEFAULT_THRESHOLD,
                "an emergency chamber must catch a horse below the health it bolts at");
    }

    /** A horse nothing has been said about yet is always due. */
    @Test
    void theFirstRefusalIsAlwaysDue() {
        assertTrue(StasisRescue.dueAgain(StasisRescue.NEVER, 0L));
        assertTrue(StasisRescue.dueAgain(StasisRescue.NEVER, 500_000L));
    }

    /** And the next one waits out the quiet period. */
    @Test
    void refusalsAreThrottledPerHorse() {
        long told = 1_000L;
        assertFalse(StasisRescue.dueAgain(told, told));
        assertFalse(StasisRescue.dueAgain(told, told + StasisRescue.QUIET_TICKS - 1));
        assertTrue(StasisRescue.dueAgain(told, told + StasisRescue.QUIET_TICKS));
    }

    /**
     * A stamp from the future is a world whose clock went backwards - a restore
     * from a backup. Better to say the line again than to go silent for the rest
     * of the world's life.
     */
    @Test
    void aClockThatWentBackwardsDoesNotSilenceIt() {
        assertTrue(StasisRescue.dueAgain(9_000L, 5L));
    }

    /** Every line names the horse - that is the whole of what makes them useful. */
    @Test
    void everyLineNamesTheHorse() {
        assertTrue(StasisRescue.saved("Ennis").contains("Ennis"));
        assertTrue(StasisRescue.savedInBank("Ennis").contains("Ennis"));
        assertTrue(StasisRescue.refused("Ennis").contains("Ennis"));
        assertEquals(StasisRescue.refused("Ennis"), StasisRescue.refused("Ennis"));
    }

    /**
     * <b>The two saves are different sentences.</b> A chamber off your belt and
     * one out of the cabinet leave the player in different positions - one of
     * them is a bottle short in their pocket and the other is not - and a player
     * who is told the same thing either way will pat the wrong one.
     */
    @Test
    void theBankSaveSaysItWasTheBank() {
        assertNotEquals(StasisRescue.saved("Ennis"), StasisRescue.savedInBank("Ennis"));
        assertTrue(StasisRescue.savedInBank("Ennis").contains("bank"));
        assertFalse(StasisRescue.saved("Ennis").contains("bank"));
    }
}
