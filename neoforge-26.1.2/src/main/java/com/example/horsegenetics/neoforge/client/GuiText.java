package com.example.horsegenetics.neoforge.client;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * Small shared text helpers for this mod's screens - greedy pixel-width word
 * wrap, used by the gene browser and the per-horse gene inspector.
 */
public final class GuiText {

    private GuiText() {
    }

    /**
     * Greedy word-wrap of {@code s} to lines no wider than {@code maxWidthPx}.
     * Breaks only on spaces; a single word longer than the limit gets its own
     * (over-wide) line rather than being cut. An empty / blank input yields one
     * empty line so callers can advance a cursor unconditionally.
     */
    public static List<String> wrap(Font font, String s, int maxWidthPx) {
        List<String> lines = new ArrayList<>();
        if (s == null || s.isEmpty()) {
            lines.add("");
            return lines;
        }
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && font.width(candidate) > maxWidthPx) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(candidate);
            }
        }
        if (cur.length() > 0) {
            lines.add(cur.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    /** {@code s} clipped to {@code max} characters with a trailing ellipsis. */
    public static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }
}
