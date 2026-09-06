package com.example.horsegenetics.common.coat.pattern;

import java.util.Map;

/**
 * The set of colour lookups the {@link CoatTextureComposer} may resolve pigment
 * through in phase 2 - one <b>base</b> {@link GradientLut} (the natural
 * red/black gradient) plus zero or more <b>alternates</b>, keyed by a short
 * string.
 *
 * <p>Almost every horse resolves against the base. A gene implementing
 * {@link com.example.horsegenetics.common.genetics.LutContribution} - the
 * {@code LUT} locus - can name an alternate key for a horse homozygous for one
 * of its variant alleles, and phase 2 then samples that LUT instead. An
 * unknown key falls back to the base, so a save that predates a LUT texture
 * still renders.
 *
 * <p>Pure data: the {@code int[]}s are loaded by the game module (or a build
 * tool) and handed in, exactly as {@link GradientLut} is.
 */
public record LutSet(GradientLut base, Map<String, GradientLut> alternates) {

    public LutSet {
        alternates = Map.copyOf(alternates);
    }

    /** A set with only the natural gradient - the ordinary case, and every pre-LUT caller. */
    public static LutSet of(GradientLut base) {
        return new LutSet(base, Map.of());
    }

    /** The LUT under {@code key}, or the base when {@code key} is {@code null} or unknown. */
    public GradientLut resolve(String key) {
        if (key == null) {
            return base;
        }
        GradientLut alt = alternates.get(key);
        return alt != null ? alt : base;
    }
}
