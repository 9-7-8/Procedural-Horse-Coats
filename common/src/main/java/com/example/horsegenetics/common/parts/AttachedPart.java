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
 *   <li><b>{@link #baseTint}, {@link #tipTint}, {@link #emissive}</b> are
 *       per-submit arguments. Also free, and also per-horse rather than per-mesh -
 *       which is why one greyscale {@link PartSheet} serves every colour a lineage
 *       ever drifts to.</li>
 * </ul>
 *
 * <h2>Grown by one locus, dressed by others</h2>
 * The shape comes from the gene that grants the part; its colour and its glow
 * come from other loci, applied by {@link #dressed}. That is what makes the horn
 * polygenic without the unicorn locus knowing the others exist -
 * {@code GrownParts.of} does the composing, and a part nobody dresses is white
 * and dark.
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
 * @param baseTint opaque ARGB multiplied into the greyscale sheet at the root
 * @param tipTint  the same at the far end - equal to {@code baseTint} for a
 *                 one-colour part, and blended from one to the other along its
 *                 length by {@link #tintAt} for a two-tone one
 * @param emissive drawn a second time full-bright, so it glows in the dark -
 *                 over the regions its kind names in {@link PartKind#glowRegions()}
 * @param shown    how many of the mesh's countable elements show - an antler's
 *                 tines, base to tip. Four full ones and a fifth at 60% for
 *                 {@code 4.6}; {@link #ALL_ELEMENTS} for a part with no count
 * @param translucent the {@link PartSheet#BONE} region is drawn see-through and
 *                 the points stay opaque - a crystalline antler
 * @param bloomTint the colour of the {@link PartSheet#BLOOM} clumps, or
 *                 {@link #NO_BLOOM} for a part that draws none
 */
public record AttachedPart(PartShape shape, float stretch, float girth, float tilt,
                           int baseTint, int tipTint, boolean emissive,
                           float shown, boolean translucent, int bloomTint) {

    /** What a part nobody has dressed wears: opaque white, so the sheet's grain shows as it is. */
    public static final int UNDYED = 0xFFFFFFFF;

    /** {@link #shown} for a part with nothing to count - every box draws. */
    public static final float ALL_ELEMENTS = Float.POSITIVE_INFINITY;

    /** {@link #bloomTint} of a part that carries no leaves. Transparent black, which no bloom is. */
    public static final int NO_BLOOM = 0;

    /**
     * How opaque a crystalline part's shafts are. Low enough that the head shows
     * through, high enough that the antler still has a silhouette in the dark.
     */
    public static final float CRYSTAL_ALPHA = 0.55f;

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
        if (!(shown >= 0f)) {          // also catches NaN
            shown = 0f;
        }
    }

    /** A part with nothing to count, nothing see-through and no bloom - every horn. */
    public AttachedPart(PartShape shape, float stretch, float girth, float tilt,
                        int baseTint, int tipTint, boolean emissive) {
        this(shape, stretch, girth, tilt, baseTint, tipTint, emissive, ALL_ELEMENTS, false, NO_BLOOM);
    }

    private static float clampScale(float v) {
        if (!(v > MIN_SCALE)) {        // also catches NaN
            return MIN_SCALE;
        }
        return Math.min(MAX_SCALE, v);
    }

    /**
     * A horn from the numbers the unicorn locus carries - white and dark until
     * {@link #dressed} says otherwise.
     *
     * @param length   position on the {@link HornSize} ladder, {@code [0,1]}
     * @param girth    cross-section multiplier
     * @param tilt     radians of forward lean
     * @param style    one of {@link HornGenerator}'s four
     */
    public static AttachedPart horn(double length, double girth, double tilt, int style) {
        PartShape shape = PartShape.of(PartKind.HORN, style, length);
        return new AttachedPart(shape, shape.stretchTo(length), (float) girth, (float) tilt,
                UNDYED, UNDYED, false);
    }

    /**
     * One antler of a rack, from the numbers the antlers locus carries - bone
     * coloured by {@code tint}, unlit, solid and bare until other loci dress it.
     *
     * @param left     the left side rather than the right
     * @param form     an {@link AntlerGenerator} habit
     * @param variant  which of {@link AntlerGenerator#VARIANTS} racks of it
     * @param size     position on the {@link AntlerSize} ladder, {@code [0,1]} -
     *                 the class is the mesh, the remainder a uniform stretch
     * @param tines    how many tines show; fractional grows the last one in
     * @param reach    extra scale along the beam, on top of the size
     * @param girth    extra scale across it
     * @param tint     opaque ARGB of the bone
     */
    public static AttachedPart antler(boolean left, int form, int variant, double size,
                                      double tines, double reach, double girth, int tint) {
        PartKind kind = left ? PartKind.ANTLER_LEFT : PartKind.ANTLER_RIGHT;
        PartShape shape = PartShape.of(kind, form * AntlerGenerator.VARIANTS + variant, size);
        // Size is a UNIFORM scale - a bigger rack, not a taller thinner one - so it
        // multiplies both axes; reach and girth are the non-uniform nudges on top.
        float grow = shape.stretchTo(size);
        return new AttachedPart(shape, (float) (grow * reach), (float) (grow * girth), 0f,
                tint, tint, false, (float) tines, false, NO_BLOOM);
    }

    /** This part in other colours and light, its shape untouched. */
    public AttachedPart dressed(int base, int tip, boolean glows) {
        return new AttachedPart(shape, stretch, girth, tilt, base, tip, glows, shown, translucent, bloomTint);
    }

    /** This part see-through or not, its shape untouched. */
    public AttachedPart crystalline(boolean see) {
        return new AttachedPart(shape, stretch, girth, tilt, baseTint, tipTint, emissive, shown, see, bloomTint);
    }

    /** This part with leaves of {@code tint} on it, or none for {@link #NO_BLOOM}. */
    public AttachedPart blooming(int tint) {
        return new AttachedPart(shape, stretch, girth, tilt, baseTint, tipTint, emissive, shown, translucent, tint);
    }

    /** Does this part draw its {@link PartSheet#BLOOM} clumps? */
    public boolean blooms() {
        return bloomTint != NO_BLOOM;
    }

    /** Two colours rather than one - the renderer draws such a part a segment at a time. */
    public boolean twoTone() {
        return baseTint != tipTint;
    }

    /**
     * The colour {@code along} of the way from root ({@code 0}) to tip ({@code 1}).
     * Each end holds its own colour for its first quarter and the change happens in
     * between, on a smoothstep - so a two-tone horn reads as a horn of one colour
     * with a tip of another, rather than as a gradient with no colour of its own.
     */
    public int tintAt(float along) {
        if (!twoTone()) {
            return baseTint;
        }
        float t = (along - 0.25f) / 0.5f;
        t = t < 0f ? 0f : (t > 1f ? 1f : t);
        t = t * t * (3f - 2f * t);
        return 0xFF000000
                | mix(baseTint >> 16, tipTint >> 16, t) << 16
                | mix(baseTint >> 8, tipTint >> 8, t) << 8
                | mix(baseTint, tipTint, t);
    }

    private static int mix(int a, int b, float t) {
        a &= 0xFF;
        b &= 0xFF;
        return Math.round(a + (b - a) * t);
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
