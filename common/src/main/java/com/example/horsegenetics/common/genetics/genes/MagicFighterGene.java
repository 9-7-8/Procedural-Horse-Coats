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
 * <b>Magic fighter</b> ({@code horsegenetics.magic_fighter}) - what the horse
 * hits for.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th><th>damage</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td><td>{@value #BASELINE_DAMAGE}</td></tr>
 *   <tr><td>{@code Gld/n}</td><td>{@code more}</td><td>baseline plus that copy's percentage</td></tr>
 *   <tr><td>{@code Gld/Gld}</td><td>{@code double-more}</td><td><b>both</b> copies' percentages, added</td></tr>
 *   <tr><td>{@code Wmp/n}</td><td>{@code less}</td><td>baseline less that copy's percentage</td></tr>
 *   <tr><td>{@code Wmp/Wmp}</td><td>{@code double-less}</td><td>both, subtracting - down to the floor</td></tr>
 *   <tr><td>{@code Gld/Wmp}</td><td>{@code balanced}</td><td>the wimp counts <b>against</b> the gladiator</td></tr>
 * </table>
 *
 * <h2>A wimp copy really does cancel a gladiator copy</h2>
 * That is the whole reason this locus is codominant and summed rather than
 * ranked. A {@code Gld/Wmp} horse is not "a gladiator that also carries a
 * wimp"; the two percentages are added with opposite signs and the horse comes
 * out near the baseline. Breeding a fighter therefore means breeding
 * <i>out</i> the wimp allele as much as breeding in the gladiator, and a
 * spectacular fighter crossed with a poor one gives an ordinary foal that
 * carries both - which is a much more interesting thing to own than a horse
 * that simply inherited the better half.
 *
 * <h2>No ceiling, but a wild horse is never a monster</h2>
 * A gladiator copy is worth {@value #WILD_GLADIATOR_MIN} to
 * {@value #WILD_GLADIATOR_MAX} of the baseline when it is <b>drawn for a
 * founder</b> - so the worst wild fighter adds a point of damage and the best
 * adds three. Nothing caps it after that. The epigenetic value drifts a little
 * with every breeding, so a line selected for it climbs past the wild range and
 * keeps climbing; that is not a loophole, it is the locus. What a player cannot
 * do is <i>catch</i> one.
 *
 * <h2>The floor is on the damage, not on the percentage</h2>
 * A wimp copy subtracts a share of the baseline, and two unlucky ones would
 * take a horse to zero or below. Zero damage is not "a very weak horse", it is
 * a horse that cannot attack at all, which is a different animal. So the sum is
 * applied and then the <b>result</b> is floored at {@value #MIN_DAMAGE} point -
 * every horse can always hurt something, however feebly.
 */
public final class MagicFighterGene implements Gene, EpigeneticAbilityContribution {

    public static final String KEY = "horsegenetics.magic_fighter";
    public static final int PRIORITY = 146;

    /** What an ordinary horse hits for, in health points - two per heart. */
    public static final double BASELINE_DAMAGE = 3.0;

    /** The floor on the result, however many wimp copies pile up. See the class note. */
    public static final double MIN_DAMAGE = 1.0;

    /** The share of the baseline a gladiator copy is worth, when drawn for a founder. */
    public static final double WILD_GLADIATOR_MIN = 1.0 / 3.0;   // +1 damage
    public static final double WILD_GLADIATOR_MAX = 1.0;         // +3 damage

    /** The share a wimp copy takes off. */
    public static final double WILD_WIMP_MIN = 0.15;
    public static final double WILD_WIMP_MAX = 0.60;

    /** The percentage a gladiator copy is worth. Positive; the allele supplies the sign. */
    public static final String GLADIATOR_DELTA = "gladiator";
    /** The percentage a wimp copy takes off. Also positive, for the same reason. */
    public static final String WIMP_DELTA = "wimp";

    /** Share of wild horses carrying one copy of each variant. */
    public static final double WILD_GLADIATOR_PERCENT = 6.0;
    public static final double WILD_WIMP_PERCENT = 14.0;

    public final Allele Gld = new Allele(KEY, 0, "Gld", "Gladiator (Gld)");
    public final Allele Wmp = new Allele(KEY, 1, "Wmp", "Wimp (Wmp)");
    public final Allele n = new Allele(KEY, 2, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Gld, Wmp, n);

    private final Expression WILD = Expression.wildType(
            "The horse hits for " + (int) BASELINE_DAMAGE + " points - a heart and a half - which "
                    + "is what every horse without this locus does.");

    private final Expression MORE = Expression.wildType("more", "Fighter",
            "One gladiator copy. The horse hits harder than the baseline by the percentage "
                    + "written on that copy, and passes that exact percentage on with the allele. "
                    + "A wild one is worth between one and three extra points.");

    private final Expression DOUBLE_MORE = Expression.wildType("double-more", "Champion",
            "Two gladiator copies, and the percentages add. There is no ceiling on this and no "
                    + "wild horse is born with two, so every genuinely dangerous horse in a world "
                    + "is one somebody bred - and a line selected for it keeps climbing, because "
                    + "the percentage drifts a little with each generation.");

    private final Expression LESS = Expression.wildType("less", "Weak",
            "One wimp copy. The horse hits for less than the baseline by the percentage on that "
                    + "copy - and carries it silently into every cross.");

    private final Expression DOUBLE_LESS = Expression.wildType("double-less", "Harmless",
            "Two wimp copies, subtracting together. The horse hits for very little, and never "
                    + "for less than " + (int) MIN_DAMAGE + " point however far the percentages "
                    + "go: a horse that could not hurt anything at all would be a different "
                    + "animal, not a weaker one.");

    private final Expression BALANCED = Expression.wildType("balanced", "Balanced",
            "One gladiator copy and one wimp copy. They are added with opposite signs, so the "
                    + "wimp counts against the gladiator and the horse comes out near the "
                    + "baseline - while passing both on. This is why breeding a fighter means "
                    + "breeding the wimp allele out as much as breeding the gladiator in.");

    private final List<Expression> expressions =
            List.of(WILD, MORE, DOUBLE_MORE, LESS, DOUBLE_LESS, BALANCED);

    /** Carriers and the plain horse. Neither doubled form is ever born wild. */
    private final FounderTable founders = FounderTable.builder()
            .weight(Gld, n, WILD_GLADIATOR_PERCENT)
            .weight(Wmp, n, WILD_WIMP_PERCENT)
            .weight(n, n, 100.0 - WILD_GLADIATOR_PERCENT - WILD_WIMP_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic fighter"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int more = pair.count(Gld);
        int less = pair.count(Wmp);
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
     * Two values rather than one, unlike the other summed loci.
     *
     * <p>A copy only ever reads the one that matches its allele, so a single
     * shared "delta" would work arithmetically - but it would mean the two
     * alleles drew from the same distribution, and they should not. A gladiator
     * copy is worth a third to a whole baseline; a wimp copy takes off a
     * seventh to three fifths. Writing both on every copy costs one number and
     * keeps each allele's range its own, which is also what lets a carrier's
     * sheet say what it is carrying.
     */
    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(
                EpiValue.uniform(GLADIATOR_DELTA, WILD_GLADIATOR_MIN, WILD_GLADIATOR_MAX),
                EpiValue.uniform(WIMP_DELTA, WILD_WIMP_MIN, WILD_WIMP_MAX));
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype,
                                          GeneEpigenetics epigenetics) {
        double sum = signedDelta(pair.first(), epigenetics.copy(0))
                + signedDelta(pair.second(), epigenetics.copy(1));
        if (sum == 0.0) {
            return List.of();   // the wild type has no attack ability of its own
        }
        return List.of(new GeneAbility.Combat(damageFor(sum), GeneAbility.Condition.ALWAYS, 1));
    }

    /** The baseline moved by the summed percentage, floored - see the class note. */
    public static double damageFor(double sum) {
        return Math.max(MIN_DAMAGE, BASELINE_DAMAGE * (1.0 + sum));
    }

    private double signedDelta(Allele allele, EpiValues epigenetics) {
        if (allele.equals(Gld)) {
            return epigenetics.get(GLADIATOR_DELTA);
        }
        if (allele.equals(Wmp)) {
            return -epigenetics.get(WIMP_DELTA);
        }
        return 0.0;
    }
}
