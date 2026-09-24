package com.example.horsegenetics.common.care;

/**
 * <b>Bond forgets slowly</b> - the arithmetic of the daily decay, with no game
 * in it.
 *
 * <p>Owner's ask: <i>"Bond should decay down to a set level (above 0) very
 * slowly over time, at like 1 level per day."</i> Both numbers are the defaults
 * of server settings: {@code behaviour.bond_decay_per_day} and
 * {@code behaviour.bond_floor}.
 *
 * <h2>Why a floor, and why that one</h2>
 * {@link #DEFAULT_FLOOR} is the bottom of behaviour tier 1 - the tier at which a
 * horse turns its head to face its owner and does nothing else. So a horse you
 * once bonded and then left alone for a year still <em>looks up when you walk
 * past</em>, and that is all neglect can ever cost you: the tiers that walk
 * toward you and steer bareback are the ones you have to keep earning. Decaying
 * to zero would make a horse you raised from a foal exactly as wary of you as one
 * caught wild that morning, which is the same mistake foals starting at zero once
 * made, and the punishing husbandry {@code wiki/philosophy.html} rules out.
 * (Owner, 2026-09-24.)
 *
 * <h2>Why this is elapsed days rather than a tick rate</h2>
 * A horse spends most of its life in an unloaded chunk, so anything ticked is a
 * rate on <em>attention</em> rather than on time: the horse in your pocket
 * dimension would keep its bond forever and the one in the pasture you ride
 * through would lose it. Reading the number of whole Minecraft days since the
 * last stamp and charging for all of them at once makes an unload cost exactly
 * what being loaded costs, which is the only version of "over time" that means
 * anything. It is the same stamp the daily gain cap already rolls over on.
 *
 * <p>Never raises: a horse below the floor stays where it is rather than being
 * topped up to it. The floor is a place decay stops, not a level bond is held at.
 */
public final class Bond {

    private Bond() {
    }

    /**
     * <b>How much bond a horse loses per Minecraft day</b> - the owner's one
     * point. The default of {@code behaviour.bond_decay_per_day}; 0 turns decay
     * off entirely.
     *
     * <p>One point a day against a daily gain cap of fifteen is deliberately
     * trivial to out-earn. It is there so that bond is a thing you keep rather
     * than a thing you banked once, and a horse ridden even occasionally never
     * notices it.
     */
    public static final int DEFAULT_PER_DAY = 1;

    /**
     * <b>The level decay stops at</b> - 31, the bottom of behaviour tier 1. The
     * default of {@code behaviour.bond_floor}.
     */
    public static final int DEFAULT_FLOOR = 31;

    /**
     * The bond a horse is left with after {@code days} whole Minecraft days have
     * passed without it.
     *
     * @param days   whole days since the last decay; zero or negative is a no-op,
     *               so a stamp from the future (a rolled-back world) cannot
     *               <em>raise</em> the number
     * @param perDay {@code behaviour.bond_decay_per_day}; 0 or less turns it off
     * @param floor  {@code behaviour.bond_floor}; a horse already at or below it
     *               is untouched
     */
    public static int decayed(int bond, long days, int perDay, int floor) {
        if (days <= 0 || perDay <= 0 || bond <= floor) {
            return bond;
        }
        // Whether the floor is reached is asked before the subtraction, not
        // after it: a stamp old enough to make days * perDay overflow would
        // otherwise come back negative and read as bond owed back.
        long room = (long) bond - floor;
        long daysToFloor = (room + perDay - 1) / perDay;
        return days >= daysToFloor ? floor : (int) (bond - days * perDay);
    }
}
