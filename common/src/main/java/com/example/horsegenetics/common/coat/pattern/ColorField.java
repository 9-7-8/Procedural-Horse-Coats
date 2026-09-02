package com.example.horsegenetics.common.coat.pattern;

/**
 * The <b>phase-3 colour accumulator</b>: per texel a signed {@code int} red,
 * green, blue and opacity. Where {@link PigmentField} is what the natural genes
 * push <i>down</i>, this is what the magical genes push <i>up</i> - added onto
 * the colour the red/black gradient resolved, never replacing the coat
 * underneath unless a gene explicitly asks for flat paint.
 *
 * <p><b>Why signed {@code int} and not a byte.</b> Channels are nominally
 * 0-255 - a gene author writes their numbers as "0-255 is 0-100%" - but the
 * accumulator is a full {@code int} and <b>nothing is capped until
 * {@link #argb}</b>. That headroom is the point: a gene can add so much blue
 * that no combination of other genes pulls it back under 255, so the horse is
 * blue unconditionally and its author never had to know what else the horse
 * carries. A zebra gene is the same trick with the sign flipped - subtract
 * enough from all three channels that the stripe is black over anything.
 *
 * <p><b>Order-independence.</b> Because the accumulation is signed integer
 * addition, phase 3 is associative and exact: two genes that both touch blue
 * give the same answer either way round, and there is no float drift. That
 * holds for {@link #add} only - {@link #set} is a replace, and replaces are
 * order-dependent by nature.
 *
 * <p><b>Overflow.</b> {@link #add} saturates at
 * {@link Integer#MIN_VALUE}/{@link Integer#MAX_VALUE} rather than wrapping, so
 * an author's "obviously large" {@code Integer.MAX_VALUE / 2} twice over cannot
 * turn an always-blue horse black.
 *
 * <p><b>Opacity is a separate channel, deliberately.</b> Transparency used to
 * ride on the pigment channels - "both pigments essentially zero" is how
 * dominant white and splash markings punch through to the bald template. Once
 * phase 3 can add colour to a texel that carries no pigment, "no pigment" and
 * "no paint" stop being the same statement. A magical gene <i>may</i> paint a
 * dominant-white horse (white is a natural gene, and every magical gene runs
 * after every natural one), but it has to say so: raise {@link #addOpacity} or
 * use {@link #set}. Adding colour alone to a fully transparent texel shows
 * nothing.
 */
public final class ColorField implements ColorView {

    private final int size;
    private final int[] r;
    private final int[] g;
    private final int[] b;
    private final int[] a;
    private final boolean[] absolute;

    /** An all-zero field: as an accumulator, fully transparent; as a delta, a no-op. */
    public ColorField(int size) {
        this.size = size;
        this.r = new int[size * size];
        this.g = new int[size * size];
        this.b = new int[size * size];
        this.a = new int[size * size];
        this.absolute = new boolean[size * size];
    }

    /** An all-zero delta of the same shape as {@code like} - what a gene fills in. */
    public static ColorField deltaLike(ColorView like) {
        return new ColorField(like.size());
    }

    @Override public int size() { return size; }

    @Override public int red(int px, int py) { return r[py * size + px]; }

    @Override public int green(int px, int py) { return g[py * size + px]; }

    @Override public int blue(int px, int py) { return b[py * size + px]; }

    @Override public int opacity(int px, int py) { return a[py * size + px]; }

    @Override public boolean isAbsolute(int px, int py) { return absolute[py * size + px]; }

    /** Tint this texel. Signed, saturating, and leaves opacity alone. */
    public void add(int px, int py, int dr, int dg, int db) {
        int i = py * size + px;
        r[i] = saturate((long) r[i] + dr);
        g[i] = saturate((long) g[i] + dg);
        b[i] = saturate((long) b[i] + db);
    }

    /** Make this texel more (or less) solid. Signed, saturating. */
    public void addOpacity(int px, int py, int da) {
        int i = py * size + px;
        a[i] = saturate((long) a[i] + da);
    }

    /**
     * <b>Flat paint</b>: set this texel absolutely instead of adding to it. On a
     * delta this marks the texel {@link #isAbsolute}, so {@link #apply} replaces
     * rather than accumulates - which is what a gene that must show the same on
     * a black, a chestnut <i>or</i> a white horse needs, and the one thing in
     * phase 3 that is order-dependent. Reserve it for genes that mask
     * everything (see {@code DominancePattern#COMPLETE_DOMINANT}).
     */
    public void set(int px, int py, int opacity, int red, int green, int blue) {
        int i = py * size + px;
        a[i] = opacity;
        r[i] = red;
        g[i] = green;
        b[i] = blue;
        absolute[i] = true;
    }

    /** Seed this texel from a resolved ARGB colour - what phase 2 hands phase 3. */
    public void setArgb(int px, int py, int argb) {
        set(px, py, argb >>> 24, (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
    }

    /**
     * Fold one gene's {@code delta} into this accumulator: absolute texels
     * replace, every other texel adds.
     */
    public void apply(ColorView delta) {
        if (delta.size() != size) {
            throw new IllegalArgumentException("delta is " + delta.size() + "px, field is " + size + "px");
        }
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                if (delta.isAbsolute(px, py)) {
                    set(px, py, delta.opacity(px, py), delta.red(px, py), delta.green(px, py), delta.blue(px, py));
                } else {
                    add(px, py, delta.red(px, py), delta.green(px, py), delta.blue(px, py));
                    addOpacity(px, py, delta.opacity(px, py));
                }
            }
        }
    }

    /** The only place anything is capped: signed accumulators -&gt; a real pixel. */
    @Override
    public int argb(int px, int py) {
        int i = py * size + px;
        return (cap(a[i]) << 24) | (cap(r[i]) << 16) | (cap(g[i]) << 8) | cap(b[i]);
    }

    @Override
    public ColorField mutableCopy() {
        ColorField copy = new ColorField(size);
        System.arraycopy(r, 0, copy.r, 0, r.length);
        System.arraycopy(g, 0, copy.g, 0, g.length);
        System.arraycopy(b, 0, copy.b, 0, b.length);
        System.arraycopy(a, 0, copy.a, 0, a.length);
        System.arraycopy(absolute, 0, copy.absolute, 0, absolute.length);
        return copy;
    }

    private static int saturate(long v) {
        return v < Integer.MIN_VALUE ? Integer.MIN_VALUE : (v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v);
    }

    private static int cap(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
