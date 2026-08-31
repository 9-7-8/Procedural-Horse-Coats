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
