package com.example.horsegenetics.common;

/**
 * The <b>average draw</b>: an {@link Rng} that always returns the middle of
 * every range - {@code 0.5} from {@link #nextFloat()}, {@code bound / 2} from
 * {@link #nextInt}, {@code false}, {@code 0}.
 *
 * <p>It is not random and is not pretending to be. It exists for the one
 * question that has no horse attached to it: "what does <i>this genotype</i>
 * do?", asked by a punnett display, a wiki table or a unit test.
 *
 * <p><b>It is no longer the epigenetics path.</b> It used to be: a trait asked
 * about a genotype with no epigenome got an {@link Rng} that returned 0.5 from
 * every draw, which landed the gene on the middle of its range. Epigenetics are
 * stored values now, so the honest midpoint is the one the value's own schema
 * declares ({@code EpiSchema.midpoint()}) - and unlike 0.5 through a
 * distribution, it is right even when the distribution is skewed.
 *
 * <p>What is left is its use as a <b>zero-sigma Gaussian</b>: all-{@code 0.5}
 * inputs make {@link Rng#nextGaussian()} return exactly {@code 0}, which is the
 * property that made this class worth having in the first place and is what the
 * bounded-Gaussian tests pin.
 */
public final class MidpointRng implements Rng {

    public static final MidpointRng INSTANCE = new MidpointRng();

    private MidpointRng() {
    }

    @Override
    public float nextFloat() {
        return 0.5f;
    }

    @Override
    public boolean nextBoolean() {
        return false;
    }

    @Override
    public int nextInt(int bound) {
        return bound / 2;
    }

    @Override
    public long nextLong() {
        return 0L;
    }
}
