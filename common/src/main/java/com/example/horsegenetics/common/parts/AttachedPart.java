package com.example.horsegenetics.common.parts;

/**
 * <b>One grown part on one horse, resolved.</b> What a gene hands the client, and
 * the whole of what the render layer needs to draw it.
 *
 * <p>Every field here is something {@code AttachedPartLayer} actually honours.
 * That is a <b>rule and not a coincidence</b>, and it is the same rule
 * {@code CutieMarkGene.Mark} lives under: a field the layer ignores is a promise
 * the game does not keep, and an epigenetic number with no consumer is worse than
 * none at all, because it appears in the inspector, drifts down a lineage and
 * changes nothing. {@code UnicornHornGeneTest} asserts the pairing in both
 * directions.
 *
 * <h2>The split that makes a herd cheap</h2>
 * A part is one {@link #shape} - the baked mesh, shared by every horse that
 * resolves to it - plus a handful of numbers applied to that mesh at draw time.
 * The division is by <b>what it costs to change</b>, not by what it means:
 * <ul>
 *   <li><b>{@link #shape}</b> is topology, and changing it means baking. Bounded
 *       at {@code kinds x styles x }{@link PartShape#SIZE_BUCKETS} meshes for the
 *       whole game, forever.</li>
 *   <li><b>{@link #stretch}, {@link #girth}, {@link #tilt}</b> are a pose-stack
 *       transform. Free, continuous, and copied into the submit node, so they are
 *       safe under a deferred draw.</li>
 *   <li><b>{@link #tint}, {@link #emissive}</b> are per-submit arguments. Also
 *       free, and also per-horse rather than per-mesh - which is why one greyscale
 *       {@link PartSheet} serves every colour a lineage ever drifts to.</li>
 * </ul>
 *
 * <p>Nothing in here is mutated on a shared object: the trap this shape avoids is
 * setting {@code visible} or {@code xScale} on a cached {@code ModelPart} and then
 * submitting it, because submits are deferred and the next horse would overwrite
 * the first one's mesh before either was drawn.
 *
 * @param shape    which baked mesh
 * @param stretch  scale along the part's own axis, so its length is exactly what
 *                 the epigenome asked for rather than its bucket's nominal length
 * @param girth    scale across the part's axis - a slender horn or a stout one
 * @param tilt     radians the whole part leans forward from the anchor's own up
 * @param tint     opaque ARGB multiplied into the greyscale sheet
 * @param emissive drawn a second time full-bright, so it glows in the dark
 */
public record AttachedPart(PartShape shape, float stretch, float girth, float tilt,
                           int tint, boolean emissive) {

    /** How far a part may be scaled on any axis. A guard against a drifted number, not a design range. */
    private static final float MIN_SCALE = 0.2f;
    /** @see #MIN_SCALE */
    private static final float MAX_SCALE = 3.0f;

    public AttachedPart {
        if (shape == null) {
            throw new IllegalArgumentException("an attached part needs a shape");
        }
        stretch = clampScale(stretch);
        girth = clampScale(girth);
    }

    private static float clampScale(float v) {
        if (!(v > MIN_SCALE)) {        // also catches NaN
            return MIN_SCALE;
        }
        return Math.min(MAX_SCALE, v);
    }

    /**
     * A horn from the numbers the unicorn locus carries.
     *
     * @param length   position on the {@link HornSize} ladder, {@code [0,1]}
     * @param girth    cross-section multiplier
     * @param tilt     radians of forward lean
     * @param style    one of {@link HornGenerator}'s four
     * @param tint     opaque ARGB
     * @param emissive whether it glows
     */
    public static AttachedPart horn(double length, double girth, double tilt, int style,
                                    int tint, boolean emissive) {
        PartShape shape = PartShape.of(PartKind.HORN, style, length);
        return new AttachedPart(shape, shape.stretchTo(length), (float) girth, (float) tilt,
                tint, emissive);
    }

    /** What kind of part this is - the anchor and the sheet region follow from it. */
    public PartKind kind() {
        return shape.kind();
    }

    /**
     * How long this part actually comes out, in model units - the bucket's
     * nominal length times the stretch. What the horse screen reports, and what
     * {@link HornSize#of} is asked to name.
     */
    public float length() {
        return shape.nominalLength() * stretch;
    }
}
