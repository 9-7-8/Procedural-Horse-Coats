package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.horse.HorseListing;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.common.horse.StasisBrowseRow;
import com.example.horsegenetics.common.horse.TransferDeed;
import com.example.horsegenetics.neoforge.ClientConfig;
import com.example.horsegenetics.neoforge.client.ClientHorseRoster;
import com.example.horsegenetics.neoforge.client.CowboyTradeFilterHooks;
import com.example.horsegenetics.neoforge.client.HorseBrowserScreen;
import com.example.horsegenetics.neoforge.client.HorseStasisBankScreen;
import com.example.horsegenetics.neoforge.data.ModDataComponents;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.neoforge.server.DebugMenuCrawl;
import com.example.horsegenetics.neoforge.server.HorseRecords;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/**
 * <b>The query crawl</b>: build a query in the builder the way a player would (clicks on
 * the clause cells, picks from the menus, typing where there is nothing to pick), save it
 * under a name, then apply the saved search through the saved-search menu of every place a
 * query works - My horses, both Breeding preview pickers, the stasis bank's Browse tab and
 * the cowboy's counter - and check that each one shows <i>exactly</i> the horses the query
 * describes.
 *
 * <p>"Exactly" is read off a predicate written here from the query's meaning (a mare is an
 * adult female, {@code gene = any} is a non-default copy), not from {@code HorseQuery}: the
 * engine checking itself would agree with its own mistakes. So a horse that matches and is
 * missing is a FAIL, and so is one that is shown and should not be.
 */
final class QueryCrawl {

    private static final String W = "Queries";

    /** One clause as a player fills it in. {@code typed}: the value is typed, there is no list. */
    record Clause(String join, boolean not, String column, String op, Supplier<String> value, boolean typed) {
        static Clause flag(String column) {
            return new Clause("AND", false, column, "=", () -> "", false);
        }
    }

    /**
     * A query: its saved name, its clauses, the text the builder should make of them, what
     * it means (the predicate), and a horse that must be among the results somewhere.
     */
    record Query(String name, List<Clause> clauses, Supplier<String> text, Predicate<HorseListing> means,
                 boolean popup) {
        Query(String name, List<Clause> clauses, Supplier<String> text, Predicate<HorseListing> means) {
            this(name, clauses, text, means, false);
        }
    }

    /** The gene picked for the gene query - one the stallion carries something other than wild type at. */
    private static Gene gene;
    /** The first name of the horse filed in the bank's spacer chamber. */
    private static String filedName = "";

    private QueryCrawl() {
    }

    static void queue() {
        then(() -> {
            HorseListing s = ClientHorseRoster.byId(DebugMenuCrawl.stallion);
            gene = s == null ? Genes.byKey("horsegenetics.extension") : carried(s);
        });
        serverDo(W, srv -> {
            var bank = (com.example.horsegenetics.neoforge.block.HorseStasisBankBlockEntity)
                    MenuCrawlCases.player(srv).level().getBlockEntity(DebugMenuCrawl.bank);
            var snap = bank == null ? null
                    : com.example.horsegenetics.neoforge.item.StasisChamberItem.snapshotOf(bank.chambers().getItem(0));
            filedName = snap == null ? "" : snap.horseName().split(" ")[0];
            return null;
        });

        List<Query> queries = List.of(
                new Query("crawl 1 mares", List.of(Clause.flag("mare")), () -> "mare",
                        h -> h.sex() == Sex.FEMALE && h.adult()),
                new Query("crawl 2 stallions",
                        List.of(new Clause("AND", false, "sex", "=", () -> "stallion", false)),
                        () -> "sex = stallion",
                        h -> h.sex() == Sex.MALE && h.adult() && !h.gelded()),
                new Query("crawl 3 gene", List.of(new Clause("AND", false, "", "=", () -> "any", false)),
                        () -> quote(gene.name()) + " = any", h -> copies(h, gene) > 0),
                new Query("crawl 4 either",
                        List.of(Clause.flag("filly"), new Clause("OR", false, "stallion", "=", () -> "", false)),
                        () -> "filly OR stallion",
                        h -> (h.sex() == Sex.FEMALE && !h.adult()) || (h.sex() == Sex.MALE && h.adult() && !h.gelded()),
                        true),
                new Query("crawl 5 not mares", List.of(new Clause("AND", true, "mare", "=", () -> "", false)),
                        () -> "NOT mare", h -> !(h.sex() == Sex.FEMALE && h.adult())),
                new Query("crawl 6 generation",
                        List.of(new Clause("AND", false, "gen", ">=", () -> "0", true)),
                        () -> "gen >= 0", h -> h.generation() >= 0, true),
                new Query("crawl 7 filed name",
                        List.of(new Clause("AND", false, "name", "=", () -> filedName, true)),
                        () -> "name = " + quote(filedName), h -> named(h, filedName)));

        for (Query q : queries) {
            one(q);
        }
        then(() -> skip(W, "the Horse realm tab - only there inside the realm, and the crawl stays in the overworld"));
        VocabularyCrawl.queue();
    }

    // ------------------------------------------------------------------ shared with VocabularyCrawl

    static void buildInPopupFor(String w, Query q, Supplier<@Nullable Object> button,
                                Supplier<Object> popupBuilder, Supplier<String> boxText) {
        buildInPopup(w, q, button, popupBuilder, boxText);
    }

    static @Nullable String exactlyFor(List<HorseListing> shown, List<HorseListing> source, Predicate<HorseListing> means) {
        return exactly(shown, source, means);
    }

    static List<HorseListing> rowsFor(String field) {
        return rows(field);
    }

    static @Nullable String cowboyProblemFor(IntegratedServer s, Predicate<HorseListing> means) {
        return cowboyProblem(s, means);
    }

    /** How many of his string the last cowboy check meant. */
    static int cowboyMeant() {
        return lastMeant;
    }

    static String quoteFor(String word) {
        return quote(word);
    }

    private static int lastMeant;

    // ------------------------------------------------------------------ one query, everywhere

    private static void one(Query q) {
        String w = W + " \"" + q.name() + "\"";
        boolean[] built = {false};
        Step end = marker();

        // Build it on the Query tab, by hand.
        then(() -> set(HorseBrowserScreen.class, "tab", enumConstant(HorseBrowserScreen.class, "Tab", "QUERY")));
        open(HorseBrowserScreen::new);
        pressAt(w, "Clear", () -> action(-2));
        for (int i = 0; i < q.clauses().size(); i++) {
            clause(w, i, q.clauses().get(i));
        }
        then(() -> {
            String got = String.valueOf(call(builder(), "query"));
            String want = q.text().get();
            built[0] = got.equals(want);
            check(w, "the builder makes the query \"" + want + "\"",
                    built[0] ? null : "it made \"" + got + "\"");
        });
        skipUnless(() -> built[0], end, w, "the rest of this query - it could not be built");

        // Save it under its name.
        pressAt(w, "Save...", () -> action(-4));
        pressAt(w, "the menu's first row (Save this search...)", () -> pickerRow(browserPicker(), 0));
        then(() -> {
            type(q.name());
            key(ENTER);
        });
        pause(SETTLE);
        expect(w, "it is saved under its name", () -> {
            String text = q.text().get();
            return ClientConfig.isSaved(text) ? same("its name", q.name(), ClientConfig.nameOf(text)) : "not saved";
        });

        // My horses.
        pressTab("MY_HORSES");
        useSaved(w + " My horses", q, BrowserPicker.INSTANCE, () -> get(screen(), "savedSearchButton"));
        expect(w + " My horses", "the filter is the saved query", () -> same("horseFilter", q.text().get(),
                get(HorseBrowserScreen.class, "horseFilter")));
        pause(SETTLE);
        expect(w + " My horses", "the table shows exactly the horses it means",
                () -> exactly(rows("horseRows"), ClientHorseRoster.all(), q.means()));
        if (q.popup()) {
            then(() -> ((EditBox) get(Objects.requireNonNull(screen()), "horseFilterBox")).setValue(""));
            buildInPopup(w + " My horses Build", q, () -> get(screen(), "buildButton"),
                    () -> get(get(screen(), "queryPopup"), "builder"),
                    () -> String.valueOf(get(HorseBrowserScreen.class, "horseFilter")));
            pause(SETTLE);
            expect(w + " My horses Build", "the table shows exactly the horses it means",
                    () -> exactly(rows("horseRows"), ClientHorseRoster.all(), q.means()));
        }

        // Breeding preview: both pickers.
        pressTab("BREEDING_PREVIEW");
        useSaved(w + " Breeding", q, BrowserPicker.INSTANCE, () -> get(screen(), "savedSearchButton"));
        expect(w + " Breeding", "the picker filter is the saved query", () -> same("breedingFilter", q.text().get(),
                get(HorseBrowserScreen.class, "breedingFilter")));
        pause(SETTLE);
        expect(w + " Breeding", "the mares list shows exactly the mares it means",
                () -> exactly(rows("mareRows"), ClientHorseRoster.of(Sex.FEMALE), q.means()));
        expect(w + " Breeding", "the stallions list shows exactly the stallions it means",
                () -> exactly(rows("stallionRows"), ClientHorseRoster.of(Sex.MALE), q.means()));
        if (q.popup()) {
            then(() -> ((EditBox) get(Objects.requireNonNull(screen()), "breedingFilterBox")).setValue(""));
            buildInPopup(w + " Breeding Build", q, () -> get(screen(), "buildButton"),
                    () -> get(get(screen(), "queryPopup"), "builder"),
                    () -> String.valueOf(get(HorseBrowserScreen.class, "breedingFilter")));
            pause(SETTLE);
            expect(w + " Breeding Build", "the mares list shows exactly the mares it means",
                    () -> exactly(rows("mareRows"), ClientHorseRoster.of(Sex.FEMALE), q.means()));
        }
        close();

        // The stasis bank's Browse tab.
        serverDo(w, s -> {
            DebugMenuCrawl.openBlock(MenuCrawlCases.player(s), DebugMenuCrawl.bank);
            return null;
        });
        eventually(w + " Bank", "the bank opens", 60, () -> isScreen(HorseStasisBankScreen.class));
        pressAt(w + " Bank", "the Browse tab", () -> {
            Screen s = Objects.requireNonNull(screen());
            Object browse = enumConstant(com.example.horsegenetics.neoforge.menu.HorseStasisBankMenu.class, "Tab", "BROWSE");
            return scan(s, hitTest(s, "tabAt"), t -> t == browse);
        });
        useSaved(w + " Bank", q, BankPicker.INSTANCE, () -> get(screen(), "savedSearchButton"));
        expect(w + " Bank", "the filter box holds the saved query", () -> same("the filter", q.text().get(),
                ((EditBox) get(Objects.requireNonNull(screen()), "filterBox")).getValue()));
        pause(SETTLE);
        expect(w + " Bank", "Browse shows exactly the filed horses it means", () -> {
            Screen s = Objects.requireNonNull(screen());
            Set<UUID> want = new TreeSet<>();
            for (Object o : (List<?>) get(s, "rows")) {
                StasisBrowseRow r = (StasisBrowseRow) o;
                if (r.searchable() && q.means().test(r.listing())) {
                    want.add(r.listing().id());
                }
            }
            Set<UUID> got = new TreeSet<>();
            for (Object o : (List<?>) get(s, "shown")) {
                StasisBrowseRow r = (StasisBrowseRow) o;
                got.add(r.listing() == null ? new UUID(0, r.slot()) : r.listing().id());
            }
            return compare(want, got);
        });
        if (q.popup()) {
            then(() -> ((EditBox) get(Objects.requireNonNull(screen()), "filterBox")).setValue(""));
            buildInPopup(w + " Bank Build", q, () -> get(screen(), "buildButton"),
                    () -> get(get(screen(), "queryPopup"), "builder"),
                    () -> ((EditBox) get(Objects.requireNonNull(screen()), "filterBox")).getValue());
        }
        close();

        // The cowboy's counter.
        // He serves one customer at a time: the last visit must have finished.
        eventuallyServer(w + " Cowboy", "the cowboy is free to serve", 60, s ->
                DebugMenuCrawl.entity(MenuCrawlCases.player(s), DebugMenuCrawl.cowboy) instanceof Cowboy c
                        && c.getTradingPlayer() == null ? null : "still trading");
        serverDo(w, s -> {
            DebugMenuCrawl.openCowboy(MenuCrawlCases.player(s));
            return null;
        });
        eventually(w + " Cowboy", "the cowboy's counter opens", 60, () -> isScreen(MerchantScreen.class));
        // The box and its buttons show once the offers have arrived and proved him a cowboy.
        eventually(w + " Cowboy", "the counter's filter row appears", 60, () -> {
            Object b = get(CowboyTradeFilterHooks.class, "savedButton");
            return b instanceof AbstractWidget aw && aw.visible ? null : "not yet";
        });
        useSaved(w + " Cowboy", q, CowboyPicker.INSTANCE, () -> get(CowboyTradeFilterHooks.class, "savedButton"));
        expect(w + " Cowboy", "the filter box holds the saved query", () -> {
            Object box = get(CowboyTradeFilterHooks.class, "filterBox");
            return box == null ? "no filter box" : same("the filter", q.text().get(), ((EditBox) box).getValue());
        });
        eventuallyServer(w + " Cowboy", "the counter offers exactly the horses of his string it means", 40,
                s -> cowboyProblem(s, q.means()));
        expect(w + " Cowboy", "the screen shows the offers the server made", () -> {
            Screen s = screen();
            int shown = s instanceof MerchantScreen m ? m.getMenu().getOffers().size() : -1;
            return same("offers on screen", cowboyOffers[0], shown);
        });
        if (q.popup()) {
            then(() -> {
                Object box = get(CowboyTradeFilterHooks.class, "filterBox");
                if (box instanceof EditBox e) {
                    e.setValue("");
                }
            });
            buildInPopup(w + " Cowboy Build", q, () -> get(CowboyTradeFilterHooks.class, "buildButton"),
                    () -> get(get(CowboyTradeFilterHooks.class, "POPUP"), "builder"),
                    () -> {
                        Object box = get(CowboyTradeFilterHooks.class, "filterBox");
                        return box instanceof EditBox e ? e.getValue() : "";
                    });
            eventuallyServer(w + " Cowboy Build", "the counter offers exactly the horses it means", 40,
                    s -> cowboyProblem(s, q.means()));
        }
        close();

        place(end);
    }

    // ------------------------------------------------------------------ building

    /** Is the Build panel of whichever screen is up open? */
    private static boolean popupOpen(String w) {
        Object popup = w.contains("Cowboy") ? get(CowboyTradeFilterHooks.class, "POPUP")
                : get(java.util.Objects.requireNonNull(screen()), "queryPopup");
        return (Boolean) call(popup, "isOpen");
    }

    /**
     * Build {@code q} again, from nothing, in the Build popup beside a filter box: press
     * Build, fill the rows, press Done - and the box must hold the query, which is all the
     * popup promises (the box's own wiring does the filtering, checked by the caller).
     */
    private static void buildInPopup(String w, Query q, Supplier<@Nullable Object> button,
                                     Supplier<Object> popupBuilder, Supplier<String> boxText) {
        press(w, "Build", () -> {
            Object b = button.get();
            return b instanceof AbstractWidget aw && aw.visible ? aw : null;
        });
        then(() -> builderNow = popupBuilder);
        expect(w, "Build opens the builder panel", () -> {
            Object b = popupBuilder.get();
            // The builder lives in a popup; the popup is the one that holds it.
            return b != null && popupOpen(w) ? null : "the panel is not open";
        });
        pressAt(w, "Clear", () -> action(-2));
        for (int i = 0; i < q.clauses().size(); i++) {
            clause(w, i, q.clauses().get(i));
            int n = i;
            then(() -> com.example.horsegenetics.neoforge.HorseGenetics.LOGGER.info(
                    "[crawl] {} after clause {}: panel open {}, menu open {}, query \"{}\", screen {}", w, n + 1,
                    popupOpen(w), call(builder(), "menuOpen"), call(builder(), "query"), name(screen())));
        }
        LayoutAudit.audit(w + " (popup filled in)");
        then(() -> check(w, "the rows make \"" + q.text().get() + "\"",
                same("the built query", q.text().get(), String.valueOf(call(builder(), "query")))));
        pressAt(w, "Done", () -> action(-3));
        then(() -> builderNow = () -> get(HorseBrowserScreen.class, "builder"));
        pause(SETTLE);
        expect(w, "the box holds the query built in the popup", () -> same("the box", q.text().get(), boxText.get()));
    }

    private static void clause(String w, int i, Clause c) {
        if (i > 0) {
            pressAt(w, "+ clause", () -> action(-1));
        }
        if (!c.join().equals("AND")) {
            pressAt(w, "clause " + (i + 1) + "'s join", () -> cell(i, "JOIN", -1));
            pickOption(w, c.join());
        }
        if (c.not()) {
            pressAt(w, "clause " + (i + 1) + "'s NOT", () -> cell(i, "NONE", 34));
        }
        pressAt(w, "clause " + (i + 1) + "'s column", () -> cell(i, "COLUMN", -1));
        pickOption(w, c.column().isEmpty() ? null : c.column());
        then(() -> {
            String value = c.value().get();
            if (value.isEmpty()) {
                return;
            }
            if (!c.op().equals("=")) {
                double[] p = cell(i, "OP", -1);
                if (p == null) {
                    fail(w, "clause " + (i + 1) + "'s operator cell is not on screen");
                    return;
                }
                click(p[0], p[1]);
                clickOption(w, c.op());
            }
        });
        pause(SETTLE);
        then(() -> {
            String value = c.value().get();
            if (value.isEmpty()) {
                return;
            }
            double[] p = cell(i, "VALUE", -1);
            if (p == null) {
                fail(w, "clause " + (i + 1) + "'s value cell is not on screen");
                return;
            }
            click(p[0], p[1]);
            if (c.typed()) {
                // Nothing to pick for a number or a name: type it and press Enter.
                type(value);
                key(ENTER);
            } else {
                clickOption(w, value);
            }
        });
        pause(SETTLE);
    }

    /** Pick {@code label} from the open builder menu (null: the gene the query is about). */
    private static void pickOption(String w, @Nullable String label) {
        pause(SETTLE);
        LayoutAudit.audit(w + " (menu open)");
        then(() -> clickOption(w, label == null ? gene.name() : label));
        pause(SETTLE);
    }

    /**
     * Click {@code label} in the open builder menu, wheeling it into view first - a long
     * list (the columns run to every gene) shows twelve rows at a time.
     */
    private static void clickOption(String w, String label) {
        Object b = builder();
        if (!(Boolean) call(b, "menuOpen")) {
            fail(w, "no menu open to pick \"" + label + "\" from");
            return;
        }
        List<?> options = (List<?>) call(b, "options");
        int index = options.indexOf(label);
        if (index < 0) {
            fail(w, "\"" + label + "\" is not in the menu (" + options.size() + " options)");
            return;
        }
        Screen s = Objects.requireNonNull(screen());
        int x = (Integer) get(b, "menuX");
        int y = (Integer) get(b, "menuY");
        int scroll = (Integer) get(b, "menuScroll");
        if (index < scroll || index >= scroll + 12) {
            wheel(s, x + 20, y + 30, 0, scroll - index);
            scroll = (Integer) get(b, "menuScroll");
        }
        click(x + 30, y + 12 + (index - scroll) * 12 + 6);
    }

    // ------------------------------------------------------------------ the saved-search menus

    /** Where a screen keeps its saved-search menu. */
    interface PickerAt {
        Object picker();
    }

    enum BrowserPicker implements PickerAt {
        INSTANCE;

        @Override
        public Object picker() {
            return browserPicker();
        }
    }

    enum BankPicker implements PickerAt {
        INSTANCE;

        @Override
        public Object picker() {
            return get(Objects.requireNonNull(screen()), "savedSearches");
        }
    }

    enum CowboyPicker implements PickerAt {
        INSTANCE;

        @Override
        public Object picker() {
            return get(CowboyTradeFilterHooks.class, "SAVED");
        }
    }

    /** Open a screen's saved-search menu with its button and pick this query's row by name. */
    private static void useSaved(String w, Query q, PickerAt at, Supplier<@Nullable Object> button) {
        press(w, "its saved-search button", () -> {
            Object b = button.get();
            return b instanceof AbstractWidget aw && aw.visible ? aw : null;
        });
        expect(w, "the saved-search menu opens", () -> (Boolean) call(at.picker(), "isOpen") ? null : "not open");
        LayoutAudit.audit(w + " (saved searches open)");
        pressAt(w, "the row \"" + q.name() + "\"", () -> {
            Object p = at.picker();
            List<?> rows = (List<?>) call(p, "rows");
            for (int i = 1; i < rows.size(); i++) {
                if (String.valueOf(rows.get(i)).startsWith(q.name())) {
                    return pickerRow(p, i);
                }
            }
            return null;
        });
    }

    private static Object browserPicker() {
        return get(Objects.requireNonNull(screen()), "savedSearches");
    }

    private static double[] pickerRow(Object picker, int i) {
        int x = (Integer) get(picker, "x");
        int y = (Integer) get(picker, "y");
        int scroll = (Integer) get(picker, "scroll");
        return new double[] {x + 30, y + (i - scroll) * 12 + 6};
    }

    // ------------------------------------------------------------------ checks

    private static final int[] cowboyOffers = {-1};

    /** The cowboy's offers, by horse, against his string's horses the query means. */
    private static @Nullable String cowboyProblem(IntegratedServer s, Predicate<HorseListing> means) {
        var player = MenuCrawlCases.player(s);
        if (!(DebugMenuCrawl.entity(player, DebugMenuCrawl.cowboy) instanceof Cowboy dealer)) {
            return "the cowboy is gone";
        }
        // His whole string, not just the horses the crawl gave him: a cowboy keeps what
        // he had. The same horses Cowboy.updateTrades would offer unfiltered.
        Set<UUID> want = new TreeSet<>();
        for (UUID id : dealer.herdIds()) {
            Horse h = DebugMenuCrawl.horse(player, id);
            if (h == null || !h.isAlive() || h.isTamed() || (Boolean) call(dealer, "hasSold", id)
                    || !HorseRecords.of(h).hasName()) {
                continue;
            }
            HorseRecord r = HorseRecords.of(h);
            HorseListing listing = (HorseListing) call(Cowboy.class, "listingOf", h, r);
            if (means.test(listing)) {
                want.add(id);
            }
        }
        Set<UUID> got = new TreeSet<>();
        for (MerchantOffer offer : dealer.getOffers()) {
            TransferDeed deed = offer.getResult().get(ModDataComponents.HORSE_DEED.get());
            if (deed != null) {
                got.add(deed.horseId());
            }
        }
        cowboyOffers[0] = dealer.getOffers().size();
        lastMeant = want.size();
        return compare(want, got);
    }

    @SuppressWarnings("unchecked")
    private static List<HorseListing> rows(String field) {
        return (List<HorseListing>) get(Objects.requireNonNull(screen()), field);
    }

    /** The shown rows are exactly the source rows the query means. */
    private static @Nullable String exactly(List<HorseListing> shown, List<HorseListing> source,
                                            Predicate<HorseListing> means) {
        Set<UUID> want = new TreeSet<>();
        for (HorseListing h : source) {
            if (means.test(h)) {
                want.add(h.id());
            }
        }
        Set<UUID> got = new TreeSet<>();
        for (HorseListing h : shown) {
            got.add(h.id());
        }
        return compare(want, got);
    }

    private static @Nullable String compare(Set<UUID> want, Set<UUID> got) {
        if (want.equals(got)) {
            return null;
        }
        List<String> missing = new ArrayList<>();
        for (UUID id : want) {
            if (!got.contains(id)) {
                missing.add(nameOf(id));
            }
        }
        List<String> extra = new ArrayList<>();
        for (UUID id : got) {
            if (!want.contains(id)) {
                extra.add(nameOf(id));
            }
        }
        return (missing.isEmpty() ? "" : "missing " + missing + " ") + (extra.isEmpty() ? "" : "should not show " + extra)
                + " (" + got.size() + " shown, " + want.size() + " meant)";
    }

    private static String nameOf(UUID id) {
        HorseListing h = ClientHorseRoster.byId(id);
        return h == null ? id.toString().substring(0, 8) : h.firstName() + " " + h.lastName();
    }

    // ------------------------------------------------------------------ meanings

    /** The first gene in code order where the horse has a non-default copy. */
    private static Gene carried(HorseListing h) {
        for (Gene g : Genes.codeOrder()) {
            // Not a gene named like a key or a flag (Sex): the builder lists that name as the
            // key, so the gene's own column is not one a player can pick.
            if (com.example.horsegenetics.common.horse.HorseQuery.keys().contains(g.name().toLowerCase(Locale.ROOT))
                    || com.example.horsegenetics.common.horse.HorseQuery.flags().contains(g.name().toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (copies(h, g) > 0) {
                return g;
            }
        }
        return Genes.byKey("horsegenetics.extension");
    }

    /** Non-default, non-placeholder copies at this locus - what {@code = any} asks about. */
    private static int copies(HorseListing h, Gene g) {
        AllelePair pair = h.genotype() == null ? null : h.genotype().pair(g);
        if (pair == null) {
            return 0;
        }
        int n = 0;
        for (Allele a : new Allele[] {pair.first(), pair.second()}) {
            if (a != null && !g.isPlaceholder(a) && a != g.defaultAllele()) {
                n++;
            }
        }
        return n;
    }

    /** Either of a horse's names contains {@code word}, ignoring case. */
    private static boolean named(HorseListing h, String word) {
        String w = word.toLowerCase(Locale.ROOT);
        return h.displayName().toLowerCase(Locale.ROOT).contains(w)
                || h.registeredName().toLowerCase(Locale.ROOT).contains(w);
    }

    /** How the builder writes a word: quoted when it has a space in it or is a keyword. */
    private static String quote(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        boolean keyword = lower.equals("and") || lower.equals("or") || lower.equals("not")
                || lower.equals("in") || lower.equals("like");
        boolean breaks = false;
        for (char c : word.toCharArray()) {
            breaks |= Character.isWhitespace(c) || ",()=<>!".indexOf(c) >= 0;
        }
        return breaks || keyword ? "'" + word + "'" : word;
    }

    // ------------------------------------------------------------------ the browser

    /** The builder being driven: the Query tab's, or the open Build popup's. */
    private static Supplier<Object> builderNow = () -> get(HorseBrowserScreen.class, "builder");

    private static Object builder() {
        return builderNow.get();
    }

    private static double @Nullable [] action(int id) {
        for (Object h : (List<?>) get(builder(), "actions")) {
            if ((Integer) call(h, "row") == id) {
                return new double[] {(Integer) call(h, "x") + (Integer) call(h, "w") / 2.0, (Integer) call(h, "y") + 9};
            }
        }
        return null;
    }

    private static double @Nullable [] cell(int row, String what, int width) {
        for (Object h : (List<?>) get(builder(), "hits")) {
            if ((Integer) call(h, "row") == row && what.equals(name(call(h, "what")))
                    && (width < 0 || (Integer) call(h, "w") == width)) {
                return new double[] {(Integer) call(h, "x") + (Integer) call(h, "w") / 2.0, (Integer) call(h, "y") + 9};
            }
        }
        return null;
    }

    private static void pressTab(String name) {
        pressAt(W, "the " + name + " tab", () -> {
            Screen s = Objects.requireNonNull(screen());
            Object want = enumConstant(HorseBrowserScreen.class, "Tab", name);
            double[] p = scan(s, hitTest(s, "tabAt"), t -> t == want);
            for (int i = 0; p == null && i < 60; i++) {
                wheel(s, s.width / 2.0, 12, 0, i < 30 ? 1 : -1);
                p = scan(s, hitTest(s, "tabAt"), t -> t == want);
            }
            return p;
        });
        expect(W, "the " + name + " tab opens", () -> same("tab", name, name(get(HorseBrowserScreen.class, "tab"))));
    }
}
