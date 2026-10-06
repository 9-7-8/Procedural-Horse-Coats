package com.example.horsegenetics.common.parts;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Crystal growths</b> - clusters of crystals breaking out of the back along the
 * spine, each cluster three to seven crystals fanning from the midline - grown from
 * three integers.
 *
 * <h2>One cluster is one tree and one group</h2>
 * The row is the dorsal spines' row: {@link #MAX_CLUSTERS} patches from the withers to
 * the croup, rooted at the {@link PartAnchor#SPINE} anchor, numbered front to back, so
 * the count ({@code AttachedPart#shown}) and the saddle zone ({@link SaddleZone}) work on
 * clusters exactly as they do on spines. Each cluster's <b>central crystal is its
 * root</b>, standing upright, and every other crystal of the cluster hangs off that root
 * at {@code t = 0}, set out to the side and leaning away from the midline. So:
 * <ul>
 *   <li>a cluster is one group with one root, and hides, shrinks and scales as a unit -
 *       the per-element scale ({@link PartKind#scalesPerElement()}) lands on the root and
 *       carries the whole cluster with it, about its own place on the back;</li>
 *   <li>the root is upright, so its children's offsets are in the body's own frame and
 *       a crystal set out to the side stays on the back rather than sinking or floating.</li>
 * </ul>
 *
 * <h2>A crystal is three boxes - an eight-sided prism with a point</h2>
 * A shaft, the same shaft again rolled a quarter-turn's half ({@code ry = pi/4}) about
 * its own axis - two crossed squares read as an octagon - and a short narrower cap at the
 * top, rolled an eighth. The shafts sample {@link PartSheet#CRYSTAL}, the region the kind
 * draws see-through ({@link PartKind#translucentRegions()}); the cap samples
 * {@link PartSheet#BONE_TIP} and is drawn solid, so a crystal has an edge - the crystal
 * antler's split, shafts see-through and points solid. A roll is only ever on a box that
 * does not bend, which is {@code PartGeneratorTest}'s rule against coiling chains.
 *
 * <h2>The seed is the arrangement</h2>
 * Where each crystal of each cluster stands, how long it is against the central one, how
 * many crystals a cluster has and how far each leans relative to the others all come from
 * one of {@link #VARIANTS} arrangements, picked by the {@code seed} on the copy. The
 * {@code spread} bucket only scales the lean, and the size class only the lengths, so a
 * horse whose spread drifts keeps its own arrangement. Angles follow {@link PartNode}:
 * {@code -y} up, {@code -z} forward, a positive {@code rz} leans toward {@code +x}.
 * <b>Geometry only - never seen in a running game</b> when written.
 */
public final class CrystalGenerator {

    /** Arrangements the seed chooses between - each a different scatter of every cluster. */
    public static final int VARIANTS = 8;
    /** Lean-spread buckets: how far the outer crystals of a cluster fan from upright. */
    public static final int SPREADS = 3;

    /**
     * Clusters in a full row - the most a horse can show. Under 32, because the saddle
     * zone is an {@code int} mask of groups.
     */
    public static final int MAX_CLUSTERS = 6;

    /** The fewest crystals in a cluster, the central one included. */
    public static final int MIN_CRYSTALS = 3;
    /** The most. */
    public static final int MAX_CRYSTALS = 7;

    /** Boxes per crystal: two crossed shafts and a cap. */
    public static final int BOXES_PER_CRYSTAL = 3;

    /** How far an outer crystal leans at the widest point of its cluster, by spread bucket, radians. */
    private static final float[] SPREAD = {0.15f, 0.45f, 0.80f};
    /** How far across the back an outer crystal may stand from the midline, model units. */
    private static final float HALF_WIDTH = 2.6f;
    /** How far along the row an outer crystal may stand from its cluster's centre. */
    private static final float HALF_DEPTH = 1.4f;
    /** How far an inner cluster's centre may sit from its even place along the row. */
    private static final float ROW_JITTER = 0.8f;
    /** How far the crystals' roots sit into the back, so none stands on the hair. */
    private static final float SUNK = 0.4f;
    /** Shortest an outer crystal is against the central one. */
    private static final float SHORTEST_SHARE = 0.45f;
    /** Longest. */
    private static final float LONGEST_SHARE = 0.85f;
    /** A crystal's thickness against its length. */
    private static final float THICKNESS = 0.30f;
    /** The thickest any crystal is, model units. */
    private static final float MAX_GIRTH = 2.6f;
    /** The cap's length against its crystal's. */
    private static final float CAP_LENGTH = 0.28f;
    /** The cap's thickness against its crystal's. */
    private static final float CAP_GIRTH = 0.55f;

    private static final float MIN_GIRTH = 0.5f;
    private static final float JOIN = 0.95f;
    private static final float ROLL = (float) (Math.PI / 4.0);
    private static final float CAP_ROLL = (float) (Math.PI / 8.0);

    private CrystalGenerator() {
    }

    /** The style index of a variant and a spread bucket - each clamped. */
    public static int style(int variant, int spread) {
        int v = Math.max(0, Math.min(VARIANTS - 1, variant));
        int s = Math.max(0, Math.min(SPREADS - 1, spread));
        return v * SPREADS + s;
    }

    /** How many styles - every arrangement at every spread. */
    public static int styles() {
        return VARIANTS * SPREADS;
    }

    /** The arrangement a style index was built from. */
    public static int variantOf(int style) {
        return style / SPREADS;
    }

    /** The spread bucket a style index was built from. */
    public static int spreadOf(int style) {
        return style % SPREADS;
    }

    /** Where cluster {@code i} roots along the row, before its arrangement's jitter. */
    public static float rootZ(int i) {
        return DorsalSpineGenerator.ROW_LENGTH * i / (MAX_CLUSTERS - 1);
    }

    /** How many crystals each cluster of arrangement {@code variant} has, front to back. */
    public static int[] crystalsPerCluster(int variant) {
        Rng r = new SeededRng(seedOf(Math.max(0, Math.min(VARIANTS - 1, variant))));
        int[] out = new int[MAX_CLUSTERS];
        for (int i = 0; i < MAX_CLUSTERS; i++) {
            out[i] = MIN_CRYSTALS + r.nextInt(MAX_CRYSTALS - MIN_CRYSTALS + 1);
            // The rest of this cluster's numbers, drawn whether or not they are used, so
            // the next cluster's count never depends on how this one was laid out.
            skipCluster(r, out[i]);
        }
        return out;
    }

    /**
     * One row of clusters.
     *
     * @param style     a {@link #style} index; clamped
     * @param sizeClass a {@link CrystalSize} ordinal; clamped
     */
    public static List<PartNode> generate(int style, int sizeClass) {
        int st = Math.max(0, Math.min(styles() - 1, style));
        float spread = SPREAD[spreadOf(st)];
        int c = Math.max(0, Math.min(CrystalSize.classes() - 1, sizeClass));
        float tallest = CrystalSize.values()[c].length();
        Rng r = new SeededRng(seedOf(variantOf(st)));
        List<PartNode> out = new ArrayList<>(MAX_CLUSTERS * MAX_CRYSTALS * BOXES_PER_CRYSTAL);
        for (int i = 0; i < MAX_CLUSTERS; i++) {
            int crystals = MIN_CRYSTALS + r.nextInt(MAX_CRYSTALS - MIN_CRYSTALS + 1);
            // The end clusters stay at the withers and the croup, clear of the saddle; the
            // draw is taken anyway, so every cluster's arrangement is the same draw count.
            float shift = jitter(r, ROW_JITTER);
            boolean end = i == 0 || i == MAX_CLUSTERS - 1;
            float z = end ? rootZ(i) : clampRow(rootZ(i) + shift);
            int root = crystal(out, -1, 0f, SUNK, z, 0f, 0f, tallest, i);
            for (int k = 1; k < crystals; k++) {
                float x = jitter(r, HALF_WIDTH);
                float dz = jitter(r, HALF_DEPTH);
                float share = SHORTEST_SHARE + r.nextFloat() * (LONGEST_SHARE - SHORTEST_SHARE);
                float wobble = 0.5f + 0.5f * r.nextFloat();
                float pitch = jitter(r, 0.35f);
                // Fan from the midline: the further out, the further it leans away.
                float lean = Math.signum(x) * spread * wobble * (0.4f + 0.6f * Math.abs(x) / HALF_WIDTH);
                crystal(out, root, x, 0f, dz, spread * pitch, lean, tallest * share, i);
            }
            // Pad the draws to the most a cluster can have, so the arrangement of the
            // next cluster is the same whatever this one's count came out as.
            for (int k = crystals; k < MAX_CRYSTALS; k++) {
                skipCrystal(r);
            }
        }
        return out;
    }

    /**
     * One crystal: a shaft, the shaft again crossed at a quarter-turn's half, and a solid
     * cap. Rooted on the anchor if {@code parent} is negative, else at the parent's base.
     * Returns the shaft's index.
     */
    private static int crystal(List<PartNode> out, int parent, float x, float y, float z,
                               float rx, float rz, float len, int group) {
        float g = Math.max(MIN_GIRTH, Math.min(MAX_GIRTH, THICKNESS * len));
        int shaft = out.size();
        out.add(new PartNode(parent, 0f, x, y, z, rx, 0f, rz, len, g, PartSheet.CRYSTAL, group));
        out.add(new PartNode(shaft, 0f, 0f, 0f, 0f, 0f, ROLL, 0f, len, g, PartSheet.CRYSTAL, group));
        out.add(new PartNode(shaft, JOIN, 0f, 0f, 0f, 0f, CAP_ROLL, 0f,
                Math.max(0.3f, CAP_LENGTH * len), Math.max(MIN_GIRTH, CAP_GIRTH * g),
                PartSheet.BONE_TIP, group));
        return shaft;
    }

    /** The draws one outer crystal takes - kept in step with {@link #generate}. */
    private static void skipCrystal(Rng r) {
        for (int d = 0; d < 5; d++) {
            r.nextFloat();
        }
    }

    /** The draws the rest of a cluster of {@code crystals} takes after its count. */
    private static void skipCluster(Rng r, int crystals) {
        r.nextFloat();                                      // the row jitter
        for (int k = 1; k < MAX_CRYSTALS; k++) {
            skipCrystal(r);
        }
    }

    private static float clampRow(float z) {
        return Math.max(0f, Math.min(DorsalSpineGenerator.ROW_LENGTH, z));
    }

    private static float jitter(Rng r, float reach) {
        return (r.nextFloat() * 2f - 1f) * reach;
    }

    /** The seed arrangement {@code variant} is grown from. */
    private static long seedOf(int variant) {
        return 0xC4157A1L ^ ((variant + 1) * 0x9E3779B97F4A7C15L);
    }
}
