package com.example.horsegenetics.common.horse;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The tier ladder has no holes in it.</b>
 *
 * <p>Everything downstream of {@link StasisTier} - the bank's Browse tab, the
 * upgrade recipes, the tooltips - reads the capability flags rather than
 * switching on the tier by name, on the assumption that a higher tier can do
 * everything a lower one can. Nothing in the language enforces that; it is four
 * independent booleans written by hand, and a typo in one of them is a chamber
 * that quietly loses a capability its own recipe charged a diamond for.
 */
class StasisTierTest {

    /** Each rung keeps everything the rung below unlocked. */
    @Test
    void capabilitiesOnlyEverAccumulate() {
        StasisTier[] ladder = StasisTier.values();
        for (int i = 1; i < ladder.length; i++) {
            StasisTier below = ladder[i - 1];
            StasisTier here = ladder[i];
            assertTrue(!below.searchable() || here.searchable(),
                    here + " lost searchability that " + below + " had");
            assertTrue(!below.heals() || here.heals(),
                    here + " lost healing that " + below + " had");
            assertTrue(!below.collectsDrops() || here.collectsDrops(),
                    here + " lost drop collection that " + below + " had");
            assertTrue(!below.breedsInBank() || here.breedsInBank(),
                    here + " lost in-bank breeding that " + below + " had");
        }
    }

    /**
     * And each rung adds something, which is what the recipes charge for - no
     * rung above the bottom may cost a diamond and hand back the tier below.
     */
    @Test
    void eachRungAddsSomething() {
        StasisTier[] ladder = StasisTier.values();
        assertEquals(0, count(ladder[0]), "the bottom tier is storage only");
        for (int i = 1; i < ladder.length; i++) {
            assertTrue(count(ladder[i]) > count(ladder[i - 1]),
                    ladder[i] + " buys nothing over " + ladder[i - 1]);
        }
    }

    /**
     * <b>Searching and healing are one capability wearing two names</b>, and
     * the ladder must never drift into having one without the other. Both are
     * the bank being able to look inside the chamber: the Browse tab reads the
     * horse to list it, and the upkeep reads the same horse to find out that it
     * is hurt, what it eats and how much of its bar is gone. A tier that could
     * be healed but not searched would be a bank that mends a horse it cannot
     * name.
     */
    @Test
    void healingAndSearchingAreTheSameWindow() {
        for (StasisTier tier : StasisTier.values()) {
            assertEquals(tier.searchable(), tier.heals(),
                    tier + " can do one of search/heal but not the other");
        }
    }

    private static int count(StasisTier tier) {
        return (tier.searchable() ? 1 : 0)
                + (tier.heals() ? 1 : 0)
                + (tier.collectsDrops() ? 1 : 0)
                + (tier.breedsInBank() ? 1 : 0);
    }

    /** Ids are the saved form, so two tiers sharing one would read back as the wrong chamber. */
    @Test
    void idsAreUniqueAndRoundTrip() {
        Set<String> seen = new HashSet<>();
        for (StasisTier tier : StasisTier.values()) {
            assertTrue(seen.add(tier.id()), "two tiers share the id " + tier.id());
            assertSame(tier, StasisTier.byId(tier.id()));
        }
        assertNull(StasisTier.byId("no_such_tier"));
    }

    /** {@code next()} is what an upgrade recipe produces, and the top rung has none. */
    @Test
    void nextWalksTheLadderAndStops() {
        StasisTier tier = StasisTier.BASIC;
        int rungs = 1;
        while (tier.next() != null) {
            StasisTier up = tier.next();
            assertNotNull(up);
            assertEquals(tier.ordinal() + 1, up.ordinal());
            tier = up;
            rungs++;
        }
        assertSame(StasisTier.SPACER, tier, "the ladder does not end at the top tier");
        assertEquals(StasisTier.values().length, rungs, "next() skipped a tier");
    }
}
