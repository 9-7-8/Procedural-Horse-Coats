package com.example.horsegenetics.common.trait;

/**
 * <b>Body attributes in units a player has a feel for.</b> Speed as metres per
 * second, jump strength as the height in metres the horse can actually clear.
 *
 * <p>Both of these are <i>display</i> conversions. Nothing here feeds the
 * simulation - the attributes remain the attributes, and this is the last step
 * before a number reaches a screen. It lives in {@code common/} because it is
 * arithmetic on two doubles with no Minecraft in it, and because the browser
 * designer shows the same two stats.
 *
 * <h2>Speed</h2>
 * A ridden horse travels on the <b>player's</b> movement model, not the mob
 * one, which is why {@link #METRES_PER_SECOND_PER_SPEED} is the same constant
 * that turns a walking player's {@code 0.1} into {@code 4.317} m/s. Per tick a
 * living entity accelerates by its speed attribute and then keeps {@code 0.546}
 * of its velocity (block friction {@code 0.6} times the standard {@code 0.91}),
 * so the distance covered in a steady tick converges on
 *
 * <pre>{@code   d = a + 0.546a/(1 - 0.546) = 2.2026a}</pre>
 *
 * and the rider's own forward input carries vanilla's {@code 0.98} factor, so
 * over twenty ticks {@code 0.98 * 2.2026 * 20 = 43.17} metres per second per
 * point of speed. That is the number {@code HorseSpeedFloor} reasons with in
 * prose ({@code 0.1125} is about 4.8 m/s) and the one the breed designer's
 * vanilla ceiling ({@code 0.3375}, "about 14.6 m/s") already assumes.
 *
 * <h2>Jump</h2>
 * Jump height is <b>not</b> linear in jump strength - the horse is thrown
 * upward and then fights gravity and drag, so the height is a cubic in the
 * attribute. {@link #jumpMetres} is the standard fit, and it is what the breed
 * sheet's metre figures were computed with. Vanilla's own roll,
 * {@code 0.4}-{@code 0.8}, is about 1.1 to 3.6 blocks; the familiar "horses
 * jump 5.3 blocks" figure is jump strength {@code 1.0}, which vanilla never
 * hands out but this mod's genetics reach comfortably.
 *
 * <p><b>Unverified against this mod's own horses.</b> The fit assumes vanilla
 * jump physics at body scale 1.0 and normal gravity. A horse carrying the size
 * genes, or one of the magical jump genes that touches gravity, may clear
 * something different from what this prints - see
 * {@code wiki/horse-body.html#verification}.
 */
public final class HorseUnits {

    /**
     * Metres per second per point of {@code MOVEMENT_SPEED} for a ridden horse.
     * See the class note for where it comes from; it is a property of the
     * player movement model, not a tuning knob, so changing it is claiming the
     * derivation is wrong.
     */
    public static final double METRES_PER_SECOND_PER_SPEED = 43.17;

    private HorseUnits() {
    }

    /** A {@code MOVEMENT_SPEED} attribute value as metres per second. */
    public static double metresPerSecond(final double movementSpeed) {
        return Math.max(movementSpeed, 0.0) * METRES_PER_SECOND_PER_SPEED;
    }

    // The cubic fit. Named coefficients rather than a literal-studded
    // expression so that the one that is a straight offset is visibly a
    // straight offset, and so a future refit has somewhere to land.
    private static final double J3 = -0.1817584952;
    private static final double J2 = 3.689713992;
    private static final double J1 = 2.128599134;
    private static final double J0 = -0.343930367;

    /**
     * The highest block a horse with this {@code JUMP_STRENGTH} can get onto,
     * in metres.
     *
     * <p>Clamped at zero: the fit crosses into negative height below a jump
     * strength of about {@code 0.14}, which is well under anything vanilla
     * rolls but comfortably inside what this mod's genetics can resolve to. A
     * horse that cannot leave the ground should read {@code 0}, not a negative
     * height.
     */
    public static double jumpMetres(final double jumpStrength) {
        final double x = Math.max(jumpStrength, 0.0);
        return Math.max(((J3 * x + J2) * x + J1) * x + J0, 0.0);
    }
}
