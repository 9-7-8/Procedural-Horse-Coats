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
 * <b>Dominant white</b> ({@code horsegenetics.white}).
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code w/w}</td><td>wild type</td></tr>
 *   <tr><td>{@code W/w}, {@code W/W}</td><td>{@code white} - both pigments gone everywhere; <b>masks</b> every other gene</td></tr>
 * </table>
 *
 * <p>The white outcome is the model's one true epistatic result: the overlay
 * goes fully transparent, the white template shows through unchanged, and
 * nothing another natural gene did can be seen. A <i>magical</i> gene still
 * can paint over it - white is natural, and phase 3 runs after phase 1.
 *
 * <p>Natural, deterministic. Founder frequency {@code 1/}{@value #WILD_WHITE_ONE_IN}
 * per allele. (White <i>markings</i> are the separate splash gene.)
 */
public final class WhiteGene implements Gene {

    public static final String KEY = "horsegenetics.white";
    public static final int WILD_WHITE_ONE_IN = 50;

    public final Allele W = new Allele(KEY, 0, "W", "Dominant white (W)");
    public final Allele w = new Allele(KEY, 1, "w", "Wild-type (w)");
    private final List<Allele> alleles = List.of(W, w);

    private final Expression WILD = Expression.wildType("Pigment is left alone.");

    private final Expression WHITE = Expression.of("white", "Dominant white")
            .describe("Every pigment removed over the whole body, so the horse renders pure white and "
                    + "no other coat gene it carries can be seen.")
            .masking()
            .restrict((ctx, coat) -> {
                PigmentField f = coat.mutableCopy();
                CoatRegions.restrictAll(ctx.skin(), f, (field, px, py, p) -> {
                    field.setRed(px, py, 0f);
                    field.setBlack(px, py, 0f);
                });
                return f;
            });

    private final List<Expression> expressions = List.of(WILD, WHITE);

    private final FounderTable founders = FounderTable.hardyWeinberg(W, w, 1.0 / WILD_WHITE_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Dominant white"; }
    @Override public int priority() { return 60; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return w; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(W) ? WHITE : WILD;
    }

    public boolean isWhite(AllelePair pair) {
        return pair.has(W);
    }
}
