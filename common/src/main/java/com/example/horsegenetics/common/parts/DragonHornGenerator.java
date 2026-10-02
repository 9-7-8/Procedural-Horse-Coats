package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>One dragon horn</b> - a tapering chain of boxes rooted behind the ear that
 * sweeps back over the neck - grown from three integers. A pair is two of these, one
 * per side, each its own mesh, for the reason {@link AntlerGenerator} gives: a
 * mirrored copy by negative scale would turn culled boxes inside out.
 *
 * <h2>The three forms</h2>
 * <ul>
 *   <li>{@link #SWEPT} - the ordinary dragon horn. It leaves the skull raked back and
 *       every later segment bends a little further back, so the horn follows the line
 *       of the neck. How far it bends is the {@code sweep} bucket: a gentle rake that
 *       still points up, to a curve that passes the horizontal and tips down.</li>
 *   <li>{@link #STRAIGHT} - an angled spike with no bend; the {@code sweep} bucket
 *       sets how far back it leans instead.</li>
 *   <li>{@link #CURLED} - a ram-style curl: a constant backward bend with a small
 *       outward lean per segment, so the circle opens into a spiral that clears the
 *       neck. The {@code sweep} bucket sets how far round it goes.</li>
 * </ul>
 *
 * <h2>What is baked and what is not</h2>
 * Form, sweep and splay change which way each box points, so they are topology and
 * live in the {@link PartShape}'s style - {@link #style}. Length and girth are a
 * draw-time scale and colour a draw-time tint, the horn's arrangement. Splay is baked
 * in three steps rather than applied as a transform because the layer's only free
 * rotation is a lean about {@code x}, and an outward angle on a pair needs one about
 * {@code z} that differs by side; the steps show only down a line bred for splay.
 *
 * <p>Angles are radians in the parent's frame ({@link PartNode}): {@code -y} up,
 * {@code -z} forward, a positive {@code rx} tips forward (so the backward rake here
 * is negative), a positive {@code rz} swings toward {@code +x}. No box sets both
 * {@code rx} and {@code ry}. <b>Geometry only - never seen in a running game</b>
 * when written.
 */
public final class DragonHornGenerator {

    /** Raked back and curving further back along the neck. The ordinary form. */
    public static final int SWEPT = 0;
    /** An angled spike. */
    public static final int STRAIGHT = 1;
    /** A ram-style curl, opening outward. */
    public static final int CURLED = 2;

    /** How many forms - the {@code form} category on the copy. */
    public static final int FORMS = 3;
    /** Sweep buckets: how far a swept horn bends, a spike leans, a curl goes round. */
    public static final int SWEEPS = 4;
    /** Splay buckets: how far the pair angle outward from the skull. */
    public static final int SPLAYS = 3;

    /** Hard cap on boxes per horn. */
    public static final int MAX_NODES = 12;

    /** A swept horn's total bend past its root, in degrees, by sweep bucket. */
    private static final float[] SWEEP_DEGREES = {25f, 50f, 75f, 100f};
    /** A spike's rake back from upright, in radians, by sweep bucket. */
    private static final float[] SPIKE_RAKE = {0.55f, 0.75f, 0.95f, 1.15f};
    /** A curl's total turn, in degrees, by sweep bucket. */
    private static final float[] CURL_DEGREES = {180f, 220f, 260f, 300f};
    /** The root's outward angle, in radians, by splay bucket. */
    private static final float[] SPLAY = {0.15f, 0.40f, 0.65f};

    /** Segments per size class for a sweep or a spike. */
    private static final int[] SEGMENTS = {4, 4, 5, 5, 6};
    /** The same for a curl, which needs more to stay round. */
    private static final int[] CURL_SEGMENTS = {6, 7, 8, 9, 10};

    /** How far back a swept horn leaves the skull, in radians. */
    private static final float ROOT_RAKE = 0.75f;
    private static final float MIN_GIRTH = 0.6f;
    private static final float JOIN = 0.93f;

    private DragonHornGenerator() {
    }

    /** The style index of a form, sweep and splay - each clamped. */
    public static int style(int form, int sweep, int splay) {
        int f = Math.max(0, Math.min(FORMS - 1, form));
        int w = Math.max(0, Math.min(SWEEPS - 1, sweep));
        int p = Math.max(0, Math.min(SPLAYS - 1, splay));
        return (f * SWEEPS + w) * SPLAYS + p;
    }

    /** How many styles - every form, sweep and splay. */
    public static int styles() {
        return FORMS * SWEEPS * SPLAYS;
    }

    /** The form a style index was built from. */
    public static int formOf(int style) {
        return style / (SWEEPS * SPLAYS);
    }

    /**
     * One horn.
     *
     * @param style     a {@link #style} index; clamped
     * @param sizeClass a {@link DragonHornSize} ordinal; clamped
     * @param left      the {@code +x} side rather than the {@code -x} one
     */
    public static List<PartNode> generate(int style, int sizeClass, boolean left) {
        int st = Math.max(0, Math.min(styles() - 1, style));
        int form = formOf(st);
        int sweep = st / SPLAYS % SWEEPS;
        int splay = st % SPLAYS;
        int c = Math.max(0, Math.min(DragonHornSize.classes() - 1, sizeClass));
        float s = left ? 1f : -1f;
        float length = DragonHornSize.values()[c].length();
        float rootRz = s * SPLAY[splay];
        List<PartNode> out = new ArrayList<>();
        switch (form) {
            case STRAIGHT -> chain(out, SEGMENTS[c], length, -SPIKE_RAKE[sweep], rootRz, 0f, 0f, 0.70f);
            case CURLED -> {
                int segs = CURL_SEGMENTS[c];
                float bend = -(float) Math.toRadians(CURL_DEGREES[sweep]) / segs;
                chain(out, segs, length, -ROOT_RAKE * 0.8f, rootRz, bend, s * 0.08f, 0.70f);
            }
            default -> {
                int segs = SEGMENTS[c];
                float bend = -(float) Math.toRadians(SWEEP_DEGREES[sweep]) / (segs - 1);
                chain(out, segs, length, -ROOT_RAKE, rootRz, bend, s * 0.03f, 0.75f);
            }
        }
        return out;
    }

    /**
     * A tapering chain of {@code segs} boxes {@code reach} long, its root turned
     * {@code (rootRx, rootRz)} and every later box {@code (rx, rz)} off its parent.
     * The last two boxes sample {@link PartSheet#HORN_TIP}, the horn's own rule.
     */
    private static void chain(List<PartNode> out, int segs, float reach,
                              float rootRx, float rootRz, float rx, float rz, float taper) {
        float segLen = reach / segs;
        float base = Math.min(2.8f, 1.4f + 0.07f * reach);
        int prev = -1;
        for (int i = 0; i < segs; i++) {
            float g = Math.max(MIN_GIRTH, base * (1f - taper * i / segs));
            int tex = i >= segs - 2 ? PartSheet.HORN_TIP : PartSheet.HORN;
            int me = out.size();
            if (i == 0) {
                out.add(new PartNode(prev, 0f, 0f, 0f, 0f, rootRx, 0f, rootRz, segLen, g, tex));
            } else {
                out.add(new PartNode(prev, JOIN, 0f, 0f, 0f, rx, 0f, rz, segLen, g, tex));
            }
            prev = me;
        }
    }
}
