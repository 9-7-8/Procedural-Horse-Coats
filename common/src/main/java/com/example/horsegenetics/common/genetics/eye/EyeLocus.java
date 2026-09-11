package com.example.horsegenetics.common.genetics.eye;

import com.example.horsegenetics.common.genetics.Genes;

/**
 * <b>The thirteen eye loci, named once</b> - the single source of truth for
 * their keys, which side of the head each belongs to, and which band each sits
 * in.
 *
 * <p>It exists because three different things need the same list and must not
 * grow three copies of it: the gene classes take their {@code key()} from here,
 * {@link Eyes#resolve} walks it to build a {@link EyePhenotype}, and
 * {@link EyeRequest} names its target with it rather than with a loose string.
 *
 * <h2>Why thirteen and not one</h2>
 * A horse's two eyes are not one trait. They can differ in colour, in whether
 * either has a sector of a second colour, in what colour that sector is,
 * in whether either glows, and in what colour the white round them is - and
 * every one of those is separately heritable, which is the whole design. One
 * "eye" locus with a hundred alleles would make every combination a lottery
 * ticket; thirteen small ones make it a breeding programme. See
 * {@code wiki/eye-colour.html}.
 *
 * <h2>The band split</h2>
 * Eight of them are <b>natural</b> - a real horse has an iris colour, a sclera
 * colour, and can have a sector of a second colour in one eye. Five are
 * <b>magical</b>: nothing glows in life and nothing has a third eye. That split
 * is the owner's call and it is the reason this enum carries
 * {@link #natural()} at all.
 */
public enum EyeLocus {

    IRIS_RIGHT("eye_colour_right", EyeSideRef.RIGHT, true),
    IRIS_LEFT("eye_colour_left", EyeSideRef.LEFT, true),

    SECTOR_RIGHT("eye_sector_right", EyeSideRef.RIGHT, true),
    SECTOR_LEFT("eye_sector_left", EyeSideRef.LEFT, true),

    SECTOR_COLOUR_RIGHT("eye_sector_colour_right", EyeSideRef.RIGHT, true),
    SECTOR_COLOUR_LEFT("eye_sector_colour_left", EyeSideRef.LEFT, true),

    SCLERA_RIGHT("eye_sclera_right", EyeSideRef.RIGHT, true),
    SCLERA_LEFT("eye_sclera_left", EyeSideRef.LEFT, true),

    GLOW_IRIS_RIGHT("eye_glow_iris_right", EyeSideRef.RIGHT, false),
    GLOW_IRIS_LEFT("eye_glow_iris_left", EyeSideRef.LEFT, false),

    GLOW_SCLERA_RIGHT("eye_glow_sclera_right", EyeSideRef.RIGHT, false),
    GLOW_SCLERA_LEFT("eye_glow_sclera_left", EyeSideRef.LEFT, false),

    /** The one locus with no side: it is the head's middle. */
    THIRD_EYE("eye_third", EyeSideRef.THIRD, false);

    /**
     * Which eye a locus belongs to, as a {@code CoatRegions.eyeRects} index.
     * A nested holder rather than a second enum because the only values it will
     * ever have are the two the sheet knows about, plus the forehead.
     */
    public static final class EyeSideRef {
        /** {@code CoatRegions.RIGHT_EYE} - the head's west face. */
        public static final int RIGHT = 0;
        /** {@code CoatRegions.LEFT_EYE} - the east face. */
        public static final int LEFT = 1;
        /** The forehead. Not an index into {@code eyeRects}. */
        public static final int THIRD = 2;

        private EyeSideRef() {}
    }

    private final String key;
    private final int side;
    private final boolean natural;

    EyeLocus(String slug, int side, boolean natural) {
        this.key = Genes.NS + "." + slug;
        this.side = side;
        this.natural = natural;
    }

    /** {@code horsegenetics.eye_*} - what the gene registers under. */
    public String key() {
        return key;
    }

    /** {@link EyeSideRef#RIGHT}, {@link EyeSideRef#LEFT} or {@link EyeSideRef#THIRD}. */
    public int side() {
        return side;
    }

    public boolean natural() {
        return natural;
    }

    /** This locus for the other eye - {@link #THIRD_EYE} has no twin and returns itself. */
    public EyeLocus twin() {
        return switch (this) {
            case IRIS_RIGHT -> IRIS_LEFT;
            case IRIS_LEFT -> IRIS_RIGHT;
            case SECTOR_RIGHT -> SECTOR_LEFT;
            case SECTOR_LEFT -> SECTOR_RIGHT;
            case SECTOR_COLOUR_RIGHT -> SECTOR_COLOUR_LEFT;
            case SECTOR_COLOUR_LEFT -> SECTOR_COLOUR_RIGHT;
            case SCLERA_RIGHT -> SCLERA_LEFT;
            case SCLERA_LEFT -> SCLERA_RIGHT;
            case GLOW_IRIS_RIGHT -> GLOW_IRIS_LEFT;
            case GLOW_IRIS_LEFT -> GLOW_IRIS_RIGHT;
            case GLOW_SCLERA_RIGHT -> GLOW_SCLERA_LEFT;
            case GLOW_SCLERA_LEFT -> GLOW_SCLERA_RIGHT;
            case THIRD_EYE -> THIRD_EYE;
        };
    }

    /** The iris locus for one {@code eyeRects} index. */
    public static EyeLocus iris(int eye) {
        return eye == EyeSideRef.RIGHT ? IRIS_RIGHT : IRIS_LEFT;
    }

    /** The sector locus for one {@code eyeRects} index. */
    public static EyeLocus sector(int eye) {
        return eye == EyeSideRef.RIGHT ? SECTOR_RIGHT : SECTOR_LEFT;
    }

    /** The sector-colour locus for one {@code eyeRects} index. */
    public static EyeLocus sectorColour(int eye) {
        return eye == EyeSideRef.RIGHT ? SECTOR_COLOUR_RIGHT : SECTOR_COLOUR_LEFT;
    }

    /** The sclera locus for one {@code eyeRects} index. */
    public static EyeLocus sclera(int eye) {
        return eye == EyeSideRef.RIGHT ? SCLERA_RIGHT : SCLERA_LEFT;
    }

    /** The glowing-iris locus for one {@code eyeRects} index. */
    public static EyeLocus glowIris(int eye) {
        return eye == EyeSideRef.RIGHT ? GLOW_IRIS_RIGHT : GLOW_IRIS_LEFT;
    }

    /** The glowing-sclera locus for one {@code eyeRects} index. */
    public static EyeLocus glowSclera(int eye) {
        return eye == EyeSideRef.RIGHT ? GLOW_SCLERA_RIGHT : GLOW_SCLERA_LEFT;
    }
}
