package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;

import java.util.List;

/**
 * Reusable "paint / restrict this body region" helpers, built on
 * {@link HorseSkinGeometry}. Coat-generating genes call these instead of
 * hand-rolling texel loops.
 *
 * <p>Two flavours:
 * <ul>
 *   <li><b>pigment restriction</b> - {@code restrict*} mutate a
 *       {@link PigmentField} (used during the gene {@code restrict} pass), and</li>
 *   <li><b>direct paint</b> - {@code paint*} write ARGB into an {@code int[]}
 *       overlay (used during the {@code paint} pass, or by a whole-coat
 *       generator like {@link BayCoat}).</li>
 * </ul>
 */
public final class CoatRegions {

    private CoatRegions() {}

    public static final List<Part> LEGS = List.of(
            Part.LEFT_FRONT_LEG, Part.RIGHT_FRONT_LEG, Part.LEFT_HIND_LEG, Part.RIGHT_HIND_LEG);

    /** Two eyes as {x, y, w, h} texel rects on the 128px sheet (from the white template). */
    public static final int[][] EYE_RECTS = {
            {3, 40, 10, 5},   // horse's right eye - head WEST face
            {26, 40, 10, 5},  // horse's left eye  - head EAST face
    };

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    // ---- direct paint (ARGB overlay) -----------------------------------

    public interface Paint {
        /** Colour for this texel, or a fully-transparent value to leave it alone. */
        int argb(int px, int py, HorseSkinGeometry.BodyPoint p);
    }

    /** Paint every mapped texel of {@code part} using {@code paint}. */
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

    /** Paint the bottom {@code hoofFraction} of every leg (0.10-0.15 looks like a hoof). */
    public static void fillHooves(int[] overlay, int argb, double hoofFraction) {
        for (Part leg : LEGS) {
            paintLowerLeg(overlay, leg, hoofFraction, (px, py, p) -> argb);
        }
    }

    /** Paint a leg from the hoof up to {@code heightFraction} of its height. */
    public static void paintLowerLeg(int[] overlay, Part leg, double heightFraction, Paint paint) {
        Bounds b = HorseSkinGeometry.bounds(leg);
        double cutoff = b.yMin() + b.span(HorseSkinGeometry.Axis.Y) * clamp01(heightFraction);
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

    /** Set both pigments to a "full black point" (black 1, red 0) across a part. */
    public static void blackenPart(PigmentField field, Part part) {
        restrictPart(field, part, (f, px, py, p) -> {
            f.setBlack(px, py, 1.0f);
            f.setRed(px, py, 0.0f);
        });
    }

    /** Full-black the bottom {@code heightFraction} of a leg (bay/black points). */
    public static void blackenLowerLeg(PigmentField field, Part leg, double heightFraction) {
        Bounds b = HorseSkinGeometry.bounds(leg);
        double cutoff = b.yMin() + b.span(HorseSkinGeometry.Axis.Y) * clamp01(heightFraction);
        HorseSkinGeometry.forEachTexel(leg, (px, py, part, face, point) -> {
            if (point.y() <= cutoff) {
                field.setBlack(px, py, 1.0f);
                field.setRed(px, py, 0.0f);
            }
        });
    }

    /**
     * Full-black the muzzle plus the front {@code upFraction} of the head
     * (0 = muzzle only, 1 = whole head) - bay/seal face points.
     */
    public static void blackenFace(PigmentField field, double upFraction) {
        blackenPart(field, Part.MUZZLE);
        Bounds head = HorseSkinGeometry.bounds(Part.HEAD);
        double back = head.xMax() - head.span(HorseSkinGeometry.Axis.X) * clamp01(upFraction);
        HorseSkinGeometry.forEachTexel(Part.HEAD, (px, py, part, face, point) -> {
            if (point.x() >= back) {
                field.setBlack(px, py, 1.0f);
                field.setRed(px, py, 0.0f);
            }
        });
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
