package com.example.horsegenetics.neoforge.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The last Social summary the server sent for each horse, keyed by entity id. Fed by
 * {@code HorseSocialSyncPayload} while a horse's information screen is open.
 */
public final class ClientHorseSocialCache {

    public record Social(String role, String roleDescription, String standing, List<String> companions,
                         String rival) {
    }

    private static final Map<Integer, Social> BY_ENTITY = new ConcurrentHashMap<>();

    public static void put(int entityId, Social social) {
        BY_ENTITY.put(entityId, social);
    }

    public static Social get(int entityId) {
        return BY_ENTITY.get(entityId);
    }

    public static void clear() {
        BY_ENTITY.clear();
    }

    private ClientHorseSocialCache() {
    }
}
