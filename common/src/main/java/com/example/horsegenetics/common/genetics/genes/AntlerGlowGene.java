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
 * <b>Antler glow</b> ({@code horsegenetics.antler_glow}) - a <b>magical recessive</b>
 * that lights the points of an antlered horse's rack: every tine's tip and the end of
 * each beam glow in the dark, in the antler's own colour. On a horse with no antlers
 * it does nothing.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Glw/n}</td><td>a wild type - nothing shows</td></tr>
 *   <tr><td>{@code Glw/Glw}</td><td>{@code antler-glow} - the points glow</td></tr>
 * </table>
 *
 * <p>The horn glow locus's twin, with one difference: a horn glows all over, an
 * antler only at its points, which is {@code PartKind.glowRegions}. The cost is the
 * same - the lit regions drawn a second time full-bright - and only a glowing horse
 * pays it.
 *
 * <h2>No wild carriers</h2>
 * A wild horse is {@code Glw/Glw} or {@code n/n}, never a carrier, so the allele is in
 * the wild in a form that can show - the rule every recessive of this kind follows.
 * Most of those founders have no antlers for it to light.
 */
public final class AntlerGlowGene implements Gene {

    public static final String KEY = "horsegenetics.antler_glow";

    /** Magical band, in the antler run. */
    public static final int PRIORITY = 232;

    /** Founders born {@code Glw/Glw}, in percent. */
    public static final double WILD_HOMOZYGOUS_PERCENT = 10.0;

    public final Allele Glw = new Allele(KEY, 0, "Glw", "Glowing antlers (Glw)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Glw, n);

    private final Expression WILD = Expression.wildType("Antlers, if the horse has any, that do not glow.");

    private final Expression CARRIER = Expression.wildType("antler-glow-carrier", "Antler glow carrier",
            "One copy, which shows nothing. Two carriers bred together are how glowing antlers "
                    + "appear in a line that lacks them.");

    private final Expression GLOWING = Expression.wildType("antler-glow", "Glowing antlers",
            "The points of the antlers - every tine's tip and the end of each beam - glow in the "
                    + "dark, in the antler's own colour. Shows only on a horse with antlers.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, GLOWING);

    private final FounderTable founders = FounderTable.builder()
            .weight(Glw, WILD_HOMOZYGOUS_PERCENT)
            .weight(n, 100.0 - WILD_HOMOZYGOUS_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Antler glow"; }
    @Override public int priority() { return PRIORITY; }
    @Override public boolean isNatural() { return false; }
    @Override public GeneRarity rarity() { return GeneRarity.RARE; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return n; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return switch (pair.count(Glw)) {
            case 2 -> GLOWING;
            case 1 -> CARRIER;
            default -> WILD;
        };
    }

    /** Would this horse's antlers glow, if it had any? Two copies, or nothing. */
    public boolean glows(AllelePair pair) {
        return pair != null && pair.count(Glw) == 2;
    }
}
