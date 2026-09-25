package com.example.horsegenetics.common.horse;

/**
 * <b>When an emergency chamber takes the horse out of the fight.</b>
 *
 * <p>An <i>Emergency Horse Stasis Chamber</i> is carried, not used: it watches
 * the horses its owner owns and swallows one the instant that horse's health
 * falls below {@link #DEFAULT_THRESHOLD} of its own maximum. A horse inside any
 * chamber cannot be hurt at all, so going in is a hard stop against dying - the
 * capture <b>is</b> the save, and the player does nothing to earn it.
 *
 * <p>An empty one <b>filed in a stasis bank</b> counts too, and is the fallback
 * for the chamber you forgot to take out: see {@link #savedInBank}. Which of the
 * two caught the horse is the only thing that differs downstream, and it differs
 * in exactly one sentence.
 *
 * <p>This class is the arithmetic and the words. Which horse, whose, what is in
 * whose inventory and how a live animal becomes a data component are all
 * {@code neoforge/server/EmergencyStasisHandler}'s, the way {@code LastStand} and
 * {@code HorseLastStandHandler} split the same job.
 *
 * <h2>Why a fraction, and why it is below the bolt</h2>
 * A horse's maximum health is its own - this mod breeds frail ponies and
 * Percherons off the same code - so a flat number of hearts would be a
 * different rescue for each of them. The default sits deliberately <b>under</b>
 * {@code Escape.DEFAULT_THRESHOLD}: a horse bolts at a fifth and is captured at
 * a tenth, so the ordinary escape behaviour still gets its chance to save the
 * animal the ordinary way first, and the chamber is the thing that happens when
 * running did not work.
 *
 * <h2>A state, not a crossing</h2>
 * {@link #rescues} asks where the health <i>is</i>, not whether this blow moved
 * it across the line. A horse already under the threshold - because its owner
 * was out of range, or carrying nothing, or logged out when it first got hurt -
 * must still be caught by the next chamber that can reach it. Testing for a
 * crossing would make the rescue depend on who was watching at the moment it
 * first went low, which is not a thing a player can reason about.
 */
public final class StasisRescue {

    /**
     * How low health falls, as a fraction of the horse's own maximum, before an
     * emergency chamber takes it. The owner's number: a tenth.
     */
    public static final double DEFAULT_THRESHOLD = 0.10;

    /**
     * How long an owner is left alone between two refusals about the same horse.
     * Ten seconds: a full chamber refuses on <em>every</em> blow that horse
     * takes, and a horse in a fire takes one every half-second.
     */
    public static final long QUIET_TICKS = 200L;

    /** No refusal has been said about this horse yet. */
    public static final long NEVER = Long.MIN_VALUE;

    private StasisRescue() {
    }

    /**
     * <b>Is this horse low enough to be swallowed?</b>
     *
     * @param healthAfter what the horse's health will be once this blow has
     *                    landed - which may be zero or less, since a chamber is
     *                    allowed to catch a horse out of a killing blow
     * @param maxHealth   the horse's own maximum
     * @param threshold   the configured fraction; {@code 0} turns the whole
     *                    behaviour off
     */
    public static boolean rescues(float healthAfter, float maxHealth, double threshold) {
        if (threshold <= 0.0 || maxHealth <= 0.0F) {
            return false;
        }
        return healthAfter < maxHealth * threshold;
    }

    /** Has the quiet period since the last refusal about this horse run out? */
    public static boolean dueAgain(long lastToldAt, long now) {
        return lastToldAt == NEVER || now - lastToldAt >= QUIET_TICKS || now < lastToldAt;
    }

    /** What the owner is told when a chamber caught one, wherever they are standing. */
    public static String saved(String horseName) {
        return horseName + " was about to die - an emergency chamber has it.";
    }

    /**
     * The same news, when the chamber that took the horse was <b>filed in a
     * stasis bank</b> rather than carried.
     *
     * <p>A separate sentence rather than the same one, because the two leave the
     * player in different positions and the difference is actionable: a chamber
     * off your belt is gone from your belt, and a chamber in a bank means the
     * horse is already where the rest of the stable is and the bank is one
     * empty short. Saying "an emergency chamber has it" for both would have the
     * player patting their pocket for a bottle that is a thousand blocks away.
     */
    public static String savedInBank(String horseName) {
        return horseName + " was about to die - an emergency chamber in your stasis bank has it.";
    }

    /**
     * <b>Where the chamber that caught a horse was</b>, as the log row's
     * {@code other} column reads it. Empty means carried; this means filed.
     * Kept here beside the two sentences so the row and the chat line cannot
     * come to disagree about which happened.
     */
    public static final String FROM_BANK = "your stasis bank";

    /**
     * What the owner is told when it could not. One clear line naming the horse,
     * because the alternative is a player who believes they are insured and is
     * not - see {@code wiki/horse-stasis.html}, where refusing a second horse
     * rather than queueing or swapping is the settled call.
     */
    public static String refused(String horseName) {
        return "Your emergency chamber already has a horse in it - it cannot save " + horseName + ".";
    }
}
