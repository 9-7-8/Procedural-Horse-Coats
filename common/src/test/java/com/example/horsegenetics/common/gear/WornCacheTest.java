package com.example.horsegenetics.common.gear;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The worn meshes' cache: it is capped, it drops what was used longest ago, and
 * it never hands one texture's mesh to another.
 */
class WornCacheTest {

    /** What the game keys a mesh on. */
    private record Key(String texture, String model, float lift) {
    }

    @Test
    @DisplayName("past its cap it drops the least recently used, not the oldest built")
    void itEvictsTheLeastRecentlyUsed() {
        WornCache<String, String> cache = new WornCache<>(2);
        cache.get("a", k -> "A");
        cache.get("b", k -> "B");
        cache.get("a", k -> "again");       // a is now the younger
        cache.get("c", k -> "C");
        assertEquals(2, cache.size());
        assertTrue(cache.holds("a"));
        assertFalse(cache.holds("b"));
        assertTrue(cache.holds("c"));
    }

    @Test
    @DisplayName("a held mesh is built once")
    void aHeldMeshIsBuiltOnce() {
        WornCache<String, String> cache = new WornCache<>(4);
        AtomicInteger builds = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            assertEquals("A", cache.get("a", k -> {
                builds.incrementAndGet();
                return "A";
            }));
        }
        assertEquals(1, builds.get());
    }

    @Test
    @DisplayName("a mesh is only ever returned for its own texture, model and lift")
    void aMeshIsNeverReturnedForAnotherTexture() {
        WornCache<Key, String> cache = new WornCache<>(8);
        assertEquals("mane", cache.get(new Key("braid_mane", "adult", 0.25f), k -> "mane"));
        assertEquals("tail", cache.get(new Key("braid_tail", "adult", 0.25f), k -> "tail"));
        assertEquals("foal", cache.get(new Key("braid_mane", "foal", 0.25f), k -> "foal"));
        assertEquals("deep", cache.get(new Key("braid_mane", "adult", 0.5f), k -> "deep"));
        assertEquals("mane", cache.get(new Key("braid_mane", "adult", 0.25f), k -> "wrong"));
        assertEquals(4, cache.size());
    }

    @Test
    @DisplayName("clearing empties it, and a cap under one is refused")
    void clearingAndTheCap() {
        WornCache<String, String> cache = new WornCache<>(3);
        cache.get("a", k -> "A");
        cache.clear();
        assertEquals(0, cache.size());
        assertEquals("fresh", cache.get("a", k -> "fresh"));
        assertEquals(3, cache.cap());
        assertThrows(IllegalArgumentException.class, () -> new WornCache<String, String>(0));
    }
}
