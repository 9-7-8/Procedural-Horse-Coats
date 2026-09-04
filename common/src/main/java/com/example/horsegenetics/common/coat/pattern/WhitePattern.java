package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

/**
 * <b>The two shapes congenital white spotting comes in</b>, each as one painter
 * driven by a single {@code strength} in {@code [0, 1]}.
 *
 * <h2>Why one painter per family instead of one per gene</h2>
 * Real white patterning is produced by a handful of loci that each have
 * <i>many</i> alleles - {@code KIT} alone has thirty-plus named ones - and the
 * difference between two alleles at the same locus is overwhelmingly a
 * difference of <b>degree</b>, not of kind. UC Davis describes {@code W5},
 * {@code W10} and {@code W13} in near-identical words and separates them by how
 * much white a copy tends to produce; the same is true of {@code SW1} against
 * {@code SW3}. Writing a bespoke painter per allele would therefore be writing
 * the same painter eight times and pretending the differences were principled.
 *
 * <p>So each family gets one painter and each <i>outcome</i> picks a strength.
 * That is also what makes the combination table do real work: an allele has no
 * phenotype of its own, a <b>combination</b> lands on an outcome, and an outcome
 * is a number on this ramp.
 *
 * <h2>The two shapes</h2>
 * <ul>
 *   <li>{@link #sabino} - the <b>{@code KIT}</b> shape. White grows inward from
 *       the extremities (legs, belly, face) with <b>ragged, roaned margins</b>,
 *       and at high strength the body follows, colour retreating last to the
 *       ears, crest and tail. This is what "sabino-like" means throughout the
 *       {@code W} series.</li>
 *   <li>{@link #splash} - the <b>{@code MITF} / {@code PAX3}</b> shape. The
 *       horse is <b>dipped in white from below</b>: a level, <b>sharply
 *       bounded</b> line rises up the body, taking the legs and belly whole,
 *       with a broad blaze to bald face above it. The crisp margin is the
 *       diagnostic difference from sabino, not a stylistic choice.</li>
 * </ul>
 *
 * <p>Both are <b>natural</b> phase-1 painters: they only ever set both pigments
 * to zero, which is what makes the bald white template show through. Both are
 * pure - handed a read-only view, they return a new field - and both draw every
 * number they need from the expressing allele copy's epigenetic seed, so the
 * determinism contract holds. The <b>draw order is part of the contract</b> and
 * is documented on each method: change it and every existing horse carrying
 * that gene repaints.
 *
 * <h2>White finds white</h2>
 * Both painters <b>read the coat they are painting over</b> and paint harder
 * the more of it is already de-pigmented. This is not a flourish - it is the
 * single most-repeated observation in the source, and without it the model gets
 * the headline cases wrong:
 * <ul>
 *   <li>{@code MITF} splash and {@code PAX3} splash are different genes, so a
 *       horse can carry one copy of each. They are supposed to be markedly
 *       whiter than either alone. Painted blindly they are not: two waterlines
 *       drawn at roughly the same height are one waterline.</li>
 *   <li>{@code W20} is described as a <b>booster</b> - subtle by itself, and
 *       adding white beside another spotting variant. Reading the coat is what
 *       lets it be that.</li>
 *   <li>A frame or tobiano horse that is also splash is louder than either.</li>
 * </ul>
 * The rule is one line in each painter and no interaction table anywhere: a
 * gene's effective strength rises with how white the horse already is. Both
 * genes stay pure functions of what they are handed, and phase 1 is already an
 * ordered fold, so nothing about the pipeline's contract moves.
 *
 * <p><b>Not modelled:</b> blue eyes. Splash's most recognisable feature is one
 * or two blue eyes, but eyes are copied verbatim from the template after the
 * whole coat is composed ({@link CoatRegions#redrawEyes}) so that a wide pattern
 * can never blind a horse. Eye colour wants its own gene reading this one.
 */
public final class WhitePattern {

    private WhitePattern() {}

    // ------------------------------------------------------------------
    // The KIT shape: ragged, from the extremities inward
    // ------------------------------------------------------------------

    /** How far the jagged leg margin wanders, as a fraction of leg height. */
    private static final double LEG_JAG = 0.20;
    /** Body-space frequency of the leg-margin wobble. */
    private static final double LEG_JAG_FREQ = 1.6;
    /** Strength at which body white starts, and the strength at which it is total. */
    private static final double BODY_ONSET = 0.42;
    private static final double BODY_FULL = 0.90;
    /**
     * Body-space frequency of the body-white patch field, and how far its
     * threshold is wobbled at a fine scale to leave a ragged margin.
     *
     * <p>The patch field has to be <b>low</b>-frequency. An earlier version
     * thresholded a two-octave fractal at body-unit scale and got salt-and-pepper
     * confetti - which is a perfectly good <i>roan</i> and completely wrong for
     * sabino, whose white comes in patches with torn edges. Big warped blobs
     * plus a fine wobble on the threshold is the same recipe frame uses, and
     * for the same reason.</p>
     */
    private static final double BODY_SCALE = 0.17;
    private static final double BODY_JAG = 0.16;
    private static final double BODY_JAG_FREQ = 3.3;
    /** Blaze half-width in body units, at strength 0 and at strength 1. */
    private static final double FACE_HALF_MIN = 0.15;
    private static final double FACE_HALF_MAX = 3.75;

    /**
     * The {@code KIT} / sabino shape at {@code strength}, from a barely-marked
     * horse ({@code ~0.1}: a star and a low sock or two) through classic
     * sabino-1 ({@code ~0.4}) to sabino-white ({@code ~0.9}: ninety per cent
     * white with colour left only on the ears and crest).
     *
     * <p>The strength is raised by whatever white is already on the horse - see
     * "white finds white" above - so a {@code KIT} pattern over a tobiano or a
     * frame is louder than the same pattern on a solid horse.
     *
     * <p><b>Draw order</b>, off {@code ctx.epigeneticsFor(geneKey)}:
     * {@code nextLong()} (the noise seed), then four {@code nextFloat()}s (one
     * per leg, in {@link CoatRegions#LEGS} order), then {@code nextFloat()} for
     * the belly and {@code nextFloat()} for the face.
     */
    public static PigmentField sabino(CoatBuildContext ctx, PigmentView coat, String geneKey, double strength) {
        double s = clamp01(strength + SABINO_STACKING * alreadyWhite(coat, ctx.skin()));
        Rng epi = ctx.epigeneticsFor(geneKey);
        long seed = epi.nextLong();

        // Each leg climbs to roughly the family strength, but they are never
        // level with one another - and the spread narrows as the horse whitens,
        // because at ninety per cent white there is nothing left to vary.
        double spread = 0.05 + 0.30 * (1.0 - s);
        double[] legH = new double[CoatRegions.LEGS.size()];
        for (int i = 0; i < legH.length; i++) {
            legH[i] = clamp01(s * 1.05 + (epi.nextFloat() - 0.5) * 2.0 * spread);
        }
        double bellyRoll = epi.nextFloat();
        double faceRoll = epi.nextFloat();

        double faceHalf = FACE_HALF_MIN + (FACE_HALF_MAX - FACE_HALF_MIN) * s * (0.75 + 0.25 * faceRoll);
        double faceLength = clamp01(0.12 + 0.9 * s);

        Skin skin = ctx.skin();
        Bounds body = HorseSkinGeometry.bodyBounds(skin);
        double span = body.span(Axis.Y);
        // The belly patch reaches this far up the horse; nothing at all below ~0.1.
        double bellyLine = body.yMin() + span * (s * (0.62 + 0.16 * bellyRoll));
        // Body white only begins once the extremities are already white, then
        // takes over quickly - the ladder from "a few belly spots" to
        // "sabino-white" is short in real horses too.
        double bodyCut = 1.0 - 0.95 * clamp01((s - BODY_ONSET) / (BODY_FULL - BODY_ONSET));

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            int legIndex = CoatRegions.LEGS.indexOf(part);
            if (legIndex >= 0) {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double frac = (point.y() - b.yMin()) / b.span(Axis.Y);
                double jag = (PatchNoise.fbm2(seed ^ (0x11L * (legIndex + 1)),
                        point.x() * LEG_JAG_FREQ, point.y() * LEG_JAG_FREQ, point.z() * LEG_JAG_FREQ * 1.5)
                        - 0.5) * 2.0 * LEG_JAG;
                if (frac < legH[legIndex] + jag) {
                    whiten(f, px, py);
                }
                return;
            }
            if (part == Part.HEAD || part == Part.MUZZLE) {
                if (Math.abs(point.z()) <= faceHalf && withinFace(skin, part, point, faceLength)) {
                    whiten(f, px, py);
                }
                return;
            }
            if (part == Part.LEFT_EAR || part == Part.RIGHT_EAR) {
                return; // the ears are the last thing to go, and on KIT they never do
            }
            if (point.y() < bellyLine) {
                // A soft-bottomed patch rising off the underline.
                double n = PatchNoise.field(seed ^ 0x7EL, point.x(), point.y(), point.z(), 0.12);
                double rise = 1.0 - PatchNoise.smoothstep(bellyLine - span * 0.18, bellyLine, point.y());
                if (n * (0.4 + 0.6 * rise) > 1.0 - 0.66 * s) {
                    whiten(f, px, py);
                    return;
                }
            }
            if (bodyCut >= 1.0) {
                return;
            }
            // Body white. The crest and the dock hold their colour longest, so a
            // near-white sabino still reads as a horse with a coloured mane
            // rather than as a dominant white.
            double anchor = (part == Part.MANE || part == Part.TAIL) ? 0.16 : 0.0;
            double bw = PatchNoise.field(seed ^ 0x1234L, point.x(), point.y(), point.z(), BODY_SCALE);
            double bodyJag = BODY_JAG * (PatchNoise.fbm2(seed ^ 0x5AB0L, point.x() * BODY_JAG_FREQ,
                    point.y() * BODY_JAG_FREQ, point.z() * BODY_JAG_FREQ * 1.4) - 0.5);
            if (bw + bodyJag > bodyCut + anchor) {
                whiten(f, px, py);
            }
        });
        return f;
    }

    // ------------------------------------------------------------------
    // The MITF / PAX3 shape: dipped in white from below
    // ------------------------------------------------------------------

    /** How far the waterline wobbles, as a fraction of body height. */
    private static final double LEVEL_WOBBLE = 0.055;
    private static final double LEVEL_FREQ = 0.9;
    /**
     * The waterline is a <b>hard</b> cut, not a fade. Two reasons, and they
     * point the same way: splash margins are sharply bounded in life, and the
     * gradient has no grey between a coloured texel and a spent one - a
     * half-scaled black texel samples the LUT's warm diagonal and reads
     * <i>gold</i>, so a one-pixel fade paints a tan fringe along the whole
     * waterline. The irregularity comes from wobbling <i>where</i> the line
     * falls, which is free of that problem.
     */

    /**
     * The splash shape at {@code strength}: the horse dipped in white to a
     * level line that rises with strength, plus a blaze widening to a bald
     * face. The margin is deliberately <b>much crisper</b> than
     * {@link #sabino}'s - one wobbled waterline rather than a field of torn
     * patches - because that is the visible difference between the two
     * patterns, and the thing a horse is looked at to tell them apart.
     *
     * <p>The strength is raised by whatever white is already on the horse - see
     * "white finds white" above. That is what makes an {@code MITF} splash and a
     * {@code PAX3} splash on the same horse add up instead of overlapping, and
     * it is the reason splash is two genes here rather than one.
     *
     * <p><b>Draw order</b>, off {@code ctx.epigeneticsFor(geneKey)}:
     * {@code nextLong()} (the noise seed), then {@code nextFloat()} for how high
     * the waterline sits, {@code nextFloat()} for the face's width and
     * {@code nextFloat()} for its length.
     */
    public static PigmentField splash(CoatBuildContext ctx, PigmentView coat, String geneKey, double strength) {
        double s = clamp01(strength + SPLASH_STACKING * alreadyWhite(coat, ctx.skin()));
        Rng epi = ctx.epigeneticsFor(geneKey);
        long seed = epi.nextLong();
        double levelRoll = epi.nextFloat();
        double faceRoll = epi.nextFloat();
        double lengthRoll = epi.nextFloat();

        Skin skin = ctx.skin();
        Bounds body = HorseSkinGeometry.bodyBounds(skin);
        double span = body.span(Axis.Y);
        // Splash is measured from the ground up, so the fraction is of the whole
        // horse, not of the barrel: 0.35 already means high stockings and belly.
        double level = body.yMin() + span * clamp01(0.06 + s * (0.78 + 0.18 * levelRoll));
        double wobble = span * LEVEL_WOBBLE;

        double faceHalf = 0.75 + 3.1 * s * (0.7 + 0.3 * faceRoll);
        double faceLength = clamp01(0.3 + 0.7 * s * (0.6 + 0.4 * lengthRoll));

        PigmentField f = coat.mutableCopy();
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            if (part == Part.HEAD || part == Part.MUZZLE) {
                if (Math.abs(point.z()) <= faceHalf && withinFace(skin, part, point, faceLength)) {
                    whiten(f, px, py);
                }
                return;
            }
            // One waterline over the whole horse, in absolute body-space Y, so a
            // patch runs unbroken from the barrel down a leg with no seam. Two
            // octaves of wobble: a long slow roll along the body plus a fine
            // one, so the line reads torn rather than ruled.
            double lift = (PatchNoise.fbm2(seed, point.x() * LEVEL_FREQ, 0.0, point.z() * LEVEL_FREQ) - 0.5)
                    * 2.0 * wobble
                    + (PatchNoise.fbm2(seed ^ 0x9E37L, point.x() * LEVEL_FREQ * 4.0, 0.0,
                            point.z() * LEVEL_FREQ * 4.0) - 0.5) * 2.0 * wobble * 0.35;
            if (point.y() < level + lift) {
                whiten(f, px, py);
            }
        });
        return f;
    }

    // ------------------------------------------------------------------

    /**
     * How much an already-white horse adds to a painter's strength. Splash
     * leans on it hardest, because two splash loci meeting is the case it
     * exists for; the {@code KIT} ramp is more conservative, since a sabino
     * over a tobiano should read as a loud pinto and not as a white horse.
     */
    private static final double SPLASH_STACKING = 0.55;
    private static final double SABINO_STACKING = 0.35;

    /**
     * Below this a pigment counts as gone. The same threshold the composer uses
     * to decide a texel is transparent, and far below anything a <i>dilution</i>
     * leaves behind - a grey double-dilute cream still carries about 0.012, and
     * a pale horse is not a white-spotted one.
     */
    private static final float GONE = 0.001f;

    /**
     * The fraction of this skin's texels that carry no pigment at all - i.e.
     * how white an earlier white-pattern gene has already made the horse.
     * Counts only texels both of whose pigments are <b>exactly</b> spent, so
     * dilutions never register.
     */
    private static double alreadyWhite(PigmentView coat, Skin skin) {
        int[] tally = new int[2];
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> {
            tally[1]++;
            if (coat.red(px, py) <= GONE && coat.black(px, py) <= GONE) {
                tally[0]++;
            }
        });
        return tally[1] == 0 ? 0.0 : tally[0] / (double) tally[1];
    }

    /**
     * Is this head / muzzle texel within {@code lengthFraction} of the head's
     * length, measured back from the nose? The muzzle is always in - a face
     * marking that stops short of the nose is a star, and stars are drawn by
     * giving the fraction a small value on the head alone.
     */
    private static boolean withinFace(Skin skin, Part part, HorseSkinGeometry.BodyPoint point,
                                      double lengthFraction) {
        if (part == Part.MUZZLE) {
            return true;
        }
        Bounds head = HorseSkinGeometry.bounds(skin, part);
        return point.x() >= head.xMax() - head.span(Axis.X) * clamp01(lengthFraction);
    }

    private static void whiten(PigmentField f, int px, int py) {
        f.setRed(px, py, 0f);
        f.setBlack(px, py, 0f);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
