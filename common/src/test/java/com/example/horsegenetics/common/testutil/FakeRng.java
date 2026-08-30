package com.example.horsegenetics.common.testutil;

import com.example.horsegenetics.common.Rng;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Deterministic {@link Rng} for tests: hand it the exact sequence of values
 * you want {@code nextFloat()} / {@code nextBoolean()} to return, in order.
 * Running past the end of a sequence fails loudly rather than guessing.
 */
public final class FakeRng implements Rng {

    private final Deque<Float> floats = new ArrayDeque<>();
    private final Deque<Boolean> booleans = new ArrayDeque<>();

    public FakeRng floats(float... values) {
        for (float v : values) {
            floats.add(v);
        }
        return this;
    }

    public FakeRng booleans(boolean... values) {
        for (boolean v : values) {
            booleans.add(v);
        }
        return this;
    }

    @Override
    public float nextFloat() {
        if (floats.isEmpty()) {
            throw new IllegalStateException("FakeRng.nextFloat() called more times than values provided");
        }
        return floats.removeFirst();
    }

    @Override
    public boolean nextBoolean() {
        if (booleans.isEmpty()) {
            throw new IllegalStateException("FakeRng.nextBoolean() called more times than values provided");
        }
        return booleans.removeFirst();
    }
}
