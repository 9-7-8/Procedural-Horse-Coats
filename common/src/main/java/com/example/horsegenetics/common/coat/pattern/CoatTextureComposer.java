package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.GeneEpigenetics;
import com.example.horsegenetics.common.genetics.EyeColor;
import com.example.horsegenetics.common.genetics.EyeColorContribution;
import com.example.horsegenetics.common.genetics.EyePatch;
import com.example.horsegenetics.common.genetics.EyePatchContribution;
import com.example.horsegenetics.common.genetics.EyePatches;
import com.example.horsegenetics.common.genetics.EyeSpread;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.genetics.LutContribution;
import com.example.horsegenetics.common.genetics.WhiteLockContribution;

import java.util.Optional;

/**
 * The three-phase coat pipeline. Turns a {@link Genotype} + its
 * {@link Epigenome} into a 128px ARGB coat texture for a given {@link Skin}
 * (adult vs foal) and age ({@code adult} - grey only greys adults), given the
 * white template and the red/black {@link GradientLut}.
 *
 * <ol>
 *   <li><b>natural (melanin) phase</b> - every texel starts max red + max
 *       black; each visible natural gene (in {@link Genes#naturalOrder()})
 *       returns a {@link PigmentField} with the pigment pushed further down.
 *       Downward only.</li>
 *   <li><b>resolve</b> - {@code (red, black)} -&gt; {@link GradientLut}, into
 *       the {@link ColorField}. Fully restricted -&gt; transparent; near
 *       black -&gt; composited at less than full opacity, so the template's
 *       hair shading survives where the horse is darkest.</li>
 *   <li><b>magical (RGB) phase</b> - each visible magical gene (in
 *       {@link Genes#magicalOrder()}) returns a signed RGB delta, folded into
 *       that colour field by integer addition (or, for flat paint, a replace).
 *       Nothing is capped to 0-255 until the field is converted. A final
 *       <b>shadow pass</b> then lifts any texel this phase painted true black
 *       up to {@link #SHADOW_FLOOR}, because the composite below is a multiply
 *       and black multiplies the template's shading away.</li>
 *   <li><b>composite</b> onto the template, alpha-aware, keeping template
 *       alpha.</li>
 *   <li><b>eyes</b> - copied verbatim from the template.</li>
 *   <li><b>overlay</b> - each gene implementing
 *       {@link CoatOverlayContribution} gets to write final pixels over the
 *       finished coat and to mark texels <b>emissive</b>. Almost nothing runs
 *       here; see {@link CoatOverlay} for the two things that have to.</li>
 * </ol>
 *
 * <p>The composer owns both fields; genes only ever see read-only views and
 * hand back their own contribution, so the whole bake is a fold and a gene can
 * be tested against a synthetic coat on its own.
 */
public final class CoatTextureComposer {

    /**
     * Opacity of a texel that resolves to <b>black</b>. The composite is a
     * multiply, so a fully opaque black texel scales the template to nothing
     * and the coat loses the template's hair shading exactly where the horse is
     * darkest - a flat, dead {@code #000000} patch. Compositing black at 80%
     * lets a fifth of the template through, which is what gives a black horse
     * its {@code #2C2C2C}-ish strand detail.
     */
    private static final int PURE_BLACK_ALPHA = 0xCC; // 80%

    /**
     * How far off black a colour can be and still get some of that softening,
     * as a maximum channel value.
     *
     * <p><b>This used to be an equality test against {@code #000000}</b>, which
     * quietly made the rule depend on one pixel of the gradient art being
     * exactly zero. It was: both bottom corners of the chart were black, so a
     * black horse ({@code red 1, black 1}) and a bay's points ({@code red 0,
     * black 1}) both softened. Then the chart's low-red column was recoloured
     * toward blue-grey, its corner moved to {@code #000104}, and every bay,
     * seal brown and bay dun on the server started wearing dead-flat black
     * points while a plain black horse kept its shading - a rendering rule
     * switched off by an art edit, with nothing to say so.
     *
     * <p>So the softening ramps out over this range instead of switching. The
     * ramp is deliberately wide enough that the effective multiply is
     * near-constant across it (0.200 at pure black, dipping to 0.184 around
     * {@code #202020} before climbing again), so no coat gets a visible step
     * where it crosses.
     */
    private static final int NEAR_BLACK = 0x30;

    /**
     * <b>The shadow floor.</b> Nothing phase 3 paints is darker than this in
     * every channel; the shadow pass lifts anything that is, keeping its hue.
     * See {@link ColorField#liftShadows} for why a black texel is a rendering
     * problem and not just a dark colour - the composite is a multiply, and
     * multiplying by zero takes the template's hair shading with it.
     *
     * <p>{@link #NEAR_BLACK} is the phase-2 answer to the same concern, and the
     * two are kept off each other's texels on purpose: that one softens the
     * <i>alpha</i> of a coat the gradient resolved dark, this one floors the
     * <i>colour</i> a magical gene painted - including flat paint, which sets
     * its own opacity and so never meets the alpha ramp at all. Applying both
     * to one texel lifts it twice, which put a black mane above the brightness
     * of the dark bay body under it.
     */
    private static final int SHADOW_FLOOR = 0x15;

    /**
     * A texel goes fully transparent (bald white template shows through) only
     * when <i>both</i> pigments are essentially <b>zero</b> - i.e. dominant
     * white ({@code W_}) or a splash marking, both of which
     * {@link PigmentField#whiten} at full strength, which lands on exactly
     * zero. This must stay far below any value a
     * <i>dilution</i> can legitimately leave behind: grey keeps 0.15, and grey
     * stacked on a double-dilute cream still lands near 0.012 - a near-white
     * coat that must resolve in the gradient, not vanish. (A 0.02 cutoff here
     * was turning grey cremello / grey perlino chestnuts and bays into flat
     * white horses.)
     */
    private static final float TRANSPARENT_EPS = 0.001f;

    private CoatTextureComposer() {}

    /**
     * A finished coat: the ARGB sheet, plus the texels a
     * {@link CoatOverlayContribution} marked full-bright ({@code null} when none
     * did, which is the ordinary case).
     */
    public record Baked(int[] argb, boolean[] emissive) {

        /** Does any gene on this horse want an emissive render pass at all? */
        public boolean hasEmissive() {
            return emissive != null;
        }
    }

    /** {@link #bake}'s pixels alone - what every caller but the emissive layer wants. */
    public static int[] compose(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult,
                                int[] template, GradientLut lut) {
        return bake(genotype, epigenome, skin, adult, template, LutSet.of(lut)).argb();
    }

    /** {@link #compose} with a {@link LutSet}, so the {@code LUT} locus can swap the phase-2 gradient. */
    public static int[] compose(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult,
                                int[] template, LutSet luts) {
        return bake(genotype, epigenome, skin, adult, template, luts).argb();
    }

    public static Baked bake(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult,
                             int[] template, GradientLut lut) {
        return bake(genotype, epigenome, skin, adult, template, LutSet.of(lut));
    }

    public static Baked bake(Genotype genotype, Epigenome epigenome, Skin skin, boolean adult,
                             int[] template, LutSet luts) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        if (template.length != n * n) {
            throw new IllegalArgumentException("template must be " + (n * n) + " ARGB pixels, got " + template.length);
        }

        CoatBuildContext ctx = new CoatBuildContext(genotype, epigenome, skin, adult);

        // The LUT locus (magical, phase-2): a horse homozygous for a variant
        // allele resolves its pigment against an unnatural gradient instead of
        // the red/black one. Selected here, out of band, because the swap is not
        // a phase-1 restriction or a phase-3 tint.
        GradientLut lut = luts.resolve(selectAlternateLut(genotype));

        // 1. natural phase - each gene's expression folds the pigment field further down.
        PigmentField pigment = naturalPhase(genotype, ctx, n);

        // 2. resolve - pigment through the gradient, into the colour field.
        // Texels this skin doesn't map are left at zero = fully transparent.
        ColorField colour = new ColorField(n);
        PigmentField resolved = pigment;
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            float r = resolved.red(px, py);
            float b = resolved.black(px, py);
            if (r <= TRANSPARENT_EPS && b <= TRANSPARENT_EPS) {
                return;
            }
            int rgb = lut.sample(r, b) & 0xFFFFFF;
            colour.setArgb(px, py, (nearBlackAlpha(rgb) << 24) | rgb);
        });

        // The white lock, if any gene asks for one. Seeded with the white the
        // melanin genes made, and grown after every magical gene paints - see
        // whiteLock / extendWhiteLock.
        boolean[] locked = whiteLock(genotype, pigment, skin);
        boolean[] lockedEyes = locked == null ? null : eyeMask(skin);

        // 3. magical phase - each gene's signed RGB delta accumulates.
        for (Gene gene : Genes.magicalOrder()) {
            Expression expression = gene.expressionIn(genotype.pair(gene), genotype);
            if (expression.wildType()) {
                continue;
            }
            ColorField delta = expression.tint(ctx, pigment, colour);
            if (delta != null) {
                colour.apply(delta, locked);
                if (locked != null) {
                    extendWhiteLock(locked, lockedEyes, colour, skin);
                }
            }
        }

        // 3a. shadow pass - nothing phase 3 painted leaves it true black.
        colour.liftShadows(SHADOW_FLOOR);

        // 4. composite onto the template, alpha-aware multiply.
        int[] out = new int[n * n];
        for (int i = 0; i < out.length; i++) {
            int t = template[i];
            int ta = t >>> 24;
            if (ta == 0) {
                out[i] = 0;
                continue;
            }
            int o = colour.argb(i % n, i / n);
            float oa = (o >>> 24) / 255f;
            int rr = blend((t >> 16) & 0xFF, (o >> 16) & 0xFF, oa);
            int gg = blend((t >> 8) & 0xFF, (o >> 8) & 0xFF, oa);
            int bb = blend(t & 0xFF, o & 0xFF, oa);
            out[i] = (ta << 24) | (rr << 16) | (gg << 8) | bb;
        }

        CoatRegions.redrawEyes(skin, out, template);

        // 5. overlay phase - final pixels and emissive texels, over the finished
        // coat. Runs after the eyes precisely so a gene can colour them.
        CoatOverlay overlay = new CoatOverlay(skin, out.clone());
        overlay.lock(locked);

        // 5a. Eye colour, before the general overlay pass, so a gene that wants
        // the whole eye (light's glowing gold, the leopard complex's white rim)
        // still gets the last word over the iris tint.
        paintEyes(genotype, epigenome, overlay, whiteCoverage(pigment, skin));

        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof CoatOverlayContribution contribution)) {
                continue;
            }
            AllelePair pair = genotype.pair(gene);
            if (gene.expressionIn(pair, genotype).wildType()) {
                continue;
            }
            contribution.overlay(pair, ctx, overlay);
        }
        overlay.applyTo(out);

        return new Baked(out, overlay.emissiveMask());
    }

    /**
     * The alternate-LUT key the {@code LUT} locus selects for this genotype, or
     * {@code null} for the natural gradient. The first gene implementing
     * {@link LutContribution} with a non-wild, non-empty answer wins - there is
     * only one such gene, so "first" is just defensiveness.
     */
    /**
     * Phase 1 on its own: the pigment field every texel carries <b>before</b> the
     * gradient lookup, with no colour resolved and no magical phase run.
     *
     * <p>This is what a tool needs to answer &ldquo;which part of the chart does
     * this coat actually read from?&rdquo; - the pair at each texel is exactly
     * what {@link GradientLut#sample} is handed, so
     * {@link GradientLut#chartX}/{@link GradientLut#chartY} turn it into a
     * position on the artwork. Deriving it from a <em>baked</em> sheet instead
     * cannot work: by then the colour has been multiplied by the hair template
     * and moved again by any phase-3 tint.
     *
     * <p>It runs the same loop {@link #bake} does, because it <em>is</em> that
     * loop - a second copy would drift the first time a gene changed how it
     * restricts.
     */
    public static PigmentField pigmentField(Genotype genotype, Epigenome epigenome,
                                            Skin skin, boolean adult) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        return naturalPhase(genotype, new CoatBuildContext(genotype, epigenome, skin, adult), n);
    }

    private static PigmentField naturalPhase(Genotype genotype, CoatBuildContext ctx, int n) {
        PigmentField pigment = new PigmentField(n);
        for (Gene gene : Genes.naturalOrder()) {
            Expression expression = gene.expressionIn(genotype.pair(gene), genotype);
            if (expression.wildType()) {
                continue;
            }
            PigmentField next = expression.restrict(ctx, pigment);
            if (next != null) {
                pigment = next;
            }
        }
        return pigment;
    }

    /**
     * <b>How white a texel has to be to be locked</b>, as a minimum on every
     * channel. Not an equality test against {@code #FFFFFF}: a magical gene
     * paints white with a {@code TOWARD} at 88-98% strength, which lands a few
     * counts short on every channel, and an equality test would lock the
     * natural white and silently miss all of it.
     */
    private static final int WHITE_MIN = 0xEE;

    /**
     * The texels the white lock covers, or {@code null} when no gene on this
     * horse asks for one (the ordinary case, and the reason every caller
     * downstream takes a nullable mask rather than an all-false array).
     *
     * <p>It starts as <b>the white the melanin genes made</b>: a texel phase 1
     * whitened all the way to zero pigment, which phase 2 leaves fully
     * transparent so the white template shows through. Those are exactly the
     * natural markings - a tobiano patch, a splash, a blaze, a stocking.
     *
     * <p><b>The eyes are cut back out of it</b>, and only the eyes. A blue eye
     * on a white face is pigment biology rather than a marking, and the eye
     * texels of a dominant-white horse are whitened along with the rest of the
     * head - so locking them would delete every eye-colour gene on precisely
     * the horses whose eyes are worth looking at.
     *
     * @see com.example.horsegenetics.common.genetics.WhiteLockContribution
     */
    private static boolean[] whiteLock(Genotype genotype, PigmentField pigment, Skin skin) {
        boolean locking = false;
        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof WhiteLockContribution lock)) {
                continue;
            }
            AllelePair pair = genotype.pair(gene);
            if (gene.expressionIn(pair, genotype).wildType()) {
                continue;
            }
            if (lock.locksWhite(pair, genotype)) {
                locking = true;
                break;
            }
        }
        if (!locking) {
            return null;
        }
        int n = HorseSkinGeometry.SHEET_SIZE;
        boolean[] locked = new boolean[n * n];
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (pigment.red(px, py) <= TRANSPARENT_EPS && pigment.black(px, py) <= TRANSPARENT_EPS) {
                locked[py * n + px] = true;
            }
        });
        boolean[] eyes = eyeMask(skin);
        for (int i = 0; i < locked.length; i++) {
            if (eyes[i]) {
                locked[i] = false;
            }
        }
        return locked;
    }

    /** The texels inside {@link CoatRegions#eyeRects}, as a sheet-sized mask. */
    private static boolean[] eyeMask(Skin skin) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        boolean[] eyes = new boolean[n * n];
        for (int[] rect : CoatRegions.eyeRects(skin)) {
            for (int y = rect[1]; y < rect[1] + rect[3]; y++) {
                for (int x = rect[0]; x < rect[0] + rect[2]; x++) {
                    if (x >= 0 && y >= 0 && x < n && y < n) {
                        eyes[y * n + x] = true;
                    }
                }
            }
        }
        return eyes;
    }

    /**
     * Grow the lock over whatever the gene that just painted left <b>white</b>.
     * This is the half that makes the rule "a white texel is final" rather than
     * "the natural markings win": white a magical gene puts down is locked from
     * the moment it lands, so the gene above it cannot take it away either.
     *
     * <p>{@code eyes} is passed in rather than recomputed because this runs once
     * per magical gene on the horse. Eye texels are never added, for the reason
     * {@link #whiteLock} gives - they were cut out of the seed and would
     * otherwise creep back in the first time anything painted one pale.
     */
    private static void extendWhiteLock(boolean[] locked, boolean[] eyes,
                                        ColorField colour, Skin skin) {
        int n = HorseSkinGeometry.SHEET_SIZE;
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            int i = py * n + px;
            if (locked[i] || eyes[i]) {
                return;
            }
            int argb = colour.argb(px, py);
            if ((argb >>> 24) < 0x08) {
                return;     // nothing painted here yet; the seed pass owns bare template
            }
            if (((argb >> 16) & 0xFF) >= WHITE_MIN
                    && ((argb >> 8) & 0xFF) >= WHITE_MIN
                    && (argb & 0xFF) >= WHITE_MIN) {
                locked[i] = true;
            }
        });
    }

    private static String selectAlternateLut(Genotype genotype) {
        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof LutContribution lut)) {
                continue;
            }
            if (gene.expressionIn(genotype.pair(gene), genotype).wildType()) {
                continue;
            }
            Optional<String> key = lut.alternateLut(genotype.pair(gene), genotype);
            if (key.isPresent()) {
                return key.get();
            }
        }
        return null;
    }

    /**
     * The opacity a resolved colour composites at: {@value #PURE_BLACK_ALPHA}
     * at black, ramping to fully opaque by {@link #NEAR_BLACK}. See that
     * constant for why this is a ramp and not an {@code == 0} test.
     */
    private static int nearBlackAlpha(int rgb) {
        int max = Math.max((rgb >> 16) & 0xFF, Math.max((rgb >> 8) & 0xFF, rgb & 0xFF));
        if (max >= NEAR_BLACK) {
            return 0xFF;
        }
        return 0xFF - (0xFF - PURE_BLACK_ALPHA) * (NEAR_BLACK - max) / NEAR_BLACK;
    }

    private static int blend(int templateCh, int overlayCh, float a) {
        float factor = (overlayCh / 255f) * a + (1f - a);
        int v = Math.round(templateCh * factor);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    // ------------------------------------------------------------------
    // Eye colour - one channel, several claimants
    // ------------------------------------------------------------------

    /** The gene that won the iris, and what it claimed. */
    record EyeClaim(Gene gene, EyeColor color) {}

    /**
     * <b>Draw both irises.</b> Three layers, and the order between them is the
     * whole model:
     *
     * <ol>
     *   <li>the winning <b>pigment</b> claim - cream, champagne, tiger eye -
     *       over the whole of both eyes;</li>
     *   <li>the winning <b>depigmenting</b> claim, if any, over as much of each
     *       eye as {@link EyeSpread} says it reached. That is what makes one
     *       blue eye and the blue wedge possible at all: a depigmented iris is
     *       not a colour painted over the pigment, it is pigment that never
     *       arrived, so what shows where the blue stops is the horse's own eye
     *       and it has to still be there underneath;</li>
     *   <li>every {@link EyePatchContribution}'s patches, in code order.</li>
     * </ol>
     */
    private static void paintEyes(Genotype genotype, Epigenome epigenome, CoatOverlay overlay,
                                  double whiteCoverage) {
        EyeClaim winner = eyeClaimOf(genotype, epigenome, whiteCoverage, Integer.MAX_VALUE);
        if (winner != null && winner.color().depigmented()) {
            EyeClaim under = eyeClaimOf(genotype, epigenome, whiteCoverage, EyeColor.RANK_DEPIGMENTED);
            if (under != null) {
                overlay.tintIris(under.color().rgb(), under.color().strength());
            }
            EyeColor blue = winner.color();
            // Off the winning gene's own copy: the spread values live on every
            // gene that can claim an eye, precisely because which one wins is
            // not known until here.
            EyeSpread spread = EyeSpread.roll(
                    GeneEpigenetics.forGene(winner.gene(), genotype, epigenome).expressed());
            overlay.tintIrisSector(CoatRegions.RIGHT_EYE, spread.right(), blue.rgb(), blue.strength());
            overlay.tintIrisSector(CoatRegions.LEFT_EYE, spread.left(), blue.rgb(), blue.strength());
        } else if (winner != null) {
            overlay.tintIris(winner.color().rgb(), winner.color().strength());
        }

        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof EyePatchContribution contribution)) {
                continue;
            }
            contribution.eyePatches(genotype.pair(gene), genotype, epigenome, whiteCoverage)
                    .ifPresent(patches -> {
                        paint(overlay, CoatRegions.RIGHT_EYE, patches.right());
                        paint(overlay, CoatRegions.LEFT_EYE, patches.left());
                    });
        }
    }

    private static void paint(CoatOverlay overlay, int eye, java.util.List<EyePatch> patches) {
        for (EyePatch patch : patches) {
            if (!patch.empty()) {
                overlay.tintIrisSector(eye, patch.quadrants(),
                        patch.color().rgb(), patch.color().strength());
            }
        }
    }

    /**
     * The winning {@link EyeColor} claim for this horse below {@code maxRank},
     * or {@code null} for the template's own dark eye.
     *
     * <p>A horse has one iris colour, so the claims are <b>ranked, not
     * blended</b>: highest {@link EyeColor#rank()} wins and a tie goes to the
     * earlier gene in {@link Genes#codeOrder()}. See
     * {@link EyeColorContribution} for why the white loci are claimants at all
     * and why blue out-ranks a pigment colour.
     *
     * <p>{@code maxRank} is exclusive, and exists for exactly one caller: the
     * eye painter asks a second time, capped below
     * {@link EyeColor#RANK_DEPIGMENTED}, to find out what colour the horse's
     * iris would have been if the white loci had left it alone.
     */
    static EyeClaim eyeClaimOf(Genotype genotype, Epigenome epigenome, double whiteCoverage,
                               int maxRank) {
        EyeClaim best = null;
        for (Gene gene : Genes.codeOrder()) {
            if (!(gene instanceof EyeColorContribution contribution)) {
                continue;
            }
            Optional<EyeColor> claim =
                    contribution.eyeColor(genotype.pair(gene), genotype, epigenome, whiteCoverage);
            if (claim.isEmpty() || claim.get().rank() >= maxRank) {
                continue;
            }
            if (best == null || best.color().losesTo(claim.get())) {
                best = new EyeClaim(gene, claim.get());
            }
        }
        return best;
    }

    /** The winning claim's colour - what a caller that does not care who made it wants. */
    static Optional<EyeColor> eyeColorOf(Genotype genotype, Epigenome epigenome, double whiteCoverage) {
        EyeClaim claim = eyeClaimOf(genotype, epigenome, whiteCoverage, Integer.MAX_VALUE);
        return Optional.ofNullable(claim == null ? null : claim.color());
    }

    /**
     * The fraction of mapped texels the white loci have left with <b>no pigment
     * at all</b> - the signal {@link EyeColorContribution} reads to decide
     * whether a horse is white enough for a blue eye.
     *
     * <p>Measured on the resolved pigment field rather than per locus on
     * purpose: a horse that is broadly white because two mild alleles stacked
     * has exactly the same claim on a blue eye as one that is white from a
     * single bold allele, and no per-locus test can see that.
     */
    private static double whiteCoverage(PigmentView coat, Skin skin) {
        int[] tally = new int[2];
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            tally[1]++;
            if (coat.red(px, py) <= TRANSPARENT_EPS && coat.black(px, py) <= TRANSPARENT_EPS) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0.0 : tally[0] / (double) tally[1];
    }

}
