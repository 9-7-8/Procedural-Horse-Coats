package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The debug block: a plain box round a horse's foot</b> ({@link PartKind#TEST_BLOCK}).
 *
 * <p>It exists to prove the worn side of the attached parts - that boxes hung on a
 * hoof anchor ride the leg through the stride and the rear - before horse shoes
 * arrive as items onto drawing that already works. It is the box-built twin of the
 * debug boot ({@code common/gear/WornPieces.TEST_BOOT_FORE}), which proved the same of
 * a lifted piece.
 *
 * <h2>Where it sits</h2>
 * The anchor is the middle of the sole ({@link PartAnchor#HOOF_FRONT_LEFT} and the
 * other three). A leg is {@link #HOOF} units square; the block is that plus
 * {@link #PROUD} on every side it covers, the foot's four walls and the sole, and
 * reaches {@link #HEIGHT} up the leg from the sole. So it is a worn piece by the worn
 * rule, standing off the horse rather than coincident with it:
 * {@code WornPartTest} holds {@link #PROUD} to the row's lift and the floor.
 *
 * <h2>Why two boxes and not one</h2>
 * A cube's UV rectangle is its own unwrap, {@code 2 * (width + girth)} texels wide,
 * laid down at the corner of its sheet region ({@link PartSheet}: sixteen texels).
 * One box the full {@link #side()} across would unwrap eighteen wide and read two
 * texels of the next region. So the block is two halves side by side, each a flat
 * box, the second hung on the first so the part stays one tree.
 */
public final class WornBlockGenerator {

    /** A leg's cross-section, in model units: every leg box of both horse models is four square. */
    public static final float HOOF = 4f;

    /** How far the block stands off the foot, on its walls and under the sole. */
    public static final float PROUD = 0.25f;

    /** How far up the leg the block reaches from the sole. */
    public static final float HEIGHT = 2f;

    private WornBlockGenerator() {
    }

    /** The block's width and depth: the foot, and {@link #PROUD} either side of it. */
    public static float side() {
        return HOOF + 2f * PROUD;
    }

    /** The boxes: two halves, the first rooted on the anchor. */
    public static List<PartNode> generate() {
        float side = side();
        float half = side / 2f;
        // From PROUD under the sole (y is down) up to HEIGHT above it.
        float len = HEIGHT + PROUD;
        List<PartNode> out = new ArrayList<>(2);
        out.add(new PartNode(-1, 0f, -half / 2f, PROUD, 0f, 0f, 0f, 0f,
                len, side, PartSheet.BONE, PartNode.NO_GROUP, half));
        // At the first half's own base, one half-width across.
        out.add(new PartNode(0, 0f, half, 0f, 0f, 0f, 0f, 0f,
                len, side, PartSheet.BONE, PartNode.NO_GROUP, half));
        return out;
    }
}
