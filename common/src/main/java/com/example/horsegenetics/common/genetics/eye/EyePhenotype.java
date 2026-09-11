package com.example.horsegenetics.common.genetics.eye;

/**
 * <b>A horse's eyes, resolved</b> - both of them, and the third one if it has
 * it. What {@link Eyes#resolve} returns and what the coat composer paints.
 *
 * @param right the eye on the head's west face - {@code CoatRegions.RIGHT_EYE}
 * @param left  the east face - {@code CoatRegions.LEFT_EYE}
 * @param third the eye in the middle of the forehead, or {@code null} for the
 *              horses that have two like everything else
 */
public record EyePhenotype(EyeRender right, EyeRender left, EyeRender third) {

    /** Two ordinary brown eyes and no third. */
    public static final EyePhenotype WILD = new EyePhenotype(EyeRender.WILD, EyeRender.WILD, null);

    public boolean hasThird() {
        return third != null;
    }

    /** The two eyes differ - complete heterochromia, or one of them sectoral. */
    public boolean heterochromatic() {
        return !right.equals(left);
    }

    /**
     * Must a horse with these eyes get a texture of its own? True as soon as any
     * eye carries a {@link EyeHue#CHAOS} anywhere, since chaos is read off the
     * allele copy and no two horses draw the same one.
     */
    public boolean varies() {
        return right.varies() || left.varies() || (third != null && third.varies());
    }
}
