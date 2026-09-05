package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.AlleleRandomness;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.EpigeneticTraitContribution;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.TraitBuilder;

import java.util.List;

/**
 * The shared shape of the <b>magical body-stat genes</b> - {@link MagicSpeedGene},
 * {@link MagicHealthGene} and {@link MagicJumpGene}. It is the
 * {@link MagicSizeGene} pattern generalised to the three additive stats:
 * <ul>
 *   <li>a <b>codominant</b> locus with three alleles - one that pushes the stat
 *       up, one that pushes it down, and the wild type;</li>
 *   <li>every allele copy carries a percentage drawn from <b>its own</b>
 *       epigenetic seed, and <b>both copies add</b>, the "up" allele counting
 *       positive and the "down" allele negative;</li>
 *   <li>the percentage is a bounded normal draw about {@value #MEAN_DELTA} with
 *       a standard deviation of {@value #SIGMA_DELTA}, floored at
 *       {@value #MIN_DELTA} so a copy can never point the wrong way;</li>
 *   <li>the sum multiplies the stat through a {@code multiply&hellip;Unclamped}
 *       hook on {@link TraitBuilder}, so it lands <i>after</i> every natural
 *       locus and outside their bounds - a magically fast pony is still slower
 *       than a magically fast racehorse.</li>
 * </ul>
 *
 * <h2>Only heterozygotes are born wild</h2>
 * The founder table lists the two carriers and the plain horse and nothing
 * else, so {@value #WILD_CARRIER_PERCENT}% of wild horses carry one copy and
 * <b>every doubled horse is one somebody bred</b>. Combined with the sibling
 * loci - most wild horses carry a copy of all four - the wild population has a
 * quiet continuous spread on every axis, and a horse that is remarkable on any
 * one of them is a breeding result.
 *
 * <h2>Paints nothing</h2>
 * Every outcome is a {@link Expression#wildType() wild type}, so
 * {@link Gene#affectsCoat()} is false, the locus is out of the texture key, and
 * the genotype gallery collapses it to a single entry however the alleles fall.
 *
 * @see MagicSizeGene the fourth gene of the set - kept separate because it
 *      multiplies <i>scale</i>, which carries its own two-stage natural clamp.
 */
public abstract class AbstractMagicStatGene implements Gene, EpigeneticTraitContribution {

    /** The percentage one variant copy is worth, on average - about a tenth either way. */
    public static final double MEAN_DELTA = 0.10;

    /** Its standard deviation. Small enough that one copy is usually subtle. */
    public static final double SIGMA_DELTA = 0.07;

    /** The floor on a copy's percentage - an "up" allele can never come out subtracting. */
    public static final double MIN_DELTA = 0.01;

    /** Two copies at the {@code +6}&sigma; bound - the most one of these genes can do. */
    public static final double MAX_FACTOR_APPROX = 1.0 + 2 * (MEAN_DELTA + 6 * SIGMA_DELTA);

    /** Share of wild horses carrying one variant copy. Most of them, by design. */
    public static final double WILD_CARRIER_PERCENT = 80.0;
    private static final double WILD_UP_PERCENT = 40.0;
    private static final double WILD_DOWN_PERCENT = 40.0;

    private final String key;
    private final int priority;
    private final String displayName;

    /** The allele that pushes the stat <b>up</b> (declared first, so {@code order() == 0}). */
    public final Allele up;
    /** The allele that pushes the stat <b>down</b> ({@code order() == 1}). */
    public final Allele down;
    /** The wild type ({@code order() == 2}). */
    public final Allele n;
    private final List<Allele> alleles;

    private final Expression WILD;
    private final Expression MORE;
    private final Expression DOUBLE_MORE;
    private final Expression LESS;
    private final Expression DOUBLE_LESS;
    private final Expression BALANCED;
    private final List<Expression> expressions;

    private final FounderTable founders;

    /**
     * @param upToken     the "up" allele's text in a genotype code (e.g. {@code "Swift"})
     * @param downToken    the "down" allele's text (e.g. {@code "Sluggish"})
     * @param text        the six outcome descriptions and their four display names
     */
    protected AbstractMagicStatGene(String key, int priority, String displayName,
                                    String upToken, String upLabel,
                                    String downToken, String downLabel,
                                    Vocabulary text) {
        this.key = key;
        this.priority = priority;
        this.displayName = displayName;

        this.up = new Allele(key, 0, upToken, upLabel);
        this.down = new Allele(key, 1, downToken, downLabel);
        this.n = new Allele(key, 2, "n", "Wild-type (n)");
        this.alleles = List.of(up, down, n);

        this.WILD = Expression.wildType(text.wild());
        this.MORE = Expression.wildType("more", text.moreName(), text.more());
        this.DOUBLE_MORE = Expression.wildType("double-more", text.doubleMoreName(), text.doubleMore());
        this.LESS = Expression.wildType("less", text.lessName(), text.less());
        this.DOUBLE_LESS = Expression.wildType("double-less", text.doubleLessName(), text.doubleLess());
        this.BALANCED = Expression.wildType("balanced", text.balancedName(), text.balanced());
        this.expressions = List.of(WILD, MORE, DOUBLE_MORE, LESS, DOUBLE_LESS, BALANCED);

        this.founders = FounderTable.builder()
                .weight(up, n, WILD_UP_PERCENT)
                .weight(down, n, WILD_DOWN_PERCENT)
                .weight(n, n, 100.0 - WILD_UP_PERCENT - WILD_DOWN_PERCENT)
                .build();
    }

    /** Push {@code factor} into this stat's unclamped magical multiplier on {@link TraitBuilder}. */
    protected abstract void applyMagic(TraitBuilder out, double factor);

    @Override public String key() { return key; }
    @Override public String name() { return displayName; }
    @Override public int priority() { return priority; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int more = pair.count(up);
        int less = pair.count(down);
        if (more > 0 && less > 0) {
            return BALANCED;
        }
        return switch (more) {
            case 2 -> DOUBLE_MORE;
            case 1 -> MORE;
            default -> switch (less) {
                case 2 -> DOUBLE_LESS;
                case 1 -> LESS;
                default -> WILD;
            };
        };
    }

    /**
     * Both copies, added. {@code up} counts positive and {@code down} negative,
     * so every combination falls out of one line and the balanced pair cancels
     * without a special case.
     */
    @Override
    public void contribute(AllelePair pair, Genotype genotype, AlleleRandomness epigenetics,
                           TraitBuilder out) {
        double sum = signedDelta(pair.first(), epigenetics.copy(0))
                + signedDelta(pair.second(), epigenetics.copy(1));
        if (sum != 0.0) {
            applyMagic(out, 1.0 + sum);
        }
    }

    private double signedDelta(Allele allele, Rng epigenetics) {
        if (allele.equals(up)) {
            return delta(epigenetics);
        }
        if (allele.equals(down)) {
            return -delta(epigenetics);
        }
        return 0.0; // the baseline allele is worth nothing, as everywhere else
    }

    /**
     * One copy's percentage: a bounded normal draw about {@link #MEAN_DELTA},
     * floored at {@link #MIN_DELTA}. Always positive - the sign is the allele's
     * job, not the distribution's.
     */
    public static double delta(Rng epigenetics) {
        return Math.max(MIN_DELTA, MEAN_DELTA + epigenetics.nextGaussian() * SIGMA_DELTA);
    }

    /** The six outcome descriptions plus the four display names, for the base constructor. */
    public record Vocabulary(String wild,
                             String moreName, String more,
                             String doubleMoreName, String doubleMore,
                             String lessName, String less,
                             String doubleLessName, String doubleLess,
                             String balancedName, String balanced) {
    }
}
