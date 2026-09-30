package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A unicorn horn: a tapering chain of boxes.</b> Each segment is a child of
 * the one below it, so the spiral and the curve fall out of the parenting and
 * there is no trigonometry in here at all.
 *
 * <h2>Why the twist and the curve are never on the same segment</h2>
 * This is the one thing about the shape that is not obvious, and getting it
 * wrong produces a horn that coils like a spring.
 *
 * <p>Vanilla composes a part's rotation as {@code rotationZYX}, so a segment is
 * turned about <b>x first</b> and then about the <b>parent's</b> y. On a segment
 * with no bend those two are the same axis as the horn's own, and {@code ry} is a
 * clean roll - a square section rolled a little per segment reads as a twisted
 * horn, because the corners trace a helix down the length. On a segment that is
 * <i>also</i> bent, {@code ry} sweeps the bend sideways instead, and fifteen
 * segments of that is a corkscrew whose tip points somewhere behind the horse's
 * ear.
 *
 * <p>So the four styles split cleanly: {@link #SMOOTH} is the only one that
 * bends, and the three twisted ones are dead straight. That is not a compromise
 * - a straight tapered spiral is the storybook horn and a narwhal tusk both, and
 * the gentle scimitar curve is what an untwisted horn needs to stop reading as a
 * spike.
 *
 * <h2>Base width grows with length, but not proportionally</h2>
 * A horn scaled uniformly looks like a photograph enlarged rather than a
 * different animal. A narwhal horn is thicker at the root than a nub is, and
 * far more slender for its length.
 */
public final class HornGenerator {

    /** Smooth and gently curved forward. The only style that bends. */
    public static final int SMOOTH = 0;
    /** A lazy quarter-turn over the whole horn. */
    public static final int LOOSE = 1;
    /** The classic spiral. */
    public static final int SPIRAL = 2;
    /** As tight as a square section can go before the segments gap - see {@link #TWIST_DEGREES}. */
    public static final int TIGHT = 3;

    /** How many distinct shapes this generator builds. One epigenetic category. */
    public static final int STYLES = 4;

    /**
     * Roll added per segment, in degrees, by style.
     *
     * <p>{@link #TIGHT} stops at 34 rather than going further because a square
     * section rolled much more than that shows daylight between one segment's
     * face and the next's. Tightening the look past this point is the ridge-box
     * trick (a thin box on one corner of each segment), not a bigger number -
     * and that is a shape change, so it would be a fifth style rather than a
     * tweak to this one.
     */
    private static final float[] TWIST_DEGREES = {0f, 12f, 22f, 34f};

    /**
     * Forward bend per segment, in degrees, for {@link #SMOOTH} only.
     *
     * <p>Three degrees over up to fifteen segments is up to 45 degrees of total
     * sweep, which on a long horn is a scimitar and on a nub is invisible. That
     * scaling is wanted: a long horn has room to curve and a stub does not.
     */
    private static final float CURVE_DEGREES = 3f;

    /** Model units of horn per segment. Segment count follows from the length. */
    private static final float SEGMENT_LENGTH = 1.6f;

    /**
     * Fewest and most segments. The floor keeps a nub from being a single stubby
     * box with no taper; the ceiling is what bounds the cost of the part - a
     * horn can never be more boxes than this, however a drifted number arrives.
     */
    private static final int MIN_SEGMENTS = 3;
    /** @see #MIN_SEGMENTS */
    public static final int MAX_SEGMENTS = 15;

    /**
     * Thinnest box this will emit, in model units. Below about half a unit a box
     * is under a texel wide on the sheet and shimmers as the camera moves.
     */
    private static final float MIN_GIRTH = 0.6f;

    /**
     * Where along its parent a segment sits. Not 1.0: butting a tapered child
     * exactly onto its parent's tip leaves a visible step, and on a bent chain a
     * wedge of daylight on the outside of the bend. A little overlap hides both
     * and costs nothing.
     */
    private static final float JOIN = 0.94f;

    private HornGenerator() {
    }

    /**
     * @param length model units from the forehead to the point
     * @param style  one of {@link #SMOOTH}, {@link #LOOSE}, {@link #SPIRAL},
     *               {@link #TIGHT}; anything else is clamped into range
     */
    public static List<PartNode> generate(float length, int style) {
        int shape = style < 0 ? 0 : (style >= STYLES ? STYLES - 1 : style);
        float reach = Math.max(0.5f, length);
        float base = 1.3f + 0.055f * reach;
        int segments = Math.max(MIN_SEGMENTS,
                Math.min(MAX_SEGMENTS, Math.round(reach / SEGMENT_LENGTH)));
        float segment = reach / segments;

        float roll = (float) Math.toRadians(TWIST_DEGREES[shape]);
        float bend = shape == SMOOTH ? (float) Math.toRadians(CURVE_DEGREES) : 0f;

        List<PartNode> nodes = new ArrayList<>(segments);
        for (int i = 0; i < segments; i++) {
            // Linear taper to a point. The tip is MIN_GIRTH rather than zero: a
            // box cannot come to a point, and a horn that ends in a flat 0.6-unit
            // square is what every blocky horn in the game looks like anyway.
            float along = (float) i / segments;
            float girth = Math.max(MIN_GIRTH, base * (1.0f - 0.9f * along));
            int region = i >= segments - 2 ? PartSheet.HORN_TIP : PartSheet.HORN;
            if (i == 0) {
                // The root carries no rotation of its own: the forward lean of
                // the whole horn is a per-horse number the client applies to the
                // anchor, so two horses can share this baked shape and wear it at
                // different angles.
                nodes.add(new PartNode(-1, 0f, 0f, 0f, 0f, 0f, 0f, 0f, segment, girth, region));
            } else {
                nodes.add(new PartNode(i - 1, JOIN, 0f, 0f, 0f, bend, roll, 0f, segment, girth, region));
            }
        }
        return nodes;
    }
}
