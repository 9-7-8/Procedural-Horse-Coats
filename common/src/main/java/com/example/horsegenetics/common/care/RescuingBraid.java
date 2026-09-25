package com.example.horsegenetics.common.care;

/**
 * <b>When a rescuing braid spends itself.</b>
 *
 * <p>A braid is worn in a horse's mane or tail and does one thing: at the moment
 * the animal would die, it breaks, and the horse - with whoever is aboard - is
 * somewhere else. Where {@link LastStand} buys six seconds standing in the same
 * fight, a braid buys the trip out of it.
 *
 * <p>This class is the rule and the words. Which horse, whose, where home is and
 * how an animal is moved are all
 * {@code neoforge/server/RescuingBraidHandler}'s, the way {@link LastStand} and
 * {@code HorseLastStandHandler} split the same job, and {@link Escape} and
 * {@code HorseEscapeGoal} split theirs.
 *
 * <h2>It rides on top of the last stand rather than replacing it</h2>
 * Owner's call. The two are on the same event and the braid is the later
 * subscriber, so the story is: the blow is refused, the horse is held at
 * {@link LastStand#healthLeft} with the totem particles, and <i>then</i> the
 * braid takes it home. The player gets both, in that order, which is the only
 * ordering in which the immunity window is worth anything - a horse teleported
 * out of a fight and then killed by the next thing it lands beside has been
 * moved rather than saved.
 *
 * <p>Which is why {@link #fires} tests <b>a state, not a crossing</b>, exactly as
 * {@code StasisRescue} does and for the same reason. By the time the braid is
 * asked, the last stand has usually already zeroed the damage - so "did this
 * blow kill it" is the wrong question and would answer no on precisely the
 * horses the braid exists for. The question is <i>is this animal at the end of
 * its life right now</i>, and there are two ways to be: the blow is fatal, or
 * something has just held it on the floor.
 */
public final class RescuingBraid {

    private RescuingBraid() {
    }

    /**
     * <b>Should a worn braid break now?</b>
     *
     * @param damage what will really come off the health - after armour, after
     *               enchantments, and after anything earlier on the event has
     *               refused it
     * @param health what the horse has now
     * @param floor  the health a last stand leaves a horse on -
     *               {@link LastStand#healthLeft}. A horse sitting on it has just
     *               been saved by the width of a hair, or was already there; both
     *               are the moment a braid is for
     */
    public static boolean fires(double damage, double health, double floor) {
        if (health <= 0.0) {
            return false;       // already gone - there is nothing left to send home
        }
        return damage >= health || health <= floor;
    }

    /**
     * What the owner is told. Names the horse and where it went, because the
     * whole point of the item is that it moves the animal while the player is
     * busy with whatever was killing it - and a save that does not say where is
     * a horse the player then has to go and find.
     */
    public static String saved(String horseName, String destination) {
        return horseName + "'s braid broke - it is back at " + destination + ".";
    }

    /** {@link #saved}'s second half, for a horse sent to its own stall. */
    public static final String ITS_STALL = "its stall";

    /** And for one sent to the owner's holding pen, having no stall of its own. */
    public static final String THE_HOLDING_PEN = "your holding pen";

    /**
     * What the owner is told when a braid could not fire.
     *
     * <p>A braid resolves its destination at the moment it breaks rather than
     * being bound to one when it is made (owner's call), so the one way it can
     * fail is that there is nowhere to send the horse: no stall, no holding pen,
     * or no room in either. <b>The braid is not spent</b> in that case - it is
     * still in the horse's hair, and it will work the day the player hangs a
     * sign up.
     *
     * <p>Said out loud rather than passed over in silence, for the reason the
     * emergency chamber's refusal is: a player who believes a horse is insured
     * and is not finds out by finding a corpse.
     */
    public static String nowhereToSend(String horseName) {
        return horseName + " is wearing a braid and has nowhere to go - "
                + "give it a stall, or hang up a holding pen sign.";
    }
}
