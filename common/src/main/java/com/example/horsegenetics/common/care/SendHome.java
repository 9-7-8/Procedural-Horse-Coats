package com.example.horsegenetics.common.care;

import java.util.List;

/**
 * <b>What the horse browser's <i>Send home</i> button costs, and what it says.</b>
 *
 * <p>Free by default - the owner's call (2026-10-01): the button sends a horse
 * to its own stall, or the holding pen when it has none, from anywhere and out
 * of a stasis chamber too, with nothing spent. A server may put a price on it:
 * <b>one item and a count per trip</b>, which then replaces tickets for the
 * button entirely. Tickets and whistles stay the way to move a horse from the
 * world with no menu, and every line that mentions a price says so.
 *
 * <p>There is also a short per-player cooldown, started only by a trip that
 * actually happened, so a mashed button sends one horse rather than queueing
 * chunk loads.
 *
 * <p>Pure, and in {@code common/} so the rules and the wording can be pinned by
 * a unit test. The NeoForge side ({@code server/StallRecall}) resolves the item
 * id against the registry, counts the player's pack, keeps the cooldown clock
 * and sends the lines.
 */
public final class SendHome {

    /** The default cooldown between two sends by one player, in seconds. */
    public static final int DEFAULT_COOLDOWN_SECONDS = 5;

    private static final int TICKS_PER_SECOND = 20;

    private SendHome() {
    }

    /**
     * <b>The price of one trip</b>, as the server config names it: a registry
     * item id and how many of it. {@link #FREE} when either half is missing.
     *
     * @param itemId a namespaced item id such as {@code minecraft:emerald}, or
     *               blank for free. Not checked against any registry here -
     *               the caller decides what an unknown id means (free, with a
     *               warning in the log)
     * @param count  items taken per successful trip; 0 or less is free
     */
    public record Price(String itemId, int count) {

        public static final Price FREE = new Price("", 0);

        public Price {
            itemId = itemId == null ? "" : itemId.trim();
        }

        /** The config's two values, normalised so a half-set price is free. */
        public static Price of(String itemId, int count) {
            Price price = new Price(itemId, count);
            return price.free() ? FREE : price;
        }

        public boolean free() {
            return itemId.isEmpty() || count <= 0;
        }
    }

    // ------------------------------------------------------------------
    // The cooldown
    // ------------------------------------------------------------------

    /**
     * <b>How many ticks are left before this player may send again</b>, or 0.
     *
     * @param lastSendTick the server tick of their last successful send, or a
     *                     negative number when they have never sent one
     * @param nowTick      the current server tick
     * @param seconds      the configured cooldown; 0 or less turns it off
     */
    public static long cooldownLeft(long lastSendTick, long nowTick, int seconds) {
        if (seconds <= 0 || lastSendTick < 0) {
            return 0;
        }
        long left = lastSendTick + (long) seconds * TICKS_PER_SECOND - nowTick;
        return Math.max(0, left);
    }

    /** The refusal while the cooldown runs, rounded up to whole seconds. */
    public static String cooldownLine(long ticksLeft) {
        long seconds = Math.max(1, (ticksLeft + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
        return "Wait " + seconds + (seconds == 1 ? " more second" : " more seconds")
                + " before sending another horse home.";
    }

    // ------------------------------------------------------------------
    // The wording
    // ------------------------------------------------------------------

    /**
     * <b>The one sentence that points at the other ways home.</b> The names
     * come from the live item roster, so a renamed item is renamed here too.
     * Empty when there is nothing to name.
     */
    public static String alternativesLine(List<String> itemNames) {
        if (itemNames == null || itemNames.isEmpty()) {
            return "";
        }
        return "Tickets and whistles can also bring a horse home (" + String.join(", ", itemNames) + ").";
    }

    /** {@code "2 x Emerald"} - the price as every line writes it. */
    public static String priceText(Price price, String itemName) {
        return price.count() + " x " + itemName;
    }

    /** The button's own label. */
    public static String buttonLabel(Price price, String itemName) {
        return price.free() ? "Send home" : "Send home (" + priceText(price, itemName) + ")";
    }

    /**
     * The button's tooltip. A free button needs no pointer elsewhere; a priced
     * one names the price and then the alternatives, so a server owner who sets
     * a price is still pointing players at the items that do it for nothing.
     */
    public static String tooltip(Price price, String itemName, boolean inStasis, String alternatives) {
        String what = inStasis
                ? "take this horse out of its stasis chamber and put it in its stall. "
                        + "The empty chamber is left where it was."
                : "send this horse to its stall - or to your holding pen, if it has no stall of its own.";
        if (price.free()) {
            return Character.toUpperCase(what.charAt(0)) + what.substring(1);
        }
        return withAlternatives("Pay " + priceText(price, itemName) + " to " + what, alternatives);
    }

    /** The refusal when a price is set and the player cannot pay it. Nothing is spent. */
    public static String cannotPayLine(Price price, String itemName, String alternatives) {
        return withAlternatives("Sending a horse home costs " + priceText(price, itemName)
                + ", and you do not have " + (price.count() == 1 ? "it." : "them."), alternatives);
    }

    private static String withAlternatives(String line, String alternatives) {
        return alternatives == null || alternatives.isEmpty() ? line : line + " " + alternatives;
    }
}
