package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Cheek spikes</b> - a short row of cones along the cheek, pointing out from the
 * head and a little back - grown from one integer. One mesh per side, and one style:
 * everything that differs between two horses' spikes is a length, a thickness and a
 * count, and all three are applied at draw time.
 *
 * <h2>A row, so each spike scales about its own root</h2>
 * The spikes point sideways, across the anchor's own up, so a whole-part stretch would
 * move them apart instead of lengthening them. Every box is in a group and each spike
 * is one tree rooted on the anchor ({@link PartKind#scalesPerElement()}): the length and
 * girth land on each spike in its own frame, and the row keeps its place on the cheek.
 *
 * <h2>Numbered from the back</h2>
 * Spike {@code 0} is the rearmost and the longest, and each one forward of it is
 * shorter - a bearded dragon's row. So "the first {@code k}" is always the big end of
 * the row, and a count bred up grows the next one in toward the mouth.
 *
 * <p>Each spike is a base box and a narrower point; the point samples
 * {@link PartSheet#BONE_TIP}, so a two-tone horse wears its tip colour there. Angles
 * follow {@link PartNode}: {@code -y} up, {@code -z} forward. The right side is model
 * {@code -x}; the left is the right with each spike's lean negated, never a negative
 * scale. <b>Geometry only - never seen in a running game</b> when written.
 */
public final class CheekSpikeGenerator {

    /**
     * Spikes in a full row - the most a horse can show per side. The treatment's two to
     * four; the wild range is that, and a line can be bred down to one.
     */
    public static final int MAX_SPIKES = 4;

    /** How far apart two spikes root along the cheek, in model units. */
    public static final float SPACING = 1.3f;

    /** How far each spike sweeps back from straight out, in radians. */
    static final float BACK = -0.30f;
    /** Each spike's length as a share of the longest, rear to front. */
    private static final float[] SHARE = {1.0f, 0.85f, 0.70f, 0.55f};

    private static final float MIN_GIRTH = 0.5f;
    private static final float JOIN = 0.93f;
    private static final float QUARTER_TURN = (float) (Math.PI / 2);

    private CheekSpikeGenerator() {
    }

    /** How many styles - one. */
    public static int styles() {
        return 1;
    }

    /** Where spike {@code j} roots along the cheek from the anchor; {@code +z} is back. */
    public static float rootZ(int j) {
        return SPACING * ((MAX_SPIKES - 1) / 2f - j);
    }

    /**
     * One side's row.
     *
     * @param sizeClass a {@link CheekSpikeSize} ordinal; clamped
     * @param left      the left side ({@code +x}) rather than the right
     */
    public static List<PartNode> generate(int sizeClass, boolean left) {
        int c = Math.max(0, Math.min(CheekSpikeSize.classes() - 1, sizeClass));
        float longest = CheekSpikeSize.values()[c].length();
        float side = left ? 1f : -1f;
        List<PartNode> out = new ArrayList<>(MAX_SPIKES * 2);
        for (int j = 0; j < MAX_SPIKES; j++) {
            float length = longest * SHARE[j];
            float base = Math.max(MIN_GIRTH, Math.min(1.3f, 0.3f + 0.45f * length));
            int me = out.size();
            // Swept back about x first, then laid over a quarter turn to point out.
            out.add(new PartNode(-1, 0f, 0f, 0f, rootZ(j), BACK, 0f, side * QUARTER_TURN,
                    0.6f * length, base, PartSheet.BONE, j));
            out.add(new PartNode(me, JOIN, 0f, 0f, 0f, 0f, 0f, 0f, 0.45f * length,
                    Math.max(MIN_GIRTH, 0.55f * base), PartSheet.BONE_TIP, j));
        }
        return out;
    }
}
