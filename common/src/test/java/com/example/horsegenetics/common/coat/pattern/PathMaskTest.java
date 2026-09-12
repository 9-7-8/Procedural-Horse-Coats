package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.genetics.spec.SpecSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The <b>{@code PATH} mask's geometry</b> - {@link SpecPainter#pathCoverage}.
 *
 * <p>{@code PATH} is the one mask that carries a shape rather than a rule for
 * making one, so there is no field to measure and no distribution to defend.
 * What can go wrong instead is geometry, and all of it is the quiet kind: a
 * curve that misses the points somebody placed, a fill whose parity flips along
 * one row of texels, an end tangent that overshoots the horse. Each of those
 * looks like a rendering artefact and is arithmetic.
 *
 * <p>Everything here is in {@code units} space (the identity conversion,
 * {@code uMin=0, uSpan=1}) so the numbers in the test are the numbers in the
 * plane. The {@code body} normalisation is one multiply on top and is checked
 * separately.
 */
class PathMaskTest {

    private static final double SOFT = 0.25;

    /** A stroked path in units space. */
    private static double stroke(double[] pts, boolean curve, boolean closed,
                                 double u, double v, double width) {
        return SpecPainter.pathCoverage(pts, curve, closed, false, 0, 1, 0, 1, u, v,
                width / 2, SOFT);
    }

    /** A filled path in units space. */
    private static double fill(double[] pts, boolean curve, double u, double v) {
        return SpecPainter.pathCoverage(pts, curve, true, true, 0, 1, 0, 1, u, v, 0, SOFT);
    }

    // ------------------------------------------------------------------
    // Stroking
    // ------------------------------------------------------------------

    /**
     * The width means what it says: a stroke of width 2 reaches exactly one
     * body unit either side of the line, then fades over {@code softness}.
     */
    @Test
    void aStrokeIsAsWideAsItSays() {
        double[] line = {0, 0, 10, 0};
        assertEquals(1.0, stroke(line, false, false, 5, 0, 2.0), 1e-9, "on the line");
        assertEquals(1.0, stroke(line, false, false, 5, 0.99, 2.0), 1e-9, "just inside the half-width");
        assertEquals(0.0, stroke(line, false, false, 5, 1.0 + SOFT, 2.0), 1e-9, "past the fade");
        double mid = stroke(line, false, false, 5, 1.0 + SOFT / 2, 2.0);
        assertTrue(mid > 0.1 && mid < 0.9, "the fade should be a fade, got " + mid);
    }

    /**
     * <b>A stroke ends where the path ends</b> - it does not run on. An
     * open path is a segment, not a line, and the cap is round because the
     * distance is to the nearest <em>point</em> of the segment.
     */
    @Test
    void anOpenStrokeDoesNotRunPastItsEnds() {
        double[] line = {0, 0, 10, 0};
        assertEquals(1.0, stroke(line, false, false, 10, 0, 2.0), 1e-9, "at the end point");
        assertEquals(0.0, stroke(line, false, false, 11.5, 0, 2.0), 1e-9, "well past the end");
        // The cap is a disc of the half-width around the end point.
        assertEquals(1.0, stroke(line, false, false, 10.9, 0, 2.0), 1e-9, "inside the round cap");
    }

    /** Closing the path adds the segment from the last point back to the first. */
    @Test
    void closingAddsTheReturnSegment() {
        double[] tri = {0, 0, 10, 0, 5, 8};
        assertEquals(0.0, stroke(tri, false, false, 2.5, 4, 1.0), 1e-9,
                "the closing edge should not exist on an open path");
        assertEquals(1.0, stroke(tri, false, true, 2.5, 4, 1.0), 1e-9,
                "the closing edge runs from (5,8) back to (0,0)");
    }

    // ------------------------------------------------------------------
    // The curve
    // ------------------------------------------------------------------

    /**
     * <b>A smoothed path passes through every control point.</b> This is the
     * whole reason it is Catmull-Rom and not a Bezier: the points are what an
     * author placed and what the creator draws as draggable handles, so a
     * curve that merely gets near them would make the editor lie about its own
     * data.
     */
    @Test
    void theCurvePassesThroughEveryControlPoint() {
        double[] pts = {0, 0, 4, 6, 9, 1, 14, 7, 18, 2};
        for (int i = 0; i < pts.length / 2; i++) {
            double u = pts[i * 2];
            double v = pts[i * 2 + 1];
            assertEquals(1.0, stroke(pts, true, false, u, v, 0.2), 1e-9,
                    "the curve misses its own control point " + i + " at (" + u + ", " + v + ")");
        }
    }

    /**
     * <b>And it does not overshoot past the ends.</b> The end tangents come
     * from duplicating the first and last point rather than from extrapolating
     * a phantom one; extrapolation makes the curve leave the shape and come
     * back, which on a horse means a marking with a whisker on it.
     */
    @Test
    void theCurveDoesNotOvershootItsEnds() {
        double[] pts = {0, 0, 4, 6, 9, 1};
        // Sampled off the spline directly rather than through a stroke: a
        // stroke has a softness fade around it, so asking "is the coverage zero
        // behind the first point" measures the fade and not the curve.
        int spans = pts.length / 2 - 1;
        double minU = Double.MAX_VALUE;
        double maxU = -Double.MAX_VALUE;
        for (int span = 0; span < spans; span++) {
            for (int i = 0; i <= 400; i++) {
                double u = SpecPainter.spline(pts, pts.length / 2, false, span, i / 400.0, 0);
                minU = Math.min(minU, u);
                maxU = Math.max(maxU, u);
            }
        }
        assertTrue(minU >= -1e-9, "the curve ran back to u=" + minU + ", behind its first point");
        assertTrue(maxU <= 9 + 1e-9, "the curve ran on to u=" + maxU + ", past its last point");
    }

    /** A smoothed span really is curved - it is not the straight line in disguise. */
    @Test
    void smoothingActuallyBendsTheLine() {
        // Asymmetric on purpose. A span whose two neighbours are placed
        // symmetrically has a Catmull-Rom midpoint that lands exactly on the
        // chord's midpoint, so a symmetric probe cannot tell a curve from a
        // straight line - which is how this test failed when it was written.
        double[] pts = {0, 0, 5, 0, 10, 5, 30, 5};
        double biggest = 0;
        for (int i = 1; i < 8; i++) {
            double t = i / 8.0;
            double cu = SpecPainter.spline(pts, pts.length / 2, false, 1, t, 0);
            double cv = SpecPainter.spline(pts, pts.length / 2, false, 1, t, 1);
            // The chord of span 1 runs (5,0) to (10,5).
            biggest = Math.max(biggest, Math.abs((cu - 5) - (cv - 0)));
        }
        assertTrue(biggest > 0.2,
                "the smoothed span should leave its chord, but the largest departure was "
                        + biggest);
    }

    // ------------------------------------------------------------------
    // Filling
    // ------------------------------------------------------------------

    /** Inside is solid, outside fades, and the boundary is the outline. */
    @Test
    void aFilledPathIsSolidInside() {
        double[] square = {0, 0, 10, 0, 10, 10, 0, 10};
        assertEquals(1.0, fill(square, false, 5, 5), 1e-9, "the middle");
        assertEquals(1.0, fill(square, false, 0.2, 0.2), 1e-9, "just inside a corner");
        assertEquals(0.0, fill(square, false, -SOFT, 5), 1e-9, "past the fade, outside");
        assertEquals(0.0, fill(square, false, 5, 10 + SOFT), 1e-9, "past the fade, above");
    }

    /**
     * <b>The parity test must not double-count a vertex that lies on the ray.</b>
     * This is the classic way an even-odd fill goes wrong, and it shows up as a
     * single row of texels through the shape coming out inverted - which on a
     * horse reads as a scanline, not as a bug in a comparison. Every row through
     * the shape is swept, vertices included.
     */
    @Test
    void theFillHasNoInvertedRows() {
        double[] diamond = {5, 0, 10, 5, 5, 10, 0, 5};
        for (int i = 0; i <= 100; i++) {
            double v = i * 0.1;
            double got = fill(diamond, false, 5, v);
            boolean shouldBeIn = v > 0.5 && v < 9.5;
            if (shouldBeIn) {
                assertEquals(1.0, got, 1e-9, "row v=" + v + " came out hollow");
            }
        }
        // And the vertices themselves, which are exactly on their own rows.
        assertEquals(1.0, fill(diamond, false, 5, 5), 1e-9);
        assertEquals(1.0, fill(diamond, false, 5.0, 0.6), 1e-9);
    }

    /** A concave shape stays concave - the notch is not filled in. */
    @Test
    void aConcaveShapeKeepsItsNotch() {
        // A chevron: the notch is the wedge between the two arms.
        double[] chevron = {0, 0, 5, 6, 10, 0, 10, 3, 5, 9, 0, 3};
        assertEquals(1.0, fill(chevron, false, 5, 7), 1e-9, "inside the upper arm");
        assertEquals(0.0, fill(chevron, false, 5, 1.0), 1e-9, "the notch below the vertex");
    }

    // ------------------------------------------------------------------
    // Purity and the guards
    // ------------------------------------------------------------------

    /** Same inputs, same answer - it reads no state at all. */
    @Test
    void itIsPure() {
        double[] pts = {0, 0, 4, 6, 9, 1};
        for (int i = 0; i < 50; i++) {
            assertEquals(stroke(pts, true, true, 3.3, 2.2, 1.4),
                    stroke(pts, true, true, 3.3, 2.2, 1.4), 0.0);
        }
    }

    /**
     * {@code body} space is one multiply on the control points, and only on the
     * control points: a path at 0.25-0.75 of a ten-unit axis is the same shape
     * as one written 2.5-7.5 in units, and the stroke width means the same
     * thing in both.
     */
    @Test
    void bodySpaceScalesThePointsAndNotTheWidth() {
        double[] normalised = {0.25, 0.5, 0.75, 0.5};
        double[] units = {2.5, 5.0, 7.5, 5.0};
        for (double u = 0; u < 10; u += 0.25) {
            double a = SpecPainter.pathCoverage(normalised, false, false, false,
                    0, 10, 0, 10, u, 5.0, 0.5, SOFT);
            double b = SpecPainter.pathCoverage(units, false, false, false,
                    0, 1, 0, 1, u, 5.0, 0.5, SOFT);
            assertEquals(b, a, 1e-9, "the two spaces disagree at u=" + u);
        }
    }

    /** Fewer than two points paints nothing rather than throwing. */
    @Test
    void aDegeneratePathPaintsNothing() {
        assertEquals(0.0, SpecPainter.pathCoverage(new double[]{1, 1}, false, false, false,
                0, 1, 0, 1, 1, 1, 1, SOFT), 0.0);
    }

    /** The curve sampling constant is shared with the creator, so it is pinned. */
    @Test
    void theCurveSamplingIsTheSharedConstant() {
        assertEquals(8, SpecSchema.PATH_CURVE_SAMPLES,
                "the creator's port hard-codes this number too - change both or neither");
    }

    // ------------------------------------------------------------------
    // The minimal shape - 'pointsMin'
    // ------------------------------------------------------------------

    /** A stroked path morphed toward {@code min} at blend {@code t}. */
    private static double morph(double[] pts, double[] min, double t,
                                boolean curve, double u, double v, double width) {
        return SpecPainter.pathCoverage(pts, min, t, curve, false, false,
                0, 1, 0, 1, u, v, width / 2, SOFT);
    }

    /**
     * The two ends are the two shapes. At full expression the mark is exactly
     * what {@code points} drew and the minimal shape is not consulted; at zero
     * it is exactly {@code pointsMin}. This is the whole contract - everything
     * else is what happens between them.
     */
    @Test
    void theEndsOfTheBlendAreTheTwoShapesThemselves() {
        double[] full = {0.2, 0.5, 0.8, 0.5};
        double[] min = {0.4, 0.5, 0.6, 0.5};
        for (double u = 0; u <= 1.0; u += 0.05) {
            assertEquals(stroke(full, false, false, u, 0.5, 0.1),
                    morph(full, min, 1.0, false, u, 0.5, 0.1), 1e-12,
                    "expression 1 is not the drawn shape at u=" + u);
            assertEquals(stroke(min, false, false, u, 0.5, 0.1),
                    morph(full, min, 0.0, false, u, 0.5, 0.1), 1e-12,
                    "expression 0 is not the minimal shape at u=" + u);
        }
    }

    /**
     * <b>It shrinks, it does not fade.</b> That is the difference between a
     * morph and the crossfade the format deliberately did not take: at half
     * expression the mark still has a hard edge, and that edge sits halfway
     * between the two ends rather than the middle being a half-covered blur of
     * both.
     */
    @Test
    void halfwayIsAShapeAndNotAGhostOfTwo() {
        double[] full = {0.1, 0.5, 0.9, 0.5};
        double[] min = {0.4, 0.5, 0.6, 0.5};

        // A hard edge, so the softness is tightened to 0.02 - the default fade
        // is a quarter of the plane wide and would swallow the distinction.
        double tight = 0.02;
        double outside = SpecPainter.pathCoverage(full, min, 0.5, false, false, false,
                0, 1, 0, 1, 0.85, 0.5, 0.0, tight);
        double inside = SpecPainter.pathCoverage(full, min, 0.5, false, false, false,
                0, 1, 0, 1, 0.50, 0.5, 0.0, tight);
        double fullThere = SpecPainter.pathCoverage(full, null, 1.0, false, false, false,
                0, 1, 0, 1, 0.85, 0.5, 0.0, tight);
        double minThere = SpecPainter.pathCoverage(min, null, 1.0, false, false, false,
                0, 1, 0, 1, 0.85, 0.5, 0.0, tight);

        // The line at half expression runs 0.25..0.75, so 0.85 is off its end
        // while still being well inside the fully expressed 0.1..0.9.
        assertEquals(1.0, inside, 1e-9, "the middle of a half-expressed mark is still solid");
        assertEquals(0.0, outside, 1e-9,
                "a texel past the end of the half-expressed mark is outside it, not half-lit");
        assertEquals(0.5, 0.5 * fullThere + 0.5 * minThere, 1e-9,
                "the crossfade this format did NOT take would report half coverage here");
    }

    /** A null minimal shape is the ordinary path, whatever the blend says. */
    @Test
    void noMinimalShapeIgnoresTheBlend() {
        double[] pts = {0.2, 0.5, 0.8, 0.5};
        assertEquals(stroke(pts, false, false, 0.5, 0.5, 0.2),
                morph(pts, null, 0.0, false, 0.5, 0.5, 0.2), 1e-12);
    }

    /**
     * The morph is applied to the control points, so a <b>curved</b> path
     * morphs as a curve: the blend of two splines is the spline of the blended
     * points, because Catmull-Rom is linear in them. Checked rather than
     * assumed, since it is the reason the lerp can live inside the sampler.
     */
    @Test
    void aCurvedPathMorphsThroughItsControlPoints() {
        double[] full = {0.1, 0.4, 0.35, 0.8, 0.65, 0.2, 0.9, 0.6};
        double[] min = {0.4, 0.45, 0.47, 0.6, 0.55, 0.4, 0.6, 0.55};
        double t = 0.35;
        double[] blended = new double[full.length];
        for (int i = 0; i < full.length; i++) {
            blended[i] = min[i] + (full[i] - min[i]) * t;
        }
        for (double u = 0.05; u <= 0.95; u += 0.05) {
            for (double v = 0.2; v <= 0.8; v += 0.2) {
                assertEquals(stroke(blended, true, false, u, v, 0.15),
                        morph(full, min, t, true, u, v, 0.15), 1e-12,
                        "the morphed curve is not the curve through the morphed points at "
                                + u + "," + v);
            }
        }
    }
}
