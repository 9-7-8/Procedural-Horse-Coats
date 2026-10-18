package com.example.horsegenetics.neoforge.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * <b>The query builder, opened from a filter box.</b> Every box that takes a query - My
 * horses, the realm, Breeding preview, the stasis bank, the cowboy's counter - has a Build
 * button beside it that opens this: the Query tab's own {@link QueryBuilderTab}, drawn as
 * a panel under the box, filled from what the box holds, and writing back into the box
 * on every change, so the list behind it filters as the query is built. Done closes it;
 * Save... hands the query to the host's saved-search menu. (Owner, 2026-10-17: "unify the
 * query builder" - one builder in five places, rather than one tab and four boxes.)
 *
 * <p>The host owns the screen and routes input here first while it is open, the way each
 * host already does for its {@link SavedSearchPicker}: an open panel takes every click,
 * key and character, so nothing under it moves.
 */
public final class QueryBuilderPopup {

    private static final int PANEL = 0xF0101018;
    private static final int EDGE = 0xFF3C3C4A;
    private static final int DIM = 0xFF8890A8;
    private static final int MARGIN = 6;

    private final QueryBuilderTab builder = new QueryBuilderTab();
    private @Nullable EditBox target;
    private @Nullable Consumer<String> onSave;
    private boolean open;
    private int x;
    private int y;
    private int w;
    private int h = 90;
    private int screenH;
    /** What the box held when it last heard from the builder - written only on a change. */
    private String written = "";
    /** Why the rows are empty although the box is not, if they are. */
    private String note = "";

    public QueryBuilderPopup() {
        builder.setUseLabel("Done");
    }

    /**
     * Open under {@code box}, filled from its text. {@code onSave} receives the built
     * query when Save... is pressed (the host opens its saved-search menu with it).
     */
    public void open(EditBox box, int screenW, int screenH, Consumer<String> onSave) {
        this.target = box;
        this.onSave = onSave;
        this.open = true;
        this.screenH = screenH;
        String text = box.getValue();
        if (builder.load(text)) {
            note = "";
        } else {
            note = "This filter cannot be shown as rows; building here replaces it.";
        }
        written = text;
        x = MARGIN;
        w = screenW - 2 * MARGIN;
        place(box);
    }

    private void place(EditBox box) {
        int below = box.getY() + box.getHeight() + 2;
        y = below + h <= screenH - 4 ? below : Math.max(4, box.getY() - h - 2);
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        target = null;
    }

    /** The builder inside, for whoever needs to read the query it is making. */
    public QueryBuilderTab builder() {
        return builder;
    }

    public void draw(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        g.fill(x, y, x + w, y + h, PANEL);
        int used = builder.draw(g, font, x + 4, y + 4, w - 8, mouseX, mouseY, -1, -1);
        if (!note.isEmpty()) {
            g.text(font, Component.literal(note), x + 6, y + 4 + used, DIM, false);
            used += 11;
        }
        int want = used + 8;
        if (want != h) {
            // Its height follows the rows; re-placed so a taller panel still fits.
            h = want;
            if (target != null) {
                place(target);
            }
        }
        builder.drawMenu(g, font, mouseX, mouseY);
    }

    /** A press while open: always taken. */
    public boolean click(double mx, double my) {
        if (!open) {
            return false;
        }
        boolean inside = mx >= x && mx < x + w && my >= y && my < y + h;
        if (!inside && !builder.menuOpen()) {
            close();
            return true;
        }
        QueryBuilderTab.Action action = builder.click(mx, my);
        writeThrough();
        if (action == QueryBuilderTab.Action.USE) {
            close();
        } else if (action == QueryBuilderTab.Action.SAVE && onSave != null) {
            String query = builder.query();
            close();
            onSave.accept(query);
        }
        return true;
    }

    public boolean keyPressed(int key) {
        if (!open) {
            return false;
        }
        if (builder.keyPressed(key)) {
            writeThrough();
            return true;
        }
        if (key == 256) { // Escape closes the panel, not the screen under it
            close();
        }
        return true;
    }

    public boolean charTyped(int codepoint) {
        if (!open) {
            return false;
        }
        builder.charTyped(codepoint);
        return true;
    }

    /** The wheel while open: the open menu scrolls, and nothing under the panel does. */
    public boolean scroll(double sy) {
        if (!open) {
            return false;
        }
        builder.scroll(sy);
        return true;
    }

    /** Put the built query in the box, if it changed - the box's own wiring filters the list. */
    private void writeThrough() {
        if (target == null) {
            return;
        }
        String query = builder.query();
        if (!query.equals(written) && !(query.isEmpty() && !note.isEmpty())) {
            written = query;
            target.setValue(query);
        }
    }
}
