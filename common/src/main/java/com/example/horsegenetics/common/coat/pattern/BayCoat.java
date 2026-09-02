package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

/**
 * Builds a <b>bay</b> coat into a {@link PigmentField}:
 * red-brown body, black points (mane, tail, ear tips, hooves), and black that
 * climbs the legs + face by a <b>random</b> amount, fading out at its top edge.
 *
 * <p><b>Seal brown is the top of this same distribution</b> - a high leg / face
 * roll gives the "black creeps most of the way up" seal look; there is no
 * separate seal gene.
 *
 * <p>The heights come from the horse's <b>agouti {@code A} copy</b>: one
 * uniform "point extent" number spread across the full range, so bays really do
 * run from low socks to seal rather than clustering at the bottom, plus a small
 * independent jitter per leg (a real horse's four socks are not the same
 * height) and a face height that only climbs on the horses whose legs already
 * did. Because the number rides on the allele, a foal that inherits its dam's
 * {@code A} inherits her point extent exactly.
 */
public final class BayCoat {

    /** How much black the body keeps - lower = redder body. */
    public static final float BODY_BLACK = 0.32f;
    /** Hooves are always solidly black for at least this fraction of leg height. */
    public static final double HOOF_FRACTION = 0.12;
    /**
     * Fraction of the coloured band that is *solid* black before it starts
     * fading. The rest of the band is a smoothstep fade to nothing - low so the
     * black-to-body transition is a long, edgeless gradient, not a hard line.
     */
    private static final double SOLID_PORTION = 0.3;

    private BayCoat() {}

    /** Lowest / highest fraction of the leg the black can climb. */
    private static final double LEG_MIN = 0.15;
    private static final double LEG_RANGE = 0.80;
    /** How far one leg may differ from the horse's own average, either way. */
    private static final double LEG_JITTER = 0.14;

    /**
     * Roll this horse's point heights from {@code epi} and paint. Consumes 5
     * {@code nextFloat()}s: one "point extent" for the horse, then one jitter
     * per leg.
     */
    public static void apply(CoatBuildContext ctx, PigmentField f, Rng epi) {
        double extent = epi.nextFloat();                       // 0 = low socks .. 1 = seal
        double leg = LEG_MIN + extent * LEG_RANGE;
        double face = 0.04 + extent * extent * 0.62;           // the face only follows high legs
        double[] legs = new double[CoatRegions.LEGS.size()];
        for (int i = 0; i < legs.length; i++) {
            legs[i] = leg * (1.0 - LEG_JITTER + epi.nextFloat() * LEG_JITTER * 2.0);
        }
        apply(ctx, f, legs, face);
    }

    /** Paint with one explicit height for all four legs. */
    public static void apply(CoatBuildContext ctx, PigmentField f, double legHeight, double faceHeight) {
        double[] legs = new double[CoatRegions.LEGS.size()];
        java.util.Arrays.fill(legs, legHeight);
        apply(ctx, f, legs, faceHeight);
    }

    /**
     * Paint with explicit heights (fractions of leg height / head length),
     * {@code legHeights} in {@link CoatRegions#LEGS} order.
     */
    public static void apply(CoatBuildContext ctx, PigmentField f, double[] legHeights, double faceHeight) {
        Skin skin = ctx.skin();

        // 1. bay body: keep the red, knock the black down everywhere
        CoatRegions.restrictAll(skin, f, (field, px, py, p) -> field.setBlack(px, py, BODY_BLACK));

        // 2. hard black points
        CoatRegions.blackenPart(skin, f, Part.MANE);
        CoatRegions.blackenPart(skin, f, Part.TAIL);
        CoatRegions.blackenPart(skin, f, Part.LEFT_EAR);
        CoatRegions.blackenPart(skin, f, Part.RIGHT_EAR);

        // 3. black up each leg (its own height), fading out at the top
        for (int i = 0; i < CoatRegions.LEGS.size(); i++) {
            double h = legHeights[Math.min(i, legHeights.length - 1)];
            rampBlackUpLeg(skin, f, CoatRegions.LEGS.get(i), Math.max(HOOF_FRACTION / SOLID_PORTION, h));
        }

        // 4. black up the face, fading out
        rampBlackUpFace(skin, f, faceHeight);
    }

    private static void rampBlackUpLeg(Skin skin, PigmentField f, Part leg, double band) {
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double yMin = b.yMin();
        double span = b.span(Axis.Y);
        double solid = band * SOLID_PORTION;
        HorseSkinGeometry.forEachTexel(skin, leg, (px, py, part, face, point) -> {
            double frac = (point.y() - yMin) / span;
            float k = fade(frac, solid, band);
            if (k > 0f) {
                f.setBlack(px, py, lerp(f.black(px, py), 1.0f, k));
                f.setRed(px, py, lerp(f.red(px, py), 0.0f, k));
            }
        });
    }

    private static void rampBlackUpFace(Skin skin, PigmentField f, double band) {
        CoatRegions.blackenPart(skin, f, Part.MUZZLE);
        if (!HorseSkinGeometry.hasPart(skin, Part.HEAD)) {
            return;
        }
        Bounds h = HorseSkinGeometry.bounds(skin, Part.HEAD);
        double xMax = h.xMax();
        double span = h.span(Axis.X);
        double solid = band * SOLID_PORTION;
        HorseSkinGeometry.forEachTexel(skin, Part.HEAD, (px, py, part, face, point) -> {
            double fromNose = (xMax - point.x()) / span;
            float k = fade(fromNose, solid, band);
            if (k > 0f) {
                f.setBlack(px, py, lerp(f.black(px, py), 1.0f, k));
                f.setRed(px, py, lerp(f.red(px, py), 0.0f, k));
            }
        });
    }

    /**
     * 1 up to {@code solid}, then a <b>smoothstep</b> fade to 0 by {@code band},
     * then 0. Smoothstep (flat slope at both ends) means neither the start nor
     * the end of the fade reads as an edge - the black just dissolves into the
     * body colour up the leg / face.
     */
    private static float fade(double t, double solid, double band) {
        if (t <= solid) {
            return 1f;
        }
        if (t >= band) {
            return 0f;
        }
        double u = (t - solid) / (band - solid); // 0 .. 1 across the fade zone
        double s = u * u * (3.0 - 2.0 * u);       // smoothstep
        return (float) (1.0 - s);
    }

    private static float lerp(float a, float b, float k) {
        return a + (b - a) * k;
    }
}
