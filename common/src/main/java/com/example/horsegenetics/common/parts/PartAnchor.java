package com.example.horsegenetics.common.parts;

/**
 * <b>Where on the horse a part is rooted.</b>
 *
 * <p>Deliberately an enum and <b>not a bone name</b>. The client owns the
 * mapping from an anchor to a chain of {@code ModelPart}s and an offset, so
 * {@code common/} never learns Minecraft's part names (hard rule 1) and
 * retargeting an anchor - "the horn sits a little further forward" - is a client
 * edit rather than a gene edit.
 *
 * <p>It is also what keeps the adult and the foal from needing two genes. The
 * two horse models do <b>not</b> share a head origin: the adult's head box is
 * {@code (-3,-11,-2)..(3,-6,5)} and the foal's is
 * {@code (-3,-3.95,-6.71)..(3,0.05,2.30)}. An anchor names the <i>place</i>, and
 * the client keeps one offset per model for it.
 */
public enum PartAnchor {

    /**
     * The flat of the skull between the eyes and forward of the ears, pointing
     * up and a little forward. Where a horn goes.
     */
    FOREHEAD,

    /**
     * The top of the skull just inside and forward of the <b>right</b> ear, where
     * an antler's pedicle would be. Model {@code -x}, the side of vanilla's own
     * {@code right_ear}.
     */
    CROWN_RIGHT,

    /** The same on the other side, model {@code +x}. */
    CROWN_LEFT

    // The next anchors, named here only so the shape of the enum is obvious and
    // NOT declared until something draws them: SPINE (along the back, for
    // crystals and dorsal spines), WITHERS_PAIR (wings), HOOF_x4 (feathering,
    // shoes). Each is one row in the client's offset table and one entry here.
}
