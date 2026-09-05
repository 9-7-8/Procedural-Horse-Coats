package com.example.horsegenetics.common;

/**
 * The <b>average draw</b>: an {@link Rng} that always returns the middle of
 * every range - {@code 0.5} from {@link #nextFloat()}, {@code bound / 2} from
 * {@link #nextInt}, {@code false}, {@code 0}.
 *
 * <p>It is not random and is not pretending to be. It exists for the one
 * question that has no horse attached to it: "what does <i>this genotype</i>
 * do?", asked by a punnett display, a wiki table or a unit test, where there is
 * no epigenome to read and inventing one would answer about a horse nobody
 * owns. {@link com.example.horsegenetics.common.trait.HorseTraits#resolve(
 * com.example.horsegenetics.common.genetics.Genotype)} uses it so an
 * epigenetically-varying trait reports its midpoint rather than a number drawn
 * off a seed the caller never supplied.
 *
 * <p><b>Never use it to build a horse.</b> A real horse's epigenetic draws come
 * from the expressing allele copy's seed ({@code CoatBuildContext
 * .epigeneticsFor}), which is stored, heritable and reproducible; this one would
 * make every horse identical.
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
