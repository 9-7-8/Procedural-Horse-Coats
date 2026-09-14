package com.example.horsegenetics.common.repro;

/**
 * <b>How long each reproductive stage lasts, in ticks</b>, all derived from one
 * setting: the gestation length.
 *
 * <p>Squeezing a real 340-day pregnancy into a Minecraft day turns a 5-day heat
 * into 18 seconds - accurate and unplayable. So every other stage is scaled
 * from gestation by its real-world ratio and then <b>floored at one day</b>:
 *
 * <pre>stage = max(day, gestation x referenceDays / 340)</pre>
 *
 * <p>Only <i>independent</i> stages are floored. The cycle is estrus plus
 * diestrus, not a third floored number, and peak fertility is a window inside
 * estrus rather than a stage after it - flooring either would double-count
 * days. Foal heat reuses the estrus length.
 *
 * <p>{@code dayTicks} is a parameter rather than a constant so the testing tools
 * can shrink the whole calendar at once; see {@code ServerConfig}.
 */
public final class ReproTiming {

    /** One Minecraft day. */
    public static final long DAY_TICKS = 24_000L;

    public static final double DEFAULT_GESTATION_DAYS = 1.0;

    public static final double REFERENCE_GESTATION_DAYS = 340.0;
    public static final double REFERENCE_ESTRUS_DAYS = 5.0;
    public static final double REFERENCE_DIESTRUS_DAYS = 16.0;
    /** Foal heat begins about a week after birth. */
    public static final double REFERENCE_FOAL_HEAT_DELAY_DAYS = 7.0;

    public static final ReproTiming STANDARD = of(DEFAULT_GESTATION_DAYS, DAY_TICKS);

    private final long dayTicks;
    private final long gestationTicks;
    private final long estrusTicks;
    private final long diestrusTicks;
    private final long postpartumTicks;

    private ReproTiming(double gestationDays, long dayTicks) {
        this.dayTicks = dayTicks;
        this.gestationTicks = Math.max(1L, Math.round(gestationDays * dayTicks));
        this.estrusTicks = stage(REFERENCE_ESTRUS_DAYS);
        this.diestrusTicks = stage(REFERENCE_DIESTRUS_DAYS);
        this.postpartumTicks = stage(REFERENCE_FOAL_HEAT_DELAY_DAYS);
    }

    public static ReproTiming of(double gestationDays, long dayTicks) {
        if (dayTicks <= 0) {
            throw new IllegalArgumentException("dayTicks must be positive, got " + dayTicks);
        }
        if (!(gestationDays > 0.0)) {
            throw new IllegalArgumentException("gestationDays must be positive, got " + gestationDays);
        }
        return new ReproTiming(gestationDays, dayTicks);
    }

    private long stage(double referenceDays) {
        long scaled = Math.round(gestationTicks * (referenceDays / REFERENCE_GESTATION_DAYS));
        return Math.max(dayTicks, scaled);
    }

    public long dayTicks() {
        return dayTicks;
    }

    public long gestationTicks() {
        return gestationTicks;
    }

    public long estrusTicks() {
        return estrusTicks;
    }

    public long diestrusTicks() {
        return diestrusTicks;
    }

    /** One full cycle - estrus then diestrus. Derived, never floored on its own. */
    public long cycleTicks() {
        return estrusTicks + diestrusTicks;
    }

    /** From birth until foal heat begins. */
    public long postpartumTicks() {
        return postpartumTicks;
    }

    /** Foal heat is an ordinary heat in length. */
    public long foalHeatTicks() {
        return estrusTicks;
    }

    @Override
    public String toString() {
        return "ReproTiming{day=" + dayTicks + ", gestation=" + gestationTicks + ", estrus=" + estrusTicks
                + ", diestrus=" + diestrusTicks + ", postpartum=" + postpartumTicks + "}";
    }
}
