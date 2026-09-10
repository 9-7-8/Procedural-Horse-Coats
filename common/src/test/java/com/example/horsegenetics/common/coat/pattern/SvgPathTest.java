package com.example.horsegenetics.common.coat.pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The <b>{@code SVG} mask's geometry</b> - {@link SvgPath}.
 *
 * <p>{@code PathMaskTest} says why a drawn shape needs testing at all: nothing
 * about it is a distribution to defend, and everything that can go wrong is
 * arithmetic that looks like a rendering artefact. This adds the half of that
 * which is <b>somebody else's format</b>, and so is not a matter of taste at
 * all - either the file draws what a browser would draw with it, or importing
 * one is a lie.
 *
 * <p>The cases below are therefore the parts of the grammar that are easy to
 * get subtly wrong and impossible to notice: the reflected control point of an
 * {@code S}, the arc's endpoint parameterisation, the two fill rules
 * disagreeing about a hole, the transform list's composition order, and
 * {@code preserveAspectRatio} leaving its slack on the right axis.
 */
class SvgPathTest {

    private static final double[] HIT = new double[3];

    private static double distance(SvgPath.Shape shape, double x, double y) {
        SvgPath.distance(shape, x, y, "round", 0, HIT);
        return HIT[0];
    }

    // ------------------------------------------------------------------
    // The grammar
    // ------------------------------------------------------------------

    /**
     * A square written four ways - absolute, relative, with the shorthand
     * {@code H}/{@code V}, and with the moveto's repeated coordinates read as
     * linetos - is the same square.
     *
     * <p>That last one is the rule nobody remembers: {@code "M 0 0 10 0"} is a
     * moveto and then a <i>lineto</i>, not two movetos, and a parser that gets
     * it wrong turns one outline into a string of separate subpaths that fill
     * to nothing.
     */
    @Test
    void theFourWaysToWriteASquareAgree() {
        String[] forms = {
                "M 0 0 L 10 0 L 10 10 L 0 10 Z",
                "m 0 0 l 10 0 l 0 10 l -10 0 z",
                "M 0 0 H 10 V 10 H 0 Z",
                "M 0 0 10 0 10 10 0 10 Z"
        };
        for (String d : forms) {
            SvgPath.Shape shape = SvgPath.parse(d, null);
            assertEquals(1, shape.subpaths(), d);
            assertEquals(4, shape.xs().length, d);
            assertTrue(SvgPath.inside(shape, 5, 5, false), d);
            assertFalse(SvgPath.inside(shape, 15, 5, false), d);
        }
    }

    /**
     * The number grammar is not Java's. {@code 10-5} is two numbers,
     * {@code .5.5} is two more, and an exponent is legal - all three come out
     * of real drawing programs, and each of them stops a naive tokeniser dead.
     */
    @Test
    void theNumberGrammarIsSvgsAndNotJavas() {
        SvgPath.Shape run = SvgPath.parse("M0 0L10-5L1e1 5Z", null);
        assertEquals(3, run.xs().length);
        assertEquals(10, run.xs()[1], 1e-9);
        assertEquals(-5, run.ys()[1], 1e-9);
        assertEquals(10, run.xs()[2], 1e-9);

        SvgPath.Shape dots = SvgPath.parse("M.5.5L2.5.5", null);
        assertEquals(0.5, dots.xs()[0], 1e-9);
        assertEquals(0.5, dots.ys()[0], 1e-9);
        assertEquals(2.5, dots.xs()[1], 1e-9);
        assertEquals(0.5, dots.ys()[1], 1e-9);
    }

    /**
     * {@code S} takes its first control point from the <b>reflection</b> of the
     * previous cubic's second one. Getting that wrong still draws a curve -
     * it just draws a kink at every join, which is exactly the thing an
     * {@code S} exists to prevent.
     */
    @Test
    void aSmoothCubicReflectsThePreviousControlPoint() {
        // Two spans that a reflection makes a straight line, and any other
        // reading makes a bulge. The second span's implied control point is
        // (20, 0) reflected through (10, 0), which is where the explicit form
        // below puts it.
        SvgPath.Shape smooth = SvgPath.parse("M 0 0 C 5 0 5 0 10 0 S 15 0 20 0", null);
        SvgPath.Shape explicit = SvgPath.parse("M 0 0 C 5 0 5 0 10 0 C 15 0 15 0 20 0", null);
        assertEquals(explicit.xs().length, smooth.xs().length);
        for (int i = 0; i < smooth.xs().length; i++) {
            assertEquals(explicit.ys()[i], smooth.ys()[i], 1e-9);
        }
        // And the whole of it really is flat, which is the point of the case.
        for (int i = 0; i < smooth.ys().length; i++) {
            assertEquals(0, smooth.ys()[i], 1e-9);
        }
    }

    /**
     * The elliptical arc lands on the endpoint it was given and bulges the way
     * its sweep flag says.
     *
     * <p>The endpoint parameterisation is the one piece of the format that is
     * genuinely a page of algebra, and its failure mode is not a crash: it is a
     * circle that closes a texel off, or a rounded corner cut the wrong way
     * round. Both are only visible against a case that names which way is which.
     */
    @Test
    void anArcReachesItsEndpointAndSweepsTheRightWay() {
        SvgPath.Shape sweep = SvgPath.parse("M 0 0 A 10 10 0 0 1 20 0", null);
        int last = sweep.xs().length - 1;
        assertEquals(20, sweep.xs()[last], 1e-6);
        assertEquals(0, sweep.ys()[last], 1e-6);
        // sweep=1 is the positive-angle direction, which on a y-DOWN page is
        // clockwise on screen - so left-to-right it goes over the top, which is
        // NEGATIVE y. Getting this backwards is the classic arc bug and looks
        // like a rounded corner cut the wrong way round.
        assertTrue(sweep.box()[1] < -5, "sweep=1 should bulge to -y, got " + sweep.box()[1]);
        assertTrue(sweep.box()[3] < 1e-6, "and should not cross to +y");

        SvgPath.Shape other = SvgPath.parse("M 0 0 A 10 10 0 0 0 20 0", null);
        assertTrue(other.box()[3] > 5, "sweep=0 should bulge to +y, got " + other.box()[3]);
    }

    /**
     * A radius too small to reach the endpoint is scaled up until it does,
     * rather than producing a NaN - the correction the specification's appendix
     * calls out, and the one an exported file hits whenever a shape was scaled
     * down after it was drawn.
     */
    @Test
    void anUnreachableArcRadiusIsScaledUpRatherThanFailing() {
        SvgPath.Shape shape = SvgPath.parse("M 0 0 A 1 1 0 0 1 20 0", null);
        for (int i = 0; i < shape.xs().length; i++) {
            assertTrue(Double.isFinite(shape.xs()[i]) && Double.isFinite(shape.ys()[i]),
                    "point " + i + " is not finite");
        }
        assertEquals(20, shape.xs()[shape.xs().length - 1], 1e-6);
    }

    // ------------------------------------------------------------------
    // Subpaths and the fill rule
    // ------------------------------------------------------------------

    /**
     * <b>The hole in a letter O is a fill rule, not a geometry.</b>
     *
     * <p>Two concentric squares wound the <i>same</i> way are one solid blob
     * under {@code nonzero} and a ring under {@code evenodd}; wound the other
     * way they are a ring under both. Which one the artist meant is written in
     * their file and cannot be recovered from the points, which is the whole
     * argument for the parameter existing.
     */
    @Test
    void theFillRuleIsWhatDecidesWhetherAHoleIsAHole() {
        String sameWinding = "M 0 0 H 30 V 30 H 0 Z M 10 10 H 20 V 20 H 10 Z";
        SvgPath.Shape shape = SvgPath.parse(sameWinding, null);
        assertEquals(2, shape.subpaths());
        assertTrue(SvgPath.inside(shape, 15, 15, false), "nonzero should fill the middle");
        assertFalse(SvgPath.inside(shape, 15, 15, true), "evenodd should punch it out");
        // Both agree about the ring itself and about the outside.
        assertTrue(SvgPath.inside(shape, 5, 15, false));
        assertTrue(SvgPath.inside(shape, 5, 15, true));
        assertFalse(SvgPath.inside(shape, 40, 15, false));
        assertFalse(SvgPath.inside(shape, 40, 15, true));

        String opposed = "M 0 0 H 30 V 30 H 0 Z M 10 10 V 20 H 20 V 10 Z";
        SvgPath.Shape ring = SvgPath.parse(opposed, null);
        assertFalse(SvgPath.inside(ring, 15, 15, false), "opposed winding is a hole under nonzero too");
    }

    /**
     * A {@code Z} that is not followed by a moveto starts the next subpath at
     * the point it returned to - the shape a chain of outlines written without
     * repeating the {@code M} has, and a case that silently loses a subpath if
     * the current point is not restored.
     */
    @Test
    void aClosepathReturnsToTheSubpathsStart() {
        SvgPath.Shape shape = SvgPath.parse("M 0 0 H 10 V 10 Z L -10 0 L -10 10 Z", null);
        assertEquals(2, shape.subpaths());
        assertTrue(SvgPath.inside(shape, 8, 6, false), "the first triangle survived");
        assertTrue(SvgPath.inside(shape, -7, 4, false), "and the second one started at 0,0");
    }

    // ------------------------------------------------------------------
    // Transforms
    // ------------------------------------------------------------------

    /**
     * The transform list composes <b>left to right</b>, the leftmost outermost
     * - so {@code "translate(10 0) scale(2)"} scales and then moves, and the
     * other order is a different picture entirely.
     */
    @Test
    void theTransformListComposesLeftToRight() {
        SvgPath.Shape shape = SvgPath.parse("M 0 0 H 5", "translate(10 0) scale(2)");
        assertEquals(10, shape.xs()[0], 1e-9);
        assertEquals(20, shape.xs()[1], 1e-9);

        SvgPath.Shape other = SvgPath.parse("M 0 0 H 5", "scale(2) translate(10 0)");
        assertEquals(20, other.xs()[0], 1e-9);
        assertEquals(30, other.xs()[1], 1e-9);
    }

    /** {@code rotate(a cx cy)} turns about the point it names, not about the origin. */
    @Test
    void aRotateAboutAPointTurnsAboutThatPoint() {
        SvgPath.Shape shape = SvgPath.parse("M 10 10 L 20 10", "rotate(90 10 10)");
        assertEquals(10, shape.xs()[0], 1e-9);
        assertEquals(10, shape.ys()[0], 1e-9);
        assertEquals(10, shape.xs()[1], 1e-9);
        assertEquals(20, shape.ys()[1], 1e-9);
    }

    // ------------------------------------------------------------------
    // Fitting
    // ------------------------------------------------------------------

    /**
     * {@code meet} fits the whole drawing in and leaves the slack on the axis
     * with room to spare; {@code slice} fills and overflows; {@code none}
     * stretches. All three are visible pictures and the file cannot say which
     * was meant, which is why it is a parameter.
     */
    @Test
    void thePreserveAspectRatioModesDoTheThreeDifferentThings() {
        double[] box = {0, 0, 100, 100};
        // A viewport twice as wide as it is tall.
        double[] meet = SvgPath.fit(box, 0, 0, 40, 20, "meet", "xMidYMid");
        assertEquals(0.2, meet[0], 1e-9);
        assertEquals(0.2, meet[1], 1e-9);
        assertEquals(10, meet[2], 1e-9, "the slack goes on x, centred");

        double[] slice = SvgPath.fit(box, 0, 0, 40, 20, "slice", "xMidYMid");
        assertEquals(0.4, slice[0], 1e-9);
        assertEquals(-10, slice[3], 1e-9, "and the overflow goes on y");

        double[] none = SvgPath.fit(box, 0, 0, 40, 20, "none", "xMidYMid");
        assertEquals(0.4, none[0], 1e-9);
        assertEquals(0.2, none[1], 1e-9);

        double[] min = SvgPath.fit(box, 0, 0, 40, 20, "meet", "xMinYMid");
        assertEquals(0, min[2], 1e-9, "xMin puts the drawing against the near edge");
    }

    // ------------------------------------------------------------------
    // Measuring
    // ------------------------------------------------------------------

    /** Distance to the outline is distance to the outline, inside it as well as out. */
    @Test
    void distanceIsMeasuredToTheOutlineFromEitherSide() {
        SvgPath.Shape square = SvgPath.parse("M 0 0 H 10 V 10 H 0 Z", null);
        assertEquals(0, distance(square, 0, 5), 1e-9);
        assertEquals(3, distance(square, -3, 5), 1e-9);
        assertEquals(4, distance(square, 4, 5), 1e-9);
    }

    /**
     * A {@code butt} cap stops square at the endpoint where a {@code round} one
     * carries on past it - the one visible difference between the two, and the
     * reason the parameter is copied off the file rather than assumed.
     */
    @Test
    void theCapDecidesWhatHappensPastAFreeEnd() {
        SvgPath.Shape line = SvgPath.parse("M 0 0 H 10", null);
        // Straight out past the end. A round cap is a disc, so the distance is
        // the plain one; a flat cap crosses 'half' at its own plane, so it reads
        // half a width further; a square one gets that half back.
        SvgPath.distance(line, -2, 0, "round", 0.5, HIT);
        assertEquals(2, HIT[0], 1e-9);
        SvgPath.distance(line, -2, 0, "butt", 0.5, HIT);
        assertEquals(2.5, HIT[0], 1e-9);
        SvgPath.distance(line, -2, 0, "square", 0.5, HIT);
        assertEquals(2, HIT[0], 1e-9);

        // And off the corner, where the round cap curves and the flat ones do
        // not - the only place the three are visibly different shapes.
        SvgPath.distance(line, -2, 3, "round", 0.5, HIT);
        assertEquals(Math.sqrt(13), HIT[0], 1e-9);
        SvgPath.distance(line, -2, 3, "butt", 0.5, HIT);
        assertEquals(3, HIT[0], 1e-9, "sideways of a flat cap only the offset counts");

        // Just inside the end: covered by every cap but the butt.
        SvgPath.distance(line, -0.2, 0, "round", 0.5, HIT);
        assertTrue(HIT[0] < 0.5, "a round cap reaches past the endpoint");
        SvgPath.distance(line, -0.2, 0, "butt", 0.5, HIT);
        assertTrue(HIT[0] > 0.5, "a butt cap stops at it");
        SvgPath.distance(line, -0.2, 0, "square", 0.5, HIT);
        assertTrue(HIT[0] < 0.5, "and a square cap reaches half a width past it");
    }

    /**
     * Arc length is reported along the subpath, which is what a dash pattern is
     * measured with - and it has to be the length of the <i>flattened</i> path,
     * or a dashed curve drifts out of step with a dashed straight line.
     */
    @Test
    void theArcLengthComesBackWithTheDistance() {
        SvgPath.Shape line = SvgPath.parse("M 0 0 H 10 V 10", null);
        SvgPath.distance(line, 4, 0, "round", 0, HIT);
        assertEquals(4, HIT[1], 1e-9);
        SvgPath.distance(line, 10, 7, "round", 0, HIT);
        assertEquals(17, HIT[1], 1e-9);
    }

    // ------------------------------------------------------------------
    // Refusals
    // ------------------------------------------------------------------

    /**
     * The three ways a pasted string goes wrong each name themselves, because
     * all three otherwise produce a horse with no marking on it and nothing to
     * read.
     */
    @Test
    void aMalformedPathSaysWhatIsWrongWithIt() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> SvgPath.parse("L 10 10", null)).getMessage().contains("moveto"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> SvgPath.parse("M 0 0 X 5 5", null)).getMessage().contains("number"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> SvgPath.parse("M 0 0 A 5 5 0 2 1 10 0", null)).getMessage().contains("flags"));

        // And the cost ceiling, which is the one that would otherwise be a
        // game that takes a minute to make a horse rather than an error.
        StringBuilder huge = new StringBuilder("M 0 0");
        for (int i = 0; i < SvgPath.MAX_POINTS + 10; i++) {
            huge.append(" L ").append(i).append(" 1");
        }
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> SvgPath.parse(huge.toString(), null)).getMessage().contains("ceiling"));
    }
}
