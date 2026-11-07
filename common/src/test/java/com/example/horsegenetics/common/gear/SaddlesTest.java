package com.example.horsegenetics.common.gear;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The mod saddle (#75): the chestplate's tiers, the 10% ceiling, and the bonus only while ridden. */
class SaddlesTest {

    @Test
    @DisplayName("the top tier is the config's ceiling, 10% by default, on speed and jump both")
    void theTopTierIsTheCeiling() {
        Saddles.Bonus top = Saddles.bonus(Saddles.Rates.DEFAULT, Saddles.Tier.NETHERITE, true);
        assertEquals(0.10, top.speed(), 1e-12);
        assertEquals(0.10, top.jump(), 1e-12);
    }

    @Test
    @DisplayName("nothing while nobody rides: a saddled horse in a field is the horse its genes made")
    void nothingUnridden() {
        assertSame(Saddles.Bonus.NONE, Saddles.bonus(Saddles.Rates.DEFAULT, Saddles.Tier.NETHERITE, false));
        assertSame(Saddles.Bonus.NONE, Saddles.bonus(Saddles.Rates.DEFAULT, null, true));
    }

    @Test
    @DisplayName("every tier is worth more than the one before, and none more than the top")
    void theLadderClimbs() {
        double last = 0.0;
        for (Saddles.Tier tier : Saddles.Tier.values()) {
            double speed = Saddles.Rates.DEFAULT.of(tier).speed();
            assertTrue(speed > last, tier + " is not above the tier before it");
            assertTrue(speed <= Saddles.DEFAULT_TOP_BONUS + 1e-12, tier + " is above the ceiling");
            last = speed;
        }
    }

    @Test
    @DisplayName("a server's ceiling moves the whole ladder, is capped, and the switch turns it all off")
    void theConfig() {
        Saddles.Rates doubled = new Saddles.Rates(true, 0.20, 0.0);
        assertEquals(0.20 * Saddles.Tier.IRON.share(), doubled.of(Saddles.Tier.IRON).speed(), 1e-12);
        assertEquals(0.0, doubled.of(Saddles.Tier.IRON).jump(), 1e-12);
        assertEquals(Saddles.MAX_TOP_BONUS, new Saddles.Rates(true, 5.0, 5.0).of(Saddles.Tier.NETHERITE).speed(), 1e-12);
        assertEquals(0.0, new Saddles.Rates(true, -1.0, -1.0).of(Saddles.Tier.NETHERITE).speed(), 1e-12);
        assertEquals(Saddles.DEFAULT_TOP_BONUS,
                new Saddles.Rates(true, Double.NaN, Double.NaN).of(Saddles.Tier.NETHERITE).speed(), 1e-12);
        assertSame(Saddles.Bonus.NONE, new Saddles.Rates(false, 0.1, 0.1).of(Saddles.Tier.NETHERITE));
    }

    @Test
    @DisplayName("the seven chestplate materials ship; another mod's copper or gold gets no second saddle")
    void whatShips() {
        assertEquals(7, Saddles.Tier.values().length);
        assertEquals("golden_saddle", Saddles.Tier.GOLD.id());
        assertEquals("chainmail_saddle", Saddles.Tier.CHAINMAIL.id());
        assertTrue(Saddles.ships("gold"));
        assertTrue(Saddles.ships("golden"));
        assertTrue(Saddles.ships("copper"));
        assertFalse(Saddles.ships("tin"));
        assertEquals("tin_saddle", Saddles.idFor("tin"));
        assertSame(Saddles.Tier.DIAMOND, Saddles.tierOf(true));
        assertSame(Saddles.Tier.IRON, Saddles.tierOf(false));
    }
}
