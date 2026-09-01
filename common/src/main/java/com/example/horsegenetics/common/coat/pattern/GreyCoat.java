package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

import java.util.EnumMap;
import java.util.Map;

/**
 * Builds a <b>dapple grey</b> into a {@link CoatBuildContext}'s pigment field.
 *
 * <p><b>Why this isn't just "restrict both pigments":</b> greying replaces
 * pigmented hairs with white ones, and a mix of white and dark hairs reads
 * <i>neutral</i> - so a grey has to end up on the gradient's <b>zero-red
 * column</b>, which is the only place the LUT is actually grey. Scaling red and
 * black together instead walks the sample down the diagonal, and the diagonal
 * runs through the gradient's golds: an "equally restricted" black horse at
 * {@code keep = 0.4} samples {@code (150, 109, 56)} - a tan, not a grey. (Which
 * is why the old flat {@code KEEP = 0.15} had to sit so close to white to look
 * grey at all, and why every grey then looked like the same white horse.)
 *
 * <p>So this pass <b>remaps</b> instead of scaling: it works out how dark the
 * texel currently is, puts that darkness back as <i>black</i> pigment scaled by
 * how far the greying has gone, and keeps only a fading trace of the red. What
 * was underneath still shows through - a greying chestnut is lighter than a
 * greying black, and a young one keeps a rose / steel cast - but every one of
 * them lands on the neutral ramp instead of in the golds.
 *
 * <p>On top of that:
 * <ul>
 *   <li><b>Dapples</b> - rounded patches that hold <i>less</i> pigment than the
 *       web running between them, from {@link BodyNoise#cellDistance} sampled
 *       in body space so the rings cross part seams without a join. Spacing,
 *       strength and the flow that warps them off a regular grid are all
 *       per-horse.</li>
 *   <li><b>Progression</b> - how far along this horse's greying is: dark steel
 *       grey, mid dapple grey, or nearly-white old grey. (Real greying advances
 *       with age; the pipeline has no age input past adult/foal, so a horse's
 *       stage is drawn once and fixed for life.)</li>
 *   <li><b>Late points</b> - mane, tail, ears, muzzle and the lower legs keep
 *       more pigment than the barrel, most strongly on the least-greyed horses:
 *       the "dark points on a light body" look of a young grey.</li>
 * </ul>
 */
public final class GreyCoat {

    /**
     * Share of its darkness the least-greyed adult keeps - a dark steel grey.
     * Deliberately well short of 1: with no age input every grey adult has to
     * read as a grey, so the range runs steel-to-white rather than starting at
     * the horse's original colour.
     */
    private static final float KEEP_YOUNG = 0.46f;
    /** ...and the most-greyed: a hair off white. */
    private static final float KEEP_OLD = 0.10f;
    /** How much of the base coat's red survives, young .. old (the rose-grey cast). */
    private static final float RED_YOUNG = 0.22f;
    private static final float RED_OLD = 0.02f;
    /**
     * How much lighter a dapple centre is than the web around it, at full
     * contrast. Most of a composed texel's variation comes from the white
     * template's own shading, so this has to be generous to read as dapples
     * across a paddock rather than as noise.
     */
    private static final float DAPPLE_DEPTH = 0.42f;

    /** Body units between dapple centres, min .. max (the body is ~22 units long). */
    private static final double DAPPLE_SPACING_MIN = 2.8;
    private static final double DAPPLE_SPACING_RANGE = 2.2;

    private GreyCoat() {}

    /**
     * Roll this horse's greying from {@code epi} and paint. Consumes
     * 1 {@code nextLong()} (the dapple field's seed) + 4 {@code nextFloat()}s
     * (progression, dapple spacing, dapple strength, point retention).
     */
    public static void apply(CoatBuildContext ctx, Rng epi) {
        long noiseSeed = epi.nextLong();
        float progress = epi.nextFloat();                          // steel grey .. near white
        double spacing = DAPPLE_SPACING_MIN + epi.nextFloat() * DAPPLE_SPACING_RANGE;
        float dappleStrength = 0.5f + epi.nextFloat() * 0.5f;
        float pointRetention = epi.nextFloat();

        apply(ctx, noiseSeed, progress, spacing, dappleStrength, pointRetention);
    }

    /** Paint with explicit knobs (the sample tool and tests drive this directly). */
    public static void apply(CoatBuildContext ctx, long noiseSeed, float progress, double spacing,
                             float dappleStrength, float pointRetention) {
        Skin skin = ctx.skin();
        PigmentField f = ctx.pigment();

        float p = clamp01(progress);
        float keepWeb = lerp(KEEP_YOUNG, KEEP_OLD, p);
        float redKeep = lerp(RED_YOUNG, RED_OLD, p);
        // Dapple contrast peaks in the middle of greying: a barely-started or an
        // almost-finished horse has little pigment left to vary.
        float contrast = clamp01(dappleStrength * (1f - Math.abs(p - 0.5f) * 1.4f));
        float keepDapple = keepWeb * (1f - DAPPLE_DEPTH * contrast);
        float pointBoost = pointRetention * (1f - p) * 0.9f;

        double warpScale = 1.0 / (spacing * 3.0);
        double dappleScale = 1.0 / spacing;
        double warp = spacing * 0.45;

        Map<Part, Bounds> legBounds = new EnumMap<>(Part.class);
        for (Part leg : CoatRegions.LEGS) {
            if (HorseSkinGeometry.hasPart(skin, leg)) {
                legBounds.put(leg, HorseSkinGeometry.bounds(skin, leg));
            }
        }

        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            // Warp the sample so the dapple lattice flows instead of gridding up.
            double n = BodyNoise.value(noiseSeed ^ 0x51L,
                    point.x() * warpScale, point.y() * warpScale, point.z() * warpScale);
            double m = BodyNoise.value(noiseSeed ^ 0x52L,
                    point.z() * warpScale, point.x() * warpScale, point.y() * warpScale);
            double wx = point.x() + (n - 0.5) * warp;
            double wy = point.y() + (m - 0.5) * warp;
            double wz = point.z() + (n - m) * warp;

            double d = BodyNoise.cellDistance(noiseSeed, wx * dappleScale, wy * dappleScale, wz * dappleScale);
            // 0 at a dapple centre -> 1 out in the web between dapples.
            float web = (float) smoothstep(0.35, 0.78, d);

            float keep = lerp(keepDapple, keepWeb, web);
            float boost = pointBoost * pointWeight(part, point, legBounds);
            if (boost > 0f) {
                keep = Math.min(1f, keep * (1f + boost));
            }

            float red = f.red(px, py);
            float black = f.black(px, py);
            // How dark this texel reads before greying. Black pigment carries most
            // of it; red is dark enough to matter (a chestnut is not a white horse)
            // but nowhere near as dark as eumelanin.
            float darkness = clamp01(0.55f * red + 0.95f * black);

            f.setBlack(px, py, darkness * keep);
            f.setRed(px, py, red * redKeep * keep);
        });
    }

    /**
     * How much of the "late point" boost a texel gets: full on mane / tail /
     * ears / muzzle, half on the head, and on a leg a ramp that is full at the
     * hoof and gone by mid-cannon.
     */
    private static float pointWeight(Part part, HorseSkinGeometry.BodyPoint point, Map<Part, Bounds> legBounds) {
        switch (part) {
            case MANE, TAIL, LEFT_EAR, RIGHT_EAR, MUZZLE:
                return 1f;
            case HEAD:
                return 0.5f;
            default:
                Bounds b = legBounds.get(part);
                if (b == null) {
                    return 0f;
                }
                double frac = (point.y() - b.yMin()) / b.span(Axis.Y);
                return (float) (1.0 - smoothstep(0.05, 0.55, frac));
        }
    }

    private static double smoothstep(double edge0, double edge1, double t) {
        double u = (t - edge0) / (edge1 - edge0);
        u = u < 0 ? 0 : (u > 1 ? 1 : u);
        return u * u * (3.0 - 2.0 * u);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
