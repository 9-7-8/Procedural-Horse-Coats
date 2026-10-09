package com.example.horsegenetics.common.pack;

/**
 * <b>Where a chest hangs on a horse, and whether a click landed on it.</b>
 *
 * <p>One box per flank, in the horse's own frame: {@code left} is the horse's
 * near side, {@code up} is up, {@code forward} is toward its head, all in
 * blocks from the point between its hooves, for a horse of scale one. The
 * numbers are the body bone's - vanilla's equine body is ten model units wide
 * and the chest sits against it, just under the line of the back and behind
 * where a saddle goes - a pixel and a half forward of where vanilla hangs a
 * donkey's, which on this model ran past the rump (seen 2026-10-08).
 *
 * <p>The renderer draws the chest from the same three centre constants, so the
 * thing a player clicks is the thing they see. The click box is deliberately
 * a little larger than the drawn one: a chest on a walking horse is a small
 * target, and a near miss that mounts the horse instead is the worse mistake.
 *
 * <p>Pure geometry - no entity, no Minecraft - so the hit test is a unit test.
 */
public final class PackBox {

    /** Centre of the drawn chest, from the horse's midline, in blocks at scale one. */
    public static final double CENTRE_OUT = 7.5 / 16.0;
    /** Centre of the drawn chest above the ground. */
    public static final double CENTRE_UP = 16.5 / 16.0;
    /** Centre of the drawn chest along the horse; negative is behind its middle. */
    public static final double CENTRE_FORWARD = -4.5 / 16.0;

    /** The drawn chest: how thick it stands off the flank, and its height and length. */
    public static final double DRAWN_THICK = 5.0 / 16.0;
    public static final double DRAWN_SIZE = 8.0 / 16.0;

    /** Half the click box, each way. */
    static final double HALF_OUT = 0.22;
    static final double HALF_UP = 0.32;
    static final double HALF_FORWARD = 0.32;

    private PackBox() {
    }

    /** Which flank. */
    public enum Side {
        LEFT(1.0),
        RIGHT(-1.0);

        private final double sign;

        Side(final double sign) {
            this.sign = sign;
        }

        /** {@code +1} for the near side, {@code -1} for the off side. */
        public double sign() {
            return sign;
        }
    }

    /**
     * Which side of the horse a point is on - for choosing the flank a chest
     * goes on from where the player holding it is standing.
     *
     * @param dx,dz    the point, relative to the horse, in world axes
     * @param yawDegrees the horse's body yaw, Minecraft's convention (0 faces +Z)
     */
    public static Side sideOf(final double dx, final double dz, final double yawDegrees) {
        return left(dx, dz, yawDegrees) >= 0.0 ? Side.LEFT : Side.RIGHT;
    }

    /**
     * The flank whose chest this ray strikes first, or {@code null} if it
     * strikes neither of the ones that are there.
     *
     * @param ox,oy,oz   the ray's origin relative to the horse's feet, world axes
     * @param dx,dy,dz   the ray's direction, world axes (any length but zero)
     * @param reach      how far along the ray counts, in blocks
     * @param yawDegrees the horse's body yaw
     * @param scale      the horse's size, 1 for an ordinary one
     * @param hasLeft    whether there is a chest on the near side to hit
     * @param hasRight   whether there is one on the off side
     */
    public static Side hit(final double ox, final double oy, final double oz,
                           final double dx, final double dy, final double dz,
                           final double reach, final double yawDegrees, final double scale,
                           final boolean hasLeft, final boolean hasRight) {
        final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 0.0 || scale <= 0.0 || reach <= 0.0) {
            return null;
        }
        // Into the horse's frame, and into scale-one units, so the boxes below
        // never have to know how big this horse is.
        final double oLeft = left(ox, oz, yawDegrees) / scale;
        final double oUp = oy / scale;
        final double oFwd = forward(ox, oz, yawDegrees) / scale;
        final double dLeft = left(dx, dz, yawDegrees) / length;
        final double dUp = dy / length;
        final double dFwd = forward(dx, dz, yawDegrees) / length;
        final double limit = reach / scale;

        double best = Double.POSITIVE_INFINITY;
        Side found = null;
        for (final Side side : Side.values()) {
            if (side == Side.LEFT ? !hasLeft : !hasRight) {
                continue;
            }
            final double t = enter(
                    oLeft - side.sign() * CENTRE_OUT, oUp - CENTRE_UP, oFwd - CENTRE_FORWARD,
                    dLeft, dUp, dFwd);
            if (t <= limit && t < best) {
                best = t;
                found = side;
            }
        }
        return found;
    }

    /** Distance along a unit ray to the click box centred on the origin, or infinity. */
    private static double enter(final double ox, final double oy, final double oz,
                                final double dx, final double dy, final double dz) {
        double near = 0.0;
        double far = Double.POSITIVE_INFINITY;
        final double[] o = {ox, oy, oz};
        final double[] d = {dx, dy, dz};
        final double[] half = {HALF_OUT, HALF_UP, HALF_FORWARD};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(d[axis]) < 1.0e-9) {
                if (Math.abs(o[axis]) > half[axis]) {
                    return Double.POSITIVE_INFINITY;
                }
                continue;
            }
            double a = (-half[axis] - o[axis]) / d[axis];
            double b = (half[axis] - o[axis]) / d[axis];
            if (a > b) {
                final double swap = a;
                a = b;
                b = swap;
            }
            near = Math.max(near, a);
            far = Math.min(far, b);
            if (near > far) {
                return Double.POSITIVE_INFINITY;
            }
        }
        return near;
    }

    // Minecraft's yaw: 0 faces +Z, and turns clockwise seen from above, so a
    // horse facing +Z has +X on its left.
    private static double left(final double x, final double z, final double yawDegrees) {
        final double yaw = Math.toRadians(yawDegrees);
        return x * Math.cos(yaw) + z * Math.sin(yaw);
    }

    private static double forward(final double x, final double z, final double yawDegrees) {
        final double yaw = Math.toRadians(yawDegrees);
        return -x * Math.sin(yaw) + z * Math.cos(yaw);
    }
}
