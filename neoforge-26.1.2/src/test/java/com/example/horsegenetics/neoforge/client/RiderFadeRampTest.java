package com.example.horsegenetics.neoforge.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <b>The one part of the ride fade that is arithmetic rather than rendering.</b>
 *
 * <p>Everything else about the fade needs a client with a screen on it and is
 * on the page's Verification tab. This is the ramp: which alpha a given pitch
 * asks for. It is testable only because {@link RiderFade#ramp} was written
 * without {@code Mth} - this module's test classpath carries no Minecraft on
 * purpose.
 *
 * <p>The cases that matter are the ends and the degenerate one. A ramp that
 * quietly returned zero somewhere would make the horse vanish, which the
 * owner's "never fully invisible" rule forbids outright, and a
 * {@code fullPitch} below {@code startPitch} is a config the user is allowed
 * to write and must not divide by zero.
 */
class RiderFadeRampTest {

    private static final float START = 30.0F;
    private static final float FULL = 75.0F;
    private static final float FLOOR = 0.25F;

    @Test
    @DisplayName("looking at or above the start pitch leaves the horse solid")
    void solidAboveStart() {
        assertEquals(1.0F, RiderFade.ramp(-90.0F, START, FULL, FLOOR), 1.0E-4F,
                "looking straight up faded the horse");
        assertEquals(1.0F, RiderFade.ramp(0.0F, START, FULL, FLOOR), 1.0E-4F,
                "looking at the horizon faded the horse");
        assertEquals(1.0F, RiderFade.ramp(START, START, FULL, FLOOR), 1.0E-4F,
                "the start pitch itself should still be fully solid");
    }

    @Test
    @DisplayName("looking at or past the full pitch reaches the floor, and never below it")
    void floorBelowFull() {
        assertEquals(FLOOR, RiderFade.ramp(FULL, START, FULL, FLOOR), 1.0E-4F);
        assertEquals(FLOOR, RiderFade.ramp(90.0F, START, FULL, FLOOR), 1.0E-4F,
                "straight down went past the floor - the horse must never disappear");
    }

    @Test
    @DisplayName("halfway between the two pitches is halfway to the floor")
    void linearBetween() {
        float mid = RiderFade.ramp((START + FULL) / 2.0F, START, FULL, FLOOR);
        assertEquals(1.0F - (1.0F - FLOOR) / 2.0F, mid, 1.0E-4F);
    }

    @Test
    @DisplayName("every pitch in range stays between the floor and solid")
    void neverLeavesTheRange() {
        for (float pitch = -90.0F; pitch <= 90.0F; pitch += 1.0F) {
            float alpha = RiderFade.ramp(pitch, START, FULL, FLOOR);
            assertTrue(alpha >= FLOOR && alpha <= 1.0F,
                    "pitch " + pitch + " gave alpha " + alpha + ", outside [" + FLOOR + ", 1]");
        }
    }

    @Test
    @DisplayName("a full pitch at or below the start pitch is a hard switch, not a crash")
    void degenerateConfigIsASwitch() {
        // The config permits this pair. Written naively it divides by zero and
        // paints the horse with NaN, which is a white flicker rather than an
        // error anybody would see reported.
        assertEquals(1.0F, RiderFade.ramp(29.0F, 30.0F, 30.0F, FLOOR), 1.0E-4F);
        assertEquals(FLOOR, RiderFade.ramp(31.0F, 30.0F, 30.0F, FLOOR), 1.0E-4F);
        // An inverted pair switches at the START pitch, not at the full one:
        // above it the horse is solid, below it the floor. Asserted both ways
        // round because the first draft of this test had it backwards, which
        // is a fair sign of how easy the pair is to misread.
        assertEquals(1.0F, RiderFade.ramp(50.0F, 60.0F, 10.0F, FLOOR), 1.0E-4F,
                "a pitch above the start pitch should be solid whatever the full pitch says");
        assertEquals(FLOOR, RiderFade.ramp(70.0F, 60.0F, 10.0F, FLOOR), 1.0E-4F,
                "a pitch past the start pitch should answer the floor when the pair is inverted");
        assertTrue(Float.isFinite(RiderFade.ramp(45.0F, 30.0F, 30.0F, FLOOR)),
                "a zero-width ramp produced a non-finite alpha");
    }

    @Test
    @DisplayName("a floor of 1 is the fade switched off")
    void floorOfOneIsNoFade() {
        for (float pitch = 0.0F; pitch <= 90.0F; pitch += 5.0F) {
            assertEquals(1.0F, RiderFade.ramp(pitch, START, FULL, 1.0F), 1.0E-4F);
        }
    }
}
