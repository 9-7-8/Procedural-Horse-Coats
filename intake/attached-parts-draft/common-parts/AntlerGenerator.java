package com.example.horsegenetics.common.parts;

import com.example.horsegenetics.common.Rng;
import java.util.ArrayList;
import java.util.List;

/**
 * A mirrored pair of antlers grown by a tiny recursive rule: a beam is a short chain that
 * sweeps back and out; at some segments a tine forks off forward and outward; a long tine
 * may fork once more. Seed and size are the only inputs, so every horse's rack is its own
 * and still unmistakably an antler - geometry that is unbounded the way a coat is.
 *
 * <p>Angles are radians in the parent's frame. -y is up, -z is forward, so a positive
 * {@code rx} leans a limb forward and a positive {@code rz} swings its tip toward -x
 * (this horse's right); the left antler is the same list with the signs of {@code ry} and
 * {@code rz} flipped and its root moved to +x.
 */
public final class AntlerGenerator {

    private static final int MAX_DEPTH = 2;
    /** Hard cap on boxes; a herd of these must stay cheap. See "Cost per frame". */
    public static final int MAX_NODES = 44;

    private AntlerGenerator() {
    }

    /**
     * @param size   0..1: 0 is a spike-and-fork yearling, 1 a many-tined stag
     */
    public static List<PartNode> generate(Rng rng, double size) {
        double s = Math.max(0.0, Math.min(1.0, size));
        List<PartNode> out = new ArrayList<>();
        // one RNG draw list, replayed for the mirror, so both sides are the same antler
        long sideSeed = rng.nextLong();
        for (int side = -1; side <= 1; side += 2) {
            Rng r = new com.example.horsegenetics.common.SeededRng(sideSeed);
            float beam = (float) (8.0 + 10.0 * s);
            float girth = (float) (1.4 + 0.7 * s);
            int segs = 3 + (int) Math.round(2.0 * s);
            int tinesWanted = 1 + (int) Math.round(3.0 * s);
            grow(out, r, side, -1, beam / segs, girth, segs, tinesWanted, 0, true);
        }
        return out.size() > MAX_NODES ? new ArrayList<>(out.subList(0, MAX_NODES)) : out;
    }

    private static void grow(List<PartNode> out, Rng r, int side, int parent, float segLen, float girth,
                             int segs, int tines, int depth, boolean isBeam) {
        int prev = parent;
        int tinesLeft = tines;
        for (int i = 0; i < segs; i++) {
            float g = Math.max(0.6f, girth * (1.0f - 0.55f * i / segs));
            int tex = (i >= segs - 1) ? PartSheet.BONE_TIP : PartSheet.BONE;
            int me = out.size();
            if (prev < 0) {
                // root, behind the ear, pointing up and a little out and back
                out.add(new PartNode(-1, 0f, side * 2.0f, 0f, 0f,
                        -0.20f, 0f, side * 0.22f, segLen, g, tex));
            } else if (i == 0) {
                // first segment of a tine: forward and outward off its parent, from a chosen point along it
                out.add(new PartNode(prev, 0.55f + 0.35f * r.nextFloat(), 0f, 0f, 0f,
                        0.55f + 0.3f * r.nextFloat(), 0f, side * (0.35f + 0.25f * r.nextFloat()), segLen, g, tex));
            } else {
                // continuing segment: sweep back a touch and out - the beam's curve
                out.add(new PartNode(prev, 0.9f, 0f, 0f, 0f,
                        isBeam ? -0.20f : 0.12f, 0f, side * (isBeam ? 0.05f : 0.10f), segLen, g, tex));
            }
            // Tines fork off this segment, the beam only, from its second segment up
            if (isBeam && i >= 1 && tinesLeft > 0 && out.size() + 4 < MAX_NODES) {
                tinesLeft--;
                float tineLen = segLen * (0.9f + 0.9f * r.nextFloat()) * (1.0f - 0.12f * i);
                int tineSegs = tineLen > segLen * 1.4f && depth < MAX_DEPTH ? 2 : 1;
                growFrom(out, r, side, me, tineLen / tineSegs, girth * 0.62f, tineSegs, depth + 1);
            }
            prev = me;
        }
    }

    private static void growFrom(List<PartNode> out, Rng r, int side, int parent, float segLen, float girth,
                                 int segs, int depth) {
        grow(out, r, side, parent, segLen, girth, segs, 0, depth, false);
    }
}
