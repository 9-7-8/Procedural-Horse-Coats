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
    DRAGON_HORN_LEFT(PartAnchor.NAPE_LEFT, PartSheet.HORN);

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
        };
    }

    /**
     * Does a foal wear this part? A foal wears a half-size horn (owner's call,
     * 2026-09-30); it wears <b>no antlers</b> - the treatment's default (P5), and
     * the biology: antlers grow from pedicles that do not exist at birth - and no ram's
     * horns either, which come in with the rack's rule rather than the unicorn's.
     * Dragon horns are a hard part and come with maturity too (owner, 2026-10-01:
     * foals wear the soft parts only).
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
        };
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
}
