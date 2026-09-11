package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

/**
 * The sink for the coat pipeline's <b>fourth phase</b>: final ARGB pixels
 * written <i>after</i> the composite onto the template and after the eyes are
 * redrawn, plus the one thing no earlier phase can express - which texels are
 * <b>emissive</b>.
 *
 * <h2>Why there is a phase after the composite at all</h2>
 * Phases 1 and 3 describe <i>pigment</i> and <i>colour</i>, and both are then
 * multiplied onto the white template so the template's own detail - hooves,
 * nostrils, the shading between mane strands - survives on every coat. That is
 * the right default and it is why the pipeline is built that way. Two things
 * sit outside it:
 * <ul>
 *   <li><b>The eyes.</b> {@link CoatRegions#redrawEyes} copies them back from
 *       the template as the last act of the bake, precisely so that a gene
 *       painting a wide white pattern can never blind a horse. A gene that
 *       wants to colour the eyes <i>on purpose</i> therefore has to run after
 *       that, or it is simply overwritten.</li>
 *   <li><b>Emissiveness.</b> "This texel glows" is not a colour, so there is no
 *       channel for it in either accumulator. It is carried here as its own
 *       mask and handed to the renderer, which draws those texels a second time
 *       at full brightness.</li>
 * </ul>
 *
 * <h2>Order</h2>
 * A sink rather than a returned contribution - the same shape as
 * {@link com.example.horsegenetics.common.trait.TraitBuilder}, and for the same
 * reason: these are absolute writes, not additions, so there is nothing to
 * fold. Genes are visited in
 * {@link com.example.horsegenetics.common.genetics.Genes#codeOrder()} and the
 * last writer to a texel wins. Purity is kept where it matters: {@link #base}
 * is the composed coat and never changes, so no gene can read another's
 * overlay and the result does not depend on the visit order except where two
 * genes deliberately claim the same texel.
 */
public final class CoatOverlay {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    private final Skin skin;
    private final int[] base;
    private final int[] paint = new int[N * N];
    private final boolean[] emissive = new boolean[N * N];
    private boolean painted;
    private boolean glowing;
    /**
     * The white lock, or {@code null} - see
     * {@link com.example.horsegenetics.common.genetics.WhiteLockContribution}.
     * A locked texel refuses every write, so the check sits in {@link #paint}
     * and every other painter here reaches it for free.
     */
    private boolean[] locked;

    /**
     * <b>The white template</b>, or {@code null} - the only thing that reliably
     * says which texels of an eye are iris and which are sclera.
     *
     * <p>It used to be read off {@link #base}: an eye is a block of pure black
     * beside a block of near-white, so "dark" meant iris. That stopped being
     * true the moment an iris could be invisible - a cleared iris shows the
     * horse own coat, which on a pale horse is bright, so the sclera painter
     * claimed it and the colour bled across. The template never changes, so
     * classifying from it is right whatever any earlier painter did.
     */
    private int[] eyeTemplate;

    CoatOverlay(Skin skin, int[] base) {
        this.skin = skin;
        this.base = base;
    }

    /** Hand over the template the eyes are classified from. Set once, before any gene paints. */
    void eyeTemplate(int[] template) {
        this.eyeTemplate = template;
    }

    /** Is this eye texel part of the iris rather than the sclera? */
    private boolean isIris(int px, int py) {
        return lumaAt(px, py) <= SCLERA_LUMA;
    }

    /**
     * How bright this eye texel is on the template, {@code [0,1]} - the weight
     * both eye painters scale by, so the antialiased rim between iris and sclera
     * takes a proportional share of each instead of a hard edge.
     */
    private double lumaAt(int px, int py) {
        int[] t = eyeTemplate;
        int argb = t != null && px >= 0 && py >= 0 && px < N && py < N
                ? t[py * N + px]
                : base(px, py);
        if ((argb >>> 24) == 0) {
            return 0.0;
        }
        return luma(argb);
    }

    /** Which mesh is being painted - a foal has no {@code MANE} or {@code MUZZLE}. */
    public Skin skin() {
        return skin;
    }

    /**
     * The finished coat pixel at this texel, template detail and all. Read-only:
     * this is what the horse looks like before any overlay, and it is the same
     * for every gene in this phase.
     */
    public int base(int px, int py) {
        if (px < 0 || py < 0 || px >= N || py >= N) {
            return 0;
        }
        return base[py * N + px];
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * The texels this overlay may not write - the white lock, held by the
     * composer. Set once, before any gene paints.
     */
    void lock(boolean[] mask) {
        this.locked = mask;
    }

    /** Replace this texel outright. An {@code argb} with zero alpha is ignored. */
    public void paint(int px, int py, int argb) {
        if ((argb >>> 24) == 0 || px < 0 || py < 0 || px >= N || py >= N) {
            return;
        }
        if (locked != null && locked[py * N + px]) {
            return;
        }
        paint[py * N + px] = argb;
        painted = true;
    }

    /**
     * Walk this texel {@code strength} of the way from what it already is to
     * {@code rgb}, keeping its alpha. The gentle form: a gold hoof painted this
     * way keeps the template's hoof shading instead of becoming a flat blob.
     * A fully transparent texel is left alone - there is nothing there to tint.
     */
    public void blendToward(int px, int py, int rgb, double strength) {
        int b = base(px, py);
        int a = b >>> 24;
        if (a == 0) {
            return;
        }
        double t = strength < 0 ? 0 : (strength > 1 ? 1 : strength);
        if (t == 0) {
            // Nothing to do - and doing it anyway is not harmless. A write of
            // the base colour is still a WRITE, and these are absolute
            // last-writer-wins, so a zero-strength pass over a texel another
            // painter has already claimed silently undoes it. That is exactly
            // what tintIris does to every sclera texel it walks over: the
            // 1 - luma weighting is 0 there, and a black sclera came out white.
            return;
        }
        int r = mix((b >> 16) & 0xFF, (rgb >> 16) & 0xFF, t);
        int g = mix((b >> 8) & 0xFF, (rgb >> 8) & 0xFF, t);
        int bl = mix(b & 0xFF, rgb & 0xFF, t);
        paint(px, py, (a << 24) | (r << 16) | (g << 8) | bl);
    }

    private static int mix(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }

    /** {@link #blendToward} over every texel of one body part. */
    public void blendPart(Part part, int rgb, double strength) {
        if (!HorseSkinGeometry.hasPart(skin, part)) {
            return;
        }
        HorseSkinGeometry.forEachTexel(skin, part,
                (px, py, p, face, point) -> blendToward(px, py, rgb, strength));
    }

    /** {@link #blendToward} over the bottom {@code heightFraction} of one leg - a hoof. */
    public void blendLowerLeg(Part leg, double heightFraction, int rgb, double strength) {
        forEachLowerLeg(leg, heightFraction, (px, py) -> blendToward(px, py, rgb, strength));
    }

    /**
     * Walk toward {@code rgb} <b>scaled by how bright this texel already is</b>,
     * so the dark parts of a region stay dark. {@link #blendToward} would drag
     * everything to one flat colour; this keeps the texel's own light and shade
     * and only changes its hue.
     */
    public void shadeToward(int px, int py, int rgb, double strength) {
        int b = base(px, py);
        if ((b >>> 24) == 0) {
            return;
        }
        // Rec. 601 luma, which is close enough and does not need a colour space.
        double luma = (0.299 * ((b >> 16) & 0xFF) + 0.587 * ((b >> 8) & 0xFF) + 0.114 * (b & 0xFF)) / 255.0;
        int scaled = (channel(((rgb >> 16) & 0xFF) * luma) << 16)
                | (channel(((rgb >> 8) & 0xFF) * luma) << 8)
                | channel((rgb & 0xFF) * luma);
        blendToward(px, py, scaled, strength);
    }

    private static int channel(double v) {
        int i = (int) Math.round(v);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    /**
     * {@link #shadeToward} over the eye texels - the only way to change a
     * horse's eye colour, since {@link CoatRegions#redrawEyes} has already put
     * the template's eyes back by the time this phase runs.
     *
     * <p>Shaded rather than blended on purpose: an eye is a bright sclera and a
     * dark pupil, and a flat lerp turns both of them into one gold rectangle -
     * which stops reading as an eye at all.
     */
    public void shadeEyes(int rgb, double strength) {
        forEachEyeTexel((px, py) -> shadeToward(px, py, rgb, strength));
    }

    /**
     * <b>Colour the iris</b> - the dark texels of the eye - and leave the sclera
     * white. This is what eye colour means; {@link #shadeEyes} is the opposite
     * operation and is for something else.
     *
     * <p>On the coat sheet an adult eye is a 2&times;2 block of pure black beside
     * a 2&times;2 block of near-white, and a foal's is the black block alone. So
     * a texel is weighted by <b>how dark it already is</b> ({@code 1 - luma}):
     * black takes the colour outright, white is untouched, and the antialiased
     * grey between them takes a proportional share, which keeps the eye's shape.
     *
     * <p>Contrast {@link #shadeToward}, which weights by <i>brightness</i> so a
     * gold hoof keeps the template's shading. Pointed at an eye that colours the
     * sclera and leaves the iris black - right for a glowing eye or the leopard
     * complex's white rim, wrong for an iris.
     */
    public void tintIris(int rgb, double strength) {
        forEachEyeTexel((px, py) -> tintIrisTexel(px, py, rgb, strength));
    }

    private void tintIrisTexel(int px, int py, int rgb, double strength) {
        if ((base(px, py) >>> 24) == 0) {
            return;
        }
        blendToward(px, py, rgb, strength * (1.0 - lumaAt(px, py)));
    }

    private void tintScleraTexel(int px, int py, int rgb, double strength) {
        if ((base(px, py) >>> 24) == 0) {
            return;
        }
        blendToward(px, py, rgb, strength * lumaAt(px, py));
    }

    /**
     * {@link #tintIris} over <b>part of one iris</b> - the primitive both kinds
     * of heterochromia are drawn with.
     *
     * <p>An iris is a 2&times;2 block, so {@code quadrants} is a four-bit mask
     * over it ({@link com.example.horsegenetics.common.genetics.EyePatch}).
     * Which texels those are is worked out from the coat itself rather than
     * hard-coded: the eye rect is a strip of iris beside a strip of sclera and
     * the two eyes' faces are <b>mirrored on the sheet</b>, so the iris sits at
     * a different offset in each. The dark texels are found, their bounding box
     * halved both ways, and each texel assigned to the quadrant it lands in.
     * A sclera texel that falls inside a selected quadrant is harmless - the
     * {@code 1 - luma} weighting leaves it alone, the same as it does in
     * {@link #tintIris}.
     *
     * <p>Like every blend in this phase, it walks from the <b>unmodified</b>
     * coat rather than from an earlier patch, so overlapping patches do not
     * accumulate: use {@code strength} 1 unless blending with the template's own
     * iris is what you meant.
     *
     * @param eye       index into {@link CoatRegions#eyeRects} - 0 is the head's
     *                  west face, 1 the east
     * @param quadrants the mask; 0 paints nothing
     */
    public void tintIrisSector(int eye, int quadrants, int rgb, double strength) {
        int[][] rects = CoatRegions.eyeRects(skin);
        if (quadrants == 0 || eye < 0 || eye >= rects.length) {
            return;
        }
        int[] r = rects[eye];
        int[] box = irisBounds(r);
        double midX = (box[0] + box[2] + 1) / 2.0;
        double midY = (box[1] + box[3] + 1) / 2.0;
        for (int y = r[1]; y < r[1] + r[3]; y++) {
            for (int x = r[0]; x < r[0] + r[2]; x++) {
                int q = (x < midX ? 0 : 1) + (y < midY ? 0 : 2);
                if ((quadrants & (1 << q)) == 0) {
                    continue;
                }
                tintIrisTexel(x, y, rgb, strength);
            }
        }
    }

    /**
     * {@code {xMin, yMin, xMax, yMax}} of the dark texels inside an eye rect -
     * the iris, as opposed to the sclera beside it. Falls back to the whole rect
     * if nothing in it is dark, which cannot happen on either shipped template
     * but keeps a hand-made one from dividing by a degenerate box.
     */
    private int[] irisBounds(int[] r) {
        int xMin = Integer.MAX_VALUE, yMin = Integer.MAX_VALUE, xMax = -1, yMax = -1;
        for (int y = r[1]; y < r[1] + r[3]; y++) {
            for (int x = r[0]; x < r[0] + r[2]; x++) {
                if ((base(x, y) >>> 24) == 0) {
                    continue;
                }
                if (isIris(x, y)) {
                    xMin = Math.min(xMin, x);
                    yMin = Math.min(yMin, y);
                    xMax = Math.max(xMax, x);
                    yMax = Math.max(yMax, y);
                }
            }
        }
        return xMax < 0
                ? new int[]{r[0], r[1], r[0] + r[2] - 1, r[1] + r[3] - 1}
                : new int[]{xMin, yMin, xMax, yMax};
    }

    /**
     * <b>Colour the sclera</b> - the light texels of the eye - and leave the
     * iris alone. The exact complement of {@link #tintIris}, and weighted the
     * same way for the same reason: an adult eye is a block of near-white beside
     * a block of pure black, so each texel takes the colour in proportion to how
     * <b>bright</b> it already is.
     *
     * <p>A foal has no sclera on its template - its eye is the pupil block alone
     * - so this does nothing on one. That is not a bug to work round: a foal's
     * eye genuinely has no white in it to colour, and the same horse grown up
     * shows the locus.
     */
    public void tintSclera(int rgb, double strength) {
        forEachEyeTexel((px, py) -> tintScleraTexel(px, py, rgb, strength));
    }

    /** {@link #tintIris} over one eye only - {@code eye} is an {@code eyeRects} index. */
    public void tintIris(int eye, int rgb, double strength) {
        forEachEyeTexel(eye, (px, py) -> tintIrisTexel(px, py, rgb, strength));
    }

    /** {@link #tintSclera} over one eye only. */
    public void tintSclera(int eye, int rgb, double strength) {
        forEachEyeTexel(eye, (px, py) -> tintScleraTexel(px, py, rgb, strength));
    }

    /**
     * <b>Draw a third eye on the forehead</b>, absolutely.
     *
     * <p>Everything else here tints: it walks the template's own eye toward a
     * colour, which is what keeps a two-texel iris reading as an eye. There is
     * nothing to tint on a forehead - both templates are plain white there - so
     * this writes the pixels outright, in the {@code sclera / iris / iris /
     * sclera} layout {@link CoatRegions#thirdEyeRect} documents.
     *
     * <p>A {@code null} colour means that half is not painted at all, which is
     * how an invisible iris or sclera reaches the forehead: the coat shows
     * through, exactly as it does on the two real eyes.
     */
    public void paintThirdEye(Integer irisRgb, Integer scleraRgb) {
        int[] r = CoatRegions.thirdEyeRect(skin);
        for (int y = r[1]; y < r[1] + r[3]; y++) {
            for (int x = r[0]; x < r[0] + r[2]; x++) {
                Integer rgb = thirdEyeIris(r, x) ? irisRgb : scleraRgb;
                if (rgb != null) {
                    paint(x, y, 0xFF000000 | (rgb & 0xFFFFFF));
                }
            }
        }
    }

    /**
     * {@link #tintIrisSector} for the third eye - the same four quadrants over
     * its two iris columns, so a third eye can carry a sector like any other.
     */
    public void paintThirdEyeSector(int quadrants, int rgb) {
        int[] r = CoatRegions.thirdEyeRect(skin);
        double midX = r[0] + r[2] / 2.0;
        double midY = r[1] + r[3] / 2.0;
        for (int y = r[1]; y < r[1] + r[3]; y++) {
            for (int x = r[0]; x < r[0] + r[2]; x++) {
                if (!thirdEyeIris(r, x)) {
                    continue;
                }
                int q = (x < midX ? 0 : 1) + (y < midY ? 0 : 2);
                if ((quadrants & (1 << q)) != 0) {
                    paint(x, y, 0xFF000000 | (rgb & 0xFFFFFF));
                }
            }
        }
    }

    /** Mark the third eye's iris or sclera full-bright. */
    public void markEmissiveThirdEye(boolean iris, boolean sclera) {
        int[] r = CoatRegions.thirdEyeRect(skin);
        for (int y = r[1]; y < r[1] + r[3]; y++) {
            for (int x = r[0]; x < r[0] + r[2]; x++) {
                if (thirdEyeIris(r, x) ? iris : sclera) {
                    markEmissive(x, y);
                }
            }
        }
    }

    /** The middle two columns of the forehead rect are the iris; the outer two are sclera. */
    private static boolean thirdEyeIris(int[] rect, int x) {
        int col = x - rect[0];
        return col > 0 && col < rect[2] - 1;
    }

    private static double luma(int argb) {
        return (0.299 * ((argb >> 16) & 0xFF) + 0.587 * ((argb >> 8) & 0xFF)
                + 0.114 * (argb & 0xFF)) / 255.0;
    }

    // ------------------------------------------------------------------
    // Emissiveness
    // ------------------------------------------------------------------

    /**
     * Mark this texel as rendering <b>full-bright</b>. Independent of painting:
     * a gene may light up a region it did not colour, or colour one it does not
     * light. The colour drawn is whatever ends up on the finished coat there.
     */
    public void markEmissive(int px, int py) {
        if (px < 0 || py < 0 || px >= N || py >= N) {
            return;
        }
        emissive[py * N + px] = true;
        glowing = true;
    }

    public void markEmissivePart(Part part) {
        if (!HorseSkinGeometry.hasPart(skin, part)) {
            return;
        }
        HorseSkinGeometry.forEachTexel(skin, part, (px, py, p, face, point) -> markEmissive(px, py));
    }

    public void markEmissiveLowerLeg(Part leg, double heightFraction) {
        forEachLowerLeg(leg, heightFraction, this::markEmissive);
    }

    public void markEmissiveEyes() {
        forEachEyeTexel(this::markEmissive);
    }

    /**
     * Mark only the <b>iris</b> - the dark texels - leaving the white round it
     * alone. The complement of {@link #markEmissiveSclera}, and the two are
     * separate because they are two completely different faces: a lit iris is a
     * lamp, a lit sclera with a hole in it is a dhampir.
     */
    public void markEmissiveIris(int eye) {
        forEachEyeTexel(eye, (px, py) -> {
            if ((base(px, py) >>> 24) != 0 && isIris(px, py)) {
                markEmissive(px, py);
            }
        });
    }

    /** {@link #markEmissiveSclera} over one eye only. */
    public void markEmissiveSclera(int eye) {
        forEachEyeTexel(eye, (px, py) -> {
            if ((base(px, py) >>> 24) != 0 && !isIris(px, py)) {
                markEmissive(px, py);
            }
        });
    }

    /**
     * Mark only the <b>sclera</b> - the light texels of the eye - leaving the
     * iris alone. The exact complement of {@link #tintIris}, and weighted the
     * same way and for the same reason: an adult eye is a block of near-white
     * beside a block of pure black, so "the sclera" is "the texels that are
     * already bright", worked out from the coat rather than hard-coded.
     *
     * <p>A glowing iris and a glowing sclera are very different faces. Light's
     * gold eye wants the whole thing ({@link #markEmissiveEyes}); a dhampir
     * wants the white to burn and the pupil to stay a hole in it.
     */
    public void markEmissiveSclera() {
        forEachEyeTexel((px, py) -> {
            if ((base(px, py) >>> 24) != 0 && !isIris(px, py)) {
                markEmissive(px, py);
            }
        });
    }

    /**
     * Above this luma an eye texel is sclera rather than iris. Half-way: the
     * template's sclera is near-white and its iris is pure black, so anything
     * in between is the antialiased boundary and belongs to whichever side it
     * is closer to.
     */
    private static final double SCLERA_LUMA = 0.5;

    // ------------------------------------------------------------------

    private interface TexelVisitor {
        void at(int px, int py);
    }

    private void forEachLowerLeg(Part leg, double heightFraction, TexelVisitor visitor) {
        if (!HorseSkinGeometry.hasPart(skin, leg)) {
            return;
        }
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double f = heightFraction < 0 ? 0 : (heightFraction > 1 ? 1 : heightFraction);
        double cutoff = b.yMin() + b.span(Axis.Y) * f;
        HorseSkinGeometry.forEachTexel(skin, leg, (px, py, part, face, point) -> {
            if (point.y() <= cutoff) {
                visitor.at(px, py);
            }
        });
    }

    private void forEachEyeTexel(TexelVisitor visitor) {
        for (int[] r : CoatRegions.eyeRects(skin)) {
            for (int y = r[1]; y < r[1] + r[3]; y++) {
                for (int x = r[0]; x < r[0] + r[2]; x++) {
                    visitor.at(x, y);
                }
            }
        }
    }

    private void forEachEyeTexel(int eye, TexelVisitor visitor) {
        int[][] rects = CoatRegions.eyeRects(skin);
        if (eye < 0 || eye >= rects.length) {
            return;
        }
        int[] r = rects[eye];
        for (int y = r[1]; y < r[1] + r[3]; y++) {
            for (int x = r[0]; x < r[0] + r[2]; x++) {
                visitor.at(x, y);
            }
        }
    }

    // ------------------------------------------------------------------
    // Results - read by the composer only
    // ------------------------------------------------------------------

    /** Fold the painted texels into the finished coat, in place. */
    void applyTo(int[] out) {
        if (!painted) {
            return;
        }
        for (int i = 0; i < out.length; i++) {
            int c = paint[i];
            if ((c >>> 24) != 0) {
                out[i] = c;
            }
        }
    }

    /**
     * Which texels render full-bright, or {@code null} if none do - which is the
     * ordinary case, and lets the renderer skip the whole emissive layer without
     * scanning an array of 16 384 falses.
     */
    boolean[] emissiveMask() {
        return glowing ? emissive : null;
    }
}
