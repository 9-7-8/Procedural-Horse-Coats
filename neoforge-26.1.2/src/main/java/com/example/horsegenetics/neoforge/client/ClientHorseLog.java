package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.log.HorseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The client's copy of {@link com.example.horsegenetics.neoforge.network.HorseLogPayload}
 * - what has been happening to the player's horses, as the browser's Log tab
 * reads it.
 *
 * <p>Thinner than {@link ClientHorseRoster}, and for a reason: a roster row has
 * to be parsed into a {@link com.example.horsegenetics.common.horse.HorseListing}
 * on arrival because the table sorts and filters on things the genetic code has
 * to be read to know. A log row is already everything it will ever be - the
 * server decided its order, its wording is a method on the row itself, and the
 * only question the tab asks of it is its kind. So it is kept exactly as it
 * arrived.
 *
 * <p>The list is <b>newest first</b> because the server sent it that way, and
 * nothing here re-sorts: one place decides that order.
 */
public final class ClientHorseLog {

    private static List<HorseEvent> events = List.of();

    /** Bumped on every accepted log, so the tab can tell it needs to relayout. */
    private static int version;

    /** True once a log has arrived, so an empty one can be told from "not asked yet". */
    private static boolean received;

    private ClientHorseLog() {
    }

    public static void accept(List<HorseEvent> rows) {
        events = rows == null ? List.of() : List.copyOf(rows);
        received = true;
        version++;
    }

    /** Every row, newest first. */
    public static List<HorseEvent> all() {
        return events;
    }

    /**
     * The rows of one kind, newest first; {@code null} for every kind, which is
     * the tab's All chip. Filtered out of the list in the order it arrived, so
     * picking a chip cannot change what order anything is in.
     */
    public static List<HorseEvent> of(HorseEvent.Kind kind) {
        if (kind == null) {
            return events;
        }
        List<HorseEvent> out = new ArrayList<>();
        for (HorseEvent event : events) {
            if (event.kind() == kind) {
                out.add(event);
            }
        }
        return out;
    }

    /** How many rows of one kind there are - what a filter chip counts. */
    public static int count(HorseEvent.Kind kind) {
        if (kind == null) {
            return events.size();
        }
        int n = 0;
        for (HorseEvent event : events) {
            if (event.kind() == kind) {
                n++;
            }
        }
        return n;
    }

    public static boolean received() {
        return received;
    }

    public static int version() {
        return version;
    }

    /** Dropped on disconnect, like every other client cache in this package. */
    public static void clear() {
        events = List.of();
        received = false;
        version++;
    }
}
