package com.example.horsegenetics.common.care;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The wheel's slices: up is slice 0, clockwise, a dead centre, and the edges. */
class CommandWheelGeometryTest {

    private static final double DEAD = 10;

    @Test
    void fourSlicesClockwiseFromTheTop() {
        // Screen y grows down, so "up" is negative dy.
        assertEquals(0, CommandWheel.sliceAt(0, -50, 4, DEAD));
        assertEquals(1, CommandWheel.sliceAt(50, 0, 4, DEAD));
        assertEquals(2, CommandWheel.sliceAt(0, 50, 4, DEAD));
        assertEquals(3, CommandWheel.sliceAt(-50, 0, 4, DEAD));
    }

    @Test
    void theDeadCentrePicksNothing() {
        assertEquals(-1, CommandWheel.sliceAt(0, 0, 4, DEAD));
        assertEquals(-1, CommandWheel.sliceAt(6, -6, 4, DEAD));
        assertEquals(-1, CommandWheel.sliceAt(0, -DEAD, 4, DEAD));
        assertEquals(0, CommandWheel.sliceAt(0, -DEAD - 0.01, 4, DEAD));
    }

    @Test
    void sliceEdgesAreHalfwayBetweenCentres() {
        // With four slices the top one spans -45..45 degrees.
        assertEquals(0, CommandWheel.sliceAt(49, -50, 4, DEAD));
        assertEquals(1, CommandWheel.sliceAt(51, -50, 4, DEAD));
        assertEquals(0, CommandWheel.sliceAt(-49, -50, 4, DEAD));
        assertEquals(3, CommandWheel.sliceAt(-51, -50, 4, DEAD));
    }

    @Test
    void farOutsideTheRingStillCounts() {
        assertEquals(2, CommandWheel.sliceAt(3, 5000, 4, DEAD));
    }

    @Test
    void anyCountWorks() {
        for (int n = 1; n <= 9; n++) {
            for (int s = 0; s < n; s++) {
                double a = CommandWheel.centreAngle(s, n);
                assertEquals(s, CommandWheel.sliceAt(Math.sin(a) * 40, -Math.cos(a) * 40, n, DEAD),
                        "slice " + s + " of " + n);
            }
        }
        assertEquals(-1, CommandWheel.sliceAt(0, -50, 0, DEAD));
    }
}
