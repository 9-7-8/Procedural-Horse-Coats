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
import com.example.horsegenetics.common.genetics.spec.GeneAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A conditioned multi-pair factor locus</b> - a positive and a negative
 * allele for <i>each</i> of several weather conditions, incompletely dominant,
 * with the size of each copy's effect written on that copy.
 *
 * <h2>Why this is not AbstractMagicFactorGene</h2>
 * It was originally specified as "nearly a copy" of {@link MagicSwimSpeedGene},
 * and that was wrong in a way worth recording. {@link AbstractMagicFactorGene}
 * is <b>strictly three alleles</b> - one up, one down, one wild type - so it
 * cannot carry a positive and a negative allele per weather type. What it does
 * provide, and what this class reuses, is the <i>inheritance</i>: codominance, a
 * percentage drawn per allele copy, and both copies adding with the down allele
 * counting negative.
 *
 * <p>The alternative was one locus per weather type, which is cheaper and
 * multiplies the locus count by the number of weathers, twice over once the twin
 * is counted. This shape was chosen instead because it keeps the count at two
 * <i>and</i> gives a genuinely interesting compound heterozygote: a horse with
 * one "better in rain" copy and one "worse in storms" copy is both of those
 * things, and the two never fight because their conditions never both hold.
 *
 * <h2>Both copies are read independently</h2>
 * Unlike the single-pair factor gene there is no single sum: each copy produces
 * its own conditioned modifier. Two copies of the same allele stack under the
 * same condition, which is where the "much better in rain" horse comes from.
 */
public abstract class AbstractWeatherGene implements Gene, EpigeneticAbilityContribution {

    /** The name of the one value every copy carries. */
    public static final String DELTA = "delta";

    /** Mean effect of one copy, its spread, and the floor that stops a copy pointing the wrong way. */
    public static final double MEAN_DELTA = 0.18;
    public static final double SIGMA_DELTA = 0.09;
    public static final double MIN_DELTA = 0.02;

    /** Share of wild founders carrying one variant copy, across all of them. */
    public static final double WILD_CARRIER_PERCENT = 20.0;

    /** One weather an allele pair may key off. */
    public record Weather(String token, String flag, String label) {
    }

    /** The weathers both loci offer. Snow is a biome question, not a weather one - see the flag. */
    public static final List<Weather> WEATHERS = List.of(
            new Weather("R", "raining", "rain"),
            new Weather("T", "thundering", "a storm"),
            new Weather("S", "snowing", "snowfall"));

    private final String key;
    private final int priority;
    private final String displayName;
    private final String attribute;

    /** Indexed by allele order: the weather it keys off, and whether it helps. */
    private record Variant(Allele allele, Weather weather, boolean positive) {
    }

    private final List<Variant> variants = new ArrayList<>();
    private final Allele n;
    private final List<Allele> alleles;
    private final List<Expression> expressions;
    private final Expression wild;
    private final Expression[] singles;
    private final Expression mixed;
    private final FounderTable founders;

    protected AbstractWeatherGene(String key, int priority, String displayName, String attribute,
                                  String statWord, String wildText) {
        this.key = key;
        this.priority = priority;
        this.displayName = displayName;
        this.attribute = attribute;

        for (Weather w : WEATHERS) {
            add(w, true, statWord);
            add(w, false, statWord);
        }
        this.n = new Allele(key, variants.size(), "n", "Wild-type (n)");

        List<Allele> all = new ArrayList<>();
        for (Variant v : variants) {
            all.add(v.allele());
        }
        all.add(n);
        this.alleles = List.copyOf(all);

        this.wild = Expression.wildType(wildText);
        this.mixed = Expression.wildType(idPrefix() + "-mixed", "Mixed weather sense",
                "Two different weather alleles. Both apply, and they never fight - each is gated "
                        + "on its own weather, and two kinds of weather do not both hold. A horse "
                        + "that is better in rain and worse in a storm is exactly that.");

        List<Expression> out = new ArrayList<>();
        out.add(wild);
        this.singles = new Expression[alleles.size()];
        for (Variant v : variants) {
            String name = (v.positive() ? "Better in " : "Worse in ") + v.weather().label();
            Expression e = Expression.wildType(idPrefix() + "-" + v.allele().token(), name,
                    "While " + v.weather().label() + " holds, the horse's " + statWord
                            + (v.positive() ? " rises" : " falls")
                            + " by the percentage written on that copy. Two copies of the same "
                            + "allele add, and the horse is unchanged in any other weather. It is "
                            + "the only kind of locus in the mod you cannot check on demand - you "
                            + "wait for the sky.");
            singles[v.allele().order()] = e;
            out.add(e);
        }
        out.add(mixed);
        this.expressions = List.copyOf(out);

        FounderTable.Builder b = FounderTable.builder();
        double each = WILD_CARRIER_PERCENT / variants.size();
        for (Variant v : variants) {
            b.weight(v.allele(), n, each);
        }
        this.founders = b.weight(n, n, 100.0 - WILD_CARRIER_PERCENT).build();
    }

    /**
     * <b>{@code +} and {@code v}, never {@code +} and {@code -}.</b> It was the
     * latter, and that was a real bug with a long fuse: {@code -} is
     * {@link com.example.horsegenetics.common.genetics.Genotype}'s own gene
     * separator, so any horse carrying a "suffers in" allele wrote a genotype
     * code its own parser could not read back
     * ({@code horsegenetics.weather_jump=T-/T-} splits into three segments, two
     * of them nonsense). Nothing caught it because it needs a founder to
     * actually roll one of these, which is a few percent of a few percent; it
     * surfaced when adding the eye loci shifted the founder RNG stream.
     */
    private void add(Weather w, boolean positive, String statWord) {
        String token = w.token() + (positive ? "+" : "v");
        String label = (positive ? "Thrives in " : "Suffers in ") + w.label();
        variants.add(new Variant(new Allele(key, variants.size(), token, label + " (" + token + ")"),
                w, positive));
    }

    /** Prefix for this locus's expression ids. */
    protected abstract String idPrefix();

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
        int a = pair.first().order();
        int b = pair.second().order();
        if (a == n.order()) {
            return wild;
        }
        if (b == n.order() || a == b) {
            return singles[a];
        }
        return mixed;
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.gaussian(DELTA, MEAN_DELTA, SIGMA_DELTA, MIN_DELTA));
    }

    /**
     * One conditioned modifier per variant copy. They are emitted separately
     * rather than summed because each carries its own condition - summing would
     * require them to share one, which is the whole thing this locus does not do.
     */
    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype,
                                          GeneEpigenetics epigenetics) {
        List<GeneAbility> out = new ArrayList<>(2);
        addCopy(out, pair.first(), epigenetics.copy(0).get(DELTA));
        addCopy(out, pair.second(), epigenetics.copy(1).get(DELTA));
        return List.copyOf(out);
    }

    private void addCopy(List<GeneAbility> out, Allele allele, double delta) {
        if (allele.order() == n.order()) {
            return;
        }
        Variant v = variants.get(allele.order());
        double amount = v.positive() ? delta : -delta;
        out.add(new GeneAbility.AttributeMod(attribute, "multiply_total", amount,
                new GeneAbility.Condition.Flag(v.weather().flag(), false), 1));
    }
}
