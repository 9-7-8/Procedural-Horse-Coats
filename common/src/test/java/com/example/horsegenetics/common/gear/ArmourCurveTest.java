package com.example.horsegenetics.common.gear;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A modded horse armour follows its mod's chestplate (#296), along vanilla's plain rungs. */
class ArmourCurveTest {

    // Vanilla 26.1.2's chest -> horse pairs for leather, copper, iron and diamond,
    // and netherite's horse value as the cap. The game reads these off
    // ArmorMaterials at run time; they are written here only to pin the curve.
    private static final List<ArmourCurve.Rung> PLAIN = List.of(
            new ArmourCurve.Rung(3, 3), new ArmourCurve.Rung(4, 4),
            new ArmourCurve.Rung(6, 5), new ArmourCurve.Rung(8, 11));
    private static final int CAP = 19;

    @Test
    @DisplayName("a chestplate worth a vanilla rung gets that rung's horse armour")
    void eachRungMapsToItself() {
        for (ArmourCurve.Rung rung : PLAIN) {
            assertEquals(rung.body(), ArmourCurve.bodyFor(rung.chest(), PLAIN, CAP), "chest " + rung.chest());
        }
    }

    @Test
    @DisplayName("between two rungs it is the straight line: a 7 chestplate is 8 on a horse")
    void betweenRungsIsLinear() {
        assertEquals(8, ArmourCurve.bodyFor(7, PLAIN, CAP));
        assertEquals(5, ArmourCurve.bodyFor(5.9, PLAIN, CAP));
    }

    @Test
    @DisplayName("past diamond it carries on diamond's slope, and never past netherite")
    void aboveTheTopCarriesOnToTheCap() {
        assertEquals(14, ArmourCurve.bodyFor(9, PLAIN, CAP));
        assertEquals(17, ArmourCurve.bodyFor(10, PLAIN, CAP));
        assertEquals(CAP, ArmourCurve.bodyFor(30, PLAIN, CAP));
    }

    @Test
    @DisplayName("below leather it is in proportion, and never nothing")
    void belowTheBottomIsProportional() {
        assertEquals(2, ArmourCurve.bodyFor(2, PLAIN, CAP));
        assertEquals(1, ArmourCurve.bodyFor(0.1, PLAIN, CAP));
    }

    @Test
    @DisplayName("more chestplate never means less horse armour")
    void monotone() {
        int last = 0;
        for (double chest = 0.25; chest <= 20; chest += 0.25) {
            int body = ArmourCurve.bodyFor(chest, PLAIN, CAP);
            assertTrue(body >= last, "chest " + chest + " gave " + body + " after " + last);
            last = body;
        }
    }

    @Test
    @DisplayName("the rungs' order does not matter, and one rung is a straight proportion")
    void orderAndOneRung() {
        assertEquals(8, ArmourCurve.bodyFor(7, List.of(PLAIN.get(3), PLAIN.get(0), PLAIN.get(2), PLAIN.get(1)), CAP));
        assertEquals(10, ArmourCurve.bodyFor(12, List.of(new ArmourCurve.Rung(6, 5)), CAP));
    }

    @Test
    @DisplayName("no reading and a non-function are refused, not guessed at")
    void refusals() {
        assertThrows(IllegalArgumentException.class, () -> ArmourCurve.bodyFor(0, PLAIN, CAP));
        assertThrows(IllegalArgumentException.class, () -> ArmourCurve.bodyFor(Double.NaN, PLAIN, CAP));
        assertThrows(IllegalArgumentException.class, () -> ArmourCurve.bodyFor(5, List.of(), CAP));
        assertThrows(IllegalArgumentException.class, () -> ArmourCurve.bodyFor(5,
                List.of(new ArmourCurve.Rung(8, 11), new ArmourCurve.Rung(8, 19)), CAP));
    }
}
