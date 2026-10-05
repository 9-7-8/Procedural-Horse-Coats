package com.example.horsegenetics.neoforge.carts.util;

import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * The reverse lookup on {@link CartWorld}'s puller-to-cart map: which entity id is
 * pulling a given cart. Kept free of Minecraft so this module's test classpath,
 * which carries none, can load it.
 *
 * <p>The UUIDs must be compared with {@code equals}. A map loaded from disk holds
 * UUIDs the codec built, never the cart's own instance, so a reference compare
 * matched only until the first save and load (#198).
 */
public final class CartPulls {

    private CartPulls() {}

    public static OptionalInt pullerOf(Map<Integer, UUID> pulling, UUID cart) {
        for (Map.Entry<Integer, UUID> e : pulling.entrySet()) {
            if (cart.equals(e.getValue())) return OptionalInt.of(e.getKey());
        }
        return OptionalInt.empty();
    }
}
