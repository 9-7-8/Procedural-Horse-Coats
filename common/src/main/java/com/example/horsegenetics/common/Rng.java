package com.example.horsegenetics.common;

/**
 * A minimal randomness source. The common module depends on this instead of
 * java.util.Random or Minecraft's RandomSource so that either can be plugged
 * in by the version adapter (e.g. wrapping the world's seeded RandomSource
 * on 26.1.2, or Forge's equivalent on 1.12.2) without common ever importing
 * a Minecraft class.
 */
public interface Rng {

    /** Returns a float in [0.0, 1.0). */
    float nextFloat();

    /** Returns true or false with equal probability. */
    boolean nextBoolean();

    /**
     * Returns an int uniformly distributed in {@code [0, bound)}.
     * {@code bound} must be positive. Used for picking a random element out of
     * a list (e.g. name-word tables).
     */
    int nextInt(int bound);

    /**
     * Returns a uniformly distributed {@code long} across the full 64-bit range.
     * Used to roll the <b>epigenetic seed</b> carried by one allele copy - the
     * value that seeds that gene's non-deterministic coat work, so the skin
     * regenerates identically every session and a foal that inherits the copy
     * inherits the look (see {@code SeededRng} /
     * {@code genetics.AlleleEpigenetics} / {@code genetics.Epigenome}).
     */
    long nextLong();
}
