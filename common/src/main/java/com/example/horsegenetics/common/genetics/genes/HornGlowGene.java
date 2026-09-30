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
 * <b>Horn glow</b> ({@code horsegenetics.horn_glow}) - a <b>magical recessive</b>
 * that makes a unicorn's horn glow in the dark, whatever colour it is. On a horse
 * with no horn it does nothing.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code n/n}</td><td>wild type</td></tr>
 *   <tr><td>{@code Glw/n}</td><td>a wild type - nothing shows</td></tr>
 *   <tr><td>{@code Glw/Glw}</td><td>{@code horn-glow} - the horn glows</td></tr>
 * </table>
 *
 * <p>The glow is the horn's own colour drawn a second time at full brightness, so
 * a red horn glows red and a two-tone horn glows both its colours; a black horn
 * glows too, as a dark shape that stays visible at night. It used to be a
 * one-in-eight epigenetic roll on the unicorn locus, and is a locus of its own so
 * that it can be bred for (owner's call, 2026-09-30).
 *
 * <h2>No wild carriers</h2>
 * A wild horse is either {@code Glw/Glw} or {@code n/n}, never a carrier - the
 * rule every recessive of this kind follows, so the allele is in the wild in a
 * form that can show. It still only shows when the horse also has a horn, so most
 * of those founders are glowing-horn horses without a horn to glow.
 */
public final class HornGlowGene implements Gene {

    public static final String KEY = "horsegenetics.horn_glow";

    /** Magical band, beside {@link HornColourGene}. */
    public static final int PRIORITY = 228;

    /**
     * Founders born {@code Glw/Glw}, in percent. About one in eight, which is the
     * share of unicorns that glowed back when this was an epigenetic roll.
     */
    public static final double WILD_HOMOZYGOUS_PERCENT = 12.5;

    public final Allele Glw = new Allele(KEY, 0, "Glw", "Glowing horn (Glw)");
    public final Allele n = new Allele(KEY, 1, "n", "Wild-type (n)");
    private final List<Allele> alleles = List.of(Glw, n);

    private final Expression WILD = Expression.wildType("A horn, if the horse has one, that does not glow.");

    private final Expression CARRIER = Expression.wildType("horn-glow-carrier", "Horn glow carrier",
            "One copy, which shows nothing. Two carriers bred together are how a glowing horn "
                    + "appears in a line that lacks one.");

    private final Expression GLOWING = Expression.wildType("horn-glow", "Glowing horn",
            "The horn glows in the dark, in whatever colour it is. Shows only on a horse with a "
                    + "horn - on any other horse this does nothing.");

    private final List<Expression> expressions = List.of(WILD, CARRIER, GLOWING);

    private final FounderTable founders = FounderTable.builder()
            .weight(Glw, WILD_HOMOZYGOUS_PERCENT)
            .weight(n, 100.0 - WILD_HOMOZYGOUS_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Horn glow"; }
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

    /** Would this horse's horn glow, if it had one? Two copies, or nothing. */
    public boolean glows(AllelePair pair) {
        return pair != null && pair.count(Glw) == 2;
    }
}
