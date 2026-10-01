package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>One ram's horn</b> - a heavy, tapering, ridged chain of boxes - grown from four
 * integers. A pair is two of these, one per side, each its own mesh, for the reason
 * {@link AntlerGenerator} gives: a mirrored copy by negative scale would turn culled
 * boxes inside out.
 *
 * <h2>The four shapes</h2>
 * <ul>
 *   <li>{@link #CURL} - the ram's spiral. Every segment turns the same amount
 *       backward, so the chain goes up, back, down past the ear and forward again
 *       - a circle in the side view - and a small outward lean per segment opens the
 *       circle into a spiral that clears the face. How far round it goes is the
 *       {@code curl} bucket: a little over half a turn to more than a full one.</li>
 *   <li>{@link #CORKSCREW} - long, straight and twisted, rising up and out. The twist
 *       is a roll per segment on segments with no bend, the {@link HornGenerator}
 *       rule for a twist that does not coil; the {@code curl} bucket sets how tight.</li>
 *   <li>{@link #FOUR} - two horns per side, like a Jacob sheep: a short upright one,
 *       and a curl hanging from the same root and sweeping down. One root, so one
 *       tree - the lower horn's first box sits at the base of the upper one.</li>
 *   <li>{@link #SCURS} - two or three loose stubs, the half-horned state.</li>
 * </ul>
 *
 * <h2>No count, no seed</h2>
 * Unlike an antler a horn has nothing to count, and two rams' horns of one shape
 * differ by how far they curl, how long and how thick they are, and their colour -
 * so the mesh key is shape x curl bucket x size class, and length, girth and colour
 * are draw-time. Colour runs root to tip ({@code AttachedPart.tintAt}), which is how a
 * horn can be black at the base and any colour at the point.
 *
 * <p>Angles are radians in the parent's frame ({@link PartNode}): {@code -y} up,
 * {@code -z} forward, a positive {@code rx} tips forward, a positive {@code rz}
 * swings toward {@code +x}. No box sets both {@code rx} and {@code ry}.
 * <b>Geometry only - never seen in a running game</b> when written.
 */
public final class RamHornGenerator {

    /** The ram's spiral. The ordinary shape. */
    public static final int CURL = 0;
    /** Long, straight and twisted. */
    public static final int CORKSCREW = 1;
    /** Two horns a side - one up, one curled down. */
    public static final int FOUR = 2;
    /** Loose stubs. */
    public static final int SCURS = 3;

    /** How many shapes. In {@code ram_horn_form}'s allele order. */
    public static final int FORMS = 4;

    /** Curl buckets - how far round a curl goes, how tight a corkscrew twists. */
    public static final int CURLS = 4;

    /** Hard cap on boxes per horn. */
    public static final int MAX_NODES = 24;

    /** Total turn of a curl, in degrees, by bucket. */
    private static final float[] CURL_DEGREES = {200f, 260f, 320f, 390f};

    /** Roll per corkscrew segment, in degrees, by bucket - the {@link HornGenerator} ceiling is 34. */
    private static final float[] TWIST_DEGREES = {12f, 20f, 27f, 34f};

    /** Segments per size class for a full horn. */
    private static final int[] SEGMENTS = {4, 6, 8, 10, 12};

    private static final float MIN_GIRTH = 0.6f;
    private static final float JOIN = 0.93f;

    private RamHornGenerator() {
    }

    /**
     * One horn.
     *
     * @param form      one of the shape constants; clamped
     * @param curl      {@code 0..CURLS-1}; clamped
     * @param sizeClass a {@link RamHornSize} ordinal; clamped
     * @param left      the {@code +x} side rather than the {@code -x} one
     */
    public static List<PartNode> generate(int form, int curl, int sizeClass, boolean left) {
        int f = Math.max(0, Math.min(FORMS - 1, form));
        int k = Math.max(0, Math.min(CURLS - 1, curl));
        int c = Math.max(0, Math.min(RamHornSize.classes() - 1, sizeClass));
        float s = left ? 1f : -1f;
        float length = RamHornSize.values()[c].length();
        List<PartNode> out = new ArrayList<>();
        switch (f) {
            case CORKSCREW -> chain(out, -1, 0f, SEGMENTS[c], length * 0.9f, length, -0.20f, s * 0.55f,
                    0f, 0f, (float) Math.toRadians(TWIST_DEGREES[k]), 0.75f);
            case FOUR -> {
                // The upright horn first, so it holds the root.
                int upper = Math.max(2, SEGMENTS[c] / 2);
                chain(out, -1, 0f, upper, length * 0.45f, length, -0.10f, s * 0.30f,
                        -0.08f, s * 0.04f, 0f, 0.65f);
                // The curled one from the same root, starting back and down.
                curl(out, 0, 0f, SEGMENTS[c], length * 0.85f, length * 0.9f, k, s, -1.10f);
            }
            case SCURS -> chain(out, -1, 0f, 2 + c / 2, Math.min(4.5f, length * 0.22f), length * 0.7f,
                    -0.25f, s * 0.45f, 0.15f, s * 0.10f, 0f, 0.55f);
            default -> curl(out, -1, 0f, SEGMENTS[c], length, length, k, s, -0.55f);
        }
        return out;
    }

    /** A spiral: a constant backward bend per segment with a small outward lean. */
    private static void curl(List<PartNode> out, int parent, float at, int segs, float reach, float girthBasis,
                             int k, float s, float rootRx) {
        float bend = -(float) Math.toRadians(CURL_DEGREES[k]) / segs;
        chain(out, parent, at, segs, reach, girthBasis, rootRx, s * 0.55f, bend, s * 0.07f, 0f, 0.70f);
    }

    /**
     * A tapering chain of {@code segs} boxes {@code reach} long, its root turned
     * {@code (rootRx, rootRz)} and every later box {@code (rx, ry, rz)} off its parent.
     */
    private static void chain(List<PartNode> out, int parent, float at, int segs, float reach, float girthBasis,
                              float rootRx, float rootRz, float rx, float rz, float ry, float taper) {
        float segLen = reach / segs;
        float base = Math.min(3.6f, 1.6f + 0.07f * girthBasis);
        int prev = parent;
        for (int i = 0; i < segs; i++) {
            float g = Math.max(MIN_GIRTH, base * (1f - taper * i / segs));
            int tex = i >= segs - 2 ? PartSheet.RAM_TIP : PartSheet.RAM_HORN;
            int me = out.size();
            if (i == 0) {
                out.add(new PartNode(prev, at, 0f, 0f, 0f, rootRx, 0f, rootRz, segLen, g, tex));
            } else {
                // A twist rolls with ry and must not also bend (see PartNode); a curl
                // bends with rx and never rolls.
                out.add(new PartNode(prev, JOIN, 0f, 0f, 0f, ry != 0f ? 0f : rx, ry, ry != 0f ? 0f : rz,
                        segLen, g, tex));
            }
            prev = me;
        }
    }
}
