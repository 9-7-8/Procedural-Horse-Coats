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

    CoatOverlay(Skin skin, int[] base) {
        this.skin = skin;
        this.base = base;
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

    /** Replace this texel outright. An {@code argb} with zero alpha is ignored. */
    public void paint(int px, int py, int argb) {
        if ((argb >>> 24) == 0 || px < 0 || py < 0 || px >= N || py >= N) {
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
