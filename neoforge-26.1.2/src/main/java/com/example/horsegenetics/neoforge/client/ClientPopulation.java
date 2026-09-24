package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.network.PopulationDataPayload;

import java.util.List;

/**
 * The client's copy of the last {@link PopulationDataPayload} - every horse in
 * the world, as the family overview draws them.
 *
 * <p>One answer at a time, like {@link ClientOffspring}, and for the same
 * reason: it is asked for on a button press, so a cache of several would be a
 * cache of several different ages with nothing on screen saying which.
 * {@link #version()} is bumped on arrival so an open screen knows to lay itself
 * out again rather than diffing a list of thousands every frame.
 */
public final class ClientPopulation {

    private static List<PopulationDataPayload.Entry> entries = List.of();
    private static boolean truncated;
    private static boolean received;
    private static int version;

    private ClientPopulation() {
    }

    public static void accept(PopulationDataPayload payload) {
        entries = payload.entries();
        truncated = payload.truncated();
        received = true;
        version++;
    }

    public static List<PopulationDataPayload.Entry> entries() {
        return entries;
    }

    /** Was the world's table bigger than one payload may carry? */
    public static boolean truncated() {
        return truncated;
    }

    /** True once an answer has arrived, so "no horses" can be told from "not asked yet". */
    public static boolean received() {
        return received;
    }

    public static int version() {
        return version;
    }

    /** Dropped on disconnect, like every other client cache in this package. */
    public static void clear() {
        entries = List.of();
        truncated = false;
        received = false;
        version++;
    }
}
