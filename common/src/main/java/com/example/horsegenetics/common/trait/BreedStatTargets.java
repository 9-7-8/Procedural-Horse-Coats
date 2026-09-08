package com.example.horsegenetics.common.trait;

import java.util.EnumMap;
import java.util.Map;

/**
 * The per-axis {@link TargetBand}s a breed pins its <b>founders</b> to - read
 * only by {@code BreedFounder}, when it decides what numbers to write on a wild
 * horse of that breed.
 *
 * <p><b>It is not consulted again.</b> This used to be threaded into
 * {@link HorseTraits#resolve} and read on every single body resolution, which
 * meant a breed re-imposed its standard on every horse that carried its label
 * forever - and a cross had the two standards <i>averaged</i>. A Percheron bred
 * to a Falabella produced a foal pulled back toward mid-size no matter which
 * alleles it actually inherited. Now a breed shapes the horses it starts with
 * and then lets go, which is the owner's call: keeping a line to a standard is
 * the player's job, not the game's.
 *
 * <p>{@link #NONE} is the "Unknown breed / no breed" value: every axis absent,
 * so a founder's body-stat loci are left wild and its body is exactly what it
 * was before breeds existed.
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
