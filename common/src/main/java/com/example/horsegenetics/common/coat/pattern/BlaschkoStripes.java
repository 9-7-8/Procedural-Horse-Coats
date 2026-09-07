package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

/**
 * <b>Blaschko lines</b> in body space - the streaked, fragmentary, left-right
 * mismatched field that {@code BrindleGene} paints with.
 *
 * <h2>Why brindle needs its own field</h2>
 * Brindle and zebra look superficially alike - "stripes on a horse" - and
 * sharing one stripe field between them is what made both of them wrong. They
 * are not the same picture and they do not come from the same place:
 *
 * <table>
 *   <tr><th></th><th>zebra ({@link ZebraStripes})</th><th>brindle (here)</th></tr>
 *   <tr><td>cause</td><td>a patterning system laid over a body map</td><td><b>X-inactivation mosaicism</b> - which cell clone won, where</td></tr>
 *   <tr><td>edges</td><td>crisp</td><td><b>soft, wavy, tapered</b></td></tr>
 *   <tr><td>symmetry</td><td>roughly matched side to side</td><td><b>none</b> - a streak on the right need not exist on the left</td></tr>
 *   <tr><td>continuity</td><td>a band runs the whole way</td><td><b>broken</b>: streaks fork, fade out and reappear</td></tr>
 *   <tr><td>width</td><td>fairly even along a band</td><td>varies <b>within one streak</b> - narrow at the back, broad on the barrel</td></tr>
 *   <tr><td>shape</td><td>vertical, arcing over the hip</td><td>a lazy <b>S</b> down the side</td></tr>
 *   <tr><td>legs</td><td>rings all the way round</td><td>a few <b>crosswise strokes</b>, upper leg only</td></tr>
 *   <tr><td>head</td><td>striped, dark muzzle</td><td><b>nothing</b></td></tr>
 * </table>
 *
 * <p>The asymmetry is the one that matters most, and it is the one a plain
 * stripe field structurally cannot produce: its phase is a function of
 * {@code |z|}, so whatever it draws on the right it draws on the left. A
 * brindle mare is a <b>functional mosaic</b> - each patch of skin randomly
 * silenced one of her two X chromosomes early in development - so the two sides
 * of her were coloured in by different draws. Here each band is rolled
 * <i>per side</i> and the two rolls are blended across a couple of body units
 * either side of the spine, so the sides differ without a seam down the back.
 *
 * <p>Coverage is <b>1 in the middle of a streak</b> and 0 between; the gene
 * decides what to do with that. Pure, like {@link BodyNoise}: same
 * {@code (seed, point, knobs)} in, same coverage out.
 */
public final class BlaschkoStripes {

    /**
     * The per-horse knobs.
     *
     * @param seed    the warp / break / per-band field's seed
     * @param spacing centre-to-centre streak distance in body units; the adult
     *                barrel is 22 long
     * @param duty    mean fraction of a period that is streak, before the
     *                per-band width hash varies it
     * @param warp    how far, in body units, the noise may bend a streak off
     *                its plane. <b>It has to stay well under half the
     *                spacing</b> - once the warp exceeds the gap, adjacent
     *                streaks bend into each other and the whole thing stops
     *                being stripes and starts being wood grain.
     */
    public record Pattern(long seed, double spacing, double duty, double warp) {}

    /** Soft edge as a fraction of the half-period. Wide: brindle feathers, it does not draw. */
    private static final double EDGE = 0.22;
    /** How gently the warp field bends a streak. */
    private static final double WARP_SCALE = 1.0 / 7.0;
    /** Per-band width spread. Brindle streaks are nothing like one width. */
    private static final double WIDTH_MIN = 0.45;
    private static final double WIDTH_RANGE = 1.10;

    /**
     * How far, per body unit off the centreline, a streak leans - the chevron
     * at the topline that the research describes as V-shaped, and the same
     * thing that keeps a constant-X face from rendering as one flat band.
     */
    private static final double SLANT = 0.40;
    /** How far the streak swings forward at mid-barrel: the S rather than the ruled line. */
    private static final double SWAY = 1.5;

    /** Below this much of the along-the-streak noise the streak simply is not there. */
    private static final double BREAK = 0.32;
    private static final double BREAK_SOFT = 0.30;
    private static final double BREAK_SCALE = 0.30;

    /** Half-width, in body units, of the blend between the left and right band rolls. */
    private static final double SIDE_BLEND = 2.0;
    /**
     * Gain on the per-side roll. Above 1 it saturates, so most bands are fully
     * present on a given side and the remainder ramp down to absent - rather
     * than every band on the horse sitting at some middling half-strength.
     */
    private static final double SIDE_GAIN = 2.0;

    /** The barrel: streaks die out before the lowest belly rather than banding it. */
    private static final double BELLY_BOTTOM = 0.05;
    private static final double BELLY_TOP = 0.30;

    /**
     * Upper-leg strokes only, and crosswise: where they start up the leg box and
     * how far they take to fade in.
     *
     * <p><b>"Upper leg" is not the top of the leg box.</b> The top third of
     * every leg is inside the barrel and never rendered, so a window centred on
     * the box's upper half draws strokes nobody can see. These target the
     * forearm and the gaskin - the part of the limb that is actually below the
     * body.
     */
    private static final double LEG_START = 0.28;
    private static final double LEG_FADE = 0.22;
    private static final double LEG_TIGHT = 0.50;

    private BlaschkoStripes() {}

    /**
     * Streak coverage at a texel: <b>1</b> in the middle of a streak, <b>0</b>
     * off it, with a soft, tapered edge in between. Parts brindle does not
     * touch - the head, the muzzle, the ears, the mane and the tail - return
     * {@code 0}.
     */
    public static double coverage(Skin skin, Part part, BodyPoint point, Pattern pat) {
        if (pat.spacing() <= 0 || !HorseSkinGeometry.hasPart(skin, part)) {
            return 0;
        }
        switch (part) {
            case BODY:
                return barrel(skin, point, pat);
            case NECK:
                return neck(skin, point, pat);
            case LEFT_FRONT_LEG:
            case RIGHT_FRONT_LEG:
            case LEFT_HIND_LEG:
            case RIGHT_HIND_LEG:
                return upperLeg(skin, part, point, pat);
            default:
                // Head, muzzle, ears, mane, tail. The head is not a defining
                // location for BR1 and is very often untouched, and the
                // rest-pose projection is only approximate up there anyway.
                return 0;
        }
    }

    /**
     * The barrel and quarters - the clearest area for brindle, and the only
     * place the S-flow is long enough to read.
     *
     * <p>The streak is a plane of constant {@code x}, plus three things that
     * take it away from being one: the chevron {@link #SLANT} at the topline,
     * a {@link #SWAY} that pushes the middle of the streak forward and lets the
     * two ends trail (which is the S), and the noise warp.
     */
    private static double barrel(Skin skin, BodyPoint point, Pattern pat) {
        Bounds b = HorseSkinGeometry.bounds(skin, Part.BODY);
        double fy = (point.y() - b.yMin()) / b.span(Axis.Y);   // 0 belly .. 1 topline
        double sway = SWAY * Math.sin((fy - 0.10) * Math.PI * 1.2);
        double along = point.x() + Math.abs(point.z()) * SLANT + sway;
        double fade = smooth01((fy - BELLY_BOTTOM) / (BELLY_TOP - BELLY_BOTTOM));
        return streak(along, point, pat) * fade;
    }

    /**
     * The neck - a common and often highly visible location, where the streaks
     * run lengthwise from the crest down toward the throat. Same field, no
     * sway: the neck is too short for the S to be anything but a wobble.
     */
    private static double neck(Skin skin, BodyPoint point, Pattern pat) {
        double along = point.x() + Math.abs(point.z()) * SLANT;
        return streak(along, point, pat);
    }

    /**
     * The upper legs, where brindle turns <b>crosswise</b>: a limb's Blaschko
     * lines wrap round it rather than running down it, so what shows here is
     * irregular partial bands and short transverse strokes on the upper leg,
     * not striping carried down to the hoof.
     */
    private static double upperLeg(Skin skin, Part leg, BodyPoint point, Pattern pat) {
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double span = b.span(Axis.Y);
        if (span <= 0) {
            return 0;
        }
        double up = (point.y() - b.yMin()) / span;
        double window = smooth01((up - LEG_START) / LEG_FADE);
        if (window <= 0) {
            return 0;
        }
        // Phase in y, so a stroke crosses the limb; the "along" axis the break
        // and the per-side roll read is then x, which runs round it.
        return streakAcross(point.y() / LEG_TIGHT, point, pat) * window;
    }

    /** A streak phased along the body's X axis - the body and neck case. */
    private static double streak(double along, BodyPoint point, Pattern pat) {
        double u = (along + warp(point, pat)) / pat.spacing();
        return evaluate(u, point.y(), point.z(), point, pat);
    }

    /** A streak phased up the limb - the crosswise case. Already in periods. */
    private static double streakAcross(double u, BodyPoint point, Pattern pat) {
        double phased = u + warp(point, pat) / pat.spacing();
        return evaluate(phased, point.x(), point.z(), point, pat);
    }

    /**
     * The band evaluator, and where the three irregularities live: a per-band
     * width hash so no two streaks are the same thickness, a noise field
     * <b>frozen to the band index</b> so a streak breaks and reappears as you
     * walk along it rather than as you walk across it, and the per-side
     * presence roll that is the mosaicism.
     */
    private static double evaluate(double u, double alongA, double alongB, BodyPoint point, Pattern pat) {
        int band = (int) Math.floor(u);
        double d = Math.abs((u - band) - 0.5) * 2.0;

        double width = pat.duty() * (WIDTH_MIN + WIDTH_RANGE * bandHash(pat.seed(), band, 1));
        double core = 1.0 - BodyStripes.smoothstep(width - EDGE, width + EDGE, d);
        if (core <= 0) {
            return 0;
        }

        double along = BodyNoise.value(pat.seed() ^ 0x51ED270155AA33CCL,
                band * 4.0, alongA * BREAK_SCALE, alongB * BREAK_SCALE);
        double present = smooth01((along - BREAK) / BREAK_SOFT);
        if (present <= 0) {
            return 0;
        }

        return core * present * side(band, point.z(), pat.seed());
    }

    /**
     * <b>The mosaicism.</b> Each band is rolled once for the horse's right and
     * once for its left, and the texel reads whichever side it is on - blended
     * over {@link #SIDE_BLEND} body units either side of the spine so the two
     * meet without a cut down the topline. Two copies of the same allele, two
     * independent answers: that is what a functional mosaic is.
     */
    private static double side(int band, double z, long seed) {
        double right = smooth01(bandHash(seed, band, 2) * SIDE_GAIN);
        double left = smooth01(bandHash(seed, band, 3) * SIDE_GAIN);
        double t = smooth01((z + SIDE_BLEND) / (2.0 * SIDE_BLEND));
        return left + (right - left) * t;
    }

    private static double warp(BodyPoint point, Pattern pat) {
        return (BodyNoise.value(pat.seed(), point.x() * WARP_SCALE, point.y() * WARP_SCALE,
                point.z() * WARP_SCALE) - 0.5) * 2.0 * pat.warp();
    }

    /**
     * A stable number in {@code [0, 1)} for one band. {@link BodyNoise}
     * interpolates, which is the wrong shape here: two adjacent streaks should
     * be unrelated, not a gradient between neighbours.
     */
    private static double bandHash(long seed, int band, int salt) {
        long h = seed ^ (band * 0x9E3779B97F4A7C15L) ^ (salt * 0xC2B2AE3D27D4EB4FL);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) / (double) (1L << 53);
    }

    private static double smooth01(double t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }
}
