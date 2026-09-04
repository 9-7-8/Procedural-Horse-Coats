package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatRegions;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
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
 * <b>Dun</b> ({@code horsegenetics.dun}) - real-horse {@code TBX3}, and the
 * mod's second <b>three-allele</b> locus after {@link MatpGene}.
 *
 * <p>The real gene carries three alleles whose relationship needs a table, not
 * a dominance label: {@code D} dilutes <i>and</i> marks, {@code d1} does not
 * dilute but still <i>marks</i>, and {@code d2} does neither. So dilution reads
 * {@code D > d1 = d2} while marking reads {@code D = d1 > d2} - two different
 * dominance orders over the same three alleles, which is exactly the shape a
 * single dominance label could not say. Six combinations, three outcomes:
 *
 * <table>
 *   <tr><th>combination</th><th>outcome</th></tr>
 *   <tr><td>{@code d2/d2}</td><td>wild type - no dilution, no markings</td></tr>
 *   <tr><td>{@code d1/d2}, {@code d1/d1}</td><td>{@code primitive-marks} - undiluted, but a dorsal stripe</td></tr>
 *   <tr><td>{@code D/d2}, {@code D/d1}, {@code D/D}</td><td>{@code dun} - diluted body, dorsal stripe <i>and</i> leg bars</td></tr>
 * </table>
 *
 * <h2>Which allele draws the stripe</h2>
 * The dorsal stripe belongs to <b>{@code D} and {@code d1} alike</b> - it is
 * the marking half of the locus, and it is the whole of what {@code d1} does.
 * {@code d2} is the only allele that draws nothing, which is why it, and not
 * the old catch-all {@code d}, is this gene's {@linkplain #defaultAllele()
 * baseline}.
 *
 * <h2>How an undiluted horse can show a darker stripe</h2>
 * Phase 1 is <b>downward-only</b> - a natural gene may never add pigment back,
 * so {@code d1} cannot literally paint a dark line onto an undiluted coat. It
 * does not need to: a primitive marking is <i>countershading</i>, and "a stripe
 * darker than the body" and "a body lighter than the stripe" are the same
 * picture. So both marked outcomes run the <b>same painter</b> and differ only
 * in their constants - {@code D} takes a real bite out of the body, {@code d1}
 * takes a little red off whatever has red to spare - and in both cases the
 * marking is the region that <i>skips</i> the dilution. That keeps the whole
 * locus inside the restrict-only contract with no special case.
 *
 * <p><b>Body dilution.</b> A mild, roughly hue-keeping lightening. On a black
 * base it must land on the gradient's <b>neutral column</b> (grullo is a
 * mouse-grey, not a warm brown), so the dilution feeds <b>no</b> black back in
 * as red; a red or bay base already carries red and comes out a paler tan.
 *
 * <p><b>Primitive markings.</b> A full-length <b>dorsal stripe</b> from poll to
 * tail ({@link CoatRegions#dorsalStripe}), on both marked outcomes; and faint
 * horizontal <b>leg barring</b> ({@link CoatRegions#legBar}), on {@code dun}
 * only - bars are the marking a real non-dun almost never shows, and a
 * {@code d1} horse's legs are usually the part of it with no red left to take.
 *
 * <p>Natural, deterministic. Founder allele frequencies
 * {@code 1/}{@value #WILD_DUN_ONE_IN} for {@code D} - unchanged, so the wild
 * population has exactly as many duns as before - and
 * {@code 1/}{@value #WILD_MARKED_ONE_IN} for {@code d1}, carved out of what
 * used to be one undifferentiated {@code d}. See {@code wiki/gene-dun.html}.
 */
public final class DunGene implements Gene {

    public static final String KEY = "horsegenetics.dun";
    /** Founder frequency of {@code D}: one allele copy in this many. */
    public static final int WILD_DUN_ONE_IN = 24;
    /** Founder frequency of {@code d1}: one allele copy in this many. */
    public static final int WILD_MARKED_ONE_IN = 10;

    /**
     * Body dilution under {@code D}. {@code keepRed} is <b>not a constant</b>:
     * a black horse still carries {@code red = 1} (its blackness is all
     * eumelanin), so keeping any of that red would land grullo in the warm
     * browns instead of on the gradient's neutral column. So keepRed is
     * interpolated by how much <i>black</i> the texel has - a chestnut (no
     * black) keeps almost all its red and comes out a pale red dun, a black one
     * loses nearly all of it and comes out a mouse-grey grullo, a bay body sits
     * between.
     */
    private static final float KEEP_RED_CHESTNUT = 0.85f;
    private static final float KEEP_RED_BLACK = 0.0f;
    private static final float KEEP_BLACK = 0.48f;

    /**
     * The same three numbers for {@code d1} - the <i>undiluted</i> outcome, so
     * they are chosen to move as little as possible while still letting the
     * stripe read. Two deliberate choices:
     * <ul>
     *   <li><b>Black is never touched</b> ({@code MARKED_KEEP_BLACK = 1}). The
     *       gradient's whole {@code black = 1} row is pure black, and the
     *       composer gives a texel that resolves to pure black
     *       <i>80% opacity</i> - so nudging a black texel off that row makes it
     *       fully opaque and therefore <b>darker</b>, and a body darker than
     *       its own dorsal stripe is worse than no stripe at all.</li>
     *   <li><b>Red is taken only where there is red to take</b> - keepRed runs
     *       <i>up</i> to 1 as the texel's black rises, the mirror image of
     *       {@code D}'s ramp <i>down</i> to 0. So a chestnut lightens a little,
     *       a bay body a little less, and a true black or a bay's points not at
     *       all. That last one is not a gap: a real non-dun black shows no
     *       primitive markings either.</li>
     * </ul>
     */
    private static final float MARKED_KEEP_RED_CHESTNUT = 0.86f;
    private static final float MARKED_KEEP_RED_BLACK = 1.0f;
    private static final float MARKED_KEEP_BLACK = 1.0f;

    private static final double DORSAL_HALF_WIDTH = 1.5;   // body units either side of the spine
    private static final double BAR_SPACING = 4.2;         // few, well-spaced bars
    private static final double BAR_DUTY = 0.28;           // thin
    private static final double BAR_REACH = 0.60;          // bars fade out above this fraction of the leg
    /** A leg bar keeps this much of its <i>black</i> (only) - so it reads as a dark band on the grullo
     *  grey, not as a re-saturated warm patch (which is what re-introducing red on a black base does). */
    private static final float BAR_KEEP_BLACK = 0.82f;

    public final Allele D = new Allele(KEY, 0, "D", "Dun (D)");
    public final Allele d1 = new Allele(KEY, 1, "d1", "Non-dun, marked (d1)");
    public final Allele d2 = new Allele(KEY, 2, "d2", "Non-dun, unmarked (d2)");
    private final List<Allele> alleles = List.of(D, d1, d2);

    private final Expression WILD = Expression.wildType("No dilution and no primitive markings.");

    private final Expression MARKED = Expression.of("primitive-marks", "Primitive markings")
            .describe("No real dilution - the horse is its base colour - but a dorsal stripe still runs "
                    + "from poll to tail, a shade darker than the body around it. One d1 copy is "
                    + "enough; it is how a plain bay or black ends up with a spine line and no dun.")
            .restrict(primitive(MARKED_KEEP_RED_CHESTNUT, MARKED_KEEP_RED_BLACK, MARKED_KEEP_BLACK, false));

    private final Expression DUN = Expression.of("dun", "Dun")
            .describe("The body lightens - dun on a bay, red dun on a chestnut, mouse-grey grullo on a "
                    + "black - while a dorsal stripe from poll to tail and faint horizontal bars on "
                    + "the legs skip the dilution and stay dark.")
            .restrict(primitive(KEEP_RED_CHESTNUT, KEEP_RED_BLACK, KEEP_BLACK, true));

    private final List<Expression> expressions = List.of(WILD, MARKED, DUN);

    /**
     * The six combinations at their Hardy-Weinberg shares given
     * {@code p(D) = 1/24} and {@code p(d1) = 1/10}. Written out rather than
     * computed so an author can retune one row without disturbing the others.
     * The three {@code D} rows still sum to the 8.16% of founders the old
     * two-allele table gave, so adding {@code d1} split the non-dun population
     * without making duns any rarer.
     */
    private final FounderTable founders = FounderTable.builder()
            .weight(D, D, 0.173611)
            .weight(D, d1, 0.833333)
            .weight(D, d2, 7.152778)
            .weight(d1, d1, 1.000000)
            .weight(d1, d2, 17.166667)
            .weight(d2, d2, 73.673611)
            .build();

    @Override public String key() { return KEY; }
    @Override public String name() { return "Dun"; }
    @Override public int priority() { return 34; }
    @Override public List<Allele> alleles() { return alleles; }
    @Override public Allele defaultAllele() { return d2; }
    @Override public List<Expression> expressions() { return expressions; }
    @Override public FounderTable founderTable(FounderContext context) { return founders; }

    @Override
    public Expression expressionOf(AllelePair pair) {
        if (pair.has(D)) {
            return DUN;
        }
        return pair.has(d1) ? MARKED : WILD;
    }

    /** Does this combination dilute the body - i.e. carry {@code D}? */
    public boolean isDun(AllelePair pair) {
        return pair.has(D);
    }

    /** Does this combination draw a dorsal stripe - {@code D} or {@code d1}? */
    public boolean isMarked(AllelePair pair) {
        return pair.has(D) || pair.has(d1);
    }

    /**
     * The one painter both marked outcomes use. The dilution is what makes the
     * markings visible: the marking regions are simply where it is lerped back
     * off, so a marking keeps whatever colour the horse would otherwise be.
     *
     * @param legBars whether to draw leg barring as well as the dorsal stripe
     */
    private static Expression.Pigment primitive(float keepRedChestnut, float keepRedBlack,
                                                float keepBlackBody, boolean legBars) {
        return (ctx, coat) -> {
            Skin skin = ctx.skin();
            PigmentField f = coat.mutableCopy();
            HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
                float r = f.red(px, py);
                float b = f.black(px, py);
                float keepRed = lerp(keepRedChestnut, keepRedBlack, b);
                float keepBlack = keepBlackBody;

                // dorsal stripe: skip the dilution entirely, so it keeps the base
                // colour (a near-black stripe on a grullo, a red one on a red dun).
                float dorsal = (float) CoatRegions.dorsalStripe(skin, part, point, DORSAL_HALF_WIDTH);
                keepRed = lerp(keepRed, 1f, dorsal);
                keepBlack = lerp(keepBlack, 1f, dorsal);

                // leg bars: keep extra *black* only - re-introducing red here would
                // turn a bar on a black leg into a warm patch instead of a dark band.
                if (legBars && isLeg(part)) {
                    float bar = (float) CoatRegions.legBar(skin, part, point, BAR_SPACING, BAR_DUTY, BAR_REACH);
                    keepBlack = Math.max(keepBlack, lerp(keepBlackBody, BAR_KEEP_BLACK, bar));
                }

                f.setRed(px, py, r * keepRed);
                f.setBlack(px, py, b * keepBlack);
            });
            return f;
        };
    }

    private static boolean isLeg(Part part) {
        return part == Part.LEFT_FRONT_LEG || part == Part.RIGHT_FRONT_LEG
                || part == Part.LEFT_HIND_LEG || part == Part.RIGHT_HIND_LEG;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
