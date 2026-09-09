package com.example.horsegenetics.common;

import java.util.Random;

/**
 * A deterministic {@link Rng} built from a single {@code long} seed - the
 * "replay" randomness used when a horse's coat is regenerated. Every allele
 * copy carries an epigenetic seed (rolled once for a founder, inherited
 * unchanged after that); every time the skin is rebuilt, each
 * non-deterministic gene derives its own {@code SeededRng} from the seed of the
 * copy that expresses ({@code CoatBuildContext.epigeneticsFor}) so the same
 * dapples / sock heights come back out.
 *
 * <p>Backed by {@link java.util.Random} - not a Minecraft class, so it stays
 * inside the common module's no-game-dependency rule.
 */
public final class SeededRng implements Rng {

    private final Random random;

    public SeededRng(long seed) {
        this.random = new Random(scramble(seed));
    }

    /** Seed derived from a base seed and a namespace string (e.g. a gene key). */
    public SeededRng(long baseSeed, String namespace) {
        this(baseSeed ^ ((long) namespace.hashCode() * 0x9E3779B97F4A7C15L));
    }

    /**
     * <b>The seed scramble {@link Random} does not do.</b>
     *
     * <p>{@code java.util.Random}'s own seed handling is a single XOR against a
     * constant, which is not a mix: seeds 0, 1, 2... stay neighbours inside the
     * generator, and the draw at a given <i>position</i> in their streams stays
     * correlated across the whole set. Two things fell out of that, and both
     * were recorded as gaps before this existed:
     *
     * <ul>
     *   <li><b>The first {@code nextFloat()} barely moved.</b> Off sequential
     *       seeds it spanned about 0.22 to 0.59 and never reached either end of
     *       [0,1). A test looping {@code new SeededRng(seed++)} for one float
     *       measured a 62/22/16 split as 100/0/0, and it cost most of an hour
     *       to believe the generator rather than the code under test.</li>
     *   <li><b>Position in the stream mattered.</b> Each gene reads its knobs
     *       at its own offset, decided by where it sits in {@code
     *       Genes.codeOrder()} - so moving two unrelated genes' priorities slid
     *       magic jump onto a bad offset and pushed its mean 1.7% high over
     *       3000 sequential seeds. Fifteen standard errors, on a distribution
     *       that had not changed at all.</li>
     * </ul>
     *
     * <p>This is splitmix64's finaliser, which is what {@code SplittableRandom}
     * uses for the same job. It is two multiply-xorshift rounds - cheap, and
     * enough that consecutive seeds produce streams with no usable relationship
     * at any position. <b>It deliberately changes every horse ever generated</b>
     * (every coat drawn from an epigenetic seed comes out different), which is
     * why it was left as the owner's call rather than done as a tidy-up.
     *
     * <p>Written out rather than calling {@code SplittableRandom}: this module
     * targets TeaVM and one day Java 8, so it uses no Java 9+ API and nothing
     * that a browser backend has to supply.
     */
    private static long scramble(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
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
