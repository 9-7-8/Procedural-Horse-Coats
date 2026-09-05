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

    @Test
    void unknownActsLikeAnOrdinaryBreed() {
        // pure breed + unknown -> a cross with "Unknown"
        BreedLineage c = BreedLineage.combine(FRIESIAN, BreedLineage.UNKNOWN);
        assertEquals(Kind.CROSS, c.kind());
        assertEquals("Friesian × Unknown cross", c.displayName());
        // unknown + unknown -> unknown
        assertEquals(Kind.UNKNOWN, BreedLineage.combine(BreedLineage.UNKNOWN, BreedLineage.UNKNOWN).kind());
    }

    @Test
    void tokensRoundTrip() {
        for (BreedLineage l : new BreedLineage[]{FRIESIAN, FR_AR, BreedLineage.MIXED, BreedLineage.UNKNOWN}) {
            assertEquals(l, BreedLineage.parse(l.toToken()), l.toToken());
        }
        assertEquals("cross:arabian+friesian", FR_AR.toToken());
    }

    @Test
    void blankAndNullParseToUnknown() {
        assertEquals(Kind.UNKNOWN, BreedLineage.parse(null).kind());
        assertEquals(Kind.UNKNOWN, BreedLineage.parse("").kind());
        assertEquals(Kind.UNKNOWN, BreedLineage.parse("unknown").kind());
    }
}
