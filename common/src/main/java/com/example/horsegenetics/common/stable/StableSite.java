package com.example.horsegenetics.common.stable;

/**
 * <b>Is this ground level enough to put a stable on, and at what height?</b>
 *
 * <p>A generated stable is one rigid piece, and vanilla's jigsaw gives the whole
 * piece <b>one</b> height: the surface under its centre. Nothing looks at the
 * rest of the footprint, so a big stable started on a hilltop ran on over the
 * valley beside it, and the beard under it (which only fills a limited depth)
 * left a thin hollow slab with open air and mobs below. That is issue #2.
 *
 * <p>The rule, the owner's pick from that issue's options: sample the ground at
 * the footprint's corners and centre. If the highest and lowest samples are more
 * than {@code maxSpread} blocks apart, the site is rejected and that chunk gets
 * no stable. Otherwise the stable's ground sits at the <b>lowest</b> sample, so
 * the high side is cut into the slope and the beard has at most
 * {@code maxSpread} blocks to fill rather than a valley. This is the woodland
 * mansion's approach.
 *
 * <p>Heights are "first free" heights (the first air block above the
 * surface), which is what the jigsaw projects a start piece's ground onto.
 */
public final class StableSite {

    private StableSite() {
    }

    /**
     * What {@link #fit} decided.
     *
     * @param accepted whether the stable may go here
     * @param spread   highest sample minus lowest, in blocks
     * @param drop     blocks to move the stable by (zero or negative): the lowest
     *                 sample minus the centre's. Meaningful only when accepted.
     */
    public record Fit(boolean accepted, int spread, int drop) {
    }

    /**
     * @param samples     ground heights under the footprint, the centre among them
     * @param centre      the ground height the stable currently stands at
     * @param maxSpread   the most uneven ground still built on
     */
    public static Fit fit(int[] samples, int centre, int maxSpread) {
        if (samples.length == 0) {
            throw new IllegalArgumentException("no ground samples");
        }
        int low = samples[0];
        int high = samples[0];
        for (int h : samples) {
            low = Math.min(low, h);
            high = Math.max(high, h);
        }
        int spread = high - low;
        return new Fit(spread <= maxSpread, spread, Math.min(0, low - centre));
    }
}
