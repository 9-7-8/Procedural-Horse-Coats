package com.example.horsegenetics.common.gear;

/**
 * <b>Who steers a ridden horse, and why.</b> The one place the rule is written.
 *
 * <p>Vanilla hands the reins to a player passenger only when the horse is
 * saddled. This mod splits what the vanilla saddle did (#72): the vanilla item is
 * now the <b>bridle</b>, worn in the Headstall slot, and the bridle is what
 * steers. A horse at the top bond tier still takes direction from its owner with
 * nothing on its head at all, which is the bareback rule that came first.
 *
 * <p>The NeoForge side reads the horse and asks this; it never decides itself.
 * {@code BarebackSteeringHandler} then lends a hidden saddle for either answer
 * but {@link Who#NOBODY}, so vanilla's mounted path runs unchanged.
 *
 * <p><b>A saddle alone does not steer</b> (#74). Below the bareback tier a
 * horse that wears a saddle and no bridle carries its rider as a passenger and
 * goes where it likes, as a horse with nothing on does. A server can turn that
 * off ({@code behaviour.saddle_needs_bridle}), and then a saddle steers for
 * anyone, as vanilla's did. The mod's own saddle (#75) is what will sit in
 * that slot; until then it is whatever another mod puts there.
 */
public final class Reins {

    /** The bond tier at which a horse takes direction from its owner bare. */
    public static final int BAREBACK_TIER = 3;

    /** {@code behaviour.saddle_needs_bridle}: on, a saddle without a bridle carries a passenger (owner, 2026-10-02). */
    public static final boolean DEFAULT_SADDLE_NEEDS_BRIDLE = true;

    /** Why the rider steers, or that nobody does. */
    public enum Who {
        /** Nobody steers: the rider is a passenger and the horse goes where it likes. */
        NOBODY,
        /** Anyone riding steers, because the horse wears a bridle (owner, 2026-10-02). */
        BRIDLE,
        /** The owner steers with no bridle, because the bond is high enough. */
        BAREBACK,
        /** Anyone riding steers by the saddle, because the server turned the saddle rule off. */
        SADDLE
    }

    /**
     * @param playerRider  the first passenger is a player
     * @param tamed        the horse is tamed - an untamed horse throws its rider, whatever it wears
     * @param bridled      the horse wears a bridle in its Headstall
     * @param saddled      the horse wears a real saddle in its saddle slot - not a bridle, not a lent one
     * @param riderIsOwner the rider is the horse's owner
     * @param bondTier     the horse's behaviour tier ({@code HorseCareAttachment.behaviourTier})
     * @param bolting      the horse is running for its life, which takes the reins from anyone
     * @param saddleNeedsBridle the server's {@code behaviour.saddle_needs_bridle}
     */
    public static Who steers(boolean playerRider, boolean tamed, boolean bridled, boolean saddled,
                             boolean riderIsOwner, int bondTier, boolean bolting,
                             boolean saddleNeedsBridle) {
        if (!playerRider || !tamed || bolting) {
            return Who.NOBODY;
        }
        if (bridled) {
            return Who.BRIDLE;
        }
        if (saddled && !saddleNeedsBridle) {
            return Who.SADDLE;
        }
        // The bond works through a saddle as well as bare: tier 3 "steers freely
        // with or without a bridle" (owner, 2026-10-02). With a saddle on it is
        // not a bareback ride, which the caller tells apart by the saddle.
        return riderIsOwner && bondTier >= BAREBACK_TIER ? Who.BAREBACK : Who.NOBODY;
    }

    private Reins() {
    }
}
