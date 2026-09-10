package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.progress.ProgressTask;

import java.util.Set;

/**
 * The client's copy of {@code HorseProgressData} for this player - what the
 * checklist draws from. Pushed by {@code ProgressSyncPayload}; never asked for,
 * and never authoritative.
 */
public final class ClientProgress {

    private static volatile Set<String> done = Set.of();

    private ClientProgress() {
    }

    public static void accept(java.util.List<String> ids) {
        done = Set.copyOf(ids);
    }

    public static boolean isDone(ProgressTask task) {
        return done.contains(task.id());
    }

    public static int count() {
        return done.size();
    }

    /** Dropped on disconnect - progress belongs to a save. */
    public static void clear() {
        done = Set.of();
    }
}
