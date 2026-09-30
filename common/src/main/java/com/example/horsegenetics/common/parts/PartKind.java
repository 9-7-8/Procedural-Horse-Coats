package com.example.horsegenetics.common.parts;

/**
 * <b>What a gene or a breed asks for.</b> One value per kind of grown part; the
 * client maps a kind to a render pass, and {@code common/} never sees a mesh.
 *
 * <p>There is exactly one today. That is the point of the slice rather than an
 * oversight: the registry, the layer and the placement contract are built once
 * and the second kind is a value here plus a generator. A kind nothing draws
 * would be a promise the game does not keep, so kinds arrive with their
 * geometry, not ahead of it.
 */
public enum PartKind {

    /**
     * A single tapering horn on the forehead - the unicorn locus. Opaque, ivory
     * by default, and the one part in the mod that has no seeded variation at
     * all: a horn is a cone, and everything that differs between two horses'
     * horns is length, girth, twist, lean and colour.
     */
    HORN(PartAnchor.FOREHEAD, PartSheet.HORN);

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
        };
    }
}
