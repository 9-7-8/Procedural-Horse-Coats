package com.example.horsegenetics.common.coat.pattern;

/**
 * The <b>overlay layer</b> the genes build up: per texel, how much red
 * (pheomelanin) and black (eumelanin) pigment survives, each in {@code [0, 1]}.
 *
 * <p>Every texel starts at {@code (red = 1, black = 1)} - a maximally
 * pigmented black horse. Genes then knock pigment down
 * ({@link #restrictRed}/{@link #restrictBlack} multiply it toward 0,
 * {@link #setRed}/{@link #setBlack} clamp it). {@link CoatTextureComposer}
 * later reads each texel and looks the colour up in the red/black gradient.
 */
public final class PigmentField {

    private final int size;
    private final float[] red;
    private final float[] black;

    public PigmentField(int size) {
        this.size = size;
        this.red = new float[size * size];
        this.black = new float[size * size];
        java.util.Arrays.fill(red, 1.0f);
        java.util.Arrays.fill(black, 1.0f);
    }

    public int size() {
        return size;
    }

    public float red(int px, int py) {
        return red[py * size + px];
    }

    public float black(int px, int py) {
        return black[py * size + px];
    }

    public void setRed(int px, int py, float value) {
        red[py * size + px] = clamp01(value);
    }

    public void setBlack(int px, int py, float value) {
        black[py * size + px] = clamp01(value);
    }

    /** {@code red *= (1 - amount)} - {@code amount} 0 keeps it, 1 removes it. */
    public void restrictRed(int px, int py, float amount) {
        int i = py * size + px;
        red[i] = clamp01(red[i] * (1.0f - amount));
    }

    public void restrictBlack(int px, int py, float amount) {
        int i = py * size + px;
        black[i] = clamp01(black[i] * (1.0f - amount));
    }

    /**
     * The shared <b>dilution</b> move: {@code black *= keepBlack} and
     * {@code red = red * keepRed + blackBefore * blackTint}.
     *
     * <p>The {@code blackTint} term is the part that matters and the reason a
     * plain {@code restrictBlack} is not enough. Bay paints its points
     * <i>absolutely</i> - {@code red = 0}, {@code black = 1} - and the gradient's
     * zero-red column stays visually black all the way down to {@code black
     * ~0.4}, so a dilution that only scales black leaves a "diluted" point
     * indistinguishable from jet black (single cream's {@code keepBlack = 0.7}
     * landed on {@code #111111}). Feeding a fraction of the eumelanin that was
     * removed back in as pheomelanin walks the sample <b>left-to-right off that
     * column</b>, into the warm browns where a real diluted black lives - amber
     * champagne's chocolate points, perlino's rusty ones.
     */
    public void dilute(int px, int py, float keepRed, float keepBlack, float blackTint) {
        int i = py * size + px;
        float b = black[i];
        red[i] = clamp01(red[i] * keepRed + b * blackTint);
        black[i] = clamp01(b * keepBlack);
    }

    /** Visit every texel (mapped or not). */
    public void forEach(PixelOp op) {
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                op.at(px, py);
            }
        }
    }

    @FunctionalInterface
    public interface PixelOp {
        void at(int px, int py);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
