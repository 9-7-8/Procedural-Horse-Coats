package com.example.horsegenetics.common.parts;

/**
 * One box of an attached part, in the horse model's own units (1 unit = 1/16 block,
 * y DOWN, front is -z - the same space {@code HdHorseModel} is written in).
 *
 * <p>The box hangs from its own origin along <b>-y</b> (up, in model space): its cube is
 * {@code x,z in [-girth/2, +girth/2]}, {@code y in [-len, 0]}. A child sits at
 * {@code t} of the way along its parent's length (1 = the tip), then rotates. Chaining
 * nodes with a small rotation each is how a curve or a helix is built out of boxes with
 * no matrix maths: the parent's pose composes for us.
 *
 * <p>Roots ({@code parent < 0}) are placed at the anchor plus {@code (ox, oy, oz)}.
 *
 * @param tex which 16x16 region of the part sheet the box samples ({@link PartSheet})
 */
public record PartNode(int parent, float t, float ox, float oy, float oz,
                       float rx, float ry, float rz, float len, float girth, int tex) {
}
