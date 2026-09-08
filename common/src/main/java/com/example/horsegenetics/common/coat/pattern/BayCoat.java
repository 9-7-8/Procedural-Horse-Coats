package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.BayShade;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

/**
 * Builds a <b>bay</b> coat into a {@link PigmentField} - the whole bay
 * continuum, from a bright blood bay through an ordinary bay and a liver bay to
 * a seal brown, off <b>one number</b>: {@link BayShade#spread}, {@code 0} for
 * black held to the points and {@code 1} for black over nearly all of it.
 *
 * <h2>What the spread moves</h2>
 * <ol>
 *   <li><b>The body</b> - {@link #bodyBlack}, from a warm copper red at
 *       {@code 0} to nearly black at {@code 1}. This is the term that makes a
 *       liver bay a liver bay, and it is the one the old constant-body version
 *       had no way to say at all.</li>
 *   <li><b>How far black climbs the legs and the face</b> - the same two
 *       formulas as before, now driven by the shade score rather than by a bare
 *       uniform roll.</li>
 *   <li><b>The soft points</b> - {@link #softPoints}, which only switch on near
 *       the dark end. A seal brown is not "a bay whose black climbed high"; it
 *       is a nearly-black horse with <i>lighter</i> tan areas left at the
 *       muzzle, over the eye, and at the elbow and flank. Without them the dark
 *       end of the range is just a black horse.</li>
 * </ol>
 *
 * <p>The mane, tail, ears and hooves are black at every point on the range,
 * because every bay has black points; that is what makes it a bay.
 *
 * <p>The spread is <b>mostly genetic</b> - see {@link BayShade} - so two seal
 * browns mostly throw dark foals, and a blood bay hiding a dark haplotype is a
 * surprise two generations out. The per-horse part of it rides on the
 * expressing {@code A} copy's epigenetic seed, so a foal that inherits its
 * dam's {@code A} inherits her exact roll.
 */
public final class BayCoat {

    /** Body black at {@link BayShade#spread} 0 - the reddest a bay body gets. */
    public static final float BODY_BLACK_LIGHT = 0.10f;
    /** Body black at spread 1 - all but black, and only the soft points keep it off. */
    public static final float BODY_BLACK_DARK = 0.82f;
    /**
     * Curve on the body-black ramp. {@code 1.7} is not a taste knob: it is what
     * puts spread {@code 0.5} back on <b>0.32</b>, the constant body black that
     * was verified in-game as an ordinary bay, so the middle of the new range is
     * the horse that was already known to be right and only the ends are new.
     */
    private static final double BODY_CURVE = 1.7;

    /** Hooves are always solidly black for at least this fraction of leg height. */
    public static final double HOOF_FRACTION = 0.12;
    /**
     * Fraction of the coloured band that is *solid* black before it starts
     * fading. The rest of the band is a smoothstep fade to nothing - low so the
     * black-to-body transition is a long, edgeless gradient, not a hard line.
     */
    private static final double SOLID_PORTION = 0.3;

    /** Lowest / highest fraction of the leg the black can climb. */
    private static final double LEG_MIN = 0.15;
    private static final double LEG_RANGE = 0.80;
    /** How far one leg may differ from the horse's own average, either way. */
    private static final double LEG_JITTER = 0.14;

    /** Face black: a floor, and a range the <i>square</i> of the spread walks. */
    private static final double FACE_MIN = 0.04;
    private static final double FACE_RANGE = 0.62;

    /** Spread at which soft points begin to appear, and where they reach full strength. */
    private static final double SOFT_START = 0.62;
    private static final double SOFT_FULL = 0.95;
    /**
     * Black level a fully-expressed soft point falls to. It is a <b>warm brown
     * against a near-black body</b>, not the reddest the chart has: at 0.13 the
     * muzzle came out a pale beige blob with a box-shaped edge, which reads as
     * a marking rather than as mealiness.
     */
    private static final float SOFT_BLACK = 0.28f;
    /** How far from the forehead the pale ring over the eye reaches, in body units. */
    private static final double SOFT_EYE_REACH = 2.6;
    /** How much of the muzzle's mealiness is left where it meets the head. */
    private static final double MUZZLE_BACK = 0.45;
    /** The inner leg's share of a soft point, and how far down the leg it reaches. */
    private static final double INNER_LEG = 0.70;
    private static final double INNER_LEG_REACH = 0.55;

    private BayCoat() {}

    // ------------------------------------------------------------------
    // The spread -> the things it moves
    // ------------------------------------------------------------------

    /** Black left on the body at this spread. */
    public static float bodyBlack(double spread) {
        double t = clamp01(spread);
        return (float) (BODY_BLACK_LIGHT
                + (BODY_BLACK_DARK - BODY_BLACK_LIGHT) * Math.pow(t, BODY_CURVE));
    }

    /** Fraction of leg height the black climbs at this spread, before per-leg jitter. */
    public static double legHeight(double spread) {
        return LEG_MIN + clamp01(spread) * LEG_RANGE;
    }

    /**
     * Fraction of head length the black climbs at this spread. <b>Squared</b>,
     * so the face only follows once the legs are already high - which is what
     * gives a seal brown rather than "socks plus a black face".
     */
    public static double faceHeight(double spread) {
        double t = clamp01(spread);
        return FACE_MIN + t * t * FACE_RANGE;
    }

    /**
     * How strongly the soft tan points show at this spread: {@code 0} for
     * anything short of a very dark bay, ramping to {@code 1} at the seal end.
     */
    public static double softPoints(double spread) {
        return smooth01((clamp01(spread) - SOFT_START) / (SOFT_FULL - SOFT_START));
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    /**
     * Paint this horse's shade from its stored numbers: the shade offset inside
     * {@link BayShade#spread}, then one height multiplier per leg, so a bay's
     * four points are never quite level with each other.
     */
    public static void apply(CoatBuildContext ctx, PigmentField f, EpiValues epi) {
        double spread = BayShade.spread(ctx.genotype(), epi);
        double leg = legHeight(spread);
        double[] legs = new double[CoatRegions.LEGS.size()];
        for (int i = 0; i < legs.length; i++) {
            legs[i] = leg * epi.get(LEG_JITTER_VALUE, i);
        }
        apply(ctx, f, spread, legs);
    }

    /** Per-leg multiplier on the point height - see {@link #LEG_JITTER}. */
    public static final String LEG_JITTER_VALUE = "leg_jitter";

    /** Everything a bay stores: its shade offset and its four point heights. */
    public static EpiSchema schema() {
        return EpiSchema.of(
                BayShade.shadeValue(),
                EpiValue.perLeg(LEG_JITTER_VALUE, 1.0 - LEG_JITTER, 1.0 + LEG_JITTER));
    }

    /** Paint at an explicit spread, all four legs level - the tools' and the tests' entry. */
    public static void apply(CoatBuildContext ctx, PigmentField f, double spread) {
        double[] legs = new double[CoatRegions.LEGS.size()];
        java.util.Arrays.fill(legs, legHeight(spread));
        apply(ctx, f, spread, legs);
    }

    /**
     * Paint at an explicit spread with explicit per-leg heights (fractions of
     * leg height, in {@link CoatRegions#LEGS} order).
     */
    public static void apply(CoatBuildContext ctx, PigmentField f, double spread, double[] legHeights) {
        Skin skin = ctx.skin();
        float body = bodyBlack(spread);

        // 1. the body: keep the red, knock the black down to this shade's level
        CoatRegions.restrictAll(skin, f, (field, px, py, p) -> field.setBlack(px, py, body));

        // 2. hard black points - every bay has these, at every shade
        CoatRegions.blackenPart(skin, f, Part.MANE);
        CoatRegions.blackenPart(skin, f, Part.TAIL);
        CoatRegions.blackenPart(skin, f, Part.LEFT_EAR);
        CoatRegions.blackenPart(skin, f, Part.RIGHT_EAR);

        // 3. black up each leg (its own height), fading out at the top
        for (int i = 0; i < CoatRegions.LEGS.size(); i++) {
            rampBlackUpLeg(skin, f, CoatRegions.LEGS.get(i),
                    legHeights[Math.min(i, legHeights.length - 1)]);
        }

        // 4. black up the face, fading out
        rampBlackUpFace(skin, f, faceHeight(spread));

        // 5. and, at the dark end only, take it back off the soft points
        paintSoftPoints(skin, f, softPoints(spread));
    }

    /**
     * Black up one leg to {@code band}, solid at the bottom and smoothstepping
     * out at the top.
     *
     * <p>The hoof floor is applied to the <b>solid</b> portion, not to the band.
     * It used to raise the whole band to {@code HOOF_FRACTION / SOLID_PORTION}
     * = 0.4, which meant every leg shorter than that came out at exactly 0.4 -
     * so the per-leg jitter vanished on any bay whose black did not already
     * climb past the knee, and a blood bay's four socks were identical to the
     * texel. Flooring only the solid portion keeps the guarantee the constant
     * exists for (the hoof is always solidly black) and leaves the jitter
     * intact underneath it.
     */
    private static void rampBlackUpLeg(Skin skin, PigmentField f, Part leg, double height) {
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double yMin = b.yMin();
        double span = b.span(Axis.Y);
        double solid = Math.max(HOOF_FRACTION, height * SOLID_PORTION);
        double band = Math.max(height, solid);
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
     * The <b>seal brown's tan</b>: pull pigment back toward a warm tan at the
     * muzzle, over the eye, and at the elbow and the flank - the places a real
     * seal brown keeps light while the rest of it goes near-black.
     *
     * <p>It runs <i>last</i>, so it takes the muzzle back off the face ramp. A
     * seal brown's muzzle is mealy tan; an ordinary bay's is black, and at
     * {@code strength} 0 - which is every bay short of the darkest - this whole
     * pass does nothing.
     *
     * <p>The <b>outside</b> of the legs is left alone deliberately: a seal
     * brown's legs read as black, and it is the inner surface that keeps the
     * tan. That is the fifth classic soft point and it went undrawn for a while
     * because "the inside of a leg" is a <i>face</i> of a part rather than a
     * part, which nothing here could address -
     * {@link CoatRegions#medial} is that, and this and
     * {@link com.example.horsegenetics.common.genetics.genes.PangareGene} are
     * its two users.
     */
    private static void paintSoftPoints(Skin skin, PigmentField f, double strength) {
        if (strength <= 0) {
            return;
        }
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            float k = (float) (strength * softWeight(skin, part, face, point));
            if (k > 0f) {
                f.setBlack(px, py, lerp(f.black(px, py), SOFT_BLACK, k));
                f.setRed(px, py, lerp(f.red(px, py), 1.0f, k));
            }
        });
    }

    /** How much of a soft point sits at this texel, {@code 0}-{@code 1}. */
    private static double softWeight(Skin skin, Part part, HorseSkinGeometry.Face face,
                                     BodyPoint point) {
        if (CoatRegions.LEGS.contains(part)) {
            // The inner surface of the upper leg, and nothing on the outside.
            // It fades out downward: the tan is on the thigh and the forearm,
            // not on the cannon, which is solidly black on any seal brown.
            Bounds b = HorseSkinGeometry.bounds(skin, part);
            double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
            double up = smooth01((fy - (1.0 - INNER_LEG_REACH)) / INNER_LEG_REACH);
            return INNER_LEG * CoatRegions.medial(skin, part, face) * up;
        }
        switch (part) {
            case MUZZLE: {
                // Strongest at the nose and weakening toward the head, so the
                // mealiness runs out rather than stopping at the box edge.
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double span = b.span(Axis.X);
                double fromNose = span <= 0 ? 0 : (b.xMax() - point.x()) / span;
                return MUZZLE_BACK + (1.0 - MUZZLE_BACK) * (1.0 - clamp01(fromNose));
            }
            case HEAD:
                // The same forehead-centred falloff dun's face mask uses - the
                // pale ring a seal brown carries round the eye sits there.
                return CoatRegions.faceMask(skin, part, point, SOFT_EYE_REACH);
            case BODY: {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double fx = (point.x() - b.xMin()) / b.span(Axis.X);   // 0 rump .. 1 shoulder
                double fy = (point.y() - b.yMin()) / b.span(Axis.Y);   // 0 belly .. 1 topline
                // Two broad, low, overlapping fields rather than two small
                // round ones: a seal brown's light areas are a whole soft
                // underside strongest at the elbow and the stifle, and drawn
                // small they read as a pair of orange freckles on the barrel.
                double elbow = blob(fx, fy, 0.80, 0.08, 0.28, 0.48);
                double stifle = blob(fx, fy, 0.20, 0.10, 0.30, 0.46);
                return Math.max(elbow, stifle);
            }
            default:
                return 0.0;
        }
    }

    /** A soft elliptical blob in barrel-fraction space: 1 at the centre, 0 past the radii. */
    private static double blob(double fx, double fy, double cx, double cy, double rx, double ry) {
        double dx = (fx - cx) / rx;
        double dy = (fy - cy) / ry;
        return 1.0 - smooth01(Math.sqrt(dx * dx + dy * dy));
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

    private static double smooth01(double t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static float lerp(float a, float b, float k) {
        return a + (b - a) * k;
    }
}
