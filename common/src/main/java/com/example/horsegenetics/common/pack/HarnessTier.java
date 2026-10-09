package com.example.horsegenetics.common.pack;

/**
 * <b>What a storage harness's fittings are made of</b>, and how much of the
 * harness bonus that metal earns.
 *
 * <p>A horse wears a harness before it can carry a chest at all, and the
 * harness takes a share of the load off it. The leather is the same on every
 * one; what differs is the metal the frame and buckles are made of, and the
 * better the metal the larger the share. (Owner, 2026-10-08.)
 *
 * <p>The tiers divide <b>one number</b> between them - the best harness's
 * reduction, {@link PackLoad#DEFAULT_HARNESS_BEST} unless a server says
 * otherwise - in equal steps, so a server that wants gear to matter more than
 * breeding raises that one number and every tier moves with it. Ten per cent
 * by default, on purpose small: pulling ability is something you breed for,
 * and a harness that out-carried a draught line would make the breeding
 * pointless.
 *
 * <p>The order is the order of the constants. A new tier goes where it ranks
 * and every share re-divides; nothing saved refers to a tier by number.
 */
public enum HarnessTier {
    COPPER,
    IRON,
    GOLD,
    NETHERITE;

    /** This tier's part of the best harness's reduction: the last tier's is all of it. */
    public double share() {
        return (ordinal() + 1) / (double) values().length;
    }

    /** The fraction of the load this tier takes off, when the best takes off {@code best}. */
    public double reduction(final double best) {
        return share() * PackLoad.harnessBest(best);
    }
}
