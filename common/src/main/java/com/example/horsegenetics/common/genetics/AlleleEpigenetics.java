package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;

/**
 * The heritable payload that travels with <b>one copy of one allele</b>: its
 * {@link #priority()} and its {@link #epigeneticSeed()}.
 *
 * <p>Epigenetics are <b>tied to the allele</b>, not to the horse - a horse
 * carrying {@code A/a} has an epigenetic seed on its {@code A} and another on
 * its {@code a}, and a foal that inherits the {@code A} inherits <b>that
 * copy's</b> seed <i>exactly</i>, with no variation. So bay point heights run
 * in families rather than being re-rolled every generation.
 *
 * <p><b>Priority</b> is an integer in {@code [1, Integer.MAX_VALUE]} that also
 * rides along with the allele copy. Its only job today is the <b>homozygote
 * tie-break</b>: when both copies at a gene are the same allele, both are
 * "expressed", so the copy with the <b>higher</b> priority is the one whose
 * epigenetics the coat pipeline reads (see
 * {@link Epigenome#expressed(Gene, Genotype)}). It's kept as a full-range int
 * because more uses are planned.
 *
 * <p>A horse never carries the same priority twice at one gene - see
 * {@link #deconflict}.
 */
public record AlleleEpigenetics(int priority, long epigeneticSeed) {

    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = Integer.MAX_VALUE;

    public AlleleEpigenetics {
        if (priority < MIN_PRIORITY) {
            throw new IllegalArgumentException("priority must be >= " + MIN_PRIORITY + ", got " + priority);
        }
    }

    /** A fresh copy for a founder / wild horse: random priority, random seed. Consumes 1 int + 1 long. */
    public static AlleleEpigenetics random(Rng rng) {
        // nextInt(MAX_PRIORITY) is [0, MAX-1]; +1 lands in [1, MAX].
        return new AlleleEpigenetics(rng.nextInt(MAX_PRIORITY) + 1, rng.nextLong());
    }

    /** Same seed, priority moved one step - clamped to stay inside the legal range. */
    public AlleleEpigenetics bumped(boolean up) {
        if (up && priority == MAX_PRIORITY) {
            return new AlleleEpigenetics(priority - 1, epigeneticSeed);
        }
        if (!up && priority == MIN_PRIORITY) {
            return new AlleleEpigenetics(priority + 1, epigeneticSeed);
        }
        return new AlleleEpigenetics(up ? priority + 1 : priority - 1, epigeneticSeed);
    }

    /**
     * The rule for a newborn: if both copies at a gene came in carrying the
     * <b>same</b> priority, one of them is bumped a single step - up or down at
     * random - so a horse never has a tie to break. Returns the (possibly
     * replaced) <b>second</b> copy; consumes 1 {@link Rng#nextBoolean()} only
     * when there is actually a tie.
     */
    public static AlleleEpigenetics deconflict(AlleleEpigenetics first, AlleleEpigenetics second, Rng rng) {
        return first.priority() == second.priority() ? second.bumped(rng.nextBoolean()) : second;
    }
}
