package com.example.horsegenetics.common.coat.pattern;

/**
 * <b>Phase 1</b> of the coat pipeline: per texel, how much red (pheomelanin)
 * and black (eumelanin) pigment survives, each in {@code [0, 1]}.
 *
 * <p>Every texel starts at {@code (red = 1, black = 1)} - a maximally
 * pigmented black horse. Natural genes then knock pigment down
 * ({@link #restrictRed}/{@link #restrictBlack} multiply it toward 0,
 * {@link #setRed}/{@link #setBlack} clamp it, {@link #dilute} does both and
 * walks the sample sideways, {@link #whiten} mixes in white hair). Pigment only
 * ever comes off; nothing here can
 * add colour, which is what the magical {@link ColorField} is for.
 *
 * <p>{@link CoatTextureComposer} then resolves each texel through the
 * red/black gradient into that colour field.
 *
 * <p>A gene never mutates the field it was handed - it works on a
 * {@link PigmentView#mutableCopy()} and returns that. See {@code Gene#restrict}.
 */
public final class PigmentField implements PigmentView {

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

    @Override
    public int size() {
        return size;
    }

    @Override
    public float red(int px, int py) {
        return red[py * size + px];
    }

    @Override
    public float black(int px, int py) {
        return black[py * size + px];
    }

    @Override
    public PigmentField mutableCopy() {
        PigmentField copy = new PigmentField(size);
        System.arraycopy(red, 0, copy.red, 0, red.length);
        System.arraycopy(black, 0, copy.black, 0, black.length);
        return copy;
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

    /**
     * The <b>cool</b> dilution, and the exact opposite move to {@link #dilute}:
     * take the red down <i>first</i> - all of it, wherever black was masking it
     * - and only then take the black down. Where {@link #dilute} deliberately
     * walks a diluted black into the warm browns, this one keeps it on the
     * gradient's neutral column, which is where a real grullo lives: a
     * blue-grey mouse colour, not a mouse-brown one.
     *
     * <p>The order is the whole of it. A black horse is {@code (red = 1,
     * black = 1)} - a full load of pheomelanin that the eumelanin above it
     * hides, visible nowhere but on the gradient's bottom row. Scale the two
     * together and the black comes off <i>first</i>, unmasking that red on the
     * way out and walking the sample diagonally into the golds; a "grullo"
     * built that way is a milk-chocolate horse. So this scales the
     * <b>visible</b> red - {@code red * (1 - black)} - and stores back whatever
     * reproduces it against the black that is left:
     *
     * <pre>{@code black' = black * keepBlack
     * red'   = red * (1 - black) * keepRed / (1 - black')}</pre>
     *
     * <p>It is the same invariant {@link #whiten} keeps, for the same reason,
     * with the two pigments free to move by different amounts. On a chestnut
     * ({@code black = 0}) nothing is masked and it collapses to
     * {@code red * keepRed}; on a black horse the numerator is 0, red goes
     * straight to 0 and the sample slides <i>down</i> the neutral column. Every
     * base in between keeps exactly as much warmth as was showing before.
     *
     * <p>It is an identity at {@code keepRed = keepBlack = 1}. On a texel
     * already at {@code black = 1} that is left undiluted there is no room to
     * store red at all - and none is needed, since the gradient's bottom row is
     * black whatever the red says - so the stored value is left alone rather
     * than flushed to 0, and the identity holds there too.
     */
    public void diluteNeutral(int px, int py, float keepRed, float keepBlack) {
        int i = py * size + px;
        float b = black[i];
        float visibleRed = red[i] * (1.0f - b);
        float newBlack = clamp01(b * keepBlack);
        float room = 1.0f - newBlack;
        black[i] = newBlack;
        if (room > 1e-4f) {
            red[i] = clamp01(visibleRed * keepRed / room);
        }
    }

    /**
     * The shared <b>whitening</b> move: mix white hair into this texel by
     * {@code amount}, 0 leaving it alone and 1 taking it to bald white. Every
     * white marking - a soft patch edge, a roan fleck, a varnished appaloosa
     * texel - goes through here rather than scaling the two pigments itself.
     *
     * <p><b>Why it is not just {@code red *= keep; black *= keep}.</b> A black
     * horse is {@code (red = 1, black = 1)}: it carries a full load of
     * pheomelanin that is simply <i>masked</i> - the gradient's whole bottom row
     * is {@code #000000}, so the red is invisible at {@code black = 1} and
     * nowhere else. Scaling both pigments together unmasks it on the way out,
     * and the sample walks the diagonal through the golds: the old roan ramp
     * put {@code #5F330B} and {@code #885517} - milk chocolate and tan - around
     * the edge of every marking on an otherwise jet-black horse.
     *
     * <p>So the invariant this keeps is the <b>visible</b> red, not the stored
     * red. Red only shows through where black is absent, i.e.
     * {@code visible = red * (1 - black)}; whitening scales <i>that</i> by
     * {@code keep} alongside the black, and the stored red is whatever
     * reproduces it against the black that is left:
     *
     * <pre>{@code black' = black * keep
     * red'   = red * (1 - black) * keep / (1 - black')}</pre>
     *
     * On a black horse the numerator is 0, so red drops straight to 0 and the
     * fade runs down the gradient's {@code red = 0} column - the one neutral
     * ramp in the chart, {@code #212121 -> #414142 -> #A6A6A7 -> #FFFFFF}.
     * Shades of grey, which is what white hairs through black hairs look like.
     * On a chestnut ({@code black = 0}) nothing is masked, the expression
     * collapses to {@code red * keep}, and strawberry roan is untouched. Every
     * base in between - a bay's body, a smoky black, a diluted point - keeps
     * exactly as much warmth as was showing before.
     *
     * <p>It is an identity at {@code amount = 0}, which is what lets a soft edge
     * meet unmarked coat without a seam.
     */
    public void whiten(int px, int py, float amount) {
        if (amount <= 0f) {
            return;
        }
        int i = py * size + px;
        float keep = 1.0f - clamp01(amount);
        float b = black[i];
        float visibleRed = red[i] * (1.0f - b);
        float newBlack = b * keep;
        float room = 1.0f - newBlack;
        red[i] = room <= 1e-4f ? 0f : clamp01(visibleRed * keep / room);
        black[i] = clamp01(newBlack);
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
