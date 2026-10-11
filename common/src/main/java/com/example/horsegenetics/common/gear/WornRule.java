package com.example.horsegenetics.common.gear;

/**
 * <b>The rule every worn piece obeys: nothing a horse wears is paper-thin.</b>
 * (Owner, 2026-10-09.)
 *
 * <p>A worn piece stands at least {@link #FLOOR} of a model unit off the horse,
 * and each piece chooses its own depth above that. Grown parts are not worn and
 * keep their own floor ({@code PartGeneratorTest}).
 *
 * <p>Every lift a piece declares goes through {@link #lift}, so a piece that
 * would be drawn flat fails where it is declared rather than in a photo.
 */
public final class WornRule {

    /** The least a worn piece may stand off the horse, in model units. */
    public static final float FLOOR = 0.25f;

    private WornRule() {
    }

    /** {@code lift}, or an exception if it is under the floor (or not a number). */
    public static float lift(float lift) {
        if (!(lift >= FLOOR) || Float.isInfinite(lift)) {
            throw new IllegalArgumentException(
                    "a worn piece stands at least " + FLOOR + " of a model unit off the horse, not " + lift);
        }
        return lift;
    }
}
