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
 *                 the epigenome asked for rather than its bucket's nominal length -
 *                 along each element's own axis instead, for a kind that
 *                 {@link PartKind#scalesPerElement() scales per element}
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
 * @param opacity  how opaque the regions its kind draws see-through
 *                 ({@link PartKind#translucentRegions()}) are: {@link #OPAQUE} for a
 *                 solid part, {@link #CRYSTAL_ALPHA} for a crystalline antler's shafts
 *                 and a crystal growth's,
 *                 a sail's own number for its membrane, and {@code 0} to draw those
 *                 regions not at all - a bone sail, its bare rays with no skin
 * @param bloomTint the colour of the {@link PartSheet#BLOOM} clumps, or
 *                 {@link #NO_BLOOM} for a part that draws none
 * @param span     scale along a row - how much of the back a sail covers from the
 *                 withers - applied on the pose stack along the row's own length.
 *                 {@code 1} for every other part
 */
public record AttachedPart(PartShape shape, float stretch, float girth, float tilt,
                           int baseTint, int tipTint, boolean emissive,
                           float shown, float opacity, int bloomTint, float span) {

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

    /** {@link #opacity} of a part with nothing see-through. */
    public static final float OPAQUE = 1f;

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
        if (!(opacity >= 0f)) {        // also catches NaN
            opacity = 0f;
        }
        opacity = Math.min(OPAQUE, opacity);
        span = clampScale(span);
    }

    /** A part with nothing to count, nothing see-through and no bloom - every horn. */
    public AttachedPart(PartShape shape, float stretch, float girth, float tilt,
                        int baseTint, int tipTint, boolean emissive) {
        this(shape, stretch, girth, tilt, baseTint, tipTint, emissive, ALL_ELEMENTS, OPAQUE, NO_BLOOM, 1f);
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
     * A narwhal horn from the numbers the tusks locus's {@code Nar} copy carries - the
     * horn's shape and rules on the {@link NarwhalSize} ladder, white and dark until
     * the tusk colour locus dresses it.
     *
     * @param length position on the {@link NarwhalSize} ladder, {@code [0,1]}
     * @param girth  cross-section multiplier
     * @param tilt   radians off the head's own axis; negative lifts the point
     * @param twist  {@code 0..}{@link HornGenerator#NARWHAL_STYLES}{@code -1}: spiral or tight
     */
    public static AttachedPart narwhal(double length, double girth, double tilt, int twist) {
        PartShape shape = PartShape.of(PartKind.NARWHAL, twist, length);
        return new AttachedPart(shape, shape.stretchTo(length), (float) girth, (float) tilt,
                UNDYED, UNDYED, false);
    }

    /**
     * One boar tusk, from the numbers the tusks locus's {@code Tsk} copy carries - white
     * and dark until the tusk colour locus dresses it.
     *
     * @param left   the left side rather than the right
     * @param form   a {@link TuskGenerator} form
     * @param sweep  {@code [0,1]}: how far the tusk curls up from where it leaves the jaw
     * @param hook   which way a hooked tusk's tip turns, {@code 0..}{@link TuskGenerator#HOOKS}{@code -1}
     * @param length position on the {@link TuskSize} ladder - class is the mesh, the rest a stretch
     * @param girth  extra scale across the tusk
     */
    public static AttachedPart tusk(boolean left, int form, double sweep, int hook, double length, double girth) {
        PartKind kind = left ? PartKind.TUSK_LEFT : PartKind.TUSK_RIGHT;
        // Only a hooked tusk reads the hook, so the other forms share one mesh.
        int style = TuskGenerator.style(form, bucket(sweep, TuskGenerator.SWEEPS),
                form == TuskGenerator.HOOKED ? hook : 0);
        PartShape shape = PartShape.of(kind, style, length);
        // A uniform scale within a class, the dragon horn's rule: the tusk leaves the
        // jaw sideways, so a stretch along the anchor's y alone would shear it.
        float grow = shape.stretchTo(length);
        return new AttachedPart(shape, grow, (float) (grow * girth), 0f, UNDYED, UNDYED, false);
    }

    /**
     * One sabre fang, from the numbers the tusks locus's {@code Sab} copy carries - white
     * and dark until the tusk colour locus dresses it.
     *
     * @param left    the left side rather than the right
     * @param form    a {@link SabreGenerator} form
     * @param pattern which serration pattern, {@code 0..}{@link SabreGenerator#PATTERNS}{@code -1}
     * @param length  position on the {@link SabreSize} ladder - class is the mesh, the rest a stretch
     * @param girth   extra scale across the blade
     * @param tilt    radians off straight down the face; negative leans the point forward
     */
    public static AttachedPart sabre(boolean left, int form, int pattern, double length, double girth,
                                     double tilt) {
        PartKind kind = left ? PartKind.SABRE_LEFT : PartKind.SABRE_RIGHT;
        PartShape shape = PartShape.of(kind, SabreGenerator.style(form, pattern), length);
        float grow = shape.stretchTo(length);
        return new AttachedPart(shape, grow, (float) (grow * girth), (float) tilt, UNDYED, UNDYED, false);
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
                tint, tint, false, (float) tines, OPAQUE, NO_BLOOM, 1f);
    }

    /**
     * One ram's horn, from the numbers the ram horns locus carries - one colour,
     * root to tip, until the horn tip locus gives it a second.
     *
     * @param left  the left side rather than the right
     * @param form  a {@link RamHornGenerator} shape
     * @param curl  {@code [0,1]}: how far round a curl goes, how tight a corkscrew twists
     * @param size  position on the {@link RamHornSize} ladder - class is the mesh, the rest a stretch
     * @param girth extra scale across the horn
     * @param tint  opaque ARGB of the keratin
     */
    public static AttachedPart ramHorn(boolean left, int form, double curl, double size, double girth, int tint) {
        PartKind kind = left ? PartKind.RAM_HORN_LEFT : PartKind.RAM_HORN_RIGHT;
        double c = curl < 0.0 ? 0.0 : (curl > 1.0 ? 1.0 : curl);
        int bucket = Math.min(RamHornGenerator.CURLS - 1, (int) (c * RamHornGenerator.CURLS));
        PartShape shape = PartShape.of(kind, form * RamHornGenerator.CURLS + bucket, size);
        float grow = shape.stretchTo(size);
        return new AttachedPart(shape, grow, (float) (grow * girth), 0f, tint, tint, false);
    }

    /**
     * One dragon horn, from the numbers the dragon horns locus carries - white and
     * dark until its colour locus dresses it.
     *
     * @param left   the left side rather than the right
     * @param form   a {@link DragonHornGenerator} form
     * @param sweep  {@code [0,1]}: how far a swept horn bends, a spike leans, a curl goes round
     * @param splay  {@code [0,1]}: how far the pair angle outward
     * @param length position on the {@link DragonHornSize} ladder - class is the mesh, the rest a stretch
     * @param girth  extra scale across the horn
     */
    public static AttachedPart dragonHorn(boolean left, int form, double sweep, double splay,
                                          double length, double girth) {
        PartKind kind = left ? PartKind.DRAGON_HORN_LEFT : PartKind.DRAGON_HORN_RIGHT;
        int style = DragonHornGenerator.style(form,
                bucket(sweep, DragonHornGenerator.SWEEPS), bucket(splay, DragonHornGenerator.SPLAYS));
        PartShape shape = PartShape.of(kind, style, length);
        // Length is a uniform scale within a class, as a ram's horn's is - a longer
        // horn is a bigger one, not a thinner one - and girth the nudge on top.
        float grow = shape.stretchTo(length);
        return new AttachedPart(shape, grow, (float) (grow * girth), 0f, UNDYED, UNDYED, false);
    }

    /**
     * A row of dorsal spines, from the numbers the dorsal spines locus carries - white
     * and dark until its colour locus dresses it. The kind scales per element
     * ({@link PartKind#scalesPerElement()}), so {@link #stretch} and {@link #girth}
     * are each spine's height and thickness, applied about its own root, and the
     * row's length along the back never changes.
     *
     * @param form   a {@link DorsalSpineGenerator} form
     * @param taper  {@code [0,1]}: how sharply each spine narrows to its point
     * @param length position on the {@link SpineSize} ladder - class is the mesh, the rest a stretch
     * @param count  how many spines show, from the withers back; fractional grows the last one in
     * @param girth  extra thickness on top of the height
     */
    public static AttachedPart dorsalSpines(int form, double taper, double length, double count, double girth) {
        int style = DorsalSpineGenerator.style(form, bucket(taper, DorsalSpineGenerator.TAPERS));
        PartShape shape = PartShape.of(PartKind.SPINES, style, length);
        float grow = shape.stretchTo(length);
        return new AttachedPart(shape, grow, (float) (grow * girth), 0f, UNDYED, UNDYED, false,
                (float) count, OPAQUE, NO_BLOOM, 1f);
    }

    /**
     * A back sail, from the numbers the back sail locus carries - white and dark until
     * its colour locus dresses it. Like the spine row it scales per element, but only in
     * height: {@link #stretch} is each spine's and its membrane's, and {@link #girth} is
     * {@code 1}, because a membrane panel is as wide as the gap between two spines and a
     * thicker spine would close it. Coverage is the {@link #span} along the row instead.
     *
     * @param form     a {@link SailGenerator} form
     * @param curve    {@code [0,1]}: how strongly the profile arches
     * @param length   position on the {@link SailSize} ladder - class is the mesh, the rest a stretch
     * @param count    how many spines show, from the withers back; fractional grows the last one in
     * @param coverage how far back the row reaches, as a share of the full row
     * @param opacity  how opaque the membrane is
     */
    public static AttachedPart backSail(int form, double curve, double length, double count,
                                        double coverage, double opacity) {
        int style = SailGenerator.style(form, bucket(curve, SailGenerator.CURVES));
        PartShape shape = PartShape.of(PartKind.SAIL, style, length);
        return new AttachedPart(shape, shape.stretchTo(length), 1f, 0f, UNDYED, UNDYED, false,
                (float) count, (float) opacity, NO_BLOOM, (float) coverage);
    }

    /**
     * One side's shoulder and hip plates, from the numbers the body plates locus
     * carries - white and dark until its colour locus dresses it. The kind scales per
     * element ({@link PartKind#scalesPerElement()}): {@link #stretch} is each slab's
     * height down the flank and {@link #girth} its length and thickness, both about the
     * slab's own top edge, so the clusters stay where they are on the body. Size is a
     * uniform grow, as a dragon horn's is - a bigger plate, not a taller thinner one.
     *
     * @param left      the left side rather than the right
     * @param form      a {@link PlateGenerator} form
     * @param overlap   {@code [0,1]}: how far each slab laps the one below
     * @param spikiness {@code [0,1]}: how far the slabs flare, and how big a rib or spike
     * @param size      position on the {@link PlateSize} ladder - class is the mesh, the rest a stretch
     * @param count     slabs per cluster, from the top down; fractional grows the next one in
     */
    public static AttachedPart plates(boolean left, int form, double overlap, double spikiness,
                                      double size, double count) {
        PartKind kind = left ? PartKind.PLATES_LEFT : PartKind.PLATES_RIGHT;
        int style = PlateGenerator.style(form, bucket(overlap, PlateGenerator.OVERLAPS),
                bucket(spikiness, PlateGenerator.SPIKES));
        PartShape shape = PartShape.of(kind, style, size);
        float grow = shape.stretchTo(size);
        return new AttachedPart(shape, grow, grow, 0f, UNDYED, UNDYED, false,
                (float) count, OPAQUE, NO_BLOOM, 1f);
    }

    /**
     * Crystal growths along the back, from the numbers the back crystals locus carries -
     * white and dark until its colour locus dresses it, the shafts see-through at
     * {@link #CRYSTAL_ALPHA} and the points solid. The kind scales per element
     * ({@link PartKind#scalesPerElement()}): {@link #stretch} and {@link #girth} are each
     * cluster's own grow, about its central crystal, so the clusters stay where they are
     * along the back. Size is a uniform grow - a bigger crystal, not a taller thinner one.
     *
     * @param variant which of {@link CrystalGenerator#VARIANTS} arrangements - the seed's
     * @param spread  {@code [0,1]}: how far the outer crystals of a cluster fan from upright
     * @param size    position on the {@link CrystalSize} ladder - class is the mesh, the rest a stretch
     * @param count   how many clusters show, from the withers back; fractional grows the last one in
     */
    public static AttachedPart crystals(int variant, double spread, double size, double count) {
        int style = CrystalGenerator.style(variant, bucket(spread, CrystalGenerator.SPREADS));
        PartShape shape = PartShape.of(PartKind.CRYSTALS, style, size);
        float grow = shape.stretchTo(size);
        return new AttachedPart(shape, grow, grow, 0f, UNDYED, UNDYED, false,
                (float) count, CRYSTAL_ALPHA, NO_BLOOM, 1f);
    }

    /** {@code x} in {@code [0,1]}, clamped, to one of {@code n} equal buckets. */
    private static int bucket(double x, int n) {
        double c = x < 0.0 ? 0.0 : (x > 1.0 ? 1.0 : x);
        return Math.min(n - 1, (int) (c * n));
    }

    /** This part in other colours and light, its shape untouched. */
    public AttachedPart dressed(int base, int tip, boolean glows) {
        return new AttachedPart(shape, stretch, girth, tilt, base, tip, glows, shown, opacity, bloomTint, span);
    }

    /** This part's crystal shafts see-through or not, its shape untouched. */
    public AttachedPart crystalline(boolean see) {
        return seeThrough(see ? CRYSTAL_ALPHA : OPAQUE);
    }

    /** This part with its kind's see-through regions at {@code alpha}, its shape untouched. */
    public AttachedPart seeThrough(float alpha) {
        return new AttachedPart(shape, stretch, girth, tilt, baseTint, tipTint, emissive, shown, alpha, bloomTint, span);
    }

    /** This part with leaves of {@code tint} on it, or none for {@link #NO_BLOOM}. */
    public AttachedPart blooming(int tint) {
        return new AttachedPart(shape, stretch, girth, tilt, baseTint, tipTint, emissive, shown, opacity, tint, span);
    }

    /**
     * Is anything about this part see-through - does it draw its kind's
     * {@link PartKind#translucentRegions()} in the blended pass, or (at {@code 0}) skip
     * them? Never for a kind with no such regions, whatever its opacity says.
     */
    public boolean translucent() {
        return opacity < OPAQUE && kind().translucentRegions() != 0;
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
