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
 * <p>Piece 2 of #72 adds the mod saddle and the passenger-only rule; it is
 * a new input here, not a second rule somewhere else.
 */
public final class Reins {

    /** The bond tier at which a horse takes direction from its owner bare. */
    public static final int BAREBACK_TIER = 3;

    /** Why the rider steers, or that nobody does. */
    public enum Who {
        /** Nobody steers: the rider is a passenger and the horse goes where it likes. */
        NOBODY,
        /** Anyone riding steers, because the horse wears a bridle (owner, 2026-10-02). */
        BRIDLE,
        /** The owner steers with no bridle, because the bond is high enough. */
        BAREBACK
    }

    /**
     * @param playerRider  the first passenger is a player
     * @param tamed        the horse is tamed - an untamed horse throws its rider, whatever it wears
     * @param bridled      the horse wears a bridle in its Headstall
     * @param riderIsOwner the rider is the horse's owner
     * @param bondTier     the horse's behaviour tier ({@code HorseCareAttachment.behaviourTier})
     * @param bolting      the horse is running for its life, which takes the reins from anyone
     */
    public static Who steers(boolean playerRider, boolean tamed, boolean bridled,
                             boolean riderIsOwner, int bondTier, boolean bolting) {
        if (!playerRider || !tamed || bolting) {
            return Who.NOBODY;
        }
        if (bridled) {
            return Who.BRIDLE;
        }
        return riderIsOwner && bondTier >= BAREBACK_TIER ? Who.BAREBACK : Who.NOBODY;
    }

    private Reins() {
    }
}
