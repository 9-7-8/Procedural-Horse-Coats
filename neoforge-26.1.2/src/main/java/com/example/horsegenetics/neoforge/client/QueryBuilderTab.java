package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.breed.Breed;
import com.example.horsegenetics.common.breed.Breeds;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.horse.HorseQuery;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <b>The Query tab: build a search without knowing the language.</b>
 *
 * <p>The filter boxes take a SQL {@code WHERE} clause, which is the right thing
 * for somebody who knows what to type and no help at all to somebody who does
 * not - the vocabulary is twenty columns plus <i>every gene in the
 * registry</i>, and a help line cannot list that. This tab is the other way in:
 * a row of dropdowns per clause, the query written out as you build it, and a
 * live count of what it would match. (Owner, 2026-09-26.)
 *
 * <p>It exists <b>here and nowhere else</b> on purpose. The stasis bank and the
 * cowboy's counter are vanilla-sized panels with no room for it, so those get
 * the other two ways in - type it, or pick one you saved here. That is the whole
 * shape of the feature: build in the roomy screen, use anywhere.
 *
 * <h2>Why it writes text rather than holding a tree</h2>
 * The builder's output is a <b>string in the same language a player could have
 * typed</b>, which means there is one query language and not two. A structured
 * representation that only this tab could produce would be a second dialect the
 * saved list, the other screens and {@code HorseQuery} would all have to learn -
 * and a query the player could look at, understand and then not be able to edit
 * by hand. Building and typing produce the same artefact.
 *
 * <p><b>Not verified in-game.</b>
 */
public final class QueryBuilderTab {

    // Row geometry. One clause is one line.
    private static final int ROW_H = 20;
    private static final int JOIN_W = 42;
    private static final int NOT_W = 34;
    private static final int COL_W = 132;
    private static final int OP_W = 44;
    private static final int VAL_W = 150;
    private static final int KILL_W = 14;
    private static final int PAD = 4;

    private static final int WELL = 0xFF0B0B10;
    private static final int EDGE = 0xFF3C3C4A;
    private static final int HOVER = 0xFF2E3542;
    private static final int TEXT = 0xFFE4E8F0;
    private static final int DIM = 0xFF8890A8;
    private static final int GOOD = 0xFF9BE08A;
    private static final int ACCENT = 0xFF55A0E0;

    private static final List<String> OPS =
            List.of("=", "!=", ">", ">=", "<", "<=", "LIKE");

    /** One line of the query. {@code joiner} is ignored on the first row. */
    private static final class Clause {
        String joiner = "AND";
        boolean not;
        String column = "";
        String op = "=";
        String value = "";
    }

    private final List<Clause> clauses = new ArrayList<>();

    /** What the builder produced last time it was asked - the Use/Save buttons read it. */
    private String built = "";

    // --- the open dropdown, if any ---

    private enum Menu { NONE, JOIN, COLUMN, OP, VALUE }

    private Menu menu = Menu.NONE;
    private int menuRow = -1;
    private int menuX;
    private int menuY;
    private int menuScroll;

    /**
     * Type-ahead over the open menu: letters jump to the entry that starts with
     * them rather than filtering, and the lists are alphabetical so that jumping
     * lands somewhere predictable. See {@link TypeAhead}.
     */
    private final TypeAhead typeAhead = new TypeAhead();

    /** Which entry the type-ahead last landed on, so it is drawn as chosen. */
    private int menuCursor;

    private static final int MENU_ROW_H = 12;
    private static final int MENU_VISIBLE = 12;
    private static final int MENU_W = 190;

    /** Where each clickable thing landed this frame, so clicks and drawing agree. */
    private record Hit(int row, Menu what, int x, int y, int w) {
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<Hit> actions = new ArrayList<>();

    public QueryBuilderTab() {
        clauses.add(new Clause());
    }

    /** The query as it stands, in the language the filter boxes take. */
    public String query() {
        StringBuilder out = new StringBuilder();
        for (Clause clause : clauses) {
            String piece = piece(clause);
            if (piece.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ').append(clause.joiner).append(' ');
            }
            out.append(piece);
        }
        built = out.toString();
        return built;
    }

    /**
     * One clause as text. A column with no value is a <b>bare word</b> rather
     * than a broken comparison - which is exactly right for the flags, where
     * "mare" is the whole predicate and there is nothing to compare it to.
     */
    private static String piece(Clause clause) {
        if (clause.column.isEmpty()) {
            return "";
        }
        String body;
        if (clause.value.isEmpty()) {
            body = quoteIfNeeded(clause.column);
        } else {
            body = quoteIfNeeded(clause.column) + " " + clause.op + " "
                    + quoteIfNeeded(clause.value);
        }
        return clause.not ? "NOT " + body : body;
    }

    /** Anything with a space or a keyword in it has to survive the tokenizer. */
    private static String quoteIfNeeded(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        boolean keyword = lower.equals("and") || lower.equals("or") || lower.equals("not")
                || lower.equals("in") || lower.equals("like");
        if (word.indexOf(' ') >= 0 || keyword) {
            return "'" + word + "'";
        }
        return word;
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    /**
     * @return the height used, so the caller can put its own footer under it
     */
    public int draw(GuiGraphicsExtractor g, Font font, int left, int top, int width,
                    int mouseX, int mouseY, int matched, int total) {
        hits.clear();
        actions.clear();
        int y = top;

        for (int i = 0; i < clauses.size(); i++) {
            drawRow(g, font, clauses.get(i), i, left, y, mouseX, mouseY);
            y += ROW_H;
        }

        y += 2;
        int x = left;
        x += drawAction(g, font, "+ clause", -1, x, y, mouseX, mouseY, GOOD) + PAD;
        x += drawAction(g, font, "Clear", -2, x, y, mouseX, mouseY, DIM) + PAD;
        y += ROW_H;

        // The query itself, which is the thing being built and the thing a
        // player learns the language from.
        String text = query();
        g.fill(left, y, left + width, y + ROW_H - 2, WELL);
        fitted(g, font, text.isEmpty() ? "nothing yet - pick a column above" : text,
                left + 4, y + 5, width - 8, text.isEmpty() ? DIM : ACCENT);
        y += ROW_H;

        fitted(g, font, total < 0 ? "" : "matches " + matched + " of " + total,
                left + 4, y + 2, width - 8, DIM);
        y += 12;

        x = left;
        x += drawAction(g, font, "Use it", -3, x, y, mouseX, mouseY,
                text.isEmpty() ? DIM : GOOD) + PAD;
        x += drawAction(g, font, "Save...", -4, x, y, mouseX, mouseY,
                text.isEmpty() ? DIM : TEXT) + PAD;
        y += ROW_H;

        return y - top;
    }

    private void drawRow(GuiGraphicsExtractor g, Font font, Clause clause, int index,
                         int left, int y, int mouseX, int mouseY) {
        int x = left;
        if (index > 0) {
            cell(g, font, clause.joiner, index, Menu.JOIN, x, y, JOIN_W, mouseX, mouseY, ACCENT);
        }
        x += JOIN_W + PAD;
        cell(g, font, clause.not ? "NOT" : "-", index, Menu.NONE, x, y, NOT_W, mouseX, mouseY,
                clause.not ? GOOD : DIM);
        x += NOT_W + PAD;
        cell(g, font, clause.column.isEmpty() ? "column..." : clause.column,
                index, Menu.COLUMN, x, y, COL_W, mouseX, mouseY,
                clause.column.isEmpty() ? DIM : TEXT);
        x += COL_W + PAD;
        // No operator on a flag - "mare = something" is not a thing to offer.
        boolean wantsValue = !clause.column.isEmpty() && !isFlag(clause.column);
        if (wantsValue) {
            cell(g, font, clause.op, index, Menu.OP, x, y, OP_W, mouseX, mouseY, TEXT);
            x += OP_W + PAD;
            cell(g, font, clause.value.isEmpty() ? "value..." : clause.value,
                    index, Menu.VALUE, x, y, VAL_W, mouseX, mouseY,
                    clause.value.isEmpty() ? DIM : TEXT);
            x += VAL_W + PAD;
        } else {
            x += OP_W + PAD + VAL_W + PAD;
        }
        cell(g, font, "x", index, Menu.NONE, x, y, KILL_W, mouseX, mouseY, DIM);
        // The two Menu.NONE cells above are the NOT toggle and the delete; they
        // are told apart by their x when the click comes back. See click().
    }

    private void cell(GuiGraphicsExtractor g, Font font, String label, int row, Menu what,
                      int x, int y, int w, int mouseX, int mouseY, int colour) {
        boolean over = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_H - 2;
        g.fill(x, y, x + w, y + ROW_H - 2, over ? HOVER : WELL);
        g.fill(x, y, x + w, y + 1, EDGE);
        g.fill(x, y + ROW_H - 3, x + w, y + ROW_H - 2, EDGE);
        fitted(g, font, label, x + 3, y + 5, w - 6, colour);
        hits.add(new Hit(row, what, x, y, w));
    }

    private int drawAction(GuiGraphicsExtractor g, Font font, String label, int id,
                           int x, int y, int mouseX, int mouseY, int colour) {
        int w = font.width(label) + 12;
        boolean over = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_H - 2;
        g.fill(x, y, x + w, y + ROW_H - 2, over ? HOVER : WELL);
        fitted(g, font, label, x + 6, y + 5, w - 8, colour);
        actions.add(new Hit(id, Menu.NONE, x, y, w));
        return w;
    }

    /** The open dropdown, drawn last by the caller so it covers the rows. */
    public void drawMenu(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        if (menu == Menu.NONE) {
            return;
        }
        List<String> options = options();
        int shown = Math.min(MENU_VISIBLE, Math.max(1, options.size()));
        int h = shown * MENU_ROW_H + MENU_ROW_H;
        g.fill(menuX - 1, menuY - 1, menuX + MENU_W + 1, menuY + h + 1, EDGE);
        g.fill(menuX, menuY, menuX + MENU_W, menuY + h, WELL);

        String typed = typeAhead.typed();
        fitted(g, font, typed.isEmpty() ? "type to jump" : typed,
                menuX + 4, menuY + 2, MENU_W - 8, typed.isEmpty() ? DIM : ACCENT);

        for (int i = 0; i < shown && menuScroll + i < options.size(); i++) {
            int index = menuScroll + i;
            int ry = menuY + MENU_ROW_H + i * MENU_ROW_H;
            boolean over = mouseX >= menuX && mouseX < menuX + MENU_W
                    && mouseY >= ry && mouseY < ry + MENU_ROW_H;
            if (over || index == menuCursor) {
                g.fill(menuX, ry, menuX + MENU_W, ry + MENU_ROW_H, HOVER);
            }
            fitted(g, font, options.get(index), menuX + 4, ry + 2, MENU_W - 8,
                    index == menuCursor ? ACCENT : TEXT);
        }
        if (options.size() > MENU_VISIBLE) {
            fitted(g, font, (options.size() - MENU_VISIBLE) + " more - scroll, or type",
                    menuX + 4, menuY + h + 2, MENU_W - 8, DIM);
        }
    }

    public boolean menuOpen() {
        return menu != Menu.NONE;
    }

    // ------------------------------------------------------------------
    // The option lists
    // ------------------------------------------------------------------

    private List<String> options() {
        switch (menu) {
            case JOIN:
                return List.of("AND", "OR");
            case OP:
                return OPS;
            case COLUMN: {
                List<String> out = new ArrayList<>(HorseQuery.keys());
                out.addAll(HorseQuery.flags());
                for (Gene gene : Genes.codeOrder()) {
                    out.add(gene.name());
                }
                // Alphabetical, because the type-ahead below is only navigable
                // if the entries it walks past are in an order you can predict.
                return TypeAhead.sorted(out);
            }
            case VALUE:
                return valuesFor(menuRow < 0 ? "" : clauses.get(menuRow).column);
            default:
                return List.of();
        }
    }

    /**
     * What is worth offering for this column. The point of the tab: a player who
     * does not know that {@code hom} is a word should be shown it, and a player
     * who does not know their own breeds should not have to remember them.
     */
    private static List<String> valuesFor(String column) {
        List<String> out = new ArrayList<>();
        Gene gene = geneNamed(column);
        if (gene != null) {
            out.addAll(HorseQuery.zygosities());
            for (Allele allele : gene.alleles()) {
                out.add(allele.token());
            }
            return out;
        }
        String lower = column.toLowerCase(Locale.ROOT);
        if (lower.equals("breed")) {
            for (Breed breed : Breeds.all()) {
                out.add(breed.name());
            }
            return TypeAhead.sorted(out);
        }
        if (lower.equals("sex")) {
            return List.of("mare", "stallion", "gelding", "filly", "colt");
        }
        if (lower.equals("age")) {
            return List.of("foal", "adult");
        }
        // Numbers and free text: nothing to list, so the filter line is the
        // whole of it - type the value and press Enter.
        return out;
    }

    private static Gene geneNamed(String word) {
        if (word.isEmpty()) {
            return null;
        }
        for (Gene gene : Genes.codeOrder()) {
            if (gene.name().equalsIgnoreCase(word)) {
                return gene;
            }
        }
        return null;
    }

    private static boolean isFlag(String column) {
        for (String flag : HorseQuery.flags()) {
            if (flag.equalsIgnoreCase(column)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    /** What a click asked the screen to do, beyond editing the rows. */
    public enum Action { NONE, USE, SAVE }

    public Action click(double mx, double my) {
        if (menu != Menu.NONE) {
            pickFromMenu(mx, my);
            return Action.NONE;
        }
        for (Hit hit : actions) {
            if (mx < hit.x() || mx >= hit.x() + hit.w()
                    || my < hit.y() || my >= hit.y() + ROW_H - 2) {
                continue;
            }
            switch (hit.row()) {
                case -1 -> clauses.add(new Clause());
                case -2 -> {
                    clauses.clear();
                    clauses.add(new Clause());
                }
                case -3 -> {
                    return query().isEmpty() ? Action.NONE : Action.USE;
                }
                case -4 -> {
                    return query().isEmpty() ? Action.NONE : Action.SAVE;
                }
                default -> { }
            }
            return Action.NONE;
        }
        for (Hit hit : hits) {
            if (mx < hit.x() || mx >= hit.x() + hit.w()
                    || my < hit.y() || my >= hit.y() + ROW_H - 2) {
                continue;
            }
            if (hit.what() == Menu.NONE) {
                // The NOT toggle and the delete, told apart by width.
                Clause clause = clauses.get(hit.row());
                if (hit.w() == NOT_W) {
                    clause.not = !clause.not;
                } else if (clauses.size() > 1) {
                    clauses.remove(hit.row());
                } else {
                    clauses.set(0, new Clause());
                }
                return Action.NONE;
            }
            openMenu(hit.what(), hit.row(), hit.x(), hit.y() + ROW_H);
            return Action.NONE;
        }
        return Action.NONE;
    }

    private void openMenu(Menu what, int row, int x, int y) {
        menu = what;
        menuRow = row;
        menuX = x;
        menuY = y;
        menuScroll = 0;
        menuCursor = 0;
        typeAhead.reset();
    }

    private void closeMenu() {
        menu = Menu.NONE;
        menuRow = -1;
        typeAhead.reset();
    }

    private void pickFromMenu(double mx, double my) {
        List<String> options = options();
        int shown = Math.min(MENU_VISIBLE, Math.max(1, options.size()));
        int index = -1;
        if (mx >= menuX && mx < menuX + MENU_W
                && my >= menuY + MENU_ROW_H && my < menuY + MENU_ROW_H + shown * MENU_ROW_H) {
            int hit = menuScroll + (int) ((my - menuY - MENU_ROW_H) / MENU_ROW_H);
            if (hit >= 0 && hit < options.size()) {
                index = hit;
            }
        }
        if (index >= 0) {
            apply(options.get(index));
        }
        closeMenu();
    }

    private void apply(String chosen) {
        if (menuRow < 0 || menuRow >= clauses.size()) {
            return;
        }
        Clause clause = clauses.get(menuRow);
        switch (menu) {
            case JOIN -> clause.joiner = chosen;
            case OP -> clause.op = chosen;
            case VALUE -> clause.value = chosen;
            case COLUMN -> {
                clause.column = chosen;
                clause.value = "";      // a value for the old column means nothing now
            }
            default -> { }
        }
    }

    /**
     * Typing while a dropdown is open narrows it - and, for a column with no list
     * to offer (a number, a name), <b>Enter takes what was typed</b>. That is the
     * one place the builder has to accept free text, and it is the place a list
     * could never have helped.
     */
    public boolean charTyped(int codepoint) {
        if (menu == Menu.NONE) {
            return false;
        }
        List<String> options = options();
        int found = typeAhead.accept(codepoint, options, menuCursor);
        if (found >= 0) {
            menuCursor = found;
            // Put it in view without snapping it to the top - a list that
            // scrolls the target to the first row loses the context either side
            // of it, which is half of what you are reading an alphabetical list
            // for.
            if (found < menuScroll) {
                menuScroll = found;
            } else if (found >= menuScroll + MENU_VISIBLE) {
                menuScroll = found - MENU_VISIBLE + 1;
            }
            menuScroll = Math.max(0, Math.min(menuScroll,
                    Math.max(0, options.size() - MENU_VISIBLE)));
        }
        return true;
    }

    public boolean keyPressed(int key) {
        if (menu == Menu.NONE) {
            return false;
        }
        if (key == 256) {                       // Escape
            closeMenu();
            return true;
        }
        if (key == 259) {                       // Backspace
            typeAhead.reset();
            return true;
        }
        if (key == 264) {                       // Down
            step(1);
            return true;
        }
        if (key == 265) {                       // Up
            step(-1);
            return true;
        }
        if (key == 257 || key == 335) {         // Enter
            List<String> options = options();
            if (menuCursor >= 0 && menuCursor < options.size()) {
                apply(options.get(menuCursor));
            }
            closeMenu();
            return true;
        }
        return true;
    }

    /** Arrow keys walk the list, because a jump you overshot wants one step back. */
    private void step(int by) {
        List<String> options = options();
        if (options.isEmpty()) {
            return;
        }
        menuCursor = Math.max(0, Math.min(options.size() - 1, menuCursor + by));
        if (menuCursor < menuScroll) {
            menuScroll = menuCursor;
        } else if (menuCursor >= menuScroll + MENU_VISIBLE) {
            menuScroll = menuCursor - MENU_VISIBLE + 1;
        }
    }

    public boolean scroll(double sy) {
        if (menu == Menu.NONE) {
            return false;
        }
        int max = Math.max(0, options().size() - MENU_VISIBLE);
        menuScroll = Math.max(0, Math.min(menuScroll - (int) sy, max));
        return true;
    }

    private static void fitted(GuiGraphicsExtractor g, Font font, String text,
                               int x, int y, int maxW, int colour) {
        float w = font.width(text);
        if (w <= maxW || w <= 0) {
            g.text(font, Component.literal(text), x, y, colour, false);
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(maxW / w);
        g.text(font, Component.literal(text), 0, 0, colour, false);
        pose.popMatrix();
    }
}
