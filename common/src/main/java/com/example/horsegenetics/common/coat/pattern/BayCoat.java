package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

/**
 * Builds a <b>bay</b> coat into a {@link CoatBuildContext}'s pigment field:
 * red-brown body, black points (mane, tail, ear tips, hooves), and black that
 * climbs the legs + face by a <b>random</b> amount, fading out at its top edge.
 *
 * <p><b>Seal brown is the top of this same distribution</b> - a high leg / face
 * roll gives the "black creeps most of the way up" seal look; there is no
 * separate seal gene. The two epigenetic numbers are: one <b>leg</b> height
 * (shared by all four legs) and one <b>face</b> height.
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

    /** Roll the (leg, face) heights from {@code epi} and paint. Consumes 4 {@code nextFloat()}s. */
    public static void apply(CoatBuildContext ctx, Rng epi) {
        double leg = 0.12 + epi.nextFloat() * epi.nextFloat() * 0.85;  // low socks .. near-full (seal)
        double face = 0.04 + epi.nextFloat() * epi.nextFloat() * 0.60;
        apply(ctx, leg, face);
    }

    /** Paint with explicit heights (fractions of leg height / head length). */
    public static void apply(CoatBuildContext ctx, double legHeight, double faceHeight) {
        Skin skin = ctx.skin();
        PigmentField f = ctx.pigment();

        // 1. bay body: keep the red, knock the black down everywhere
        CoatRegions.restrictAll(skin, f, (field, px, py, p) -> field.setBlack(px, py, BODY_BLACK));

        // 2. hard black points
        CoatRegions.blackenPart(skin, f, Part.MANE);
        CoatRegions.blackenPart(skin, f, Part.TAIL);
        CoatRegions.blackenPart(skin, f, Part.LEFT_EAR);
        CoatRegions.blackenPart(skin, f, Part.RIGHT_EAR);

        // 3. black up the legs (all four the same), fading out at the top
        for (Part leg : CoatRegions.LEGS) {
            rampBlackUpLeg(skin, f, leg, Math.max(HOOF_FRACTION / SOLID_PORTION, legHeight));
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
