package com.example.horsegenetics.common.care;

/**
 * <b>A horse is never killed by one blow</b> - the arithmetic of the save, with
 * no game in it.
 *
 * <p>Owner's ask: <i>"Horses should never be able to be one-shot and die. Any
 * health which would kill them takes them to one health point instead, and they
 * get 120 ticks of immunity to damage, then the immunity disables until they
 * heal back to full health again."</i> All three numbers are the defaults of
 * server settings: {@code behaviour.last_stand},
 * {@code behaviour.last_stand_health}, {@code behaviour.last_stand_immunity_ticks}
 * and {@code behaviour.last_stand_rearm_fraction}.
 *
 * <p>Here rather than in the game module for {@link Escape}'s reason: what is
 * left once the level is taken out is three thresholds and a clock, and a clock
 * with a "never" value in it is exactly the kind of thing that is wrong in a way
 * nobody notices from inside a running game. What stays in NeoForge is the part
 * only a level can answer - what hurt it, whether that thing is allowed to be
 * survived, and where the stamp is kept.
 *
 * <h2>Why the save has to be spent, and re-earned</h2>
 * A save with no cost is immortality with extra steps: a horse standing in lava
 * would be rescued to one health point every six seconds forever. Spending it
 * makes the save a <em>chance to get away</em> rather than a state - the horse
 * survives the blow, has {@link #DEFAULT_IMMUNITY_TICKS} ticks in which nothing
 * can touch it, and then is as mortal as any other animal until it has genuinely
 * recovered. The recovery is the price, and it is the price a player already pays
 * to heal a horse at all - food, water and {@link Hunger} to spend.
 *
 * <h2>Why re-arming is read off the health, not ticked</h2>
 * "Healed back to full" is a fact about the horse, not an event: reading it at
 * the moment something is hurting the horse costs nothing the rest of the time,
 * cannot go stale while the chunk is unloaded, and cannot miss the heal that
 * finished the job. The same reasoning as
 * {@code HorseCareHandler.creditBondTiers}, for the same reason.
 */
public final class LastStand {

    private LastStand() {
    }

    /**
     * <b>The health a saved horse is left standing on</b> - one point, which is
     * half a heart. The default of {@code behaviour.last_stand_health}.
     */
    public static final double DEFAULT_HEALTH_LEFT = 1.0;

    /**
     * <b>How long nothing can touch a saved horse</b>, in ticks - the owner's
     * 120, which is six seconds. The default of
     * {@code behaviour.last_stand_immunity_ticks}.
     *
     * <p>Six seconds is long enough to gallop out of a creeper's blast radius or
     * off a burning block, which is what the window is <em>for</em> - it buys the
     * horse the distance its own {@link Escape bolt} needs, since a horse that is
     * saved and then hit again on the next tick was not saved.
     */
    public static final int DEFAULT_IMMUNITY_TICKS = 120;

    /**
     * <b>How far back a horse has to heal before the save is available again</b>,
     * as a fraction of its own maximum - all the way, by default. The default of
     * {@code behaviour.last_stand_rearm_fraction}.
     */
    public static final double DEFAULT_REARM_FRACTION = 1.0;

    /**
     * The smallest health a save may leave - a twentieth of a point. Not zero:
     * {@code setHealth(0)} is death, so a server that set the health to 0 would
     * have written a setting that kills the horse it saves.
     */
    public static final double MIN_HEALTH_LEFT = 0.05;

    /**
     * The stamp value meaning <b>the save has never been spent</b>. Any negative
     * number reads the same way, which matters because the store this is kept in
     * answers {@code Long.MIN_VALUE} for a key that was never written.
     */
    public static final long NEVER = -1L;

    /**
     * <b>Would this blow kill?</b> {@code damage} is what is about to come off
     * the horse's health, after armour; {@code health} is what it has now.
     *
     * <p>Greater-or-equal, not greater: damage exactly equal to the remaining
     * health leaves zero, and zero health is dead.
     */
    public static boolean fatal(double damage, double health) {
        return health > 0.0 && damage >= health;
    }

    /** <b>Is the save available?</b> {@code spentAt} is the stamp; see {@link #NEVER}. */
    public static boolean armed(long spentAt) {
        return spentAt < 0L;
    }

    /**
     * <b>Is this horse still inside its window?</b> {@code immunityTicks} is the
     * configured length; zero or less is a server that wants the save without the
     * breathing space.
     *
     * <p>{@code now < spentAt} counts as inside it, by {@link Escape#threatFresh}'s
     * reasoning: the game clock can go backwards across a reload, and a horse that
     * guesses wrong on that should guess in the direction of staying alive.
     */
    public static boolean immune(long spentAt, long now, int immunityTicks) {
        if (spentAt < 0L || immunityTicks <= 0) {
            return false;
        }
        return now < spentAt || now - spentAt < immunityTicks;
    }

    /**
     * <b>Has this horse recovered enough to be saved again?</b>
     *
     * <p>A {@code fraction} below 1 hands the save back before the horse has
     * really recovered, and one low enough to sit under {@link #healthLeft} makes
     * the horse immortal - it is re-armed the instant it is saved. That is the
     * server's call to make and not this method's to refuse, but it is why the
     * setting's comment says so out loud.
     */
    public static boolean rearms(double health, double maxHealth, double fraction) {
        return maxHealth > 0.0 && health >= maxHealth * fraction;
    }

    /**
     * <b>What a saved horse is actually set to</b>, clamped into the health it
     * could possibly have.
     *
     * <p>Never zero or less, or the save would kill what it was saving, and never
     * more than the horse's own maximum - which is a real case rather than a
     * defensive one, since {@code behaviour.last_stand_health} is a flat number of
     * health points and the smallest horses in this mod have fewer than most
     * servers would think to type.
     */
    public static float healthLeft(double configured, double maxHealth) {
        double capped = maxHealth > 0.0 ? Math.min(configured, maxHealth) : configured;
        return (float) Math.max(capped, MIN_HEALTH_LEFT);
    }

    /**
     * <b>The line the owner reads when a horse should have died and did not.</b>
     *
     * <p>It exists for the reason the lethal-foal chat line does: a horse that
     * walks out of an explosion on half a heart reads as the mod failing to kill
     * it. One line, to the owner alone, and it says the whole rule - because the
     * half of it that matters is the half that happens later, when the same horse
     * is not saved a second time.
     *
     * <p>Not rate-limited, and it does not need to be: a horse can only be saved
     * once per recovery, so the only way to see this twice in a row is to heal one
     * to full in between.
     */
    public static String survived(String horseName, int immunityTicks) {
        String line = horseName + " took a killing blow and is still standing, on its last breath.";
        if (immunityTicks > 0) {
            line += " Nothing can touch it for " + seconds(immunityTicks) + " seconds - get it out.";
        }
        return line + " It will not be saved again until it is back to full health.";
    }

    /**
     * Ticks as the seconds a player counts, with the trailing {@code .0} dropped -
     * "6", not "6.0", and "1.5" where the server asked for thirty ticks.
     */
    static String seconds(int ticks) {
        double s = ticks / 20.0;
        if (s == Math.floor(s)) {
            return Long.toString((long) s);
        }
        return String.valueOf(Math.round(s * 10.0) / 10.0);
    }
}
