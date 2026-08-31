package com.example.horsegenetics.common.horse;

/**
 * The lower and upper of a foal's two parents' {@code speed} / {@code health}
 * values, captured at birth. Purely so the UI can colour a foal's own stat
 * against where its parents sat: <b>above both</b> parents, <b>above one</b>,
 * or <b>below both</b>. Absent for founders and for records that predate the
 * field.
 */
public record ParentStats(double speedMin, double speedMax, double healthMin, double healthMax) {

    public static ParentStats of(double damSpeed, double sireSpeed, double damHealth, double sireHealth) {
        return new ParentStats(
                Math.min(damSpeed, sireSpeed), Math.max(damSpeed, sireSpeed),
                Math.min(damHealth, sireHealth), Math.max(damHealth, sireHealth));
    }

    /** -1 = below both parents, 0 = between (above exactly one), 1 = above both. */
    public int rankSpeed(double childSpeed) {
        return rank(childSpeed, speedMin, speedMax);
    }

    public int rankHealth(double childHealth) {
        return rank(childHealth, healthMin, healthMax);
    }

    private static int rank(double value, double min, double max) {
        if (value > max) {
            return 1;
        }
        if (value < min) {
            return -1;
        }
        return 0;
    }
}
