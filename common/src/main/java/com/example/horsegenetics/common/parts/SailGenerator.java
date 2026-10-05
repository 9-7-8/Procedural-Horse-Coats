package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A back sail</b> - a row of upright fin-ray spines along the back with a membrane
 * stretched between each pair of neighbours - grown from two integers.
 *
 * <h2>The dorsal spine row, with skin between</h2>
 * The sail stands in the same row {@link DorsalSpineGenerator} does: {@link #MAX_SPINES}
 * spines rooted at the {@link PartAnchor#SPINE} anchor plus
 * {@link DorsalSpineGenerator#rootZ}, each its own chain and its own
 * {@link PartNode#group}, numbered withers to croup. So the count and the saddle zone
 * work on a sail exactly as they do on a spine row, with nothing added.
 *
 * <h2>Each panel belongs to the spine behind it</h2>
 * The membrane between spine {@code i - 1} and spine {@code i} is {@link #COLUMNS}
 * flat boxes hung off spine {@code i}'s root, in group {@code i}; spine {@code 0} has
 * none. That is what keeps every panel spanned: a horse showing four spines shows the
 * three panels between them and never one reaching back to a spine it does not have,
 * and a spine the saddle hides takes the panel in front of it along. The columns are
 * the spine's children, so its per-element height scale reaches them too and the
 * membrane always stands as tall as its spines.
 *
 * <h2>Upright, so a panel lines up with its neighbour</h2>
 * Unlike a dorsal spine, a fin ray does not rake. A panel is built in the frame of the
 * spine behind it; if both spines leaned, the panel would lean with one and miss the
 * other. The sail's shape is in its heights instead.
 *
 * <h2>Membrane is its own region, and the only see-through one</h2>
 * Spines sample {@link PartSheet#BONE} and {@link PartSheet#BONE_TIP} and are drawn
 * solid; every membrane box samples {@link PartSheet#MEMBRANE}, the region
 * {@link PartKind#translucentRegions()} names for a sail, so the blended pass draws only
 * the membrane. A membrane box is flat ({@link PartNode#width}): thin across the horse,
 * a third of a gap long along it.
 *
 * <h2>Three forms, three curves</h2>
 * <ul>
 *   <li>{@link #TALL} - a Spinosaurus sail: highest over the middle of the back,
 *       falling toward the withers and the croup;</li>
 *   <li>{@link #LOW} - a ridge fin the full length of the back, under half as tall;</li>
 *   <li>{@link #SCALLOPED} - the tall profile, but each panel sags in an arch between
 *       its two spines rather than running straight from tip to tip.</li>
 * </ul>
 * The curve bucket is how strongly the profile arches: how far the ends fall below the
 * peak. Form and curve change box sizes, so they are topology and live in the style;
 * height is the per-spine scale, coverage a scale along the row, opacity and colour
 * draw-time tints. Angles follow {@link PartNode}: {@code -y} up, {@code -z} forward.
 * <b>Geometry only - never seen in a running game</b> when written.
 */
public final class SailGenerator {

    /** Highest over the middle of the back. */
    public static final int TALL = 0;
    /** A ridge fin the full length of the back. */
    public static final int LOW = 1;
    /** The tall profile with each panel sagging between its spines. */
    public static final int SCALLOPED = 2;

    /** How many forms - the {@code form} category on the copy. */
    public static final int FORMS = 3;
    /** Curve buckets: how strongly the profile arches. */
    public static final int CURVES = 3;

    /** Spines in a sail - the dorsal row's, so the saddle zone and count read it the same way. */
    public static final int MAX_SPINES = DorsalSpineGenerator.MAX_SPINES;

    /** Flat boxes per panel. Three, so a sagging panel has a lower middle. */
    public static final int COLUMNS = 3;

    /** How thick the membrane is across the horse, model units. */
    public static final float MEMBRANE_THICKNESS = 0.3f;

    /** How far up its spines a panel reaches - just under the tips, so a point shows. */
    private static final float MEMBRANE_REACH = 0.92f;
    /** How deep a scalloped panel sags at its middle, as a share of its height there. */
    private static final float SCALLOP_DIP = 0.35f;
    /** Where along the row a tall sail peaks, withers {@code 0} to croup {@code 1}. */
    private static final float PEAK = 0.45f;
    /** How far the ends fall below the peak, by curve bucket - the arch. */
    private static final float[] ARCH = {0.35f, 0.60f, 0.85f};
    /** A low sail's height against a tall one's of the same class. */
    private static final float LOW_SHARE = 0.45f;
    /** No spine is shorter than this share of the class height, however hard the arch. */
    private static final float FLOOR = 0.12f;
    /** Boxes per spine, by size class - a taller spine needs more to taper smoothly. */
    private static final int[] SEGMENTS = {2, 2, 3, 3};
    /** How much narrower a spine's last box is than its first. */
    private static final float TAPER = 0.55f;

    private static final float MIN_GIRTH = 0.5f;
    private static final float JOIN = 0.93f;

    private SailGenerator() {
    }

    /** The style index of a form and curve bucket - each clamped. */
    public static int style(int form, int curve) {
        int f = Math.max(0, Math.min(FORMS - 1, form));
        int c = Math.max(0, Math.min(CURVES - 1, curve));
        return f * CURVES + c;
    }

    /** How many styles - every form and curve. */
    public static int styles() {
        return FORMS * CURVES;
    }

    /** The form a style index was built from. */
    public static int formOf(int style) {
        return style / CURVES;
    }

    /** The curve bucket a style index was built from. */
    public static int curveOf(int style) {
        return style % CURVES;
    }

    /**
     * Spine {@code i}'s height as a share of the class height. A tall or scalloped sail
     * peaks at {@code 1} near {@link #PEAK}; a low one is {@link #LOW_SHARE} of that and
     * flatter, its ends rolling down only half as far.
     */
    static float heightShare(int form, int curve, int i) {
        float x = (float) i / (MAX_SPINES - 1);
        float arch = ARCH[Math.max(0, Math.min(CURVES - 1, curve))];
        float reach = x < PEAK ? PEAK : 1f - PEAK;
        float d = (x - PEAK) / reach;
        if (form == LOW) {
            return Math.max(FLOOR, LOW_SHARE * (1f - 0.5f * arch * d * d));
        }
        return Math.max(FLOOR, 1f - arch * d * d);
    }

    /**
     * One sail.
     *
     * @param style     a {@link #style} index; clamped
     * @param sizeClass a {@link SailSize} ordinal; clamped
     */
    public static List<PartNode> generate(int style, int sizeClass) {
        int st = Math.max(0, Math.min(styles() - 1, style));
        int form = formOf(st);
        int curve = curveOf(st);
        int c = Math.max(0, Math.min(SailSize.classes() - 1, sizeClass));
        float tallest = SailSize.values()[c].length();
        int segs = SEGMENTS[c];
        float[] height = new float[MAX_SPINES];
        for (int i = 0; i < MAX_SPINES; i++) {
            height[i] = tallest * heightShare(form, curve, i);
        }
        List<PartNode> out = new ArrayList<>(MAX_SPINES * (segs + COLUMNS));
        for (int i = 0; i < MAX_SPINES; i++) {
            int root = spine(out, i, DorsalSpineGenerator.rootZ(i), height[i], segs);
            if (i > 0) {
                panel(out, root, i, height[i - 1], height[i], form == SCALLOPED);
            }
        }
        return out;
    }

    /** One upright spine, its point on {@link PartSheet#BONE_TIP}; returns its root box's index. */
    private static int spine(List<PartNode> out, int group, float z, float height, int segs) {
        float segLen = height / segs;
        float base = Math.min(1.6f, 0.8f + 0.08f * height);
        int root = out.size();
        int prev = -1;
        for (int k = 0; k < segs; k++) {
            float g = Math.max(MIN_GIRTH, base * (1f - TAPER * k / (segs - 1)));
            int tex = k == segs - 1 ? PartSheet.BONE_TIP : PartSheet.BONE;
            int me = out.size();
            if (k == 0) {
                out.add(new PartNode(-1, 0f, 0f, 0f, z, 0f, 0f, 0f, segLen, g, tex, group));
            } else {
                out.add(new PartNode(prev, JOIN, 0f, 0f, 0f, 0f, 0f, 0f, segLen, g, tex, group));
            }
            prev = me;
        }
        return root;
    }

    /**
     * The membrane in front of spine {@code group}: {@link #COLUMNS} flat boxes standing
     * from the back, side by side across the gap to the spine before it, each as tall
     * as a straight edge from tip to tip would be over it - less a sag, on a scalloped sail.
     */
    private static void panel(List<PartNode> out, int root, int group, float front, float back, boolean scalloped) {
        float gap = DorsalSpineGenerator.rootZ(group) - DorsalSpineGenerator.rootZ(group - 1);
        float wide = gap / COLUMNS;
        for (int j = 0; j < COLUMNS; j++) {
            float f = (j + 0.5f) / COLUMNS;
            float top = (back + (front - back) * (1f - f)) * MEMBRANE_REACH;
            if (scalloped) {
                top *= 1f - SCALLOP_DIP * (float) Math.sin(Math.PI * f);
            }
            out.add(new PartNode(root, 0f, 0f, 0f, -gap + wide * (j + 0.5f), 0f, 0f, 0f,
                    top, wide, PartSheet.MEMBRANE, group, MEMBRANE_THICKNESS));
        }
    }
}
