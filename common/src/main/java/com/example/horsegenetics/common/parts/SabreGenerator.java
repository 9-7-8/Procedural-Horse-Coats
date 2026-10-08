package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>One sabre fang</b> - a flat tapering blade that hangs from the front of the upper
 * lip to below the chin - grown from three integers. A pair is two of these, one per
 * side, each its own mesh, never one mirrored by a negative scale
 * ({@link AntlerGenerator}).
 *
 * <h2>It hangs, so its root is turned over</h2>
 * Every part mesh grows up its own {@code -y}. A fang grows down, so its root box
 * carries a half-turn about {@code x} and the rest follow as its children. That is in
 * the mesh rather than in the layer (as the narwhal horn's forward turn is) because the
 * half-turn keeps the blade's axis on the anchor's {@code y}: the layer's stretch still
 * runs along the fang and its lean still tips it, with no new case there. After the
 * half-turn the mesh's {@code -z} is the horse's <b>back</b>, so a positive {@code rx}
 * on a later box curves the blade back toward the throat, and the serrations sit on
 * {@code -z}, the back edge.
 *
 * <h2>The three forms</h2>
 * <ul>
 *   <li>{@link #STRAIGHT} - two boxes, a blade and its point.</li>
 *   <li>{@link #CURVED} - three boxes, each bent a little further back.</li>
 *   <li>{@link #SERRATED} - the straight blade with small barbs down its back edge.
 *       Where they sit is the {@code pattern}, picked by the copy's seed.</li>
 * </ul>
 *
 * <h2>A blade is a flat box</h2>
 * Thin across the horse ({@link PartNode#width}) and deep along it, the third flat box
 * after a sail's membrane and a plate's slab. {@code PartGeneratorTest} allows it for
 * this kind alone.
 *
 * <p><b>Geometry only - never seen in a running game</b> when written; every number
 * here is a first proposal for the owner's eyes (tusks treatment).
 */
public final class SabreGenerator {

    /** One straight blade. */
    public static final int STRAIGHT = 0;
    /** Curved back toward the throat. */
    public static final int CURVED = 1;
    /** Straight, with a notched back edge. */
    public static final int SERRATED = 2;

    /** How many forms - the tusks locus reads {@code style % FORMS}. */
    public static final int FORMS = 3;
    /** How many serration patterns - the copy's seed picks one. */
    public static final int PATTERNS = 3;

    /** Hard cap on boxes per fang: two of blade and the most barbs a pattern has. */
    public static final int MAX_NODES = 7;

    /** Where the barbs sit along the blade box, then along the point box, by pattern. */
    private static final float[][][] BARBS = {
            {{0.35f, 0.70f}, {0.30f}},
            {{0.25f, 0.50f, 0.75f}, {0.25f, 0.60f}},
            {{0.55f}, {0.15f, 0.55f}},
    };

    /** How far each side leans away from the midline, in radians. */
    private static final float SPLAY = 0.08f;
    /** A curved fang's bend per box, in radians. */
    private static final float BEND = 0.22f;
    /** How far a barb points off the blade, back and down, in radians. */
    private static final float BARB_RAKE = 1.1f;
    private static final float BARB_LENGTH = 0.9f;
    private static final float BARB_GIRTH = 0.5f;

    /** Thinnest a blade box is along the horse. */
    private static final float MIN_DEPTH = 0.6f;
    private static final float JOIN = 0.92f;

    private SabreGenerator() {
    }

    /** The style index of a form and a serration pattern - each clamped. The pattern only shows on {@link #SERRATED}. */
    public static int style(int form, int pattern) {
        int f = Math.max(0, Math.min(FORMS - 1, form));
        int p = f == SERRATED ? Math.max(0, Math.min(PATTERNS - 1, pattern)) : 0;
        return f * PATTERNS + p;
    }

    /** How many styles - every form and pattern. */
    public static int styles() {
        return FORMS * PATTERNS;
    }

    /** The form a style index was built from. */
    public static int formOf(int style) {
        return style / PATTERNS;
    }

    /**
     * One fang.
     *
     * @param style     a {@link #style} index; clamped
     * @param sizeClass a {@link SabreSize} ordinal; clamped
     * @param left      the {@code +x} side rather than the {@code -x} one
     */
    public static List<PartNode> generate(int style, int sizeClass, boolean left) {
        int st = Math.max(0, Math.min(styles() - 1, style));
        int form = formOf(st);
        int pattern = st % PATTERNS;
        int c = Math.max(0, Math.min(SabreSize.classes() - 1, sizeClass));
        float s = left ? 1f : -1f;
        float reach = SabreSize.values()[c].length();
        float depth = Math.min(2.2f, 1.1f + 0.09f * reach);

        // How the length and the taper are shared out, root to point.
        float[] share = form == CURVED ? new float[] {0.40f, 0.34f, 0.26f} : new float[] {0.62f, 0.38f};
        float[] deep = form == CURVED ? new float[] {1.0f, 0.72f, 0.45f} : new float[] {1.0f, 0.55f};
        float[] wide = form == CURVED ? new float[] {0.70f, 0.60f, 0.50f} : new float[] {0.70f, 0.50f};

        List<PartNode> out = new ArrayList<>(MAX_NODES);
        for (int i = 0; i < share.length; i++) {
            float g = Math.max(MIN_DEPTH, depth * deep[i]);
            int tex = i == share.length - 1 ? PartSheet.HORN_TIP : PartSheet.HORN;
            if (i == 0) {
                // The half-turn that hangs it, and the lean away from the midline: after
                // the half-turn a positive rz swings toward -x, so the left side is negative.
                out.add(new PartNode(-1, 0f, 0f, 0f, 0f, (float) Math.PI, 0f, -s * SPLAY,
                        reach * share[i], g, tex, PartNode.NO_GROUP, wide[i]));
            } else {
                out.add(new PartNode(i - 1, JOIN, 0f, 0f, 0f, form == CURVED ? BEND : 0f, 0f, 0f,
                        reach * share[i], g, tex, PartNode.NO_GROUP, wide[i]));
            }
        }
        if (form == SERRATED) {
            for (int box = 0; box < 2; box++) {
                // On the back edge, a little inside it so a barb shows no gap at its root.
                float edge = -(out.get(box).girth() / 2f - 0.2f);
                for (float t : BARBS[pattern][box]) {
                    out.add(new PartNode(box, t, 0f, 0f, edge, BARB_RAKE, 0f, 0f,
                            BARB_LENGTH, BARB_GIRTH, PartSheet.HORN_TIP));
                }
            }
        }
        return out;
    }
}
