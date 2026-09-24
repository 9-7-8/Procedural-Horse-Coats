package com.example.horsegenetics.common.horse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
 * anything - it never matches a query, and {@link #locked} counts the rows a
 * query had to leave out so the player is told rather than left wondering where
 * a horse went.
 *
 * <h2>Why the listing may be missing</h2>
 * {@link #listing} is the same {@link HorseListing} the browser's <i>My
 * horses</i> table sorts, looked up by the id the chamber remembers. It is
 * absent when the stable's records have nothing under that id - a chamber
 * somebody else filled and handed over, or a stable so large the roster was cut
 * short. That row keeps its name and says it has no papers rather than
 * pretending the chamber is empty.
 *
 * <p>Pure Java, and the reason the Browse tab has almost no logic of its own:
 * the gating and the filtering are testable without a game, which is where
 * {@code StasisBrowseTest} works.
 */
public record StasisBrowseRow(String name, StasisTier tier, HorseListing listing) {

    public StasisBrowseRow {
        name = name == null ? "" : name;
    }

    /** May a query reach this row? Both halves are needed: a tier and a record. */
    public boolean searchable() {
        return tier != null && tier.searchable() && listing != null;
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
     * shows only the rows it can actually reach - so a Basic chamber and a horse
     * with no papers drop out the moment anything is typed, and
     * {@link #locked} is what tells the player so.
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
        List<StasisBrowseRow> out = new ArrayList<>();
        for (StasisBrowseRow row : rows) {
            if (row.searchable() && kept.contains(row.listing().id())) {
                out.add(row);
            }
        }
        return List.copyOf(out);
    }

    /** How many of {@code rows} no query can ever reach. Zero is the quiet case. */
    public static int locked(List<StasisBrowseRow> rows) {
        int n = 0;
        for (StasisBrowseRow row : rows) {
            if (!row.searchable()) {
                n++;
            }
        }
        return n;
    }
}
