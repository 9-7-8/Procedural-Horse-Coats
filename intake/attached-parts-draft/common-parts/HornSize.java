package com.example.horsegenetics.common.parts;

/**
 * Named horn lengths, in model units (a horse's head box is 6x5x7, so STANDARD is a
 * little longer than the whole head is tall). {@link HornGenerator} takes any float
 * length, so an allele can pick a class today and an epigenome number can slide
 * between them later without touching the generator.
 */
public enum HornSize {
    NUB(3.0f), SHORT(6.0f), STANDARD(10.0f), LONG(15.0f), NARWHAL(24.0f);

    public final float length;

    HornSize(float length) {
        this.length = length;
    }

    /** {@code s} in [0,1] -> a length between a nub and a narwhal, geometric so each step reads as "bigger". */
    public static float lengthFor(double s) {
        double c = Math.max(0.0, Math.min(1.0, s));
        return (float) (NUB.length * Math.pow(NARWHAL.length / NUB.length, c));
    }
}
