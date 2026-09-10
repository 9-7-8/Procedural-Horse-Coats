package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.List;

/**
 * <b>Eyesight locus</b> ({@code horsegenetics.eyesight}) - whether the horse was
 * built for the dark or for the daylight.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code Cav/Cav}</td><td>caveborn - faster in the dark, slower in the light</td></tr>
 *   <tr><td>{@code Day/Day}</td><td>daywalker - faster in the light, slower in the dark</td></tr>
 *   <tr><td>{@code Cav/Day}</td><td>nothing; the two cancel</td></tr>
 *   <tr><td>anything with an {@code n}</td><td>nothing</td></tr>
 * </table>
 *
 * <h2>Light level, not the clock</h2>
 * "Caveborn" that keys off {@code night} is a horse which is no faster in a
 * cave, and that reads as a bug on the first mining trip. Combining the existing
 * {@code night} and {@code sky_visible} flags to approximate "underground" was
 * the cheaper design and was rejected: it cannot tell a lit tunnel from a dark
 * one, and the lit tunnel is where the interesting trade-off lives. <b>Your own
 * torches slow your caveborn horse down</b>, and that is kept rather than fixed.
 *
 * <h2>The compound heterozygote needs no rule</h2>
 * {@code Cav/Day} cancels by arithmetic, not by a special case: the two are
 * opposite-signed modifiers on one attribute gated on opposite conditions, so
 * exactly one is ever active and they average out across a day. Nothing in the
 * translator has to know.
 *
 * <h2>A locus with no wrong answer</h2>
 * Most magical loci are ladders - one allele is better and the project is to
 * find it. Both of these are better somewhere, so the question a breeder faces
 * is not which is stronger but what the horse is <i>for</i>. The mod has very
 * few of those.
 */
public final class EyesightGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.eyesight";
    public static final int PRIORITY = 174;

    /** The attribute both alleles move, in opposite directions under opposite conditions. */
    public static final String ATTRIBUTE = "movement_speed";

    /** The bonus, and the matching penalty. Modest: this is a feel gene, not a stat gene. */
    public static final double BONUS = 0.18;
    public static final double PENALTY = -0.12;

    /** Share of wild founders showing each doubled form. */
    public static final double WILD_EACH_PERCENT = 1.6;

    private final Allele cav = new Allele(KEY, 0, "Cav", "Caveborn (Cav)");
    private final Allele day = new Allele(KEY, 1, "Day", "Daywalker (Day)");
    private final Allele n = new Allele(KEY, 2, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(cav, day, n);

    private final Expression wild = Expression.wildType(
            "The horse moves at the same pace whatever the light.");

    private final Expression caveborn = Expression.wildType("caveborn", "Caveborn",
            "Two caveborn copies. The horse moves noticeably better in the dark - underground at "
                    + "noon counts, because this reads the actual light and not the clock - and "
                    + "worse in bright light. Lighting your own tunnel slows it down, which is a "
                    + "real trade in a game where lighting a tunnel is also how you stop things "
                    + "spawning in it.");

    private final Expression daywalker = Expression.wildType("daywalker", "Daywalker",
            "Two daywalker copies. Faster in good light and slower in the dark - the plainly "
                    + "useful one for anybody who travels by day, and a liability in a cave.");

    private final Expression balanced = Expression.wildType("balanced", "Balanced",
            "One of each. They are opposite modifiers under opposite conditions, so exactly one "
                    + "is ever active and across a day they cancel. The horse is ordinary and "
                    + "throws both.");

    private final List<Expression> expressions = List.of(wild, caveborn, daywalker, balanced);

    private final FounderTable founders = FounderTable.builder()
            .weight(cav, cav, WILD_EACH_PERCENT)
            .weight(day, day, WILD_EACH_PERCENT)
            .weight(n, n, 100.0 - 2 * WILD_EACH_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Eyesight locus"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int c = pair.count(cav);
        int d = pair.count(day);
        if (c > 0 && d > 0) {
            return balanced;
        }
        if (c == 2) {
            return caveborn;
        }
        if (d == 2) {
            return daywalker;
        }
        return wild;
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        Expression e = expressionOf(pair);
        if (e == caveborn) {
            return List.of(mod(BONUS, dark(false)), mod(PENALTY, dark(true)));
        }
        if (e == daywalker) {
            return List.of(mod(BONUS, dark(true)), mod(PENALTY, dark(false)));
        }
        return List.of();
    }

    /** {@code dark}, optionally negated - "in the light" has no flag of its own and needs none. */
    private static GeneAbility.Condition dark(boolean negate) {
        return new GeneAbility.Condition.Flag("dark", negate);
    }

    private static GeneAbility mod(double amount, GeneAbility.Condition when) {
        return new GeneAbility.AttributeMod(ATTRIBUTE, "multiply_total", amount, when, 1);
    }
}
