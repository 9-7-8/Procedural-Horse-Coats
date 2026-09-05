package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.AbilityContribution;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.spec.GeneAbility;
import com.example.horsegenetics.common.genetics.spec.GeneAbility.Condition;
import com.example.horsegenetics.common.genetics.spec.GeneAbility.Trigger;
import com.example.horsegenetics.common.trait.TraitBuilder;
import com.example.horsegenetics.common.trait.TraitContribution;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Milk</b> ({@code horsegenetics.milk}) - a <b>magical</b> gene, and the
 * first one whose whole effect is something a player does <i>to</i> the horse
 * rather than something they look at. It paints nothing at all.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>{@code mares-milk} - a mare gives milk</td></tr>
 *   <tr><td>{@code Watr/n}, {@code Lava/n}</td><td>{@code milk-carrier} - a mare gives milk; nothing else shows</td></tr>
 *   <tr><td>{@code Watr/Watr}</td><td>{@code water-milk} - any adult horse gives <b>water</b></td></tr>
 *   <tr><td>{@code Lava/Lava}</td><td>{@code lava-milk} - any adult horse gives <b>lava</b></td></tr>
 *   <tr><td>{@code Watr/Lava}</td><td>{@code milk-embryonic-lethal} - the two never share an embryo</td></tr>
 * </table>
 *
 * <h2>Why a single copy does nothing</h2>
 * Both variants are recessive to the wild type and to each other, which makes
 * this the mod's cleanest double-carrier locus: about one wild horse in seven
 * carries {@code Watr} and one in nine {@code Lava}, and neither shows. A player
 * who wants a lava horse cannot catch one - they have to notice that two
 * ordinary-looking mares threw one, or breed a line and find out. That is the
 * whole design of the health loci pointed at something worth having instead of
 * something to avoid.
 *
 * <h2>The lethal</h2>
 * {@code Watr/Lava} is an <b>embryonic lethal</b>: the pairing simply produces
 * no foal. It is the same code path as {@link MetGene} - the Mendelian draw is
 * untouched and the breeding handler reads the genotype it drew and cancels the
 * birth - and it has the same two consequences: {@link #canOccur} is false, so
 * the catalogue gives it no pen, and {@link #expressionOf} still answers for it
 * because parsing is tolerant.
 *
 * <p>Unlike the health loci it is <b>not</b> a
 * {@link com.example.horsegenetics.common.trait.HealthContribution}: the server
 * config's health switch governs genetic <i>disorders</i>, and two magical
 * fluids refusing to share an embryo is not a disorder, it is the rule of the
 * gene. Turning the disorders off must not quietly make a water/lava horse
 * possible.
 *
 * <h2>Sex</h2>
 * Milk is a mare's, as it is everywhere else in the game; water and lava are
 * not milk and come from any grown horse. That asymmetry is deliberate - the
 * ordinary reading of the locus stays ordinary, and the magical readings are
 * visibly not.
 */
public final class MilkGene implements Gene, TraitContribution, AbilityContribution {

    public static final String KEY = "horsegenetics.milk";
    public static final int PRIORITY = 130;

    /** Founder allele frequencies. Neither variant shows in one copy, so carriers are common. */
    public static final double WILD_WATR_FREQUENCY = 0.08;
    public static final double WILD_LAVA_FREQUENCY = 0.06;

    /** How long a horse needs between fillings, in ticks. Lava is worth more, so it takes longer. */
    public static final int MILK_COOLDOWN_TICKS = 200;
    public static final int WATER_COOLDOWN_TICKS = 200;
    public static final int LAVA_COOLDOWN_TICKS = 1200;

    public static final com.example.horsegenetics.common.trait.Condition EMBRYONIC_LETHAL =
            com.example.horsegenetics.common.trait.Condition.lethalAtConception(
                    "milk-embryonic-lethal", "Incompatible humours (milk)",
                    "One water copy and one lava copy. The two never share an embryo, so the "
                            + "pairing produces no foal at all.");

    public final Allele Watr = new Allele(KEY, 0, "Watr", "Water (Watr)");
    public final Allele Lava = new Allele(KEY, 1, "Lava", "Lava (Lava)");
    public final Allele n = new Allele(KEY, 2, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Watr, Lava, n);

    private final Expression MARES_MILK = Expression.wildType("mares-milk", "Mare's milk",
            "The ordinary horse. A grown mare can be milked into a bucket; nothing about the "
                    + "coat changes.");

    private final Expression CARRIER = Expression.wildType("milk-carrier", "Milk carrier",
            "One magical copy, which shows nothing - the horse still gives plain milk, and only "
                    + "if it is a mare. Two carriers bred together are the only way to see the rest "
                    + "of this locus.");

    private final Expression WATER = Expression.wildType("water-milk", "Water-bearing",
            "Two water copies. Any grown horse of either sex fills a bucket with water, "
                    + "indefinitely and anywhere.");

    private final Expression LAVA = Expression.wildType("lava-milk", "Lava-bearing",
            "Two lava copies. Any grown horse of either sex fills a bucket with lava, slowly. "
                    + "The horse is not harmed by it and does not set anything alight.");

    private final Expression LETHAL = Expression.wildType("milk-lethal", "Incompatible humours",
            "One water copy and one lava copy. The embryo never develops, so this pairing "
                    + "produces no foal.");

    private final List<Expression> expressions = List.of(MARES_MILK, CARRIER, WATER, LAVA, LETHAL);

    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), this::canOccur);

    /**
     * Baseline last, so the table's rows run rarest to commonest and a high
     * founder roll is the ordinary horse. A {@link LinkedHashMap} rather than
     * {@code Map.of}, because the iteration order <i>is</i> the row order and
     * {@code Map.of} shuffles it differently on every JVM start - which would
     * make a world's founders unreproducible.
     */
    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(Watr, WILD_WATR_FREQUENCY);
        p.put(Lava, WILD_LAVA_FREQUENCY);
        p.put(n, 1.0 - WILD_WATR_FREQUENCY - WILD_LAVA_FREQUENCY);
        return p;
    }

    private final List<GeneAbility> milkAbility = List.of(bucketOf("minecraft:milk_bucket",
            MILK_COOLDOWN_TICKS, new Condition.All(List.of(flag("adult"), flag("sex_female")))));

    private final List<GeneAbility> waterAbility =
            List.of(bucketOf("minecraft:water_bucket", WATER_COOLDOWN_TICKS, flag("adult")));

    private final List<GeneAbility> lavaAbility =
            List.of(bucketOf("minecraft:lava_bucket", LAVA_COOLDOWN_TICKS, flag("adult")));

    @Override public String key() { return KEY; }
    @Override public String name() { return "Milk"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(Watr)) {
            return WATER;
        }
        if (pair.homozygousFor(Lava)) {
            return LAVA;
        }
        if (pair.has(Watr) && pair.has(Lava)) {
            return LETHAL;
        }
        return pair.has(n) && (pair.has(Watr) || pair.has(Lava)) ? CARRIER : MARES_MILK;
    }

    /** No horse is ever born carrying one of each - the embryo does not develop. */
    @Override
    public boolean canOccur(AllelePair pair) {
        return !(pair.has(Watr) && pair.has(Lava));
    }

    @Override
    public void contribute(AllelePair pair, Genotype genotype, TraitBuilder out) {
        if (pair.has(Watr) && pair.has(Lava)) {
            out.condition(EMBRYONIC_LETHAL);
        }
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        if (pair.homozygousFor(Watr)) {
            return waterAbility;
        }
        if (pair.homozygousFor(Lava)) {
            return lavaAbility;
        }
        if (pair.has(Watr) && pair.has(Lava)) {
            return List.of(); // never born
        }
        return milkAbility;
    }

    /** What this horse fills a bucket with, for the wiki and the info surfaces. */
    public String yieldItem(AllelePair pair) {
        if (pair.homozygousFor(Watr)) {
            return "minecraft:water_bucket";
        }
        if (pair.homozygousFor(Lava)) {
            return "minecraft:lava_bucket";
        }
        return "minecraft:milk_bucket";
    }

    private static GeneAbility bucketOf(String produces, int cooldown, Condition when) {
        return new GeneAbility.Yield(new Trigger.OnInteract("minecraft:bucket"),
                "minecraft:bucket", produces, cooldown, when, 1);
    }

    private static Condition flag(String name) {
        return new Condition.Flag(name, false);
    }
}
