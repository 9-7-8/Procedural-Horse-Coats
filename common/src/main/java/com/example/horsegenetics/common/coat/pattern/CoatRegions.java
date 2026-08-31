package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;

import java.util.List;

/**
 * Reusable "paint / restrict this body region" helpers, built on
 * {@link HorseSkinGeometry}. Coat-generating genes call these instead of
 * hand-rolling texel loops.
 *
 * <ul>
 *   <li><b>restrict*</b> / <b>blacken*</b> / <b>whiten*</b> mutate a
 *       {@link PigmentField} (the natural-gene pass);</li>
 *   <li><b>paint*</b> write ARGB into an {@code int[]} overlay.</li>
 * </ul>
 */
public final class CoatRegions {

    private CoatRegions() {}

    public static final List<Part> LEGS = List.of(
            Part.LEFT_FRONT_LEG, Part.RIGHT_FRONT_LEG, Part.LEFT_HIND_LEG, Part.RIGHT_HIND_LEG);

    /**
     * The two eyes: the pupil dot + the sclera dot, exactly as in the vanilla
     * white texture (1px + 1px at 64px, so 2x2 + 2x2 here). {x, y, w, h}.
     */
    public static final int[][] EYE_RECTS = {
            {6, 42, 4, 2},   // horse's right eye - head WEST face: sclera (6,42) + pupil (8,42), each 2x2
            {28, 42, 4, 2},  // horse's left eye  - head EAST face: sclera (28,42) + pupil (30,42)
    };

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    // ---- direct paint (ARGB overlay) -----------------------------------

    public interface Paint {
        int argb(int px, int py, HorseSkinGeometry.BodyPoint p);
    }

    public static void paintPart(int[] overlay, Part part, Paint paint) {
        HorseSkinGeometry.forEachTexel(part, (px, py, pt, face, point) -> {
            int c = paint.argb(px, py, point);
            if ((c >>> 24) != 0) {
                overlay[py * N + px] = c;
            }
        });
    }

    public static void fillPart(int[] overlay, Part part, int argb) {
        paintPart(overlay, part, (px, py, p) -> argb);
    }

    public static void fillMane(int[] overlay, int argb) {
        fillPart(overlay, Part.MANE, argb);
    }

    public static void fillTail(int[] overlay, int argb) {
        fillPart(overlay, Part.TAIL, argb);
    }

    public static void fillEars(int[] overlay, int argb) {
        fillPart(overlay, Part.LEFT_EAR, argb);
        fillPart(overlay, Part.RIGHT_EAR, argb);
    }

    public static void fillHooves(int[] overlay, int argb, double hoofFraction) {
        for (Part leg : LEGS) {
            paintLowerLeg(overlay, leg, hoofFraction, (px, py, p) -> argb);
        }
    }

    public static void paintLowerLeg(int[] overlay, Part leg, double heightFraction, Paint paint) {
        Bounds b = HorseSkinGeometry.bounds(leg);
        double cutoff = b.yMin() + b.span(Axis.Y) * clamp01(heightFraction);
        HorseSkinGeometry.forEachTexel(leg, (px, py, part, face, point) -> {
            if (point.y() <= cutoff) {
                int c = paint.argb(px, py, point);
                if ((c >>> 24) != 0) {
                    overlay[py * N + px] = c;
                }
            }
        });
    }

    /** Copy the eye texels straight from {@code template} into {@code dst} (both row-major ARGB, 128px). */
    public static void redrawEyes(int[] dst, int[] template) {
        for (int[] r : EYE_RECTS) {
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

    public static void restrictPart(PigmentField field, Part part, Restrict rule) {
        HorseSkinGeometry.forEachTexel(part, (px, py, p, face, point) -> rule.at(field, px, py, point));
    }

    public static void restrictAll(PigmentField field, Restrict rule) {
        HorseSkinGeometry.forEachTexel((px, py, part, face, point) -> rule.at(field, px, py, point));
    }

    /** Full-black point (black 1, red 0) across a part. */
    public static void blackenPart(PigmentField field, Part part) {
        restrictPart(field, part, (f, px, py, p) -> {
            f.setBlack(px, py, 1.0f);
            f.setRed(px, py, 0.0f);
        });
    }

    /** Full-black the bottom {@code heightFraction} of a leg. */
    public static void blackenLowerLeg(PigmentField field, Part leg, double heightFraction) {
        Bounds b = HorseSkinGeometry.bounds(leg);
        double cutoff = b.yMin() + b.span(Axis.Y) * clamp01(heightFraction);
        HorseSkinGeometry.forEachTexel(leg, (px, py, part, face, point) -> {
            if (point.y() <= cutoff) {
                field.setBlack(px, py, 1.0f);
                field.setRed(px, py, 0.0f);
            }
        });
    }

    /** Full-black the muzzle plus the front {@code upFraction} of the head. */
    public static void blackenFace(PigmentField field, double upFraction) {
        blackenPart(field, Part.MUZZLE);
        Bounds head = HorseSkinGeometry.bounds(Part.HEAD);
        double back = head.xMax() - head.span(Axis.X) * clamp01(upFraction);
        HorseSkinGeometry.forEachTexel(Part.HEAD, (px, py, part, face, point) -> {
            if (point.x() >= back) {
                field.setBlack(px, py, 1.0f);
                field.setRed(px, py, 0.0f);
            }
        });
    }

    /** Remove <i>both</i> pigments (-> transparent -> white template) up a leg. */
    public static void whitenLowerLeg(PigmentField field, Part leg, double heightFraction) {
        Bounds b = HorseSkinGeometry.bounds(leg);
        double cutoff = b.yMin() + b.span(Axis.Y) * clamp01(heightFraction);
        HorseSkinGeometry.forEachTexel(leg, (px, py, part, face, point) -> {
            if (point.y() <= cutoff) {
                field.setRed(px, py, 0f);
                field.setBlack(px, py, 0f);
            }
        });
    }

    /**
     * A white blaze: a centreline stripe on the muzzle + head, {@code halfWidth}
     * body-units either side of {@code z == 0}, running up {@code lengthFraction}
     * of the head length from the nose.
     */
    public static void whitenBlaze(PigmentField field, double halfWidth, double lengthFraction) {
        Bounds head = HorseSkinGeometry.bounds(Part.HEAD);
        double back = head.xMax() - head.span(Axis.X) * clamp01(lengthFraction);
        for (Part part : new Part[]{Part.MUZZLE, Part.HEAD}) {
            boolean isMuzzle = part == Part.MUZZLE;
            HorseSkinGeometry.forEachTexel(part, (px, py, pp, face, point) -> {
                if (Math.abs(point.z()) <= halfWidth && (isMuzzle || point.x() >= back)) {
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
