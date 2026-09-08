package com.example.horsegenetics.common.genetics.epi;

import java.util.Arrays;

/**
 * <b>The literal numbers one gene has written on one allele copy</b> - the
 * thing that used to be a single {@code long} seed, and the reason this whole
 * change exists.
 *
 * <p>A gene reads them by name, in whatever order it likes:
 *
 * <pre>{@code
 * EpiValues epi = ctx.epigeneticsFor(KEY);
 * double cover = epi.get("cover");
 * long   shape = epi.seed("shape");
 * double frontLeft = epi.get("coronet", 0);
 * }</pre>
 *
 * <p>Compare what that replaced: a fixed sequence of {@code nextFloat()} calls
 * whose <i>position</i> was the meaning of each number, so re-tuning a gene by
 * inserting one draw silently rewrote every horse in every save. Now the name
 * is the meaning, the number is stored, and re-ordering a schema costs nothing.
 *
 * <h2>Immutable, and inherited verbatim</h2>
 * A foal that inherits an allele copy inherits <b>this object's numbers</b>,
 * subject only to {@link EpiDrift}'s nudge. {@link #with} returns a new
 * instance rather than mutating, because these are shared freely between a
 * horse, its record and the copies handed to painters.
 *
 * <p>Storage is two flat arrays: scalars and categories share {@code scalars}
 * (a category is an index held as a whole number), seeds live in {@code seeds}
 * indexed by value position. {@link EpiSchema} owns the offsets.
 */
public final class EpiValues {

    /** For a gene that declares no epigenetics at all. */
    public static final EpiValues EMPTY = new EpiValues(EpiSchema.EMPTY, new double[0], new long[0]);

    private final EpiSchema schema;
    private final double[] scalars;
    private final long[] seeds;

    EpiValues(EpiSchema schema, double[] scalars, long[] seeds) {
        this.schema = schema;
        // Quantised on the way in, so what a horse carries is exactly what its
        // code string says - see EpiCodec.quantise for why that is a correctness
        // requirement and not just tidiness.
        for (int i = 0; i < scalars.length; i++) {
            scalars[i] = EpiCodec.quantise(scalars[i]);
        }
        this.scalars = scalars;
        this.seeds = seeds;
    }

    public EpiSchema schema() {
        return schema;
    }

    public boolean isEmpty() {
        return schema.isEmpty();
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    /** A magnitude, by name. Fails loudly if the gene never declared it. */
    public double get(String name) {
        return get(name, 0);
    }

    /**
     * One leg's magnitude of a per-leg value, in {@code CoatRegions.LEGS} order.
     * A single-arity value reads the same number for every leg, so a painter
     * that asks per leg still works if the schema is later narrowed.
     */
    public double get(String name, int leg) {
        int i = schema.require(name);
        EpiValue v = schema.get(i);
        if (v.kind() == EpiValue.Kind.SEED) {
            throw new IllegalArgumentException("'" + name + "' is a seed; read it with seed()");
        }
        int slot = v.arity() == 1 ? 0 : Math.min(Math.max(leg, 0), v.arity() - 1);
        return scalars[schema.scalarOffset(i) + slot];
    }

    /** The {@code long} behind a noise field. */
    public long seed(String name) {
        int i = schema.require(name);
        if (schema.get(i).kind() != EpiValue.Kind.SEED) {
            throw new IllegalArgumentException("'" + name + "' is not a seed; read it with get()");
        }
        return seeds[i];
    }

    /** An index into the gene's own list, already inside {@code [0, count)}. */
    public int category(String name) {
        int i = schema.require(name);
        EpiValue v = schema.get(i);
        if (v.kind() != EpiValue.Kind.CATEGORY) {
            throw new IllegalArgumentException("'" + name + "' is not a category");
        }
        int count = (int) v.max();
        int raw = (int) scalars[schema.scalarOffset(i)];
        return raw < 0 ? 0 : (raw >= count ? count - 1 : raw);
    }

    /**
     * A packed {@code 0xRRGGBB} built from three channels named
     * {@code <prefix>_r} / {@code _g} / {@code _b}. Colours are stored per
     * channel so a lineage's colour can drift; this is the one place that is
     * folded back into the packed int the painters take.
     */
    public int rgb(String prefix) {
        int r = channel(prefix + "_r");
        int g = channel(prefix + "_g");
        int b = channel(prefix + "_b");
        return (r << 16) | (g << 8) | b;
    }

    private int channel(String name) {
        double v = get(name);
        int c = (int) Math.round(v);
        return c < 0 ? 0 : (c > 255 ? 255 : c);
    }

    // ------------------------------------------------------------------
    // Editing - what the designer, the spawn screen and a splice carrot use
    // ------------------------------------------------------------------

    /** This copy with one magnitude replaced, clamped to its hard safety bound. */
    public EpiValues with(String name, double value) {
        return with(name, 0, value);
    }

    public EpiValues with(String name, int leg, double value) {
        int i = schema.require(name);
        EpiValue v = schema.get(i);
        double[] next = scalars.clone();
        int slot = v.arity() == 1 ? 0 : Math.min(Math.max(leg, 0), v.arity() - 1);
        next[schema.scalarOffset(i) + slot] = v.clamp(value);
        return new EpiValues(schema, next, seeds);
    }

    public EpiValues withSeed(String name, long value) {
        int i = schema.require(name);
        long[] next = seeds.clone();
        next[i] = value;
        return new EpiValues(schema, scalars, next);
    }

    // ------------------------------------------------------------------
    // Internals - EpiRoll, EpiDrift and the codec build these directly
    // ------------------------------------------------------------------

    static EpiValues of(EpiSchema schema, double[] scalars, long[] seeds) {
        return new EpiValues(schema, scalars, seeds);
    }

    double[] rawScalars() {
        return scalars;
    }

    long[] rawSeeds() {
        return seeds;
    }

    /**
     * Fold every stored number into {@code h}, FNV-1a style. This is what the
     * coat texture key is built from now that there is no single seed to hash -
     * see {@code Epigenome.visibleFingerprint}.
     */
    public long hashInto(long h) {
        for (double s : scalars) {
            h = (h ^ Double.doubleToLongBits(s)) * 0x100000001b3L;
        }
        for (long s : seeds) {
            h = (h ^ s) * 0x100000001b3L;
        }
        return h;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EpiValues v
                && Arrays.equals(v.scalars, scalars)
                && Arrays.equals(v.seeds, seeds);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(scalars) * 31 + Arrays.hashCode(seeds);
    }
}
