package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.ColorField;
import com.example.horsegenetics.common.coat.pattern.TestCoatPattern;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.List;

/**
 * <b>Test</b> ({@code horsegenetics.test}) - the diagnostic gene.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code t/t}</td><td>wild type</td></tr>
 *   <tr><td>{@code T/t}, {@code T/T}</td><td>{@code test-overlay} - the diagnostic gradient, flat on top; <b>masks</b> everything</td></tr>
 * </table>
 *
 * <p><b>Magical</b> - it paints a {@link TestCoatPattern} gradient
 * (pink&rarr;blue along body X, red&rarr;yellow along body Y) flat over the
 * resolved coat in phase 3, so the full colourful field is visible on any base:
 * black, chestnut, or dominant white.
 *
 * <p>Its founder table is the clearest reason the model declares frequency per
 * <b>combination</b>: a quarter of wild horses are {@code T/t} and
 * <b>none at all</b> are {@code T/T}. A per-allele frequency cannot say that -
 * it would always imply a {@code p²} homozygote rate. Deliberately common while
 * the skin engine is being built; expect the gene removed once it is trusted.
 */
public final class TestGene implements Gene {

    public static final String KEY = "horsegenetics.test";

    /** Share of founder horses carrying one {@code T}. */
    public static final double WILD_TEST_CARRIER_PERCENT = 25.0;

    public final Allele T = new Allele(KEY, 0, "T", "Test (T)");
    public final Allele t = new Allele(KEY, 1, "t", "Wild-type (t)");
    private final List<Allele> alleles = List.of(T, t);

    private final Expression WILD = Expression.wildType("No diagnostic overlay.");

    /**
     * Flat, opaque paint - the one magical outcome that {@link ColorField#set}s
     * instead of adding, because a diagnostic gradient is only useful if it
     * reads the same over black, chestnut and dominant white alike.
     */
    private final Expression OVERLAY = Expression.of("test-overlay", "Test overlay")
            .describe("A flat diagnostic gradient painted over the whole horse - pink to blue along "
                    + "its length, red to yellow up its height - hiding whatever coat is underneath.")
            .masking()
            .tint((ctx, coat, accumulated) -> {
                ColorField delta = ColorField.deltaLike(accumulated);
                TestCoatPattern pattern = new TestCoatPattern(HorseSkinGeometry.bodyBounds(ctx.skin()));
                HorseSkinGeometry.forEachTexel(ctx.skin(), (px, py, part, face, point) -> {
                    int argb = pattern.argb(point.x(), point.y(), point.z());
                    delta.set(px, py, argb >>> 24, (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
                });
                return delta;
            });

    private final List<Expression> expressions = List.of(WILD, OVERLAY);

    /** A quarter of founders are carriers; the homozygote never occurs in the wild. */
    private final FounderTable founders = FounderTable.builder()
            .weight(T, t, WILD_TEST_CARRIER_PERCENT)
            .weight(t, t, 100.0 - WILD_TEST_CARRIER_PERCENT)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Test"; }
    @Override public int priority() { return 900; }
    @Override public boolean isNatural() { return false; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return t; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(T) ? OVERLAY : WILD;
    }

    public boolean isTest(AllelePair pair) {
        return pair.has(T);
    }
}
