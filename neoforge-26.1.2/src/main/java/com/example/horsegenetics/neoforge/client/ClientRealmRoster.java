package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseListing;
import com.example.horsegenetics.neoforge.network.HorseRosterPayload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The client's copy of {@code RealmRosterPayload} - <b>every horse living in
 * the horse realm</b>, as the browser's <i>Horse realm</i> tab reads them.
 *
 * <p>Deliberately a second cache beside {@link ClientHorseRoster} rather than a
 * flag on it: the two answer different questions ("what is in my stable" and
 * "what is in the field"), the same horse can legitimately be in both, and
 * arriving in the realm must not overwrite what the player knows about their own
 * horses. Everything else about it - the parse-once rule, and dropping a row
 * whose code will not parse rather than throwing into the render loop - is that
 * class's, for that class's reasons.
 *
 * <h2>It arrives in batches and it is kept</h2>
 * The field is thousands of horses, so the server sends it as a run of packets
 * and this accumulates them: {@code first} clears, {@code last} marks the run
 * complete. Until then {@link #loading()} is true and the tab says how far it
 * has got, because a table that sat blank for two seconds and then filled would
 * look broken for exactly as long as it takes to read.
 *
 * <p>Once complete it is <b>kept for as long as the player is in the realm</b>
 * (owner, 2026-09-26), and dropped on leaving by
 * {@link ClientLifecycleHandler}. That is the right lifetime rather than a
 * timer: the tab is only reachable from inside the realm, so the cache and the
 * screen have the same natural span, and re-asking costs a full re-encode of
 * every horse in the field. <b>Refresh</b> is how you ask again inside it.
 */
public final class ClientRealmRoster {

    private static final Map<UUID, HorseListing> BY_ID = new LinkedHashMap<>();

    private static int version;
    private static boolean received;
    private static boolean loading;

    private ClientRealmRoster() {
    }

    /**
     * One batch. A run that starts replaces whatever was here - the server only
     * ever starts one when somebody asked for the field fresh.
     */
    public static void accept(List<HorseRosterPayload.Entry> entries, boolean first, boolean last) {
        if (first) {
            BY_ID.clear();
        }
        for (HorseRosterPayload.Entry entry : entries) {
            put(entry);
        }
        loading = !last;
        received = true;
        version++;
    }

    /** Parse once, here, and drop a row whose code will not read rather than throwing. */
    private static void put(HorseRosterPayload.Entry entry) {
        try {
            BY_ID.put(entry.id(), HorseListing.of(
                    entry.id(), entry.firstName(), entry.lastName(), entry.barnName(),
                    entry.breed(), entry.generation(), Genotype.parse(entry.geneticCode()),
                    entry.adult(), entry.tamed(), entry.bond(), entry.inHerd(),
                    entry.loaded(), entry.where(),
                    entry.tamedBy(), entry.bredBy(), entry.hasParents(), entry.gelded()));
        } catch (RuntimeException unparseable) {
            // One bad code must not cost the player the rest of the field.
        }
    }

    /**
     * <b>One horse joined or left the field.</b> The server keeps everyone in
     * the realm in step this way rather than making each of them re-fetch
     * thousands of rows on a timer - see {@code RealmRosterDeltaPayload}.
     *
     * <p>Applied whether or not a full roster has arrived yet. A delta that
     * lands mid-run is simply part of the field, and one that names a horse this
     * client has never heard of is news rather than an error.
     */
    public static void applyDelta(List<HorseRosterPayload.Entry> added, List<UUID> removed) {
        for (UUID id : removed) {
            BY_ID.remove(id);
        }
        for (HorseRosterPayload.Entry entry : added) {
            put(entry);
        }
        version++;
    }

    public static List<HorseListing> all() {
        return List.copyOf(BY_ID.values());
    }

    /** One row by id, or null. What the table's footer reads for its selection. */
    public static HorseListing byId(UUID id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** Has anything arrived at all - i.e. is there something to draw? */
    public static boolean received() {
        return received;
    }

    /** Are there more batches still coming? */
    public static boolean loading() {
        return loading;
    }

    /** How many rows are in hand, which is what the tab counts while filling. */
    public static int size() {
        return BY_ID.size();
    }

    /**
     * Bumped by every batch, so the table rebuilds its rows as the field fills
     * rather than only when the run ends.
     */
    public static int version() {
        return version;
    }

    /** Dropped on disconnect, and on leaving the realm - see the class note. */
    public static void clear() {
        BY_ID.clear();
        received = false;
        loading = false;
        version++;
    }
}
