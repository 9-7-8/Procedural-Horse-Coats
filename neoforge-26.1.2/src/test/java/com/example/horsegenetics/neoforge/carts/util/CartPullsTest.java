package com.example.horsegenetics.neoforge.carts.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>#198: who is pulling a cart, after the world was saved and loaded.</b>
 *
 * <p>A reloaded {@link CartWorld} holds UUIDs the codec built, and the cart asks
 * with its own UUID object: equal, never the same instance. The two are built
 * separately here for exactly that reason.
 */
class CartPullsTest {

    @Test
    @DisplayName("a cart is found by an equal UUID that is not the same object")
    void equalUuidFromDiskMatches() {
        UUID stored = new UUID(0x1234L, 0x5678L);
        UUID asked = new UUID(0x1234L, 0x5678L);
        Map<Integer, UUID> pulling = new HashMap<>();
        pulling.put(7, new UUID(1L, 1L));
        pulling.put(42, stored);

        assertEquals(OptionalInt.of(42), CartPulls.pullerOf(pulling, asked),
                "a cart reloaded from disk lost its puller");
    }

    @Test
    @DisplayName("a cart nobody pulls has no puller")
    void unpulledCartIsEmpty() {
        Map<Integer, UUID> pulling = new HashMap<>();
        pulling.put(7, new UUID(1L, 1L));

        assertTrue(CartPulls.pullerOf(pulling, new UUID(2L, 2L)).isEmpty());
        assertTrue(CartPulls.pullerOf(new HashMap<>(), new UUID(2L, 2L)).isEmpty());
    }
}
