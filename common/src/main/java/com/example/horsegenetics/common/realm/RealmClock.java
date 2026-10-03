package com.example.horsegenetics.common.realm;

/**
 * <b>How much of the time that passed counts toward a realm horse's breeding.</b>
 *
 * <p>Nothing in {@code common.repro} counts down: every stage is derived from an
 * absolute tick and "now". So the realm does not scale timers; the NeoForge side
 * ({@code server/HorseRealmRepro}) keeps, per horse, an offset of ticks that its
 * reproduction did not live through, and hands {@code common.repro} game time
 * minus that offset. This class is the rule for how big each addition to the
 * offset is. The game side only measures the gap and stores the result.
 *
 * <p>Two settings, both server config, carried in {@link Pace}:
 * <ul>
 *   <li><b>The rate</b> ({@code realm.breeding_rate_percent}), credited for time
 *       the horse was loaded and ticking. 100 is normal speed (owner, 2026-10-02:
 *       "breeding should advance at normal speed"); 25 makes every stage four
 *       times as long; 0 stops reproduction there outright.</li>
 *   <li><b>The pause</b> ({@code realm.pause_when_unloaded}), for time it was
 *       not: a gap since the last scan longer than {@link #DORMANT_AFTER_SCANS}
 *       scans means nobody was near enough to keep the horse loaded, and on by
 *       default none of that gap counts (owner, 2026-10-02: a mare nobody is near
 *       "does not progress, and does not catch up when somebody arrives"). Off,
 *       the gap is credited at the rate as if she had been watched - at 100, plain
 *       game time.</li>
 * </ul>
 *
 * <p>The judgement is per horse, because each horse measures its own gap: a mare
 * in a chunk nobody has loaded is paused even while a player stands on the other
 * side of the realm.
 */
public final class RealmClock {

    /** {@code realm.breeding_rate_percent}'s default: normal speed while loaded. */
    public static final int DEFAULT_RATE_PERCENT = 100;

    /** {@code realm.pause_when_unloaded}'s default: on. */
    public static final boolean DEFAULT_PAUSE_WHEN_UNLOADED = true;

    /**
     * How many of a horse's own scan periods may pass before the gap reads as
     * "it was not being simulated" rather than "it was simulated slowly". Three,
     * because one missed scan is ordinary lag, and an unloaded horse is away for
     * minutes or days, never for a few seconds.
     */
    public static final int DORMANT_AFTER_SCANS = 3;

    private RealmClock() {
    }

    /** The realm's two settings, as one value; built only from the server config. */
    public record Pace(int ratePercent, boolean pauseWhenUnloaded) {
        public Pace {
            if (ratePercent < 0 || ratePercent > 100) {
                throw new IllegalArgumentException("rate must be 0-100: " + ratePercent);
            }
        }

        /** Reproduction in the realm is stopped outright, loaded or not. */
        public boolean stopped() {
            return ratePercent == 0;
        }
    }

    /** A gap of this many ticks since the horse's last scan means it was unloaded. */
    public static boolean dormant(long gap, long scanTicks) {
        return gap > scanTicks * DORMANT_AFTER_SCANS;
    }

    /**
     * How many ticks of a {@code gap} since this horse's last scan its
     * reproduction does NOT live through - the amount to add to its offset.
     * Never negative and never more than the gap.
     */
    public static long deferred(long gap, long scanTicks, Pace pace) {
        if (gap <= 0L) {
            return 0L;
        }
        if (pace.pauseWhenUnloaded() && dormant(gap, scanTicks)) {
            return gap;
        }
        long credited = Math.round(gap * (pace.ratePercent() / 100.0));
        return gap - credited;
    }

    /**
     * What a realm mare's breeding line adds, so a still clock never looks stuck:
     * "paused in the realm" when the rate is 0, otherwise a reminder that it
     * stops while nobody is near. Empty when neither applies.
     */
    public static String note(Pace pace) {
        if (pace.stopped()) {
            return "paused in the realm";
        }
        return pace.pauseWhenUnloaded() ? "stops while nobody is near" : "";
    }

    /** {@code line} with {@link #note} in brackets after it; an empty line stays empty. */
    public static String withNote(String line, Pace pace) {
        String note = note(pace);
        return line.isEmpty() || note.isEmpty() ? line : line + " (" + note + ")";
    }
}
