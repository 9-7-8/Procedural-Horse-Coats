package com.example.horsegenetics.common;

import java.util.Random;

/**
 * A deterministic {@link Rng} built from a single {@code long} seed - the
 * "replay" randomness used when a horse's coat is regenerated. A horse's
 * epigenetic seed is rolled once at birth and stored; every time the skin is
 * rebuilt, each non-deterministic gene derives its own {@code SeededRng}
 * ({@code new SeededRng(epigeneticSeed, gene.key())}) so the same freckles /
 * sock heights come back out.
 *
 * <p>Backed by {@link java.util.Random} - not a Minecraft class, so it stays
 * inside the common module's no-game-dependency rule.
 */
public final class SeededRng implements Rng {

    private final Random random;

    public SeededRng(long seed) {
        this.random = new Random(seed);
    }

    /** Seed derived from a base seed and a namespace string (e.g. a gene key). */
    public SeededRng(long baseSeed, String namespace) {
        this(baseSeed ^ ((long) namespace.hashCode() * 0x9E3779B97F4A7C15L));
    }

    @Override
    public float nextFloat() {
        return random.nextFloat();
    }

    @Override
    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    @Override
    public long nextLong() {
        return random.nextLong();
    }
}
