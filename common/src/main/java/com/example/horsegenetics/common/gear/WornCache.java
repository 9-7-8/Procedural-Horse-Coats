package com.example.horsegenetics.common.gear;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * <b>The worn meshes' own cache: a cap, and the least recently used goes first.</b>
 *
 * <p>Separate from the grown parts' cache on purpose (owner, 2026-10-09). That
 * one has no eviction because the set of grown meshes is bounded by the genes;
 * worn meshes are keyed on a texture, and a modpack or a resource pack can
 * supply any number of those.
 *
 * <p>The key is whatever the caller says makes two meshes different - for the
 * game, the texture, the model it was built from and the lift - and a value is
 * only ever returned for an equal key. Not synchronised: the render thread is
 * the only caller.
 */
public final class WornCache<K, V> {

    private final int cap;
    private final LinkedHashMap<K, V> map;

    public WornCache(int cap) {
        if (cap < 1) {
            throw new IllegalArgumentException("a cache holds at least one mesh, not " + cap);
        }
        this.cap = cap;
        // Access order: a get moves its entry to the young end.
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > WornCache.this.cap;
            }
        };
    }

    /** The value for {@code key}, built now if it is not held. */
    public V get(K key, Function<K, V> build) {
        V held = this.map.get(key);
        if (held == null) {
            held = build.apply(key);
            this.map.put(key, held);
        }
        return held;
    }

    public boolean holds(K key) {
        return this.map.containsKey(key);
    }

    public int size() {
        return this.map.size();
    }

    public int cap() {
        return this.cap;
    }

    public void clear() {
        this.map.clear();
    }
}
