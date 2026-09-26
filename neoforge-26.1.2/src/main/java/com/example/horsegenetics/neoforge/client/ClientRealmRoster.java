package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseListing;
import com.example.horsegenetics.neoforge.network.HorseRosterPayload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The client's copy of {@code RealmRosterPayload} - <b>every horse standing in
 * the horse realm</b>, as the browser's <i>Horse realm</i> tab reads them.
 *
 * <p>Deliberately a second cache beside {@link ClientHorseRoster} rather than a
 * flag on it: the two answer different questions ("what is in my stable" and
 * "what is in the field"), the same horse can legitimately be in both, and
 * arriving in the realm must not overwrite what the player knows about their own
 * horses. Everything else about it - the parse-once rule, and dropping a row
 * whose code will not parse rather than throwing into the render loop - is that
 * class's, for that class's reasons.
 */
public final class ClientRealmRoster {

    private static final Map<UUID, HorseListing> BY_ID = new LinkedHashMap<>();

    private static int version;
    private static boolean received;
    private static boolean truncated;

    private ClientRealmRoster() {
    }

    public static void accept(List<HorseRosterPayload.Entry> entries) {
        BY_ID.clear();
        for (HorseRosterPayload.Entry entry : entries) {
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
        truncated = entries.size() >= HorseRosterPayload.MAX_ENTRIES;
        received = true;
        version++;
    }

    public static List<HorseListing> all() {
        return List.copyOf(BY_ID.values());
    }

    public static boolean received() {
        return received;
    }

    public static boolean truncated() {
        return truncated;
    }

    public static int version() {
        return version;
    }

    /** Dropped on disconnect, like every other client cache in this package. */
    public static void clear() {
        BY_ID.clear();
        received = false;
        truncated = false;
        version++;
    }
}
