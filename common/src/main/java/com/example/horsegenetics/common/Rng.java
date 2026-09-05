package com.example.horsegenetics.common;

/**
 * A minimal randomness source. The common module depends on this instead of
 * java.util.Random or Minecraft's RandomSource so that either can be plugged
 * in by the version adapter (e.g. wrapping the world's seeded RandomSource
 * on 26.1.2, or Forge's equivalent on 1.12.2) without common ever importing
 * a Minecraft class.
 */
public interface Rng {

    /** How many uniform draws {@link #nextGaussian()} consumes. Part of a gene's draw-order contract. */
    int GAUSSIAN_SAMPLES = 12;

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

    /**
     * An approximately <b>standard normal</b> draw - mean 0, standard deviation
     * 1 - built from {@value #GAUSSIAN_SAMPLES} uniform draws (the Irwin-Hall
     * construction: sum twelve uniforms and subtract six).
     *
     * <p>Two properties are worth the twelve calls, and both of them are why
     * this is not Box-Muller:
     * <ul>
     *   <li><b>All-{@code 0.5} inputs give exactly {@code 0}</b>, so
     *       {@link MidpointRng} - the "what does this genotype do on average"
     *       source - lands on the distribution's <i>mean</i>. Box-Muller would
     *       hand it a value 1.18 standard deviations below the mean, which is
     *       not a midpoint of anything.</li>
     *   <li><b>The tails are bounded</b> at &plusmn;6&sigma; rather than
     *       infinite. For a trait that decides how big a horse is, a hard bound
     *       on absurdity is a feature: it means the guard clamps exist for
     *       safety and never actually fire.</li>
     * </ul>
     *
     * <p>The approximation is good to about three decimal places out to
     * &plusmn;2&sigma; and understates the far tail, which is the right trade
     * here - a distribution with a real infinite tail would occasionally produce
     * a horse the size of a chunk.
     */
    default double nextGaussian() {
        double sum = 0.0;
        for (int i = 0; i < GAUSSIAN_SAMPLES; i++) {
            sum += nextFloat();
        }
        return sum - GAUSSIAN_SAMPLES / 2.0;
    }

}
