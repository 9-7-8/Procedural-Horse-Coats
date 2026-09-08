package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.HorseListing;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.network.HorseRosterPayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The client's copy of {@link HorseRosterPayload} - the player's own horses, as
 * the browser's <i>My horses</i> table and the <i>Breeding preview</i> pickers
 * read them.
 *
 * <p>Each entry is turned into a {@link HorseListing} <b>once, here</b>, on
 * arrival: that parses the genetic code and resolves the horse's four body
 * numbers, its coat description and its disorders, all of which the table sorts
 * and filters on. A table redraws every frame and a sort compares thousands of
 * times, so doing any of that per row per frame is the kind of waste that only
 * shows up on somebody else's machine. A code that will not parse is dropped
 * with the rest of its row rather than throwing into the render loop.
 */
public final class ClientHorseRoster {

    private static final Map<UUID, HorseListing> BY_ID = new LinkedHashMap<>();

    /** Bumped on every accepted roster, so a screen can tell it needs to relayout. */
    private static int version;

    /** True once a roster has arrived, so an empty list can be told from "not asked yet". */
    private static boolean received;

    /** Was the last roster cut off at the packet cap? */
    private static boolean truncated;

    private ClientHorseRoster() {
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
                        entry.tamedBy(), entry.bredBy(), entry.hasParents()));
            } catch (RuntimeException unparseable) {
                // One bad code must not cost the player the rest of the stable.
            }
        }
        truncated = entries.size() >= HorseRosterPayload.MAX_ENTRIES;
        received = true;
        version++;
    }

    public static List<HorseListing> all() {
        return List.copyOf(BY_ID.values());
    }

    /** The mares, or the stallions - the two sides of the breeding picker. */
    public static List<HorseListing> of(Sex sex) {
        List<HorseListing> out = new ArrayList<>();
        for (HorseListing horse : BY_ID.values()) {
            if (horse.sex() == sex) {
                out.add(horse);
            }
        }
        return List.copyOf(out);
    }

    public static HorseListing byId(UUID id) {
        return id == null ? null : BY_ID.get(id);
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
