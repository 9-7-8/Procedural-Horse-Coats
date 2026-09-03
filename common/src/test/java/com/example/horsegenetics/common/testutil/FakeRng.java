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
    private final Deque<Integer> ints = new ArrayDeque<>();
    private final Deque<Long> longs = new ArrayDeque<>();

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

    public FakeRng ints(int... values) {
        for (int v : values) {
            ints.add(v);
        }
        return this;
    }

    public FakeRng longs(long... values) {
        for (long v : values) {
            longs.add(v);
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

    @Override
    public int nextInt(int bound) {
        if (ints.isEmpty()) {
            throw new IllegalStateException("FakeRng.nextInt() called more times than values provided");
        }
        int v = ints.removeFirst();
        if (v < 0 || v >= bound) {
            throw new IllegalStateException(
                    "FakeRng.nextInt(" + bound + ") next queued value " + v + " is out of range");
        }
        return v;
    }

    @Override
    public long nextLong() {
        if (longs.isEmpty()) {
            throw new IllegalStateException("FakeRng.nextLong() called more times than values provided");
        }
        return longs.removeFirst();
    }

    /** Fails if any queued value was left unconsumed - the "consumed exactly N draws" check. */
    public void assertExhausted() {
        if (!floats.isEmpty() || !booleans.isEmpty() || !ints.isEmpty() || !longs.isEmpty()) {
            throw new AssertionError("FakeRng had unused values: floats=" + floats.size()
                    + " booleans=" + booleans.size() + " ints=" + ints.size() + " longs=" + longs.size());
        }
    }
}
