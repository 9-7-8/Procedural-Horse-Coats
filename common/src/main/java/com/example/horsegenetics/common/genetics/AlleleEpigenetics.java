package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.epi.EpiDrift;
import com.example.horsegenetics.common.genetics.epi.EpiRoll;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

/**
 * The heritable payload that travels with <b>one copy of one allele</b>: its
 * {@link #priority()} and the literal {@link #values()} that copy carries for
 * its gene.
 *
 * <p>Epigenetics are <b>tied to the allele</b>, not to the horse - a horse
 * carrying {@code A/a} has one set of numbers on its {@code A} and another on
 * its {@code a}, and a foal that inherits the {@code A} inherits <b>that
 * copy's</b> numbers, subject only to {@link EpiDrift}'s nudge. So bay point
 * heights run in families rather than being re-rolled every generation.
 *
 * <h2>What used to be here</h2>
 * This held a single {@code long epigeneticSeed}, and every gene recovered its
 * numbers by replaying a PRNG off it in a documented draw order. That was
 * compact and completely opaque: "how much jump does this copy add" could only
 * be answered by running the gene. It now stores the jump percentage as a
 * number. The cost is size - an epigenome code went from tens of characters per
 * gene to a couple of hundred - and it was judged worth paying for a genotype a
 * player can read and edit.
 *
 * <p><b>Priority</b> is an integer in {@code [1, Integer.MAX_VALUE]} that also
 * rides along with the allele copy. Its only job today is the <b>homozygote
 * tie-break</b>: when both copies at a gene are the same allele, both are
 * "expressed", so the copy with the <b>higher</b> priority is the one whose
 * values the coat pipeline reads (see
 * {@link Epigenome#expressed(Gene, Genotype)}). It does <b>not</b> drift - it is
 * an ordinal deciding which copy wins a tie, not a magnitude, and nudging it
 * would mean a horse's expressed copy could silently swap between generations
 * for no visible reason.
 *
 * <p>A horse never carries the same priority twice at one gene - see
 * {@link #deconflict}.
 */
public record AlleleEpigenetics(int priority, EpiValues values) {

    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = Integer.MAX_VALUE;

    public AlleleEpigenetics {
        if (priority < MIN_PRIORITY) {
            throw new IllegalArgumentException("priority must be >= " + MIN_PRIORITY + ", got " + priority);
        }
        if (values == null) {
            throw new IllegalArgumentException("an allele copy needs values (EpiValues.EMPTY if none)");
        }
    }

    /**
     * A fresh copy for a founder / wild horse: random priority, and every value
     * {@code schema} declares rolled from its own distribution.
     */
    public static AlleleEpigenetics founder(EpiSchema schema, Rng rng) {
        // nextInt(MAX_PRIORITY) is [0, MAX-1]; +1 lands in [1, MAX].
        return new AlleleEpigenetics(rng.nextInt(MAX_PRIORITY) + 1, EpiRoll.founder(schema, rng));
    }

    /** This copy as passed to a foal: same priority, values nudged by drift. */
    public AlleleEpigenetics drifted(Rng rng) {
        EpiValues next = EpiDrift.drift(values, rng);
        return next == values ? this : new AlleleEpigenetics(priority, next);
    }

    /** Same values, priority moved one step - clamped to stay inside the legal range. */
    public AlleleEpigenetics bumped(boolean up) {
        if (up && priority == MAX_PRIORITY) {
            return new AlleleEpigenetics(priority - 1, values);
        }
        if (!up && priority == MIN_PRIORITY) {
            return new AlleleEpigenetics(priority + 1, values);
        }
        return new AlleleEpigenetics(up ? priority + 1 : priority - 1, values);
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
