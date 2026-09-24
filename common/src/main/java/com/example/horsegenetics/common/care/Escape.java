package com.example.horsegenetics.common.care;

/**
 * <b>When a horse stops being a mount and becomes an animal trying not to
 * die</b> - the arithmetic of bolting, with no game in it.
 *
 * <p>Owner's ask: <i>"When a horse reaches 20% health (threshold configurable),
 * it should buck off its owner, jump fences, and do whatever it can to escape
 * damage and avoid dying."</i> The setting is
 * {@code behaviour.escape_health_fraction}.
 *
 * <p>Here rather than in the game module for {@link HurtNotice}'s reason: three
 * of the four decisions in that sentence are thresholds and durations, and a
 * threshold with hysteresis is exactly the kind of thing that is wrong in a way
 * nobody notices from inside a running game. What is left in NeoForge is the
 * part only a level can answer - what hurt it, which way is away, and whether
 * the fence in front of it can be jumped.
 *
 * <h2>Why bolting has to be harder to stop than to start</h2>
 * The naive rule - run while health is at or below the threshold - makes a
 * horse that heals a single point stop dead in front of the thing that was
 * killing it, and then start again on the next hit. {@link #RECOVERY_MARGIN}
 * is the whole fix: it starts at the threshold and stops only once the horse
 * is clear of it, so one bolt is one bolt rather than a stutter.
 *
 * <h2>And why it outlasts the blow</h2>
 * Damage is instantaneous and flight is not. {@link #THREAT_LINGERS_TICKS}
 * keeps a horse running for ten seconds after the last thing that hurt it,
 * because a horse that stops the tick the zombie stops swinging has not
 * escaped anything - it is standing next to a zombie.
 */
public final class Escape {

    private Escape() {
    }

    /**
     * <b>At or below this fraction of its own maximum health, a horse bolts.</b>
     * The owner's twenty percent, and the default of the config setting that
     * replaces it.
     */
    public static final double DEFAULT_THRESHOLD = 0.20;

    /**
     * How far back above the threshold a horse has to climb before it stops
     * running - five percent of its maximum. Without it, healing one point mid
     * flight ends the flight.
     */
    public static final double RECOVERY_MARGIN = 0.05;

    /**
     * How long a horse keeps running after the last thing that hurt it, in
     * ticks - ten seconds. Long enough to get out of a pen and away from what
     * was in it; short enough that a horse left alone at low health goes back
     * to grazing rather than sprinting off to the horizon.
     */
    public static final long THREAT_LINGERS_TICKS = 200L;

    /**
     * <b>Should this horse start running?</b> {@code threshold} is the
     * configured fraction; zero or less turns the behaviour off entirely, which
     * is what a server that does not want it sets.
     */
    public static boolean bolts(double health, double maxHealth, double threshold) {
        return threshold > 0.0 && maxHealth > 0.0 && health <= maxHealth * threshold;
    }

    /**
     * <b>Should a horse already running keep running?</b> The same question as
     * {@link #bolts} with {@link #RECOVERY_MARGIN} added, so that the health it
     * takes to stop is strictly more than the health it took to start.
     */
    public static boolean keepsBolting(double health, double maxHealth, double threshold) {
        return threshold > 0.0 && maxHealth > 0.0
                && health <= maxHealth * (threshold + RECOVERY_MARGIN);
    }

    /**
     * Is whatever hurt this horse recent enough to still be worth running from?
     * {@code hurtAt} is the game tick of the last damage, or a negative number
     * for a horse nothing has hurt.
     *
     * <p>{@code now < hurtAt} is treated as fresh rather than as expired: the
     * game clock can go backwards across a reload, and a horse that guesses
     * wrong on that should guess in the direction of staying alive.
     */
    public static boolean threatFresh(long hurtAt, long now) {
        return hurtAt >= 0L && (now < hurtAt || now - hurtAt <= THREAT_LINGERS_TICKS);
    }
}
