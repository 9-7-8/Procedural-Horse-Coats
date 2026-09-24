package com.example.horsegenetics.common.horse;

/**
 * <b>The four grades of stasis chamber</b>, and the one thing each one adds.
 *
 * <p>A stasis chamber holds a horse as data instead of as a ticking entity. The
 * tier does not change <i>that</i> - every tier stores a horse, and every tier
 * stores it for free - it changes what the <b>Horse Stasis Bank</b> is allowed
 * to do with the horse while it is in there. That is the whole ladder, and the
 * reason the recipes climb the way they do: the bottom rung has to stay cheap
 * enough that somebody with two hundred horses actually shelves them, and each
 * rung above it is paying for a capability, not for storage.
 *
 * <p><b>The flags are cumulative and that is enforced</b>, not merely intended -
 * see {@code StasisTierTest}. A tier that unlocked drop collection but silently
 * dropped searchability would be a ladder with a hole in it, and the bank's UI
 * reads these flags rather than switching on the tier by name.
 *
 * <p>This lives in {@code common/} because it is the one part of the stasis
 * system with no Minecraft in it. The snapshot itself cannot follow it here: a
 * capture is an entity's saved NBT, which is a Minecraft type, so
 * {@code neoforge/data/StasisSnapshot} stays in the game module. See
 * {@code wiki/horse-stasis.html}.
 */
public enum StasisTier {

    /** Storage and nothing else. The cheap rung, and the point of the feature. */
    BASIC("basic", false, false, false, false,
            "Holds one horse. The bank cannot look inside."),

    /**
     * The bank can see the horse: everything about it becomes searchable in the
     * Browse tab, and the bank can mend it from its feed and water.
     *
     * <p><b>Those two are one capability</b>, which is why one rung buys both.
     * Basic's contract is that the bank cannot look inside, and a bank that
     * cannot look inside cannot know that the horse is hurt, what it eats, or
     * how much of its bar is missing. Healing is that same window used for
     * something other than a list.
     */
    INTERMEDIATE("intermediate", true, true, false, false,
            "Holds one horse. The bank can search it, and heal it from its feed and water."),

    /** The bank passively collects what the horse would have dropped. */
    ADVANCED("advanced", true, true, true, false,
            "Searchable and healed, and the bank collects this horse's drops."),

    /** The horse can be bred without ever leaving the bank. */
    SPACER("spacer", true, true, true, true,
            "Searchable, healed, collects drops, and can be bred inside the bank.");

    private final String id;
    private final boolean searchable;
    private final boolean heals;
    private final boolean collectsDrops;
    private final boolean breedsInBank;
    private final String what;

    StasisTier(String id, boolean searchable, boolean heals, boolean collectsDrops,
               boolean breedsInBank, String what) {
        this.id = id;
        this.searchable = searchable;
        this.heals = heals;
        this.collectsDrops = collectsDrops;
        this.breedsInBank = breedsInBank;
        this.what = what;
    }

    /** The stable string form - what a registry name and a saved file both use. */
    public String id() {
        return id;
    }

    /** May the bank's Browse tab show and search this horse's details? */
    public boolean searchable() {
        return searchable;
    }

    /**
     * <b>Does the bank mend this horse while it is shelved?</b>
     *
     * <p>Also the tick gate: a bank holding not one chamber that answers this
     * does no upkeep work at all, however full of Basic chambers it is. See
     * {@code HorseStasisBankBlockEntity}.
     */
    public boolean heals() {
        return heals;
    }

    /** Does the bank collect this horse's milk and drops into its buffer? */
    public boolean collectsDrops() {
        return collectsDrops;
    }

    /** May this horse be bred while still inside the bank, against another Spacer? */
    public boolean breedsInBank() {
        return breedsInBank;
    }

    /** One line for a tooltip. English here, like {@code TicketItem.Tier}. */
    public String what() {
        return what;
    }

    /** The tier one rung up, or {@code null} at the top. What an upgrade recipe produces. */
    public StasisTier next() {
        StasisTier[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }

    /** The tier with this {@link #id()}, or {@code null} if nothing has it. */
    public static StasisTier byId(String id) {
        for (StasisTier tier : values()) {
            if (tier.id.equals(id)) {
                return tier;
            }
        }
        return null;
    }
}
