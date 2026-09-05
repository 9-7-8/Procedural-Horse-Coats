package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.AlleleRandomness;
import com.example.horsegenetics.common.trait.EpigeneticTraitContribution;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.TraitBuilder;

import java.util.List;

/**
 * <b>Magic body size</b> ({@code horsegenetics.body_size}) - a <b>magical</b>
 * gene that paints nothing and changes only how big the horse is.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th><th>size</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td><td>unchanged</td></tr>
 *   <tr><td>{@code Big/n}</td><td>{@code giant}</td><td>one copy's percentage, about +10%</td></tr>
 *   <tr><td>{@code Big/Big}</td><td>{@code double-giant}</td><td><b>both</b> copies' percentages, added</td></tr>
 *   <tr><td>{@code Small/n}</td><td>{@code tiny}</td><td>one copy's percentage off, about -10%</td></tr>
 *   <tr><td>{@code Small/Small}</td><td>{@code double-tiny}</td><td>both copies' percentages off</td></tr>
 *   <tr><td>{@code Big/Small}</td><td>{@code balanced}</td><td>the two nearly cancel</td></tr>
 * </table>
 *
 * <h2>Codominant, and the percentages add</h2>
 * Every allele copy carries a percentage, and <b>both copies contribute</b>:
 * the horse's size is one plus the sum of them, {@code Big} counting positive
 * and {@code Small} negative. That is what codominance means here, and it is
 * where the interesting horses come from - two big copies of {@code +18%} and
 * {@code +21%} make a horse nearly 40% over, which is far outside anything the
 * wild population contains.
 *
 * <p>Six combinations and <b>six outcomes</b>, one each. A codominant locus is
 * the case where every combination genuinely differs, so collapsing any two of
 * them into a shared row would be a lie about the gene.
 *
 * <p>{@code Big/Small} lands <i>near</i> 1.0 rather than exactly on it, because
 * the two percentages are independent draws and only cancel exactly by
 * coincidence. The residual is the honest consequence of addition, and it is
 * also useful: a horse a hair off normal size is carrying both extremes.
 *
 * <h2>Where the percentage comes from</h2>
 * One {@link Rng#nextGaussian()} per copy, off <b>that copy's</b> epigenetic
 * seed ({@link AlleleRandomness#copy}) - not the expressing copy, which would
 * count one allele twice and the other not at all.
 *
 * <p>The distribution is <b>normal, centred on {@value #MEAN_DELTA}</b> with a
 * standard deviation of {@value #SIGMA_DELTA}. So a typical carrier is about
 * 1.1x or 0.9x and most of the population sits between 1.03x and 1.17x either
 * way: visible as a spread across a paddock, rarely remarkable on any one horse.
 * That is the point - almost every wild horse carries this gene, so if a single
 * copy were dramatic then "dramatic" would be the baseline and nothing would
 * read as unusual.
 *
 * <p>{@link Rng#nextGaussian()} is bounded at &plusmn;6&sigma;, so a copy's
 * percentage cannot exceed {@code MEAN + 6 * SIGMA} and the theoretical ceiling
 * for {@code Big/Big} is about {@value #MAX_FACTOR_APPROX}x. The
 * {@link HorseTraits#MAGICAL_MAX_SCALE} clamp is a guard that this gene no
 * longer reaches.
 *
 * <h2>Only heterozygotes are born wild</h2>
 * The founder table lists {@code Big/n}, {@code Small/n} and {@code n/n} and
 * nothing else - and {@value #WILD_CARRIER_PERCENT}% of wild horses carry a
 * variant copy, which makes this by far the commonest non-baseline gene in the
 * mod. Every doubled horse, in either direction, is therefore something
 * <i>somebody bred</i>. The gene is background variation you find everywhere and
 * a breeding project at the same time, which is the shape the health loci have
 * pointed the other way.
 *
 * <h2>What makes it the magical version</h2>
 * {@link LcorlGene} and {@link Hmga2Gene} are the real size loci and behave like
 * it: a fixed weight per copy, additive, spanning about {@code 0.88} to
 * {@code 1.10} of a vanilla horse. This one multiplies whatever they settled on,
 * through {@link TraitBuilder#multiplyScaleUnclamped}, so it applies
 * <i>after</i> the natural clamp - a magically enormous pony is still smaller
 * than a magically enormous draught horse.
 *
 * <p>Deliberately size only. It moves no other number - not speed, not health,
 * not jump. A large horse is a spectacle and a stable problem, not a better
 * horse, and the natural loci already own the trade between size and everything
 * else.
 */
public final class MagicSizeGene implements Gene, EpigeneticTraitContribution {

    public static final String KEY = "horsegenetics.body_size";
    public static final int PRIORITY = 140;

    /** The percentage one variant copy is worth, on average - so one copy is about 1.1x or 0.9x. */
    public static final double MEAN_DELTA = 0.10;

    /** Its standard deviation. Small enough that one copy is usually subtle. */
    public static final double SIGMA_DELTA = 0.07;

    /**
     * The floor on a copy's percentage. A {@code Big} allele that rolled a
     * negative percentage would be a big allele making a horse smaller, which is
     * not a distribution tail, it is a bug in the reader's head.
     */
    public static final double MIN_DELTA = 0.01;

    /** Two copies at the {@code +6}&sigma; bound - the most this gene can do. For the Javadoc. */
    public static final double MAX_FACTOR_APPROX = 1.0 + 2 * (MEAN_DELTA + 6 * SIGMA_DELTA);

    /** Share of wild horses carrying one variant copy. Most of them, by design. */
    public static final double WILD_CARRIER_PERCENT = 80.0;
    private static final double WILD_BIG_PERCENT = 40.0;
    private static final double WILD_SMALL_PERCENT = 40.0;

    public final Allele Big = new Allele(KEY, 0, "Big", "Gigantism (Big)");
    public final Allele Small = new Allele(KEY, 1, "Small", "Miniaturism (Small)");
    public final Allele n = new Allele(KEY, 2, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Big, Small, n);

    private final Expression WILD = Expression.wildType(
            "Ordinary size for whatever the horse's own size genes say - which, since most horses "
                    + "carry a copy of this one, is itself slightly unusual.");

    private final Expression GIANT = Expression.wildType("giant", "Larger",
            "One big copy. The horse is larger than its own genes would make it, by the percentage "
                    + "written on that copy - usually around a tenth. It passes that exact "
                    + "percentage on with the allele.");

    private final Expression DOUBLE_GIANT = Expression.wildType("double-giant", "Much larger",
            "Two big copies, and the percentages add. Around a fifth over on average, and far more "
                    + "when both copies rolled well - the largest horses in the world are bred "
                    + "here, because no wild horse is born with two.");

    private final Expression TINY = Expression.wildType("tiny", "Smaller",
            "One small copy. The same thing in reverse: smaller by the percentage on that copy, "
                    + "usually around a tenth, and inherited with the allele.");

    private final Expression DOUBLE_TINY = Expression.wildType("double-tiny", "Much smaller",
            "Two small copies, and the percentages subtract together. Around a fifth under on "
                    + "average and a great deal less at the extreme, which is where the very "
                    + "smallest horses come from.");

    private final Expression BALANCED = Expression.wildType("balanced", "Balanced",
            "One big copy and one small copy. Their percentages very nearly cancel, so the horse "
                    + "is close to its ordinary size while carrying, and passing on, both "
                    + "extremes. A horse a hair off normal is usually this.");

    private final List<Expression> expressions =
            List.of(WILD, GIANT, DOUBLE_GIANT, TINY, DOUBLE_TINY, BALANCED);

    /**
     * <b>Heterozygotes only.</b> Written out rather than derived from
     * Hardy-Weinberg, because random mating is exactly what this table is not:
     * the doubled combinations are the reward for breeding and must not turn up
     * in a wild-caught horse. Baseline last, so a high founder roll is the plain
     * horse - the rule every table in the mod follows.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(Big, n, WILD_BIG_PERCENT)
            .weight(Small, n, WILD_SMALL_PERCENT)
            .weight(n, n, 100.0 - WILD_BIG_PERCENT - WILD_SMALL_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic body size"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int big = pair.count(Big);
        int small = pair.count(Small);
        if (big > 0 && small > 0) {
            return BALANCED;
        }
        return switch (big) {
            case 2 -> DOUBLE_GIANT;
            case 1 -> GIANT;
            default -> switch (small) {
                case 2 -> DOUBLE_TINY;
                case 1 -> TINY;
                default -> WILD;
            };
        };
    }

    /**
     * Both copies, added. {@code Big} counts positive and {@code Small}
     * negative, so every combination falls out of one loop and
     * {@code Big/Small} cancels without being a special case.
     */
    @Override
    public void contribute(AllelePair pair, Genotype genotype, AlleleRandomness epigenetics,
                           TraitBuilder out) {
        double sum = signedDelta(pair.first(), epigenetics.copy(0))
                + signedDelta(pair.second(), epigenetics.copy(1));
        if (sum != 0.0) {
            out.multiplyScaleUnclamped(1.0 + sum);
        }
    }

    private double signedDelta(Allele allele, Rng epigenetics) {
        if (allele.equals(Big)) {
            return delta(epigenetics);
        }
        if (allele.equals(Small)) {
            return -delta(epigenetics);
        }
        return 0.0; // the baseline allele is worth nothing, as everywhere else
    }

    /**
     * One copy's percentage: a normal draw about {@link #MEAN_DELTA}, floored at
     * {@link #MIN_DELTA}. Always positive - the sign is the allele's job, not
     * the distribution's.
     */
    public static double delta(Rng epigenetics) {
        return Math.max(MIN_DELTA, MEAN_DELTA + epigenetics.nextGaussian() * SIGMA_DELTA);
    }
}
