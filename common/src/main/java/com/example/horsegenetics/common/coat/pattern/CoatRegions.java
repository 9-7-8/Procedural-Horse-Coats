package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.BodyPoint;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;

import java.util.List;

/**
 * Reusable "paint / restrict this body region" helpers, built on
 * {@link HorseSkinGeometry}. Coat-generating genes call these (passing
 * {@code ctx.skin()}) instead of hand-rolling texel loops. Parts a mesh
 * doesn't have (a foal has no MANE / MUZZLE) are silently skipped.
 */
public final class CoatRegions {

    private CoatRegions() {}

    public static final List<Part> LEGS = List.of(
            Part.LEFT_FRONT_LEG, Part.RIGHT_FRONT_LEG, Part.LEFT_HIND_LEG, Part.RIGHT_HIND_LEG);

    /**
     * Adult eyes: 2x2 pupil + 2x2 sclera per eye, verbatim from the template.
     * {x,y,w,h}. <b>Fixed 2026-09-06</b>: the left eye's rect was {@code {28,
     * 42, 4, 2}}, two texels short of where its pixels actually sit
     * ({@code x30-33}) - it grabbed 2 background pixels plus the pupil and
     * missed the sclera entirely. The template's raw column order is
     * <i>intentionally</i> mirrored between the two eyes (white-then-black on
     * the west face, black-then-white on the east - inherited unmodified from
     * vanilla's own {@code horse.png}), which is what makes the pupil land
     * nose-side on both faces once the standard box-UV unwrap reverses one
     * face's U-axis relative to the other. Cropping 2 columns short broke that
     * compensation and rendered the east eye's pupil on the wrong side.
     */
    private static final int[][] EYE_RECTS_ADULT = {
            {6, 42, 4, 2},   // right eye - head WEST face
            {30, 42, 4, 2},  // left eye  - head EAST face
    };

    /**
     * Foal eyes: the 2x2 pupil per eye on the head's LEFT / RIGHT faces of
     * {@code horse_white_baby.png} (the baby texture has no bright sclera). The
     * earlier values sat in the centre facial-marking blob, not on the eyes, so
     * the composed coat painted straight over the pupils.
     */
    private static final int[][] EYE_RECTS_BABY = {
            {6, 20, 2, 2},
            {40, 20, 2, 2},
    };

    /**
     * Index into {@link #eyeRects} of the eye on the head's <b>west</b> face.
     * The two eyes are addressed by index rather than by name because that is
     * all the sheet knows about them; "right" is the side of the horse the west
     * face is, not a side of the texture.
     */
    public static final int RIGHT_EYE = 0;

    /** Index into {@link #eyeRects} of the eye on the head's <b>east</b> face. */
    public static final int LEFT_EYE = 1;

    public static int[][] eyeRects(Skin skin) {
        return skin == Skin.BABY ? EYE_RECTS_BABY : EYE_RECTS_ADULT;
    }

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    // ---- direct paint (ARGB overlay) -----------------------------------

    public interface Paint {
        int argb(int px, int py, HorseSkinGeometry.BodyPoint p);
    }

    public static void paintPart(Skin skin, int[] overlay, Part part, Paint paint) {
        if (!HorseSkinGeometry.hasPart(skin, part)) {
            return;
        }
        HorseSkinGeometry.forEachTexel(skin, part, (px, py, pt, face, point) -> {
            int c = paint.argb(px, py, point);
            if ((c >>> 24) != 0) {
                overlay[py * N + px] = c;
            }
        });
    }

    public static void fillPart(Skin skin, int[] overlay, Part part, int argb) {
        paintPart(skin, overlay, part, (px, py, p) -> argb);
    }

    public static void fillMane(Skin skin, int[] overlay, int argb) {
        fillPart(skin, overlay, Part.MANE, argb);
    }

    public static void fillTail(Skin skin, int[] overlay, int argb) {
        fillPart(skin, overlay, Part.TAIL, argb);
    }

    public static void fillEars(Skin skin, int[] overlay, int argb) {
        fillPart(skin, overlay, Part.LEFT_EAR, argb);
        fillPart(skin, overlay, Part.RIGHT_EAR, argb);
    }

    public static void fillHooves(Skin skin, int[] overlay, int argb, double hoofFraction) {
        for (Part leg : LEGS) {
            paintLowerLeg(skin, overlay, leg, hoofFraction, (px, py, p) -> argb);
        }
    }

    public static void paintLowerLeg(Skin skin, int[] overlay, Part leg, double heightFraction, Paint paint) {
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double cutoff = b.yMin() + b.span(Axis.Y) * clamp01(heightFraction);
        HorseSkinGeometry.forEachTexel(skin, leg, (px, py, part, face, point) -> {
            if (point.y() <= cutoff) {
                int c = paint.argb(px, py, point);
                if ((c >>> 24) != 0) {
                    overlay[py * N + px] = c;
                }
            }
        });
    }

    /** Copy the eye texels straight from {@code template} into {@code dst}. */
    public static void redrawEyes(Skin skin, int[] dst, int[] template) {
        for (int[] r : eyeRects(skin)) {
            for (int y = r[1]; y < r[1] + r[3]; y++) {
                for (int x = r[0]; x < r[0] + r[2]; x++) {
                    if (x >= 0 && y >= 0 && x < N && y < N) {
                        dst[y * N + x] = template[y * N + x];
                    }
                }
            }
        }
    }

    // ---- pigment restriction -----------------------------------------

    public interface Restrict {
        void at(PigmentField field, int px, int py, HorseSkinGeometry.BodyPoint p);
    }

    public static void restrictPart(Skin skin, PigmentField field, Part part, Restrict rule) {
        if (!HorseSkinGeometry.hasPart(skin, part)) {
            return;
        }
        HorseSkinGeometry.forEachTexel(skin, part, (px, py, p, face, point) -> rule.at(field, px, py, point));
    }

    public static void restrictAll(Skin skin, PigmentField field, Restrict rule) {
        HorseSkinGeometry.forEachTexel(skin, (px, py, part, face, point) -> rule.at(field, px, py, point));
    }

    /** Full-black point (black 1, red 0) across a part. */
    public static void blackenPart(Skin skin, PigmentField field, Part part) {
        restrictPart(skin, field, part, (f, px, py, p) -> {
            f.setBlack(px, py, 1.0f);
            f.setRed(px, py, 0.0f);
        });
    }

    /** Full-black the bottom {@code heightFraction} of a leg. */
    public static void blackenLowerLeg(Skin skin, PigmentField field, Part leg, double heightFraction) {
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double cutoff = b.yMin() + b.span(Axis.Y) * clamp01(heightFraction);
        HorseSkinGeometry.forEachTexel(skin, leg, (px, py, part, face, point) -> {
            if (point.y() <= cutoff) {
                field.setBlack(px, py, 1.0f);
                field.setRed(px, py, 0.0f);
            }
        });
    }

    /** Full-black the muzzle (if present) plus the front {@code upFraction} of the head. */
    public static void blackenFace(Skin skin, PigmentField field, double upFraction) {
        blackenPart(skin, field, Part.MUZZLE);
        if (!HorseSkinGeometry.hasPart(skin, Part.HEAD)) {
            return;
        }
        Bounds head = HorseSkinGeometry.bounds(skin, Part.HEAD);
        double back = head.xMax() - head.span(Axis.X) * clamp01(upFraction);
        HorseSkinGeometry.forEachTexel(skin, Part.HEAD, (px, py, part, face, point) -> {
            if (point.x() >= back) {
                field.setBlack(px, py, 1.0f);
                field.setRed(px, py, 0.0f);
            }
        });
    }

    /**
     * Remove <i>both</i> pigments (-&gt; white template) up a leg.
     *
     * <p><b>No built-in gene calls this any more.</b> The cut is a hard
     * {@code point.y() <= cutoff}, so every sock it draws ends in a perfect
     * ring - which is why the white-pattern loci paint their own jagged or
     * wobbled margins in {@link WhitePattern} instead. Kept as a helper for a
     * data-driven or third-party gene that genuinely wants a clean edge; if you
     * want a realistic one, jitter the cutoff per texel.
     */
    public static void whitenLowerLeg(Skin skin, PigmentField field, Part leg, double heightFraction) {
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double cutoff = b.yMin() + b.span(Axis.Y) * clamp01(heightFraction);
        HorseSkinGeometry.forEachTexel(skin, leg, (px, py, part, face, point) -> {
            if (point.y() <= cutoff) {
                field.whiten(px, py, 1f);
            }
        });
    }

    /**
     * A white blaze: a centreline stripe on the muzzle (if present) + head,
     * {@code halfWidth} body-units either side of {@code z == 0}, up
     * {@code lengthFraction} of the head length from the nose.
     *
     * <p><b>Nothing calls this.</b> Face markings come from
     * {@link WhitePattern#faceMarking} now - one shared vocabulary of a star, a
     * stripe and a snip plus a width, which every white locus draws from, and
     * which can express the <i>detached</i> patches this shape structurally
     * cannot. Kept only so this warning has somewhere to live: a centreline
     * stripe is not a face-marking vocabulary, and reaching for one here is how
     * the loci ended up with four reinventions of the same wrong shape.
     */
    public static void whitenBlaze(Skin skin, PigmentField field, double halfWidth, double lengthFraction) {
        double back = 0;
        if (HorseSkinGeometry.hasPart(skin, Part.HEAD)) {
            Bounds head = HorseSkinGeometry.bounds(skin, Part.HEAD);
            back = head.xMax() - head.span(Axis.X) * clamp01(lengthFraction);
        }
        for (Part part : new Part[]{Part.MUZZLE, Part.HEAD}) {
            if (!HorseSkinGeometry.hasPart(skin, part)) {
                continue;
            }
            boolean isMuzzle = part == Part.MUZZLE;
            double b = back;
            HorseSkinGeometry.forEachTexel(skin, part, (px, py, pp, face, point) -> {
                if (Math.abs(point.z()) <= halfWidth && (isMuzzle || point.x() >= b)) {
                    field.whiten(px, py, 1f);
                }
            });
        }
    }

    // ---- primitive markings (dun) ----------------------------------

    /**
     * Coverage of a <b>dorsal stripe</b> at a texel: {@code 1} on the
     * centreline, smoothly fading to {@code 0} by {@code halfWidth} body-units
     * to either side of {@code z == 0}, and - on the barrel and neck - further
     * weighted so it only lands on the <b>upper</b> part of the box and not the
     * belly (the belly also runs along {@code z ~ 0}). The mane and the tail
     * get the full width: a real dun stripe carries up the crest and hangs in
     * the tail, and reaching the tail is the single most diagnostic thing about
     * it.
     *
     * <p><b>The head and the muzzle are not on this list.</b> They used to be,
     * which drew a dark line straight down a dun face. A real dorsal stripe
     * runs withers to dock; what a dun has on its face is a mask or
     * {@linkplain #faceCobweb cobwebbing}, a different shape from a different
     * cause, and giving both to one function is how the face ended up striped.
     */
    public static double dorsalStripe(Skin skin, Part part, BodyPoint point, double halfWidth) {
        double topWeight;
        switch (part) {
            case MANE, TAIL -> topWeight = 1.0;
            case BODY, NECK -> {
                Bounds b = HorseSkinGeometry.bounds(skin, part);
                double frac = (point.y() - b.yMin()) / b.span(Axis.Y);
                topWeight = smooth01((frac - 0.45) / 0.45);
            }
            default -> {
                return 0;
            }
        }
        if (topWeight <= 0 || halfWidth <= 0) {
            return 0;
        }
        double d = Math.abs(point.z()) / halfWidth;
        double lateral = d >= 1 ? 0 : 1 - d * d * (3 - 2 * d);
        return lateral * topWeight;
    }

    /** How far, in body units, noise may bend a leg bar off its plane. */
    private static final double BAR_WARP = 0.55;
    private static final double BAR_WARP_SCALE = 0.22;
    /** Soft edge as a fraction of the half-period - dun bars feather, they do not draw. */
    private static final double BAR_EDGE = 0.22;
    /** Below this much round-the-limb noise a band simply is not there. */
    private static final double BAR_BREAK = 0.34;
    private static final double BAR_BREAK_SOFT = 0.26;
    private static final double BAR_ROUND_SCALE = 0.45;

    /**
     * Coverage of dun <b>leg barring</b> at a texel on a leg part.
     *
     * <p><b>Not bracelets.</b> The field here used to be a plain function of
     * {@code y} - evenly spaced, equally strong, and identical the whole way
     * round the limb - so every leg wore the same three rings, low down on the
     * cannon. Real bars are short transverse <i>strokes</i>: uneven in spacing,
     * thickness and strength, frequently broken part-way round the leg,
     * concentrated at and just above the knee and the hock rather than spread
     * down to the hoof, and often nearly absent on one leg while another
     * carries three. So the field is built out of four separate irregularities:
     *
     * <ul>
     *   <li>a band phase in {@code y} <b>warped by 3D noise</b>, so a band
     *       wanders and reads slightly diagonal instead of ruler-flat;</li>
     *   <li>a per-band <b>thickness and strength</b> hash - no two bars alike;</li>
     *   <li>a second noise sample <b>frozen to the band index</b>, so it varies
     *       as you walk <i>round</i> the limb and not as you walk up it: that is
     *       what turns a ring into a slash, and one band into two fragments;</li>
     *   <li>a <b>window</b> that peaks at the joint and dies out at the pastern
     *       and again where the leg meets the body.</li>
     * </ul>
     *
     * <p>Pass a different {@code seed} per leg and the set stops matching, which
     * is the last of the four: real barring is not symmetric.
     *
     * @param seed    randomness for this leg
     * @param joint   height up the leg box, {@code [0, 1]}, where barring is
     *                strongest - the knee / hock
     * @param spread  how far either side of {@code joint} a bar can still reach
     * @param spacing centre-to-centre band distance in body units
     * @param duty    mean fraction of a period that is bar, before the per-band
     *                thickness hash varies it
     */
    public static double legBar(Skin skin, Part leg, BodyPoint point, long seed,
                                double joint, double spread, double spacing, double duty) {
        if (!HorseSkinGeometry.hasPart(skin, leg) || spacing <= 0) {
            return 0;
        }
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double span = b.span(Axis.Y);
        if (span <= 0) {
            return 0;
        }
        double up = point.y() - b.yMin();
        double window = 1.0 - smooth01(Math.abs(up / span - joint) / Math.max(1e-4, spread));
        if (window <= 0) {
            return 0;
        }

        double warp = (BodyNoise.value(seed, point.x() * BAR_WARP_SCALE, point.y() * BAR_WARP_SCALE,
                point.z() * BAR_WARP_SCALE) - 0.5) * 2.0 * BAR_WARP;
        double phase = (up + warp) / spacing;
        int band = (int) Math.floor(phase);
        double d = Math.abs((phase - band) - 0.5) * 2.0;      // 0 mid-bar .. 1 mid-gap

        double width = duty * (0.55 + 0.90 * bandHash(seed, band, 1));
        double strength = 0.55 + 0.45 * bandHash(seed, band, 2);
        double core = 1.0 - smooth01((d - (width - BAR_EDGE)) / (2.0 * BAR_EDGE));
        if (core <= 0) {
            return 0;
        }

        // Frozen in y to this band, free in x and z: the break runs round the
        // limb, not up it.
        double round = BodyNoise.value(seed ^ 0x51ED270155AA33CCL,
                point.x() * BAR_ROUND_SCALE, band * 4.0, point.z() * BAR_ROUND_SCALE);
        double presence = smooth01((round - BAR_BREAK) / BAR_BREAK_SOFT);

        return core * presence * strength * window;
    }

    /**
     * Coverage of a dun <b>shoulder bar</b>: one soft stroke crossing the
     * shoulder, leaning back and down from the withers - transverse or
     * diagonal, unlike the dorsal stripe head-to-tail run. It lives on the
     * front of the barrel and never reaches the belly.
     *
     * <p>Deliberately <b>one</b> smudged stroke rather than the fan of fine
     * lines some horses carry: at two texels to the body unit a fan is three
     * pixels of noise, and the common case a field guide describes is anyway
     * "a smudgy shadow".
     *
     * @param centre    where along the barrel the stroke sits, {@code 0} at the
     *                  rump and {@code 1} at the shoulder
     * @param halfWidth half the stroke width, in the same fraction-of-barrel units
     * @param lean      how far back the foot of the stroke is from its top
     */
    public static double shoulderBar(Skin skin, Part part, BodyPoint point, long seed,
                                     double centre, double halfWidth, double lean) {
        if (part != Part.BODY || halfWidth <= 0) {
            return 0;
        }
        Bounds b = HorseSkinGeometry.bounds(skin, part);
        double fx = (point.x() - b.xMin()) / b.span(Axis.X);   // 0 rump .. 1 shoulder
        double fy = (point.y() - b.yMin()) / b.span(Axis.Y);   // 0 belly .. 1 topline
        double window = smooth01((fx - 0.52) / 0.20) * smooth01((fy - 0.10) / 0.32);
        if (window <= 0) {
            return 0;
        }
        double warp = (BodyNoise.value(seed ^ 0x5A0FDE1277C3B901L, point.x() * 0.20,
                point.y() * 0.20, point.z() * 0.20) - 0.5) * 2.0 * 0.05;
        double u = fx + lean * (1.0 - fy) + warp;
        return (1.0 - smooth01(Math.abs(u - centre) / halfWidth)) * window;
    }

    /**
     * Coverage of a dun <b>face mask</b>: a broad darkening centred on the
     * forehead and falling off with distance, over the head box only. It is
     * what a strongly masked dun shows instead of the finer
     * {@linkplain #faceCobweb cobwebbing}, and the base that cobwebbing is
     * drawn on.
     *
     * @param reach radius in body units at which the mask has faded to nothing
     */
    public static double faceMask(Skin skin, Part part, BodyPoint point, double reach) {
        if (part != Part.HEAD || reach <= 0 || !HorseSkinGeometry.hasPart(skin, part)) {
            return 0;
        }
        return 1.0 - smooth01(foreheadDistance(skin, part, point) / reach);
    }

    /**
     * Coverage of <b>cobwebbing</b>: fine darker rings radiating from the
     * forehead, warped by noise so they branch and break rather than sitting as
     * clean circles. Multiplied by {@link #faceMask}, so it fades out with the
     * same reach - cobwebbing is the detail inside the mask, not a separate
     * marking somewhere else on the head.
     *
     * @param ringSpacing centre-to-centre ring distance in body units
     */
    public static double faceCobweb(Skin skin, Part part, BodyPoint point, long seed,
                                    double ringSpacing, double reach) {
        double mask = faceMask(skin, part, point, reach);
        if (mask <= 0 || ringSpacing <= 0) {
            return 0;
        }
        double r = foreheadDistance(skin, part, point);
        double warp = (BodyNoise.value(seed, point.x() * 0.55, point.y() * 0.55, point.z() * 0.55) - 0.5)
                * ringSpacing * 0.9;
        double phase = (r + warp) / ringSpacing;
        double d = Math.abs((phase - Math.floor(phase)) - 0.5) * 2.0;
        return (1.0 - smooth01((d - 0.18) / 0.26)) * mask;
    }

    /** Body-unit distance from a head texel to the middle of the forehead. */
    private static double foreheadDistance(Skin skin, Part part, BodyPoint point) {
        Bounds b = HorseSkinGeometry.bounds(skin, part);
        double dx = point.x() - (b.xMax() - b.span(Axis.X) * 0.30);
        double dy = point.y() - (b.yMax() - b.span(Axis.Y) * 0.22);
        return Math.sqrt(dx * dx + dy * dy + point.z() * point.z());
    }

    /**
     * A stable number in {@code [0, 1)} for one band of one leg - the per-band
     * thickness and strength jitter. {@link BodyNoise} interpolates, which is
     * the wrong shape here: two adjacent bands should be unrelated, not a
     * gradient between neighbours.
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

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
