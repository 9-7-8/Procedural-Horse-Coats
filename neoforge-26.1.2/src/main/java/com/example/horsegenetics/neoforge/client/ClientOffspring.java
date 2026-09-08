package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.network.OffspringDataPayload;

import java.util.List;
import java.util.UUID;

/**
 * The client's copy of the last {@link OffspringDataPayload} - one horse's
 * descendants, generation by generation, as the information screen's
 * <b>Offspring</b> tab draws them.
 *
 * <p>Exactly <b>one</b> horse's answer is held at a time, and that is
 * deliberate: the tab refreshes only on a button press, so a cache of several
 * would be a cache of several different ages with nothing on screen saying
 * which. {@link #rootId()} is what the screen checks before drawing - an answer
 * about a different horse is not this horse's, and is treated as no answer.
 */
public final class ClientOffspring {

    private static UUID rootId;
    private static List<OffspringDataPayload.Generation> generations = List.of();

    private ClientOffspring() {
    }

    public static void accept(OffspringDataPayload payload) {
        rootId = payload.rootId();
        generations = payload.generations();
    }

    /** Whose descendants these are, or {@code null} if nothing has been asked for. */
    public static UUID rootId() {
        return rootId;
    }

    /** Empty unless {@code id} is the horse the last answer was about. */
    public static List<OffspringDataPayload.Generation> of(UUID id) {
        return id != null && id.equals(rootId) ? generations : List.of();
    }

    public static void clear() {
        rootId = null;
        generations = List.of();
    }
}
