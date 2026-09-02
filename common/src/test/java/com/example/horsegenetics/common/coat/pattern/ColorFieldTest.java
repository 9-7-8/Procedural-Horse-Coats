package com.example.horsegenetics.common.coat.pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase 3's accumulator: signed, uncapped, saturating, order-independent. */
class ColorFieldTest {

    private static final int SIZE = 8;

    @Test
    void aFreshFieldIsFullyTransparentBlack() {
        ColorField f = new ColorField(SIZE);
        assertEquals(0, f.argb(3, 3));
        assertFalse(f.isAbsolute(3, 3));
    }

    @Test
    void channelsAccumulatePastTheNominalRangeButOnlyConvertCapped() {
        ColorField f = new ColorField(SIZE);
        f.setArgb(2, 2, 0xFF204060);
        f.add(2, 2, 1000, 0, 0);
        assertEquals(1000 + 0x20, f.red(2, 2), "the accumulator keeps the overshoot");
        assertEquals(0xFF, (f.argb(2, 2) >> 16) & 0xFF, "conversion is where it caps");
        f.add(2, 2, -5000, 0, 0);
        assertTrue(f.red(2, 2) < 0, "and it can go negative");
        assertEquals(0, (f.argb(2, 2) >> 16) & 0xFF);
    }

    @Test
    void hugeContributionsSaturateInsteadOfWrapping() {
        ColorField f = new ColorField(SIZE);
        f.add(0, 0, Integer.MAX_VALUE / 2, 0, 0);
        f.add(0, 0, Integer.MAX_VALUE / 2, 0, 0);
        f.add(0, 0, Integer.MAX_VALUE / 2, 0, 0);
        assertEquals(Integer.MAX_VALUE, f.red(0, 0), "an always-blue horse must not wrap to black");
        assertEquals(0xFF, (f.argb(0, 0) >> 16) & 0xFF);

        ColorField g = new ColorField(SIZE);
        g.add(0, 0, 0, 0, Integer.MIN_VALUE / 2);
        g.add(0, 0, 0, 0, Integer.MIN_VALUE / 2);
        g.add(0, 0, 0, 0, Integer.MIN_VALUE / 2);
        assertEquals(Integer.MIN_VALUE, g.blue(0, 0));
        assertEquals(0, g.argb(0, 0) & 0xFF);
    }

    @Test
    void twoAdditiveGenesGiveTheSameCoatEitherWayRound() {
        ColorField red = new ColorField(SIZE);
        ColorField blue = new ColorField(SIZE);
        red.add(1, 1, 90, -10, 0);
        blue.add(1, 1, 0, 40, 200);

        ColorField a = seeded();
        a.apply(red);
        a.apply(blue);

        ColorField b = seeded();
        b.apply(blue);
        b.apply(red);

        assertEquals(a.argb(1, 1), b.argb(1, 1));
        assertEquals(a.red(1, 1), b.red(1, 1));
    }

    @Test
    void flatPaintReplacesTheAccumulatorAndSaysSo() {
        ColorField delta = new ColorField(SIZE);
        delta.set(4, 4, 255, 10, 20, 30);
        assertTrue(delta.isAbsolute(4, 4));
        assertFalse(delta.isAbsolute(4, 5));

        ColorField acc = seeded();
        acc.add(4, 4, 500, 500, 500); // whatever was there is thrown away
        acc.apply(delta);
        assertEquals(0xFF0A141E, acc.argb(4, 4));
    }

    @Test
    void colourAloneDoesNotShowOnATexelTheNaturalPhaseLeftTransparent() {
        ColorField acc = new ColorField(SIZE); // opacity 0 = dominant white / a splash marking
        ColorField tint = new ColorField(SIZE);
        tint.add(5, 5, 200, 0, 0);
        acc.apply(tint);
        assertEquals(0, acc.argb(5, 5) >>> 24, "still transparent - the gene has to ask for opacity");

        ColorField paint = new ColorField(SIZE);
        paint.add(5, 5, 0, 0, 0);
        paint.addOpacity(5, 5, 255);
        acc.apply(paint);
        assertEquals(0xFF, acc.argb(5, 5) >>> 24);
        assertEquals(200, (acc.argb(5, 5) >> 16) & 0xFF, "and the earlier tint was waiting underneath");
    }

    @Test
    void aCopyIsIndependentOfTheFieldItCameFrom() {
        ColorField f = seeded();
        ColorField copy = f.mutableCopy();
        copy.add(1, 1, 100, 100, 100);
        assertEquals(0xFF204060, f.argb(1, 1));
    }

    @Test
    void applyingADeltaOfTheWrongSizeIsRejected() {
        ColorField f = new ColorField(SIZE);
        try {
            f.apply(new ColorField(SIZE + 1));
            throw new AssertionError("expected an IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("px"));
        }
    }

    /** A field with one seeded texel, as phase 2 would leave it. */
    private static ColorField seeded() {
        ColorField f = new ColorField(SIZE);
        f.setArgb(1, 1, 0xFF204060);
        f.setArgb(4, 4, 0xFF204060);
        return f;
    }
}
