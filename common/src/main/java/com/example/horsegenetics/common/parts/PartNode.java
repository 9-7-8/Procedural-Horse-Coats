package com.example.horsegenetics.common.parts;

/**
 * <b>One box of an attached part</b>, in the horse model's own units (1 unit =
 * 1/16 block, <b>y is DOWN</b>, front is <b>-z</b> - the space
 * {@code HdHorseModel} is written in).
 *
 * <p>The box hangs from its own origin along <b>-y</b>, which is up: its cube
 * spans {@code x,z in [-girth/2, +girth/2]} and {@code y in [-len, 0]}. A child
 * sits at fraction {@link #t} along its parent's length ({@code 1} is the tip)
 * and then rotates. Chaining nodes with a small rotation each is how a curve or
 * a spiral is built out of axis-aligned boxes with <b>no matrix arithmetic
 * here</b>: the parent's pose composes for us, exactly as it does for the horse's
 * own ears on its own head.
 *
 * <p>Roots ({@code parent < 0}) sit at the anchor plus {@code (ox, oy, oz)}.
 *
 * <h2>The rotation order is vanilla's, and it is load-bearing</h2>
 * {@code ModelPart.translateAndRotate} builds {@code rotationZYX(rz, ry, rx)},
 * so a child's local geometry is turned about <b>x first</b>, then y, then z.
 * For a limb that hangs along -y this means:
 * <ul>
 *   <li><b>{@code rx}</b> tips the limb <i>forward</i> (toward -z) for a positive
 *       angle - it is the bend;</li>
 *   <li><b>{@code ry}</b> is a yaw <i>in the parent's frame</i>. On an untipped
 *       segment that is the same thing as a roll about the limb's own axis, which
 *       is what a spiral wants; on a tipped one it sweeps the bend sideways and
 *       the chain corkscrews instead of curving. That is why
 *       {@link HornGenerator} never gives one segment both.</li>
 * </ul>
 *
 * @param parent index into the node list of the box this hangs from, or any
 *               negative value for a root. <b>Always less than this node's own
 *               index</b> - a generator emits parents before children so the
 *               client can bake in one pass.
 * @param t      where along the parent this sits, 0 at its base and 1 at its tip
 * @param ox     extra offset from that point, model units
 * @param oy     extra offset from that point, model units (negative is up)
 * @param oz     extra offset from that point, model units (negative is forward)
 * @param rx     rotation about x, radians - the bend
 * @param ry     rotation about y, radians - the roll, on an untipped segment
 * @param rz     rotation about z, radians - the lean
 * @param len    how far the box reaches up its own axis, model units
 * @param girth  the box's square cross-section, model units
 * @param tex    which {@link PartSheet} region the box samples
 */
public record PartNode(int parent, float t, float ox, float oy, float oz,
                       float rx, float ry, float rz, float len, float girth, int tex) {

    /** Whether this box is rooted at the anchor rather than on another box. */
    public boolean isRoot() {
        return parent < 0;
    }
}
