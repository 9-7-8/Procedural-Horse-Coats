package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Sample;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCoatPatternTest {

    private static final Bounds B = HorseSkinGeometry.bodyBounds();
    private final TestCoatPattern pattern = new TestCoatPattern();

    private static int r(int argb) { return (argb >> 16) & 0xFF; }
    private static int g(int argb) { return (argb >> 8) & 0xFF; }
    private static int b(int argb) { return argb & 0xFF; }

    @Test
    void alwaysOpaque() {
        assertEquals(0xFF, pattern.argb(0, 0, 0) >>> 24);
        assertEquals(0xFF, pattern.argb(B.xMax(), B.yMax(), B.zMax()) >>> 24);
    }

    @Test
    void movingTowardTheNoseGoesPinkToBlue() {
        double y = (B.yMin() + B.yMax()) / 2;
        int tail = pattern.argb(B.xMin(), y, 0);
        int nose = pattern.argb(B.xMax(), y, 0);
        assertTrue(b(nose) > b(tail), "blue channel should rise toward the nose");
        assertTrue(r(nose) < r(tail), "red channel should fall toward the nose");
    }

    @Test
    void movingUpGoesRedToYellow() {
        double x = (B.xMin() + B.xMax()) / 2;
        int hoof = pattern.argb(x, B.yMin(), 0);
        int back = pattern.argb(x, B.yMax(), 0);
        // red -> yellow is a rising green channel
        assertTrue(g(back) > g(hoof), "green channel should rise toward the topline");
    }

    @Test
    void noChangeAlongZ() {
        int left = pattern.argb(10, 10, B.zMin());
        int centre = pattern.argb(10, 10, 0);
        int right = pattern.argb(10, 10, B.zMax());
        assertEquals(left, centre);
        assertEquals(centre, right);
    }

    @Test
    void gradientIsMonotonicAlongEachAxis() {
        double y = (B.yMin() + B.yMax()) / 2;
        int prevB = -1;
        for (double x = B.xMin(); x <= B.xMax(); x += (B.xMax() - B.xMin()) / 20) {
            int cur = b(pattern.argb(x, y, 0));
            assertTrue(cur >= prevB, "blue must not decrease toward the nose");
            prevB = cur;
        }
        double x = (B.xMin() + B.xMax()) / 2;
        int prevG = -1;
        for (double yy = B.yMin(); yy <= B.yMax(); yy += (B.yMax() - B.yMin()) / 20) {
            int cur = g(pattern.argb(x, yy, 0));
            assertTrue(cur >= prevG, "green must not decrease toward the topline");
            prevG = cur;
        }
    }

    @Test
    void bakeFillsMostOfTheSheetOpaquelyAndMatchesThePattern() {
        int[] argb = CoatSheetRasterizer.bake(pattern);
        int n = HorseSkinGeometry.SHEET_SIZE;
        assertEquals(n * n, argb.length);

        int painted = 0;
        for (int py = 0; py < n; py++) {
            for (int px = 0; px < n; px++) {
                int px_argb = argb[py * n + px];
                Optional<Sample> s = HorseSkinGeometry.sample(px, py);
                if (s.isEmpty()) {
                    assertEquals(0, px_argb, "unmapped texel (" + px + "," + py + ") should be transparent");
                    continue;
                }
                painted++;
                assertEquals(0xFF, px_argb >>> 24, "mapped texel must be opaque");
                var p = s.get().point();
                assertEquals(pattern.argb(p.x(), p.y(), p.z()), px_argb,
                        "baked colour must equal the pattern at that body point");
            }
        }
        assertTrue(painted > 8000, "expected most of the sheet painted, got " + painted);
    }
}
