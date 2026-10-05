package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.Genome;

/**
 * <b>The coat a pair of saved codes describes, parsed once.</b> A screen or an
 * item renderer that holds only a horse's codes - a transfer deed, a pedigree
 * record, a foal on the Offspring tab - would otherwise run
 * {@link Genome#parse} on every frame it draws them, and then rebuild every key
 * {@link CoatData} memoises, because a fresh {@code CoatData} starts with none.
 * Handing back the same instance for the same codes is what lets those memos
 * pay off off the entity path too (#206).
 *
 * <p>A least-recently-used cache counted in entries: {@link TexelBudgetCache}
 * with a cost of one, so there is one LRU in the mod rather than two. The bound
 * is only there to stop a long session growing it - a player sees a handful of
 * deeds and pedigrees at a time, and a miss costs one parse.
 *
 * <p>Safe to share because a {@code CoatData} is immutable. A code that will
 * not parse throws out of {@link #coatOf} and is not admitted, so the next
 * frame asks again - exactly what the uncached callers did. {@link #clear()}
 * belongs wherever the gene registry can change underneath it: a cached coat
 * holds the gene instances it was parsed against.
 */
public final class CoatCodeCache {

    /** The two codes a horse is saved as - the cache key. */
    private record Codes(String geneticCode, String epigenomeCode) {
    }

    private final TexelBudgetCache<Codes, CoatData> entries;

    /** @param capacity how many coats to keep; must be positive */
    public CoatCodeCache(int capacity) {
        this.entries = new TexelBudgetCache<Codes, CoatData>(capacity, coat -> 1L, (codes, coat) -> {
        });
    }

    /**
     * The coat for these codes - the same instance every time while it stays
     * cached.
     *
     * @throws RuntimeException if the codes do not parse; nothing is cached then
     */
    public CoatData coatOf(String geneticCode, String epigenomeCode) {
        return entries.get(new Codes(geneticCode, epigenomeCode),
                codes -> new CoatData(Genome.parse(codes.geneticCode(), codes.epigenomeCode())));
    }

    /** How many coats are held. */
    public int size() {
        return entries.size();
    }

    /** Forget every coat. */
    public void clear() {
        entries.clear();
    }
}
