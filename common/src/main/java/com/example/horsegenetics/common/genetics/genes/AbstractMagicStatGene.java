package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.trait.EpigeneticTraitContribution;
import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TraitBuilder;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.List;

/**
 * The shared shape of the <b>magical body-stat genes</b> - {@link MagicSpeedGene},
 * {@link MagicHealthGene} and {@link MagicJumpGene}. It is the
 * {@link MagicSizeGene} pattern generalised to the three additive stats:
 * <ul>
 *   <li>a <b>codominant</b> locus - one allele that pushes the stat up, one that
 *       pushes it down, the <b>vampiric</b> allele, and the wild type;</li>
 *   <li>every up or down copy carries a percentage drawn from <b>its own</b>
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
 * <h2>The vampiric allele</h2>
 * The dhampir's strength, split out of the old dhampir gene and put where the
 * body's magic already lives. A {@code Vmp} copy is worth a <b>fixed</b> amount -
 * {@link #vampiricPerCopy()}, half of the dhampir's old multiplier on this axis -
 * rather than an epigenetic percentage, so {@code Vmp/n} is exactly half the
 * boost and {@code Vmp/Vmp} is the whole of it (triple health, half again the
 * speed, twice the jump). It adds into the same sum as the other copy, so a
 * {@code Vmp/Swift} horse gets both. Owner's call: "a brown dhampir will carry
 * magic strength, so it is still stronger than a normal horse, but not as
 * dramatic as a full white dhampir."
 *
 * <h2>Only heterozygotes are born wild</h2>
 * The founder table lists the carriers and the plain horse and nothing else, so
 * {@value #WILD_CARRIER_PERCENT}% of wild horses carry one copy and <b>every
 * doubled horse is one somebody bred</b>. Vampiric carriers are a small slice of
 * that ({@value #WILD_VAMPIRIC_PERCENT}%).
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

    /** The name of the one value a copy of any of these loci carries. */
    public static final String DELTA = "delta";

    /** The percentage one variant copy is worth, on average - about a tenth either way. */
    public static final double MEAN_DELTA = 0.10;

    /** Its standard deviation. Small enough that one copy is usually subtle. */
    public static final double SIGMA_DELTA = 0.07;

    /** The floor on a copy's percentage - an "up" allele can never come out subtracting. */
    public static final double MIN_DELTA = 0.01;

    /** Two copies at the {@code +6}&sigma; bound - the most the percentage alleles can do. */
    public static final double MAX_FACTOR_APPROX = 1.0 + 2 * (MEAN_DELTA + 6 * SIGMA_DELTA);

    private static final double WILD_UP_PERCENT = 40.0;
    private static final double WILD_DOWN_PERCENT = 40.0;
    /** Share of wild horses carrying one vampiric copy. */
    public static final double WILD_VAMPIRIC_PERCENT = 2.0;
    /** Share of wild horses carrying one variant copy of any kind. Most of them, by design. */
    public static final double WILD_CARRIER_PERCENT = WILD_UP_PERCENT + WILD_DOWN_PERCENT + WILD_VAMPIRIC_PERCENT;

    /** The vampiric allele's token, the same on all three loci. */
    public static final String VAMPIRIC_TOKEN = "Vmp";

    private final String key;
    private final int priority;
    private final String displayName;

    /** The allele that pushes the stat <b>up</b> (declared first, so {@code order() == 0}). */
    public final Allele up;
    /** The allele that pushes the stat <b>down</b> ({@code order() == 1}). */
    public final Allele down;
    /** The dhampir's allele - a fixed boost per copy ({@code order() == 2}). */
    public final Allele vampiric;
    /** The wild type ({@code order() == 3}). */
    public final Allele n;
    private final List<Allele> alleles;

    private final Expression WILD;
    private final Expression MORE;
    private final Expression DOUBLE_MORE;
    private final Expression LESS;
    private final Expression DOUBLE_LESS;
    private final Expression BALANCED;
    private final Expression VAMPIRIC;
    private final Expression VAMPIRIC_MORE;
    private final Expression VAMPIRIC_LESS;
    private final Expression DOUBLE_VAMPIRIC;
    private final List<Expression> expressions;

    private final FounderTable founders;

    /**
     * @param upToken     the "up" allele's text in a genotype code (e.g. {@code "Swift"})
     * @param downToken   the "down" allele's text (e.g. {@code "Sluggish"})
     * @param text        the six outcome descriptions and their four display names
     * @param statWords   how this stat is spoken of in a sentence - "hearts", "speed", "jump"
     */
    protected AbstractMagicStatGene(String key, int priority, String displayName,
                                    String upToken, String upLabel,
                                    String downToken, String downLabel,
                                    Vocabulary text, String statWords) {
        this.key = key;
        this.priority = priority;
        this.displayName = displayName;

        this.up = new Allele(key, 0, upToken, upLabel);
        this.down = new Allele(key, 1, downToken, downLabel);
        this.vampiric = new Allele(key, 2, VAMPIRIC_TOKEN, "Vampiric (" + VAMPIRIC_TOKEN + ")");
        this.n = new Allele(key, 3, "n", "Wild-type (n)");
        this.alleles = List.of(up, down, vampiric, n);

        this.WILD = Expression.wildType(text.wild());
        this.MORE = Expression.wildType("more", text.moreName(), text.more());
        this.DOUBLE_MORE = Expression.wildType("double-more", text.doubleMoreName(), text.doubleMore());
        this.LESS = Expression.wildType("less", text.lessName(), text.less());
        this.DOUBLE_LESS = Expression.wildType("double-less", text.doubleLessName(), text.doubleLess());
        this.BALANCED = Expression.wildType("balanced", text.balancedName(), text.balanced());
        this.VAMPIRIC = Expression.wildType("vampiric", "Vampiric",
                "One vampiric copy - the dhampir's strength, at half measure. A fixed boost to "
                        + statWords + " rather than a percentage rolled per horse, so every carrier "
                        + "gets exactly the same.");
        this.VAMPIRIC_MORE = Expression.wildType("vampiric-more", "Vampiric and boosted",
                "One vampiric copy and one that pushes " + statWords + " up. Both add: the fixed "
                        + "half-dhampir boost, and that copy's own percentage on top.");
        this.VAMPIRIC_LESS = Expression.wildType("vampiric-less", "Vampiric, held back",
                "One vampiric copy and one that pulls " + statWords + " down. The fixed half-dhampir "
                        + "boost, less that copy's percentage.");
        this.DOUBLE_VAMPIRIC = Expression.wildType("double-vampiric", "Fully vampiric",
                "Two vampiric copies - the whole of a white dhampir's strength on this axis. No "
                        + "wild horse is born with two.");
        this.expressions = List.of(WILD, MORE, DOUBLE_MORE, LESS, DOUBLE_LESS, BALANCED,
                VAMPIRIC, VAMPIRIC_MORE, VAMPIRIC_LESS, DOUBLE_VAMPIRIC);

        this.founders = FounderTable.builder()
                .weight(up, n, WILD_UP_PERCENT)
                .weight(down, n, WILD_DOWN_PERCENT)
                .weight(vampiric, n, WILD_VAMPIRIC_PERCENT)
                .weight(n, n, 100.0 - WILD_CARRIER_PERCENT)
                .build();
    }

    /** Push {@code factor} into this stat's unclamped magical multiplier on {@link TraitBuilder}. */
    protected abstract void applyMagic(TraitBuilder out, double factor);

    /**
     * What one vampiric copy adds to this stat's sum - half of what a
     * {@code Vmp/Vmp} horse gets, so two copies are exactly the dhampir's old
     * multiplier. Fixed, not epigenetic.
     */
    public abstract double vampiricPerCopy();

    /**
     * Which body axis this gene drives. A breed uses it at <b>founder time</b>
     * to decide which of these loci to double up and what to write on the
     * copies ({@code BreedFounder}); nothing consults it when a horse's body is
     * resolved, which is what stops a bred line being pulled back toward its
     * breed's standard.
     */
    public abstract StatAxis axis();

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
        int vamp = pair.count(vampiric);
        int more = pair.count(up);
        int less = pair.count(down);
        if (vamp == 2) {
            return DOUBLE_VAMPIRIC;
        }
        if (vamp == 1) {
            return more == 1 ? VAMPIRIC_MORE : less == 1 ? VAMPIRIC_LESS : VAMPIRIC;
        }
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
     * Both copies, added. {@code up} counts positive, {@code down} negative and
     * {@code vampiric} its fixed share, so every combination falls out of one
     * line and the balanced pair cancels without a special case.
     */
    @Override
    public void contribute(AllelePair pair, Genotype genotype, GeneEpigenetics epigenetics,
                           TraitBuilder out) {
        double sum = signedDelta(pair.first(), epigenetics.copy(0))
                + signedDelta(pair.second(), epigenetics.copy(1));
        if (sum != 0.0) {
            applyMagic(out, 1.0 + sum);
        }
    }

    private double signedDelta(Allele allele, EpiValues epigenetics) {
        if (allele.equals(up)) {
            return epigenetics.get(DELTA);
        }
        if (allele.equals(down)) {
            return -epigenetics.get(DELTA);
        }
        if (allele.equals(vampiric)) {
            return vampiricPerCopy();
        }
        return 0.0; // the baseline allele is worth nothing, as everywhere else
    }

    /**
     * The one number a copy of this locus carries: the percentage it is worth.
     * Always positive - the sign is the allele's job, not the value's - and
     * always readable, which is the entire point of storing it. A vampiric
     * copy carries one too and ignores it.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.gaussian(DELTA, MEAN_DELTA, SIGMA_DELTA, MIN_DELTA));
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
