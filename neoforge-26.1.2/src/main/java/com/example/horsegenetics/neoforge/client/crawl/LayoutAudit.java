package com.example.horsegenetics.neoforge.client.crawl;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/**
 * <b>The layout audit</b>: run alongside the crawl ({@code -PmenuCrawl=layout}, or
 * {@code queries,layout}), it looks at one drawn frame of every screen state the crawl
 * stops at and checks that nothing is in the way of reading it.
 *
 * <ul>
 *   <li><b>Boxes:</b> no two visible widgets overlap, and none runs off the screen.</li>
 *   <li><b>Text on the screen:</b> no string runs off its edge.</li>
 *   <li><b>Text not cut off:</b> no string is clipped at the side by the box it is drawn in
 *       (clipped at the top or bottom is a list mid-scroll, which is fine).</li>
 *   <li><b>Text big enough:</b> nothing shrunk below {@link #MIN_SCALE} to fit.</li>
 *   <li><b>Text not on text:</b> no two strings overlap, unless an opaque box drawn between
 *       them covers the first - a menu over a list is meant to hide what is under it.</li>
 *   <li><b>Text readable against its background:</b> contrast at least {@link #MIN_CONTRAST}
 *       (WCAG's figure for large text and controls) against the last solid box drawn under it.
 *       A textured background (a vanilla panel) has no one colour, so it is not judged.</li>
 * </ul>
 *
 * <p>Each distinct finding is a FAIL once per run, however many states show it; each audit
 * ends with one PASS line saying how much it looked at.
 */
final class LayoutAudit {

    /**
     * Below this text is drawn too small to read. {@code FitText.MIN_SCALE} is the same number
     * and {@code GuiText.drawFitted} stops there (#268); kept a separate constant on purpose, so
     * lowering the floor cannot also lower the bar that checks it.
     */
    static final float MIN_SCALE = 0.7f;
    /** WCAG 2's contrast for large text and UI components. */
    static final double MIN_CONTRAST = 3.0;
    /** How many findings one audit lists before it says how many more. */
    private static final int MAX_LISTED = 12;

    private static final Set<String> reported = new HashSet<>();

    private LayoutAudit() {
    }

    static boolean enabled() {
        return String.valueOf(System.getProperty(MenuCrawlClient.PROPERTY)).contains("layout");
    }

    /** Queue an audit of whatever is on screen when its turn comes. Nothing, unless enabled. */
    static void audit(String where) {
        if (!enabled()) {
            return;
        }
        queue.add(() -> {
            if (screen() == null) {
                return 0;
            }
            LayoutCapture.request();
            return 0;
        });
        queue.add(() -> {
            if (screen() == null) {
                return 0;
            }
            if (!LayoutCapture.ready()) {
                if (waited() >= 20) {
                    fail(where, "layout - no frame was drawn to look at");
                    return 0;
                }
                return AGAIN;
            }
            look(where, java.util.Objects.requireNonNull(screen()));
            return 0;
        });
    }

    private static void look(String where, Screen s) {
        List<String> found = new ArrayList<>();
        String screenName = s.getClass().getSimpleName();
        int w = s.width;
        int h = s.height;

        // Boxes: the widgets.
        List<AbstractWidget> shown = visibleWidgets(s);
        for (int i = 0; i < shown.size(); i++) {
            AbstractWidget a = shown.get(i);
            LayoutCapture.Rect ra = rect(a);
            if (ra.x0() < 0 || ra.y0() < 0 || ra.x1() > w || ra.y1() > h) {
                found.add("box \"" + label(a) + "\" runs off the " + w + "x" + h + " screen at " + show(ra));
            }
            for (int j = i + 1; j < shown.size(); j++) {
                AbstractWidget b = shown.get(j);
                LayoutCapture.Rect over = ra.intersect(rect(b));
                if (over != null && over.w() * over.h() >= 1) {
                    found.add("boxes \"" + label(a) + "\" and \"" + label(b) + "\" overlap at " + show(over));
                }
            }
        }

        // Text.
        List<LayoutCapture.Text> texts = new ArrayList<>();
        for (LayoutCapture.Text t : LayoutCapture.texts) {
            if (visible(t)) {
                texts.add(t);
            }
        }
        for (LayoutCapture.Text t : texts) {
            LayoutCapture.Rect b = t.box();
            if (b.x0() < -0.5f || b.y0() < -0.5f || b.x1() > w + 0.5f || b.y1() > h + 0.5f) {
                found.add("text \"" + clip(t.text()) + "\" runs off the screen at " + show(b));
            }
            LayoutCapture.Rect c = t.clip();
            if (c != null && (b.x0() < c.x0() - 0.5f || b.x1() > c.x1() + 0.5f)) {
                found.add("text \"" + clip(t.text()) + "\" is cut off at the side by its clip box " + show(c));
            }
            if (t.scale() < MIN_SCALE) {
                found.add("text \"" + clip(t.text()) + "\" is drawn at " + Math.round(t.scale() * 100)
                        + "% - too small to read");
            }
            Integer bg = background(t);
            if (bg != null) {
                double ratio = contrast(t.color(), bg);
                if (ratio < MIN_CONTRAST) {
                    found.add(String.format("text \"%s\" is hard to read: contrast %.1f:1, #%06X on #%06X",
                            clip(t.text()), ratio, t.color() & 0xFFFFFF, bg & 0xFFFFFF));
                }
            }
        }
        for (int i = 0; i < texts.size(); i++) {
            for (int j = i + 1; j < texts.size(); j++) {
                LayoutCapture.Text a = texts.get(i);
                LayoutCapture.Text b = texts.get(j);
                LayoutCapture.Rect over = a.box().intersect(b.box());
                if (over == null || over.w() * over.h() < 4 || covered(over, a.order(), b.order())) {
                    continue;
                }
                found.add("text \"" + clip(a.text()) + "\" and \"" + clip(b.text()) + "\" overlap at " + show(over));
            }
        }

        int listed = 0;
        int fresh = 0;
        for (String f : found) {
            if (!reported.add(screenName + "|" + f)) {
                continue;
            }
            fresh++;
            if (listed++ < MAX_LISTED) {
                fail(where, "layout - " + f);
            }
        }
        if (listed > MAX_LISTED) {
            fail(where, "layout - and " + (listed - MAX_LISTED) + " more like these");
        }
        pass(where, "layout - looked at " + texts.size() + " strings and " + shown.size() + " boxes; "
                + found.size() + " findings, " + fresh + " new");
    }

    /** Text the clip leaves none of was never on screen: not a reading problem. */
    private static boolean visible(LayoutCapture.Text t) {
        LayoutCapture.Rect c = t.clip();
        return c == null || c.intersect(t.box()) != null;
    }

    /** Does an opaque box, drawn after the first text and before the second, hide the overlap? */
    private static boolean covered(LayoutCapture.Rect over, int first, int second) {
        for (LayoutCapture.Box b : LayoutCapture.boxes) {
            if (b.order() > first && b.order() < second && b.box().contains(over)
                    && (b.textured() || alpha(b.color()) >= 0xC0)) {
                return true;
            }
        }
        return false;
    }

    /** The colour under the text's middle: the last solid box drawn there before it, if one colour. */
    private static @Nullable Integer background(LayoutCapture.Text t) {
        float cx = (t.box().x0() + t.box().x1()) / 2f;
        float cy = (t.box().y0() + t.box().y1()) / 2f;
        LayoutCapture.Box under = null;
        for (LayoutCapture.Box b : LayoutCapture.boxes) {
            if (b.order() < t.order() && b.box().contains(cx, cy)
                    && (b.textured() || alpha(b.color()) >= 0xC0)) {
                under = b;
            }
        }
        return under == null || under.textured() ? null : under.color();
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    /** WCAG 2 contrast ratio between two colours (alpha ignored). */
    static double contrast(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int rgb) {
        return 0.2126 * channel((rgb >> 16) & 0xFF) + 0.7152 * channel((rgb >> 8) & 0xFF)
                + 0.0722 * channel(rgb & 0xFF);
    }

    private static double channel(int c) {
        double v = c / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static LayoutCapture.Rect rect(AbstractWidget w) {
        return new LayoutCapture.Rect(w.getX(), w.getY(), w.getX() + w.getWidth(), w.getY() + w.getHeight());
    }

    private static String show(LayoutCapture.Rect r) {
        return Math.round(r.x0()) + "," + Math.round(r.y0()) + " " + Math.round(r.w()) + "x" + Math.round(r.h());
    }

    private static String clip(String text) {
        String t = text.strip();
        return t.length() <= 40 ? t : t.substring(0, 37) + "...";
    }
}
