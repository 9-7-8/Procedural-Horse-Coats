package com.example.horsegenetics.common.trait;

import com.example.horsegenetics.common.MidpointRng;
import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genome;
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

    /**
     * Scale bounds for a <b>magical</b> size gene, applied after the natural
     * clamp - see {@link TraitBuilder#multiplyScaleUnclamped}. Ten times either
     * way: far enough that a magically large horse is a spectacle rather than a
     * large horse, and near enough that it still fits through a door frame it
     * has any business fitting through.
     */
    public static final double MAGICAL_MIN_SCALE = 0.1;
    public static final double MAGICAL_MAX_SCALE = 10.0;

    private HorseTraits() {
    }

    /**
     * The horse this genotype describes, with the health genetics switched on
     * and <b>no epigenome</b> - so an {@link EpigeneticTraitContribution}
     * reports its midpoint (see {@link MidpointRng}). This is the right call for
     * a question about a <i>genotype</i>; to resolve an actual horse, pass its
     * epigenome.
     */
    public static Traits resolve(Genotype genotype) {
        return resolve(genotype, null, true);
    }

    /** The horse this {@link Genome} describes, with the health genetics switched on. */
    public static Traits resolve(Genome genome) {
        return resolve(genome.genotype(), genome.epigenome(), true);
    }

    /**
     * @param healthGenetics {@code false} suppresses every
     *        {@link HealthContribution} - the disorders stop affecting the
     *        horse, while still being carried and inherited exactly as before.
     *        The server config's "off" position, and nothing else, passes
     *        {@code false}.
     */
    public static Traits resolve(Genotype genotype, boolean healthGenetics) {
        return resolve(genotype, null, healthGenetics);
    }

    /**
     * The full form. {@code epigenome} may be {@code null}, in which case every
     * {@link EpigeneticTraitContribution} draws from {@link MidpointRng} - the
     * midpoint of what the genotype can produce, not a horse.
     *
     * @param healthGenetics {@code false} suppresses every
     *        {@link HealthContribution}, as above.
     */
    public static Traits resolve(Genotype genotype, Epigenome epigenome, boolean healthGenetics) {
        TraitBuilder out = new TraitBuilder();
        for (Gene gene : Genes.codeOrder()) {
            boolean plain = gene instanceof TraitContribution;
            boolean epigenetic = gene instanceof EpigeneticTraitContribution;
            if (!plain && !epigenetic) {
                continue;
            }
            if (!healthGenetics && gene instanceof HealthContribution) {
                continue;
            }
            AllelePair pair = genotype.pair(gene);
            if (plain) {
                ((TraitContribution) gene).contribute(pair, genotype, out);
            }
            if (epigenetic) {
                ((EpigeneticTraitContribution) gene)
                        .contribute(pair, genotype, randomnessFor(gene, genotype, epigenome), out);
            }
        }
        return out.build();
    }

    /**
     * One gene's per-horse randomness, derived exactly the way
     * {@code CoatBuildContext} derives the coat's - so a gene that varies both
     * its coat and its body reads the same numbers, not two sets.
     *
     * <p>With no epigenome every accessor is {@link MidpointRng}, so an
     * epigenetic trait reports the midpoint of what the genotype can produce.
     */
    private static AlleleRandomness randomnessFor(Gene gene, Genotype genotype, Epigenome epigenome) {
        if (epigenome == null) {
            return MIDPOINT;
        }
        Epigenome.Copies copies = epigenome.copies(gene);
        return new AlleleRandomness() {
            @Override
            public Rng expressed() {
                return new SeededRng(epigenome.expressedSeed(gene, genotype), gene.key());
            }

            @Override
            public Rng copy(int slot) {
                long seed = (slot == 0 ? copies.first() : copies.second()).epigeneticSeed();
                return new SeededRng(seed, gene.key());
            }
        };
    }

    /** Every accessor is the midpoint - see {@link #resolve(Genotype)}. */
    private static final AlleleRandomness MIDPOINT = new AlleleRandomness() {
        @Override
        public Rng expressed() {
            return MidpointRng.INSTANCE;
        }

        @Override
        public Rng copy(int slot) {
            return MidpointRng.INSTANCE;
        }
    };

    /** The all-wild-type horse - the baselines, nothing added. */
    public static Traits baseline() {
        return new TraitBuilder().build();
    }
}
