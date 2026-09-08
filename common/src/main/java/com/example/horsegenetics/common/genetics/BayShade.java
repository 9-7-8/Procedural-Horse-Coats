package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

/**
 * <b>The bay-shade score</b> - the one number that decides whether a bay horse
 * is a blood bay, an ordinary bay, a liver bay or a seal brown, and the single
 * place the four labels are defined.
 *
 * <h2>Why a score and not four alleles</h2>
 * Blood bay, bay, liver bay and seal brown are <b>phenotype descriptions, not
 * loci</b>. Every one of them tests as the same bay foundation - one functional
 * <i>MC1R</i> copy and one functional <i>ASIP</i> copy, {@code E_A_} - and no
 * validated equine allele distinguishes them. (The {@code A+}/{@code A}/
 * {@code At}/{@code a} series people quote for seal brown is real in other
 * mammals and unvalidated in horses; see {@code wiki/gene-shade.html}.) What
 * the evidence does support is a <b>continuum</b> driven by a regulatory region
 * plus dosage effects, so that is what this is: several inputs summed into one
 * number, which is then read as a band.
 *
 * <h2>The inputs</h2>
 * <table>
 *   <tr><th>input</th><th>range</th><th>why</th></tr>
 *   <tr><td>{@link com.example.horsegenetics.common.genetics.genes.ShadeGene}
 *       dosage</td><td>-3 .. +3</td>
 *       <td>the chromosome-22 <i>ASIP</i>/<i>RALY</i> region - by far the
 *       strongest real association with how far black spreads</td></tr>
 *   <tr><td><i>MC1R</i> dosage ({@code E/E})</td><td>+{@value #EE_BONUS}</td>
 *       <td>{@code E/E} bays average darker than {@code E/e} ones</td></tr>
 *   <tr><td><i>ASIP</i> dosage ({@code A/a})</td><td>+{@value #ONE_AGOUTI_BONUS}</td>
 *       <td>one functional {@code A} copy averages darker than two - real, but
 *       weak, so it is deliberately the smallest term</td></tr>
 *   <tr><td>expression roll</td><td>&plusmn;{@value #EXPRESSION_RANGE}</td>
 *       <td>the polygenic / developmental / seasonal spread. Drawn off the
 *       expressing {@code A} copy's epigenetic seed, so it is inherited with
 *       that copy exactly the way a background modifier set would be</td></tr>
 * </table>
 *
 * <p>The first three are genotype ({@link #geneticScore}) and decide the
 * <b>label</b>; the roll is added on top and decides the <b>look</b>
 * ({@link #spread}). Its range is narrower than a band, on purpose: two horses
 * with the same genotype differ visibly, but the horse never contradicts the
 * name the gene dictionary gave it by more than an edge.
 *
 * <p>Consequences worth knowing: a chestnut or a black carries and transmits
 * all of this silently, so a chestnut line can throw a seal brown generations
 * later; and a seal brown needs {@code ShD/ShD} <i>and</i> either {@code E/E}
 * or {@code A/a}, which is why one is a find rather than a routine roll.
 */
public final class BayShade {

    /** How much a second functional {@code MC1R} copy darkens the score. */
    public static final double EE_BONUS = 1.0;

    /** How much carrying only one functional {@code ASIP} copy darkens the score. */
    public static final double ONE_AGOUTI_BONUS = 0.5;

    /** Half-width of the per-horse expression roll, in score units. */
    public static final double EXPRESSION_RANGE = 0.75;

    /**
     * The three band edges. They are set so that a horse with the
     * <b>neutral</b> shade haplotype is an ordinary bay whatever its two dosage
     * terms do - {@code Sh/Sh} spans {@code 0} to {@code +1.5} across
     * {@code E/e A/A} and {@code E/E A/a}, and all of it has to read as "bay"
     * or the dosage terms would be doing the shade locus's job. Everything
     * beyond an ordinary bay therefore needs a haplotype to carry it there,
     * which is what makes shade the gene a breeder selects on.
     *
     * <p>Over the wild population they come out at roughly 15% blood bay, 61%
     * bay, 19% liver bay and 5% seal brown - the seal being rare because it
     * needs {@code ShD/ShD} <i>and</i> a dosage term on top.
     */
    public static final double BLOOD_MAX = -1.5;
    /** Genetic score at or below which a bay is an ordinary bay. See {@link #BLOOD_MAX}. */
    public static final double BAY_MAX = 1.5;
    /** Genetic score at or below which a bay is a liver bay; above it, a seal brown. */
    public static final double LIVER_MAX = 3.0;

    /** The lowest score any horse can reach, roll included. */
    public static final double MIN_SCORE = -3.0 - EXPRESSION_RANGE;
    /** The highest score any horse can reach, roll included. */
    public static final double MAX_SCORE = 3.0 + EE_BONUS + ONE_AGOUTI_BONUS + EXPRESSION_RANGE;

    private BayShade() {
    }

    /**
     * The four named points on the continuum. They are labels for bands of
     * {@link #geneticScore}, not alleles and not separate expressions of
     * anything - which is the whole claim this class exists to make.
     */
    public enum Shade {
        BLOOD("blood-bay", "Blood bay"),
        BAY("bay", "Bay"),
        LIVER("liver-bay", "Liver bay"),
        SEAL("seal-brown", "Seal brown");

        private final String id;
        private final String label;

        Shade(String id, String label) {
            this.id = id;
            this.label = label;
        }

        /** The {@link Expression#id()} agouti uses for this shade. */
        public String id() {
            return id;
        }

        public String label() {
            return label;
        }
    }

    /**
     * The part of the score a horse's genotype fixes - {@code -3} to
     * {@code +4.5}. No randomness, so this is what a label, a breeding
     * prediction and the gene dictionary may be built on.
     *
     * <p>Answers for any horse, including one that is not bay. A chestnut's
     * score is real and heritable; it simply has no black for it to move.
     */
    public static double geneticScore(Genotype genotype) {
        double score = Genes.SHADE.dosage(genotype.pair(Genes.SHADE));
        AllelePair extension = genotype.pair(Genes.EXTENSION);
        if (extension.homozygousFor(Genes.EXTENSION.E)) {
            score += EE_BONUS;
        }
        AllelePair agouti = genotype.pair(Genes.AGOUTI);
        if (agouti.has(Genes.AGOUTI.A) && agouti.has(Genes.AGOUTI.a)) {
            score += ONE_AGOUTI_BONUS;
        }
        return score;
    }

    /** Which band {@code genotype} sits in, before the per-horse roll. */
    public static Shade shadeOf(Genotype genotype) {
        return bandOf(geneticScore(genotype));
    }

    /** Which band a genetic score sits in. */
    public static Shade bandOf(double geneticScore) {
        if (geneticScore <= BLOOD_MAX) {
            return Shade.BLOOD;
        }
        if (geneticScore <= BAY_MAX) {
            return Shade.BAY;
        }
        return geneticScore <= LIVER_MAX ? Shade.LIVER : Shade.SEAL;
    }

    /**
     * <b>How far black spreads on this horse</b>, {@code 0} (black held to the
     * points, reddest body) to {@code 1} (black over nearly all of it, only the
     * soft tan points left). {@link #geneticScore} plus one expression roll,
     * rescaled onto {@code [0, 1]}.
     *
     * <p>Reads the stored {@link #SHADE} offset - the number that separates two
     * bays with the same shade genes.
     */
    public static double spread(Genotype genotype, EpiValues epi) {
        double score = geneticScore(genotype) + epi.get(SHADE);
        return (score - MIN_SCORE) / (MAX_SCORE - MIN_SCORE);
    }

    /** How far this horse reads from its raw shade score. */
    public static final String SHADE = "shade";

    /** The shade offset every bay carries. Composed into {@code AgoutiGene}'s schema. */
    public static EpiValue shadeValue() {
        return EpiValue.uniform(SHADE, -EXPRESSION_RANGE, EXPRESSION_RANGE);
    }
}
