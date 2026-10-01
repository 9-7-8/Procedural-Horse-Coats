package com.example.horsegenetics.common.parts;

/**
 * <b>The identity of a baked part mesh</b> - and therefore the cache key the
 * client builds one model per.
 *
 * <p>A shape holds only what changes the <b>topology</b>: which kind of part,
 * which style, and which size bucket. Everything else a horse's part differs by -
 * its exact length, its girth, how far forward it leans, what colour it is,
 * whether it glows - is a transform or a tint at draw time and is deliberately
 * <b>not</b> in here. Putting length in the key would bake one mesh per horse;
 * putting tint in it would bake one per colour per horse.
 *
 * <h2>Why a size bucket at all, if length is a transform</h2>
 * Because length is not <i>only</i> a scale. A three-unit nub and a
 * twenty-four-unit tusk want different segment counts: squashing fifteen segments
 * into three units leaves boxes a third of a texel wide, which shimmer, and
 * stretching three segments to twenty-four units gives a horn made of three
 * visible blocks. So the segment count steps, and the steps are these buckets.
 *
 * <p>Sixteen of them, geometrically spaced, put any two neighbouring buckets
 * within 6.7% of each other - so the per-horse scale that lands a horn on its
 * exact epigenetic length never stretches a mesh by more than that, and the taper
 * cannot visibly distort. The seam the buckets would otherwise cause is closed by
 * the same scale: nothing pops as a lineage's horns grow through a boundary.
 *
 * <h2>The bound on cost is structural</h2>
 * The sum over kinds of {@code styles x sizeBuckets} is every part mesh this mod
 * can ever bake, however many horses exist and whatever a drifted epigenome asks
 * for - {@code PartGenerators.allShapes().size()}, pinned by
 * {@code PartGeneratorTest}. That is the whole reason the key is coarse, and it is
 * worth more than a budget somebody has to remember to enforce.
 *
 * @param kind  what the part is
 * @param style which of {@link PartKind#styles()} variants
 * @param size  which of {@link PartKind#sizeBuckets()} length steps
 */
public record PartShape(PartKind kind, int style, int size) {

    /**
     * The horn's length steps. Sixteen, so the widest gap between neighbours is
     * under 7% - see the class note. Other kinds name their own count in
     * {@link PartKind#sizeBuckets()}: an antler's five are size classes with a
     * different rack in each, so its stretch within a class is wider (under 20%)
     * and that is deliberate - the class change is the topology, the stretch only
     * the seam.
     */
    public static final int SIZE_BUCKETS = 16;

    public PartShape {
        if (kind == null) {
            throw new IllegalArgumentException("a part shape needs a kind");
        }
        if (style < 0 || style >= kind.styles()) {
            throw new IllegalArgumentException(
                    kind + ": style " + style + " outside 0.." + (kind.styles() - 1));
        }
        if (size < 0 || size >= kind.sizeBuckets()) {
            throw new IllegalArgumentException(
                    kind + ": size bucket " + size + " outside 0.." + (kind.sizeBuckets() - 1));
        }
    }

    /**
     * The shape a part of this kind, style and normalised size resolves to.
     *
     * <p>{@code length} is the gene's own {@code [0,1]} position on the size
     * ladder and is <b>clamped, not wrapped or extrapolated</b> - an epigenetic
     * value may legally sit eight design spans outside its range, and a horn a
     * chunk long is a hazard rather than a triumph.
     */
    public static PartShape of(PartKind kind, int style, double length) {
        double clamped = length < 0.0 ? 0.0 : (length > 1.0 ? 1.0 : length);
        int buckets = kind.sizeBuckets();
        int bucket = (int) (clamped * buckets);
        return new PartShape(kind, Math.max(0, Math.min(kind.styles() - 1, style)),
                Math.min(buckets - 1, bucket));
    }

    /** The middle of this bucket, on the {@code [0,1]} size ladder. */
    public double centre() {
        return (size + 0.5) / kind.sizeBuckets();
    }

    /** How long the mesh this shape bakes to actually is, in model units. */
    public float nominalLength() {
        return lengthAt(centre());
    }

    /** The length {@code position} on this kind's ladder asks for. */
    private float lengthAt(double position) {
        return switch (kind) {
            case HORN -> HornSize.lengthFor(position);
            case ANTLER_RIGHT, ANTLER_LEFT -> AntlerSize.lengthFor(position);
            case RAM_HORN_RIGHT, RAM_HORN_LEFT -> RamHornSize.lengthFor(position);
        };
    }

    /**
     * The scale along the part's own axis that takes the baked mesh to the length
     * {@code position} asks for. Always within 7% of 1 for an in-range position -
     * see the class note - so it stretches the mesh rather than distorting it.
     */
    public float stretchTo(double position) {
        double clamped = position < 0.0 ? 0.0 : (position > 1.0 ? 1.0 : position);
        return lengthAt(clamped) / nominalLength();
    }
}
