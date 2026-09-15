package com.example.horsegenetics.common.care;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hunger's rules: it drains at the same rate however often the game asks, a starving horse
 * cannot heal, healing never spends past the starving line, and a hungry horse looks for
 * food in the owner's order.
 */
class HungerTest {

    @Test
    void drainIsTheSameWhateverTheScan() {
        double coarse = Hunger.FULL;
        for (int i = 0; i < 24; i++) {
            coarse = Hunger.drain(coarse, 1_000L);
        }
        double fine = Hunger.FULL;
        for (int i = 0; i < 800; i++) {
            fine = Hunger.drain(fine, 30L);
        }
        assertEquals(Hunger.HUNGRY, coarse, 1e-9, "a full horse is hungry after one day");
        assertEquals(coarse, fine, 1e-9);
    }

    @Test
    void drainStopsAtEmpty() {
        assertEquals(0.0, Hunger.drain(5.0, Hunger.DAY_TICKS * 10));
    }

    @Test
    void aStarvingHorseCannotHeal() {
        assertFalse(Hunger.canHeal(Hunger.STARVING));
        assertFalse(Hunger.canHeal(0.0));
        assertEquals(0.0, Hunger.affordable(Hunger.STARVING, 10.0));
        assertTrue(Hunger.canHeal(Hunger.STARVING + 0.5));
    }

    @Test
    void healingNeverSpendsPastStarving() {
        double hunger = 20.0;
        double healed = Hunger.affordable(hunger, 100.0);
        assertEquals((20.0 - Hunger.STARVING) / Hunger.COST_PER_HEALTH, healed, 1e-9);
        assertEquals(Hunger.STARVING, Hunger.afterHealing(hunger, healed), 1e-9);
    }

    @Test
    void aFedHorseAffordsWhatItNeeds() {
        assertEquals(3.0, Hunger.affordable(Hunger.FULL, 3.0), 1e-9);
        assertEquals(Hunger.FULL - 3.0 * Hunger.COST_PER_HEALTH, Hunger.afterHealing(Hunger.FULL, 3.0), 1e-9);
    }

    @Test
    void healingIsFasterThanTheHealItReplaced() {
        double old = 1.0 / 30L * 20.0;     // one point every thirty ticks, per second
        assertTrue(Hunger.healOver(20L, false) > old);
        assertEquals(2.0 * Hunger.healOver(30L, false), Hunger.healOver(30L, true), 1e-9);
    }

    @Test
    void eatingClampsAtFull() {
        assertEquals(Hunger.FULL, Hunger.eat(90.0, Hunger.Food.HAY));
    }

    @Test
    void foodIsSoughtInTheOwnersOrder() {
        Hunger.Food[] expected = {
                Hunger.Food.FAVOURITE, Hunger.Food.DROPPED, Hunger.Food.CAKE, Hunger.Food.HAY,
                Hunger.Food.CROP, Hunger.Food.GRASS, Hunger.Food.MOSS, Hunger.Food.MUSHROOM,
                Hunger.Food.FLOWER};
        int i = 0;
        for (Hunger.Food f : Hunger.Food.values()) {
            if (f.sought()) {
                assertEquals(expected[i++], f);
            }
        }
        assertEquals(expected.length, i);
    }

    @Test
    void aFedHorseGrazesOnlyWellBelowFull() {
        assertTrue(Hunger.grazes(Hunger.GRAZE_BELOW - 1.0));
        assertFalse(Hunger.grazes(Hunger.GRAZE_BELOW));
        assertTrue(Hunger.GRAZE_BELOW > Hunger.HUNGRY && Hunger.GRAZE_BELOW < Hunger.SATED);
    }

    @Test
    void aHungryHorseEatsUntilSated() {
        assertTrue(Hunger.seeksFood(Hunger.HUNGRY - 1.0));
        assertFalse(Hunger.seeksFood(Hunger.HUNGRY));
        assertTrue(Hunger.wantsMore(Hunger.SATED - 1.0));
        assertFalse(Hunger.wantsMore(Hunger.SATED));
    }
}
