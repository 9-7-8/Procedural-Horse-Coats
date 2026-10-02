package com.example.horsegenetics.common.breed;

import com.example.horsegenetics.common.Rng;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Which breed a converting vanilla undead horse becomes.</b> Every breed whose
 * sheet says {@code "undead_of": pool} is in the pool; of those, the ones that
 * <i>live</i> in the horse's biome come first - a skeleton horse converting in the
 * Nether becomes a Blackened Skeleton Horse - and failing that the ones that live
 * nowhere wild, the Great Valley Skeleton Horse (owner, 2026-10-02). Only if neither
 * exists does the whole pool draw. Uniform within the chosen set, from the caller's
 * seeded rng, so a replayed conversion picks the same breed.
 */
public final class UndeadPools {

    private UndeadPools() {
    }

    /** The pool's breeds, in registry order. */
    public static List<Breed> members(String pool) {
        List<Breed> out = new ArrayList<>();
        for (Breed b : Breeds.all()) {
            if (pool.equals(b.undeadOf())) {
                out.add(b);
            }
        }
        return out;
    }

    /** The breed for a horse of {@code pool} converting in {@code biomeId}, or {@code null} for an empty pool. */
    public static Breed pick(String pool, String biomeId, Rng rng) {
        List<Breed> all = members(pool);
        if (all.isEmpty()) {
            return null;
        }
        List<Breed> local = new ArrayList<>();
        List<Breed> homeless = new ArrayList<>();
        for (Breed b : all) {
            if (biomeId != null && b.biomes().contains(biomeId)) {
                local.add(b);
            } else if (b.biomes().isEmpty()) {
                homeless.add(b);
            }
        }
        List<Breed> from = !local.isEmpty() ? local : (!homeless.isEmpty() ? homeless : all);
        return from.get(Math.min(from.size() - 1, (int) (rng.nextFloat() * from.size())));
    }
}
