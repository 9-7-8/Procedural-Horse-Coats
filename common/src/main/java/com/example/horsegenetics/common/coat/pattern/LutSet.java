package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.CommonMaps;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.LutContribution;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

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
        alternates = CommonMaps.copyOf(alternates);
    }

    /** A set with only the natural gradient - the ordinary case, and every pre-LUT caller. */
    public static LutSet of(GradientLut base) {
        return new LutSet(base, CommonMaps.empty());
    }

    /**
     * <b>Every alternate the registry declares</b>, loaded through
     * {@code reader} - which takes a resource path
     * ({@code "textures/coat/lutbluepink.png"}) and returns the LUT behind it,
     * or {@code null} if it cannot.
     *
     * <p>This is what the offline tools use, and it exists because they used to
     * each write the palette list out by hand: four copies of
     * {@code Map.of("bluepink", ...)} in the icon baker, the wiki baker, the
     * sample tool and the golden test, none of which knows when a palette is
     * added. The game has never had that problem - it walks
     * {@link LutContribution#lutResources} - so this is the same walk, in the
     * one place all of them can call. Adding a palette is a row in
     * {@code LutGene.VARIANTS} and nothing else.
     */
    public static LutSet fromRegistry(GradientLut base, Function<String, GradientLut> reader) {
        Map<String, GradientLut> alternates = new LinkedHashMap<>();
        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof LutContribution lut)) {
                continue;
            }
            for (Map.Entry<String, String> e : lut.lutResources().entrySet()) {
                if (alternates.containsKey(e.getKey())) {
                    continue;
                }
                GradientLut loaded = reader.apply(e.getValue());
                if (loaded != null) {
                    alternates.put(e.getKey(), loaded);
                }
            }
        }
        return new LutSet(base, alternates);
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
