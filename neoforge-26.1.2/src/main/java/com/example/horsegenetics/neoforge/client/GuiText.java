package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.ui.FitText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Small shared text helpers for this mod's screens - greedy pixel-width word
 * wrap, used by the gene browser and the per-horse gene inspector, and the one
 * fitted-text draw every screen uses.
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

    /**
     * Draw {@code text} at {@code (x, y)} in at most {@code maxW} pixels, by
     * {@link FitText}: squeezed down to {@link FitText#MIN_SCALE}, past that cut with an
     * ellipsis, and the full text shown in a tooltip while the mouse is over the cut
     * line (#268). Every screen's fitted text comes here, so the rule is one rule.
     *
     * <p>The hover test is made at draw time, against the line's box mapped through
     * the current pose into screen space, and only where the scissor shows it; the
     * tooltip itself is deferred by vanilla to the end of the same frame
     * ({@code extractDeferredElements}), so it draws over the screen. The mouse is read
     * from {@code MouseHandler.getScaledX/YPos}, the same scaled coordinates a screen's
     * {@code mouseX} is made from - unverified on a screen that remaps its mouse.
     */
    public static void drawFitted(GuiGraphicsExtractor g, Font font, String text,
                                  int x, int y, int maxW, int colour, boolean shadow) {
        FitText.Fit fit = FitText.fit(text, maxW, font::width);
        if (fit.shown().isEmpty() && !fit.cut()) {
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        if (fit.scale() < 1f) {
            pose.scale(fit.scale());
        }
        if (!fit.shown().isEmpty()) {
            g.text(font, Component.literal(fit.shown()), 0, 0, colour, shadow);
        }
        if (fit.cut()) {
            tooltipIfHovered(g, font.width(fit.shown()), font.lineHeight, text);
        }
        pose.popMatrix();
    }

    /** {@link #drawFitted} with no shadow, as most of this mod's panels draw. */
    public static void drawFitted(GuiGraphicsExtractor g, Font font, String text,
                                  int x, int y, int maxW, int colour) {
        drawFitted(g, font, text, x, y, maxW, colour, false);
    }

    /** The current pose maps (0,0)-(w,h) to the line's box on screen. */
    private static void tooltipIfHovered(GuiGraphicsExtractor g, int w, int h, String full) {
        Minecraft mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double my = mc.mouseHandler.getScaledYPos(mc.getWindow());
        Vector2f a = g.pose().transformPosition(0, 0, new Vector2f());
        Vector2f b = g.pose().transformPosition(Math.max(w, 1), h, new Vector2f());
        if (mx >= Math.min(a.x, b.x) && mx < Math.max(a.x, b.x)
                && my >= Math.min(a.y, b.y) && my < Math.max(a.y, b.y)
                && g.containsPointInScissor((int) mx, (int) my)) {
            g.setTooltipForNextFrame(mc.font, Component.literal(full), (int) mx, (int) my);
        }
    }
}
