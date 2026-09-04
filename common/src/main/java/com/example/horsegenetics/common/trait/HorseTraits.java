package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;

/**
 * <b>Genotype in, body out.</b> One walk of
 * {@link Genes#codeOrder()} that hands every gene implementing
 * {@link TraitContribution} a {@link TraitBuilder}, and returns the resulting
 * {@link Traits}.
 *
 * <h2>What this replaced</h2>
 * A foal's speed and health used to be drawn uniformly from
 * {@code [0.75 * min(parents), 1.5 * max(parents)]} - an uncapped random walk
 * with no genetics in it at all. Two full siblings could differ by a factor of
 * two, a line's numbers drifted upward on luck rather than on choices, and
 * nothing a player did to a pairing was legible in the result. That whole
 * mechanism is gone. Every number here is a sum of allele weights, so a horse's
 * stats <i>are</i> its genotype, and improving a line means finding and fixing
 * the alleles that carry the weight.
 *
 * <h2>The baselines</h2>
 * A horse carrying nothing but wild types lands on the constants below, which
 * sit a little under vanilla's midpoints - {@code 0.1875} against a vanilla
 * range of {@code 0.1125}-{@code 0.3375}, eleven hearts against {@code 15}-{@code 30}
 * health, {@code 0.5} jump against {@code 0.4}-{@code 0.8}. Deliberately low:
 * the variant alleles are what push a horse up through the vanilla range, so a
 * bred line beats a wild-caught one and there is somewhere to go.
 *
 * <h2>Determinism</h2>
 * Pure: no {@link com.example.horsegenetics.common.Rng}, no epigenetics, no
 * entity state. The same genotype always resolves to the same body, on the
 * server, on a reload, and in a unit test. Nothing is stored - callers resolve
 * on demand, so re-tuning a gene's weight moves the horses that already exist
 * instead of leaving a save full of numbers nobody can explain.
 */
public final class HorseTraits {

    /** Movement speed of an all-wild-type horse. Vanilla rolls {@code 0.1125}-{@code 0.3375}. */
    public static final double BASE_SPEED = 0.1875;

    /** Max health of an all-wild-type horse - eleven hearts. Vanilla rolls {@code 15}-{@code 30}. */
    public static final double BASE_HEALTH = 22.0;

    /** Jump strength of an all-wild-type horse. Vanilla rolls {@code 0.4}-{@code 0.8}. */
    public static final double BASE_JUMP = 0.5;

    /** Body scale of an all-wild-type horse: vanilla size. */
    public static final double BASE_SCALE = 1.0;

    /**
     * The floor on max health - half a heart. A genetic health value must never
     * resolve to zero: the attribute would be degenerate, and killing a horse
     * is the damage path's job. See {@code wiki/roadmap.html} §6.4.
     */
    public static final double MIN_HEALTH = 1.0;

    /** Floors on the other two, for the same reason - a zero-speed horse is stuck, not sick. */
    public static final double MIN_SPEED = 0.02;
    public static final double MIN_JUMP = 0.1;

    /** Scale bounds. A dwarf bottoms out well inside these; they are a guard, not a design. */
    public static final double MIN_SCALE = 0.45;
    public static final double MAX_SCALE = 1.75;

    private HorseTraits() {
    }

    /** The horse this genotype describes, with the health genetics switched on. */
    public static Traits resolve(Genotype genotype) {
        return resolve(genotype, true);
    }

    /**
     * @param healthGenetics {@code false} suppresses every
     *        {@link HealthContribution} - the disorders stop affecting the
     *        horse, while still being carried and inherited exactly as before.
     *        The server config's "off" position, and nothing else, passes
     *        {@code false}.
     */
    public static Traits resolve(Genotype genotype, boolean healthGenetics) {
        TraitBuilder out = new TraitBuilder();
        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof TraitContribution contribution)) {
                continue;
            }
            if (!healthGenetics && gene instanceof HealthContribution) {
                continue;
            }
            AllelePair pair = genotype.pair(gene);
            contribution.contribute(pair, genotype, out);
        }
        return out.build();
    }

    /** The all-wild-type horse - the baselines, nothing added. */
    public static Traits baseline() {
        return new TraitBuilder().build();
    }
}
