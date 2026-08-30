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
}
