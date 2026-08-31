package com.example.horsegenetics.common.horse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores {@link HorseRecord}s and answers ancestry queries. Layer-1: no game
 * dependency. The integration layer wraps an implementation of this behind
 * NeoForge's SavedData rather than reimplementing storage.
 */
public interface HorseDatabase {

    /** Insert or replace the record keyed by {@code horse.id()}. */
    void record(HorseRecord horse);

    Optional<HorseRecord> lookup(UUID id);

    /**
     * Ancestors of {@code id}, nearest generation first (parents, then
     * grandparents, ...), up to {@code depth} generations.
     *
     * <ul>
     *   <li>The horse itself is never included.</li>
     *   <li>Each ancestor appears at most once, even under inbreeding.</li>
     *   <li>Ancestors referenced by id but absent from the database are skipped
     *       (their own parents, if unknown through them, are not explored).</li>
     *   <li>{@code depth <= 0} returns an empty list.</li>
     * </ul>
     */
    List<HorseRecord> ancestorsOf(UUID id, int depth);
}
