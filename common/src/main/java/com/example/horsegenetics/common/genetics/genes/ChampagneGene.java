package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.List;

/**
 * <b>Champagne</b> ({@code horsegenetics.champagne}) - a dilution that does not
 * read its own dose.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code c/c}</td><td>wild type</td></tr>
 *   <tr><td>{@code Ch/c}, {@code Ch/Ch}</td><td>{@code champagne} - the full dilution either way</td></tr>
 * </table>
 *
 * <p>Natural: it just moves the pigment sample. It keeps most of the red, cuts
 * black hard, and feeds part of the removed black back in as red
 * ({@link com.example.horsegenetics.common.coat.pattern.PigmentField#dilute}),
 * so it reads off the <i>current</i> pigment - gold champagne (on chestnut)
 * stays gold, classic champagne (on black) lands taupe, and amber champagne
 * (on bay) keeps <b>chocolate points</b> over a gold body instead of washing
 * the points out to the body colour. Champagne on a white horse is invisible.
 *
 * <p>Deterministic. Founder frequency {@code 1/}{@value #WILD_CHAMPAGNE_ONE_IN}
 * per allele.
 */
public final class ChampagneGene implements Gene {

    public static final String KEY = "horsegenetics.champagne";
    public static final int WILD_CHAMPAGNE_ONE_IN = 40;

    /** Pheomelanin kept - champagne barely touches red (gold champagne stays gold). */
    private static final float KEEP_RED = 0.55f;
    /** Eumelanin kept - hard, but not so hard that a black horse ends up gold. */
    private static final float KEEP_BLACK = 0.42f;
    /**
     * Fraction of a texel's eumelanin fed back in as pheomelanin. This is what
     * gives an <b>amber champagne</b> its chocolate points: bay's black points
     * carry no red at all, and without this term champagne washed them to the
     * same gold as the body.
     */
    private static final float BLACK_TINT = 0.30f;

    public final Allele Ch = new Allele(KEY, 0, "Ch", "Champagne (Ch)");
    public final Allele c = new Allele(KEY, 1, "c", "Wild-type (c)");
    private final List<Allele> alleles = List.of(Ch, c);

    private final Expression WILD = Expression.wildType("No dilution.");

    private final Expression CHAMPAGNE = Expression.of("champagne", "Champagne")
            .describe("Red mostly kept and black cut hard, with some of the removed black fed back as "
                    + "red - gold champagne on a chestnut, classic taupe on a black, amber with "
                    + "chocolate points on a bay. One copy and two look the same.")
            .restrict((ctx, coat) -> {
                PigmentField f = coat.mutableCopy();
                CoatRegions.restrictAll(ctx.skin(), f,
                        (field, px, py, p) -> field.dilute(px, py, KEEP_RED, KEEP_BLACK, BLACK_TINT));
                return f;
            });

    private final List<Expression> expressions = List.of(WILD, CHAMPAGNE);

    private final FounderTable founders = FounderTable.hardyWeinberg(Ch, c, 1.0 / WILD_CHAMPAGNE_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Champagne"; }
    @Override public int priority() { return 50; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return c; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(Ch) ? CHAMPAGNE : WILD;
    }

    public boolean isChampagne(AllelePair pair) {
        return pair.has(Ch);
    }
}
