package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.DominancePattern;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genotype;

import java.util.List;

/**
 * <b>Dun</b> ({@code horsegenetics.dun}) - real-horse {@code TBX3}. {@code D}
 * dominant, {@code d} wild-type. Natural, deterministic.
 *
 * <p>Two effects at once:
 * <ul>
 *   <li><b>Body dilution</b> - a mild, roughly hue-keeping lightening. On a
 *       black base it must land on the gradient's <b>neutral column</b>
 *       (grullo is a mouse-grey, not a warm brown), so the dilution feeds
 *       <b>no</b> black back in as red ({@code blackTint = 0}); a red or bay
 *       base already carries red and comes out a paler tan.</li>
 *   <li><b>Primitive markings</b> - the parts of the coat that <i>do not</i>
 *       dilute: a full-length <b>dorsal stripe</b> from poll to tail
 *       ({@link CoatRegions#dorsalStripe}) and faint horizontal <b>leg
 *       barring</b> ({@link CoatRegions#legBar}). They read dark because
 *       everything around them got lighter, not because pigment was added.</li>
 * </ul>
 *
 * <p>The real locus has three alleles ({@code D} / {@code d1} / {@code d2})
 * with a dominance order a single {@link DominancePattern} can't express; this
 * is the two-allele form. See {@code wiki/gene-dun.html}.
 */
public final class DunGene implements Gene {

    public static final String KEY = "horsegenetics.dun";
    public static final int WILD_DUN_ALLELE_ODDS = 24;

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

    public final Allele D = new Allele(KEY, "D", "Dun (D)", true, true);
    public final Allele d = new Allele(KEY, "d", "Non-dun (d)", false, true);
    private final List<Allele> alleles = List.of(D, d);

    @Override public String key() { return KEY; }
    @Override public int priority() { return 34; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele wildType() { return d; }

    /** Dominant: one {@code D} gives full dun. */
    @Override public DominancePattern dominance() { return DominancePattern.DOMINANT; }

    @Override
    public AllelePair randomPair(Rng rng) {
        return new AllelePair(
                rng.nextInt(WILD_DUN_ALLELE_ODDS) == 0 ? D : d,
                rng.nextInt(WILD_DUN_ALLELE_ODDS) == 0 ? D : d);
    }

    public boolean isDun(AllelePair pair) {
        return pair.has(D);
    }

    @Override
    public boolean isVisible(AllelePair pair, Genotype genotype) {
        return isDun(pair);
    }

    @Override
    public PigmentField restrict(AllelePair pair, CoatBuildContext ctx, PigmentView coat) {
        if (!isDun(pair)) {
            return null;
        }
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
