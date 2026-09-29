package com.example.horsegenetics.common.parts;

/** What a gene or breed asks for. The client maps the kind to a layer pass; common never sees a mesh. */
public enum PartKind {
    /** One horn on the forehead. Bone/ivory, opaque. */
    HORN(PartAnchor.FOREHEAD, PartSheet.HORN, false),
    /** A mirrored pair of branching antlers behind the ears. */
    ANTLERS(PartAnchor.CROWN, PartSheet.BONE, false),
    /** A cluster of crystals along the spine. Translucent. */
    CRYSTALS(PartAnchor.SPINE, PartSheet.CRYSTAL, true);

    public final PartAnchor anchor;
    public final int defaultTex;
    public final boolean translucent;

    PartKind(PartAnchor anchor, int defaultTex, boolean translucent) {
        this.anchor = anchor;
        this.defaultTex = defaultTex;
        this.translucent = translucent;
    }
}
