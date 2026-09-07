package com.example.horsegenetics.common.genetics.genes;

import com.example.horsegenetics.common.coat.pattern.CoatTextureComposer;
import com.example.horsegenetics.common.coat.pattern.GradientLut;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Axis;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Part;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Skin;
import com.example.horsegenetics.common.genetics.Epigenome;
import com.example.horsegenetics.common.genetics.Genotype;
import com.example.horsegenetics.common.testutil.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>The two roans, and why they are not the same pattern.</b>
 *
 * <p>Classic roan ({@code Rn}) and varnish roan (the leopard complex's
 * {@code LP/LP}) share a word and almost nothing else, and a horse is told
 * apart by <i>shape</i> rather than by how much white it has. This class is that
 * identification checklist, run against the real pipeline:
 *
 * <ol>
 *   <li><b>Check the head and distal legs.</b> A dark head plus dark lower legs
 *       with broadly mixed body hairs is classic roan.</li>
 *   <li><b>Look at the body distribution.</b> Even roaning over shoulder,
 *       barrel, back, flank and hip is classic; irregular pale areas
 *       interspersed with dark anatomical shields is varnish.</li>
 *   <li><b>Look for the inverted V</b> above the dark lower leg, which is
 *       classic roan's most characteristic edge.</li>
 * </ol>
 *
 * <p>Everything is measured as <b>mean luma on a black-based horse</b>, where
 * the un-roaned control sits at {@value #BLACK_LUMA}: white hairs raise it, so
 * "darker" and "more roaned" are the same axis and one number reads both.
 */
class RoanPatternsTest {

    private static final int N = HorseSkinGeometry.SHEET_SIZE;

    /** Mean luma of an un-roaned black horse - the floor everything is measured against. */
    private static final double BLACK_LUMA = 0.20;

    private static final String BLACK = Codes.of("extension", "E/E", "agouti", "a/a");
    private static final String CLASSIC = BLACK + "-horsegenetics.roan=Rn/rn";
    private static final String VARNISH = BLACK + "-horsegenetics.leopard=LP/LP";

    private static final long[] SEEDS = {0L, 1L, 2L, 3L, 7L, 42L};

    // ------------------------------------------------------------------
    // Classic roan
    // ------------------------------------------------------------------

    /**
     * <b>Broad, relatively even distribution over the trunk</b> - the hallmark.
     * Shoulder, barrel, back, belly and hip all roan together; there is no
     * front-to-back gradient, because a classic roan is not lighter at one end.
     */
    @Test
    void classicRoanIsEvenOverTheWholeTrunk() {
        for (long seed : SEEDS) {
            int[] img = compose(CLASSIC, seed);
            double barrel = bodyZone(img, 0.25, 0.68, 0.0, 1.0);
            double shoulder = bodyZone(img, 0.68, 1.0, 0.0, 1.0);
            double hip = bodyZone(img, 0.0, 0.25, 0.0, 1.0);
            double back = bodyZone(img, 0.0, 1.0, 0.80, 1.0);
            double belly = bodyZone(img, 0.0, 1.0, 0.0, 0.20);
            double lo = Math.min(Math.min(barrel, shoulder), Math.min(hip, Math.min(back, belly)));
            double hi = Math.max(Math.max(barrel, shoulder), Math.max(hip, Math.max(back, belly)));
            assertTrue(barrel > BLACK_LUMA + 0.08, "the barrel is barely roaned at seed " + seed);
            assertTrue(hi - lo < 0.08,
                    "classic roan is uneven over the trunk at seed " + seed + ": shoulder " + shoulder
                            + " barrel " + barrel + " hip " + hip + " back " + back + " belly " + belly);
        }
    }

    /**
     * <b>No shoulder shield.</b> A strongly defined island of dark hair over the
     * shoulder or hip is a <i>varnish</i> trait, and its absence is one of the
     * two things a classic roan is confirmed by. This gene used to draw one by
     * accident, through a front-to-back intensity gradient that left the
     * shoulder nearly unroaned.
     */
    @Test
    void classicRoanHasNoDarkShieldOverTheShoulderOrHip() {
        for (long seed : SEEDS) {
            double delta = shieldDelta(compose(CLASSIC, seed));
            assertTrue(delta > -0.06,
                    "classic roan grew a dark shoulder/hip shield at seed " + seed + " (" + delta + ")");
        }
    }

    /**
     * <b>The dark mask, the solid mane and tail, and the dark lower legs</b> -
     * classic roan's exclusions, which are as diagnostic as its coverage.
     */
    @Test
    void classicRoanLeavesTheHeadManeTailAndLowerLegsSolid() {
        for (long seed : SEEDS) {
            int[] img = compose(CLASSIC, seed);
            assertTrue(partLuma(img, Part.MANE) < BLACK_LUMA + 0.03, "the mane roaned at seed " + seed);
            assertTrue(partLuma(img, Part.TAIL) < BLACK_LUMA + 0.03, "the tail roaned at seed " + seed);
            assertTrue(legBand(img, 0.0, 0.18) < BLACK_LUMA + 0.06,
                    "the lower leg roaned at seed " + seed);
            // The head is dark, but a modest amount of forehead white is allowed
            // on some individuals - so this is a ceiling, not an equality.
            assertTrue(partLuma(img, Part.HEAD) < BLACK_LUMA + 0.12,
                    "the head roaned like the body at seed " + seed);
        }
    }

    /** The neck is roaned - substantially - and blends the dark head into the roaned shoulder. */
    @Test
    void classicRoanRoansTheNeckButThinsItTowardThePoll() {
        // "The neck is roaned at all" is an absolute, and how strongly is a
        // per-horse roll, so it is asserted over the mean of the set rather
        // than per seed: some roans are subtle, and registering any gene
        // renumbers every epigenetic seed (known gap #47), which is how a
        // per-seed floor calibrated to the weakest current draw goes red on a
        // change that has nothing to do with roan. The two claims that are
        // about the pattern's *shape* stay per seed - they are true of every
        // roan however strong it is.
        double neckTotal = 0;
        double headTotal = 0;
        for (long seed : SEEDS) {
            int[] img = compose(CLASSIC, seed);
            double neck = partLuma(img, Part.NECK);
            double barrel = bodyZone(img, 0.25, 0.68, 0.0, 1.0);
            double head = partLuma(img, Part.HEAD);
            neckTotal += neck;
            headTotal += head;
            // Orderings, per seed and with no absolute margin on them: true of
            // every classic roan however faint, which a margin is not.
            assertTrue(neck < barrel + 0.02, "the neck should not out-roan the barrel at seed " + seed);
            assertTrue(neck >= head, "the head out-roaned the neck at seed " + seed);
        }
        double meanNeck = neckTotal / SEEDS.length;
        double meanHead = headTotal / SEEDS.length;
        assertTrue(meanNeck > BLACK_LUMA + 0.06,
                "classic roan should roan the neck, mean luma " + meanNeck);
        assertTrue(meanNeck > meanHead + 0.05,
                "there should be dark-mask contrast, neck " + meanNeck + " vs head " + meanHead);
    }

    /**
     * <b>The inverted V.</b> Classic roan's most characteristic edge: the dark
     * lower leg rises into the roaned limb in a point rather than on a level
     * line, so half-way up the leg the middle of a face is still base-coloured
     * while its edges have roaned.
     *
     * <p>Note the measurement runs <b>across each face</b>. A leg is a box and
     * every texel is on one of its flat sides, so a cone measured from the leg's
     * axis is at its outer radius everywhere and draws a level line - which is
     * exactly the bug this pins.
     */
    @Test
    void classicRoanEndsTheLowerLegInAPoint() {
        for (long seed : SEEDS) {
            int[] img = compose(CLASSIC, seed);
            double[] v = chevron(img);
            assertTrue(v[0] < v[1] - 0.08,
                    "no inverted V at seed " + seed + ": mid-face " + v[0] + " vs face edge " + v[1]);
        }
    }

    // ------------------------------------------------------------------
    // Varnish roan
    // ------------------------------------------------------------------

    /**
     * <b>Varnish marks.</b> The trait varnish roan is actually recognised by:
     * pigment persists over the bony prominences, so the horse ends up outlined
     * by its own anatomy. Measured over the shoulder-blade and point-of-hip
     * shields, which should be dramatically darker than the barrel around them.
     */
    @Test
    void varnishKeepsItsPigmentOverTheBones() {
        for (long seed : SEEDS) {
            double delta = shieldDelta(compose(VARNISH, seed));
            assertTrue(delta < -0.15,
                    "varnish left no dark shield over the shoulder/hip at seed " + seed + " (" + delta + ")");
        }
    }

    /**
     * <b>Varnish reaches the head; classic roan does not.</b> The loudest single
     * difference between the two, and the first thing the identification
     * checklist looks at.
     */
    @Test
    void varnishWhitensTheHeadAndClassicRoanLeavesItDark() {
        for (long seed : SEEDS) {
            double varnish = partLuma(compose(VARNISH, seed), Part.HEAD);
            double classic = partLuma(compose(CLASSIC, seed), Part.HEAD);
            assertTrue(varnish > BLACK_LUMA + 0.08,
                    "varnish left the head dark at seed " + seed + " (" + varnish + ")");
            assertTrue(varnish > classic + 0.08,
                    "the two roans agree about the head at seed " + seed);
        }
    }

    /**
     * <b>Varnish is patchy where classic roan is even.</b> Measured as the
     * spread of local white density across the barrel: an even salt-and-pepper
     * mixture is flat from one window to the next, and irregular pale areas are
     * not.
     */
    @Test
    void varnishIsPatchierThanClassicRoan() {
        double classic = 0;
        double varnish = 0;
        for (long seed : SEEDS) {
            classic += patchiness(compose(CLASSIC, seed));
            varnish += patchiness(compose(VARNISH, seed));
        }
        classic /= SEEDS.length;
        varnish /= SEEDS.length;
        assertTrue(varnish > classic * 1.4,
                "varnish (" + varnish + ") is not measurably patchier than classic roan (" + classic + ")");
    }

    // ------------------------------------------------------------------
    // Measurement
    // ------------------------------------------------------------------

    private static int[] compose(String code, long seed) {
        int[] t = new int[N * N];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> t[py * N + px] = 0xFFFFFFFF);
        int[] l = new int[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int shade = 255 - Math.round(y / 15f * 255);
                l[y * 16 + x] = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
            }
        }
        return CoatTextureComposer.compose(Genotype.parse(code), Epigenome.fromSeed(seed),
                Skin.ADULT, true, t, new GradientLut(l, 16, 16));
    }

    private static double luma(int argb) {
        return (0.299 * ((argb >> 16) & 0xFF) + 0.587 * ((argb >> 8) & 0xFF) + 0.114 * (argb & 0xFF)) / 255.0;
    }

    /** Mean luma of the BODY part inside a box of (length, height) fractions. */
    private static double bodyZone(int[] img, double x0, double x1, double y0, double y1) {
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, Part.BODY);
        double[] t = new double[2];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != Part.BODY) {
                return;
            }
            double fx = (point.x() - b.xMin()) / b.span(Axis.X);
            double fy = (point.y() - b.yMin()) / b.span(Axis.Y);
            if (fx >= x0 && fx < x1 && fy >= y0 && fy < y1) {
                t[0] += luma(img[py * N + px]);
                t[1]++;
            }
        });
        return t[1] == 0 ? 0 : t[0] / t[1];
    }

    private static double partLuma(int[] img, Part want) {
        double[] t = new double[2];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part == want) {
                t[0] += luma(img[py * N + px]);
                t[1]++;
            }
        });
        return t[1] == 0 ? 0 : t[0] / t[1];
    }

    /** Mean luma over the legs between two fractions of each leg's height. */
    private static double legBand(int[] img, double lo, double hi) {
        double[] t = new double[2];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (!isLeg(part)) {
                return;
            }
            Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, part);
            double up = (point.y() - b.yMin()) / b.span(Axis.Y);
            if (up >= lo && up < hi) {
                t[0] += luma(img[py * N + px]);
                t[1]++;
            }
        });
        return t[1] == 0 ? 0 : t[0] / t[1];
    }

    /**
     * Mean luma inside the shoulder-blade and point-of-hip zones minus the mean
     * well outside them, on the barrel. Strongly negative means a dark
     * anatomical shield; around zero means an even coat.
     */
    private static double shieldDelta(int[] img) {
        Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, Part.BODY);
        double bw = b.span(Axis.X);
        double bh = b.span(Axis.Y);
        double bz = b.span(Axis.Z) * 0.5;
        double zc = (b.zMin() + b.zMax()) * 0.5;
        double[][] centres = {
                {b.xMin() + bw * 0.76, b.yMin() + bh * 0.56, zc + bz * 0.92, bw * 0.17},
                {b.xMin() + bw * 0.76, b.yMin() + bh * 0.56, zc - bz * 0.92, bw * 0.17},
                {b.xMin() + bw * 0.15, b.yMin() + bh * 0.70, zc + bz * 0.92, bw * 0.15},
                {b.xMin() + bw * 0.15, b.yMin() + bh * 0.70, zc - bz * 0.92, bw * 0.15},
        };
        double[] in = new double[2];
        double[] out = new double[2];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part != Part.BODY) {
                return;
            }
            double best = Double.MAX_VALUE;
            for (double[] c : centres) {
                double dx = point.x() - c[0];
                double dy = point.y() - c[1];
                double dz = point.z() - c[2];
                best = Math.min(best, Math.sqrt(dx * dx + dy * dy + dz * dz) / c[3]);
            }
            double[] bucket = best < 0.5 ? in : (best > 1.4 ? out : null);
            if (bucket != null) {
                bucket[0] += luma(img[py * N + px]);
                bucket[1]++;
            }
        });
        double i = in[1] == 0 ? 0 : in[0] / in[1];
        double o = out[1] == 0 ? 0 : out[0] / out[1];
        return i - o;
    }

    /**
     * {@code {mid-face, face-edge}} mean luma half-way up the legs. The inverted
     * V shows up as the middle still being dark while the edges have roaned.
     */
    private static double[] chevron(int[] img) {
        double[] mid = new double[2];
        double[] edge = new double[2];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (!isLeg(part) || face.normal() == Axis.Y) {
                return;
            }
            Bounds b = HorseSkinGeometry.bounds(Skin.ADULT, part);
            double up = (point.y() - b.yMin()) / b.span(Axis.Y);
            if (up < 0.30 || up > 0.55) {
                return;
            }
            Axis a = face.spanA();
            double lo = a == Axis.X ? b.xMin() : b.zMin();
            double hi = a == Axis.X ? b.xMax() : b.zMax();
            double v = a == Axis.X ? point.x() : point.z();
            double fromMiddle = Math.abs(2 * (v - lo) / (hi - lo) - 1);
            double[] bucket = fromMiddle < 0.34 ? mid : (fromMiddle > 0.66 ? edge : null);
            if (bucket != null) {
                bucket[0] += luma(img[py * N + px]);
                bucket[1]++;
            }
        });
        return new double[]{
                mid[1] == 0 ? 0 : mid[0] / mid[1],
                edge[1] == 0 ? 0 : edge[0] / edge[1],
        };
    }

    /**
     * How far the local white density wanders across the barrel: the standard
     * deviation of mean luma over 4&times;4 texture windows that lie entirely on
     * the body. Even salt-and-pepper is flat; irregular pale areas are not.
     */
    private static double patchiness(int[] img) {
        boolean[] body = new boolean[N * N];
        HorseSkinGeometry.forEachTexel(Skin.ADULT, (px, py, part, face, point) -> {
            if (part == Part.BODY) {
                body[py * N + px] = true;
            }
        });
        double sum = 0;
        double sumSq = 0;
        int windows = 0;
        for (int wy = 0; wy + 4 <= N; wy += 4) {
            for (int wx = 0; wx + 4 <= N; wx += 4) {
                int in = 0;
                double lum = 0;
                for (int y = wy; y < wy + 4; y++) {
                    for (int x = wx; x < wx + 4; x++) {
                        if (body[y * N + x]) {
                            in++;
                            lum += luma(img[y * N + x]);
                        }
                    }
                }
                if (in < 16) {
                    continue;
                }
                double mean = lum / in;
                sum += mean;
                sumSq += mean * mean;
                windows++;
            }
        }
        if (windows < 2) {
            return 0;
        }
        double mean = sum / windows;
        return Math.sqrt(Math.max(0, sumSq / windows - mean * mean));
    }

    private static boolean isLeg(Part p) {
        return p == Part.LEFT_FRONT_LEG || p == Part.RIGHT_FRONT_LEG
                || p == Part.LEFT_HIND_LEG || p == Part.RIGHT_HIND_LEG;
    }
}
