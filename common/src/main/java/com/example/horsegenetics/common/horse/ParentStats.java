package com.example.horsegenetics.common.horse;

import com.example.horsegenetics.common.trait.Traits;

/**
 * The lower and upper of a foal's two parents' speed and max health, captured
 * at birth. Purely so the UI can colour a foal's own numbers against where its
 * parents sat: <b>above both</b>, <b>between</b> them, or <b>below both</b>.
 * Absent for founders.
 *
 * <p>It reads as a snapshot and it has to be one, even though both parents'
 * numbers are now derivable from their genotypes: a parent can be dead, sold,
 * eaten by a creeper or simply not loaded by the time anyone looks at the foal,
 * and the ancestry database is allowed to have forgotten it. Storing the two
 * numbers is what lets the comparison survive that.
 */
public record ParentStats(double speedMin, double speedMax, double healthMin, double healthMax) {

    /** From the two parents' resolved bodies - the ordinary way to build one. */
    public static ParentStats of(Traits dam, Traits sire) {
        return of(dam.speed(), sire.speed(), dam.health(), sire.health());
    }

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
