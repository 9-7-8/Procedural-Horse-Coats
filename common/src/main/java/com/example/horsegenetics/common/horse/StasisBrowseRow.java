package com.example.horsegenetics.common.horse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * <b>One chamber as a line of the Horse Stasis Bank's Browse tab</b> - the name
 * on the chamber, the tier of the chamber it is in, and the horse's row from the
 * stable's own records, if the player has one for it.
 *
 * <h2>The tier is the whole gate</h2>
 * {@link StasisTier#searchable()} decides whether this row may be searched, and
 * nothing here switches on a tier by name - which is the rule
 * {@code StasisTierTest} exists to keep. A Basic chamber is still
 * <i>listed</i>: the chamber's own tooltip says which horse is inside at every
 * tier, so hiding the name in the bank would be the bank contradicting the item
 * in the player's hand. What Basic does not buy is being able to <i>ask</i>
 * anything but that name - only a query that is nothing but a name reaches it
 * ({@link #plainNameQuery}), and {@link #locked} counts the rows a query had to
 * leave out so the player is told rather than left wondering where a horse went.
 *
 * <h2>Why the listing may be missing</h2>
 * {@link #listing} is the same {@link HorseListing} the browser's <i>My
 * horses</i> table sorts, looked up by the id the chamber remembers. It is
 * absent when the stable's records have nothing under that id - a chamber
 * somebody else filled and handed over, or a stable so large the roster was cut
 * short. That row keeps its name and says it has no papers rather than
 * pretending the chamber is empty.
 *
 * <h2>The row is also where a horse is put to stud</h2>
 * {@link #atStud} is the chamber's own {@code stasis_at_stud} component, not
 * anything the bank remembers: the mark rides on the item the way the horse
 * does, so it survives being taken out and put back, needs no syncing of its
 * own, and cannot drift from the chamber it describes. {@link #slot} is which
 * chamber slot the row came from, which is the whole of what a click on it has
 * to tell the server. Only a tier that {@link StasisTier#breedsInBank()} may be
 * marked - {@link #mayStud()} - and the screen says so rather than ignoring the
 * click.
 *
 * <p>Pure Java, and the reason the Browse tab has almost no logic of its own:
 * the gating and the filtering are testable without a game, which is where
 * {@code StasisBrowseTest} works.
 */
public record StasisBrowseRow(int slot, String name, StasisTier tier, HorseListing listing, boolean atStud) {

    public StasisBrowseRow {
        name = name == null ? "" : name;
    }

    /** May a query reach this row? Both halves are needed: a tier and a record. */
    public boolean searchable() {
        return tier != null && tier.searchable() && listing != null;
    }

    /** May this chamber be put to stud at all? The top rung, and only the top rung. */
    public boolean mayStud() {
        return tier != null && tier.breedsInBank();
    }

    /** How many of {@code rows} are turned out into the bank's paddock. */
    public static int atStud(List<StasisBrowseRow> rows) {
        int n = 0;
        for (StasisBrowseRow row : rows) {
            if (row.atStud() && row.mayStud()) {
                n++;
            }
        }
        return n;
    }

    /** The horse's written name - the stable's spelling if there is one, else the chamber's. */
    public String displayName() {
        if (listing != null && !listing.displayName().isEmpty()) {
            return listing.displayName();
        }
        return name;
    }

    /** The left-hand line under the name: what the horse is, or why that cannot be said. */
    public String detail() {
        if (tier != null && !tier.searchable()) {
            return "A " + tier.id() + " chamber cannot be looked into";
        }
        if (listing == null) {
            return "No stable record under this horse's name";
        }
        return listing.sexLabel() + " - " + listing.coat();
    }

    /** The right-hand line under the tier: where the horse came from. */
    public String origin() {
        if (listing == null) {
            return "";
        }
        String breed = listing.breed().isEmpty() ? "" : listing.breed() + ", ";
        return breed + "gen " + listing.generation();
    }

    /**
     * The rows a query leaves showing. An empty query shows everything, a query
     * shows only the rows it can actually reach - so a horse with no papers drops
     * out the moment anything is typed, and so does a Basic chamber unless the
     * query is nothing but its name ({@link #plainNameQuery}); {@link #locked} is
     * what tells the player about the rest.
     */
    public static List<StasisBrowseRow> filter(List<StasisBrowseRow> rows, String query) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        if (query == null || query.trim().isEmpty()) {
            return List.copyOf(rows);
        }
        List<HorseListing> askable = new ArrayList<>();
        for (StasisBrowseRow row : rows) {
            if (row.searchable()) {
                askable.add(row.listing());
            }
        }
        Set<UUID> kept = new HashSet<>();
        for (HorseListing listing : HorseQuery.filter(askable, query)) {
            kept.add(listing.id());
        }
        List<String> words = plainNameQuery(query);
        List<StasisBrowseRow> out = new ArrayList<>();
        for (StasisBrowseRow row : rows) {
            if (row.searchable() ? kept.contains(row.listing().id())
                    : row.nameOnly() && words != null && row.nameMatches(words)) {
                out.add(row);
            }
        }
        return List.copyOf(out);
    }

    /** How many of {@code rows} no query can ever reach. Zero is the quiet case. */
    public static int locked(List<StasisBrowseRow> rows) {
        return locked(rows, "");
    }

    /**
     * How many of {@code rows} <i>this</i> query could not reach. A plain name
     * query reaches every Basic chamber - whether it matched is
     * {@link #filter}'s business, not a lock - so only a horse with no papers is
     * left out by one; any other query leaves both out, as it always has.
     */
    public static int locked(List<StasisBrowseRow> rows, String query) {
        boolean byName = plainNameQuery(query) != null;
        int n = 0;
        for (StasisBrowseRow row : rows) {
            if (!row.searchable() && !(byName && row.nameOnly())) {
                n++;
            }
        }
        return n;
    }

    /**
     * <b>A chamber the bank cannot look into, but whose name it can read</b> -
     * a tier below {@link StasisTier#searchable()}, with or without papers. The
     * name is on the row and on the bottle's tooltip already; searching by it
     * tells the player nothing the bank was hiding. (Owner, 2026-10-01.) A
     * no-papers horse in a searchable chamber is not this: its chamber is not
     * the reason it cannot be asked about.
     */
    private boolean nameOnly() {
        return tier != null && !tier.searchable();
    }

    /** Every word is in the name, and no negated word is - case folded, as a bare word is. */
    private boolean nameMatches(List<String> words) {
        String name = displayName().toLowerCase(Locale.ROOT);
        for (String word : words) {
            boolean negate = word.startsWith("-");
            String body = (negate ? word.substring(1) : word).toLowerCase(Locale.ROOT);
            if (name.contains(body) == negate) {
                return false;
            }
        }
        return true;
    }

    /**
     * <b>The query's words, if it is only a name; else null.</b> Plain means every
     * token {@link QueryExpr#tokenize} makes is a bare word, optionally led by
     * {@code -}: no key, operator, bracket, comma or quote, no keyword
     * ({@code AND}, {@code OR}, {@code NOT}, {@code IN}, {@code LIKE}), and no
     * word from {@link HorseQuery#flags()}. So {@code cinder} and
     * {@code cind -ash} are names, and {@code cinder mare}, {@code cinder gen>2}
     * and {@code name:cinder} are questions a Basic chamber cannot answer - a name
     * in a mixed query does not pull the horse in on its own. Read off the
     * language's own tokenizer and flag list, so a new flag or operator can never
     * quietly become part of a name.
     */
    static List<String> plainNameQuery(String query) {
        List<String> tokens = QueryExpr.tokenize(query);
        if (tokens.isEmpty()) {
            return null;
        }
        List<String> flags = HorseQuery.flags();
        for (String token : tokens) {
            for (int i = 0; i < token.length(); i++) {
                if ("<>=:!(),'\"".indexOf(token.charAt(i)) >= 0) {
                    return null;
                }
            }
            String body = token.startsWith("-") ? token.substring(1) : token;
            String folded = body.toLowerCase(Locale.ROOT);
            if (body.isEmpty() || body.startsWith("-") || PLAIN_KEYWORDS.contains(folded)
                    || flags.contains(folded)) {
                return null;
            }
        }
        return tokens;
    }

    /** {@link QueryExpr}'s keywords, which are grammar and never a name. */
    private static final List<String> PLAIN_KEYWORDS = List.of("and", "or", "not", "in", "like");
}
