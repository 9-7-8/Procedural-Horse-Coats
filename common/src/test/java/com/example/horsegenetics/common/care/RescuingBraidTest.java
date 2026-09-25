package com.example.horsegenetics.common.care;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>When a braid breaks.</b>
 *
 * <p>The condition is worth pinning because it is asked <i>after</i> the last
 * stand has had its go, which is not where intuition puts it. A test for "did
 * this blow kill the horse" passes every reading of the code and fails in the
 * game on exactly the horses the item is bought for: by the time the braid is
 * asked, the damage is usually already zero and the horse is standing on one
 * health point.
 */
class RescuingBraidTest {

    private static final double MAX = 30.0;
    private static final double FLOOR = LastStand.DEFAULT_HEALTH_LEFT;

    /** The plain case: nothing saved it, and this blow is the end. */
    @Test
    void aFatalBlowBreaksIt() {
        assertTrue(RescuingBraid.fires(12.0, 8.0, FLOOR));
        assertTrue(RescuingBraid.fires(8.0, 8.0, FLOOR), "exactly enough is enough");
        assertFalse(RescuingBraid.fires(7.9, 8.0, FLOOR));
    }

    /**
     * <b>The case the ordering creates, and the reason this is a state test.</b>
     * The last stand ran first, refused the blow and left the horse on the
     * floor. There is no damage left to judge, and the horse is a hair from
     * death - which is precisely when a braid should be spending itself.
     */
    @Test
    void aHorseHeldOnTheFloorBreaksItWithNoDamageLeft() {
        assertTrue(RescuingBraid.fires(0.0, FLOOR, FLOOR));
        assertTrue(RescuingBraid.fires(0.0, FLOOR - 0.1, FLOOR));
    }

    /** A healthy horse taking a scratch keeps its braid. */
    @Test
    void anOrdinaryHitLeavesItAlone() {
        assertFalse(RescuingBraid.fires(3.0, MAX, FLOOR));
        assertFalse(RescuingBraid.fires(0.0, MAX, FLOOR));
    }

    /**
     * A horse already at zero is past saving, and a braid that broke over a
     * corpse would be an item the player watched do nothing.
     */
    @Test
    void itDoesNotBreakOverADeadHorse() {
        assertFalse(RescuingBraid.fires(5.0, 0.0, FLOOR));
        assertFalse(RescuingBraid.fires(5.0, -3.0, FLOOR));
    }

    /**
     * <b>The braid sits above the bolt, and that is the composition.</b> The
     * floor a last stand leaves a horse on has to be under the health it runs
     * at, or a horse the braid sends home would arrive too healthy to enter its
     * escape behaviour - which is the half of the promise that has no code of
     * its own and rides entirely on {@code HorseEscapeGoal}'s own door.
     */
    @Test
    void aHorseOnTheFloorIsHurtEnoughToBolt() {
        assertTrue(Escape.bolts(LastStand.healthLeft(FLOOR, MAX), MAX, Escape.DEFAULT_THRESHOLD),
                "a horse held by a last stand must still be low enough to run once it lands");
    }

    /** Both lines name the horse, and they are not the same sentence. */
    @Test
    void theLinesNameTheHorseAndDiffer() {
        assertTrue(RescuingBraid.saved("Ennis", RescuingBraid.ITS_STALL).contains("Ennis"));
        assertTrue(RescuingBraid.nowhereToSend("Ennis").contains("Ennis"));
        assertNotEquals(RescuingBraid.saved("Ennis", RescuingBraid.ITS_STALL),
                RescuingBraid.saved("Ennis", RescuingBraid.THE_HOLDING_PEN));
    }
}
