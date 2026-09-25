package com.example.horsegenetics.common.log;

import com.example.horsegenetics.common.care.RescuingBraid;
import com.example.horsegenetics.common.horse.StasisRescue;
import com.example.horsegenetics.common.repro.CoverNotice;
import com.example.horsegenetics.common.repro.NaturalCover;

import java.util.UUID;

/**
 * <b>One thing that happened to one of your horses.</b> A row of the browser's
 * Log tab.
 *
 * <p>The Log tab exists because <i>most of what happens to a stable happens
 * while nobody is looking at it</i>. A mare is covered in a paddock two hundred
 * blocks away; a foal is born overnight; a horse is killed by a wolf in a
 * dimension the owner is not in; a cover is refused every heat for a reason the
 * chat line scrolled past four hours ago. Chat is an <i>alarm</i> - it reaches
 * whoever is standing there, once - and an alarm nobody heard is not a record.
 * This is the record.
 *
 * <h2>Why the whole row is in {@code common/}</h2>
 * Because the <b>wording is the feature</b>, exactly as it is for
 * {@link CoverNotice}: what a row says is the thing a player reads and the
 * thing a test can pin down, and neither needs a level, an entity or a
 * {@code Component} to decide. The game module resolves who owns what and hands
 * this plain data; {@link #line()} turns it back into a sentence.
 *
 * <h2>Why a failed cover carries a {@link CoverNotice.Reason} and not a string</h2>
 * That vocabulary already exists and is already the set of things a natural
 * cover can end in. A second list of strings beside it is a list that drifts:
 * add a refusal to {@code NaturalCover} and the log quietly keeps showing the
 * eight it knew about. Carrying the constant means a new refusal is a new row
 * kind for free, and means the log row and the chat line are <b>the same
 * sentence</b> - which is the point, since the log is where you look when you
 * missed the chat line.
 *
 * @param kind      what happened
 * @param at        the overworld game tick it happened on - monotonic, persisted,
 *                  and what {@link #when()} reads a day and a clock off
 * @param horseId   the horse this is about; the <i>foal</i> for a birth, the
 *                  <i>mare</i> for a cover
 * @param horseName that horse's display name as it read at the time, because a
 *                  horse that is dead or sold cannot be asked later
 * @param other     the other party, kind-dependent and often empty: the parents
 *                  of a foal, what killed a horse, who a horse came from or
 *                  went to, or where the chamber that rescued one was filed
 * @param reason    for {@link Kind#COVER} only, how it ended; {@code null} on
 *                  every other kind
 * @param nearby    for {@link CoverNotice.Reason#CROWDED} only, the horses around
 *                  her and what the world allowed - kept so the row can print the
 *                  same numbers the chat line did
 * @param cap       see {@code nearby}
 */
public record HorseEvent(Kind kind, long at, UUID horseId, String horseName,
                         String other, CoverNotice.Reason reason, int nearby, int cap) {

    /**
     * <b>What happened</b>, and the word the filter chip carries.
     *
     * <p>Seven kinds were the seven the plan named: a cover, a birth, a death, a
     * taming, and the three ways a horse changes hands. Ownership is three kinds
     * rather than one because they are three different events to the person
     * reading - buying from a dealer, being handed a horse, and losing one are
     * not interchangeable - and because a filter that cannot separate "what did
     * I buy" from "what did I lose" is not a filter.
     *
     * <p>{@link #RESCUE} and {@link #HOMED} are the eighth and ninth, and they
     * are here for the reason the whole tab is: both an emergency stasis chamber
     * and a rescuing braid fire <i>by themselves</i>, possibly a thousand blocks
     * from the player and possibly while they are reading something else. The
     * chat line is an alarm that scrolls away; these are the record that a horse
     * of theirs nearly died and what paid for it.
     *
     * <p>Two kinds rather than one, though they are both "something saved a
     * horse", because they leave the player in entirely different places: a
     * rescued horse is in a bottle they now have to find and open, and a homed
     * one is standing in its stall. A filter that could not separate <i>what do
     * I have to go and let out</i> from <i>what walked home by itself</i> would
     * be the same failure the three ownership kinds exist to avoid.
     */
    public enum Kind {
        COVER("Cover", "Covers"),
        BIRTH("Birth", "Births"),
        DEATH("Death", "Deaths"),
        TAMED("Tamed", "Taming"),
        PURCHASE("Bought", "Purchases"),
        SALE("Sold", "Sales"),
        TRANSFER("Transferred", "Transfers"),
        RESCUE("Rescued", "Rescues"),
        HOMED("Sent home", "Homings");

        private final String label;
        private final String plural;

        Kind(String label, String plural) {
            this.label = label;
            this.plural = plural;
        }

        /** The word in the row's type column. */
        public String label() {
            return label;
        }

        /** The word on the filter chip. */
        public String plural() {
            return plural;
        }
    }

    public HorseEvent {
        horseName = horseName == null ? "" : horseName;
        other = other == null ? "" : other;
    }

    // --- the ways one gets made -------------------------------------------
    //
    // Factories rather than a public constructor with eight arguments, because
    // seven of the eight are empty on any given kind and a call site that reads
    // `new HorseEvent(BIRTH, now, id, name, parents, null, 0, 0)` says nothing
    // about which of those zeros mean anything.

    /** A natural cover that went through, or did not, and why. */
    public static HorseEvent cover(long at, UUID mare, String mareName,
                                   CoverNotice.Reason reason, NaturalCover.Crowd crowd) {
        return new HorseEvent(Kind.COVER, at, mare, mareName, "", reason,
                crowd == null ? 0 : crowd.nearby(), crowd == null ? 0 : crowd.cap());
    }

    /** A foal on the ground. {@code parents} reads "Dam x Sire". */
    public static HorseEvent birth(long at, UUID foal, String foalName, String parents) {
        return new HorseEvent(Kind.BIRTH, at, foal, foalName, parents, null, 0, 0);
    }

    /** {@code cause} is what killed it, in words, or empty when nothing is known. */
    public static HorseEvent death(long at, UUID horse, String horseName, String cause) {
        return new HorseEvent(Kind.DEATH, at, horse, horseName, cause, null, 0, 0);
    }

    public static HorseEvent tamed(long at, UUID horse, String horseName) {
        return new HorseEvent(Kind.TAMED, at, horse, horseName, "", null, 0, 0);
    }

    /** Bought off a dealer with a transfer paper. {@code from} is the dealer. */
    public static HorseEvent purchase(long at, UUID horse, String horseName, String from) {
        return new HorseEvent(Kind.PURCHASE, at, horse, horseName, from, null, 0, 0);
    }

    /** Gone: somebody else redeemed a paper for it. {@code to} is who now has it. */
    public static HorseEvent sale(long at, UUID horse, String horseName, String to) {
        return new HorseEvent(Kind.SALE, at, horse, horseName, to, null, 0, 0);
    }

    /** Taken on from another player. {@code from} is who had it. */
    public static HorseEvent transfer(long at, UUID horse, String horseName, String from) {
        return new HorseEvent(Kind.TRANSFER, at, horse, horseName, from, null, 0, 0);
    }

    /**
     * An emergency stasis chamber caught this horse out of a blow that would
     * have killed it.
     *
     * @param inBank whether the chamber that took it was filed in a stasis bank
     *               rather than carried - which is the whole of what {@code other}
     *               says, and the only thing the two sentences differ on
     */
    public static HorseEvent rescued(long at, UUID horse, String horseName, boolean inBank) {
        return new HorseEvent(Kind.RESCUE, at, horse, horseName,
                inBank ? StasisRescue.FROM_BANK : "", null, 0, 0);
    }

    /**
     * A rescuing braid broke and put this horse back where it lives.
     *
     * @param destination where it landed, in words -
     *                    {@link RescuingBraid#ITS_STALL} or
     *                    {@link RescuingBraid#THE_HOLDING_PEN}
     */
    public static HorseEvent homed(long at, UUID horse, String horseName, String destination) {
        return new HorseEvent(Kind.HOMED, at, horse, horseName, destination, null, 0, 0);
    }

    // --- reading one ------------------------------------------------------

    /**
     * <b>The sentence in the row.</b>
     *
     * <p>A cover defers to {@link CoverNotice#line} rather than writing its own:
     * the log row and the chat line the owner may have missed are then literally
     * the same words, and there is one place to change them.
     *
     * <p>Second person for the things the reader did, third for the things that
     * happened to them. The log is one player's own, so "You bought" is both
     * shorter and truer than naming them in their own list.
     */
    public String line() {
        String name = horseName.isEmpty() ? "A horse" : horseName;
        switch (kind) {
            case COVER:
                return CoverNotice.line(name, reason, new NaturalCover.Crowd(nearby, cap));
            case BIRTH:
                return other.isEmpty()
                        ? name + " was born."
                        : name + " was born, out of " + other + ".";
            case DEATH:
                return other.isEmpty()
                        ? name + " died."
                        : name + " died: " + other + ".";
            case TAMED:
                return "You tamed " + name + ".";
            case RESCUE:
                // Deferred to StasisRescue for CoverNotice's reason: this row is
                // read by somebody who missed the chat line, and a row that
                // worded it differently would leave them wondering whether they
                // were two separate pieces of news.
                return other.isEmpty()
                        ? StasisRescue.saved(name)
                        : StasisRescue.savedInBank(name);
            case HOMED:
                return RescuingBraid.saved(name, other.isEmpty() ? RescuingBraid.ITS_STALL : other);
            case PURCHASE:
                return other.isEmpty()
                        ? "You bought " + name + "."
                        : "You bought " + name + " from " + other + ".";
            case SALE:
                return other.isEmpty()
                        ? name + " is no longer yours."
                        : name + " passed to " + other + ".";
            case TRANSFER:
            default:
                return other.isEmpty()
                        ? "You took on " + name + "."
                        : "You took on " + name + ", from " + other + ".";
        }
    }

    /**
     * <b>{@link #line()} with the leading horse name taken off</b>, for a table
     * that already has the name in a column of its own.
     *
     * <p>A mechanical trim of the real sentence rather than a second set of
     * phrasings. That distinction is the whole point: a parallel list of
     * shortened wordings is a list that drifts from {@link CoverNotice}'s the
     * first time somebody edits one and not the other, and this cannot, because
     * there is still only one sentence. A line that does not begin with the name
     * - "It didn't take - Mare was covered..." - is left whole, which reads
     * correctly in the column anyway.
     */
    public String detail() {
        String line = line();
        String name = horseName.isEmpty() ? "A horse" : horseName;
        if (!line.startsWith(name) || line.length() <= name.length()) {
            return line;
        }
        return line.substring(name.length()).trim();
    }

    /**
     * <b>Day and clock, the way the game counts them.</b> Tick zero of a day is
     * six in the morning, which is why the hour is offset - a foal born at
     * "Day 3, 00:30" was born in the small hours of the third night and not half
     * an hour after dawn.
     */
    public String when() {
        return when(at);
    }

    /** See {@link #when()}. Static so a test can ask without building a row. */
    public static String when(long tick) {
        long day = Math.floorDiv(tick, 24_000L);
        long inDay = Math.floorMod(tick, 24_000L);
        long hour = (inDay / 1_000L + 6L) % 24L;
        long minute = (inDay % 1_000L) * 60L / 1_000L;
        return "Day " + day + ", " + two(hour) + ":" + two(minute);
    }

    private static String two(long n) {
        return n < 10 ? "0" + n : Long.toString(n);
    }

    /** Worth reading in green: something went right. */
    public boolean good() {
        switch (kind) {
            case BIRTH:
            case TAMED:
            case PURCHASE:
            case TRANSFER:
            // A rescue is a near-miss, and it reads green on purpose: the horse
            // is alive and safe, which is the outcome, and the alternative -
            // amber for "something nearly went wrong" - is a colour the tab does
            // not have and a distinction nobody asked for.
            case RESCUE:
            case HOMED:
                return true;
            case COVER:
                return reason != null && reason.good();
            default:
                return false;
        }
    }

    /** Worth reading in red: something was lost. A refused cover is neither. */
    public boolean bad() {
        return kind == Kind.DEATH;
    }

    /**
     * <b>Are these the same happening?</b> Used by {@link HorseEventLog} to drop
     * a repeat - see the dedup note there. Deliberately ignores {@link #at},
     * {@link #horseName} and {@link #other}: a mare refused for the same reason
     * two seconds later is the same refusal even though the tick moved, and a
     * horse renamed between two reports of one death is still one death.
     */
    public boolean sameHappening(HorseEvent other) {
        return other != null
                && kind == other.kind
                && reason == other.reason
                && (horseId == null ? other.horseId == null : horseId.equals(other.horseId));
    }
}
