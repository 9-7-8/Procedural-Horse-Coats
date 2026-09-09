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
 * <b>Magic milk volume</b> ({@code horsegenetics.magic_milk_volume}) - how many
 * times a day the horse can be filled from.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type - once, as everywhere else in the game</td></tr>
 *   <tr><td>{@code Mlk/n}</td><td>{@code more} - once plus that copy's number</td></tr>
 *   <tr><td>{@code Mlk/Mlk}</td><td>{@code double-more} - once plus <b>both</b> copies' numbers, added</td></tr>
 * </table>
 *
 * <h2>It governs a kind, not a gene</h2>
 * This locus does not know that {@link MilkGene} exists. It emits a
 * {@code charges} effect naming the yield <i>kind</i> {@code "milk"}, and every
 * yield tagged with that kind gets the extra uses - which today is the plain
 * milk, the water and the lava, all three of them on one other locus, and
 * tomorrow is whatever else declares the same kind.
 *
 * <p>That indirection is doing real work rather than being tidy. The two loci
 * cannot see each other's epigenome - a gene is handed its own values and
 * nobody else's, deliberately - so a design where the milk gene asked "how
 * volumous am I" could not have read the number at all. Naming a kind is the
 * only version of this that works, and it has the pleasant side effect that a
 * future gene producing some other fluid is governed by the volume locus for
 * free instead of by someone remembering.
 *
 * <h2>Codominant, and the copies add</h2>
 * One copy is worth {@value #WILD_EXTRA_MIN} to {@value #WILD_EXTRA_MAX} extra
 * fillings, written on the copy rather than on the allele and inherited with
 * it. Two copies add, so a homozygote from two good parents is worth
 * substantially more than either of them - which is the ordinary shape of a
 * production trait and the reason a dairy line is worth keeping rather than
 * merely worth starting.
 */
public final class MagicMilkVolumeGene implements Gene, EpigeneticAbilityContribution {

    public static final String KEY = "horsegenetics.magic_milk_volume";
    public static final int PRIORITY = 131;

    /** The kind of yield this grants extra uses of. {@link MilkGene} tags all three of its fillings with it. */
    public static final String KIND = MilkGene.YIELD_KIND;

    /** The name of the number a copy carries: how many extra fillings it is worth. */
    public static final String EXTRA = "extra";

    /** The range a founder's copy is drawn from. Breeding drift takes a line past it. */
    public static final double WILD_EXTRA_MIN = 1.0;
    public static final double WILD_EXTRA_MAX = 3.0;

    /** Share of wild horses carrying one copy. */
    public static final double WILD_CARRIER_PERCENT = 9.0;

    public final Allele Mlk = new Allele(KEY, 0, "Mlk", "Milky (Mlk)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Mlk, n);

    private final Expression WILD = Expression.wildType(
            "The horse can be filled from once a day, which is what every horse without this "
                    + "locus does.");

    private final Expression MORE = Expression.wildType("more", "Milky",
            "One milky copy. The horse can be filled from once plus the number written on that "
                    + "copy - usually one to three more times - before it has to wait out the "
                    + "day. Unlike most of the magical loci a single copy shows, so a milky horse "
                    + "can be found rather than only bred.");

    private final Expression DOUBLE_MORE = Expression.wildType("double-more", "Very milky",
            "Two milky copies, and the numbers add. Four to seven fillings a day from an average "
                    + "pair, and more from a line selected for it - the numbers drift a little "
                    + "every generation, so a dairy line keeps improving on the horses it "
                    + "started from.");

    private final List<Expression> expressions = List.of(WILD, MORE, DOUBLE_MORE);

    /** Carriers show, so an ordinary Hardy-Weinberg spread is honest here. */
    private final FounderTable founders = FounderTable.builder()
            .weight(Mlk, n, WILD_CARRIER_PERCENT)
            .weight(Mlk, Mlk, WILD_CARRIER_PERCENT * WILD_CARRIER_PERCENT / 100.0)
            .weight(n, n, 100.0 - WILD_CARRIER_PERCENT
                    - WILD_CARRIER_PERCENT * WILD_CARRIER_PERCENT / 100.0)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Magic milk volume"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Mlk)) {
            case 2 -> DOUBLE_MORE;
            case 1 -> MORE;
            default -> WILD;
        };
    }

    @Override
    public EpiSchema epiSchema() {
        return EpiSchema.of(EpiValue.uniform(EXTRA, WILD_EXTRA_MIN, WILD_EXTRA_MAX));
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype,
                                          GeneEpigenetics epigenetics) {
        int extra = extraOf(pair.first(), epigenetics.copy(0))
                + extraOf(pair.second(), epigenetics.copy(1));
        if (extra <= 0) {
            return List.of();
        }
        return List.of(new GeneAbility.YieldCharges(KIND, extra, GeneAbility.Condition.ALWAYS, 1));
    }

    /**
     * One copy's contribution, rounded. Rounded <b>per copy</b> rather than
     * after the sum, so that the number the horse's sheet shows for each copy
     * is the number that copy actually contributes - a heterozygote worth "2"
     * and a homozygote worth "2 + 3" both add up in front of the reader.
     */
    private int extraOf(Allele allele, EpiValues epigenetics) {
        return allele.equals(Mlk) ? Math.max(1, (int) Math.round(epigenetics.get(EXTRA))) : 0;
    }
}
