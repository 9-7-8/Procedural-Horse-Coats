package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.neoforge.client.CustomHorseSpawnScreen;
import com.example.horsegenetics.neoforge.item.ModItems;
import com.example.horsegenetics.neoforge.server.DebugMenuCrawl;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/**
 * The custom horse spawn egg's editor ({@link CustomHorseSpawnScreen}): every button in the
 * right column, the filter row, a gene row's add / alleles / padlock / remove, the four
 * dropdowns, Copy and Paste, and the three ways out - Make egg and Spawn checked on the
 * server, where they land.
 */
final class SpawnScreenCrawl {

    private static final String W = "CustomHorseSpawnScreen";

    private SpawnScreenCrawl() {
    }

    static void queue() {
        serverDo(W, s -> {
            DebugMenuCrawl.hold(MenuCrawlCases.player(s), DebugMenuCrawl.spawnEgg());
            return null;
        });
        open(CustomHorseSpawnScreen::new);
        sweep(W, List.of("searchBox"),
                List.of("All genes", "Age:", "Sex:", "Breed:", "▾", "Rnd health:", "Clear", "Spawn",
                        "Make egg", "Copy horse", "Paste", "Cancel", "x"),
                SpawnScreenCrawl::rowOrRightColumn);

        int[] baseline = {0};
        then(() -> baseline[0] = added());

        // Age and sex: each press flips the horse, and the label says so.
        toggle("Age:", "baby", "Age: Foal");
        toggle("Sex:", "female", "Sex: Stallion");
        toggle("Rnd health:", "randomizeInvisible", "Rnd health: on");

        // Breed: the dropdown opens, row 1 picks the first breed, row 0 goes back to feral.
        press(W, "Breed:");
        expect(W, "Breed opens the breed list", () -> dd("BREED"));
        pressAt(W, "breed list row 1", () -> ddRow(1));
        expect(W, "a breed row picks that breed and closes the list",
                () -> firstProblem(dd("NONE"), same("breedIndex", 1, field("breedIndex"))));
        press(W, "Breed:");
        pressAt(W, "breed list row 0", () -> ddRow(0));
        expect(W, "row 0 goes back to Feral Mixed", () -> same("breedIndex", 0, field("breedIndex")));

        // Family filter.
        press(W, "All genes");
        expect(W, "the family button opens the family list", () -> dd("FAMILY"));
        pressAt(W, "family list row 1", () -> ddRow(1));
        expect(W, "a family row filters the list to it", () -> field("filter") != null ? null : "filter is still null");
        pressAt(W, "the family button again", () -> familyButton());
        pressAt(W, "family list row 0", () -> ddRow(0));
        expect(W, "row 0 shows every family again", () -> field("filter") == null ? null : "filter is " + field("filter"));

        // Search: the box takes focus, and what is typed filters the rows.
        pressField(W, "searchBox");
        expect(W, "the search box takes focus on a press",
                () -> screen() != null && screen().getFocused() == field("searchBox") ? null : "not focused");
        then(() -> ((EditBox) Objects.requireNonNull(field("searchBox"))).setValue("grey"));
        pause(SETTLE);
        expect(W, "typing in the search box narrows the gene list", () -> {
            int view = ((List<?>) Objects.requireNonNull(field("view"))).size();
            int rows = ((List<?>) Objects.requireNonNull(field("rows"))).size();
            return view > 0 && view < rows ? null : "view " + view + " of " + rows + " rows";
        });
        then(() -> ((EditBox) Objects.requireNonNull(field("searchBox"))).setValue(""));
        pause(SETTLE);

        // Randomize and its mode list.
        String[] before = {""};
        then(() -> before[0] = code());
        press(W, "randomize", () -> labelled(Objects.requireNonNull(screen()),
                (String) call(Objects.requireNonNull(field("randomizeMode")), "shortLabel")));
        expect(W, "Randomize rolls a different horse", () -> code().equals(before[0]) ? "the genotype did not change" : null);
        press(W, "randomize ▾", () -> arrow(0));
        expect(W, "the randomize arrow opens the mode list", () -> dd("RANDOMIZE"));
        pressAt(W, "outside the open list", () -> new double[] {2, 2});
        expect(W, "a press outside a list closes it", () -> dd("NONE"));

        // Add random, and its scope list.
        int[] count = {0};
        then(() -> count[0] = added());
        press(W, "add random", () -> labelled(Objects.requireNonNull(screen()),
                (String) call(Objects.requireNonNull(field("addScope")), "shortLabel")));
        expect(W, "Add random puts one more gene on the horse", () -> same("genes on the horse", count[0] + 1, added()));
        press(W, "add random ▾", () -> arrow(1));
        expect(W, "the add arrow opens the scope list", () -> dd("ADD"));
        pressAt(W, "outside the open list", () -> new double[] {2, 2});

        // Clear takes it back to what it opened on.
        press(W, "Clear");
        expect(W, "Clear takes every unlocked gene off", () -> same("genes on the horse", baseline[0], added()));

        // A gene row: add it, its first allele, its padlock, and its remove button.
        int[] row = {-1};
        then(() -> row[0] = firstUnadded());
        pressAt(W, "an unadded gene row", () -> rowPoint(row[0], 40));
        expect(W, "pressing a gene row adds that gene", () -> Boolean.TRUE.equals(rowField(row[0], "added")) ? null : "not added");
        int[] a = {0};
        then(() -> a[0] = (Integer) rowField(row[0], "a"));
        pressAt(W, "the row's first allele button", () -> alleleButton(row[0]));
        then(() -> {
            if ("ALLELE".equals(name(field("dd")))) {
                // A long allele list opens a dropdown instead of cycling: pick another row.
                double[] p = ddRow(a[0] == 0 ? 1 : 0);
                click(p[0], p[1]);
            }
        });
        pause(SETTLE);
        expect(W, "the allele button changes the first allele",
                () -> Objects.equals(a[0], rowField(row[0], "a")) ? "still " + a[0] : null);
        pressAt(W, "the row's padlock", () -> rowPoint(row[0], 10));
        expect(W, "the padlock locks the row", () -> Boolean.TRUE.equals(rowField(row[0], "locked")) ? null : "not locked");
        pressAt(W, "the padlock again", () -> rowPoint(row[0], 10));
        expect(W, "the padlock again unlocks it", () -> Boolean.FALSE.equals(rowField(row[0], "locked")) ? null : "still locked");
        press(W, "the row's x", () -> removeButton(row[0]));
        expect(W, "x takes the gene off", () -> Boolean.FALSE.equals(rowField(row[0], "added")) ? null : "still added");

        // Copy, then change the horse, then Paste puts it back.
        boolean[] copied = {false};
        then(() -> copied[0] = (Boolean) field("female"));
        press(W, "Copy horse");
        expect(W, "Copy horse puts a horse on the clipboard",
                () -> mc().keyboardHandler.getClipboard().isBlank() ? "the clipboard is empty" : null);
        press(W, "Sex:");
        press(W, "Paste");
        expect(W, "Paste puts the copied horse back (sex included)", () -> same("female", copied[0], field("female")));

        // Make egg: the editor closes and an egg carrying this horse is in the inventory.
        int[] eggs = {0};
        serverDo(W, s -> eggs[0] = presetEggs(s));
        press(W, "Make egg");
        expect(W, "Make egg closes the editor", Crawl::noScreen);
        eventuallyServer(W, "Make egg puts a preset egg in the inventory", 40,
                s -> presetEggs(s) > eggs[0] ? null : "no new preset egg");

        // Spawn: a horse appears beside the player.
        int[] horses = {0};
        serverDo(W, s -> horses[0] = DebugMenuCrawl.horsesNear(MenuCrawlCases.player(s), 12));
        open(CustomHorseSpawnScreen::new);
        press(W, "Spawn");
        expect(W, "Spawn closes the editor", Crawl::noScreen);
        eventuallyServer(W, "Spawn puts a horse in the world", 40,
                s -> DebugMenuCrawl.horsesNear(MenuCrawlCases.player(s), 12) > horses[0] ? null : "no new horse");

        open(CustomHorseSpawnScreen::new);
        press(W, "Cancel");
        expect(W, "Cancel closes the editor", Crawl::noScreen);
        close();
    }

    // ------------------------------------------------------------------ helpers

    private static void toggle(String prefix, String field, String after) {
        Object[] was = {null};
        then(() -> was[0] = field(field));
        press(W, prefix);
        expect(W, "\"" + prefix + "\" flips " + field, () -> {
            Object now = field(field);
            if (Objects.equals(now, was[0])) {
                return field + " is still " + now;
            }
            AbstractWidget w = widget(Objects.requireNonNull(screen()), prefix);
            return w != null && label(w).startsWith(after) ? null
                    : "the label reads \"" + (w == null ? "nothing" : label(w)) + "\", wanted \"" + after + "\"";
        });
        press(W, prefix);
        expect(W, "\"" + prefix + "\" again flips it back", () -> same(field, was[0], field(field)));
    }

    private static @Nullable Object field(String name) {
        return get(Objects.requireNonNull(screen(), "no screen"), name);
    }

    private static @Nullable String dd(String want) {
        return same("the open dropdown", want, name(field("dd")));
    }

    private static @Nullable String firstProblem(@Nullable String a, @Nullable String b) {
        return a != null ? a : b;
    }

    /** The middle of row {@code i} of the open dropdown (12 px rows from ddY). */
    private static double[] ddRow(int i) {
        int x = (Integer) Objects.requireNonNull(field("ddX"));
        int y = (Integer) Objects.requireNonNull(field("ddY"));
        int scroll = (Integer) Objects.requireNonNull(field("ddScroll"));
        return new double[] {x + 20, y + (i - scroll) * 12 + 6};
    }

    private static double @Nullable [] familyButton() {
        Screen s = Objects.requireNonNull(screen());
        for (AbstractWidget w : visibleWidgets(s)) {
            if (w.getY() == 24 && !(w instanceof EditBox)) {
                return new double[] {cx(w), cy(w)};
            }
        }
        return null;
    }

    private static @Nullable AbstractWidget labelled(Screen s, String exact) {
        for (AbstractWidget w : visibleWidgets(s)) {
            if (label(w).equals(exact)) {
                return w;
            }
        }
        return null;
    }

    /** The {@code n}th "▾" arrow down the right column (0 = randomize, 1 = add random). */
    private static @Nullable AbstractWidget arrow(int n) {
        List<AbstractWidget> arrows = new ArrayList<>();
        for (AbstractWidget w : visibleWidgets(Objects.requireNonNull(screen()))) {
            if (label(w).equals("▾")) {
                arrows.add(w);
            }
        }
        arrows.sort((p, q) -> Integer.compare(p.getY(), q.getY()));
        return n < arrows.size() ? arrows.get(n) : null;
    }

    private static boolean rowOrRightColumn(AbstractWidget w) {
        Screen s = screen();
        if (s == null) {
            return false;
        }
        // Randomize and Add random are labelled with their current mode.
        if (label(w).equals(call(get(s, "randomizeMode"), "shortLabel"))
                || label(w).equals(call(get(s, "addScope"), "shortLabel"))) {
            return true;
        }
        int listRight = 8 + (Integer) call(s, "listWidth");
        // A gene row's two allele buttons are labelled with the allele itself.
        return w.getX() < listRight && w.getY() >= 40;
    }

    private static int added() {
        int n = 0;
        for (Object r : (List<?>) Objects.requireNonNull(field("rows"))) {
            if (Boolean.TRUE.equals(get(r, "added"))) {
                n++;
            }
        }
        return n;
    }

    private static String code() {
        return String.valueOf(call(call(Objects.requireNonNull(screen()), "genotype"), "toCode"));
    }

    /** The first visible row in the list that is not on the horse yet. */
    private static int firstUnadded() {
        List<?> view = (List<?>) Objects.requireNonNull(field("view"));
        int scroll = (Integer) Objects.requireNonNull(field("scroll"));
        int rows = (Integer) call(Objects.requireNonNull(screen()), "visibleRows");
        for (int i = scroll; i < Math.min(view.size(), scroll + rows); i++) {
            if (!Boolean.TRUE.equals(get(view.get(i), "added"))) {
                return i;
            }
        }
        return -1;
    }

    private static @Nullable Object rowField(int i, String name) {
        List<?> view = (List<?>) Objects.requireNonNull(field("view"));
        return i < 0 || i >= view.size() ? null : get(view.get(i), name);
    }

    private static double @Nullable [] rowPoint(int i, int x) {
        if (i < 0) {
            return null;
        }
        int scroll = (Integer) Objects.requireNonNull(field("scroll"));
        return new double[] {x, 40 + (i - scroll) * 20 + 9};
    }

    private static double @Nullable [] alleleButton(int i) {
        double[] p = rowPoint(i, 0);
        if (p == null) {
            return null;
        }
        int listW = (Integer) call(Objects.requireNonNull(screen()), "listWidth");
        return new double[] {8 + listW - 96 + 19, p[1]};
    }

    private static @Nullable AbstractWidget removeButton(int i) {
        double[] p = rowPoint(i, 0);
        if (p == null) {
            return null;
        }
        for (AbstractWidget w : visibleWidgets(Objects.requireNonNull(screen()))) {
            if (label(w).equals("x") && w.getY() <= p[1] && p[1] < w.getY() + w.getHeight()) {
                return w;
            }
        }
        return null;
    }

    private static int presetEggs(net.minecraft.client.server.IntegratedServer s) {
        int n = 0;
        var inv = MenuCrawlCases.player(s).getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ModItems.PRESET_HORSE_SPAWN_EGG.get())) {
                n += stack.getCount();
            }
        }
        return n;
    }
}
