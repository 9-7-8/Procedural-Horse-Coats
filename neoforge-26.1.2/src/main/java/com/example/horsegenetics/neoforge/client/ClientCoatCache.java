package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple client-side store for coat data received via CoatSyncPayload.
 * Not persisted - repopulated from the server every time a horse comes
 * into render range. Evicted when the horse leaves the client level
 * ({@code ClientLifecycleHandler.onEntityLeave}, #200): each entry holds a
 * parsed genome, and every horse ever tracked used to stay until logout.
 */
public final class ClientCoatCache {

    private static final Map<Integer, CoatData> CACHE = new ConcurrentHashMap<>();

    public static void put(int entityId, CoatData coatData) {
        CACHE.put(entityId, coatData);
    }

    public static CoatData get(int entityId) {
        return CACHE.get(entityId);
    }

    /** Drop everything - entity ids are per-world, so this is called on world exit. */
    /** The horse has left this client's level (#200); the server re-sends on tracking start. */
    public static void forget(int entityId) {
        CACHE.remove(entityId);
    }

    public static void clear() {
        CACHE.clear();
    }

    private ClientCoatCache() {
    }
}
