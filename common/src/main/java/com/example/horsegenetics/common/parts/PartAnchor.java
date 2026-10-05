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
    CROWN_LEFT,

    /**
     * The back of the skull just behind and outside the base of the <b>right</b>
     * ear, model {@code -x} - where a dragon horn roots, so it sits clear of the
     * antlers' {@link #CROWN_RIGHT} (forward of the ears) and the horn's
     * {@link #FOREHEAD}, and sweeps back over the neck rather than up.
     */
    NAPE_RIGHT,

    /** The same on the other side, model {@code +x}. */
    NAPE_LEFT,

    /**
     * The midline of the top of the back at the <b>withers</b>, just behind where
     * the neck and mane meet it. The first anchor off the head: it rides the body
     * bone, so the body's own animation (rearing) carries it as the head carries the
     * horn. A part here runs back along {@code +z} toward the croup - the dorsal
     * spines now, and the sail and crystals the body-parts treatment plans.
     */
    SPINE,

    /**
     * The <b>right</b> flank, model {@code -x}: the side of the body just under the
     * line of the back, halfway along it. It rides the body bone, as {@link #SPINE}
     * does. A part here hangs down the flank and runs along {@code z} both ways from
     * it - the shoulder and hip plates, a cluster each side of the saddle.
     */
    BODY_RIGHT,

    /** The same on the other side, model {@code +x}. */
    BODY_LEFT,

    /**
     * The centre of the <b>front of the muzzle</b>, pointing forward along the head's
     * own axis rather than up. It rides the head, as {@link #FOREHEAD} does, so it
     * stacks with a unicorn horn rather than sharing its place. A part here grows
     * straight out of the face - the narwhal horn (tusks treatment). The only anchor
     * whose part does not stand up from it: the client turns it to face forward.
     */
    SNOUT

    // The next anchors, named here only so the shape of the enum is obvious and
    // NOT declared until something draws them: JAW_x2 (boar tusks), LIP_x2 (sabre
    // fangs), WITHERS_PAIR (wings), HOOF_x4 (feathering, shoes). Each is one row in
    // the client's offset table and one entry here.
}
