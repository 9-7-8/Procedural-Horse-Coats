package com.example.horsegenetics.common.coat;

import com.example.horsegenetics.common.genetics.CoatPhenotype;

/**
 * The fully-resolved, ready-to-render description of one horse's coat.
 * This is the abstraction boundary: the common module produces these,
 * and every version adapter's job is to turn a CoatData into pixels
 * however that Minecraft version wants (texture layer compositing,
 * dynamic textures, whatever). Nothing below this class knows Minecraft
 * exists; nothing above the version adapter needs to know genetics exist.
 *
 * <p>{@code legBlackHeight} is only meaningful when phenotype == BAY, and
 * is otherwise unused (0.0). It's a single value applied uniformly to all
 * four legs for this first pass - per-leg variation is a natural follow-up.
 * Range is [0.0, 1.0]: 0.0 = black barely creeps above the hoof, 1.0 = black
 * comes all the way up to the body.
 */
public final class CoatData {

    private final CoatPhenotype phenotype;
    private final float legBlackHeight;

    private CoatData(CoatPhenotype phenotype, float legBlackHeight) {
        this.phenotype = phenotype;
        this.legBlackHeight = legBlackHeight;
    }

    public static CoatData solid(CoatPhenotype phenotype) {
        if (phenotype == CoatPhenotype.BAY) {
            throw new IllegalArgumentException("BAY requires a leg black height - use CoatData.bay(height)");
        }
        return new CoatData(phenotype, 0f);
    }

    public static CoatData bay(float legBlackHeight) {
        if (legBlackHeight < 0f || legBlackHeight > 1f) {
            throw new IllegalArgumentException("legBlackHeight must be in [0.0, 1.0], got: " + legBlackHeight);
        }
        return new CoatData(CoatPhenotype.BAY, legBlackHeight);
    }

    /** Reconstructs a CoatData from persisted/synced primitives. Used by the storage layer. */
    public static CoatData fromRaw(CoatPhenotype phenotype, float legBlackHeight) {
        return phenotype == CoatPhenotype.BAY ? bay(legBlackHeight) : solid(phenotype);
    }

    public CoatPhenotype phenotype() {
        return phenotype;
    }

    public float legBlackHeight() {
        return legBlackHeight;
    }

    @Override
    public String toString() {
        return phenotype == CoatPhenotype.BAY
                ? "CoatData[BAY, legBlackHeight=" + legBlackHeight + "]"
                : "CoatData[" + phenotype + "]";
    }
}
