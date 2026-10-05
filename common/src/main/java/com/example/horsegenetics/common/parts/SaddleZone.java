package com.example.horsegenetics.common.parts;

import java.util.List;

/**
 * <b>Which parts of a back part a saddle hides.</b> A row along the spine runs through
 * where the saddle and the rider sit. Unsaddled, it runs its full length; with a
 * saddle on, only the elements inside the saddle zone are hidden and the rest remain
 * (owner, body-parts treatment).
 *
 * <p>Two questions, both answered here so a test can hold them without a game:
 * <ul>
 *   <li><b>When</b> - {@link #covers}: a saddle is <i>drawn</i>, or the horse is
 *       <i>ridden</i>. Not "a saddle item is present": a phantom saddle (how a
 *       top-bond horse is steered bare) is present and not drawn, and its rider
 *       still sits on the back, so keying on the item alone would put a rider
 *       inside the spines - and would hide them on a bare horse nobody is riding.</li>
 *   <li><b>Which</b> - {@link #groupsWithin}: the element groups whose root falls in
 *       a range along the row. The range is the client's, read off the vanilla
 *       saddle model; this class knows no bone and no box.</li>
 * </ul>
 * A foal wears no body part at all ({@link PartKind#showsOnFoal()}), so it never
 * reaches this.
 */
public final class SaddleZone {

    /** Groups a mask can name. Higher groups are never hidden. */
    public static final int MASK_BITS = Integer.SIZE;

    private SaddleZone() {
    }

    /** Does the saddle zone apply - is a saddle drawn, or is somebody riding? */
    public static boolean covers(boolean saddleDrawn, boolean ridden) {
        return saddleDrawn || ridden;
    }

    /**
     * The groups whose root lies within {@code [from, to]} along {@code z}, in the
     * anchor's own units, as a bit mask (bit {@code g} for group {@code g}).
     *
     * <p>A group's root is the first box of it, the one whose parent is outside the
     * group; its position is taken as its chain's root offset ({@link PartNode#oz()}),
     * which is exact for a row whose every element is a root of its own (the dorsal
     * spines) and the nearest anchored point for anything else.
     */
    public static int groupsWithin(List<PartNode> nodes, float from, float to) {
        int mask = 0;
        for (int i = 0; i < nodes.size(); i++) {
            PartNode node = nodes.get(i);
            int g = node.group();
            if (g == PartNode.NO_GROUP || g >= MASK_BITS) {
                continue;
            }
            if (!node.isRoot() && nodes.get(node.parent()).group() == g) {
                continue;
            }
            int r = i;
            while (!nodes.get(r).isRoot()) {
                r = nodes.get(r).parent();
            }
            float z = nodes.get(r).oz();
            if (z >= from && z <= to) {
                mask |= 1 << g;
            }
        }
        return mask;
    }

    /**
     * The same for a row drawn {@code span} times its baked length along {@code z} - a
     * sail that covers less of the back. Its elements then root nearer the anchor, so a
     * different run of them is under the saddle. The zone is divided by the span, back
     * into the mesh's own units, rather than every root multiplied. A span that is not a
     * positive number is treated as {@code 1}.
     */
    public static int groupsWithin(List<PartNode> nodes, float from, float to, float span) {
        float s = span > 0f ? span : 1f;
        return groupsWithin(nodes, from / s, to / s);
    }

    /** Is group {@code g} in {@code mask}? */
    public static boolean hides(int mask, int g) {
        return g >= 0 && g < MASK_BITS && (mask >>> g & 1) != 0;
    }
}
