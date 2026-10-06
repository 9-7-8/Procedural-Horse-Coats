package com.example.horsegenetics.common.parts;

/**
 * <b>What a gene or a breed asks for.</b> One value per kind of grown part; the
 * client maps a kind to a render pass, and {@code common/} never sees a mesh.
 *
 * <p>A kind nothing draws would be a promise the game does not keep, so kinds
 * arrive with their geometry, not ahead of it. The antlers were the first test of
 * the claim that the second part is "a value here plus a generator": they are two
 * values (one per side - see {@link AntlerGenerator} on why), and they needed two
 * things the horn did not - a count shown at draw time, and a part a foal does not
 * wear - which landed here as {@link #showsOnFoal()} and in the mesh's node groups.
 */
public enum PartKind {

    /**
     * A single tapering horn on the forehead - the unicorn locus. Opaque, ivory
     * by default, and with no seeded variation at all: a horn is a cone, and
     * everything that differs between two horses' horns is length, girth, twist,
     * lean and colour.
     */
    HORN(PartAnchor.FOREHEAD, PartSheet.HORN),

    /** The right-hand antler of a rack - the antlers locus. */
    ANTLER_RIGHT(PartAnchor.CROWN_RIGHT, PartSheet.BONE),

    /** The left-hand antler. A symmetric rack asks both sides for the same variant. */
    ANTLER_LEFT(PartAnchor.CROWN_LEFT, PartSheet.BONE),

    /** The right-hand ram's horn - the ram horns locus. Rooted where an antler is. */
    RAM_HORN_RIGHT(PartAnchor.CROWN_RIGHT, PartSheet.RAM_HORN),

    /** The left-hand ram's horn. */
    RAM_HORN_LEFT(PartAnchor.CROWN_LEFT, PartSheet.RAM_HORN),

    /**
     * The right-hand dragon horn - the dragon horns locus. Rooted behind the ear and
     * swept back over the neck; keratin like the unicorn's, tip and all.
     */
    DRAGON_HORN_RIGHT(PartAnchor.NAPE_RIGHT, PartSheet.HORN),

    /** The left-hand dragon horn. A pair is symmetric: both sides ask for the same style. */
    DRAGON_HORN_LEFT(PartAnchor.NAPE_LEFT, PartSheet.HORN),

    /**
     * A row of dorsal spines along the back - the dorsal spines locus, and the first
     * part off the head. One kind for the whole row: it is centred, so there is no
     * side to mirror. Bone, with a polished point ({@link DorsalSpineGenerator}).
     */
    SPINES(PartAnchor.SPINE, PartSheet.BONE),

    /**
     * A back sail - the back sail locus. The spine row's anchor and numbering, with a
     * see-through membrane between the spines ({@link SailGenerator}). Centred, like
     * the spines, so one kind.
     */
    SAIL(PartAnchor.SPINE, PartSheet.BONE),

    /**
     * The right side's shoulder and hip plates - the body plates locus. Both clusters
     * are one mesh per side ({@link PlateGenerator}): bone slabs hung down the flank,
     * their edges, ribs and spikes polished.
     */
    PLATES_RIGHT(PartAnchor.BODY_RIGHT, PartSheet.BONE),

    /** The left side's plates. A horse's two sides always ask for the same style. */
    PLATES_LEFT(PartAnchor.BODY_LEFT, PartSheet.BONE),

    /**
     * A narwhal horn - the tusks locus's {@code Nar} form. One long spiral tusk from
     * the front of the muzzle, pointing forward. It is the unicorn horn's generator
     * in its two twisted styles on a longer ladder ({@link NarwhalSize}), so a straight
     * tapered spiral of ivory, tip and all. Centred, so one kind.
     */
    NARWHAL(PartAnchor.SNOUT, PartSheet.HORN),

    /**
     * Crystal growths along the back - the back crystals locus. Clusters on the spine
     * row's anchor and numbering, see-through shafts with solid points
     * ({@link CrystalGenerator}). Centred, so one kind.
     */
    CRYSTALS(PartAnchor.SPINE, PartSheet.CRYSTAL);

    private final PartAnchor anchor;
    private final int texture;

    PartKind(PartAnchor anchor, int texture) {
        this.anchor = anchor;
        this.texture = texture;
    }

    /** Where on the horse this kind is rooted. */
    public PartAnchor anchor() {
        return anchor;
    }

    /** The {@link PartSheet} region this kind's boxes sample by default. */
    public int texture() {
        return texture;
    }

    /** How many style variants {@link PartGenerators} will build for this kind. */
    public int styles() {
        return switch (this) {
            case HORN -> HornGenerator.STYLES;
            // Habit x rack: the antler_form locus picks the first, the rack seed the second.
            case ANTLER_RIGHT, ANTLER_LEFT -> AntlerGenerator.FORMS * AntlerGenerator.VARIANTS;
            // Shape x how far it curls (how tight a corkscrew twists).
            case RAM_HORN_RIGHT, RAM_HORN_LEFT -> RamHornGenerator.FORMS * RamHornGenerator.CURLS;
            // Form x sweep x splay - every way a dragon horn points is baked.
            case DRAGON_HORN_RIGHT, DRAGON_HORN_LEFT -> DragonHornGenerator.styles();
            // Form x taper.
            case SPINES -> DorsalSpineGenerator.styles();
            // Form x curve.
            case SAIL -> SailGenerator.styles();
            // Form x overlap x spikiness.
            case PLATES_RIGHT, PLATES_LEFT -> PlateGenerator.styles();
            // The horn's two twisted styles; the narwhal never bends (HornGenerator).
            case NARWHAL -> HornGenerator.NARWHAL_STYLES;
            // Arrangement (the seed) x lean spread.
            case CRYSTALS -> CrystalGenerator.styles();
        };
    }

    /**
     * How many size buckets - meshes along the size ladder - this kind has. The
     * horn's sixteen are fine steps whose only job is a sane segment count; the
     * antler's five are the {@link AntlerSize} classes, each a different rack.
     */
    public int sizeBuckets() {
        return switch (this) {
            case HORN -> PartShape.SIZE_BUCKETS;
            case ANTLER_RIGHT, ANTLER_LEFT -> AntlerSize.classes();
            case RAM_HORN_RIGHT, RAM_HORN_LEFT -> RamHornSize.classes();
            case DRAGON_HORN_RIGHT, DRAGON_HORN_LEFT -> DragonHornSize.classes();
            case SPINES -> SpineSize.classes();
            case SAIL -> SailSize.classes();
            case PLATES_RIGHT, PLATES_LEFT -> PlateSize.classes();
            // The horn's fine steps: a segment count, not a class.
            case NARWHAL -> PartShape.SIZE_BUCKETS;
            case CRYSTALS -> CrystalSize.classes();
        };
    }

    /**
     * Does a foal wear this part? A foal wears a half-size horn (owner's call,
     * 2026-09-30); it wears <b>no antlers</b> - the treatment's default (P5), and
     * the biology: antlers grow from pedicles that do not exist at birth - and no ram's
     * horns either, which come in with the rack's rule rather than the unicorn's.
     * Dragon horns are a hard part and come with maturity too (owner, 2026-10-01:
     * foals wear the soft parts only). So do the body parts: they are hard parts,
     * and a foal's back is tiny (body-parts treatment). So is the narwhal horn: the
     * unicorn horn is the only hard part a foal wears (tusks treatment).
     */
    public boolean showsOnFoal() {
        return this == HORN;
    }

    /**
     * The sheet regions a glowing part lights. A glowing horn glows all over; a
     * glowing antler only at its points - each tine's tip and the beam's end - which
     * is the antler glow locus as the treatment wrote it.
     */
    public int glowRegions() {
        return switch (this) {
            case HORN -> PartSheet.SOLID;
            case ANTLER_RIGHT, ANTLER_LEFT -> PartSheet.bit(PartSheet.BONE_TIP);
            // Nothing makes a ram's horn glow yet; if something does, the whole horn.
            case RAM_HORN_RIGHT, RAM_HORN_LEFT -> PartSheet.SOLID;
            // Nor a dragon horn; the same answer.
            case DRAGON_HORN_RIGHT, DRAGON_HORN_LEFT -> PartSheet.SOLID;
            // Nor the spines, nor a sail, nor the plates, nor a narwhal horn. Nor the
            // crystals: the treatment leaves glow out and says this is where it would go.
            case SPINES, SAIL, PLATES_RIGHT, PLATES_LEFT, NARWHAL, CRYSTALS -> PartSheet.SOLID;
        };
    }

    /**
     * The sheet regions this kind draws see-through when the part asks for it
     * ({@link AttachedPart#translucent()}). A crystalline antler's shafts are its
     * bone, and its points stay solid. A sail's membrane is always see-through and its
     * spines never are. {@code 0} for a kind that is never see-through.
     *
     * <p>This is data on the kind rather than a constant in the renderer, so a part with
     * a see-through region of its own is a line here, not a second branch there. The
     * blended pass draws only these regions, which is what keeps it cheap: on a sail
     * it draws the membrane and nothing else.
     */
    public int translucentRegions() {
        return switch (this) {
            case ANTLER_RIGHT, ANTLER_LEFT -> PartSheet.bit(PartSheet.BONE);
            case SAIL -> PartSheet.bit(PartSheet.MEMBRANE);
            // A crystal's shafts, never its points - the crystal antler's split.
            case CRYSTALS -> PartSheet.bit(PartSheet.CRYSTAL);
            case HORN, RAM_HORN_RIGHT, RAM_HORN_LEFT, DRAGON_HORN_RIGHT, DRAGON_HORN_LEFT, SPINES,
                 PLATES_RIGHT, PLATES_LEFT, NARWHAL -> 0;
        };
    }

    /**
     * Does a saddle hide some of this part? A part that runs along the spine passes
     * under the saddle and the rider; while either is there, its elements inside the
     * saddle zone are hidden and the rest stay ({@link SaddleZone}).
     */
    public boolean saddleZoned() {
        return this == SPINES || this == SAIL || this == CRYSTALS;
    }

    /**
     * Is this part a row of elements, each scaled about its own root, rather than one
     * thing scaled as a whole? A row lies along the body, so a whole-part scale across
     * it would also stretch the row lengthwise - see {@link DorsalSpineGenerator}.
     * Every box of such a kind belongs to a group. The plates are rows down the flank,
     * two clusters to a mesh, so a whole-part scale would also pull the shoulder and
     * hip clusters apart along the body. Crystal growths are clusters along the back,
     * each grown about its own central crystal.
     */
    public boolean scalesPerElement() {
        return this == SPINES || this == SAIL || plates() || this == CRYSTALS;
    }

    /** Is this a side of shoulder and hip plates? */
    public boolean plates() {
        return this == PLATES_RIGHT || this == PLATES_LEFT;
    }

    /** Is this a dragon horn, of either side? */
    public boolean dragonHorn() {
        return this == DRAGON_HORN_RIGHT || this == DRAGON_HORN_LEFT;
    }

    /** Is this a ram's horn, of either side? */
    public boolean ramHorn() {
        return this == RAM_HORN_RIGHT || this == RAM_HORN_LEFT;
    }

    /** Is this an antler, of either side? */
    public boolean antler() {
        return this == ANTLER_RIGHT || this == ANTLER_LEFT;
    }

    /**
     * What a person calls the part, both sides of a pair named once - "Antlers", not
     * two antlers. The browser designer lists a horse's parts by it, because it
     * cannot draw them ({@link com.example.horsegenetics.common.genetics.GrownParts#listed}).
     */
    public String label() {
        return switch (this) {
            case HORN -> "Unicorn horn";
            case ANTLER_RIGHT, ANTLER_LEFT -> "Antlers";
            case RAM_HORN_RIGHT, RAM_HORN_LEFT -> "Ram's horns";
            case DRAGON_HORN_RIGHT, DRAGON_HORN_LEFT -> "Dragon horns";
            case SPINES -> "Dorsal spines";
            case SAIL -> "Back sail";
            case PLATES_RIGHT, PLATES_LEFT -> "Shoulder and hip plates";
            case NARWHAL -> "Narwhal horn";
            case CRYSTALS -> "Crystal growths";
        };
    }
}
