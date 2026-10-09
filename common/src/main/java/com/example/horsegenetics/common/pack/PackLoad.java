package com.example.horsegenetics.common.pack;

import com.example.horsegenetics.common.cart.CartDraft;

/**
 * <b>What the chests on a horse's flanks cost it.</b> A count of items in, a
 * fraction of its own speed out.
 *
 * <h2>The rule, in one line</h2>
 * <b>Every item weighs the same, and how many a horse can carry is its pulling
 * ability.</b> (Owner's call, 2026-10-08.)
 *
 * <p>A chest hung on a horse keeps its own slots - nothing is locked, so a
 * modded chest with a hundred slots is a hundred slots - and what the horse
 * pays for is what is put <i>in</i> them. The load is a plain count of items
 * across both sides, whatever they are: sixty-four feathers weigh what
 * sixty-four anvils do, because a rule a player has to look up per item is a
 * rule nobody reasons about.
 *
 * <pre>{@code   most     = maxItems  * capacity(pull)
 *   free     = freeItems * capacity(pull)
 *   fraction = clamp((items - free) / (most - free), 0, 1)
 *   kept     = 1 - (1 - minSpeed) * fraction ^ exponent}</pre>
 *
 * <h2>One axis of strength</h2>
 * {@code capacity} is {@link CartDraft#capacity} - the same curve on the same
 * score a cart reads. A horse bred to haul a wagon is the horse that carries
 * the most, rather than two parallel breeding goals that happen to look alike.
 *
 * <h2>"The most a horse can ever carry"</h2>
 * {@code most} is where the curve ends. With {@code minSpeed} at zero - the
 * default - a horse loaded that far will not walk at all, which is what makes
 * it a maximum rather than a suggestion; a server that would rather an
 * overloaded horse crawled raises {@code minSpeed} instead. Nothing is refused
 * at the chest: the slot takes the item and the horse answers for it.
 *
 * <h2>What is exempt</h2>
 * Storage that is not actually on the horse - an ender chest is a door to
 * somewhere else - never reaches this class: its items are not counted by the
 * caller. See {@code HorsePacks.load} in the NeoForge module.
 *
 * <h2>Purity</h2>
 * No entity, no config, no Minecraft. The server's settings arrive as a
 * {@link Curve}, built in one place ({@code ServerConfig.packCurve}), so no
 * reader can fall back to a constant the server has overridden.
 */
public final class PackLoad {

    /** {@code packs.weight} - whether a loaded horse slows at all. */
    public static final boolean DEFAULT_ENABLED = true;

    /**
     * {@code packs.max_items} - what an ordinary horse can carry before it
     * stops: four vanilla chests of full stacks, twice what its two flanks hold
     * in vanilla chests. So two full chests cost an ordinary horse half its
     * speed, and a weak one cannot move them at all.
     */
    public static final int DEFAULT_MAX_ITEMS = 6912;    // 4 chests x 27 slots x 64; a literal, for bake-config-keys

    /** {@code packs.free_items} - what an ordinary horse carries for nothing. */
    public static final int DEFAULT_FREE_ITEMS = 0;

    /**
     * {@code packs.curve_exponent} - one is a straight line, every item costing
     * the same; above one the load arrives late, below one it arrives early.
     * Linear by default for {@link CartDraft#loaded}'s reason: half a load is
     * half the penalty, and a player can find that out without being told.
     */
    public static final double DEFAULT_EXPONENT = 1.0;

    /** {@code packs.min_speed} - the fraction of its speed a horse at its maximum keeps. */
    public static final double DEFAULT_MIN_SPEED = 0.0;

    public static final int MAX_ITEMS_LIMIT = 10_000_000;
    public static final double MIN_EXPONENT = 0.1;
    public static final double MAX_EXPONENT = 10.0;

    private PackLoad() {
    }

    /**
     * A server's settings for the load, carried as one value so that the curve
     * and its limits cannot be read separately.
     *
     * @param enabled   off, and every horse keeps all of its speed
     * @param freeItems items an ordinary horse carries at no cost
     * @param maxItems  items at which an ordinary horse is down to {@code minSpeed}
     * @param exponent  the curve's shape between the two
     * @param minSpeed  the fraction of its speed a horse keeps at or past its maximum
     */
    public record Curve(boolean enabled, int freeItems, int maxItems, double exponent, double minSpeed) {

        public static final Curve DEFAULT = new Curve(DEFAULT_ENABLED, DEFAULT_FREE_ITEMS,
                DEFAULT_MAX_ITEMS, DEFAULT_EXPONENT, DEFAULT_MIN_SPEED);

        /** A curve that costs nothing - what {@code packs.weight = false} means. */
        public static final Curve OFF = new Curve(false, DEFAULT_FREE_ITEMS,
                DEFAULT_MAX_ITEMS, DEFAULT_EXPONENT, DEFAULT_MIN_SPEED);

        public Curve {
            // Whatever a config file says, the curve stays a curve: a maximum
            // of at least one item, a free allowance below it, a sane shape.
            maxItems = Math.max(1, maxItems);
            freeItems = Math.max(0, Math.min(freeItems, maxItems - 1));
            exponent = Double.isNaN(exponent) ? DEFAULT_EXPONENT
                    : Math.max(MIN_EXPONENT, Math.min(MAX_EXPONENT, exponent));
            minSpeed = Double.isNaN(minSpeed) ? DEFAULT_MIN_SPEED
                    : Math.max(0.0, Math.min(1.0, minSpeed));
        }
    }

    /** The most this horse can carry: where the curve ends. */
    public static double most(final Curve curve, final double pull) {
        return curve.maxItems() * CartDraft.capacity(pull);
    }

    /** What this horse carries for nothing. */
    public static double free(final Curve curve, final double pull) {
        return curve.freeItems() * CartDraft.capacity(pull);
    }

    /** How far along its curve this load puts this horse: 0 unburdened, 1 at its maximum. */
    public static double fraction(final Curve curve, final long items, final double pull) {
        if (!curve.enabled() || items <= 0) {
            return 0.0;
        }
        final double free = free(curve, pull);
        final double span = most(curve, pull) - free;
        if (span <= 0.0) {
            return items > free ? 1.0 : 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (items - free) / span));
    }

    /**
     * The fraction of its own speed a horse keeps carrying {@code items}:
     * {@code 1.0} for an empty one, {@code curve.minSpeed()} at its maximum.
     */
    public static double retention(final Curve curve, final long items, final double pull) {
        final double fraction = fraction(curve, items, pull);
        if (fraction <= 0.0) {
            return 1.0;
        }
        return 1.0 - (1.0 - curve.minSpeed()) * Math.pow(fraction, curve.exponent());
    }

    /**
     * The value to hand a multiplicative movement-speed modifier: negative or
     * zero. Named for the operation it feeds, as {@link CartDraft#speedModifier}
     * is - a wrong sign here makes a loaded horse faster.
     */
    public static double speedModifier(final Curve curve, final long items, final double pull) {
        return retention(curve, items, pull) - 1.0;
    }
}
