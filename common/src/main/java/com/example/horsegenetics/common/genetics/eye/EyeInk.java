package com.example.horsegenetics.common.genetics.eye;

import com.example.horsegenetics.common.genetics.epi.EpiValues;

/**
 * <b>A hue and the colour it came out as on this horse</b> - the pair every
 * painted part of an eye is described by.
 *
 * <p>The two are kept together rather than the rgb alone because the painter
 * needs both: the number to paint with, and the hue to ask whether it should be
 * painting at all ({@link EyeHue#INVISIBLE}) or whether this horse needs a
 * texture of its own ({@link EyeHue#CHAOS}).
 *
 * @param hue the allele's hue
 * @param rgb what it resolved to, {@code 0xRRGGBB} - meaningless when the hue
 *            does not {@link EyeHue#paints paint}
 */
public record EyeInk(EyeHue hue, int rgb) {

    /** A fixed hue, resolved against no horse in particular. */
    public static EyeInk of(EyeHue hue) {
        return new EyeInk(hue, hue.fixedRgb());
    }

    /** A hue resolved against one allele copy's values - the chaos path. */
    public static EyeInk of(EyeHue hue, EpiValues epi) {
        return new EyeInk(hue, hue.rgb(epi));
    }

    public boolean paints() {
        return hue.paints();
    }

    public boolean varies() {
        return hue.varies();
    }
}
