package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A row of dorsal spines</b> - tapering cones standing in a line along the top of
 * the back, from the withers to the croup - grown from two integers.
 *
 * <h2>One group per spine, numbered front to back</h2>
 * Every spine is its own root chain, rooted at the {@link PartAnchor#SPINE} anchor
 * plus a fixed offset along {@code +z}, and every box of spine {@code i} carries
 * {@link PartNode#group} {@code i}. Two things read that numbering, and both are why
 * the row is baked with {@link #MAX_SPINES} however many a horse shows:
 * <ul>
 *   <li>the count - a horse shows the first {@code count} spines from the withers,
 *       the antler's tine rule ({@code AttachedPart#shown});</li>
 *   <li>the saddle zone - a saddled or ridden horse hides the spines whose root
 *       falls under the saddle, as a mask of groups ({@link SaddleZone}).</li>
 * </ul>
 *
 * <h2>Each spine scales in its own frame</h2>
 * A horn or an antler is scaled as a whole on the pose stack, and for a part that
 * grows away from its anchor that is right. A row lies along the back, so scaling it
 * across its axis would also stretch it lengthwise, past the croup. The spine kind
 * therefore scales each spine about its own root instead
 * ({@link PartKind#scalesPerElement()}), and the row keeps its length.
 *
 * <h2>The three forms</h2>
 * <ul>
 *   <li>{@link #UNIFORM} - every spine the same height;</li>
 *   <li>{@link #GRADUATED} - tallest at the withers, falling to under half that at
 *       the croup;</li>
 *   <li>{@link #ALTERNATING} - tall and short in turn.</li>
 * </ul>
 * Form and taper (how sharply each spine narrows to its point) change box sizes, so
 * they are topology and live in the style; height and thickness are the per-spine
 * scale, colour a draw-time tint. Angles follow {@link PartNode}: {@code -y} up,
 * {@code -z} forward, a negative {@code rx} rakes a spine back toward the tail.
 * <b>Geometry only - never seen in a running game</b> when written.
 */
public final class DorsalSpineGenerator {

    /** Every spine the same height. */
    public static final int UNIFORM = 0;
    /** Tallest at the withers, falling toward the croup. */
    public static final int GRADUATED = 1;
    /** Tall and short in turn. */
    public static final int ALTERNATING = 2;

    /** How many forms - the {@code form} category on the copy. */
    public static final int FORMS = 3;
    /** Taper buckets: how sharply a spine narrows to its point. */
    public static final int TAPERS = 3;

    /**
     * Spines in a full row - the most a horse can show. Under 32, because the saddle
     * zone is an {@code int} mask of groups ({@code SaddleZoneTest} holds it).
     */
    public static final int MAX_SPINES = 11;

    /**
     * How far the row runs back from the anchor, in model units. The anchor sits at
     * the withers, where the neck's back edge and the mane meet the top of the body
     * (body {@code z} about -11 on the vanilla box); the body ends at {@code z 5}, so
     * fifteen units ends the row a unit short of the croup's edge.
     */
    public static final float ROW_LENGTH = 15.0f;

    /** How far each spine rakes back from upright, in radians. */
    private static final float RAKE = -0.30f;
    /** How much narrower a spine's last box is than its first, by taper bucket. */
    private static final float[] TAPER = {0.35f, 0.60f, 0.85f};
    /** Boxes per spine, by size class - a taller spine needs more to taper smoothly. */
    private static final int[] SEGMENTS = {2, 2, 3, 3};
    /** A graduated row's last spine, as a share of its first. */
    private static final float GRADUATED_END = 0.4f;
    /** An alternating row's short spines, as a share of its tall ones. */
    private static final float ALTERNATE_SHORT = 0.55f;

    private static final float MIN_GIRTH = 0.5f;
    private static final float JOIN = 0.93f;

    private DorsalSpineGenerator() {
    }

    /** The style index of a form and taper bucket - each clamped. */
    public static int style(int form, int taper) {
        int f = Math.max(0, Math.min(FORMS - 1, form));
        int t = Math.max(0, Math.min(TAPERS - 1, taper));
        return f * TAPERS + t;
    }

    /** How many styles - every form and taper. */
    public static int styles() {
        return FORMS * TAPERS;
    }

    /** The form a style index was built from. */
    public static int formOf(int style) {
        return style / TAPERS;
    }

    /** The taper bucket a style index was built from. */
    public static int taperOf(int style) {
        return style % TAPERS;
    }

    /** Where spine {@code i} roots, along the row from the anchor. */
    public static float rootZ(int i) {
        return ROW_LENGTH * i / (MAX_SPINES - 1);
    }

    /** Spine {@code i}'s height as a share of the row's tallest, in {@code form}. */
    static float heightShare(int form, int i) {
        return switch (form) {
            case GRADUATED -> 1f - (1f - GRADUATED_END) * i / (MAX_SPINES - 1);
            case ALTERNATING -> i % 2 == 0 ? 1f : ALTERNATE_SHORT;
            default -> 1f;
        };
    }

    /**
     * One row.
     *
     * @param style     a {@link #style} index; clamped
     * @param sizeClass a {@link SpineSize} ordinal; clamped
     */
    public static List<PartNode> generate(int style, int sizeClass) {
        int st = Math.max(0, Math.min(styles() - 1, style));
        int form = formOf(st);
        float taper = TAPER[taperOf(st)];
        int c = Math.max(0, Math.min(SpineSize.classes() - 1, sizeClass));
        float tallest = SpineSize.values()[c].length();
        int segs = SEGMENTS[c];
        List<PartNode> out = new ArrayList<>(MAX_SPINES * segs);
        for (int i = 0; i < MAX_SPINES; i++) {
            spine(out, i, rootZ(i), tallest * heightShare(form, i), segs, taper);
        }
        return out;
    }

    /**
     * One spine: {@code segs} boxes {@code height} tall in all, raked back, each
     * narrower than the last; the point samples {@link PartSheet#BONE_TIP}.
     */
    private static void spine(List<PartNode> out, int group, float z, float height, int segs, float taper) {
        float segLen = height / segs;
        float base = Math.min(2.2f, 1.0f + 0.12f * height);
        int prev = -1;
        for (int k = 0; k < segs; k++) {
            float g = Math.max(MIN_GIRTH, base * (1f - taper * k / (segs - 1)));
            int tex = k == segs - 1 ? PartSheet.BONE_TIP : PartSheet.BONE;
            int me = out.size();
            if (k == 0) {
                out.add(new PartNode(-1, 0f, 0f, 0f, z, RAKE, 0f, 0f, segLen, g, tex, group));
            } else {
                out.add(new PartNode(prev, JOIN, 0f, 0f, 0f, 0f, 0f, 0f, segLen, g, tex, group));
            }
            prev = me;
        }
    }
}
