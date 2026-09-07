package com.example.horsegenetics.common.coat.pattern;

/**
 * The red/black colour lookup - a wrapper over {@code redblackgradient.png}
 * (assets/horsegenetics/textures/coat/). It maps a pigment level pair to the
 * coat colour at that pixel:
 * <ul>
 *   <li>more <b>red</b> pigment ({@code redLevel} -&gt; 1) samples further
 *       <b>left</b>,</li>
 *   <li>more <b>black</b> pigment ({@code blackLevel} -&gt; 1) samples further
 *       <b>down</b>.</li>
 * </ul>
 * So {@code (1, 1)} is the bottom-left (black), {@code (1, 0)} the top-left
 * (chestnut red), {@code (0, 0)} the top-right (white).
 *
 * <p>Nothing here knows anything else about the artwork, and nothing should.
 * The chart shipped as the default has been replaced more than once and its
 * internal structure changed each time - a description of where its warm zone
 * sits is a comment that goes stale silently. Measure it with the
 * <a href="../../../../../../../wiki/lut-lab.html">LUT lab</a> instead.
 *
 * <p>Pure array maths - the {@code int[]} is loaded by the game module (or a
 * build tool) and handed in.
 */
public final class GradientLut {

    private final int width;
    private final int height;
    private final int[] argb; // row-major, 0xAARRGGBB

    public GradientLut(int[] argb, int width, int height) {
        if (argb.length != width * height) {
            throw new IllegalArgumentException("argb length " + argb.length + " != " + width + "x" + height);
        }
        this.argb = argb.clone();
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /**
     * Where a pigment pair lands on the chart, normalised so 0 is the left edge
     * and 1 the right - {@code redLevel} 1 sits at 0 (the reddest column).
     *
     * <p>This exists so the axis convention is written down <b>once</b>. It is
     * what {@link #sample} scales up, and it is what a tool that wants to draw
     * the region of the chart a coat actually reads from asks for; a caller
     * working it out again is how a viewer ends up mirroring the gradient it is
     * meant to be explaining.
     */
    public static float chartX(float redLevel) {
        return 1.0f - clamp01(redLevel);
    }

    /** Where a pigment pair lands vertically: 0 is the top, 1 the bottom (max black). */
    public static float chartY(float blackLevel) {
        return clamp01(blackLevel);
    }

    /**
     * The red level that lands at chart position {@code x} - the inverse of
     * {@link #chartX}, for a tool asking "what pigment resolves <i>here</i>?"
     * rather than "where does this pigment go?".
     *
     * <p>It lives beside its forward pair on purpose. The axis convention is one
     * fact and it is stated once; an inverse worked out at the call site is a
     * second statement of it, free to disagree, and it would disagree by
     * mirroring the chart - the single most plausible way to get this wrong and
     * the hardest to notice, since a mirrored answer is still a valid-looking
     * number in range.
     */
    public static float redAtChartX(float x) {
        return 1.0f - clamp01(x);
    }

    /** The black level that lands at chart position {@code y}. Inverse of {@link #chartY}. */
    public static float blackAtChartY(float y) {
        return clamp01(y);
    }

    /**
     * A position on the chart, and how close its colour came to what was asked
     * for. {@code distance} is 0 for an exact hit.
     */
    public record Nearest(float chartX, float chartY, int rgb, double distance) {
    }

    /**
     * The chart position whose colour is closest to {@code rgb} - the question
     * "my horse is this colour, so what pigment pair produced it?", which is the
     * one a gene author asks when tuning against reference art.
     *
     * <p>Answering it needs a search rather than arithmetic, because the chart is
     * a painted image and the mapping colour&rarr;position is neither invertible
     * nor even one-to-one: a desaturated chart has whole regions of near-identical
     * greys, and the honest answer to "where is #B0B0B0" is "one of many places,
     * here is the nearest". That is what {@code distance} is for - a large value
     * means the chart has nothing like the colour asked for, and the position
     * returned is not meaningful.
     *
     * <p>Distance is the <b>redmean</b> approximation rather than a plain RGB
     * Euclidean one. It costs three extra multiplies and is markedly better at
     * the thing this is for: telling two similar browns apart the way an eye
     * does, on a chart where most of the interesting range is low-saturation.
     */
    public Nearest nearest(int rgb) {
        int wantR = (rgb >> 16) & 0xFF;
        int wantG = (rgb >> 8) & 0xFF;
        int wantB = rgb & 0xFF;
        int bestIndex = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < argb.length; i++) {
            int c = argb[i];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            double rmean = (wantR + r) * 0.5;
            double dr = wantR - r;
            double dg = wantG - g;
            double db = wantB - b;
            double d = (2 + rmean / 256) * dr * dr + 4 * dg * dg + (2 + (255 - rmean) / 256) * db * db;
            if (d < best) {
                best = d;
                bestIndex = i;
            }
        }
        int x = bestIndex % width;
        int y = bestIndex / width;
        return new Nearest(
                width == 1 ? 0f : (float) x / (width - 1),
                height == 1 ? 0f : (float) y / (height - 1),
                argb[bestIndex] & 0xFFFFFF,
                Math.sqrt(best));
    }

    /** Bilinearly sampled coat colour (0xFFRRGGBB) for a pigment level pair, each clamped to [0,1]. */
    public int sample(float redLevel, float blackLevel) {
        float fx = chartX(redLevel) * (width - 1);   // red max -> x = 0 (left)
        float fy = chartY(blackLevel) * (height - 1); // black max -> y = height-1 (bottom)

        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        int x1 = Math.min(x0 + 1, width - 1);
        int y1 = Math.min(y0 + 1, height - 1);
        float tx = fx - x0;
        float ty = fy - y0;

        int c00 = argb[y0 * width + x0];
        int c10 = argb[y0 * width + x1];
        int c01 = argb[y1 * width + x0];
        int c11 = argb[y1 * width + x1];

        int rr = bilerp(chan(c00, 16), chan(c10, 16), chan(c01, 16), chan(c11, 16), tx, ty);
        int gg = bilerp(chan(c00, 8), chan(c10, 8), chan(c01, 8), chan(c11, 8), tx, ty);
        int bb = bilerp(chan(c00, 0), chan(c10, 0), chan(c01, 0), chan(c11, 0), tx, ty);
        return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
    }

    private static int chan(int argb, int shift) {
        return (argb >> shift) & 0xFF;
    }

    private static int bilerp(int c00, int c10, int c01, int c11, float tx, float ty) {
        float top = c00 + (c10 - c00) * tx;
        float bot = c01 + (c11 - c01) * tx;
        float v = top + (bot - top) * ty;
        int i = Math.round(v);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
