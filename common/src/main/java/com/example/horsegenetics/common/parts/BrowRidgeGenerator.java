package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A brow ridge</b> - one low wide bar of bone across the front of the skull, above
 * and in front of the eyes - grown from two integers. Centred, so one kind and no side
 * to mirror.
 *
 * <h2>The bar lies on its side</h2>
 * A box hangs along its own {@code -y} and may be thinner across {@code x} but never
 * wider, so a bar wider than it is tall is a box laid over a quarter turn: rooted at
 * the right end of the brow and running across to the left. Its square section is the
 * ridge's height and its depth front to back, kept equal. At draw time the part's
 * stretch is along the anchor's up - the ridge's height - and its girth across it,
 * which is the bar's own length: the ridge's width across the head.
 *
 * <h2>Three forms</h2>
 * A {@link #PLAIN} ridge carries a narrower second step along its top. A
 * {@link #NOTCHED} one carries three bumps, and a {@link #SPINED} one a row of three
 * short cones. Whatever stands on the bar stands back up, turned a quarter the other
 * way, and samples {@link PartSheet#BONE_TIP} at its end - so a two-tone horse wears
 * its base colour on the bar and its tip colour on the step, the bumps or the spines'
 * points. How tall they stand is the {@code bump} bucket.
 *
 * <p>Angles follow {@link PartNode}: {@code -y} up, {@code -z} forward. <b>Geometry
 * only - never seen in a running game</b> when written.
 */
public final class BrowRidgeGenerator {

    /** A bar with a narrower step along its top. */
    public static final int PLAIN = 0;
    /** A bar with three bumps. */
    public static final int NOTCHED = 1;
    /** A bar with a row of three short cones. */
    public static final int SPINED = 2;

    /** How many forms - the {@code form} category on the copy. */
    public static final int FORMS = 3;
    /** Bump buckets: how tall the bumps or spines stand. A plain ridge ignores it. */
    public static final int BUMPS = 3;

    /**
     * The bar's length across the head at a width of one, in model units. The head box
     * is six wide, so this leaves half a unit of skull either side, and the widest wild
     * ridge reaches the edges.
     */
    public static final float WIDTH = 5.0f;

    /** How far the bar's underside sinks into the skull, so a stretched ridge shows no gap. */
    static final float SINK = 0.2f;
    /** How tall a bump stands above the bar, as a share of the bar's height, by bucket. */
    private static final float[] BUMP = {0.3f, 0.5f, 0.75f};
    /** Where the three bumps or spines stand along the bar. */
    private static final float[] AT = {0.2f, 0.5f, 0.8f};

    private static final float MIN_GIRTH = 0.5f;
    private static final float JOIN = 0.93f;
    private static final float QUARTER_TURN = (float) (Math.PI / 2);

    private BrowRidgeGenerator() {
    }

    /**
     * The style index of a form and bump bucket - each clamped. A plain ridge has no
     * bumps, so it is one style whatever the bucket ({@link EarFinGenerator#style} on why).
     */
    public static int style(int form, int bump) {
        int f = Math.max(0, Math.min(FORMS - 1, form));
        int b = Math.max(0, Math.min(BUMPS - 1, bump));
        return f == PLAIN ? 0 : 1 + (f - 1) * BUMPS + b;
    }

    /** How many styles - the plain ridge, and every bump size of the other two forms. */
    public static int styles() {
        return 1 + (FORMS - 1) * BUMPS;
    }

    /** The form a style index was built from. */
    public static int formOf(int style) {
        return style <= 0 ? PLAIN : 1 + (style - 1) / BUMPS;
    }

    /** The bump bucket a style index was built from; {@code 0} for a plain ridge. */
    public static int bumpOf(int style) {
        return style <= 0 ? 0 : (style - 1) % BUMPS;
    }

    /**
     * The ridge.
     *
     * @param style     a {@link #style} index; clamped
     * @param sizeClass a {@link BrowRidgeSize} ordinal; clamped
     */
    public static List<PartNode> generate(int style, int sizeClass) {
        int st = Math.max(0, Math.min(styles() - 1, style));
        int form = formOf(st);
        int c = Math.max(0, Math.min(BrowRidgeSize.classes() - 1, sizeClass));
        float height = BrowRidgeSize.values()[c].length();
        float bump = height * BUMP[bumpOf(st)];
        List<PartNode> out = new ArrayList<>(8);
        // Rooted at the right end, laid over to run to the left: its own -y is +x.
        out.add(new PartNode(-1, 0f, -WIDTH / 2f, -(height / 2f - SINK), 0f,
                0f, 0f, QUARTER_TURN, WIDTH, height, PartSheet.BONE));
        if (form == PLAIN) {
            // The bar's own -x is up. A second, narrower step along its top.
            float step = Math.max(MIN_GIRTH, 0.5f * height);
            out.add(new PartNode(0, 0.15f, -(height / 2f + step / 2f - 0.1f), 0f, 0f,
                    0f, 0f, 0f, 0.7f * WIDTH, step, PartSheet.BONE_TIP));
            return out;
        }
        for (float at : AT) {
            if (form == NOTCHED) {
                // From the bar's axis, so half the bar's height is inside it.
                out.add(new PartNode(0, at, 0f, 0f, 0f, 0f, 0f, -QUARTER_TURN,
                        height / 2f + bump, Math.max(MIN_GIRTH, 0.2f * WIDTH), PartSheet.BONE_TIP));
            } else {
                float reach = 1.4f * bump;
                int me = out.size();
                out.add(new PartNode(0, at, 0f, 0f, 0f, 0f, 0f, -QUARTER_TURN,
                        height / 2f + 0.6f * reach, Math.max(MIN_GIRTH, 0.16f * WIDTH), PartSheet.BONE));
                out.add(new PartNode(me, JOIN, 0f, 0f, 0f, 0f, 0f, 0f,
                        0.5f * reach, MIN_GIRTH, PartSheet.BONE_TIP));
            }
        }
        return out;
    }
}
