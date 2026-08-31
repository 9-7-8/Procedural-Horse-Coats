package com.example.horsegenetics.common.coat.pattern;

/**
 * A coat pattern as a pure function of a point in horse body-space
 * (see {@link com.example.horsegenetics.common.coat.skin.HorseSkinGeometry}
 * for the axes). Given a body-space point it returns the colour that shades
 * it, packed as {@code 0xAARRGGBB}.
 *
 * <p>This is the shape every gene's pattern generator implements. A
 * {@code CoatSheetRasterizer} walks the texel grid, asks
 * {@code HorseSkinGeometry} what body-space point each texel covers, and
 * calls this to fill in the colour - so the pattern author never touches UVs
 * or mirroring, only the horse's real shape.
 */
@FunctionalInterface
public interface CoatPattern {

    /** ARGB ({@code 0xAARRGGBB}) for the coat at body-space {@code (x, y, z)}. */
    int argb(double x, double y, double z);
}
