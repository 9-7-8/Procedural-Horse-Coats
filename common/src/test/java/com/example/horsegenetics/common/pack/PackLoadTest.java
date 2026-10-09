package com.example.horsegenetics.common.pack;

import com.example.horsegenetics.common.cart.CartDraft;
import com.example.horsegenetics.common.trait.HorseTraits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pack load's claims, and the click box's, as assertions.
 *
 * <p>The ones that matter: an empty horse and a switched-off server lose
 * nothing, the default costs a pull-10 horse half its speed for a full chest
 * on each flank, pull moves the maximum, and a server can move every part of
 * the curve.
 */
class PackLoadTest {

    private static final double EPS = 1.0e-9;
    private static final long TWO_FULL_CHESTS = 2L * 27 * 64;

    @Test
    @DisplayName("an empty horse keeps all of its speed")
    void anEmptyHorseIsNotSlowed() {
        assertEquals(1.0, PackLoad.retention(PackLoad.Curve.DEFAULT, 0, HorseTraits.BASE_PULL), EPS);
        assertEquals(0.0, PackLoad.speedModifier(PackLoad.Curve.DEFAULT, 0, HorseTraits.BASE_PULL), EPS);
    }

    /** The owner's anchor for the default curve; the maximum is a whole number, hence the tolerance. */
    @Test
    @DisplayName("a pull-10 horse keeps half its speed under a full vanilla chest on each flank, by default")
    void theDefaultCurve() {
        assertEquals(0.5, PackLoad.retention(PackLoad.Curve.DEFAULT, TWO_FULL_CHESTS, 10), 1.0e-3);
        assertEquals(-0.5, PackLoad.speedModifier(PackLoad.Curve.DEFAULT, TWO_FULL_CHESTS, 10), 1.0e-3);
        // And what that leaves an ordinary horse: it moves, slowly.
        final double ordinary = PackLoad.retention(PackLoad.Curve.DEFAULT, TWO_FULL_CHESTS,
                HorseTraits.BASE_PULL);
        assertTrue(ordinary > 0.1 && ordinary < 0.2, "an ordinary horse under two full chests keeps " + ordinary);
    }

    @Test
    @DisplayName("every item costs the same on the default curve")
    void theDefaultIsLinear() {
        final double one = 1.0 - PackLoad.retention(PackLoad.Curve.DEFAULT, 1000, HorseTraits.BASE_PULL);
        final double two = 1.0 - PackLoad.retention(PackLoad.Curve.DEFAULT, 2000, HorseTraits.BASE_PULL);
        assertEquals(2.0 * one, two, EPS);
    }

    @Test
    @DisplayName("a stronger horse carries more, on the carts' own capacity curve")
    void pullIsTheCapacity() {
        final PackLoad.Curve curve = PackLoad.Curve.DEFAULT;
        assertEquals(curve.maxItems() * CartDraft.capacity(9), PackLoad.most(curve, 9), EPS);
        double last = 0.0;
        for (final double pull : new double[]{1, 3, 5, 7, 10}) {
            final double kept = PackLoad.retention(curve, TWO_FULL_CHESTS, pull);
            assertTrue(kept >= last, "pull " + pull + " kept " + kept + ", less than a weaker horse");
            last = kept;
        }
        assertTrue(PackLoad.retention(curve, TWO_FULL_CHESTS, 10)
                > PackLoad.retention(curve, TWO_FULL_CHESTS, HorseTraits.BASE_PULL));
    }

    @Test
    @DisplayName("at its maximum a horse is down to the configured floor, and never below it")
    void theMaximumIsAMaximum() {
        final PackLoad.Curve stops = PackLoad.Curve.DEFAULT;
        final long most = (long) Math.ceil(PackLoad.most(stops, HorseTraits.BASE_PULL));
        assertEquals(0.0, PackLoad.retention(stops, most, HorseTraits.BASE_PULL), EPS);
        assertEquals(0.0, PackLoad.retention(stops, most * 50, HorseTraits.BASE_PULL), EPS);

        final PackLoad.Curve crawls = new PackLoad.Curve(true, 0, 1000, 1.0, 0.25);
        assertEquals(0.25, PackLoad.retention(crawls, 1000, HorseTraits.BASE_PULL), EPS);
        assertEquals(0.25, PackLoad.retention(crawls, 99_999, HorseTraits.BASE_PULL), EPS);
    }

    @Test
    @DisplayName("a server may switch the weight off, set the maximum, and bend the curve")
    void aServerMaySetTheCurve() {
        assertEquals(1.0, PackLoad.retention(PackLoad.Curve.OFF, 1_000_000, HorseTraits.MIN_PULL), EPS);

        final PackLoad.Curve small = new PackLoad.Curve(true, 0, 640, 1.0, 0.0);
        assertEquals(0.5, PackLoad.retention(small, 320, HorseTraits.BASE_PULL), EPS);

        final PackLoad.Curve free = new PackLoad.Curve(true, 200, 1000, 1.0, 0.0);
        assertEquals(1.0, PackLoad.retention(free, 200, HorseTraits.BASE_PULL), EPS);
        assertEquals(0.5, PackLoad.retention(free, 600, HorseTraits.BASE_PULL), EPS);

        final PackLoad.Curve late = new PackLoad.Curve(true, 0, 1000, 2.0, 0.0);
        assertEquals(0.75, PackLoad.retention(late, 500, HorseTraits.BASE_PULL), EPS);
    }

    @Test
    @DisplayName("a nonsense config still makes a curve")
    void aBadCurveIsClamped() {
        final PackLoad.Curve bad = new PackLoad.Curve(true, 500, 0, Double.NaN, 7.0);
        assertEquals(1, bad.maxItems());
        assertEquals(0, bad.freeItems());
        assertEquals(PackLoad.DEFAULT_EXPONENT, bad.exponent(), EPS);
        assertEquals(1.0, bad.minSpeed(), EPS);
        final double kept = PackLoad.retention(bad, 10, HorseTraits.BASE_PULL);
        assertTrue(kept >= 0.0 && kept <= 1.0, "retention out of range: " + kept);
    }

    // ------------------------------------------------------------------
    // The click box
    // ------------------------------------------------------------------

    /** A player two blocks off the given flank of a horse facing +Z, looking straight at the chest. */
    private static PackBox.Side clickFrom(final double side, final boolean hasLeft, final boolean hasRight,
                                          final double scale) {
        final double eyeX = side * 2.0 * scale;
        final double eyeY = PackBox.CENTRE_UP * scale;
        final double eyeZ = PackBox.CENTRE_FORWARD * scale;
        return PackBox.hit(eyeX, eyeY, eyeZ, -side, 0, 0, 4.5, 0.0, scale, hasLeft, hasRight);
    }

    @Test
    @DisplayName("a click on a chest finds that chest, on either flank")
    void aClickOnTheChestHitsIt() {
        assertEquals(PackBox.Side.LEFT, clickFrom(1, true, true, 1.0));
        assertEquals(PackBox.Side.RIGHT, clickFrom(-1, true, true, 1.0));
    }

    @Test
    @DisplayName("a click passes through a flank with no chest on it to the one behind")
    void anEmptyFlankIsNotAChest() {
        assertEquals(PackBox.Side.RIGHT, clickFrom(1, false, true, 1.0));
        assertNull(clickFrom(1, false, false, 1.0));
    }

    @Test
    @DisplayName("a click on the horse's neck is not a click on its chest")
    void aClickElsewhereMisses() {
        // Same stance, aimed a block further forward - the shoulder.
        assertNull(PackBox.hit(2.0, PackBox.CENTRE_UP, PackBox.CENTRE_FORWARD + 1.0,
                -1, 0, 0, 4.5, 0.0, 1.0, true, true));
        // And out of reach entirely.
        assertNull(PackBox.hit(9.0, PackBox.CENTRE_UP, PackBox.CENTRE_FORWARD,
                -1, 0, 0, 4.5, 0.0, 1.0, true, true));
    }

    @Test
    @DisplayName("the box turns with the horse and grows with it")
    void theBoxFollowsYawAndScale() {
        // Facing -X (yaw 90), the near side is +Z.
        assertEquals(PackBox.Side.LEFT, PackBox.hit(-PackBox.CENTRE_FORWARD, PackBox.CENTRE_UP, 2.0,
                0, 0, -1, 4.5, 90.0, 1.0, true, true));
        assertEquals(PackBox.Side.LEFT, PackBox.sideOf(0, 2.0, 90.0));
        assertEquals(PackBox.Side.RIGHT, PackBox.sideOf(0, -2.0, 90.0));
        // A big horse's chest is higher and further out; the same aim scaled finds it.
        assertEquals(PackBox.Side.LEFT, clickFrom(1, true, true, 1.6));
        assertEquals(PackBox.Side.RIGHT, clickFrom(-1, true, true, 0.5));
        // And the small horse's chest is not where the big one's is.
        assertNull(PackBox.hit(2.0, PackBox.CENTRE_UP * 1.6, PackBox.CENTRE_FORWARD,
                -1, 0, 0, 4.5, 0.0, 0.5, true, true));
    }
}
