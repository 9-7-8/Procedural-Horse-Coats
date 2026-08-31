package com.example.horsegenetics.common.coat.pattern;

import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry;
import com.example.horsegenetics.common.coat.skin.HorseSkinGeometry.Bounds;

/**
 * The pattern for the diagnostic <b>Test</b> gene ({@code T}): a two-way
 * gradient over the whole horse -
 * <ul>
 *   <li><b>pink -&gt; blue</b> along body-space X (rear of the tail -&gt; nose),</li>
 *   <li><b>red -&gt; yellow</b> along body-space Y (hooves -&gt; topline),</li>
 *   <li>no variation along Z.</li>
 * </ul>
 * The two gradient colours at a point are averaged per channel, so the whole
 * animal reads as one smooth field with no seams between parts - which is the
 * property {@link HorseSkinGeometry} exists to guarantee. It's meant to be
 * unmistakable while checking the projection engine, not pretty.
 *
 * <p>Normalised against {@link HorseSkinGeometry#bodyBounds()} so the endpoints
 * really do sit at the extremes of the model.
 */
public final class TestCoatPattern implements CoatPattern {

    // 0xRRGGBB endpoints.
    private static final int PINK = 0xFF69B4;
    private static final int BLUE = 0x0000FF;
    private static final int RED = 0xFF0000;
    private static final int YELLOW = 0xFFFF00;

    private final Bounds bounds;

    public TestCoatPattern() {
        this(HorseSkinGeometry.bodyBounds());
    }

    /** Package-visible for tests that want to pin the normalisation range. */
    TestCoatPattern(Bounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public int argb(double x, double y, double z) {
        double fx = clamp01((x - bounds.xMin()) / (bounds.xMax() - bounds.xMin()));
        double fy = clamp01((y - bounds.yMin()) / (bounds.yMax() - bounds.yMin()));
        int alongX = lerpRgb(PINK, BLUE, fx);
        int alongY = lerpRgb(RED, YELLOW, fy);
        return 0xFF000000 | averageRgb(alongX, alongY);
    }

    private static int lerpRgb(int from, int to, double t) {
        int r = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int averageRgb(int a, int b) {
        int r = (((a >> 16) & 0xFF) + ((b >> 16) & 0xFF)) / 2;
        int g = (((a >> 8) & 0xFF) + ((b >> 8) & 0xFF)) / 2;
        int bl = ((a & 0xFF) + (b & 0xFF)) / 2;
        return (r << 16) | (g << 8) | bl;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
