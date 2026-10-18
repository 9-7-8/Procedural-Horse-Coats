package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.common.horse.HorseListing;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.client.ClientHorseLog;
import com.example.horsegenetics.neoforge.client.ClientHorseRoster;
import com.example.horsegenetics.neoforge.client.HorseBrowserScreen;
import com.example.horsegenetics.neoforge.server.DebugMenuCrawl;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/**
 * The horse browser ({@link HorseBrowserScreen}, the H key): every tab of the strip and, on
 * each, its widgets and its drawn regions - contents rows, list rows, column headings, a
 * table row and its Send home, the gene and allele pickers, the saved-search menu, the Query
 * builder's cells and actions, the breeding pickers, the Log and Alleles chips, a breed row.
 *
 * <p>Almost all of the browser's state is static and outlives the screen, so the case
 * starts it on Getting started by hand and reads each effect off those statics.
 */
final class BrowserCrawl {

    private static final String W = "HorseBrowserScreen";

    private static final List<String> ROSTER = List.of("horseFilterBox", "buildButton", "genePickButton",
            "allelePickButton", "savedSearchButton", "refreshRosterButton");

    private BrowserCrawl() {
    }

    static void queue() {
        then(() -> {
            set(HorseBrowserScreen.class, "tab", tab("GETTING_STARTED"));
            set(HorseBrowserScreen.class, "tutorialSection", 0);
            set(HorseBrowserScreen.class, "horseFilter", "");
            set(HorseBrowserScreen.class, "breedScroll", 0);
            set(HorseBrowserScreen.class, "listScroll", 0);
        });
        open(HorseBrowserScreen::new);

        gettingStarted();
        geneDatabase();
        recipes();
        myHorses();
        query();
        breeding();
        log();
        alleles();
        breeds();
        then(() -> {
            if (!com.example.horsegenetics.neoforge.client.ClientRealmWatch.inRealm()) {
                skip(W, "the Horse realm tab - only there inside the realm, and the crawl stays in the overworld");
            }
        });
        then(() -> Objects.requireNonNull(screen()).keyPressed(
                new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, 0, 0)));
        pause(SETTLE);
        expect(W, "Escape closes the browser", Crawl::noScreen);
    }

    // ------------------------------------------------------------------ tabs

    private static void gettingStarted() {
        sweep(W + " Getting started", List.of(), List.of());
        pressAt(W, "contents row 2", () -> {
            List<?> toc = (List<?>) field("tocEntries");
            for (Object e : Objects.requireNonNull(toc)) {
                if ((Integer) call(e, "index") == 1) {
                    return centreOfBox(e);
                }
            }
            return null;
        });
        expect(W, "a contents row opens that section", () -> same("tutorialSection", 1, field("tutorialSection")));
    }

    private static void geneDatabase() {
        pressTab("GENE_DATABASE");
        sweep(W + " Gene database", List.of("searchBox"), List.of());
        pressField(W, "searchBox");
        expect(W, "the search box takes focus on a press", () -> focused("searchBox"));
        int[] all = {0};
        then(() -> all[0] = ((List<?>) Objects.requireNonNull(field("filtered"))).size());
        then(() -> box("searchBox").setValue("grey"));
        pause(SETTLE);
        expect(W, "the search box narrows the gene list", () -> {
            int n = ((List<?>) Objects.requireNonNull(field("filtered"))).size();
            return n > 0 && n < all[0] ? null : n + " of " + all[0] + " genes shown";
        });
        then(() -> box("searchBox").setValue(""));
        // The box is read into the list once a frame: wait for the whole list again, or the
        // row pressed is a row of the filtered one.
        eventually(W, "clearing the search box brings every gene back", 40,
                () -> same("genes shown", all[0], ((List<?>) Objects.requireNonNull(field("filtered"))).size()));
        pressAt(W, "gene list row 3", () -> listRow(2));
        expect(W, "a gene row selects that gene", () -> {
            Object gene = ((List<?>) Objects.requireNonNull(field("filtered"))).get(2);
            return same("selectedKey", call(gene, "key"), field("selectedKey"));
        });
    }

    private static void recipes() {
        pressTab("RECIPES");
        sweep(W + " Recipes", List.of("searchBox", "recipeCategoryButton"), List.of());
        pressField(W, "recipeCategoryButton");
        expect(W, "the category button opens the category menu", () -> same("recipeMenuOpen", true, field("recipeMenuOpen")));
        pressAt(W, "the Gene carrots row", () -> new double[] {listX() + 10, contentTop() + 33 + 14 + 7});
        expect(W, "a category row switches the list and closes the menu", () -> {
            String p = same("recipeCategory", "GENE_CARROTS", name(field("recipeCategory")));
            return p != null ? p : same("recipeMenuOpen", false, field("recipeMenuOpen"));
        });
        pressAt(W, "recipe row 2", () -> listRow(1));
        expect(W, "a recipe row selects that recipe", () -> same("selectedRecipe", 1, field("selectedRecipe")));
        pressField(W, "recipeCategoryButton");
        pressAt(W, "the Everything else row", () -> new double[] {listX() + 10, contentTop() + 33 + 7});
        expect(W, "the first category row switches back", () -> same("recipeCategory", "OTHER", name(field("recipeCategory"))));
    }

    private static void myHorses() {
        pressTab("MY_HORSES");
        eventually(W, "the roster arrives", 80, () -> ClientHorseRoster.received() ? null : "not yet");
        pause(SETTLE);
        sweep(W + " My horses", ROSTER, List.of());

        // Column headings sort, and the same heading again turns it round.
        pressAt(W, "the Speed heading", () -> scanHit("columnAt", c -> "SPEED".equals(name(c))));
        expect(W, "a heading sorts by that column", () -> same("sort", "SPEED", name(field("sort"))));
        boolean[] desc = {false};
        then(() -> desc[0] = (Boolean) field("sortDescending"));
        pressAt(W, "the Speed heading again", () -> scanHit("columnAt", c -> "SPEED".equals(name(c))));
        expect(W, "the same heading again reverses it", () -> same("sortDescending", !desc[0], field("sortDescending")));

        // A row selects (in its name column - its middle is the Send home button).
        then(() -> set(HorseBrowserScreen.class, "selectedHorseId", null));
        pressAt(W, "the mare's row", () -> scrolledTo(() -> {
            Screen s = Objects.requireNonNull(screen());
            int[] b = scanBox(s, hitTest(s, "horseRowAt"), l -> DebugMenuCrawl.mare.equals(((HorseListing) l).id()));
            return b == null ? null : new double[] {b[0] + 12, (b[1] + b[3]) / 2.0};
        }, () -> new double[] {Objects.requireNonNull(screen()).width / 2.0, Objects.requireNonNull(screen()).height * 0.6}));
        expect(W, "a row selects that horse", () -> same("selectedHorseId", DebugMenuCrawl.mare, field("selectedHorseId")));
        pressAt(W, "the mare's Send home", () -> scanHit("horseSendAt", l -> DebugMenuCrawl.mare.equals(((HorseListing) l).id())));
        expect(W, "Send home takes its press (the row under it is not toggled)",
                () -> same("selectedHorseId", DebugMenuCrawl.mare, field("selectedHorseId")));
        then(() -> skip(W, "Send home's recall itself - it needs a stall to send her to, which the crawl's yard has not"));

        // The filter box filters the table.
        pressField(W, "horseFilterBox");
        expect(W, "the filter box takes focus on a press", () -> focused("horseFilterBox"));
        then(() -> box("horseFilterBox").setValue("mare"));
        pause(SETTLE);
        expect(W, "\"mare\" in the filter box leaves only mares", () -> {
            List<?> rows = (List<?>) Objects.requireNonNull(field("horseRows"));
            for (Object r : rows) {
                if (((HorseListing) r).sex() != Sex.FEMALE) {
                    return "a " + ((HorseListing) r).sex() + " is still listed";
                }
            }
            return rows.isEmpty() ? "no rows at all" : null;
        });

        // Saved...: opens the menu; on an unsaved filter its first row asks for a name.
        pressField(W, "savedSearchButton");
        expect(W, "Saved... opens the saved-search menu", () -> savedOpen() ? null : "not open");
        pressAt(W, "the menu's first row (Save this search...)", () -> savedRow(0));
        expect(W, "the first row on an unsaved filter asks for a name",
                () -> (Boolean) call(Objects.requireNonNull(field("savedSearches")), "isNaming") ? null : "not naming");
        pressAt(W, "outside the menu", () -> new double[] {4, Objects.requireNonNull(screen()).height - 4});
        expect(W, "a press outside closes the menu", () -> savedOpen() ? "still open" : null);
        then(() -> box("horseFilterBox").setValue(""));
        pause(SETTLE);

        // Gene... and Allele...: the dropdowns.
        pressField(W, "genePickButton");
        expect(W, "Gene... opens the gene list", () -> same("geneDd", "GENE", name(field("geneDd"))));
        pressAt(W, "gene list row 2", () -> geneDdRow(1));
        expect(W, "a gene row picks that gene and names the button", () -> {
            Object picked = field("pickedGene");
            if (picked == null) {
                return "no gene picked";
            }
            AbstractWidget b = (AbstractWidget) field("genePickButton");
            return label(b).equals(String.valueOf(call(picked, "name"))) ? null : "the button reads " + label(b);
        });
        pressField(W, "genePickButton");
        pressAt(W, "gene list row 1 (Any gene)", () -> geneDdRow(0));
        expect(W, "Any gene clears the pick", () -> field("pickedGene") == null ? null : "still " + field("pickedGene"));
        pressField(W, "allelePickButton");
        expect(W, "Allele... opens the allele list", () -> same("geneDd", "ALLELE", name(field("geneDd"))));
        pressAt(W, "allele list row 2", () -> geneDdRow(1));
        expect(W, "an allele row puts a gene: term in the filter",
                () -> String.valueOf(field("horseFilter")).contains("gene:") ? null : "filter is \"" + field("horseFilter") + "\"");
        then(() -> box("horseFilterBox").setValue(""));
        pause(SETTLE);

        int[] version = {0};
        then(() -> version[0] = ClientHorseRoster.version());
        pause(25); // one answer per question per second (ModNetworking.mayAnswer)
        pressField(W, "refreshRosterButton");
        eventually(W, "Refresh asks for the roster again and it arrives", 80,
                () -> ClientHorseRoster.version() > version[0] ? null : "same version");
    }

    private static void query() {
        pressTab("QUERY");
        sweep(W + " Query", List.of(), List.of());
        int[] n = {0};
        then(() -> n[0] = clauses());
        pressAt(W, "+ clause", () -> action(-1));
        expect(W, "+ clause adds a clause", () -> same("clauses", n[0] + 1, clauses()));
        pressAt(W, "the second clause's x", () -> cell(1, "NONE", 14));
        expect(W, "x removes its clause", () -> same("clauses", n[0], clauses()));
        pressAt(W, "the first clause's NOT", () -> cell(0, "NONE", 34));
        expect(W, "NOT negates the clause", () -> same("not", true, get(firstClause(), "not")));
        pressAt(W, "the first clause's NOT again", () -> cell(0, "NONE", 34));
        pressAt(W, "the first clause's column cell", () -> cell(0, "COLUMN", -1));
        expect(W, "the column cell opens the column menu", () -> builderMenuOpen() ? null : "no menu");
        pressAt(W, "the column menu's first option", () -> {
            Object b = builder();
            return new double[] {(Integer) get(b, "menuX") + 30, (Integer) get(b, "menuY") + 12 + 6};
        });
        expect(W, "an option fills the clause and closes the menu", () -> {
            if (builderMenuOpen()) {
                return "the menu is still open";
            }
            return String.valueOf(call(builder(), "query")).isEmpty() ? "the query is still empty" : null;
        });
        pressAt(W, "Save...", () -> action(-4));
        expect(W, "Save... opens the saved-search menu", () -> savedOpen() ? null : "not open");
        pressAt(W, "outside the menu", () -> new double[] {4, Objects.requireNonNull(screen()).height - 4});
        String[] built = {""};
        then(() -> built[0] = String.valueOf(call(builder(), "query")));
        pressAt(W, "Use it", () -> action(-3));
        expect(W, "Use it puts the query in My horses' filter and goes there", () -> {
            String p = same("tab", "MY_HORSES", name(field("tab")));
            return p != null ? p : same("horseFilter", built[0], field("horseFilter"));
        });
        then(() -> box("horseFilterBox").setValue(""));
        pressTab("QUERY");
        pressAt(W, "Clear", () -> action(-2));
        expect(W, "Clear leaves one empty clause",
                () -> clauses() == 1 && String.valueOf(call(builder(), "query")).isEmpty() ? null
                        : clauses() + " clauses, query \"" + call(builder(), "query") + "\"");
    }

    private static void breeding() {
        pressTab("BREEDING_PREVIEW");
        pause(SETTLE);
        sweep(W + " Breeding preview", List.of("breedingFilterBox", "buildButton", "savedSearchButton",
                "refreshRosterButton", "settledToggle"), List.of());
        boolean[] settled = {false};
        then(() -> settled[0] = (Boolean) field("showSettled"));
        pressField(W, "settledToggle");
        expect(W, "the loci toggle flips which loci show", () -> same("showSettled", !settled[0], field("showSettled")));
        pressField(W, "settledToggle");

        String[] sort = {""};
        then(() -> sort[0] = name(field("pickerSort")));
        pressAt(W, "the Mares heading", () -> new double[] {listX() + 20, (Integer) call(screen(), "listTop") + 6});
        expect(W, "a picker heading steps the sort", () -> name(field("pickerSort")).equals(sort[0]) ? "still " + sort[0] : null);

        then(() -> {
            set(HorseBrowserScreen.class, "damId", null);
            set(HorseBrowserScreen.class, "sireId", null);
        });
        pressAt(W, "the mare in the mares list", () -> scrolledTo(
                () -> scanHit("horseAt", l -> DebugMenuCrawl.mare.equals(((HorseListing) l).id()), Sex.FEMALE),
                () -> new double[] {listX() + 20, (Integer) call(screen(), "listTop") + 30}));
        expect(W, "a mare row picks the dam", () -> same("damId", DebugMenuCrawl.mare, field("damId")));
        pressAt(W, "the stallion in the stallions list", () -> scrolledTo(
                () -> scanHit("horseAt", l -> DebugMenuCrawl.stallion.equals(((HorseListing) l).id()), Sex.MALE),
                () -> new double[] {listX() + 20, (Integer) call(screen(), "pickerSplit") + 30}));
        expect(W, "a stallion row picks the sire", () -> same("sireId", DebugMenuCrawl.stallion, field("sireId")));
        expect(W, "with both picked the preview is built",
                () -> ((List<?>) Objects.requireNonNull(field("preview"))).isEmpty() ? "no preview" : null);

        pressField(W, "breedingFilterBox");
        expect(W, "the breeding filter box takes focus on a press", () -> focused("breedingFilterBox"));
        pressField(W, "savedSearchButton");
        expect(W, "Saved... opens the saved-search menu here too", () -> savedOpen() ? null : "not open");
        pressAt(W, "outside the menu", () -> new double[] {4, Objects.requireNonNull(screen()).height - 4});
        int[] version = {0};
        then(() -> version[0] = ClientHorseRoster.version());
        pause(25); // one answer per question per second (ModNetworking.mayAnswer)
        pressField(W, "refreshRosterButton");
        eventually(W, "Refresh asks for the roster again (breeding)", 80,
                () -> ClientHorseRoster.version() > version[0] ? null : "same version");
    }

    private static void log() {
        pressTab("LOG");
        eventually(W, "the log arrives", 80, () -> ClientHorseLog.received() ? null : "not yet");
        pause(SETTLE);
        sweep(W + " Log", List.of("refreshRosterButton"), List.of());
        pressAt(W, "the first kind's chip", () -> chip("logChips", 0));
        expect(W, "a kind chip filters the log to that kind",
                () -> field("logFilter") == null ? "still showing all" : null);
        pressAt(W, "the All chip", () -> chip("logChips", -1));
        expect(W, "All shows every kind again", () -> field("logFilter") == null ? null : "still " + field("logFilter"));
        int[] version = {0};
        then(() -> version[0] = ClientHorseLog.version());
        pause(25); // one answer per question per second (ModNetworking.mayAnswer)
        pressField(W, "refreshRosterButton");
        eventually(W, "Refresh asks for the log again and it arrives", 80,
                () -> ClientHorseLog.version() > version[0] ? null : "same version");
    }

    private static void alleles() {
        pressTab("ALLELES");
        pause(SETTLE);
        sweep(W + " Alleles", List.of("searchBox"), List.of());
        pressAt(W, "the second filter chip", () -> chip("alleleChips", 1));
        expect(W, "a filter chip filters the alleles", () -> "ALL".equals(name(field("alleleFilter"))) ? "still ALL" : null);
        pressAt(W, "the All chip", () -> chip("alleleChips", 0));
        expect(W, "All shows every allele again", () -> same("alleleFilter", "ALL", name(field("alleleFilter"))));
    }

    private static void breeds() {
        pressTab("BREEDS");
        pause(SETTLE);
        sweep(W + " Breeds", List.of("searchBox", "openBreedsFolderButton"), List.of());
        pressAt(W, "breed row 2", () -> new double[] {listX() + 10,
                (Integer) call(screen(), "listTop") + (mc().font.lineHeight + 5) * 1.5});
        expect(W, "a met breed's row opens that breed", () -> {
            Object b = ((List<?>) Objects.requireNonNull(field("breedRows"))).get(1);
            return same("selectedBreed", call(b, "id"), field("selectedBreed"));
        });
        then(() -> skip(W, "Open breeds folder - it opens a window on the desktop, outside the game"));
    }

    // ------------------------------------------------------------------ helpers

    private static Object tab(String name) {
        return enumConstant(HorseBrowserScreen.class, "Tab", name);
    }

    /**
     * Press a tab, scrolling the strip to it first if the window is too narrow to show
     * them all (it scrolls; a tab off the end is not clickable - by design).
     */
    private static void pressTab(String name) {
        queue.add(() -> {
            Screen s = screen();
            if (s == null) {
                fail(W, "the " + name + " tab - no screen");
                return 0;
            }
            Object want = tab(name);
            double[] p = scan(s, hitTest(s, "tabAt"), t -> t == want);
            for (int i = 0; p == null && i < 60; i++) {
                // Wheel over the strip: first towards its start, then towards its end.
                wheel(s, s.width / 2.0, 12, 0, i < 30 ? 1 : -1);
                if (i == 29) {
                    set(HorseBrowserScreen.class, "tabScroll", 0);
                }
                p = scan(s, hitTest(s, "tabAt"), t -> t == want);
            }
            if (p == null) {
                fail(W, "the " + name + " tab - not on screen, even scrolled");
                return SETTLE;
            }
            click(p[0], p[1]);
            return SETTLE;
        });
        expect(W, "the " + name + " tab opens its page", () -> same("tab", name, name(field("tab"))));
    }

    private static @Nullable Object field(String name) {
        return get(Objects.requireNonNull(screen(), "no screen"), name);
    }

    private static EditBox box(String name) {
        return (EditBox) Objects.requireNonNull(field(name));
    }

    private static @Nullable String focused(String name) {
        Screen s = screen();
        return s != null && s.getFocused() == get(s, name) ? null : "not focused";
    }

    private static int listX() {
        return (Integer) call(Objects.requireNonNull(screen()), "listX");
    }

    private static int contentTop() {
        return (Integer) call(Objects.requireNonNull(screen()), "contentTop");
    }

    private static double @Nullable [] listRow(int i) {
        Screen s = Objects.requireNonNull(screen());
        return scan(s, hitTest(s, "rowAt"), r -> (Integer) r == i);
    }

    private static double @Nullable [] scanHit(String method, java.util.function.Predicate<Object> want, Object... extra) {
        Screen s = Objects.requireNonNull(screen());
        return scan(s, hitTest(s, method, extra), want);
    }

    /**
     * The point {@code find} gives, scrolling the list under {@code wheelAt} with the wheel
     * until the row is in view (a player's way to a row below the fold): to the top first,
     * then down a notch at a time. A real wheel turn, so the scroll is tested too.
     */
    private static double @Nullable [] scrolledTo(java.util.function.Supplier<double @Nullable []> find,
                                                  java.util.function.Supplier<double[]> wheelAt) {
        double[] p = find.get();
        if (p != null) {
            return p;
        }
        Screen s = Objects.requireNonNull(screen());
        double[] at = wheelAt.get();
        for (int i = 0; i < 40; i++) {
            wheel(s, at[0], at[1], 0, 1);
        }
        for (int i = 0; i < 60 && p == null; i++) {
            p = find.get();
            if (p == null) {
                wheel(s, at[0], at[1], 0, -1);
            }
        }
        return p;
    }

    private static boolean savedOpen() {
        return (Boolean) call(Objects.requireNonNull(field("savedSearches")), "isOpen");
    }

    private static double[] savedRow(int i) {
        Object p = Objects.requireNonNull(field("savedSearches"));
        return new double[] {(Integer) get(p, "x") + 30, (Integer) get(p, "y") + i * 12 + 6};
    }

    private static double[] geneDdRow(int i) {
        int x = (Integer) Objects.requireNonNull(field("geneDdX"));
        int y = (Integer) Objects.requireNonNull(field("geneDdY"));
        int scroll = (Integer) Objects.requireNonNull(field("geneDdScroll"));
        return new double[] {x + 30, y + (i - scroll) * 12 + 6};
    }

    /** Chip {@code index} of a chip list (-1 = the All chip, which is the highest index). */
    private static double @Nullable [] chip(String list, int index) {
        List<?> chips = (List<?>) Objects.requireNonNull(field(list));
        int want = index;
        if (index < 0) {
            want = -1;
            for (Object c : chips) {
                want = Math.max(want, (Integer) call(c, "index"));
            }
        }
        for (Object c : chips) {
            if ((Integer) call(c, "index") == want) {
                return centreOfBox(c);
            }
        }
        return null;
    }

    private static Object builder() {
        return get(HorseBrowserScreen.class, "builder");
    }

    private static boolean builderMenuOpen() {
        return (Boolean) call(builder(), "menuOpen");
    }

    private static int clauses() {
        return ((List<?>) get(builder(), "clauses")).size();
    }

    private static Object firstClause() {
        return ((List<?>) get(builder(), "clauses")).get(0);
    }

    /** The centre of a Query action (-1 + clause, -2 Clear, -3 Use it, -4 Save...). */
    private static double @Nullable [] action(int id) {
        for (Object h : (List<?>) get(builder(), "actions")) {
            if ((Integer) call(h, "row") == id) {
                return new double[] {(Integer) call(h, "x") + (Integer) call(h, "w") / 2.0, (Integer) call(h, "y") + 9};
            }
        }
        return null;
    }

    /** A clause cell: by row, kind, and (for the two NONE cells) width - 34 is NOT, 14 is x. */
    private static double @Nullable [] cell(int row, String what, int width) {
        for (Object h : (List<?>) get(builder(), "hits")) {
            if ((Integer) call(h, "row") == row && what.equals(name(call(h, "what")))
                    && (width < 0 || (Integer) call(h, "w") == width)) {
                return new double[] {(Integer) call(h, "x") + (Integer) call(h, "w") / 2.0, (Integer) call(h, "y") + 9};
            }
        }
        return null;
    }
}
