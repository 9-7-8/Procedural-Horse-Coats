package com.example.horsegenetics.common.ui;

import org.junit.jupiter.api.Test;

import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** #268: fitted text never shrinks below {@link FitText#MIN_SCALE}; past that it is cut. */
class FitTextTest {

    /** Six pixels a character, as a stand-in for the font. */
    private static final ToIntFunction<String> WIDTH = s -> 6 * s.codePointCount(0, s.length());

    @Test
    void textThatFitsIsDrawnWholeAtFullSize() {
        FitText.Fit fit = FitText.fit("Speed", 60, WIDTH);
        assertEquals("Speed", fit.shown());
        assertEquals(1f, fit.scale());
        assertFalse(fit.cut());
    }

    @Test
    void aSlightOverrunIsSqueezedNotCut() {
        FitText.Fit fit = FitText.fit("0123456789", 50, WIDTH); // 60 px into 50
        assertEquals("0123456789", fit.shown());
        assertEquals(50f / 60f, fit.scale(), 1e-6);
        assertFalse(fit.cut());
    }

    @Test
    void aLongLineStopsShrinkingAndIsCutWithAnEllipsis() {
        String where = "overworld -535, 71, 59 and a great deal more besides"; // the 26% case
        FitText.Fit fit = FitText.fit(where, 60, WIDTH);
        assertTrue(fit.cut());
        assertTrue(fit.scale() >= FitText.MIN_SCALE, "scale " + fit.scale());
        assertTrue(fit.shown().endsWith(FitText.ELLIPSIS), fit.shown());
        assertTrue(WIDTH.applyAsInt(fit.shown()) * fit.scale() <= 60 + 1e-3, "overruns its cell");
        String kept = fit.shown().substring(0, fit.shown().length() - FitText.ELLIPSIS.length());
        assertTrue(where.startsWith(kept), kept);
        assertFalse(kept.endsWith(" "), "a cut never leaves a space before the ellipsis");
    }

    @Test
    void aCutNeverSplitsASurrogatePair() {
        String text = "🐴".repeat(40);
        FitText.Fit fit = FitText.fit(text, 30, WIDTH);
        String kept = fit.shown().substring(0, fit.shown().length() - FitText.ELLIPSIS.length());
        assertEquals(0, kept.length() % 2);
    }

    @Test
    void aCellTooNarrowForTheEllipsisShowsNothing() {
        FitText.Fit fit = FitText.fit("Bay", 2, WIDTH);
        assertEquals("", fit.shown());
        assertTrue(fit.cut());
    }

    @Test
    void emptyAndNullAreHarmless() {
        assertEquals("", FitText.fit("", 10, WIDTH).shown());
        assertEquals("", FitText.fit(null, 10, WIDTH).shown());
        assertEquals("", FitText.fit("abc", 0, WIDTH).shown());
        assertTrue(FitText.fit("abc", 0, WIDTH).cut());
    }
}
