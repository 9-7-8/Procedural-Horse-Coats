package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;
import com.example.horsegenetics.common.trait.EpigeneticTraitContribution;
import com.example.horsegenetics.common.trait.HorseTraits;
import com.example.horsegenetics.common.trait.StatAxis;
import com.example.horsegenetics.common.trait.TraitBuilder;

import java.util.List;

/**
 * <b>Magic pull</b> ({@code horsegenetics.magic_pull}) - how much the horse can
 * shift. The fifth magical body-stat locus, and the one whose number is a
 * <b>score rather than a multiplier</b>.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type - pull {@value HorseTraits#BASE_PULL}, an ordinary horse</td></tr>
 *   <tr><td>{@code Strong/n}</td><td>that copy's points, added</td></tr>
 *   <tr><td>{@code Strong/Strong}</td><td><b>both</b> copies' points, added</td></tr>
 *   <tr><td>{@code Weak/n}, {@code Weak/Weak}</td><td>the same, subtracted</td></tr>
 *   <tr><td>{@code Strong/Weak}</td><td>the two cancel as far as they go</td></tr>
 * </table>
 *
 * <h2>Why it is not an {@link AbstractMagicStatGene}</h2>
 * It is that gene's shape - a codominant locus, one allele up, one down, a
 * per-copy number written on the copy and inherited with it, both copies added -
 * and it differs in the two ways that matter:
 * <ul>
 *   <li>The sum is <b>added</b>, not multiplied. Speed, health, jump and scale
 *       multiply because they have to scale whatever the <i>natural</i> loci
 *       settled on. Pull has no natural loci; this gene is the only thing that
 *       moves it, so a copy worth {@code +2.5} is worth two and a half points on
 *       every horse that inherits it rather than a proportion of something else.
 *       The number on the copy is therefore readable on its own, which is the
 *       point of putting it on a 1-10 scale.</li>
 *   <li>There is <b>no vampiric allele</b>. The other three carry one because
 *       the dhampir's strength had to travel with the horse; pull instead gives
 *       the dhampir a per-strain score on its breed sheet, so white dhampirs
 *       pull at 9 and seal browns at 6 - a four-to-one split that no fixed
 *       per-copy amount can express, since two copies of anything are only ever
 *       twice one.</li>
 * </ul>
 *
 * <h2>Paints nothing</h2>
 * Every outcome is a {@link Expression#wildType() wild type}, so
 * {@link Gene#affectsCoat()} is false, the locus is out of the texture key, and
 * the genotype gallery collapses it to a single entry however the alleles fall.
 *
 * <h2>Only heterozygotes are born wild</h2>
 * The founder table lists the two carriers and the plain horse and nothing else,
 * so a doubled horse is one somebody bred - the same bargain the three additive
 * stats make, and for the same reason.
 *
 * <h2>Nothing reads it yet</h2>
 * {@code Traits.pull()} is resolved and inherited and drifts like any other
 * number, and no system consumes it. That is deliberate: what a score is worth
 * in cart loads is a decision for whatever ends up hitching a horse to
 * something, and is not a fact about the horse's genetics.
 */
public final class MagicPullGene implements Gene, EpigeneticTraitContribution {

    public static final String KEY = "horsegenetics.magic_pull";

    /** Inside the {@code 140} band, so it families with the other body stats. */
    public static final int PRIORITY = 149;

    /**
     * The name of the one value a copy carries. Deliberately the <b>same name</b>
     * the three additive stats use, because {@code BreedFounder.withDelta} writes
     * a breed's target through it and one name across the family is what lets
     * that stay a single method.
     */
    public static final String DELTA = AbstractMagicStatGene.DELTA;

    /**
     * The points one variant copy is worth, on average, and its spread. Half a
     * point about a baseline of five - the same tenth-of-the-way feel the three
     * additive stats get from {@code MEAN_DELTA = 0.10} against a baseline of
     * one, translated into score points.
     *
     * <p>The design range is mean &plusmn; 6&sigma;, so one copy reaches about
     * {@value #MAX_DELTA_APPROX} points and a doubled horse about twice that:
     * a wild-bred {@code Strong/Strong} tops out near a score of ten, which is
     * exactly where the breed sheets put a Shire. The two agreeing is the
     * calibration, not a coincidence.
     */
    public static final double MEAN_DELTA = 0.50;
    public static final double SIGMA_DELTA = 0.35;

    /** The floor on a copy's points - a {@code Strong} copy can never come out subtracting. */
    public static final double MIN_DELTA = 0.05;

    /** One copy at the {@code +6}&sigma; bound. */
    public static final double MAX_DELTA_APPROX = MEAN_DELTA + 6 * SIGMA_DELTA;

    private static final double WILD_UP_PERCENT = 40.0;
    private static final double WILD_DOWN_PERCENT = 40.0;

    /** Share of wild horses carrying one variant copy of either kind. Most of them, by design. */
    public static final double WILD_CARRIER_PERCENT = WILD_UP_PERCENT + WILD_DOWN_PERCENT;

    /** The allele that pushes pull <b>up</b> (declared first, so {@code order() == 0}). */
    public final Allele Strong = new Allele(KEY, 0, "Strong", "Strength (Strong)");
    /** The allele that pushes it <b>down</b> ({@code order() == 1}). */
    public final Allele Weak = new Allele(KEY, 1, "Weak", "Weakness (Weak)");
    /** The wild type ({@code order() == 2}). */
    public final Allele n = new Allele(KEY, 2, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Strong, Weak, n);

    private final Expression WILD = Expression.wildType(
            "Ordinary pulling ability - a score of five, which is what a horse carrying nothing "
                    + "at this locus is worth and what most riding breeds sit on.");

    private final Expression MORE = Expression.wildType("more", "Strong",
            "One strong copy. The horse pulls better than an ordinary one by the number written "
                    + "on that copy - usually half a point, sometimes rather more. The copy is "
                    + "inherited with the allele, so a good one is worth passing on.");

    private final Expression DOUBLE_MORE = Expression.wildType("double-more", "Very strong",
            "Two strong copies, and the numbers add. This is where the draught breeds live, and "
                    + "no wild horse is born with two - a doubled horse is one somebody bred.");

    private final Expression LESS = Expression.wildType("less", "Weak",
            "One weak copy. The horse pulls worse than an ordinary one by the number written on "
                    + "that copy.");

    private final Expression DOUBLE_LESS = Expression.wildType("double-less", "Very weak",
            "Two weak copies, and the numbers add. The miniatures and the smallest ponies; a "
                    + "horse that is very nearly not worth hitching to anything.");

    private final Expression BALANCED = Expression.wildType("balanced", "Balanced",
            "One strong copy and one weak one. They cancel as far as they go, so the horse lands "
                    + "near ordinary - but it is carrying both, and its foals will not all be "
                    + "near ordinary.");

    private final List<Expression> expressions =
            List.of(WILD, MORE, DOUBLE_MORE, LESS, DOUBLE_LESS, BALANCED);

    private final FounderTable founders = FounderTable.builder()
            .weight(Strong, n, WILD_UP_PERCENT)
            .weight(Weak, n, WILD_DOWN_PERCENT)
            .weight(n, n, 100.0 - WILD_CARRIER_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic pull"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    /**
     * Which body axis this gene drives. A breed uses it at <b>founder time</b> to
     * decide whether to double this locus up and what to write on the copies
     * ({@code BreedFounder}); nothing consults it when a horse's body is
     * resolved, which is what stops a bred line being pulled back toward its
     * breed's standard.
     */
    public StatAxis axis() {
        return StatAxis.PULL;
    }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int more = pair.count(Strong);
        int less = pair.count(Weak);
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
     * Both copies, added. {@code Strong} counts positive and {@code Weak}
     * negative, so every combination falls out of one line and the balanced pair
     * cancels without a special case.
     */
    @Override
    public void contribute(AllelePair pair, Genotype genotype, GeneEpigenetics epigenetics,
                           TraitBuilder out) {
        double sum = signedDelta(pair.first(), epigenetics.copy(0))
                + signedDelta(pair.second(), epigenetics.copy(1));
        if (sum != 0.0) {
            out.addPull(sum);
        }
    }

    private double signedDelta(Allele allele, EpiValues epigenetics) {
        if (allele.equals(Strong)) {
            return epigenetics.get(DELTA);
        }
        if (allele.equals(Weak)) {
            return -epigenetics.get(DELTA);
        }
        return 0.0; // the baseline allele is worth nothing, as everywhere else
    }

    /**
     * The one number a copy of this locus carries: the points it is worth.
     * Always positive - the sign is the allele's job, not the value's - and
     * always readable, which is the entire point of storing it.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.gaussian(DELTA, MEAN_DELTA, SIGMA_DELTA, MIN_DELTA));
    }
}
