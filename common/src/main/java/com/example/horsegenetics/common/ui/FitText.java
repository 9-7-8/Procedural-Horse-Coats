package com.example.horsegenetics.common.ui;

import java.util.function.ToIntFunction;

/**
 * How a line of text fits a cell narrower than it (#268). Every screen once
 * squeezed the whole string to the cell, which on a narrow window drew it at
 * 18-26% of its size. Now text squeezes only down to {@link #MIN_SCALE}; past
 * that it is cut and ends in {@link #ELLIPSIS}, and the screen shows the full
 * text in a tooltip on hover (owner, 2026-10-23).
 *
 * <p>The client's {@code GuiText.drawFitted} draws the result. The width of a
 * string is passed in, so the rule needs no font.
 */
public final class FitText {

    /** The smallest text is drawn at. The layout audit's own bar ({@code LayoutAudit.MIN_SCALE}). */
    public static final float MIN_SCALE = 0.7f;

    public static final String ELLIPSIS = "…";

    /**
     * @param shown the text to draw: the whole of it, or a prefix ending in {@link #ELLIPSIS}
     *              ("" when the cell cannot hold even the ellipsis)
     * @param scale what to draw it at, from {@link #MIN_SCALE} to 1
     * @param cut   whether {@code shown} is not the whole text, so a tooltip owes the rest
     */
    public record Fit(String shown, float scale, boolean cut) {
    }

    private FitText() {
    }

    public static Fit fit(String text, int maxW, ToIntFunction<String> width) {
        if (text == null || text.isEmpty()) {
            return new Fit("", 1f, false);
        }
        if (maxW <= 0) {
            return new Fit("", 1f, true);
        }
        int w = width.applyAsInt(text);
        if (w <= maxW) {
            return new Fit(text, 1f, false);
        }
        float squeeze = (float) maxW / w;
        if (squeeze >= MIN_SCALE) {
            return new Fit(text, squeeze, false);
        }
        // Cut to the widest prefix that, with the ellipsis, fits at the smallest scale.
        float budget = maxW / MIN_SCALE;
        int end = text.length();
        String shown = null;
        while (end > 0) {
            end = text.offsetByCodePoints(end, -1);
            String prefix = stripTrailingSpace(text.substring(0, end));
            String candidate = prefix + ELLIPSIS;
            if (!prefix.isEmpty() && width.applyAsInt(candidate) <= budget) {
                shown = candidate;
                break;
            }
        }
        if (shown == null) {
            return width.applyAsInt(ELLIPSIS) <= budget
                    ? new Fit(ELLIPSIS, Math.min(1f, (float) maxW / width.applyAsInt(ELLIPSIS)), true)
                    : new Fit("", 1f, true);
        }
        return new Fit(shown, Math.min(1f, (float) maxW / width.applyAsInt(shown)), true);
    }

    private static String stripTrailingSpace(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }
}
