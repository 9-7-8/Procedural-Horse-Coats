package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>An ear fin</b> - thin slabs fanned from the outer base of the ear, out from the
 * head and swept back, like a fish's fin where a second ear would be - grown from two
 * integers. One mesh per side.
 *
 * <h2>Every ray is rooted on the anchor</h2>
 * A ray is turned about x by its place in the fan and then leaned out from upright by
 * {@link #LEAN} - x first, then z, which is the order a box turns in ({@link PartNode}) -
 * so the rays all lie in one plane: the one that holds the head's own front-to-back
 * axis, tipped outward. A ray is a flat box, thin across that plane
 * ({@link PartNode#width}) - the fourth flat box after the membrane, the plate's slab
 * and the sabre's blade.
 *
 * <p>Each ray is its own tree, as a plate's slab is, and for the plate's reason: a
 * root sits at the base end of {@code PartModel.along} and takes the base colour whole.
 * The first build hung the rays off a shared knuckle, and a two-tone fin's slabs came
 * out a muddy half-blend of its two colours (seen in a photo shoot, 2026-10-09). So the
 * kind scales per element ({@link PartKind#scalesPerElement()}); every root is at the
 * anchor, so that is the same grow a whole-part scale would give.
 *
 * <h2>Three forms</h2>
 * A {@link #BLADE} is one broad slab. A {@link #FAN} is three slabs of rising length,
 * front to back. A {@link #FRILL} is five narrower slabs in an arc, each with a ray - a
 * rod down its middle that stands a little proud of it and past its end. A fan's or a
 * frill's slabs are counted ({@link PartNode#group}), so a line bred for more rays
 * grows them in one at a time; the numbering starts at the longest, so a low count is
 * still a fin. A blade is one group that always shows.
 *
 * <h2>Two colours</h2>
 * A slab samples {@link PartSheet#HORN} and is a root, so it is the base colour; its
 * point (a blade's or a fan's last box) and a frill's rod sample
 * {@link PartSheet#HORN_TIP} and hang off it with nothing below them, so a two-tone
 * horse wears its tip colour on them ({@code PartModel.along}).
 * There is <b>no membrane</b> between the rays: the treatment leaves it out for now.
 *
 * <p>Angles follow {@link PartNode}: {@code -y} up, {@code -z} forward. The right side
 * is model {@code -x}; the left is the right with every ray's lean negated, never a
 * negative scale. <b>Geometry only - never seen in a running game</b> when written.
 */
public final class EarFinGenerator {

    /** One broad slab. */
    public static final int BLADE = 0;
    /** Three slabs of rising length. */
    public static final int FAN = 1;
    /** Five slabs in an arc, each with a ray. */
    public static final int FRILL = 2;

    /** How many forms - the {@code form} category on the copy. */
    public static final int FORMS = 3;
    /** Spread buckets: how wide the fan opens. A blade has one slab and ignores it. */
    public static final int SPREADS = 3;

    /** The most rays any form bakes - a frill's. */
    public static final int MAX_RAYS = 5;

    /** How far the fin leans out from upright, in radians - the knuckle's turn. */
    public static final float LEAN = 0.9f;
    /** Where the middle of the fan points, back from upright, in radians. */
    static final float SWEEP = -0.35f;
    /** The whole angle a fan or a frill opens across, by spread bucket. */
    private static final float[] SPREAD = {0.6f, 1.0f, 1.4f};

    /** A fan's rays, front to back, as a share of the longest. */
    private static final float[] FAN_LENGTHS = {0.6f, 0.8f, 1.0f};
    /** Which countable group each of a fan's rays is, front to back - the longest first. */
    private static final int[] FAN_GROUPS = {2, 1, 0};
    /** A frill's rays, front to back. */
    private static final float[] FRILL_LENGTHS = {0.7f, 0.9f, 1.0f, 0.9f, 0.7f};
    /** A frill's groups, front to back - the middle first, then outward either side. */
    private static final int[] FRILL_GROUPS = {4, 2, 0, 1, 3};

    private static final float MIN_GIRTH = 0.5f;
    private static final float JOIN = 0.93f;

    private EarFinGenerator() {
    }

    /**
     * The style index of a form and spread bucket - each clamped. A blade has one slab
     * and nothing to open, so it is one style whatever the spread: the part cache is
     * bounded in boxes ({@code PartGeneratorTest.theWholeSetOfMeshesIsSmall}), and three
     * identical blades would spend it on nothing.
     */
    public static int style(int form, int spread) {
        int f = Math.max(0, Math.min(FORMS - 1, form));
        int s = Math.max(0, Math.min(SPREADS - 1, spread));
        return f == BLADE ? 0 : 1 + (f - 1) * SPREADS + s;
    }

    /** How many styles - the blade, and every spread of the other two forms. */
    public static int styles() {
        return 1 + (FORMS - 1) * SPREADS;
    }

    /** The form a style index was built from. */
    public static int formOf(int style) {
        return style <= 0 ? BLADE : 1 + (style - 1) / SPREADS;
    }

    /** The spread bucket a style index was built from; {@code 0} for a blade. */
    public static int spreadOf(int style) {
        return style <= 0 ? 0 : (style - 1) % SPREADS;
    }

    /** How many countable rays {@code form} bakes - none for a blade, which is always whole. */
    public static int maxRays(int form) {
        return switch (form) {
            case FAN -> FAN_LENGTHS.length;
            case FRILL -> FRILL_LENGTHS.length;
            default -> 0;
        };
    }

    /**
     * One side's fin.
     *
     * @param style     a {@link #style} index; clamped
     * @param sizeClass an {@link EarFinSize} ordinal; clamped
     * @param left      the left side ({@code +x}) rather than the right
     */
    public static List<PartNode> generate(int style, int sizeClass, boolean left) {
        int st = Math.max(0, Math.min(styles() - 1, style));
        int form = formOf(st);
        float spread = SPREAD[spreadOf(st)];
        int c = Math.max(0, Math.min(EarFinSize.classes() - 1, sizeClass));
        float longest = EarFinSize.values()[c].length();
        float side = left ? 1f : -1f;
        List<PartNode> out = new ArrayList<>(MAX_RAYS * 2);
        if (form == BLADE) {
            // One group, always shown whole: a blade has nothing to count.
            ray(out, 0, SWEEP, side * LEAN, longest, 0.42f, 0.4f, false);
            return out;
        }
        float[] lengths = form == FAN ? FAN_LENGTHS : FRILL_LENGTHS;
        int[] groups = form == FAN ? FAN_GROUPS : FRILL_GROUPS;
        int n = lengths.length;
        for (int k = 0; k < n; k++) {
            // Front to back: the first ray tips furthest forward, the last furthest back.
            float angle = SWEEP + spread * (0.5f - (float) k / (n - 1));
            if (form == FAN) {
                ray(out, groups[k], angle, side * LEAN, longest * lengths[k], 0.30f, 0.4f, false);
            } else {
                ray(out, groups[k], angle, side * LEAN, longest * lengths[k], 0.24f, 0.3f, true);
            }
        }
        return out;
    }

    /**
     * One ray rooted on the anchor: a flat slab {@code length} long turned {@code angle}
     * about x and leaned {@code lean} about z, and either its narrower point or, on a
     * frill, the rod down its middle.
     */
    private static void ray(List<PartNode> out, int group, float angle, float lean, float length,
                            float breadthShare, float thin, boolean rod) {
        float breadth = Math.max(0.6f, breadthShare * length);
        int me = out.size();
        out.add(new PartNode(-1, 0f, 0f, 0f, 0f, angle, 0f, lean, length, breadth, PartSheet.HORN, group, thin));
        if (rod) {
            out.add(new PartNode(me, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1.2f * length, MIN_GIRTH,
                    PartSheet.HORN_TIP, group));
        } else {
            float point = Math.max(MIN_GIRTH, 0.6f * breadth);
            out.add(new PartNode(me, JOIN, 0f, 0f, 0f, 0f, 0f, 0f, 0.3f * length, point,
                    PartSheet.HORN_TIP, group, Math.min(thin, point)));
        }
    }
}
