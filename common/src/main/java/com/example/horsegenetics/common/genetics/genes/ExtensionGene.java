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
 * <b>Extension</b> ({@code horsegenetics.extension}) - can this horse make black
 * pigment at all.
 *
 * <p>Two alleles, so three combinations, and two outcomes:
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code E/E}, {@code E/e}</td><td>wild type - black pigment is made, nothing changes</td></tr>
 *   <tr><td>{@code e/e}</td><td>{@code chestnut} - black completely restricted, only red pheomelanin survives</td></tr>
 * </table>
 *
 * <p>Natural, deterministic. Founders: 25 / 50 / 25.
 */
public final class ExtensionGene implements Gene {

    public static final String KEY = "horsegenetics.extension";

    public final Allele E = new Allele(KEY, 0, "E", "Extension (E)");
    public final Allele e = new Allele(KEY, 1, "e", "Non-extension / red (e)");
    private final List<Allele> alleles = List.of(E, e);

    private final Expression WILD = Expression.wildType(
            "Black pigment is produced normally. Whether it is visible is agouti's business.");

    private final Expression CHESTNUT = Expression.of("chestnut", "Chestnut")
            .describe("No black pigment anywhere - the horse is red, from liver to flaxen chestnut, "
                    + "and every gene that only moves black pigment is invisible on it.")
            .restrict((ctx, coat) -> {
                PigmentField f = coat.mutableCopy();
                CoatRegions.restrictAll(ctx.skin(), f, (field, px, py, p) -> field.setBlack(px, py, 0f));
                return f;
            });

    private final List<Expression> expressions = List.of(WILD, CHESTNUT);

    private final FounderTable founders = FounderTable.builder()
            .weight(E, E, 25.0)
            .weight(E, e, 50.0)
            .weight(e, e, 25.0)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Extension"; }
    @Override public int priority() { return 10; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return E; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(E) ? WILD : CHESTNUT;
    }

    public boolean producesBlack(AllelePair pair) {
        return pair.has(E);
    }

    public boolean isChestnut(AllelePair pair) {
        return !pair.has(E);
    }
}
