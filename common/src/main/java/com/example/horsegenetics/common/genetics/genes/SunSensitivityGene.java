package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.GeneRarity;

import java.util.List;

/**
 * <b>Sun sensitivity</b> ({@code horsegenetics.sun_sensitivity}) - a magical
 * recessive whose homozygote is burned by daylight, and the behaviour half of
 * what used to be the dhampir gene.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Sun/n}</td><td>a silent carrier</td></tr>
 *   <tr><td>{@code Sun/Sun}</td><td>{@code sun-sensitive} - burns under open sky by day</td></tr>
 * </table>
 *
 * <h2>What is not in this file</h2>
 * Burning, and running for shade or water, are behaviour, and behaviour lives in
 * the game module - {@code neoforge/server/SunSensitivityHandler} and
 * {@code SunShadeGoal}. They are deliberately not {@code effects} verbs, for the
 * reason the dhampir gene gave: each would be a verb with exactly one user.
 *
 * <p>Paints nothing, so every outcome is a {@link Expression#wildType() wild
 * type} and the locus is out of the texture key. Not spliceable: nothing in the
 * body-derived splice blacklist can see a sunburn.
 */
public final class SunSensitivityGene implements Gene {

    public static final String KEY = "horsegenetics.sun_sensitivity";

    /** In the magical behaviour band, beside night temper and night watch. */
    public static final int PRIORITY = 138;

    /** How many founders in a hundred carry one silent copy. None carry two. */
    public static final double CARRIER_PERCENT = 3.0;

    public final Allele Sun = new Allele(KEY, 0, "Sun", "Sun sensitivity (Sun)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Sun, n);

    private final Expression WILD = Expression.wildType(
            "An ordinary horse in daylight.");

    private final Expression CARRIER = Expression.wildType("sun-sensitivity-carrier",
            "Sun sensitivity carrier",
            "One copy, which shows nothing: the horse stands in the sun like any other. Two "
                    + "carriers bred together is the only way a sun-sensitive horse appears.");

    private final Expression SENSITIVE = Expression.wildType("sun-sensitive", "Sun-sensitive",
            "Burned by daylight. Under open sky by day it takes half a heart every two seconds "
                    + "and runs for the nearest deep cover or water, jumping a fence to get there, "
                    + "and it stays under cover until nightfall. Rain, water and a roof all keep "
                    + "it safe.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, SENSITIVE);

    /** Carriers only - a horse that burns in daylight would not have survived to be caught. */
    private final FounderTable founders = FounderTable.builder()
            .weight(Sun, n, CARRIER_PERCENT)
            .weight(n, n, 100.0 - CARRIER_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Sun sensitivity"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }
    @Override public boolean spliceable() { return false; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        int copies = pair.count(Sun);
        if (copies == 2) {
            return SENSITIVE;
        }
        return copies == 1 ? CARRIER : WILD;
    }

    /** Does daylight burn this horse? */
    public boolean isSensitive(AllelePair pair) {
        return pair != null && pair.count(Sun) == 2;
    }
}
