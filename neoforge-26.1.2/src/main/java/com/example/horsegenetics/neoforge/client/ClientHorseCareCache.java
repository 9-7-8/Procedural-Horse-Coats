package com.example.horsegenetics.neoforge.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side mirror of each horse's {@code HorseCareAttachment}, but only the
 * two things the inventory-screen panel shows: the <b>bond</b> number and
 * whether the horse is <b>in a herd</b>. Fed by {@code HorseCareSyncPayload}
 * (sent on change and on start-tracking), keyed by entity id, wiped on logout
 * like {@link ClientCoatCache}.
 */
public final class ClientHorseCareCache {

    public record Care(int bond, boolean inHerd) {}

    private static final Map<Integer, Care> BY_ENTITY = new ConcurrentHashMap<>();

    public static void put(int entityId, int bond, boolean inHerd) {
        BY_ENTITY.put(entityId, new Care(bond, inHerd));
    }

    public static Care get(int entityId) {
        return BY_ENTITY.get(entityId);
    }

    public static void clear() {
        BY_ENTITY.clear();
    }

    private ClientHorseCareCache() {
    }
}
