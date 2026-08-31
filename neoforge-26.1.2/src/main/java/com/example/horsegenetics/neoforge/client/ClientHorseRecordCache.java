package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.horse.HorseRecord;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client-side store of {@link HorseRecord}s received from the server. Two
 * views: by entity id (for the horse you're looking at / riding) and by
 * record UUID (for family-tree lookups). Rebuilt from network traffic every
 * session, like {@link ClientCoatCache}.
 */
public final class ClientHorseRecordCache {

    private static final Map<Integer, HorseRecord> BY_ENTITY = new ConcurrentHashMap<>();
    private static final Map<UUID, HorseRecord> BY_ID = new ConcurrentHashMap<>();
    private static final AtomicInteger TREE_VERSION = new AtomicInteger();

    public static void put(int entityId, HorseRecord record) {
        BY_ENTITY.put(entityId, record);
        BY_ID.put(record.id(), record);
    }

    public static HorseRecord get(int entityId) {
        return BY_ENTITY.get(entityId);
    }

    public static HorseRecord byId(UUID id) {
        return BY_ID.get(id);
    }

    /** Merge a family-tree response and bump the version so an open screen rebuilds. */
    public static void acceptTreeData(List<HorseRecord> records) {
        for (HorseRecord record : records) {
            BY_ID.put(record.id(), record);
        }
        TREE_VERSION.incrementAndGet();
    }

    public static int treeVersion() {
        return TREE_VERSION.get();
    }

    /** Drop everything - entity ids and per-world records don't carry between worlds. */
    public static void clear() {
        BY_ENTITY.clear();
        BY_ID.clear();
        TREE_VERSION.incrementAndGet();
    }

    private ClientHorseRecordCache() {
    }
}
