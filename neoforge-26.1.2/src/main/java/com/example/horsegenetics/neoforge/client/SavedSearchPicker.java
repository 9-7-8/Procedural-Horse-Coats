package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.ClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The saved-search dropdown, written once and hung under any filter box.</b>
 *
 * <p>Every horse search box in the mod reads one list
 * ({@link ClientConfig#savedSearches()}), so a search saved on the <i>My
 * horses</i> tab is offered in the stasis bank and the breeding pickers and the
 * realm table without anything being copied between them. (Owner, 2026-09-26:
 * "allow the user to also save searches and use them in any place.")
 *
 * <h2>Naming, without a step in the way</h2>
 * Clicking <i>Save</i> opens a name field, and <b>Enter on an empty field saves
 * under the query itself</b> (owner). So a player who wants "keepers" types it,
 * and a player who does not press Enter twice and never thinks about naming
 * again - the feature is there for the searches worth naming and absent for the
 * ones that are not.
 *
 * <h2>Why it is a plain object and not a widget</h2>
 * The two screens that need it are a {@code Screen} and an
 * {@code AbstractContainerScreen} with quite different chrome, both of which
 * already hand-roll their own dropdowns - vanilla has no anchored list widget to
 * use. A third hand-rolled one inside a third screen is the duplication worth
 * avoiding; a real {@code AbstractWidget} would buy vanilla's focus handling and
 * cost the ability to draw last, over everything, which is the one thing an
 * anchored menu must do.
 *
 * <p>The name field is typed into the same way - a string this object owns, fed
 * by the screen forwarding {@link #charTyped} and {@link #keyPressed}. An
 * {@code EditBox} would have to be added to and removed from the screen's widget
 * list as the menu opens and closes, and would take focus away from the filter
 * box that is about to get it back.
 *
 * <p><b>Not verified in-game.</b>
 */
public final class SavedSearchPicker {

    private static final int ROW_H = 12;
    private static final int VISIBLE = 10;
    private static final int W = 240;
    private static final int BORDER = 0xFF20242E;
    private static final int FILL = 0xFF12141A;
    private static final int HOVER = 0xFF2E3542;
    private static final int FIELD = 0xFF0B0B10;
    private static final int TEXT = 0xFFE4E8F0;
    private static final int ACTION = 0xFF9BE08A;
    private static final int DIM = 0xFF6E7686;

    /** How long a name may be. Long enough to be a phrase, short enough to read. */
    private static final int NAME_MAX = 40;

    private boolean open;
    private boolean naming;
    private String typed = "";
    private int x;
    private int y;
    private int scroll;

    /** The query in the box when the menu was opened - what saving would save. */
    private String current = "";

    /** What a click produced: the query to put in the filter box. */
    public record Pick(String query) {
    }

    public boolean isOpen() {
        return open;
    }

    /** True while the name field has the keyboard, so the screen leaves it alone. */
    public boolean isNaming() {
        return open && naming;
    }

    public void close() {
        open = false;
        naming = false;
        typed = "";
    }

    /**
     * Open under a filter box. {@code query} is whatever is currently typed, so
     * that the top row can offer to save or forget exactly that.
     */
    public void open(int anchorX, int anchorY, String query, int screenW, int screenH) {
        open = true;
        naming = false;
        typed = "";
        scroll = 0;
        current = query == null ? "" : query.trim();
        x = Math.max(4, Math.min(anchorX, screenW - W - 4));
        int h = Math.min(VISIBLE, Math.max(1, rows().size())) * ROW_H;
        y = Math.max(4, Math.min(anchorY, screenH - h - 4));
    }

    /**
     * The menu, top to bottom: the action row for what is typed now, then every
     * saved search. Rebuilt on demand rather than cached - the list is a dozen
     * strings and it changes underneath the menu the moment a row is clicked.
     */
    private List<ClientConfig.SavedSearch> saved() {
        return ClientConfig.savedSearches();
    }

    private List<String> rows() {
        List<String> out = new ArrayList<>();
        out.add(action());
        for (ClientConfig.SavedSearch search : saved()) {
            out.add(search.label());
        }
        return out;
    }

    private String action() {
        if (current.isEmpty()) {
            return "type a search, then save it here";
        }
        if (ClientConfig.isSaved(current)) {
            return "Forget \"" + ClientConfig.nameOf(current) + "\"";
        }
        return "Save this search...";
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    public void draw(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        if (naming) {
            drawNameField(g, font);
            return;
        }
        List<String> rows = rows();
        int shown = Math.min(VISIBLE, rows.size());
        int h = shown * ROW_H;
        g.fill(x - 1, y - 1, x + W + 1, y + h + 1, BORDER);
        g.fill(x, y, x + W, y + h, FILL);

        for (int i = 0; i < shown && scroll + i < rows.size(); i++) {
            int index = scroll + i;
            int ry = y + i * ROW_H;
            if (mouseX >= x && mouseX < x + W && mouseY >= ry && mouseY < ry + ROW_H) {
                g.fill(x, ry, x + W, ry + ROW_H, HOVER);
            }
            boolean isAction = index == 0;
            int colour = isAction ? (current.isEmpty() ? DIM : ACTION) : TEXT;
            fitted(g, font, rows.get(index), x + 4, ry + 2, W - 8, colour);
        }
        if (rows.size() > VISIBLE) {
            fitted(g, font, "scroll for " + (rows.size() - VISIBLE) + " more",
                    x + 4, y + h + 2, W - 8, DIM);
        }
    }

    /**
     * Two lines: what you are naming, and the field. The hint in the empty field
     * is the query itself, because that is literally what pressing Enter now
     * would call it - the default is shown rather than described.
     */
    private void drawNameField(GuiGraphicsExtractor g, Font font) {
        int h = ROW_H * 3;
        g.fill(x - 1, y - 1, x + W + 1, y + h + 1, BORDER);
        g.fill(x, y, x + W, y + h, FILL);
        fitted(g, font, "Name this search - Enter to save, Escape to cancel",
                x + 4, y + 2, W - 8, DIM);
        g.fill(x + 3, y + ROW_H + 1, x + W - 3, y + ROW_H * 2 + 1, FIELD);
        if (typed.isEmpty()) {
            fitted(g, font, current, x + 6, y + ROW_H + 3, W - 12, DIM);
        } else {
            fitted(g, font, typed + "_", x + 6, y + ROW_H + 3, W - 12, TEXT);
        }
        fitted(g, font, "leave it empty to use the search itself",
                x + 4, y + ROW_H * 2 + 3, W - 8, DIM);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    /**
     * A click anywhere while the menu is open. It always closes - an open menu
     * eats the next click wherever it lands, so clicking away dismisses rather
     * than dismissing and doing something else as well.
     *
     * @return the query to put in the filter box, or {@code null} when the click
     *         only dismissed or only changed what is saved
     */
    public Pick click(double mx, double my) {
        if (!open) {
            return null;
        }
        if (naming) {
            // A click anywhere during naming is "not now". Saving is Enter,
            // which is where the hand already is.
            close();
            return null;
        }
        List<String> rows = rows();
        int shown = Math.min(VISIBLE, rows.size());
        int index = -1;
        if (mx >= x && mx < x + W && my >= y && my < y + shown * ROW_H) {
            int hit = scroll + (int) ((my - y) / ROW_H);
            if (hit >= 0 && hit < rows.size()) {
                index = hit;
            }
        }
        if (index < 0) {
            close();
            return null;
        }
        if (index == 0) {
            if (current.isEmpty()) {
                close();
                return null;
            }
            if (ClientConfig.isSaved(current)) {
                ClientConfig.forgetSearch(ClientConfig.nameOf(current));
                close();
                return null;
            }
            naming = true;      // stay open, on the name field
            typed = "";
            return null;
        }
        List<ClientConfig.SavedSearch> list = saved();
        int which = index - 1;
        close();
        if (which < 0 || which >= list.size()) {
            return null;
        }
        return new Pick(list.get(which).query());
    }

    /**
     * A letter, while the name field has the keyboard. Takes a codepoint rather
     * than a char because that is what the event carries, and appending it as
     * one keeps a name typed in a language outside the basic plane intact.
     */
    public boolean charTyped(int codepoint) {
        if (!isNaming()) {
            return false;
        }
        if (codepoint < ' ' || typed.length() >= NAME_MAX) {
            return true;    // swallowed: the field has the keyboard either way
        }
        typed += new String(Character.toChars(codepoint));
        return true;
    }

    /**
     * Enter saves, Escape cancels, Backspace deletes. Everything else is
     * swallowed while the field is up, so a letter cannot reach the screen
     * behind and close the window or change a tab.
     *
     * @return true when the key was the menu's
     */
    public boolean keyPressed(int key) {
        if (!isNaming()) {
            return false;
        }
        if (key == 257 || key == 335) {         // Enter, numpad Enter
            ClientConfig.saveSearch(typed, current);
            close();
            return true;
        }
        if (key == 256) {                       // Escape
            close();
            return true;
        }
        if (key == 259 && !typed.isEmpty()) {   // Backspace
            typed = typed.substring(0, typed.length() - 1);
            return true;
        }
        return true;
    }

    /** The wheel scrolls the list while it is open, and nothing behind it. */
    public boolean scroll(double sy) {
        if (!open || naming) {
            return open;
        }
        int max = Math.max(0, rows().size() - VISIBLE);
        scroll = Math.max(0, Math.min(scroll - (int) sy, max));
        return true;
    }

    /** Squeezed rather than clipped, so a long query still reads. */
    private static void fitted(GuiGraphicsExtractor g, Font font, String text,
                               int tx, int ty, int maxW, int colour) {
        float w = font.width(text);
        if (w <= maxW || w <= 0) {
            g.text(font, Component.literal(text), tx, ty, colour, false);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(tx, ty);
        pose.scale(maxW / w);
        g.text(font, Component.literal(text), 0, 0, colour, false);
        pose.popMatrix();
    }
}
