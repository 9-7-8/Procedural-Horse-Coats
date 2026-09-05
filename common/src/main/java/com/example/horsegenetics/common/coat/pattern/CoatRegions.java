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

    /** Adult eyes: 2x2 pupil + 2x2 sclera per eye, verbatim from the template. {x,y,w,h}. */
    private static final int[][] EYE_RECTS_ADULT = {
            {6, 42, 4, 2},   // right eye - head WEST face
            {28, 42, 4, 2},  // left eye  - head EAST face
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
                field.setRed(px, py, 0f);
                field.setBlack(px, py, 0f);
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
                    field.setRed(px, py, 0f);
                    field.setBlack(px, py, 0f);
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
     * belly (the belly also runs along {@code z ~ 0}). The mane, tail, head and
     * muzzle get the full width (the stripe carries up the crest and down the
     * face, and hangs in the tail).
     */
    public static double dorsalStripe(Skin skin, Part part, BodyPoint point, double halfWidth) {
        double topWeight;
        switch (part) {
            case MANE, TAIL, HEAD, MUZZLE -> topWeight = 1.0;
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

    /**
     * Coverage of horizontal <b>leg barring</b> at a texel on a leg part:
     * near-constant-{@code y} bands repeating every {@code spacing} body-units,
     * each band {@code duty} of the period wide with a soft edge, and the whole
     * field fading out over the top {@code (1 - reach)} of the leg so the bars
     * sit on the cannon and gaskin, not the stifle.
     */
    public static double legBar(Skin skin, Part leg, BodyPoint point, double spacing, double duty, double reach) {
        if (!HorseSkinGeometry.hasPart(skin, leg)) {
            return 0;
        }
        Bounds b = HorseSkinGeometry.bounds(skin, leg);
        double frac = (point.y() - b.yMin()) / b.span(Axis.Y);
        double fade = 1.0 - smooth01((frac - reach) / Math.max(1e-4, 1 - reach));
        if (fade <= 0) {
            return 0;
        }
        double phase = (point.y() - b.yMin()) / spacing;
        double off = phase - Math.floor(phase);
        double d = Math.abs(off - 0.5) * 2.0;      // 0 mid-bar .. 1 mid-gap
        return (1.0 - smooth01((d - (duty - 0.12)) / 0.24)) * fade;
    }

    private static double smooth01(double t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
