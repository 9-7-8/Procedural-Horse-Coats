package com.example.horsegenetics.common.parts;

import com.example.horsegenetics.common.Rng;
import java.util.ArrayList;
import java.util.List;

/**
 * A cluster of crystals rooted along the spine. Each crystal is two shaft boxes crossed at
 * 45 degrees (which reads as an eight-sided prism), a narrower cap box and a point box on
 * top - six boxes for a crystal that looks faceted from any side. Lean grows with distance
 * from the spine's midline, so the cluster fans out the way a real druse does.
 *
 * <p>Root offsets are along z on the body bone (front is -z); {@link PartAnchor#SPINE}
 * puts the origin at the top of the body, so y = 0 sits on the back.
 */
public final class CrystalGenerator {

    public static final int MAX_CRYSTALS = 7;

    private CrystalGenerator() {
    }

    /**
     * @param count  crystals in the cluster, 1..{@link #MAX_CRYSTALS}
     * @param size   0..1 scales every crystal's length
     */
    public static List<PartNode> generate(Rng r, int count, double size) {
        int n = Math.max(1, Math.min(MAX_CRYSTALS, count));
        double s = Math.max(0.0, Math.min(1.0, size));
        List<PartNode> out = new ArrayList<>();
        for (int c = 0; c < n; c++) {
            float across = n == 1 ? 0f : (float) c / (n - 1) * 2f - 1f;          // -1 .. 1 along the spine
            float z = across * (1.5f + 1.0f * n);                                 // units fore-aft
            float x = (r.nextFloat() - 0.5f) * 3.0f;                              // scatter off the midline
            float len = (float) ((3.5 + 5.0 * s) * (0.55 + 0.7 * r.nextFloat()) * (1.0 - 0.35 * Math.abs(across)));
            float girth = Math.max(1.0f, len * (0.22f + 0.06f * r.nextFloat()));
            float lean = 0.15f + 0.5f * Math.abs(across) * r.nextFloat();
            float leanX = across * 0.6f * r.nextFloat();                          // fore-aft lean, away from the middle
            float rz = (x >= 0 ? -1f : 1f) * lean;
            int shaftA = out.size();
            out.add(new PartNode(-1, 0f, x, 0f, z, leanX, 0f, rz, len, girth, PartSheet.CRYSTAL));
            out.add(new PartNode(-1, 0f, x, 0f, z, leanX, (float) (Math.PI / 4), rz, len, girth, PartSheet.CRYSTAL));
            // cap and point ride on shaft A, so they lean with it
            int cap = out.size();
            out.add(new PartNode(shaftA, 1.0f, 0f, 0f, 0f, 0f, (float) (Math.PI / 8), 0f, len * 0.28f, girth * 0.72f, PartSheet.CRYSTAL_CORE));
            out.add(new PartNode(cap, 1.0f, 0f, 0f, 0f, 0f, 0f, 0f, len * 0.18f, Math.max(0.5f, girth * 0.36f), PartSheet.CRYSTAL_CORE));
        }
        return out;
    }
}
