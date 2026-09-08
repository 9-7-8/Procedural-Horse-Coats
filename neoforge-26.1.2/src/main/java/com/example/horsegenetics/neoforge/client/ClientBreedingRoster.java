package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.network.BreedingRosterPayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The client's copy of {@link BreedingRosterPayload} - the player's own horses,
 * as the Breeding preview tab's two pickers read them.
 *
 * <p>Each entry's genotype is parsed <b>once, here</b>, on arrival: the tab
 * redraws every frame and re-parsing a code per row per frame is the kind of
 * waste that only shows up on somebody else's machine. A code that will not
 * parse is dropped with the rest of its row rather than throwing into the
 * render loop.
 */
public final class ClientBreedingRoster {

    /** One horse, with its genotype already parsed. */
    public record Horse(UUID id, String name, String breed, int generation, Genotype genotype) {

        public Sex sex() {
            return genotype.sex();
        }
    }

    private static final Map<UUID, Horse> BY_ID = new LinkedHashMap<>();

    /** Bumped on every accepted roster, so a screen can tell it needs to relayout. */
    private static int version;

    /** True once a roster has arrived, so an empty list can be told from "not asked yet". */
    private static boolean received;

    /** Was the last roster cut off at the packet cap? */
    private static boolean truncated;

    private ClientBreedingRoster() {
    }

    public static void accept(List<BreedingRosterPayload.Entry> entries) {
        BY_ID.clear();
        for (BreedingRosterPayload.Entry entry : entries) {
            try {
                BY_ID.put(entry.id(), new Horse(entry.id(), entry.name(), entry.breed(),
                        entry.generation(), Genotype.parse(entry.geneticCode())));
            } catch (RuntimeException unparseable) {
                // One bad code must not cost the player the rest of the stable.
            }
        }
        truncated = entries.size() >= BreedingRosterPayload.MAX_ENTRIES;
        received = true;
        version++;
    }

    public static List<Horse> all() {
        return List.copyOf(BY_ID.values());
    }

    /** The mares, or the stallions - the two sides of the picker. */
    public static List<Horse> of(Sex sex) {
        List<Horse> out = new ArrayList<>();
        for (Horse horse : BY_ID.values()) {
            if (horse.sex() == sex) {
                out.add(horse);
            }
        }
        return List.copyOf(out);
    }

    public static Horse byId(UUID id) {
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
