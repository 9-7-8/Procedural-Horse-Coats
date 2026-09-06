package com.example.horsegenetics.common.breed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.horsegenetics.common.breed.BreedLineage.Kind;
import org.junit.jupiter.api.Test;

/** The owner's breed-label combination rules. */
class BreedLineageTest {

    private static final BreedLineage FRIESIAN = BreedLineage.pure("friesian");
    private static final BreedLineage ARABIAN = BreedLineage.pure("arabian");
    private static final BreedLineage QUARTER = BreedLineage.pure("quarter_horse");
    private static final BreedLineage FR_AR = BreedLineage.cross("friesian", "arabian");

    @Test
    void sameBreedStaysThatBreed() {
        assertEquals(FRIESIAN, BreedLineage.combine(FRIESIAN, FRIESIAN));
    }

    @Test
    void twoBreedsMakeACross_orderFree() {
        assertEquals(Kind.CROSS, BreedLineage.combine(FRIESIAN, ARABIAN).kind());
        assertEquals(BreedLineage.combine(FRIESIAN, ARABIAN), BreedLineage.combine(ARABIAN, FRIESIAN));
        assertEquals("Arabian × Friesian cross", BreedLineage.combine(ARABIAN, FRIESIAN).displayName());
    }

    @Test
    void sameCrossStaysThatCross() {
        assertEquals(FR_AR, BreedLineage.combine(FR_AR, FR_AR));
        assertEquals(FR_AR, BreedLineage.combine(FR_AR, BreedLineage.cross("arabian", "friesian")));
    }

    @Test
    void crossPlusOneOfItsBreedsStaysTheCross() {
        assertEquals(FR_AR, BreedLineage.combine(FR_AR, FRIESIAN));
        assertEquals(FR_AR, BreedLineage.combine(ARABIAN, FR_AR));
    }

    @Test
    void crossPlusAnOutsideBreedIsMixed() {
        assertSame(BreedLineage.MIXED, BreedLineage.combine(FR_AR, QUARTER));
    }

    @Test
    void twoDifferentCrossesAreMixed() {
        BreedLineage frQuarter = BreedLineage.cross("friesian", "quarter_horse");
        assertSame(BreedLineage.MIXED, BreedLineage.combine(FR_AR, frQuarter));
    }

    @Test
    void mixedIsAbsorbing() {
        assertSame(BreedLineage.MIXED, BreedLineage.combine(BreedLineage.MIXED, FRIESIAN));
        assertSame(BreedLineage.MIXED, BreedLineage.combine(QUARTER, BreedLineage.MIXED));
        assertSame(BreedLineage.MIXED, BreedLineage.combine(BreedLineage.MIXED, BreedLineage.MIXED));
    }

    /**
     * Feral Mixed is <b>absorbing</b>, exactly like Mixed. It used to combine as
     * an ordinary distinct breed ("Friesian x Unknown cross"), which forced the
     * model to answer whether a wild loner was a breed with a band, a pool and a
     * purity. Every cross involving it is now plain Mixed - including feral with
     * feral, so there is no back door to a "pure feral" line.
     */
    @Test
    void feralIsAbsorbingLikeMixed() {
        assertSame(BreedLineage.MIXED, BreedLineage.combine(FRIESIAN, BreedLineage.FERAL));
        assertSame(BreedLineage.MIXED, BreedLineage.combine(BreedLineage.FERAL, FRIESIAN));
        assertSame(BreedLineage.MIXED, BreedLineage.combine(FR_AR, BreedLineage.FERAL));
        assertSame(BreedLineage.MIXED, BreedLineage.combine(BreedLineage.FERAL, BreedLineage.FERAL));
        assertEquals("Feral Mixed", BreedLineage.FERAL.displayName());
    }

    @Test
    void tokensRoundTrip() {
        for (BreedLineage l : new BreedLineage[]{FRIESIAN, FR_AR, BreedLineage.MIXED, BreedLineage.FERAL}) {
            assertEquals(l, BreedLineage.parse(l.toToken()), l.toToken());
        }
        assertEquals("cross:arabian+friesian", FR_AR.toToken());
    }

    @Test
    void blankAndNullParseToFeral() {
        assertEquals(Kind.FERAL, BreedLineage.parse(null).kind());
        assertEquals(Kind.FERAL, BreedLineage.parse("").kind());
        assertEquals(Kind.FERAL, BreedLineage.parse("feral_mixed").kind());
    }
}
