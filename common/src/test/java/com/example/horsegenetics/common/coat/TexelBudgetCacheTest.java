package com.example.horsegenetics.common.coat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The coat texture cache's policy, tested away from the GPU.
 *
 * <p>What is being guarded is the pair of properties that make it safe to put a
 * {@code DynamicTexture} behind it: nothing leaves without the eviction hook
 * seeing it (or the client leaks a registered texture per horse, which is the
 * defect this class was written for), and the entry that was just handed to a
 * caller is never the one thrown away.
 */
class TexelBudgetCacheTest {

    /** A stand-in for "one baked sheet", small enough to do the arithmetic by hand. */
    private static final long SHEET = 100;

    private final List<String> freed = new ArrayList<String>();

    private TexelBudgetCache<String, String> cache(long budget) {
        return new TexelBudgetCache<String, String>(
                budget,
                value -> value == null ? 0 : SHEET,
                (key, value) -> freed.add(key));
    }

    private static String bake(String key) {
        return "coat:" + key;
    }

    @Test
    void aHitDoesNotReload() {
        TexelBudgetCache<String, String> cache = cache(10 * SHEET);
        int[] bakes = {0};

        String first = cache.get("a", k -> {
            bakes[0]++;
            return bake(k);
        });
        String second = cache.get("a", k -> {
            bakes[0]++;
            return bake(k);
        });

        assertEquals("coat:a", first);
        assertEquals(first, second);
        assertEquals(1, bakes[0], "second get recomposed the coat instead of hitting the cache");
        assertEquals(List.of(), freed);
    }

    @Test
    void evictsTheLeastRecentlyUsedOnceOverBudget() {
        TexelBudgetCache<String, String> cache = cache(3 * SHEET);
        cache.get("a", TexelBudgetCacheTest::bake);
        cache.get("b", TexelBudgetCacheTest::bake);
        cache.get("c", TexelBudgetCacheTest::bake);
        assertEquals(List.of(), freed, "three sheets is exactly the budget - nothing should go yet");

        cache.get("d", TexelBudgetCacheTest::bake);

        assertEquals(List.of("a"), freed);
        assertEquals(3, cache.size());
        assertEquals(3 * SHEET, cache.texels());
        assertEquals(1, cache.evictionCount());
    }

    @Test
    void useOrderIsAccessOrderNotInsertionOrder() {
        TexelBudgetCache<String, String> cache = cache(3 * SHEET);
        cache.get("a", TexelBudgetCacheTest::bake);
        cache.get("b", TexelBudgetCacheTest::bake);
        cache.get("c", TexelBudgetCacheTest::bake);

        // "a" is the eldest by insertion; touching it makes "b" the eldest by use.
        cache.get("a", TexelBudgetCacheTest::bake);
        cache.get("d", TexelBudgetCacheTest::bake);

        assertEquals(List.of("b"), freed,
                "a horse that is still on screen every frame must not be the one evicted");
    }

    @Test
    void evictsAsManyAsItTakes() {
        TexelBudgetCache<String, String> cache = new TexelBudgetCache<String, String>(
                3 * SHEET,
                value -> value.length(),
                (key, value) -> freed.add(key));

        cache.get("a", k -> repeat(SHEET));   // 100
        cache.get("b", k -> repeat(SHEET));   // 100
        cache.get("big", k -> repeat(SHEET * 3)); // 300 - over by 200, so both of the others go

        assertEquals(List.of("a", "b"), freed);
        assertEquals(1, cache.size());
        assertEquals(3 * SHEET, cache.texels());
    }

    @Test
    void neverEvictsTheEntryItJustLoaded() {
        TexelBudgetCache<String, String> cache = cache(SHEET / 2); // budget below one sheet

        String value = cache.get("a", TexelBudgetCacheTest::bake);

        assertEquals("coat:a", value);
        assertEquals(1, cache.size(), "handed back a value it had already freed");
        assertEquals(List.of(), freed);
        assertTrue(cache.texels() > cache.budget(),
                "a single over-sized entry is held over budget on purpose");

        // ...and it is still the only one held once another arrives.
        cache.get("b", TexelBudgetCacheTest::bake);
        assertEquals(List.of("a"), freed);
        assertEquals(1, cache.size());
    }

    @Test
    void aNullValueIsCachedAndCostsNothing() {
        // The "nothing on this horse glows" marker: a real entry, so the coat is
        // not recomposed every frame, but no texture behind it.
        TexelBudgetCache<String, String> cache = cache(2 * SHEET);
        int[] bakes = {0};

        for (int i = 0; i < 3; i++) {
            cache.get("dull", k -> {
                bakes[0]++;
                return null;
            });
        }

        assertEquals(1, bakes[0]);
        assertEquals(0, cache.texels());
        assertEquals(1, cache.size());
    }

    @Test
    void clearFreesEverything() {
        TexelBudgetCache<String, String> cache = cache(10 * SHEET);
        cache.get("a", TexelBudgetCacheTest::bake);
        cache.get("b", TexelBudgetCacheTest::bake);

        cache.clear();

        assertEquals(List.of("a", "b"), freed);
        assertEquals(0, cache.size());
        assertEquals(0, cache.texels());
    }

    @Test
    void aZeroBudgetIsRefusedRatherThanCachingNothing() {
        assertThrows(IllegalArgumentException.class, () -> cache(0));
    }

    private static String repeat(long n) {
        StringBuilder sb = new StringBuilder();
        for (long i = 0; i < n; i++) {
            sb.append('x');
        }
        return sb.toString();
    }
}
