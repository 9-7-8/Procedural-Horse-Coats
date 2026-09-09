package com.example.horsegenetics.common.coat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A least-recently-used cache whose capacity is measured in <b>texels</b>
 * rather than in entries, with an eviction hook so the owner can release
 * whatever the value was holding.
 *
 * <p>This exists because the generated coat textures are a memory budget, not a
 * count: one entry is a whole sheet, so "how many coats may the client keep"
 * is really "how many pixels may the client keep", and the answer has to stay
 * right when {@code HorseSkinGeometry.SHEET_SIZE} changes. An entry-count LRU
 * tuned at the current size would silently become sixteen times the memory at
 * four times the sheet; a texel budget holds the memory still and caches fewer
 * coats instead, which is the trade you actually want.
 *
 * <p>Eviction is <i>eldest-first until under budget</i>, and every removal -
 * including {@link #clear()} - goes through the same {@link Eviction} hook, so
 * there is one place that frees a value and no way to drop an entry without
 * freeing it. A zero-cost entry is evicted in its turn like any other: it is
 * cheap to hold but not cheap to recompute, and treating it specially would be
 * a second policy for no gain.
 *
 * <p><b>Never evicts the entry it just loaded.</b> If a single value is larger
 * than the whole budget the cache holds exactly that one, over budget, rather
 * than freeing it immediately and returning a corpse.
 *
 * <p><b>Threading.</b> Every method is {@code synchronized}. The one caller in
 * the mod (the coat texture factory) is on the render thread, because
 * registering a texture is a GL operation - the lock is uncontended there and
 * is here so that a future off-thread bake cannot corrupt the access order,
 * which a {@code LinkedHashMap} would do silently rather than loudly. Note that
 * the loader runs <i>inside</i> the lock, so two misses cannot compose in
 * parallel; that is a deliberate trade for the render-thread caller and would
 * need revisiting before any background baking.
 *
 * @param <K> cache key
 * @param <V> the cached resource
 */
public final class TexelBudgetCache<K, V> {

    /** Produces the value for a key on a miss. May return {@code null}, which is cached. */
    public interface Loader<K, V> {
        V load(K key);
    }

    /** The texel cost of a value - what it counts for against the budget. */
    public interface Cost<V> {
        long texels(V value);
    }

    /** Called for every entry that leaves the cache, whether by eviction or by {@link #clear()}. */
    public interface Eviction<K, V> {
        void evicted(K key, V value);
    }

    private static final class Entry<V> {
        final V value;
        final long texels;

        Entry(V value, long texels) {
            this.value = value;
            this.texels = texels;
        }
    }

    private final long budget;
    private final Cost<V> cost;
    private final Eviction<K, V> eviction;

    /** Access-ordered: {@code get} moves an entry to the young end. */
    private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<K, Entry<V>>(16, 0.75f, true);

    private long texels;
    private long evictionCount;

    /**
     * @param budget    texels the cache may hold before it starts evicting; must be positive
     * @param cost      the texel cost of a value
     * @param eviction  called for every entry that leaves, to free it
     */
    public TexelBudgetCache(long budget, Cost<V> cost, Eviction<K, V> eviction) {
        if (budget <= 0) {
            throw new IllegalArgumentException("texel budget must be positive, got " + budget);
        }
        this.budget = budget;
        this.cost = cost;
        this.eviction = eviction;
    }

    /**
     * The cached value for {@code key}, loading and admitting it on a miss.
     * A hit marks the entry most-recently-used.
     */
    public synchronized V get(K key, Loader<K, V> loader) {
        Entry<V> hit = entries.get(key);
        if (hit != null) {
            return hit.value;
        }
        V value = loader.load(key);
        long size = cost.texels(value);
        entries.put(key, new Entry<V>(value, size));
        texels += size;
        trim();
        return value;
    }

    /** Evict eldest-first until under budget, never touching the youngest entry. */
    private void trim() {
        List<Map.Entry<K, Entry<V>>> gone = new ArrayList<Map.Entry<K, Entry<V>>>();
        Iterator<Map.Entry<K, Entry<V>>> it = entries.entrySet().iterator();
        // entries.size() is the live count - the iterator has already removed
        // what is in `gone` - so this stops with exactly one entry left, which
        // is the youngest, which is the one just handed to the caller.
        while (texels > budget && entries.size() > 1 && it.hasNext()) {
            Map.Entry<K, Entry<V>> eldest = it.next();
            it.remove();
            texels -= eldest.getValue().texels;
            evictionCount++;
            gone.add(eldest);
        }
        // Freeing happens after the map is consistent, so an eviction hook that
        // re-enters the cache cannot see a half-trimmed state.
        for (Map.Entry<K, Entry<V>> e : gone) {
            eviction.evicted(e.getKey(), e.getValue().value);
        }
    }

    /** Drop everything, freeing each entry through the eviction hook. */
    public synchronized void clear() {
        List<Map.Entry<K, Entry<V>>> gone = new ArrayList<Map.Entry<K, Entry<V>>>(entries.entrySet());
        entries.clear();
        texels = 0;
        for (Map.Entry<K, Entry<V>> e : gone) {
            eviction.evicted(e.getKey(), e.getValue().value);
        }
    }

    /** How many entries are held. */
    public synchronized int size() {
        return entries.size();
    }

    /** How many texels are held. Exceeds the budget only for a single over-sized entry. */
    public synchronized long texels() {
        return texels;
    }

    /** The budget this cache was built with. */
    public long budget() {
        return budget;
    }

    /** Total entries evicted for space over the cache's life - a diagnostic, not state. */
    public synchronized long evictionCount() {
        return evictionCount;
    }
}
