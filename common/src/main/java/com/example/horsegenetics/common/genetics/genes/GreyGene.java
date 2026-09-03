package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.GreyCoat;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.List;

/**
 * <b>Grey</b> ({@code horsegenetics.grey}).
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code g/g}</td><td>wild type</td></tr>
 *   <tr><td>{@code G/g}, {@code G/G}</td><td>{@code grey} - a dapple grey, on adults only</td></tr>
 * </table>
 *
 * <p>Natural: it remaps the coat onto the gradient's neutral column, so it
 * lightens without shifting hue. <b>Non-deterministic</b> - the expressing
 * {@code G} copy's epigenetics decide how far along this horse's greying is
 * (dark steel through mid dapple to nearly white), how big and how pronounced
 * its dapples are, and how long its mane / tail / legs hold their colour. See
 * {@link GreyCoat}.
 *
 * <p>A <b>foal is born its base colour</b>: the outcome is the grey one either
 * way (so a grey foal bakes its own texture), but the painter returns nothing
 * until the horse is an adult. Real greying advances with age; the pipeline has
 * no age input past adult / foal, so a horse's stage is drawn once and fixed for
 * life - aging is out of scope by design, see {@code wiki/philosophy.html}.
 *
 * <p>Founder frequency {@code 1/}{@value #WILD_GREY_ONE_IN} per allele.
 */
public final class GreyGene implements Gene {

    public static final String KEY = "horsegenetics.grey";
    public static final int WILD_GREY_ONE_IN = 16;

    public final Allele G = new Allele(KEY, 0, "G", "Grey (G)");
    public final Allele g = new Allele(KEY, 1, "g", "Wild-type (g)");
    private final List<Allele> alleles = List.of(G, g);

    private final Expression WILD = Expression.wildType("The coat keeps its colour for life.");

    private final Expression GREY = Expression.of("grey", "Grey")
            .describe("The coat is remapped onto neutral greys and dappled - anything from dark steel "
                    + "to nearly white, with the mane, tail and legs holding their colour longest. "
                    + "A foal is born its base colour and greys on reaching adulthood.")
            .varies()
            .restrict((ctx, coat) -> {
                if (!ctx.isAdult()) {
                    return null; // a foal is born its base colour
                }
                PigmentField f = coat.mutableCopy();
                GreyCoat.apply(ctx, f, ctx.epigeneticsFor(KEY));
                return f;
            });

    private final List<Expression> expressions = List.of(WILD, GREY);

    private final FounderTable founders = FounderTable.hardyWeinberg(G, g, 1.0 / WILD_GREY_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Grey"; }
    @Override public int priority() { return 55; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return g; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(G) ? GREY : WILD;
    }

    public boolean isGrey(AllelePair pair) {
        return pair.has(G);
    }
}
