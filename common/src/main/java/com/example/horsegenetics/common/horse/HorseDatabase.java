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
     * Drop the record for {@code id}, if there is one; returns whether there
     * was. Used when a horse is deleted outright rather than dying (the horse
     * dimension clears its gallery on exit), so a throwaway horse's record
     * doesn't sit in the save forever. Records referencing a forgotten horse as
     * a parent are left alone - {@link #ancestorsOf} already skips ancestors it
     * can't find.
     */
    boolean forget(UUID id);

    /**
     * How many stored records list exactly {@code parentA} and {@code parentB}
     * as their two parents (order-independent). Used to vary foal names by how
     * many foals a pairing has already produced.
     */
    int offspringCount(UUID parentA, UUID parentB);

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

    /**
     * Descendants of {@code id}, <b>grouped by generation</b>: index 0 is the
     * horse's foals, index 1 its grandfoals, and so on, up to {@code depth}
     * generations.
     *
     * <ul>
     *   <li>The horse itself is never included.</li>
     *   <li>Each descendant appears at most once and in the <b>earliest</b>
     *       generation that reaches it, so a line that folds back on itself -
     *       a mare bred to her own grandson - lists each horse once rather
     *       than looping.</li>
     *   <li>A generation with nobody in it ends the list: there is nothing
     *       below an empty one.</li>
     *   <li>{@code depth <= 0} returns an empty list.</li>
     * </ul>
     *
     * <p>Grouped rather than flat because that is the only shape the answer is
     * useful in - "who came from this horse" is a question about generations,
     * and flattening it would throw away the one thing the caller then has to
     * reconstruct.
     */
    List<List<HorseRecord>> descendantsOf(UUID id, int depth);
}
