package com.example.horsegenetics.common.care;

import com.example.horsegenetics.common.care.SendHome.Price;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Send home: free unless both halves of a price are set, a cooldown that only
 * a real send starts, and wording that names the price and the alternatives.
 */
class SendHomeTest {

    private static final String ALT = SendHome.alternativesLine(List.of("Basic Ticket", "Ender Whistle"));

    @Test
    void theDefaultConfigIsFree() {
        // The shipped defaults are an empty id and a count of 1: free.
        assertTrue(Price.of("", 1).free());
        assertSame(Price.FREE, Price.of("", 1));
    }

    @Test
    void aHalfSetPriceIsFree() {
        assertTrue(Price.of("minecraft:emerald", 0).free());
        assertTrue(Price.of("   ", 3).free());
        assertTrue(Price.of(null, 3).free());
        assertTrue(Price.of("minecraft:emerald", -2).free());
    }

    @Test
    void aSetPriceIsAPrice() {
        Price price = Price.of(" minecraft:emerald ", 2);
        assertFalse(price.free());
        assertEquals("minecraft:emerald", price.itemId());
        assertEquals(2, price.count());
    }

    @Test
    void neverSentMeansNoCooldown() {
        assertEquals(0, SendHome.cooldownLeft(-1, 500, 5));
    }

    @Test
    void theCooldownRunsFromTheLastSend() {
        assertEquals(100, SendHome.cooldownLeft(1000, 1000, 5));
        assertEquals(1, SendHome.cooldownLeft(1000, 1099, 5));
        assertEquals(0, SendHome.cooldownLeft(1000, 1100, 5));
        assertEquals(0, SendHome.cooldownLeft(1000, 5000, 5));
    }

    @Test
    void zeroTurnsTheCooldownOff() {
        assertEquals(0, SendHome.cooldownLeft(1000, 1000, 0));
    }

    @Test
    void theCooldownLineRoundsUp() {
        assertEquals("Wait 5 more seconds before sending another horse home.", SendHome.cooldownLine(100));
        assertEquals("Wait 1 more second before sending another horse home.", SendHome.cooldownLine(1));
        assertEquals("Wait 2 more seconds before sending another horse home.", SendHome.cooldownLine(21));
    }

    @Test
    void aFreeButtonSaysSendHome() {
        assertEquals("Send home", SendHome.buttonLabel(Price.FREE, "ignored"));
        assertEquals("Send this horse to its stall - or to your holding pen, if it has no stall of its own.",
                SendHome.tooltip(Price.FREE, "ignored", false, ALT));
    }

    @Test
    void aPricedButtonNamesThePriceAndTheAlternatives() {
        Price price = Price.of("minecraft:emerald", 2);
        assertEquals("Send home (2 x Emerald)", SendHome.buttonLabel(price, "Emerald"));
        String tip = SendHome.tooltip(price, "Emerald", true, ALT);
        assertTrue(tip.startsWith("Pay 2 x Emerald to take this horse out of its stasis chamber"), tip);
        assertTrue(tip.endsWith("Tickets and whistles can also bring a horse home (Basic Ticket, Ender Whistle)."),
                tip);
    }

    @Test
    void theRefusalNamesItemCountAndAlternatives() {
        assertEquals("Sending a horse home costs 1 x Emerald, and you do not have it. " + ALT,
                SendHome.cannotPayLine(Price.of("minecraft:emerald", 1), "Emerald", ALT));
        assertEquals("Sending a horse home costs 3 x Gold Ingot, and you do not have them.",
                SendHome.cannotPayLine(Price.of("minecraft:gold_ingot", 3), "Gold Ingot", ""));
    }

    @Test
    void noNamesNoAlternativesLine() {
        assertEquals("", SendHome.alternativesLine(List.of()));
    }
}
