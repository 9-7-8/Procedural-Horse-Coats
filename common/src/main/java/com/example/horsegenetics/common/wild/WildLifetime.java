package com.example.horsegenetics.common.wild;

/**
 * <b>How long a wild horse stays, and what becomes of it when it goes.</b>
 *
 * <p>Every horse that came from a natural spawn carries the game time it arrived
 * (the host stamps it). Once {@code wild.despawn_days} days of game time have
 * passed since that stamp it leaves - read off game time, so the days count
 * while its chunk was unloaded, and it goes the first time it is next loaded and
 * unwatched. A herd goes together: the host hands in the herd lead's stamp, not
 * the member's own.
 *
 * <p>Where it goes is {@link #fate}: nowhere while a player is within
 * {@link #WATCH_RADIUS} blocks (it never vanishes in front of anyone); to the
 * horse realm if a player ever <i>touched</i> it - leashed, fed, rode, tacked -
 * and the realm hand-off is on; otherwise it is simply removed. A tamed horse is
 * never asked: the host clears the stamp the moment a horse is tamed.
 *
 * <p>Owner, 2026-10-02 (the wild horse turnover treatment): three days, touched
 * horses to the realm, nothing goes while watched.
 */
public final class WildLifetime {

    /** {@code wild.despawn_days}'s default. */
    public static final int DEFAULT_DAYS = 3;

    /** {@code wild.despawn_days}'s upper bound; 0 is the lower, and switches the lifetime off. */
    public static final int MAX_DAYS = 30;

    /** {@code wild.realm_handoff}'s default. */
    public static final boolean DEFAULT_REALM_HANDOFF = true;

    /** A horse waits while any player is within this many blocks of it. */
    public static final double WATCH_RADIUS = 32.0;

    /** One Minecraft day of game time. */
    public static final long DAY_TICKS = 24_000L;

    private WildLifetime() {
    }

    /** Has its time come? */
    public enum Verdict { STAY, GO }

    /** What the host does with a horse this scan. */
    public enum Fate {
        /** Nothing: not due, or due but somebody is watching. */
        STAY,
        /** Removed, unseen. */
        REMOVE,
        /** Released into the horse realm, still wild, keeping everything that makes it that horse. */
        TO_REALM
    }

    /**
     * Is a horse stamped at {@code stampedAt} due to leave at {@code now}, with a
     * lifetime of {@code days}? Days of 0 or less switch the lifetime off.
     *
     * <p>A stamp in the <i>future</i> - game time does not run backwards in play,
     * but a world can be edited or restored - is never due; the host re-stamps it
     * ({@link #needsRestamp}) so it gets a fresh lifetime rather than an
     * immortal one.
     */
    public static Verdict judge(long stampedAt, long now, int days) {
        if (days <= 0 || now < stampedAt) {
            return Verdict.STAY;
        }
        return now - stampedAt >= days * DAY_TICKS ? Verdict.GO : Verdict.STAY;
    }

    /** A stamp later than now came from a clock that went backwards: start the horse over. */
    public static boolean needsRestamp(long stampedAt, long now) {
        return stampedAt > now;
    }

    /**
     * What happens to a horse this scan.
     *
     * @param verdict      {@link #judge} on the stamp that governs it (its herd lead's, if loaded)
     * @param watched      a player is within {@link #WATCH_RADIUS} blocks
     * @param touched      a player has ever leashed, fed, ridden or tacked it
     * @param realmHandoff {@code wild.realm_handoff}
     */
    public static Fate fate(Verdict verdict, boolean watched, boolean touched, boolean realmHandoff) {
        if (verdict != Verdict.GO || watched) {
            return Fate.STAY;
        }
        return touched && realmHandoff ? Fate.TO_REALM : Fate.REMOVE;
    }

    /** The debug line for a horse that left. */
    public static String leftLine(String who, String where, Fate fate, long ageTicks) {
        String age = String.format("%.1f", ageTicks / (double) DAY_TICKS);
        return switch (fate) {
            case REMOVE -> who + " at " + where + " moved on after " + age + " days (untouched, unwatched)";
            case TO_REALM -> who + " at " + where + " went to the horse realm after " + age
                    + " days (a player had worked with it)";
            case STAY -> who + " at " + where + " stays";
        };
    }
}
