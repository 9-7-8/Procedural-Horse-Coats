package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.epi.EpiValues;

import java.util.EnumMap;
import java.util.Map;

/**
 * Builds a <b>grey</b> into a {@link PigmentField}.
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
 * <h2>How far along, and why the genotype sets it</h2>
 * Greying is a <b>rate</b>, and a rate needs a timeline to become a picture. The
 * pipeline has no age input past adult / foal, so instead the
 * {@linkplain com.example.horsegenetics.common.genetics.genes.GreyGene#dosage
 * copy-number dosage} picks a <b>window</b> - a slow {@code N/G2} horse is drawn
 * as a steel or mid grey for life, a {@code G3/G3} as a near-white one - and the
 * horse's own epigenetics pick where inside that window it sits. The windows
 * overlap on purpose: individual variation inside a real genotype class is large
 * enough that a slow grey and a fast one can meet in the middle.
 *
 * <h2>Regions do not grey together</h2>
 * <ul>
 *   <li><b>The head runs ahead.</b> White hairs round the eyes are the usual
 *       first sign of greying, and the head stays among the palest areas the
 *       whole way through. It gets a lead, strongest around the forehead and
 *       eyes.</li>
 *   <li><b>The legs lag</b> - full base colour at the hoof, gone by mid-cannon.
 *       This is the "dark legs on a light body" look of a young grey.</li>
 *   <li><b>The mane and tail keep their own stage</b>, rolled per horse and able
 *       to run either ahead of or behind the body, plus a <b>root-to-tip</b>
 *       ramp. The ramp is not decoration: greying is a change to hairs as they
 *       are <i>produced</i>, so the new hair at the root is whiter than the old
 *       hair at the tip, and long hair is where that has room to show.</li>
 *   <li><b>Dapples</b> - rounded patches holding <i>less</i> pigment than the web
 *       running between them, from {@link BodyNoise#cellDistance} sampled in body
 *       space so the rings cross part seams without a join. Spacing, strength and
 *       the flow that warps them off a regular grid are all per-horse, and the
 *       contrast peaks mid-greying because a barely-started or an
 *       almost-finished horse has little pigment left to vary.</li>
 *   <li><b>Chubari spots</b> - rounded <i>white</i> patches on a light grey, from
 *       the same cellular field at a much larger cell size. Uncommon.</li>
 *   <li><b>A bloody shoulder</b> - a patch of retained base colour over the
 *       shoulder that refuses to grey. Rare.</li>
 * </ul>
 *
 * <p>What grey deliberately does <b>not</b> touch: the skin, the eyes, the hooves
 * and the stored base colour. A grey horse is a bay or a chestnut underneath and
 * stays one; nothing here writes to another gene's channel, and there is no
 * flea-bitten stage and no drawn melanoma. See {@code wiki/gene-grey.html}.
 */
public final class GreyCoat {

    /**
     * The progression window each dosage class is drawn in: 0 is a dark steel
     * grey with the base colour still obvious, 1 a hair off white. Indexed by
     * {@code dosage - 1}, so row 0 is {@code N/G2} and row 3 is {@code G3/G3}.
     *
     * <p>They overlap by design. A real {@code N/G2} horse and a real
     * {@code G2/G3} horse of the same age can look alike; what is reliably true
     * is the ordering of their means, and that is what these windows encode.
     */
    private static final float[][] WINDOW = {
            {0.06f, 0.52f},   // N/G2   - steel to mid dapple
            {0.28f, 0.74f},   // N/G3, G2/G2 - the ordinary dapple grey
            {0.50f, 0.90f},   // G2/G3  - light grey
            {0.68f, 1.00f},   // G3/G3  - near white
    };
    /** The window used when the dosage cannot be read (the direct-knob entry point). */
    private static final float[] DEFAULT_WINDOW = {0.06f, 1.00f};

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

    /**
     * How far ahead of the body the head greys, as a share of the pigment it
     * would otherwise keep, and how far from the forehead that lead reaches.
     * White hairs round the eyes are the first visible sign of greying in life
     * and the head stays among the palest areas afterwards, so the lead is a
     * standing offset rather than something that fades out with progression.
     */
    private static final float HEAD_LEAD = 0.34f;
    private static final float EYE_LEAD = 0.30f;
    private static final double EYE_REACH = 3.2;

    /** How far the mane and tail may run ahead of or behind the body's stage. */
    private static final float HAIR_STAGE_SWING = 0.26f;
    /**
     * How much whiter the root of the mane and tail is than the tip. Greying
     * changes hairs as they are produced, so new hair at the root is ahead of
     * old hair at the tip.
     */
    private static final float HAIR_ROOT_LEAD = 0.30f;

    /** Chance a grey carries chubari spots, and how they are drawn when it does. */
    private static final float CHUBARI_CHANCE = 0.18f;
    private static final double CHUBARI_SPACING = 7.0;
    private static final double CHUBARI_CORE = 0.34;
    private static final double CHUBARI_SHARE = 0.34;
    private static final double CHUBARI_WHERE_SCALE = 0.09;
    /**
     * Chubari spots are only legible on a pale horse - on a steel grey a white
     * patch is just a dapple. Below this progression they are not drawn.
     */
    private static final float CHUBARI_MIN_PROGRESS = 0.55f;

    /** Chance a grey carries a bloody shoulder, and the blob that draws it. */
    private static final float BLOODY_CHANCE = 0.07f;
    private static final double BLOODY_CX = 0.74;
    private static final double BLOODY_CY = 0.52;
    private static final double BLOODY_RX = 0.26;
    private static final double BLOODY_RY = 0.42;
    /** How much of its base colour the middle of a bloody shoulder keeps. */
    private static final float BLOODY_KEEP = 0.82f;
    private static final double BLOODY_WANDER = 0.16;

    private GreyCoat() {}

    /**
     * Paint this horse's greying from its stored numbers - see
     * {@link #schema()}. Chubari spots and a bloody shoulder are
     * <b>propensities</b> rather than flags: the stored number is tested against
     * a fixed chance, so the trait is heritable as a tendency and a line can be
     * bred toward it.
     */
    public static void apply(CoatBuildContext ctx, PigmentField f, EpiValues epi) {
        long noiseSeed = epi.seed(DAPPLE_SEED);
        float where = (float) epi.get(PROGRESS);                   // where in this dosage's window
        double spacing = epi.get(DAPPLE_SPACING);
        float dappleStrength = (float) epi.get(DAPPLE_STRENGTH);
        float pointRetention = (float) epi.get(POINT_RETENTION);
        float hairSwing = (float) epi.get(HAIR_SWING);
        boolean chubari = epi.get(CHUBARI) < CHUBARI_CHANCE;
        long chubariSeed = epi.seed(CHUBARI_SEED);
        float bloody = epi.get(BLOODY) < BLOODY_CHANCE ? 1f : 0f;

        float[] window = windowFor(ctx);
        float progress = window[0] + (window[1] - window[0]) * where;

        apply(ctx, f, noiseSeed, progress, spacing, dappleStrength, pointRetention,
                hairSwing, chubari ? chubariSeed : 0L, bloody);
    }

    /**
     * The progression window this horse's <b>copy number</b> puts it in. Read
     * off the genotype rather than rolled, because the rate is the genetics and
     * only the individual variation inside it is epigenetic.
     */
    private static float[] windowFor(CoatBuildContext ctx) {
        int dosage = Genes.GREY.dosage(ctx.genotype().pair(Genes.GREY));
        return dosage >= 1 && dosage <= WINDOW.length ? WINDOW[dosage - 1] : DEFAULT_WINDOW;
    }

    /** Paint with explicit knobs (the sample tool and tests drive this directly). */
    public static void apply(CoatBuildContext ctx, PigmentField f, long noiseSeed, float progress,
                             double spacing, float dappleStrength, float pointRetention) {
        apply(ctx, f, noiseSeed, progress, spacing, dappleStrength, pointRetention, 0f, 0L, 0f);
    }

    /** Paint with every knob, including the two rare variants. */
    public static void apply(CoatBuildContext ctx, PigmentField f, long noiseSeed, float progress,
                             double spacing, float dappleStrength, float pointRetention,
                             float hairSwing, long chubariSeed, float bloody) {
        Skin skin = ctx.skin();

        float p = clamp01(progress);
        float hairP = clamp01(p + hairSwing);
        // Dapple contrast peaks in the middle of greying: a barely-started or an
        // almost-finished horse has little pigment left to vary.
        float contrast = clamp01(dappleStrength * (1f - Math.abs(p - 0.5f) * 1.4f));
        float pointBoost = pointRetention * (1f - p) * 0.9f;
        boolean spots = chubariSeed != 0L && p >= CHUBARI_MIN_PROGRESS;

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
            // How far along this texel's own greying is. The body's stage is the
            // baseline; the long hair keeps its own, and the head runs ahead.
            boolean hair = part == Part.MANE || part == Part.TAIL;
            float stage = hair ? hairRootLead(skin, part, point, hairP) : p;

            float keepWeb = lerp(KEEP_YOUNG, KEEP_OLD, stage);
            float redKeep = lerp(RED_YOUNG, RED_OLD, stage);
            float keepDapple = keepWeb * (1f - DAPPLE_DEPTH * contrast);

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

            // The head runs ahead of the body, hardest around the eyes.
            float lead = headLead(skin, part, point);
            if (lead > 0f) {
                keep *= 1f - lead;
            }

            float boost = pointBoost * pointWeight(part, point, legBounds);
            if (boost > 0f) {
                keep = Math.min(1f, keep * (1f + boost));
            }

            if (spots) {
                keep *= 1f - chubari(chubariSeed, point);
            }

            float red = f.red(px, py);
            float black = f.black(px, py);
            // How dark this texel reads before greying. Black pigment carries most
            // of it; red is dark enough to matter (a chestnut is not a white horse)
            // but nowhere near as dark as eumelanin.
            float darkness = clamp01(0.55f * red + 0.95f * black);

            float greyBlack = darkness * keep;
            float greyRed = red * redKeep * keep;

            // A bloody shoulder is base colour the greying never reached, so it
            // is a lerp back toward what was there rather than a paint over it.
            if (bloody > 0f) {
                float w = bloody * (float) bloodyShoulder(skin, part, point, noiseSeed) * BLOODY_KEEP;
                if (w > 0f) {
                    greyBlack = lerp(greyBlack, black, w);
                    greyRed = lerp(greyRed, red, w);
                }
            }

            f.setBlack(px, py, greyBlack);
            f.setRed(px, py, greyRed);
        });
    }

    /**
     * The head's <b>lead</b> over the body: how much extra of its remaining
     * pigment a head texel loses. Half of it is flat over the whole head and
     * half is a radial falloff from the forehead, which is where the white hairs
     * of a starting grey actually appear.
     */
    private static float headLead(Skin skin, Part part, BodyPoint point) {
        switch (part) {
            case HEAD:
                return HEAD_LEAD + EYE_LEAD * (float) CoatRegions.faceMask(skin, part, point, EYE_REACH);
            case MUZZLE:
            case LEFT_EAR:
            case RIGHT_EAR:
                return HEAD_LEAD;
            default:
                return 0f;
        }
    }

    /**
     * The mane and tail greyed from the root out. New hair at the root carries
     * the horse's current stage plus a lead; old hair at the tip is behind it.
     * The axis is {@link HairPattern#axesBySpan}' second - the same one the
     * <a href="../../../../../../../../wiki/gene-dun.html">midtstol</a> is centred
     * in, which on both parts runs root to tip.
     */
    private static float hairRootLead(Skin skin, Part part, BodyPoint point, float stage) {
        Axis across = HairPattern.axesBySpan(skin, part)[1];
        Bounds b = HorseSkinGeometry.bounds(skin, part);
        double span = b.span(across);
        if (span <= 0) {
            return stage;
        }
        // 0 at one edge of the hair, 1 at the other; the middle is the root.
        double d = Math.abs(point.along(across) - (b.min(across) + b.max(across)) * 0.5) / span;
        float root = (float) (1.0 - smoothstep(0.10, 0.40, d));
        return clamp01(stage + HAIR_ROOT_LEAD * root * (1f - stage));
    }

    /**
     * <b>Chubari spots</b>: rounded white patches scattered over a light grey.
     * The same cellular field the dapples use at a much larger cell size, with a
     * separate low-frequency selector deciding <i>which</i> cells are spots - if
     * the selector is sampled at the cells' own frequency it varies inside each
     * cell and cuts it in half, which is how manchado's islands came out as
     * slivers. Returns how much of the remaining pigment the spot removes.
     */
    private static double chubari(long seed, BodyPoint point) {
        double s = 1.0 / CHUBARI_SPACING;
        double d = BodyNoise.cellDistance(seed, point.x() * s, point.y() * s, point.z() * s);
        double core = 1.0 - smoothstep(CHUBARI_CORE, CHUBARI_CORE + 0.16, d);
        if (core <= 0) {
            return 0;
        }
        double which = BodyNoise.value(seed ^ 0x5901L,
                point.x() * CHUBARI_WHERE_SCALE, point.y() * CHUBARI_WHERE_SCALE,
                point.z() * CHUBARI_WHERE_SCALE);
        return which < CHUBARI_SHARE ? core : 0;
    }

    /**
     * <b>Bloody shoulder</b>: a patch over the shoulder that never greys, in the
     * horse's own base colour. An elliptical blob in barrel-fraction space with a
     * noise-wandered edge, so it is a stain rather than a decal.
     */
    private static double bloodyShoulder(Skin skin, Part part, BodyPoint point, long seed) {
        if (part != Part.BODY || !HorseSkinGeometry.hasPart(skin, part)) {
            return 0;
        }
        Bounds b = HorseSkinGeometry.bounds(skin, part);
        double fx = (point.x() - b.xMin()) / b.span(Axis.X);   // 0 rump .. 1 shoulder
        double fy = (point.y() - b.yMin()) / b.span(Axis.Y);   // 0 belly .. 1 topline
        double dx = (fx - BLOODY_CX) / BLOODY_RX;
        double dy = (fy - BLOODY_CY) / BLOODY_RY;
        double r = Math.sqrt(dx * dx + dy * dy);
        double wander = (BodyNoise.value(seed ^ 0xB10D5L, point.x() * 0.30, point.y() * 0.30,
                point.z() * 0.30) - 0.5) * 2.0 * BLOODY_WANDER;
        return 1.0 - smoothstep(0.60, 1.00, r + wander);
    }

    /**
     * How much of the "late point" boost a texel gets: full on mane / tail /
     * ears / muzzle, and on a leg a ramp that is full at the hoof and gone by
     * mid-cannon.
     *
     * <p>The <b>head is not on this list any more</b>, and that was a real bug:
     * it used to take half a point boost, which held colour on the one region a
     * greying horse loses it from first. The head now takes a
     * {@linkplain #headLead lead} instead - the opposite sign.
     */
    private static float pointWeight(Part part, BodyPoint point, Map<Part, Bounds> legBounds) {
        switch (part) {
            case MANE:
            case TAIL:
            case LEFT_EAR:
            case RIGHT_EAR:
            case MUZZLE:
                return 1f;
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

    // ------------------------------------------------------------------
    // Epigenetics
    // ------------------------------------------------------------------

    public static final String DAPPLE_SEED = "dapple_seed";
    /** Where in this dosage's window the horse has got to - how far along it is. */
    public static final String PROGRESS = "progress";
    public static final String DAPPLE_SPACING = "dapple_spacing";
    public static final String DAPPLE_STRENGTH = "dapple_strength";
    public static final String POINT_RETENTION = "point_retention";
    public static final String HAIR_SWING = "hair_swing";
    /** How chubari-prone this copy is, tested against {@link #CHUBARI_CHANCE}. */
    public static final String CHUBARI = "chubari";
    public static final String CHUBARI_SEED = "chubari_seed";
    /** How bloody-shoulder-prone this copy is, tested against {@link #BLOODY_CHANCE}. */
    public static final String BLOODY = "bloody";

    /** Everything a grey stores. Composed into {@code GreyGene}'s schema. */
    public static EpiSchema schema() {
        return EpiSchema.of(
                EpiValue.seed(DAPPLE_SEED),
                EpiValue.uniform(PROGRESS, 0, 1),
                EpiValue.uniform(DAPPLE_SPACING, DAPPLE_SPACING_MIN,
                        DAPPLE_SPACING_MIN + DAPPLE_SPACING_RANGE),
                EpiValue.uniform(DAPPLE_STRENGTH, 0.5, 1.0),
                EpiValue.uniform(POINT_RETENTION, 0, 1),
                EpiValue.uniform(HAIR_SWING, -HAIR_STAGE_SWING, HAIR_STAGE_SWING),
                EpiValue.uniform(CHUBARI, 0, 1),
                EpiValue.seed(CHUBARI_SEED),
                EpiValue.uniform(BLOODY, 0, 1));
    }
}
