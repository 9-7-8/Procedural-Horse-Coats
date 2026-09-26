package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.neoforge.ClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>Pin the Genes tab to the handful of loci you are actually breeding for.</b>
 *
 * <p>(Owner, 2026-09-26: "I should be able to filter the genes tab of any horse
 * to only show specific genes (and I can pick more than one) and it should
 * remember that between me closing... So like, just show me what alleles the
 * horse has for Flight, Healer, Waterborn, and so on.")
 *
 * <p>The existing filter on that tab is a single toggle between "every locus"
 * and "only what it carries", and on a horse carrying two dozen variants the
 * second is still a page of rows you have to read past. A breeder watching one
 * cross wants three of them, on every horse, until the cross is done.
 *
 * <h2>What is offered, and why not all of it</h2>
 * The list is the genes <b>some horse you own actually carries a variant of</b>
 * ({@code RosterGenetics.genesPresent}), plus whatever the horse in front of you
 * carries - the host passes both in. Every horse carries every registered gene,
 * so the honest full list is the whole registry and is not a list anybody can
 * use. The viewed horse is unioned in because it may be a stranger's or a
 * cowboy's, and so in no roster: a picker that cannot offer the gene on the
 * screen in front of you is broken in the one case you are looking at it.
 *
 * <h2>Multi-select, so a click does not close it</h2>
 * Unlike every other dropdown in the mod this one is a set, not a choice, so
 * clicking a row toggles it and the menu <b>stays open</b> - picking three
 * genes is three clicks and one dismissal, not three round trips. Clicking off
 * the menu closes it, which is the gesture the other dropdowns already teach.
 *
 * <h2>Where the selection lives</h2>
 * In {@link ClientConfig#geneFilter()}, on disk, as gene keys - so it survives
 * closing the screen, which was the point, and also a restart. This object
 * reads and writes it directly rather than handing a set back to the screen,
 * for the reason {@link SavedSearchPicker} does the same: one list, one owner,
 * and no chance of a screen that forgot to save.
 *
 * <p><b>Not verified in-game.</b>
 */
public final class GeneFilterPicker {

    private static final int ROW_H = 12;
    private static final int VISIBLE = 12;
    private static final int W = 190;
    private static final int BORDER = 0xFF20242E;
    private static final int FILL = 0xFF12141A;
    private static final int HOVER = 0xFF2E3542;
    private static final int TEXT = 0xFFE4E8F0;
    private static final int ACTION = 0xFF9BE08A;
    private static final int DIM = 0xFF6E7686;

    private boolean open;

    /** Alphabetical order plus type-to-jump, the same as every other menu. */
    private final TypeAhead typeAhead = new TypeAhead();

    private int cursor;
    private int x;
    private int y;
    private int scroll;

    /** The genes this menu may offer, alphabetical by display name. */
    private List<Gene> options = List.of();

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        cursor = 0;
        typeAhead.reset();
    }

    /**
     * Open under the button. {@code offered} is the herd's genes plus the viewed
     * horse's; it is sorted here so that every caller cannot forget to.
     */
    public void open(int anchorX, int anchorY, int screenW, int screenH, List<Gene> offered) {
        open = true;
        scroll = 0;
        cursor = 0;
        typeAhead.reset();
        List<Gene> sorted = new ArrayList<>(offered);
        sorted.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
        options = List.copyOf(sorted);
        x = Math.max(4, Math.min(anchorX, screenW - W - 4));
        int h = Math.min(VISIBLE, Math.max(1, rows().size())) * ROW_H;
        y = Math.max(4, Math.min(anchorY, screenH - h - 4));
    }

    /**
     * The menu top to bottom: the clear row, then every gene on offer. The clear
     * row is not a gene and is pinned at the top so it does not move about under
     * the pointer as the type-ahead walks the list.
     */
    private List<String> rows() {
        List<String> out = new ArrayList<>();
        out.add(pinned().isEmpty() ? "Showing every locus" : "Show every locus again");
        for (Gene gene : options) {
            out.add((pinned().contains(gene.key()) ? "[x] " : "[ ] ") + gene.name());
        }
        return out;
    }

    /** Just the gene names, for the type-ahead - it must not jump to the checkbox. */
    private List<String> names() {
        List<String> out = new ArrayList<>();
        out.add("");    // the clear row, which nothing should ever jump to
        for (Gene gene : options) {
            out.add(gene.name());
        }
        return out;
    }

    private static Set<String> pinned() {
        return new LinkedHashSet<>(ClientConfig.geneFilter());
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    public void draw(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        List<String> rows = rows();
        int shown = Math.min(VISIBLE, rows.size());
        int h = shown * ROW_H;
        g.fill(x - 1, y - 1, x + W + 1, y + h + 1, BORDER);
        g.fill(x, y, x + W, y + h, FILL);

        Set<String> on = pinned();
        for (int i = 0; i < shown && scroll + i < rows.size(); i++) {
            int index = scroll + i;
            int ry = y + i * ROW_H;
            if ((mouseX >= x && mouseX < x + W && mouseY >= ry && mouseY < ry + ROW_H)
                    || index == cursor) {
                g.fill(x, ry, x + W, ry + ROW_H, HOVER);
            }
            int colour;
            if (index == 0) {
                colour = on.isEmpty() ? DIM : ACTION;
            } else {
                colour = on.contains(options.get(index - 1).key()) ? ACTION : TEXT;
            }
            fitted(g, font, rows.get(index), x + 4, ry + 2, W - 8, colour);
        }
        String typed = typeAhead.typed();
        String foot = !typed.isEmpty() ? typed
                : rows.size() > VISIBLE ? "scroll for " + (rows.size() - VISIBLE) + " more"
                : "type to jump";
        fitted(g, font, foot, x + 4, y + h + 2, W - 8, typed.isEmpty() ? DIM : ACTION);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    /**
     * A click while the menu is open. A gene row toggles and <b>keeps the menu
     * up</b>; the clear row empties the pin and closes, because that gesture is
     * finished; a click off the menu dismisses it.
     *
     * @return true when the pinned set changed, so the tab can reset its scroll
     */
    public boolean click(double mx, double my) {
        if (!open) {
            return false;
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
            return false;
        }
        if (index == 0) {
            boolean had = !pinned().isEmpty();
            ClientConfig.setGeneFilter(List.of());
            close();
            return had;
        }
        toggle(options.get(index - 1));
        cursor = index;
        return true;
    }

    private void toggle(Gene gene) {
        List<String> keys = new ArrayList<>(ClientConfig.geneFilter());
        if (!keys.remove(gene.key())) {
            keys.add(gene.key());
        }
        ClientConfig.setGeneFilter(keys);
    }

    /** Type a few letters and jump to that gene. */
    public boolean charTyped(int codepoint) {
        if (!open) {
            return false;
        }
        int found = typeAhead.accept(codepoint, names(), cursor);
        if (found > 0) {
            cursor = found;
            if (cursor < scroll) {
                scroll = cursor;
            } else if (cursor >= scroll + VISIBLE) {
                scroll = cursor - VISIBLE + 1;
            }
        }
        return true;
    }

    /**
     * Escape closes. Space and Enter toggle whatever the type-ahead landed on,
     * so a gene can be picked without reaching back for the mouse. Arrows walk.
     *
     * @return true when the key was the menu's
     */
    public boolean keyPressed(int key) {
        if (!open) {
            return false;
        }
        if (key == 256) {                                   // Escape
            close();
            return true;
        }
        if (key == 264) {                                   // Down
            move(1);
            return true;
        }
        if (key == 265) {                                   // Up
            move(-1);
            return true;
        }
        if (key == 257 || key == 335 || key == 32) {        // Enter, numpad Enter, Space
            if (cursor > 0 && cursor - 1 < options.size()) {
                toggle(options.get(cursor - 1));
            } else {
                close();
            }
            return true;
        }
        return false;
    }

    private void move(int by) {
        int size = rows().size();
        if (size == 0) {
            return;
        }
        cursor = Math.max(0, Math.min(size - 1, cursor + by));
        if (cursor < scroll) {
            scroll = cursor;
        } else if (cursor >= scroll + VISIBLE) {
            scroll = cursor - VISIBLE + 1;
        }
    }

    /** The wheel scrolls the menu while it is open, and nothing behind it. */
    public boolean scroll(double sy) {
        if (!open) {
            return false;
        }
        int max = Math.max(0, rows().size() - VISIBLE);
        scroll = Math.max(0, Math.min(scroll - (int) sy, max));
        return true;
    }

    /** Squeezed rather than clipped, so a long gene name still reads. */
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
