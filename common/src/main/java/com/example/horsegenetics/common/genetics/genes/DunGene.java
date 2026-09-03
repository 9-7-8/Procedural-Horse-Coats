package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.FounderContext;
import com.example.horsegenetics.common.genetics.FounderTable;
import com.example.horsegenetics.common.genetics.Gene;

import java.util.List;

/**
 * <b>Dun</b> ({@code horsegenetics.dun}) - real-horse {@code TBX3}.
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code d/d}</td><td>wild type</td></tr>
 *   <tr><td>{@code D/d}, {@code D/D}</td><td>{@code dun} - diluted body with primitive markings</td></tr>
 * </table>
 *
 * <p>Two effects at once:
 * <ul>
 *   <li><b>Body dilution</b> - a mild, roughly hue-keeping lightening. On a
 *       black base it must land on the gradient's <b>neutral column</b>
 *       (grullo is a mouse-grey, not a warm brown), so the dilution feeds
 *       <b>no</b> black back in as red; a red or bay base already carries red
 *       and comes out a paler tan.</li>
 *   <li><b>Primitive markings</b> - the parts of the coat that <i>do not</i>
 *       dilute: a full-length <b>dorsal stripe</b> from poll to tail
 *       ({@link CoatRegions#dorsalStripe}) and faint horizontal <b>leg
 *       barring</b> ({@link CoatRegions#legBar}). They read dark because
 *       everything around them got lighter, not because pigment was added.</li>
 * </ul>
 *
 * <p><b>The real locus has three alleles</b> - {@code D} (marked dun),
 * {@code d1} (undiluted but still faintly marked) and {@code d2} (neither) -
 * with a relationship the old dominance enum could not express, which is why
 * this shipped as the two-allele form. That constraint is gone: adding
 * {@code d1} and {@code d2} is now two alleles and a few more rows in the table
 * above. See {@code wiki/gene-dun.html}.
 *
 * <p>Natural, deterministic. Founder frequency {@code 1/}{@value #WILD_DUN_ONE_IN}
 * per allele.
 */
public final class DunGene implements Gene {

    public static final String KEY = "horsegenetics.dun";
    public static final int WILD_DUN_ONE_IN = 24;

    /**
     * Body dilution. {@code keepRed} is <b>not a constant</b>: a black horse
     * still carries {@code red = 1} (its blackness is all eumelanin), so
     * keeping any of that red would land grullo in the warm browns instead of
     * on the gradient's neutral column. So keepRed is interpolated by how much
     * <i>black</i> the texel has - a chestnut (no black) keeps almost all its
     * red and comes out a pale red dun, a black one loses nearly all of it and
     * comes out a mouse-grey grullo, a bay body sits between.
     */
    private static final float KEEP_RED_CHESTNUT = 0.85f;
    private static final float KEEP_RED_BLACK = 0.0f;
    private static final float KEEP_BLACK = 0.48f;

    private static final double DORSAL_HALF_WIDTH = 1.5;   // body units either side of the spine
    private static final double BAR_SPACING = 4.2;         // few, well-spaced bars
    private static final double BAR_DUTY = 0.28;           // thin
    private static final double BAR_REACH = 0.60;          // bars fade out above this fraction of the leg
    /** A leg bar keeps this much of its <i>black</i> (only) - so it reads as a dark band on the grullo
     *  grey, not as a re-saturated warm patch (which is what re-introducing red on a black base does). */
    private static final float BAR_KEEP_BLACK = 0.82f;

    public final Allele D = new Allele(KEY, 0, "D", "Dun (D)");
    public final Allele d = new Allele(KEY, 1, "d", "Non-dun (d)");
    private final List<Allele> alleles = List.of(D, d);

    private final Expression WILD = Expression.wildType("No dilution and no primitive markings.");

    private final Expression DUN = Expression.of("dun", "Dun")
            .describe("The body lightens - dun on a bay, red dun on a chestnut, mouse-grey grullo on a "
                    + "black - while a dorsal stripe from poll to tail and faint horizontal bars on "
                    + "the legs skip the dilution and stay dark.")
            .restrict(DunGene::paintDun);

    private final List<Expression> expressions = List.of(WILD, DUN);

    private final FounderTable founders = FounderTable.hardyWeinberg(D, d, 1.0 / WILD_DUN_ONE_IN);

    @Override public String key() { return KEY; }
    @Override public String name() { return "Dun"; }
    @Override public int priority() { return 34; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return d; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        return pair.has(D) ? DUN : WILD;
    }

    public boolean isDun(AllelePair pair) {
        return pair.has(D);
    }

    private static PigmentField paintDun(CoatBuildContext ctx, PigmentView coat) {
        Skin skin = ctx.skin();
        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            float r = f.red(px, py);
            float b = f.black(px, py);
            float keepRed = lerp(KEEP_RED_CHESTNUT, KEEP_RED_BLACK, b);
            float keepBlack = KEEP_BLACK;

            // dorsal stripe: skip the dilution entirely, so it keeps the base
            // colour (a near-black stripe on a grullo, a red one on a red dun).
            float dorsal = (float) CoatRegions.dorsalStripe(skin, part, point, DORSAL_HALF_WIDTH);
            keepRed = lerp(keepRed, 1f, dorsal);
            keepBlack = lerp(keepBlack, 1f, dorsal);

            // leg bars: keep extra *black* only - re-introducing red here would
            // turn a bar on a black leg into a warm patch instead of a dark band.
            if (isLeg(part)) {
                float bar = (float) CoatRegions.legBar(skin, part, point, BAR_SPACING, BAR_DUTY, BAR_REACH);
                keepBlack = Math.max(keepBlack, lerp(KEEP_BLACK, BAR_KEEP_BLACK, bar));
            }

            f.setRed(px, py, r * keepRed);
            f.setBlack(px, py, b * keepBlack);
        });
        return f;
    }

    private static boolean isLeg(Part part) {
        return part == Part.LEFT_FRONT_LEG || part == Part.RIGHT_FRONT_LEG
                || part == Part.LEFT_HIND_LEG || part == Part.RIGHT_HIND_LEG;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
