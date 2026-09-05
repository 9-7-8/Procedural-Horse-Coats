package com.example.horsegenetics.common.trait;

import java.util.EnumMap;
import java.util.Map;

/**
 * The per-axis {@link TargetBand}s a breed pins its horses to. Threaded into
 * {@link HorseTraits#resolve} and read by the four magical body-stat genes
 * through {@link TraitBuilder#breedBand}.
 *
 * <p>{@link #NONE} is the "Unknown breed / no breed" value: every axis absent,
 * so every body-stat gene falls back to its ordinary bounded-Gaussian draw and
 * a horse's body is exactly what it was before breeds existed.
 */
public final class BreedStatTargets {

    public static final BreedStatTargets NONE = new BreedStatTargets(new EnumMap<>(StatAxis.class));

    private final Map<StatAxis, TargetBand> bands;

    private BreedStatTargets(Map<StatAxis, TargetBand> bands) {
        this.bands = bands;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The band for {@code axis}, or {@code null} if this breed does not pin it. */
    public TargetBand band(StatAxis axis) {
        return bands.get(axis);
    }

    public boolean isEmpty() {
        return bands.isEmpty();
    }

    public boolean pins(StatAxis axis) {
        return bands.containsKey(axis);
    }

    /**
     * The per-axis average of two breeds' targets - what a <b>cross</b> of two
     * pure breeds pins. An axis is only carried forward when <i>both</i> parents
     * pin it, so a Friesian &times; Thoroughbred cross keeps no speed band
     * (Friesian never had one) and its speed falls back to the ordinary draw,
     * while a Thoroughbred &times; Arabian cross keeps a speed band midway
     * between the two.
     */
    public static BreedStatTargets average(BreedStatTargets a, BreedStatTargets b) {
        Builder out = builder();
        for (StatAxis axis : StatAxis.values()) {
            TargetBand ba = a.band(axis);
            TargetBand bb = b.band(axis);
            if (ba != null && bb != null) {
                out.band(axis, TargetBand.of((ba.lo() + bb.lo()) / 2.0, (ba.hi() + bb.hi()) / 2.0));
            }
        }
        return out.build();
    }

    public static final class Builder {
        private final Map<StatAxis, TargetBand> bands = new EnumMap<>(StatAxis.class);

        public Builder band(StatAxis axis, TargetBand band) {
            if (band != null) {
                bands.put(axis, band);
            }
            return this;
        }

        public BreedStatTargets build() {
            return bands.isEmpty() ? NONE : new BreedStatTargets(new EnumMap<>(bands));
        }
    }
}
