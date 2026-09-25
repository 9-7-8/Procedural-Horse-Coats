package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.network.HorseDestinationsPayload;

import java.util.List;

/**
 * Client-side hold for the F8 destination lines - the last
 * {@link HorseDestinationsPayload} the server sent, and when it arrived.
 *
 * <h2>Why it expires</h2>
 * The server pushes an empty list when the highlight goes off, which is the
 * normal way the lines stop. The timer is for every other way the pushes can
 * stop without anybody saying so: disconnecting, changing dimension, the server
 * dropping the player out of the toggle set after an error, or the four-minute
 * auto-off racing a packet. None of those send a final empty list, and a debug
 * overlay frozen on stale positions is worse than no overlay - that is the exact
 * shape of the stranded-glow bug this same feature already had once.
 *
 * <p>{@link #STALE_MS} is six pushes' worth, so an ordinary hitch never blinks
 * the lines out, and a real stop clears them inside two seconds.
 */
public final class ClientHorseDestinations {

    /** Six times {@code HorseDestinationDebug.PUSH_INTERVAL}, in milliseconds. */
    private static final long STALE_MS = 3_000L;

    private static volatile List<HorseDestinationsPayload.Entry> entries = List.of();
    private static volatile long receivedAt;

    private ClientHorseDestinations() {
    }

    public static void accept(HorseDestinationsPayload payload) {
        entries = List.copyOf(payload.entries());
        receivedAt = System.currentTimeMillis();
    }

    /** The current lines, or empty when the server has gone quiet. */
    public static List<HorseDestinationsPayload.Entry> all() {
        if (entries.isEmpty()) {
            return List.of();
        }
        if (System.currentTimeMillis() - receivedAt > STALE_MS) {
            entries = List.of();
            return List.of();
        }
        return entries;
    }

    /** Dropped on disconnect, so a new world never inherits the old one's lines. */
    public static void clear() {
        entries = List.of();
        receivedAt = 0L;
    }
}
