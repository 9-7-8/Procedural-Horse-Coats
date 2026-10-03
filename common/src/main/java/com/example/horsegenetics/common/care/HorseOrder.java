package com.example.horsegenetics.common.care;

import java.util.Locale;

/**
 * <b>A standing order a player gave one of their horses with the command whistle.</b>
 * It lasts until it is changed, cleared by a recall, or the horse leaves the player's
 * keeping (see {@link HorseOrders}).
 *
 * <p><b>Append only, and saved by {@link #name()}, never by ordinal</b>: a world keeps
 * the name, and {@link #byName} reads a name it does not know as {@link #REJOIN_HERD}, so
 * a removed order loads as "no order" rather than as a crash or a migration.
 *
 * <p>Piece 1 of the command whistle shipped the first four, Piece 3 the two combat
 * orders, Piece 2 Graze nearby, Guard here and Go home.
 */
public enum HorseOrder {

    /** No order: the horse lives its ordinary life. The default, and what every recall restores. */
    REJOIN_HERD("Rejoin herd", 0, false, false),
    /** Stand where it was told, and walk back after anything that moved it. */
    STAY("Stay", 1, true, false),
    /** Follow the player who gave the order, at a walk. */
    FOLLOW("Follow", 1, false, false),
    /** Roam freely and ignore its owner and its herd. */
    WANDER("Wander", 2, false, false),
    /**
     * Hold the spot it was given, and go for any monster within the hunt radius of it -
     * never further - then walk back. Needs a horse bred to fight.
     */
    HUNT_MONSTERS("Hunt monsters", 2, true, true),
    /**
     * Follow the player who gave it, and go for any monster within the defend radius of
     * them, then come back to heel. Needs a horse bred to fight.
     */
    DEFEND_ME("Defend me", 2, false, true),
    /**
     * Roam and graze as it likes, but never further than the graze radius from the spot
     * it was given: past that it walks back in.
     */
    GRAZE_NEARBY("Graze nearby", 2, true, false),
    /**
     * Stand at the spot it was given, and go for any monster within the guard radius of
     * it - Hunt monsters with a short reach. Needs a horse bred to fight.
     */
    GUARD_HERE("Guard here", 2, true, true),
    /**
     * Go to its stall (or the holding pen), by the Send home trip. A one-shot: it is
     * carried out when given and never stands, so a horse never holds it.
     */
    GO_HOME("Go home", 2, false, false);

    private final String label;
    private final int bondTier;
    private final boolean anchored;
    private final boolean combat;

    HorseOrder(String label, int bondTier, boolean anchored, boolean combat) {
        this.label = label;
        this.bondTier = bondTier;
        this.anchored = anchored;
        this.combat = combat;
    }

    /** The name a player reads: on the wheel, in a refusal, on a horse's screens. */
    public String label() {
        return label;
    }

    /**
     * The bond tier this order needs (0-3; the horse-care tiers: 1 is bond 31+, 2 is 61+).
     * Rejoin herd needs none, because clearing an order is always allowed.
     */
    public int bondTier() {
        return bondTier;
    }

    /** Does this order hold the horse to the spot where it was given? */
    public boolean anchored() {
        return anchored;
    }

    /**
     * Does this order send the horse after monsters? Only a horse bred to fight may take
     * one ({@link HorseOrders#fighter}).
     */
    public boolean combat() {
        return combat;
    }

    /**
     * Does this order hold the horse STILL at its spot, walking it back after anything
     * that moved it? Graze nearby is anchored too, but roams inside its radius.
     */
    public boolean stands() {
        return this == STAY || this == HUNT_MONSTERS || this == GUARD_HERE;
    }

    /** Is this order carried out the moment it is given, and never left standing on the horse? */
    public boolean oneShot() {
        return this == GO_HOME;
    }

    /** Does this order keep the horse at the heels of the player who gave it? */
    public boolean follows() {
        return this == FOLLOW || this == DEFEND_ME;
    }

    /** A saved name back to its order; anything unknown, blank or null is {@link #REJOIN_HERD}. */
    public static HorseOrder byName(String name) {
        if (name == null || name.isBlank()) {
            return REJOIN_HERD;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return REJOIN_HERD;
        }
    }
}
