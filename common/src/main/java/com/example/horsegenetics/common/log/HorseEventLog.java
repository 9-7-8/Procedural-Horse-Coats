package com.example.horsegenetics.common.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>Every player's horse history, bounded and deduplicated.</b> The store
 * behind the browser's Log tab.
 *
 * <p>One log per <i>owner</i>, not one per horse. A horse's own story is its
 * record and its pedigree, which already exist; this answers the other
 * question - <i>what has been happening to my stable</i> - and that question is
 * asked by a person, so a person is what it is keyed by. It follows that a
 * horse sold away stays in the seller's log and appears in the buyer's from the
 * day they got it, which is what both of them would expect and what a per-horse
 * log could not do.
 *
 * <h2>Bounded, because this grows forever otherwise</h2>
 * A breeding operation of thirty mares generates a cover row per mare per heat,
 * a birth row per foal, and a death row for every one that does not make it. At
 * {@link #MAX_PER_OWNER} the oldest row falls off the end. That is a real loss
 * and it is the right one: the log is <i>what has been happening</i>, and
 * nobody reads to the bottom of an unbounded one. The pedigree is the permanent
 * record and is not bounded.
 *
 * <h2>Deduplication, and why it lives here</h2>
 * Two different things both produce repeats. A refused cover is re-evaluated
 * every two seconds for as long as the mare is in heat, so a crowded paddock
 * would write the same row a thousand times a day. And a death passes through
 * several handlers, any of which might report it. Rather than teach each call
 * site its own throttle - which is how one of them ends up without one - a
 * repeat of the <b>same happening</b> ({@link HorseEvent#sameHappening}) within
 * {@link #DEDUPE_TICKS} is dropped here, once, where a test can see it.
 *
 * <p>The window is deliberately {@link com.example.horsegenetics.common.repro.CoverNotice#QUIET_TICKS}:
 * the log and the chat line then agree about what counts as news, so a player
 * who saw one line does not find three rows, and a player who saw none finds
 * the one they missed.
 *
 * <p>Note what the rule does <b>not</b> collapse: two foals born five minutes
 * apart are two rows, because a birth is keyed by the <i>foal</i> and those are
 * two different horses. The rule is about one happening reported twice, never
 * about two happenings that look alike.
 */
public final class HorseEventLog {

    /**
     * How many rows one player keeps. Two hundred is roughly a season of a
     * serious breeding operation, and comfortably inside one packet at the
     * sizes {@link HorseEvent} carries.
     */
    public static final int MAX_PER_OWNER = 200;

    /**
     * A repeat of the same happening inside this many ticks is the same
     * happening - five minutes, matching the chat notice's quiet period.
     */
    public static final long DEDUPE_TICKS = 6_000L;

    /** Oldest first, which is the cheap end to append to and to drop from. */
    private final Map<UUID, List<HorseEvent>> byOwner = new LinkedHashMap<>();

    /** One row, and whose it is. The shape the persistence layer stores. */
    public record Owned(UUID owner, HorseEvent event) {
    }

    public HorseEventLog() {
    }

    /** Rebuild from a {@link #snapshot()}, in the order it was taken. */
    public HorseEventLog(List<Owned> entries) {
        if (entries == null) {
            return;
        }
        for (Owned owned : entries) {
            if (owned == null || owned.owner() == null || owned.event() == null) {
                continue;
            }
            rows(owned.owner()).add(owned.event());
        }
        for (List<HorseEvent> rows : byOwner.values()) {
            trim(rows);
        }
    }

    private List<HorseEvent> rows(UUID owner) {
        List<HorseEvent> rows = byOwner.get(owner);
        if (rows == null) {
            rows = new ArrayList<>();
            byOwner.put(owner, rows);
        }
        return rows;
    }

    private static void trim(List<HorseEvent> rows) {
        while (rows.size() > MAX_PER_OWNER) {
            rows.remove(0);
        }
    }

    /**
     * Write one row, unless it is a repeat.
     *
     * @return {@code true} if it was kept - which is what tells a caller whether
     *         anything the client is showing has changed
     */
    public boolean add(UUID owner, HorseEvent event) {
        if (owner == null || event == null) {
            return false;
        }
        List<HorseEvent> rows = rows(owner);
        // Backwards, and only as far as the window reaches: the rows are in tick
        // order, so the first one too old to be a repeat means every row before
        // it is too old as well.
        for (int i = rows.size() - 1; i >= 0; i--) {
            HorseEvent seen = rows.get(i);
            long since = event.at() - seen.at();
            if (since >= DEDUPE_TICKS || since < 0L) {
                break;
            }
            if (seen.sameHappening(event)) {
                return false;
            }
        }
        rows.add(event);
        trim(rows);
        return true;
    }

    /**
     * <b>One player's log, newest first.</b> That order is the feature, not a
     * convenience: a log you have to scroll to the bottom of to see what just
     * happened is a log nobody opens twice.
     */
    public List<HorseEvent> of(UUID owner) {
        List<HorseEvent> rows = byOwner.get(owner);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<HorseEvent> out = new ArrayList<>(rows);
        Collections.reverse(out);
        return out;
    }

    /**
     * One player's log of one kind, newest first - the Log tab's filter.
     * {@code null} means every kind, which is the tab's All chip.
     *
     * <p>Filtered from the same list in the same order rather than by re-sorting
     * a subset, so picking a chip can never change what order anything is in.
     */
    public List<HorseEvent> of(UUID owner, HorseEvent.Kind kind) {
        if (kind == null) {
            return of(owner);
        }
        List<HorseEvent> out = new ArrayList<>();
        for (HorseEvent event : of(owner)) {
            if (event.kind() == kind) {
                out.add(event);
            }
        }
        return out;
    }

    /** How many rows this player has kept. */
    public int size(UUID owner) {
        List<HorseEvent> rows = byOwner.get(owner);
        return rows == null ? 0 : rows.size();
    }

    /** Drop one player's log entirely. Nothing in the game calls this; tests do. */
    public void forget(UUID owner) {
        byOwner.remove(owner);
    }

    /**
     * Every row with its owner, oldest first within each player - what the
     * persistence codec writes and {@link #HorseEventLog(List)} reads back.
     */
    public List<Owned> snapshot() {
        List<Owned> out = new ArrayList<>();
        for (Map.Entry<UUID, List<HorseEvent>> entry : byOwner.entrySet()) {
            for (HorseEvent event : entry.getValue()) {
                out.add(new Owned(entry.getKey(), event));
            }
        }
        return out;
    }

    public boolean isEmpty() {
        for (List<HorseEvent> rows : byOwner.values()) {
            if (!rows.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
