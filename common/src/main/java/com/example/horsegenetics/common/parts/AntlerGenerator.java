package com.example.horsegenetics.common.parts;

import com.example.horsegenetics.common.Rng;
import com.example.horsegenetics.common.SeededRng;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>One antler</b> - a beam that sweeps up and back off the skull, with tines
 * forking off it - grown by a small rule from four integers. The rack is two of
 * these, one per side, each its own mesh.
 *
 * <h2>Why one side per mesh, and not a mirrored pair</h2>
 * The intake draft built both antlers into one list by replaying a seed with the
 * signs flipped. That stops working the moment the two sides differ, and an
 * asymmetric rack is a decided feature (D3: symmetric by default, a breed may pin
 * it otherwise). Drawing the left antler as the right one with a negative scale
 * would turn its boxes inside out under a culled render type. So a side is an
 * argument, the sides are two {@link PartKind}s, and a symmetric rack is simply
 * the same {@code variant} asked of both.
 *
 * <h2>What the four integers are</h2>
 * <ul>
 *   <li>{@code form} - the growth habit, one of {@link #FORMS}: the
 *       {@code antler_form} locus. A different rule, not a different number.</li>
 *   <li>{@code variant} - which of {@link #VARIANTS} racks of that habit, picked
 *       by the {@code rack} seed on the allele copy. The seed is a {@code long};
 *       this is the render cache's view of it (owner's rule 4: a cache key, not a
 *       limit on the genome).</li>
 *   <li>{@code sizeClass} - the {@link AntlerSize} class. A bigger class is more
 *       beam segments and room for more tines.</li>
 *   <li>{@code left} - which side, which only flips the outward lean.</li>
 * </ul>
 *
 * <h2>Baked at the maximum, shown by count</h2>
 * Every mesh carries the most tines its class can hold ({@link #maxTines}),
 * numbered base to tip in {@link PartNode#group}. How many a horse actually shows
 * is the {@code tines} number on its allele copy, applied at draw time by hiding
 * the groups past it and shrinking the last one by the fraction - so tines are
 * continuous, a line bred toward more of them grows the next one in rather than
 * popping it, and the box count is capped by this class rather than by a value
 * somebody remembers to clamp.
 *
 * <p>The same trick carries the bloom: every antler mesh has its leaf clumps in
 * the {@link PartSheet#BLOOM} region, and only a blooming horse's submit draws that
 * region. Cheaper than a second key axis, and a clump on a hidden tine hides with
 * it because it is the tine's child.
 *
 * <h2>The invariants a test holds it to</h2>
 * Deterministic; parents before children; one root; never more than
 * {@link #MAX_NODES} boxes; no box thinner than the floor; no box that both bends
 * and rolls (see {@link PartNode} - so this file never sets {@code ry}); and tine
 * groups numbered {@code 0..maxTines-1} with nothing missing.
 *
 * <p>Angles are radians in the parent's frame: {@code -y} is up, {@code -z}
 * forward, a positive {@code rx} tips a limb forward and a positive {@code rz}
 * swings it toward {@code +x}. <b>Geometry only - never seen in a running game</b>
 * when written; the look of each habit is the antler page's to verify.
 */
public final class AntlerGenerator {

    /** Forked and branched - a red deer's rack. The commonest habit. */
    public static final int FORK = 0;
    /** A long beam swept back and over, with a brow tine forward over the face - a reindeer's. */
    public static final int BROW = 1;
    /** A short beam opening into a broad, many-pointed palm - a moose's or a fallow buck's. */
    public static final int PALMATE = 2;
    /** A tall beam ending in a cup of tines - a royal head. */
    public static final int CROWN = 3;
    /** A plain spike, or a spike with one short fork - a yearling's, or a brocket's. */
    public static final int SPIKE = 4;

    /** How many habits there are. In {@code antler_form}'s allele order. */
    public static final int FORMS = 5;

    /**
     * Racks per habit. Sixteen, as the treatment proposed. Whether sixteen read as
     * sixteen different racks on a live horse is unverified; raising it is a
     * one-number change that grows the cache, nothing else.
     */
    public static final int VARIANTS = 16;

    /** The most tines one antler can carry, at the largest class. */
    public static final int MAX_TINES = 8;

    /** Hard cap on boxes per antler. Asserted over every shape by {@code PartGeneratorTest}. */
    public static final int MAX_NODES = 44;

    /** Thinnest box emitted. Under about half a unit a box shimmers. */
    private static final float MIN_GIRTH = 0.6f;

    /** Where along its parent a continuing segment sits - a little overlap hides the join. */
    private static final float JOIN = 0.92f;

    /** Beam segments per size class, for the habits with a full beam. */
    private static final int[] BEAM_SEGMENTS = {2, 3, 4, 5, 6};

    /** Tines per size class for a forked or brow-tined rack. */
    private static final int[] BRANCHED_TINES = {2, 3, 5, 6, 8};
    /** Points per size class on a palm. */
    private static final int[] PALM_POINTS = {3, 4, 5, 7, 8};
    /** Tines per size class on a crowned rack - the many-tined one. */
    private static final int[] CROWN_TINES = {3, 4, 6, 7, 8};

    private AntlerGenerator() {
    }

    /**
     * How many tines a mesh of this habit and class is baked with - the most the
     * {@code tines} number can ever show on it. A spike carries a single fork once
     * it is big enough, and none before.
     */
    public static int maxTines(int form, int sizeClass) {
        int c = clampClass(sizeClass);
        return switch (clampForm(form)) {
            case PALMATE -> PALM_POINTS[c];
            case CROWN -> CROWN_TINES[c];
            case SPIKE -> c >= 2 ? 1 : 0;
            default -> BRANCHED_TINES[c];
        };
    }

    /**
     * One antler.
     *
     * @param form      one of the habit constants; clamped into range
     * @param variant   {@code 0..VARIANTS-1}; wrapped into range
     * @param sizeClass an {@link AntlerSize} ordinal; clamped into range
     * @param left      the {@code +x} side rather than the {@code -x} one
     */
    public static List<PartNode> generate(int form, int variant, int sizeClass, boolean left) {
        int f = clampForm(form);
        int c = clampClass(sizeClass);
        int v = Math.floorMod(variant, VARIANTS);
        // The side flips the sign of every sideways lean and nothing else, and the
        // RNG never sees it - so the two sides of a symmetric rack are one antler
        // and its mirror image, draw for draw.
        float s = left ? 1f : -1f;
        Rng r = new SeededRng(seedOf(f, v));

        float length = AntlerSize.values()[c].length();
        int segs = switch (f) {
            case SPIKE, PALMATE -> Math.max(2, BEAM_SEGMENTS[c] - 1);
            default -> BEAM_SEGMENTS[c];
        };
        float beamLength = switch (f) {
            case SPIKE -> length * 0.75f;
            case PALMATE -> length * 0.65f;
            case BROW -> length * 1.1f;
            default -> length;
        };
        float segLen = beamLength / segs;
        float baseGirth = Math.min(3.0f, 1.2f + 0.06f * length);

        List<PartNode> out = new ArrayList<>();
        int[] beam = new int[segs];
        float[] beamGirth = new float[segs];

        // ---- the beam -----------------------------------------------------
        for (int i = 0; i < segs; i++) {
            float g = Math.max(MIN_GIRTH, baseGirth * (1f - 0.45f * i / segs));
            if (f == PALMATE && i >= segs - 2) {
                // The palm: the last two segments are broad. A box has a square
                // section, so "broad" is thick here and the points along its edge
                // make it read as a plate.
                g = Math.max(g, baseGirth * 1.15f);
            }
            beamGirth[i] = g;
            float jx = jitter(r, 0.06f);
            float jz = jitter(r, 0.06f);
            int tex = i == segs - 1 ? PartSheet.BONE_TIP : PartSheet.BONE;
            beam[i] = out.size();
            if (i == 0) {
                // Off the pedicle: back over the poll and out over the ear.
                float rx = switch (f) {
                    case BROW -> -0.55f;
                    case PALMATE, SPIKE -> -0.15f;
                    default -> -0.32f;
                };
                float rz = switch (f) {
                    case PALMATE -> 0.95f;
                    case BROW -> 0.25f;
                    case SPIKE -> 0.18f;
                    default -> 0.32f;
                };
                out.add(new PartNode(-1, 0f, 0f, 0f, 0f, rx + jx, 0f, s * (rz + jz),
                        segLen, g, tex, PartNode.NO_GROUP));
            } else {
                boolean upper = i >= segs / 2;
                float rx = switch (f) {
                    // The C-curve: swept back off the head, then forward over the top.
                    case BROW -> 0.26f;
                    case FORK -> upper ? 0.16f : -0.10f;
                    case CROWN -> upper ? 0.10f : -0.06f;
                    case PALMATE -> -0.05f;
                    default -> 0f;
                };
                float rz = switch (f) {
                    // A palm turns back up from the horizontal toward its points.
                    case PALMATE -> -0.30f;
                    case SPIKE -> 0f;
                    default -> 0.05f;
                };
                out.add(new PartNode(beam[i - 1], JOIN, 0f, 0f, 0f, rx + jx, 0f, s * (rz + jz),
                        segLen, g, tex, PartNode.NO_GROUP));
            }
        }

        // ---- the tines, base to tip, so the first k are always a sensible k ----
        int tines = maxTines(f, c);
        List<Integer> tips = new ArrayList<>(tines);
        for (int k = 0; k < tines; k++) {
            // Always the same five draws per tine, whatever branch it takes, so a
            // change to one habit's angles never reshuffles another's racks.
            float u1 = r.nextFloat();
            float u2 = r.nextFloat();
            float u3 = r.nextFloat();
            float u4 = r.nextFloat();
            float u5 = r.nextFloat();

            int host;
            float at;
            float rx;
            float rz;
            float len;
            switch (f) {
                case BROW -> {
                    if (k == 0) {
                        // The brow tine: low, and straight forward over the face.
                        host = 0;
                        at = 0.35f + 0.15f * u1;
                        rx = 1.30f + 0.15f * u2;
                        rz = 0.05f + 0.10f * u3;
                        len = segLen * (1.1f + 0.4f * u4);
                    } else {
                        // The rest high on the beam, pointing back and up.
                        int from = segs / 2;
                        host = Math.min(segs - 1, from + (k - 1) * (segs - from) / Math.max(1, tines - 1));
                        at = 0.30f + 0.60f * u1;
                        rx = -0.55f - 0.30f * u2;
                        rz = 0.15f + 0.25f * u3;
                        len = segLen * (0.7f + 0.6f * u4);
                    }
                }
                case PALMATE -> {
                    // Points round the palm's outer edge, fanned from front to back.
                    host = segs - 2 + (k * 2) / Math.max(1, tines);
                    at = 0.20f + 0.70f * ((k * 2) % Math.max(1, tines) + u1) / Math.max(1, tines);
                    float fan = tines == 1 ? 0f : (float) k / (tines - 1);
                    rx = 0.70f - 1.40f * fan + (u2 * 2f - 1f) * 0.10f;
                    rz = -0.75f - 0.25f * u3;
                    len = segLen * (0.35f + 0.30f * u4);
                }
                case CROWN -> {
                    if (k < 2) {
                        // Brow and bez, low and forward, as on any full rack.
                        host = Math.min(segs - 1, k);
                        at = 0.40f + 0.40f * u1;
                        rx = 0.85f + 0.25f * u2;
                        rz = 0.10f + 0.20f * u3;
                        len = segLen * (0.9f + 0.5f * u4);
                    } else {
                        // The cup: everything else off the top, splayed every way.
                        host = segs - 1;
                        at = 0.70f + 0.25f * u1;
                        boolean forward = (k & 1) == 0;
                        rx = forward ? 0.50f + 0.30f * u2 : -0.45f - 0.30f * u2;
                        rz = -0.30f + 0.80f * u3;
                        len = segLen * (0.6f + 0.5f * u4);
                    }
                }
                case SPIKE -> {
                    // The one short fork, near the top.
                    host = segs - 1;
                    at = 0.75f + 0.15f * u1;
                    rx = 0.55f + 0.20f * u2;
                    rz = 0.15f + 0.15f * u3;
                    len = segLen * (0.45f + 0.25f * u4);
                }
                default -> {
                    // Forked: spread up the beam, forward and a little out, the
                    // lower ones longer - the brow and bez of a red deer.
                    host = Math.min(segs - 1, k * segs / Math.max(1, tines));
                    at = 0.40f + 0.50f * u1;
                    rx = 0.65f + 0.35f * u2;
                    rz = 0.08f + 0.25f * u3;
                    len = segLen * (0.8f + 0.6f * u4) * (1f - 0.06f * k);
                }
            }
            len = Math.max(1.2f, len);
            float g = Math.max(MIN_GIRTH, beamGirth[host] * 0.6f);
            boolean twoSegments = len > segLen * 1.2f && c >= 2;
            float part = twoSegments ? len / 2f : len;
            int first = out.size();
            out.add(new PartNode(beam[host], at, 0f, 0f, 0f, rx, 0f, s * rz,
                    part, g, twoSegments ? PartSheet.BONE : PartSheet.BONE_TIP, k));
            int tip = first;
            if (twoSegments) {
                // A long tine curls up toward its point.
                tip = out.size();
                out.add(new PartNode(first, JOIN, 0f, 0f, 0f, -0.25f + 0.10f * u5, 0f, s * 0.05f,
                        part, Math.max(MIN_GIRTH, g * 0.7f), PartSheet.BONE_TIP, k));
            }
            tips.add(tip);
        }

        // ---- the bloom clumps, drawn only on a blooming horse ----------------
        // One on each tine, in that tine's group so it hides with it, and a few on
        // the beam itself.
        for (int k = 0; k < tips.size(); k++) {
            float u = r.nextFloat();
            out.add(new PartNode(tips.get(k), 0.45f, 0f, 0f, 0f, 0.4f * (u - 0.5f), 0f, s * 0.6f,
                    1.3f, 1.5f, PartSheet.BLOOM, k));
        }
        for (int i = 1; i < segs; i += 2) {
            float u = r.nextFloat();
            out.add(new PartNode(beam[i], 0.5f, 0f, 0f, 0f, 0.3f * (u - 0.5f), 0f, s * 1.2f,
                    1.4f, 1.7f, PartSheet.BLOOM, PartNode.NO_GROUP));
        }
        out.add(new PartNode(beam[segs - 1], 0.85f, 0f, 0f, 0f, 0.2f, 0f, s * 0.4f,
                1.2f, 1.5f, PartSheet.BLOOM, PartNode.NO_GROUP));

        if (out.size() > MAX_NODES) {
            // Unreachable by construction - the tables above cap it - and asserted
            // over every shape by PartGeneratorTest. Truncating would orphan nothing
            // (parents come first), but it would drop the bloom without saying so.
            throw new IllegalStateException("antler of " + out.size() + " boxes exceeds " + MAX_NODES);
        }
        return out;
    }

    /** The seed one habit's {@code variant} is grown from. Never sees the side. */
    private static long seedOf(int form, int variant) {
        return 0xA17E2L ^ (form * 0x9E3779B97F4A7C15L) ^ ((variant + 1) * 0xC2B2AE3D27D4EB4FL);
    }

    private static float jitter(Rng r, float reach) {
        return (r.nextFloat() * 2f - 1f) * reach;
    }

    private static int clampForm(int form) {
        return Math.max(0, Math.min(FORMS - 1, form));
    }

    private static int clampClass(int sizeClass) {
        return Math.max(0, Math.min(AntlerSize.classes() - 1, sizeClass));
    }
}
