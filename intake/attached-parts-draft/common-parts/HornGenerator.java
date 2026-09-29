package com.example.horsegenetics.common.parts;

import java.util.ArrayList;
import java.util.List;

/**
 * A unicorn horn: a tapering chain of boxes. Each segment is a child of the one below,
 * turned a little about its own axis (twist) and tipped a little forward (curve), so a
 * spiral falls out of the parenting and needs no trigonometry here.
 *
 * <p>Base width grows with length so a narwhal horn is thicker at the root than a nub
 * is, but not proportionally - a horn scaled uniformly would look like a photograph
 * enlarged, not a different animal.
 */
public final class HornGenerator {

    private HornGenerator() {
    }

    /**
     * @param length   model units along the axis
     * @param twist    degrees of yaw added per segment (0 = smooth; ~25 = a clear spiral)
     * @param curve    degrees of forward tip added per segment (0 = straight)
     * @param tilt     radians forward of the head's own up, at the root
     */
    public static List<PartNode> generate(float length, float twist, float curve, float tilt) {
        float base = 1.3f + 0.055f * length;
        int n = Math.max(3, Math.round(length / 1.6f));
        float seg = length / n;
        List<PartNode> nodes = new ArrayList<>();
        float rTwist = (float) Math.toRadians(twist);
        float rCurve = (float) Math.toRadians(curve);
        for (int i = 0; i < n; i++) {
            float f = (float) i / n;
            // linear taper to a 0.6-unit point; never thinner (a sub-1/16 box shimmers)
            float girth = Math.max(0.6f, base * (1.0f - 0.9f * f));
            int tex = i >= n - 2 ? PartSheet.HORN_TIP : PartSheet.HORN;
            if (i == 0) {
                nodes.add(new PartNode(-1, 0f, 0f, 0f, 0f, tilt, 0f, 0f, seg, girth, tex));
            } else {
                // 0.9 not 1.0: a turned child at the exact tip leaves a wedge gap on the outside of the bend
                nodes.add(new PartNode(i - 1, 0.9f, 0f, 0f, 0f, rCurve, rTwist, 0f, seg, girth, tex));
            }
        }
        return nodes;
    }

    public static List<PartNode> generate(HornSize size) {
        return generate(size.length, size == HornSize.NUB ? 0f : 22f, 3f, 0.15f);
    }
}
