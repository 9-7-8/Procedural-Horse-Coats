package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
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

    /** Remove <i>both</i> pigments (-> white template) up a leg. */
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

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
