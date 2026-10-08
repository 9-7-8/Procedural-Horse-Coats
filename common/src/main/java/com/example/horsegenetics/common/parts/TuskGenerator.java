package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>One boar tusk</b> - a short tapering chain of boxes that leaves the corner of the
 * lower jaw sideways and curls up past the upper lip - grown from three integers. A
 * pair is two of these, one per side, each its own mesh, for the reason
 * {@link AntlerGenerator} gives: a mirrored copy by negative scale would turn culled
 * boxes inside out.
 *
 * <h2>The three forms</h2>
 * <ul>
 *   <li>{@link #CURL} - the short one: a small tight upward curl, three boxes, about
 *       two thirds of the length its size asks for.</li>
 *   <li>{@link #SWEEP} - the long one: a larger, opener sweep of four boxes.</li>
 *   <li>{@link #HOOKED} - a sweep whose last two boxes turn hard, so the tusk curls
 *       back on itself near the tip. Which way is the {@code hook}: over the top of
 *       the nose, or back toward the eye.</li>
 * </ul>
 *
 * <h2>What is baked and what is not</h2>
 * Form, sweep and hook change which way each box points, so they are topology and live
 * in the {@link PartShape}'s style - {@link #style}. How far the tusk curls (the
 * {@code sweep} bucket) is baked in three steps rather than applied as a transform,
 * because the layer's only free rotation is a lean about {@code x} and this curl is
 * about {@code z}, opposite on each side - the dragon horn's splay, for the same
 * reason. Length and girth are a draw-time scale and colour a draw-time tint.
 *
 * <p>Angles are radians in the parent's frame ({@link PartNode}): {@code -y} up,
 * {@code -z} forward, a positive {@code rz} swings toward {@code +x}, a negative
 * {@code rx} rakes back. No box sets {@code ry}. <b>Geometry only - never seen in a
 * running game</b> when written; every number here is a first proposal for the owner's
 * eyes (tusks treatment).
 */
public final class TuskGenerator {

    /** Short: a small upward curl. */
    public static final int CURL = 0;
    /** Long: a larger sweep. */
    public static final int SWEEP = 1;
    /** Curling back on itself near the tip. */
    public static final int HOOKED = 2;

    /** How many forms - the tusks locus reads {@code style % FORMS}. */
    public static final int FORMS = 3;
    /** Sweep buckets: how far the tusk curls up from where it leaves the jaw. */
    public static final int SWEEPS = 3;
    /** Which way a hooked tusk's tip turns: {@code 0} over the nose, {@code 1} back toward the eye. */
    public static final int HOOKS = 2;

    /** Hard cap on boxes per tusk. */
    public static final int MAX_NODES = 5;

    /** Boxes per form. */
    private static final int[] SEGMENTS = {3, 4, 5};
    /** How much of its size's length a form reaches: the curl is the short one. */
    private static final float[] REACH = {0.65f, 1.0f, 1.0f};
    /** Total curl back toward upright, in degrees, by form then sweep bucket. */
    private static final float[][] CURL_DEGREES = {
            {50f, 70f, 90f},
            {35f, 55f, 75f},
            {45f, 60f, 75f},
    };

    /** How far from upright a tusk leaves the jaw, outward, in radians. */
    private static final float ROOT_OUT = 1.15f;
    /** How far back it leaves, in radians. */
    private static final float ROOT_BACK = 0.12f;
    /** Each later box rakes this much further back, in radians. */
    private static final float SEGMENT_BACK = 0.05f;
    /** A hook's extra turn on each of the last two boxes, in radians. */
    private static final float HOOK_TURN = 0.75f;

    private static final float MIN_GIRTH = 0.6f;
    /** Lower than the horn's: a tusk bends more per box, so it needs more overlap. */
    private static final float JOIN = 0.88f;

    private TuskGenerator() {
    }

    /** The style index of a form, sweep and hook - each clamped. */
    public static int style(int form, int sweep, int hook) {
        int f = Math.max(0, Math.min(FORMS - 1, form));
        int w = Math.max(0, Math.min(SWEEPS - 1, sweep));
        int h = Math.max(0, Math.min(HOOKS - 1, hook));
        return (f * SWEEPS + w) * HOOKS + h;
    }

    /** How many styles - every form, sweep and hook. */
    public static int styles() {
        return FORMS * SWEEPS * HOOKS;
    }

    /** The form a style index was built from. */
    public static int formOf(int style) {
        return style / (SWEEPS * HOOKS);
    }

    /**
     * One tusk.
     *
     * @param style     a {@link #style} index; clamped
     * @param sizeClass a {@link TuskSize} ordinal; clamped
     * @param left      the {@code +x} side rather than the {@code -x} one
     */
    public static List<PartNode> generate(int style, int sizeClass, boolean left) {
        int st = Math.max(0, Math.min(styles() - 1, style));
        int form = formOf(st);
        int sweep = st / HOOKS % SWEEPS;
        int hook = st % HOOKS;
        int c = Math.max(0, Math.min(TuskSize.classes() - 1, sizeClass));
        float s = left ? 1f : -1f;
        int segs = SEGMENTS[form];
        float reach = TuskSize.values()[c].length() * REACH[form];
        float segLen = reach / segs;
        float base = Math.min(2.0f, 0.9f + 0.11f * reach);
        float curl = (float) Math.toRadians(CURL_DEGREES[form][sweep]) / (segs - 1);

        List<PartNode> out = new ArrayList<>(segs);
        for (int i = 0; i < segs; i++) {
            float g = Math.max(MIN_GIRTH, base * (1f - 0.65f * i / segs));
            // The horn's rule, eased for a three-box tusk so it is not mostly tip.
            int tex = i >= segs - (segs > 3 ? 2 : 1) ? PartSheet.HORN_TIP : PartSheet.HORN;
            if (i == 0) {
                out.add(new PartNode(-1, 0f, 0f, 0f, 0f, -ROOT_BACK, 0f, s * ROOT_OUT, segLen, g, tex));
                continue;
            }
            float rx = -SEGMENT_BACK;
            float rz = -s * curl;
            if (form == HOOKED && i >= segs - 2) {
                if (hook == 0) {
                    rz -= s * HOOK_TURN;
                } else {
                    rx -= HOOK_TURN;
                }
            }
            out.add(new PartNode(i - 1, JOIN, 0f, 0f, 0f, rx, 0f, rz, segLen, g, tex));
        }
        return out;
    }
}
