package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.EpigeneticAbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * {@link AbstractMagicStatGene}'s <b>sibling for the things that are not body
 * stats</b> - the same codominant, per-copy-percentage locus, but the sum comes
 * out as a {@link GeneAbility} instead of a number on {@link
 * com.example.horsegenetics.common.trait.TraitBuilder}.
 *
 * <p>The two are deliberately not one class, and the reason is the sink rather
 * than the maths. {@link com.example.horsegenetics.common.trait.Traits} is a
 * <i>pure function of the genotype</i>: four numbers and a list of conditions,
 * re-derived wherever they are needed and stored nowhere. Swimming speed and
 * lung capacity are not among those four and should not become the fifth and
 * sixth - they mean nothing without a running game, which is exactly the line
 * {@link GeneAbility} exists on. Adding a field to {@code Traits} for every
 * magical knob would drag Minecraft-shaped concepts into the one record that is
 * meant to survive a version port unchanged.
 *
 * <p>What <b>is</b> shared, and shared on purpose, is the genetics:
 * <ul>
 *   <li>three alleles - one that pushes the quantity up, one that pushes it
 *       down, and the wild type;</li>
 *   <li>every copy carries its own percentage, drawn from its own epigenetic
 *       seed and inherited with the allele;</li>
 *   <li><b>both copies add</b>, the "up" allele counting positive and the
 *       "down" allele negative, so a horse carrying one of each comes out near
 *       the baseline while passing on both extremes;</li>
 *   <li>the percentage is a bounded normal draw about {@value #MEAN_DELTA} with
 *       a standard deviation of {@value #SIGMA_DELTA}, floored at
 *       {@value #MIN_DELTA} so a copy can never point the wrong way.</li>
 * </ul>
 *
 * <p>Reading those two paragraphs together is the whole argument: a horse that
 * swims well and a horse that runs well are the same <i>kind</i> of genetic
 * fact, and two entirely different kinds of runtime fact.
 *
 * <h2>Only heterozygotes are born wild</h2>
 * As on the body-stat genes: the founder table lists the two carriers and the
 * plain horse and nothing else, so every doubled horse is one somebody bred.
 *
 * <h2>Paints nothing</h2>
 * Every outcome is a {@link Expression#wildType() wild type}, so the locus is
 * out of the texture key and the genotype gallery collapses it to one entry
 * however the alleles fall.
 */
public abstract class AbstractMagicFactorGene implements Gene, EpigeneticAbilityContribution {

    /** The name of the one value a copy of any of these loci carries. */
    public static final String DELTA = "delta";

    /** The percentage one variant copy is worth, on average. Larger than the body stats': these have no natural loci under them to be a fraction of. */
    public static final double MEAN_DELTA = 0.25;

    /** Its standard deviation. */
    public static final double SIGMA_DELTA = 0.14;

    /** The floor on a copy's percentage - an "up" allele can never come out subtracting. */
    public static final double MIN_DELTA = 0.02;

    /** Share of wild horses carrying one variant copy. */
    public static final double WILD_CARRIER_PERCENT = 24.0;

    private final String key;
    private final int priority;
    private final String displayName;

    /** The allele that pushes the quantity <b>up</b> ({@code order() == 0}). */
    public final Allele up;
    /** The allele that pushes it <b>down</b> ({@code order() == 1}). */
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

    protected AbstractMagicFactorGene(String key, int priority, String displayName,
                                      String upToken, String upLabel,
                                      String downToken, String downLabel,
                                      AbstractMagicStatGene.Vocabulary text) {
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

        double half = WILD_CARRIER_PERCENT / 2;
        this.founders = FounderTable.builder()
                .weight(up, n, half)
                .weight(down, n, half)
                .weight(n, n, 100.0 - WILD_CARRIER_PERCENT)
                .build();
    }

    /**
     * Turn the summed factor into the abilities it means. Called only when the
     * sum is non-zero, so an implementation never has to test for the wild
     * type; {@code factor} is {@code 1.0} plus the signed sum, and so is
     * strictly positive.
     */
    protected abstract List<GeneAbility> abilitiesFor(double factor);

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
     * Both copies, added - {@code up} positive and {@code down} negative, so
     * every combination falls out of one line and the balanced pair cancels
     * without a special case.
     *
     * <p>The factor is floored just above zero. A sum of {@code -1} or below is
     * only reachable from two very unlucky "down" copies after several
     * generations of drift, but "swims at zero times its speed" is a horse
     * stuck in a pond rather than a slow one, and a negative factor is not a
     * quantity at all.
     */
    @Override
    public final List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype,
                                                GeneEpigenetics epigenetics) {
        double sum = signedDelta(pair.first(), epigenetics.copy(0))
                + signedDelta(pair.second(), epigenetics.copy(1));
        if (sum == 0.0) {
            return List.of();
        }
        return abilitiesFor(Math.max(MIN_FACTOR, 1.0 + sum));
    }

    /** The floor on the multiplier - see {@link #abilitiesFor(AllelePair, Genotype, GeneEpigenetics)}. */
    public static final double MIN_FACTOR = 0.05;

    private double signedDelta(Allele allele, EpiValues epigenetics) {
        if (allele.equals(up)) {
            return epigenetics.get(DELTA);
        }
        if (allele.equals(down)) {
            return -epigenetics.get(DELTA);
        }
        return 0.0;
    }

    /**
     * The one number a copy carries: the percentage it is worth. Always
     * positive - the sign is the allele's job, not the value's.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.gaussian(DELTA, MEAN_DELTA, SIGMA_DELTA, MIN_DELTA));
    }
}
