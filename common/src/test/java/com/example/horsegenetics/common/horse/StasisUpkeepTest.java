package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.care.Hunger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The bank's economy, without a game.</b>
 *
 * <p>Every one of these is a claim {@code wiki/horse-stasis.html} makes about
 * the bank in words, pinned here in arithmetic: that an idle bank spends
 * nothing, that a horse cannot be healed past its bar or paid for below
 * starving, that food without water buys nothing this turn and is not thrown
 * away either, and that a blood-drinker is no more feedable in a chamber than
 * it is in a field.
 */
class StasisUpkeepTest {

    private static final float MAX = 30.0F;

    /**
     * <b>A healthy horse costs the bank nothing.</b> The bank sits over a full
     * feed slot and a full meter and takes neither, because there is nothing to
     * heal - the one property that keeps a bank of fifty well horses from being
     * a standing bill.
     */
    @Test
    void anUnhurtHorseSpendsNothing() {
        StasisUpkeep.Result r = StasisUpkeep.upkeep(40.0, MAX, MAX, 100, true, Hunger.Food.HAY);
        assertFalse(r.changed());
        assertFalse(r.ate());
        assertEquals(0.0, r.healed());
        assertEquals(100, r.water());
        assertEquals(40.0, r.hunger());
    }

    /** A hurt horse with feed, water and hunger to spend gets health back. */
    @Test
    void aHurtHorseHeals() {
        StasisUpkeep.Result r = StasisUpkeep.upkeep(Hunger.FULL, 10.0F, MAX, 100, true, null);
        assertTrue(r.healed() > 0.0);
        assertTrue(r.health() > 10.0F);
        assertEquals(Hunger.healOver(StasisUpkeep.HEAL_INTERVAL, false), r.healed(), 1.0e-6);
        // Hunger paid for exactly the health that landed.
        assertEquals(Hunger.afterHealing(Hunger.FULL, r.healed()), r.hunger(), 1.0e-6);
        // ...and so did the meter, rounded up.
        assertEquals(100 - (int) Math.ceil(r.healed()), r.water());
    }

    /** Never past the bar, however much water and hunger are standing by. */
    @Test
    void healingStopsAtTheBar() {
        StasisUpkeep.Result r = StasisUpkeep.upkeep(Hunger.FULL, MAX - 0.25F, MAX, 100, true, null);
        assertEquals(MAX, r.health());
        assertEquals(0.25, r.healed(), 1.0e-5);
    }

    /**
     * <b>Water alone is not enough</b> - a horse too hungry to pay stays hurt,
     * exactly as {@link Hunger#affordable} makes it stay hurt in a field.
     */
    @Test
    void aStarvingHorseCannotBuyHealth() {
        StasisUpkeep.Result r = StasisUpkeep.upkeep(Hunger.STARVING, 10.0F, MAX, 100, true, null);
        assertEquals(0.0, r.healed());
        assertEquals(10.0F, r.health());
        assertEquals(100, r.water());
    }

    /**
     * <b>Feed alone is not enough either</b>, but the mouthful is not wasted:
     * it is banked against the turn after somebody fills the water slot.
     */
    @Test
    void foodWithoutWaterStillFeeds() {
        StasisUpkeep.Result r = StasisUpkeep.upkeep(20.0, 10.0F, MAX, 0, true, Hunger.Food.HAY);
        assertTrue(r.ate());
        assertTrue(r.changed());
        assertEquals(Hunger.eat(20.0, Hunger.Food.HAY), r.hunger());
        assertEquals(0.0, r.healed());
        assertEquals(10.0F, r.health());
    }

    /**
     * <b>One turn feeds and heals</b>, in that order - a horse that arrives too
     * hungry to pay eats first and then gets health out of the same turn.
     */
    @Test
    void eatingComesBeforeHealingInOneTurn() {
        StasisUpkeep.Result r = StasisUpkeep.upkeep(Hunger.STARVING, 10.0F, MAX, 100, true, Hunger.Food.HAY);
        assertTrue(r.ate());
        assertTrue(r.healed() > 0.0);
    }

    /** A horse that is already sated does not take a mouthful it has no room for. */
    @Test
    void aSatedHorseLeavesTheFeedAlone() {
        StasisUpkeep.Result r = StasisUpkeep.upkeep(Hunger.FULL, 10.0F, MAX, 100, true, Hunger.Food.HAY);
        assertFalse(r.ate());
        assertTrue(r.healed() > 0.0);
    }

    /**
     * <b>A diet no slot can feed heals in a bank no more than it does in a
     * field.</b> The gate is the diet's own {@code fedByItems()}, which is what
     * {@code HorseCareHandler} asks a live horse.
     */
    @Test
    void aDietNoSlotCanFeedGetsNothing() {
        StasisUpkeep.Result r = StasisUpkeep.upkeep(Hunger.FULL, 10.0F, MAX, 100, false, Hunger.Food.HAY);
        assertFalse(r.changed());
        assertEquals(100, r.water());
        assertEquals(10.0F, r.health());
    }

    /** A dry meter is never charged below zero, and a last drop still heals. */
    @Test
    void theLastOfTheWaterIsSpentNotOverdrawn() {
        StasisUpkeep.Result r = StasisUpkeep.upkeep(Hunger.FULL, 10.0F, MAX, 1, true, null);
        assertEquals(0, r.water());
        assertTrue(r.healed() > 0.0);
        assertTrue(r.healed() <= 1.0);
    }

    /**
     * A dead or malformed reading is left alone rather than resurrected - the
     * chamber's own rule for a horse it cannot read.
     */
    @Test
    void nonsenseIsLeftAlone() {
        assertFalse(StasisUpkeep.upkeep(Hunger.FULL, 0.0F, MAX, 100, true, Hunger.Food.HAY).changed());
        assertFalse(StasisUpkeep.upkeep(Hunger.FULL, 5.0F, 0.0F, 100, true, Hunger.Food.HAY).changed());
    }

    /** The turn is the live handler's scan interval, not a number of its own. */
    @Test
    void theIntervalIsTheFieldsInterval() {
        assertEquals(30, StasisUpkeep.HEAL_INTERVAL);
        assertSame(Hunger.Food.HAY, Hunger.Food.valueOf("HAY"));
    }
}
