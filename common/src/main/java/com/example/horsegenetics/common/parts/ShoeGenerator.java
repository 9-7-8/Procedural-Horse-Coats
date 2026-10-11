package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A horse shoe: a U of three bars under the foot, open at the heel</b>
 * ({@link PartKind#SHOE}).
 *
 * <p>The owner's shape (2026-10-11): a shoe is seen on the <b>underside</b> of the
 * foot and shows only the barest edge on the hoof wall; it runs round the front and
 * the two sides and not across the back; and it is a U underneath as well, so the
 * middle of the sole, the frog, shows inside it.
 *
 * <h2>Where it sits</h2>
 * The anchor is the middle of the sole (a hoof anchor), front {@code -z}, down
 * {@code +y}. A leg is {@link WornBlockGenerator#HOOF} units square. Two bars
 * {@link #BAR} wide run front to back under the two side walls and a third joins them
 * across the toe. Their outer edge stands {@link #PROUD} outside the wall, and they
 * reach {@link #PROUD} under the sole and {@link #RIM} up the wall: that rim is all
 * that shows on a standing horse. The side bars stop {@link #HEEL} short of the back
 * wall, so nothing of the shoe lies in the plane of the heel.
 *
 * <h2>One tree</h2>
 * The near side bar is the root; the other side bar and the toe bar hang on it at its
 * base. The toe bar lies across the horse, so it is turned a quarter about {@code y}:
 * a box is never wider across {@code x} than it is deep.
 */
public final class ShoeGenerator {

    /** How wide each bar of the U is, seen from under the foot. */
    public static final float BAR = 1f;

    /** How far the shoe stands outside the hoof wall and under the sole: the worn floor. */
    public static final float PROUD = 0.25f;

    /** How far up the hoof wall the shoe's edge reaches. The barest bit. */
    public static final float RIM = 0.25f;

    /** How far short of the back wall the side bars stop. */
    public static final float HEEL = 0.25f;

    private static final float QUARTER_TURN = (float) (Math.PI / 2);

    private ShoeGenerator() {
    }

    /** How thick the shoe is, top to bottom. */
    public static float thickness() {
        return PROUD + RIM;
    }

    /** The boxes: the near side bar, the off side bar, the toe bar. */
    public static List<PartNode> generate() {
        float half = WornBlockGenerator.HOOF / 2f;
        float outer = half + PROUD;
        // A side bar runs from the shoe's front edge back to just short of the heel.
        float front = -outer;
        float back = half - HEEL;
        float length = back - front;
        float middle = (front + back) / 2f;
        float side = outer - BAR / 2f;
        // The toe bar fills the gap between the side bars, along the front edge.
        float gap = 2f * (outer - BAR);
        float toe = front + BAR / 2f;
        List<PartNode> out = new ArrayList<>(3);
        out.add(new PartNode(-1, 0f, -side, PROUD, middle, 0f, 0f, 0f,
                thickness(), length, PartSheet.BONE, PartNode.NO_GROUP, BAR));
        out.add(new PartNode(0, 0f, 2f * side, 0f, 0f, 0f, 0f, 0f,
                thickness(), length, PartSheet.BONE, PartNode.NO_GROUP, BAR));
        out.add(new PartNode(0, 0f, side, 0f, toe - middle, 0f, QUARTER_TURN, 0f,
                thickness(), gap, PartSheet.BONE, PartNode.NO_GROUP, BAR));
        return out;
    }
}
