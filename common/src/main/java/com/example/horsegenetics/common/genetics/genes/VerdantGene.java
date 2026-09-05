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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Verdant</b> ({@code horsegenetics.verdant}) - a <b>magical</b> gene. A
 * horse that carries two matching copies changes the ground it walks on. It
 * paints nothing at all.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>any single variant copy, and any two <i>different</i> variants</td>
 *       <td>{@code verdant-carrier} - a wild type; nothing happens</td></tr>
 *   <tr><td>{@code mush/mush}</td><td>{@code mycelium} - mycelium spreads from the hooves</td></tr>
 *   <tr><td>{@code moss/moss}</td><td>{@code moss} - moss spreads from the hooves</td></tr>
 *   <tr><td>{@code grass/grass}</td><td>{@code grass} - grass spreads from the hooves</td></tr>
 * </table>
 *
 * <h2>Three recessives at one locus</h2>
 * Every variant needs two of <i>itself</i>: {@code mush/moss} is not half of
 * each, it is nothing. That is the rule the user asked for and it is a genuinely
 * different shape from the rest of the mod - {@link MilkGene}'s two variants are
 * also mutually recessive but the pairing of them is <i>lethal</i>, where this
 * one is merely inert. Both are combination tables and neither needs a word for
 * what it is doing.
 *
 * <p>The consequence is that a verdant horse takes real work: with the commonest
 * variant at {@value #WILD_GRASS_FREQUENCY} of the founder population, about one
 * wild horse in a hundred and fifty spreads grass, and the two carriers who
 * could throw one look exactly like every other horse. Three variants at one
 * locus also means a line bred for one of them is <i>drifting away</i> from the
 * other two, which is the sort of decision this model exists to create.
 *
 * <h2>What the spreading is</h2>
 * The gene names a <b>cover</b> - {@code mycelium}, {@code moss}, {@code grass}
 * - not a block id, and the translator decides what that means: which blocks
 * convert to what, and which it leaves alone. That judgement needs to know the
 * game's blocks, so it lives on the game side; the gene only says which of the
 * three a horse carries. See {@code wiki/gene-effects.html}'s {@code spread}
 * verb.
 */
public final class VerdantGene implements Gene, AbilityContribution {

    public static final String KEY = "horsegenetics.verdant";
    public static final int PRIORITY = 180;

    public static final double WILD_MUSH_FREQUENCY = 0.06;
    public static final double WILD_MOSS_FREQUENCY = 0.07;
    public static final double WILD_GRASS_FREQUENCY = 0.08;

    /** How far from the horse a block can be converted, in blocks. */
    public static final double SPREAD_RADIUS = 2.0;
    /** Ticks between attempts. */
    public static final int SPREAD_INTERVAL_TICKS = 60;
    /** Probability one attempt actually converts something. */
    public static final double SPREAD_CHANCE = 0.35;

    public final Allele mush = new Allele(KEY, 0, "mush", "Mycelium (mush)");
    public final Allele moss = new Allele(KEY, 1, "moss", "Moss (moss)");
    public final Allele grass = new Allele(KEY, 2, "grass", "Grass (grass)");
    public final Allele n = new Allele(KEY, 3, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(mush, moss, grass, n);

    private final Expression WILD = Expression.wildType("The ground is left alone.");

    private final Expression CARRIER = Expression.wildType("verdant-carrier", "Verdant carrier",
            "One variant copy, or one of two different ones. Nothing happens: every variant here "
                    + "needs two of itself, so a mycelium copy beside a moss copy is inert and the "
                    + "horse passes both on.");

    private final Expression MYCELIUM = Expression.wildType("mycelium", "Mycelium-bearing",
            "Two mycelium copies. Mycelium creeps outward from wherever the horse stands, taking "
                    + "over dirt and grass, and the mushrooms follow.");

    private final Expression MOSSY = Expression.wildType("moss", "Moss-bearing",
            "Two moss copies. Moss creeps outward from wherever the horse stands, over stone and "
                    + "dirt alike.");

    private final Expression GRASSY = Expression.wildType("grass", "Grass-bearing",
            "Two grass copies. Bare dirt greens over wherever the horse stands - the gentlest of "
                    + "the three, and the only one that undoes damage rather than doing it.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, MYCELIUM, MOSSY, GRASSY);

    private final FounderTable founders = FounderTable.hardyWeinberg(frequencies(), pair -> true);

    /** Baseline last, and a {@link LinkedHashMap} - see {@link MilkGene#frequencies()}. */
    private Map<Allele, Double> frequencies() {
        Map<Allele, Double> p = new LinkedHashMap<>();
        p.put(mush, WILD_MUSH_FREQUENCY);
        p.put(moss, WILD_MOSS_FREQUENCY);
        p.put(grass, WILD_GRASS_FREQUENCY);
        p.put(n, 1.0 - WILD_MUSH_FREQUENCY - WILD_MOSS_FREQUENCY - WILD_GRASS_FREQUENCY);
        return p;
    }

    private final List<GeneAbility> myceliumAbility = List.of(spread("mycelium"));
    private final List<GeneAbility> mossAbility = List.of(spread("moss"));
    private final List<GeneAbility> grassAbility = List.of(spread("grass"));

    @Override public String key() { return KEY; }
    @Override public String name() { return "Verdant"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.homozygousFor(mush)) {
            return MYCELIUM;
        }
        if (pair.homozygousFor(moss)) {
            return MOSSY;
        }
        if (pair.homozygousFor(grass)) {
            return GRASSY;
        }
        return pair.homozygousFor(n) ? WILD : CARRIER;
    }

    /** The cover this combination spreads, or {@code ""} - for the wiki and the info surfaces. */
    public String coverOf(AllelePair pair) {
        if (pair.homozygousFor(mush)) {
            return "mycelium";
        }
        if (pair.homozygousFor(moss)) {
            return "moss";
        }
        return pair.homozygousFor(grass) ? "grass" : "";
    }

    @Override
    public List<GeneAbility> abilitiesFor(AllelePair pair, Genotype genotype) {
        if (pair.homozygousFor(mush)) {
            return myceliumAbility;
        }
        if (pair.homozygousFor(moss)) {
            return mossAbility;
        }
        return pair.homozygousFor(grass) ? grassAbility : List.of();
    }

    private static GeneAbility spread(String cover) {
        return new GeneAbility.Spread(cover, SPREAD_RADIUS, SPREAD_CHANCE, SPREAD_INTERVAL_TICKS,
                GeneAbility.Condition.ALWAYS, 1);
    }
}
