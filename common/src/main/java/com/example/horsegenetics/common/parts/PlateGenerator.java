package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Shoulder and hip plates</b> - overlapping slabs stepped down the flank like the
 * lames of a pauldron, a cluster over the shoulder and a cluster over the hip, on one
 * side of the horse - grown from two integers. One mesh per side, both clusters in it.
 *
 * <h2>Slabs hang, and each one steps out over the next</h2>
 * A slab is a flat box (thin across the horse, {@link PartNode#width}) turned upside
 * down, so it hangs from its top edge, and leaned out at the bottom by the flare. The
 * next slab down starts {@code drop} lower, still flush with the body, so the one above
 * stands proud of it - armour scales, the upper lapping the lower. How far they lap is
 * the {@code overlap} bucket; how far the bottoms flare is the {@code spikiness} one.
 *
 * <h2>One group per step, both clusters in it, numbered top down</h2>
 * Slab {@code j} of the shoulder and slab {@code j} of the hip are both group
 * {@code j}, so the count ({@code AttachedPart#shown}) is per cluster: a horse showing
 * three has three at the shoulder and three at the hip, and a fractional count grows
 * the fourth in on both. Top down (owner, 2026-10-05): a low count is a pauldron at
 * the top of the shoulder, and more step down the flank. A group therefore has two
 * roots, and each is scaled about its own top edge
 * ({@link PartKind#scalesPerElement()}), so the clusters stay where they are on the
 * body however big their slabs are.
 *
 * <h2>The three forms, and the edge</h2>
 * Every slab carries a lip along its bottom edge. A {@link #RIDGED} slab adds a rib
 * along its middle, and a {@link #SPIKED} one a spike standing out from it. The lip,
 * rib and spike sample {@link PartSheet#BONE_TIP} and are a slab's last boxes, so a
 * two-tone horse wears its tip colour on them and its base colour on the slab (owner,
 * 2026-10-05). Spikiness also sets how tall the rib and how long the spike.
 *
 * <p>Angles follow {@link PartNode}: {@code -y} up, {@code -z} forward. The right side
 * is model {@code -x}; the left is the right mirrored, box for box ({@code ox} and
 * {@code rz} negated), never a negative scale. <b>Geometry only - never seen in a
 * running game</b> when written.
 */
public final class PlateGenerator {

    /** Plain slabs, with only the lip. */
    public static final int SMOOTH = 0;
    /** Slabs with a rib along the middle. */
    public static final int RIDGED = 1;
    /** Slabs with a spike standing out of each. */
    public static final int SPIKED = 2;

    /** How many forms - the {@code form} category on the copy. */
    public static final int FORMS = 3;
    /** Overlap buckets: how far each slab laps the one below. */
    public static final int OVERLAPS = 3;
    /** Spikiness buckets: how far the slabs flare, and how big the rib or spike. */
    public static final int SPIKES = 3;

    /**
     * Slabs in a full cluster - the most a horse can show per cluster. The treatment's
     * three to five; the wild range is that, and a line can be bred down to one.
     */
    public static final int MAX_PLATES = 5;

    /** The shoulder cluster's centre, along the body from the anchor, in model units. */
    public static final float SHOULDER_Z = -7.5f;
    /** The hip cluster's centre. */
    public static final float HIP_Z = 9.2f;

    /**
     * The most a cluster may reach down the flank from the anchor - the body is ten
     * units deep and the anchor sits under a unit below its top. A cluster that would
     * stack further steps closer instead, so a big slab laps more rather than hanging
     * off the belly.
     */
    static final float ROOM = 9.0f;

    /** A slab's length along the body, as a share of its height down the flank. */
    static final float ASPECT = 1.3f;
    /** Share of a slab's height the next one down starts below its top, by overlap bucket. */
    private static final float[] DROP = {0.80f, 0.65f, 0.50f};
    /** How far a slab's bottom leans out from the body, radians, by spikiness bucket. */
    private static final float[] FLARE = {0.12f, 0.22f, 0.32f};
    /** How big a rib or spike stands, by spikiness bucket. */
    private static final float[] FEATURE = {0.6f, 1.0f, 1.5f};
    /** How far a slab's inner face sinks into the body, so a flared slab shows no gap at its top. */
    private static final float EMBED = 0.1f;
    /** Where a spike's point starts along its base box - a little short of the end, as the spines' JOIN. */
    private static final float JOIN = 0.93f;
    /** The thinnest edge any box may have ({@code PartGeneratorTest.MIN_GIRTH}). */
    private static final float MIN_GIRTH = 0.5f;
    private static final float HALF_TURN = (float) Math.PI;
    private static final float QUARTER_TURN = (float) (Math.PI / 2);

    private PlateGenerator() {
    }

    /** The style index of a form, overlap bucket and spikiness bucket - each clamped. */
    public static int style(int form, int overlap, int spikiness) {
        int f = Math.max(0, Math.min(FORMS - 1, form));
        int o = Math.max(0, Math.min(OVERLAPS - 1, overlap));
        int s = Math.max(0, Math.min(SPIKES - 1, spikiness));
        return (f * OVERLAPS + o) * SPIKES + s;
    }

    /** How many styles - every form, overlap and spikiness. */
    public static int styles() {
        return FORMS * OVERLAPS * SPIKES;
    }

    /** The form a style index was built from. */
    public static int formOf(int style) {
        return style / (OVERLAPS * SPIKES);
    }

    /** The overlap bucket a style index was built from. */
    public static int overlapOf(int style) {
        return style / SPIKES % OVERLAPS;
    }

    /** The spikiness bucket a style index was built from. */
    public static int spikinessOf(int style) {
        return style % SPIKES;
    }

    /**
     * How far below the top of a cluster's first slab slab {@code j} starts, for a slab
     * {@code height} tall in {@code overlap} bucket - stepped closer when the cluster
     * would otherwise reach past {@link #ROOM}.
     */
    static float dropFor(int overlap, float height) {
        float drop = height * DROP[Math.max(0, Math.min(OVERLAPS - 1, overlap))];
        return Math.min(drop, (ROOM - height) / (MAX_PLATES - 1));
    }

    /**
     * One side's plates.
     *
     * @param style     a {@link #style} index; clamped
     * @param sizeClass a {@link PlateSize} ordinal; clamped
     * @param left      the left side ({@code +x}) rather than the right
     */
    public static List<PartNode> generate(int style, int sizeClass, boolean left) {
        int st = Math.max(0, Math.min(styles() - 1, style));
        int form = formOf(st);
        int spikes = spikinessOf(st);
        int c = Math.max(0, Math.min(PlateSize.classes() - 1, sizeClass));
        float height = PlateSize.values()[c].length();
        float drop = dropFor(overlapOf(st), height);
        float flare = FLARE[spikes];
        float feature = FEATURE[spikes];
        // Out is the world side the plates face; a hung slab's own x runs the other way
        // (it is turned half round z), so its outer face is -out in its own frame.
        float out = left ? 1f : -1f;
        List<PartNode> nodes = new ArrayList<>(MAX_PLATES * 2 * 4);
        for (int j = 0; j < MAX_PLATES; j++) {
            slab(nodes, j, SHOULDER_Z, j * drop, height, form, flare, feature, out);
            slab(nodes, j, HIP_Z, j * drop, height, form, flare, feature, out);
        }
        return nodes;
    }

    /** One slab in group {@code group}, its top edge {@code y} below the anchor, and its edge boxes. */
    private static void slab(List<PartNode> out, int group, float z, float y, float height,
                             int form, float flare, float feature, float side) {
        float length = height * ASPECT;
        float width = Math.max(0.4f, Math.min(0.7f, 0.15f * height));
        float faceOut = -side;
        int me = out.size();
        // Turned half round z, so it hangs down from its top edge, and leaned out at the
        // bottom by the flare. The left side is the right with ox and rz negated.
        out.add(new PartNode(-1, 0f, side * (width / 2f - EMBED), y, z,
                0f, 0f, -side * (HALF_TURN + flare), height, length, PartSheet.BONE, group, width));
        // The lip: a bar along the bottom edge, on the outer face, running front to back.
        float lip = MIN_GIRTH;
        out.add(new PartNode(me, 1f - 0.5f * lip / height, faceOut * (width / 2f + lip / 2f - 0.05f), 0f,
                0.45f * length, QUARTER_TURN, 0f, 0f, 0.9f * length, lip, PartSheet.BONE_TIP, group));
        if (form == RIDGED) {
            float rib = Math.max(MIN_GIRTH, 0.55f * feature);
            out.add(new PartNode(me, 0.45f, faceOut * (width / 2f + rib / 2f - 0.05f), 0f,
                    0.45f * length, QUARTER_TURN, 0f, 0f, 0.9f * length, rib, PartSheet.BONE_TIP, group));
        } else if (form == SPIKED) {
            float reach = height * 0.55f * feature;
            float base = Math.max(MIN_GIRTH, Math.min(1.2f, 0.4f + 0.5f * feature));
            // Two boxes, the point narrower than the base - both hung off the slab rather
            // than the point off the base, so each is a last box of the slab's branch and
            // a two-tone horse draws the whole spike in the tip colour (PartModel.along).
            float root = width / 2f - 0.05f;
            float first = 0.55f * reach;
            out.add(new PartNode(me, 0.45f, faceOut * root, 0f, 0f,
                    0f, 0f, faceOut * QUARTER_TURN, first, base, PartSheet.BONE_TIP, group));
            out.add(new PartNode(me, 0.45f, faceOut * (root + JOIN * first), 0f, 0f,
                    0f, 0f, faceOut * QUARTER_TURN, 0.45f * reach, Math.max(MIN_GIRTH, base * 0.55f),
                    PartSheet.BONE_TIP, group));
        }
    }
}
