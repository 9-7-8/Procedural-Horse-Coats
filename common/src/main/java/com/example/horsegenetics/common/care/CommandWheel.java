package com.example.horsegenetics.common.care;

/**
 * <b>Which slice of the command whistle's wheel the cursor is over.</b> The wheel is a
 * ring of equal slices; slice 0 is centred straight up and they go clockwise. Pure
 * geometry, so the screen only measures the cursor and draws.
 *
 * <p>Screen coordinates: x grows right, y grows DOWN, as a GUI's do.
 */
public final class CommandWheel {

    private CommandWheel() {
    }

    /**
     * The slice under the cursor, or -1 for none: inside the dead centre (release there
     * cancels), or with no slices at all. Outside the ring still counts as its slice -
     * a player flicks the mouse past the edge, and that is still a choice.
     *
     * @param dx         cursor x minus the wheel's centre x
     * @param dy         cursor y minus the wheel's centre y
     * @param slices     how many slices the wheel has
     * @param deadRadius the radius of the centre that picks nothing
     */
    public static int sliceAt(double dx, double dy, int slices, double deadRadius) {
        if (slices <= 0 || dx * dx + dy * dy <= deadRadius * deadRadius) {
            return -1;
        }
        // Clockwise angle from straight up, in [0, 2pi). Up is -y on a screen.
        double angle = Math.atan2(dx, -dy);
        if (angle < 0) {
            angle += Math.PI * 2;
        }
        double width = Math.PI * 2 / slices;
        // Slice 0 is centred on the top, so shift by half a slice before flooring.
        int slice = (int) Math.floor((angle + width / 2) / width);
        return slice % slices;
    }

    /** The clockwise-from-up angle, in radians, of a slice's centre: where its label goes. */
    public static double centreAngle(int slice, int slices) {
        return Math.PI * 2 * slice / slices;
    }
}
