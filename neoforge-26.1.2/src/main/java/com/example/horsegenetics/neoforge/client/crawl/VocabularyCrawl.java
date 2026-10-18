package com.example.horsegenetics.neoforge.client.crawl;

import com.example.horsegenetics.common.breed.BreedStatCurve;
import com.example.horsegenetics.common.genetics.Allele;
import com.example.horsegenetics.common.genetics.AllelePair;
import com.example.horsegenetics.common.genetics.Expression;
import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import com.example.horsegenetics.common.horse.HorseListing;
import com.example.horsegenetics.common.horse.HorseQuery;
import com.example.horsegenetics.common.horse.Sex;
import com.example.horsegenetics.neoforge.client.ClientHorseRoster;
import com.example.horsegenetics.neoforge.client.CowboyTradeFilterHooks;
import com.example.horsegenetics.neoforge.client.HorseBrowserScreen;
import com.example.horsegenetics.neoforge.entity.Cowboy;
import com.example.horsegenetics.neoforge.server.DebugMenuCrawl;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.example.horsegenetics.neoforge.client.crawl.Crawl.*;

/**
 * <b>The whole query vocabulary, one query each</b>: every key in {@link HorseQuery#keys()}
 * and every flag in {@link HorseQuery#flags()}, built in the Build popup with a value taken
 * from a real horse, and checked on My horses and at the cowboy's counter (his string is the
 * wild half of the yard). Each must find exactly the horses it means - by a predicate
 * written here from the language's own definitions - and at least one horse somewhere, or
 * the check proved nothing and FAILs saying so. A key or flag added to {@code HorseQuery}
 * and not given a meaning here FAILs as unspecified, the way an untested button does.
 */
final class VocabularyCrawl {

    private static final String W = "Vocabulary";

    /** One term of the language: its column, operator, value and meaning (null value: a flag). */
    record Term(String column, String op, Supplier<String> value, Predicate<HorseListing> means) {
    }

    /** How many horses each term was meant to find, over both places - all must be above zero. */
    private static final Map<String, Integer> found = new HashMap<>();

    private VocabularyCrawl() {
    }

    static void queue() {
        found.clear();
        Map<String, Term> terms = terms();
        then(() -> {
            List<String> spoken = new ArrayList<>(HorseQuery.keys());
            spoken.addAll(HorseQuery.flags());
            for (String word : spoken) {
                if (!terms.containsKey(word)) {
                    fail(W, "\"" + word + "\" is in the language and has no case here - give it a meaning and a value");
                }
            }
        });

        // My horses: one browser, every term through the Build popup.
        then(() -> set(HorseBrowserScreen.class, "tab", enumConstant(HorseBrowserScreen.class, "Tab", "MY_HORSES")));
        open(HorseBrowserScreen::new);
        eventually(W, "the roster arrives", 80, () -> ClientHorseRoster.received() ? null : "not yet");
        for (Map.Entry<String, Term> e : terms.entrySet()) {
            String word = e.getKey();
            Term t = e.getValue();
            QueryCrawl.Query q = query(word, t);
            then(() -> ((EditBox) get(Objects.requireNonNull(screen()), "horseFilterBox")).setValue(""));
            QueryCrawl.buildInPopupFor(W + " \"" + word + "\" My horses", q, () -> get(screen(), "buildButton"),
                    () -> get(get(screen(), "queryPopup"), "builder"),
                    () -> String.valueOf(get(HorseBrowserScreen.class, "horseFilter")));
            pause(SETTLE);
            expect(W + " \"" + word + "\" My horses", "the table shows exactly the horses the query means",
                    () -> {
                        List<HorseListing> source = ClientHorseRoster.all();
                        count(word, source, t.means());
                        return QueryCrawl.exactlyFor(QueryCrawl.rowsFor("horseRows"), source, t.means());
                    });
        }
        close();

        // The cowboy's counter: the same terms over his wild string.
        eventuallyServer(W, "the cowboy is free to serve", 60, s ->
                DebugMenuCrawl.entity(MenuCrawlCases.player(s), DebugMenuCrawl.cowboy) instanceof Cowboy c
                        && c.getTradingPlayer() == null ? null : "still trading");
        serverDo(W, s -> {
            DebugMenuCrawl.openCowboy(MenuCrawlCases.player(s));
            return null;
        });
        eventually(W, "the cowboy's counter opens", 60, () -> isScreen(MerchantScreen.class));
        eventually(W, "the counter's filter row appears", 60, () -> {
            Object b = get(CowboyTradeFilterHooks.class, "buildButton");
            return b instanceof AbstractWidget aw && aw.visible ? null : "not yet";
        });
        for (Map.Entry<String, Term> e : terms.entrySet()) {
            String word = e.getKey();
            Term t = e.getValue();
            QueryCrawl.Query q = query(word, t);
            then(() -> {
                Object box = get(CowboyTradeFilterHooks.class, "filterBox");
                if (box instanceof EditBox b) {
                    b.setValue("");
                }
            });
            QueryCrawl.buildInPopupFor(W + " \"" + word + "\" Cowboy", q,
                    () -> get(CowboyTradeFilterHooks.class, "buildButton"),
                    () -> get(get(CowboyTradeFilterHooks.class, "POPUP"), "builder"),
                    () -> {
                        Object box = get(CowboyTradeFilterHooks.class, "filterBox");
                        return box instanceof EditBox b ? b.getValue() : "";
                    });
            eventuallyServer(W + " \"" + word + "\" Cowboy", "the counter offers exactly the horses the query means", 40, s -> {
                String problem = QueryCrawl.cowboyProblemFor(s, t.means());
                found.merge(word, QueryCrawl.cowboyMeant(), Integer::sum);
                return problem;
            });
        }
        close();

        // Every term found something, somewhere: a query that matches nothing proves nothing.
        then(() -> {
            for (String word : terms.keySet()) {
                int n = found.getOrDefault(word, 0);
                check(W, "\"" + word + "\" found a horse somewhere (" + n + ")",
                        n > 0 ? null : "no horse in the yard or the cowboy's string has it, so the check proved nothing");
            }
        });
    }

    private static QueryCrawl.Query query(String word, Term t) {
        boolean flag = t.value() == null;
        QueryCrawl.Clause clause = flag ? QueryCrawl.Clause.flag(word)
                : new QueryCrawl.Clause("AND", false, t.column(), t.op(), t.value(), !listed(word));
        Supplier<String> text = flag ? () -> word
                : () -> t.column() + " " + t.op() + " " + QueryCrawl.quoteFor(t.value().get());
        return new QueryCrawl.Query("vocabulary " + word, List.of(clause), text, t.means());
    }

    /** Columns whose value is picked from the builder's list rather than typed. */
    private static boolean listed(String word) {
        return word.equals("sex") || word.equals("age");
    }

    private static void count(String word, List<HorseListing> source, Predicate<HorseListing> means) {
        int n = 0;
        for (HorseListing h : source) {
            if (means.test(h)) {
                n++;
            }
        }
        found.merge(word, n, Integer::sum);
    }

    // ------------------------------------------------------------------ the language, read from its definitions

    private static Map<String, Term> terms() {
        Map<String, Term> t = new java.util.LinkedHashMap<>();
        // Flags (no value).
        flag(t, "mare", h -> h.sex() == Sex.FEMALE && h.adult());
        flag(t, "stallion", h -> h.sex() == Sex.MALE && !h.gelded() && h.adult());
        flag(t, "filly", h -> h.sex() == Sex.FEMALE && !h.adult());
        flag(t, "colt", h -> h.sex() == Sex.MALE && !h.gelded() && !h.adult());
        flag(t, "gelding", h -> h.gelded() && h.sex() == Sex.MALE);
        flag(t, "foal", h -> !h.adult());
        flag(t, "adult", HorseListing::adult);
        flag(t, "tamed", HorseListing::tamed);
        flag(t, "wild", h -> !h.tamed());
        flag(t, "herd", HorseListing::inHerd);
        flag(t, "loaded", HorseListing::loaded);
        flag(t, "lost", h -> !h.loaded());
        flag(t, "lethal", HorseListing::lethal);
        flag(t, "sick", h -> !h.conditions().isEmpty());
        flag(t, "healthy", h -> h.conditions().isEmpty());
        flag(t, "bred", HorseListing::hasParents);
        flag(t, "founder", h -> !h.hasParents());

        // Keys, each with a value off a real horse, read when its turn comes.
        Supplier<HorseListing> mare = () -> ClientHorseRoster.byId(DebugMenuCrawl.mare);
        Supplier<HorseListing> stallion = () -> ClientHorseRoster.byId(DebugMenuCrawl.stallion);
        Supplier<HorseListing> sick = () -> ClientHorseRoster.byId(DebugMenuCrawl.gelding);
        text(t, "name", () -> mare.get().firstName(),
                (h, v) -> has(h.displayName(), v) || has(h.registeredName(), v));
        text(t, "barn", () -> "Crawl", (h, v) -> has(h.barnName(), v));
        text(t, "breed", () -> mare.get().breed(), (h, v) -> has(h.breed(), v));
        text(t, "coat", () -> firstWord(mare.get().coat()), (h, v) -> has(h.coat(), v));
        text(t, "sex", () -> "stallion", (h, v) -> has(h.sexLabel(), v) || has(h.sex().name(), v));
        text(t, "age", () -> "foal", (h, v) -> has(h.ageLabel(), v));
        text(t, "by", () -> mare.get().tamedBy(), (h, v) -> has(h.tamedBy(), v) || has(h.bredBy(), v));
        text(t, "where", () -> firstWord(mare.get().where()), (h, v) -> has(h.where(), v));
        text(t, "condition", () -> firstWord(sick.get().conditions().isEmpty() ? "" : sick.get().conditions().get(0)),
                (h, v) -> h.conditions().stream().anyMatch(c -> has(c, v)));
        text(t, "gene", () -> carriedToken(stallion.get()), (h, v) -> carriesToken(h, v));
        text(t, "carries", () -> carriedToken(mare.get()), (h, v) -> carriesToken(h, v));
        text(t, "genotype", () -> carriedPair(stallion.get()), (h, v) -> hasPair(h, v));
        text(t, "expresses", () -> expressed(stallion.get()), (h, v) -> expressesWord(h, v));
        number(t, "gen", "=", () -> String.valueOf(mare.get().generation()),
                (h, v) -> h.generation() == (int) v);
        number(t, "bond", ">=", () -> "50", (h, v) -> h.bond() != HorseListing.BOND_UNKNOWN && h.bond() >= v);
        number(t, "speed", ">=", () -> floor(mare.get().speed(), 2), (h, v) -> h.speed() >= v);
        number(t, "health", ">=", () -> floor(mare.get().health(), 2), (h, v) -> h.health() >= v);
        number(t, "jump", ">=", () -> floor(mare.get().jump(), 2), (h, v) -> h.jump() >= v);
        number(t, "hands", ">=", () -> floor(BreedStatCurve.handsFor(mare.get().scale()), 1),
                (h, v) -> BreedStatCurve.handsFor(h.scale()) >= v);
        number(t, "size", ">=", () -> floor(mare.get().scale(), 2), (h, v) -> h.scale() >= v);
        number(t, "pull", ">=", () -> floor(mare.get().pull(), 1), (h, v) -> h.pull() >= v);
        return t;
    }

    private interface TextMeaning {
        boolean test(HorseListing h, String value);
    }

    private interface NumberMeaning {
        boolean test(HorseListing h, double value);
    }

    private static void flag(Map<String, Term> t, String word, Predicate<HorseListing> means) {
        t.put(word, new Term(word, "=", null, means));
    }

    private static void text(Map<String, Term> t, String key, Supplier<String> value, TextMeaning means) {
        t.put(key, new Term(key, "=", value, h -> means.test(h, value.get())));
    }

    private static void number(Map<String, Term> t, String key, String op, Supplier<String> value, NumberMeaning means) {
        t.put(key, new Term(key, op, value, h -> means.test(h, Double.parseDouble(value.get()))));
    }

    private static boolean has(@Nullable String text, String word) {
        return text != null && !word.isEmpty() && text.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT));
    }

    private static String firstWord(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.strip().split("\\s+")[0];
    }

    private static String floor(double v, int places) {
        double f = Math.pow(10, places);
        return String.valueOf(Math.floor(v * f) / f);
    }

    /** A non-default, non-placeholder allele token the horse carries. */
    private static String carriedToken(HorseListing h) {
        for (Gene g : Genes.codeOrder()) {
            AllelePair pair = h.genotype().pair(g);
            if (pair == null) {
                continue;
            }
            for (Allele a : new Allele[] {pair.first(), pair.second()}) {
                if (a != null && a != g.defaultAllele() && !g.isPlaceholder(a)) {
                    return a.token();
                }
            }
        }
        return "";
    }

    /** Some allele of the horse's is spelled exactly {@code token}. */
    private static boolean carriesToken(HorseListing h, String token) {
        for (Gene g : Genes.codeOrder()) {
            AllelePair pair = h.genotype().pair(g);
            if (pair == null) {
                continue;
            }
            for (Allele a : new Allele[] {pair.first(), pair.second()}) {
                if (a != null && a.token().equals(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The pair, as A/B, at the first locus where the horse is not wild type. */
    private static String carriedPair(HorseListing h) {
        for (Gene g : Genes.codeOrder()) {
            AllelePair pair = h.genotype().pair(g);
            if (pair != null && pair.first() != null && pair.second() != null
                    && (pair.first() != g.defaultAllele() || pair.second() != g.defaultAllele())) {
                return pair.first().token() + "/" + pair.second().token();
            }
        }
        return "";
    }

    /** Some locus holds exactly these two tokens, either way round. */
    private static boolean hasPair(HorseListing h, String value) {
        int cut = value.indexOf('/');
        if (cut <= 0) {
            return false;
        }
        String x = value.substring(0, cut);
        String y = value.substring(cut + 1);
        for (Gene g : Genes.codeOrder()) {
            AllelePair pair = h.genotype().pair(g);
            if (pair == null || pair.first() == null || pair.second() == null) {
                continue;
            }
            String a = pair.first().token();
            String b = pair.second().token();
            if ((a.equals(x) && b.equals(y)) || (a.equals(y) && b.equals(x))) {
                return true;
            }
        }
        return false;
    }

    /** The name of the first gene effect the horse shows that is not wild type. */
    private static String expressed(HorseListing h) {
        for (Gene g : Genes.codeOrder()) {
            Expression e = h.genotype().expressionOf(g);
            if (e != null && !e.wildType()) {
                return e.name();
            }
        }
        return "";
    }

    /** Some gene's non-wild effect is named (or identified) by the word. */
    private static boolean expressesWord(HorseListing h, String word) {
        for (Gene g : Genes.codeOrder()) {
            Expression e = h.genotype().expressionOf(g);
            if (e == null || e.wildType()) {
                continue;
            }
            if (has(e.name(), word) || has(e.id(), word) || g.name().equalsIgnoreCase(word)
                    || g.key().toLowerCase(Locale.ROOT).endsWith("." + word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
