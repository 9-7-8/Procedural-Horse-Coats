package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.coat.CoatData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple client-side store for coat data received via CoatSyncPayload.
 * Not persisted - repopulated from the server every time a horse comes
 * into render range. Entries are never explicitly evicted; for horses
 * this is a non-issue (population is small), but a real mod with many
 * synced entities would want to clean up on entity removal.
 */
public final class ClientCoatCache {

    private static final Map<Integer, CoatData> CACHE = new ConcurrentHashMap<>();

    public static void put(int entityId, CoatData coatData) {
        CACHE.put(entityId, coatData);
    }

    public static CoatData get(int entityId) {
        return CACHE.get(entityId);
    }

    private ClientCoatCache() {
    }
}
